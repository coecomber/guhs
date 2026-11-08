package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-LEVEN-RITME: the day schedule of the town guhs.
 * <ul>
 *   <li><b>Night</b> = time of day {@link #DICHT} .. {@link #OPEN} (or forced by {@code /guhs vadsrijk ritme nacht|dag}).</li>
 *   <li><b>Shops close</b>: a shopkeeper ({@link #WINKELROLLEN}) walks home to the nearest free bed (within 40 blocks) and sleeps there
 *   until morning; where it stood a "Gesloten, morgen weer open!" sign and a night bell appear. The bell (and clicking the sleepy
 *   shopkeeper itself) opens the very same service: nobody is ever blocked.</li>
 *   <li><b>Quest flow never breaks</b> (the quest robot's assumption, gametested): an NPC that appears in ANY quest (giver, turn-in,
 *   talk/escort target, speaker, tracker spot), a travel NPC, an NPC with extra dialogue choices, or a shop whose role has no bound
 *   action never leaves its spot. Those stay 24/7 (they yawn a bit at night).</li>
 * </ul>
 * The guhs are kern/npc's own {@link RijkGuh}s, only moved ({@link RijkGuh#rust}: Npcs' keeper leaves a resting guh alone); the
 * bell + sign are code-spawned display/interaction entities ({@link Ritme#TAG}, never kept).
 */
public final class Rooster {
    /** Shops close at dusk and open at dawn (time of day). */
    public static final long DICHT = 13000, OPEN = 23000;
    /** Roles of a shop/stall that closes at night (only when its action is bound, see {@link #magSluiten}). */
    public static final Set<String> WINKELROLLEN = Set.of("winkel", "smid", "markt", "stal", "kleermaker", "juwelier", "wapenwinkel", "bank",
            "honingthee", "honingkraam", "chocomelk", "z4_vissoep", "z4_soep", "z4_limonade", "z6_tapijtwinkel");
    static final int BED_STRAAL = 40, LOOPTIJD = 20 * 25;

    /** null = follow the clock; true/false = forced night/day (dev). */
    @Nullable
    static Boolean forceer;
    private static Set<String> questNpcs;

    /** A shopkeeper that went home: its bed (null = no bed found: it dozes at its stall), since when, lying down yet. */
    private record Slaap(@Nullable Bedden.Bed bed, long sinds, boolean ligt) {
    }

    private static final Map<String, Slaap> SLAAP = new HashMap<>();
    /** npc id -> the sign + bell entities at its spot. */
    private static final Map<String, List<UUID>> BORD = new HashMap<>();
    /** bell interaction entity -> npc id. */
    private static final Map<UUID, String> BELLEN = new ConcurrentHashMap<>();
    /** Guhs that are asleep now (shopkeepers and the extra sleepers): RijkGuh.STIL keeps them from waving/strolling. */
    static final Set<UUID> SLAPEND = ConcurrentHashMap.newKeySet();

    private Rooster() {
    }

    // ---- the clock -----------------------------------------------------------------------------------------------------------

    public static boolean nacht(Level l) {
        if (forceer != null) {
            return forceer;
        }
        return nachtOp(Ritme.tijd(l));
    }

    public static boolean nachtOp(long tijd) {
        return tijd >= DICHT && tijd < OPEN;
    }

    /** Is this guh asleep by our schedule (RijkGuh.STIL)? */
    public static boolean slaapt(RijkGuh g) {
        return SLAPEND.contains(g.getUUID());
    }

    // ---- who may close -------------------------------------------------------------------------------------------------------

    /** Every NPC id any quest mentions (giver, turn-in, objectives, speakers, tracker spots). Over-inclusive on purpose. */
    public static synchronized Set<String> questNpcs() {
        if (questNpcs == null || questNpcs.isEmpty()) {
            Set<String> s = new HashSet<>();
            for (Quest q : Quests.alle().values()) {
                s.add(q.gever());
                s.add(q.inleveren());
                s.addAll(q.aanbod());
                s.addAll(q.klaar());
                s.addAll(q.praat().keySet());
                for (List<String> l : q.praat().values()) {
                    s.addAll(l);
                }
                for (Quest.Doel d : q.doelen()) {
                    s.add(d.doel());
                    if (d.plek() != null && d.plek().startsWith("npc:")) {
                        s.add(d.plek().substring(4));
                    }
                    if (d.bron() != null) {
                        s.add(d.bron());
                    }
                }
            }
            questNpcs = s;
        }
        return questNpcs;
    }

    /** May this NPC go home at night? Only a shopkeeper with a bound service that no quest/travel/extra choice needs. */
    public static boolean magSluiten(@Nullable NpcDef d) {
        if (d == null || d.rol() == null || !WINKELROLLEN.contains(d.rol())) {
            return false;
        }
        if (!NpcRollen.gekoppeld(d.rol()) && !NpcRollen.bestaat(d.rol())) {
            return false;   // (the bell could not open anything: stay)
        }
        if (questNpcs().contains(d.id()) || QuestMotor.reisNpc(d.id()) || QuestMotor.EXTRA.containsKey(d.id())) {
            return false;
        }
        return Steden.op(d.plek()) != null;   // (only in the towns)
    }

    // ---- ticking -------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        if (sl.players().isEmpty()) {
            return;
        }
        boolean nacht = nacht(sl);
        long nu = sl.getGameTime();
        for (NpcDef d : Npcs.alle()) {
            Slaap s = SLAAP.get(d.id());
            if (s == null && (!nacht || !magSluiten(d))) {
                if (nacht && d.rol() != null && WINKELROLLEN.contains(d.rol())) {
                    gaap(sl, d);   // (a 24/7 shopkeeper: a yawn now and then)
                }
                continue;
            }
            RijkGuh g = Npcs.levend(sl, d.id());
            if (!nacht) {
                if (s != null) {
                    wakker(sl, d, g);
                }
                continue;
            }
            if (g == null) {
                // (unloaded: forget the walk; the keeper respawns it at its spot when somebody comes, then we start again)
                if (s != null && !Ritme.spelerBij(sl, d.plek(), 96)) {
                    SLAAP.remove(d.id());
                    Bedden.vrij(d.id());
                    Ritme.weg(sl, BORD.computeIfAbsent(d.id(), k -> Ritme.lijst()));
                }
                continue;
            }
            if (s == null) {
                naarBed(sl, d, g, nu);
            } else {
                slaapStap(sl, d, g, s, nu);
            }
            houBord(sl, d);
        }
    }

    private static void gaap(ServerLevel sl, NpcDef d) {
        if (sl.getRandom().nextInt(40) != 0) {
            return;
        }
        RijkGuh g = Npcs.levend(sl, d.id());
        if (g != null && g.emotes.current() == null && GuhEmotes.canStart(g)) {
            g.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
        }
    }

    private static void naarBed(ServerLevel sl, NpcDef d, RijkGuh g, long nu) {
        Bedden.Bed bed = Bedden.claim(sl, d.plek(), BED_STRAAL, d.id());
        SLAAP.put(d.id(), new Slaap(bed, nu, false));
        if (bed != null) {
            BlockPos b = bed.hoofd();
            g.rust(b, 24000);   // (Npcs' keeper leaves a resting guh alone; cleared at dawn)
            g.getNavigation().moveTo(bed.x(), b.getY(), bed.z(), 0.7);
        } else {
            g.rust(d.plek(), 24000);
        }
    }

    private static void slaapStap(ServerLevel sl, NpcDef d, RijkGuh g, Slaap s, long nu) {
        if (!s.ligt()) {
            boolean klaar;
            if (s.bed() == null) {
                klaar = true;   // (dozes off at its stall)
            } else {
                double dd = g.distanceToSqr(s.bed().x(), s.bed().y(), s.bed().z());
                klaar = dd < 2.5 || nu - s.sinds() > LOOPTIJD || !Ritme.spelerBij(sl, g.blockPosition(), 24);
                if (!klaar && g.getNavigation().isDone()) {
                    g.getNavigation().moveTo(s.bed().x(), s.bed().hoofd().getY(), s.bed().z(), 0.7);
                }
            }
            if (klaar) {
                if (s.bed() != null) {
                    g.getNavigation().stop();
                    g.snapTo(s.bed().x(), s.bed().y(), s.bed().z(), s.bed().yaw(), 0);
                }
                SLAPEND.add(g.getUUID());
                g.emotes.stop();
                g.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
                SLAAP.put(d.id(), new Slaap(s.bed(), s.sinds(), true));
            }
            return;
        }
        SLAPEND.add(g.getUUID());
        g.rust(s.bed() != null ? s.bed().hoofd() : d.plek(), 24000);
        if (s.bed() != null && g.distanceToSqr(s.bed().x(), s.bed().y(), s.bed().z()) > 2.0) {
            g.snapTo(s.bed().x(), s.bed().y(), s.bed().z(), s.bed().yaw(), 0);   // (respawned / pushed: back in bed)
        }
        if (g.emotes.current() != Emote.SLAPEN && GuhEmotes.canStart(g)) {
            g.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
        }
        if (sl.getRandom().nextInt(6) == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, g.getX(), g.getY() + 0.9, g.getZ(), 1, 0.15, 0.1, 0.15, 0.0);   // (a tiny "z")
        }
    }

    private static void wakker(ServerLevel sl, NpcDef d, @Nullable RijkGuh g) {
        SLAAP.remove(d.id());
        Bedden.vrij(d.id());
        Ritme.weg(sl, BORD.computeIfAbsent(d.id(), k -> Ritme.lijst()));
        BELLEN.values().removeIf(v -> v.equals(d.id()));
        if (g == null) {
            return;
        }
        SLAPEND.remove(g.getUUID());
        g.emotes.stop();
        g.rust(d.plek(), 1);   // (the rest ends at once: it walks home, or the keeper puts it back when it is far)
        BlockPos s = Npcs.staanPlek(sl, d.plek());
        if (!Ritme.spelerBij(sl, g.blockPosition(), 24)) {
            g.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, d.yaw(), 0);
        } else {
            g.getNavigation().moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 0.7);
            if (GuhEmotes.canStart(g)) {
                g.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
            }
        }
    }

    /** The "Gesloten" sign + the night bell at the shopkeeper's spot (only while somebody is near). */
    private static void houBord(ServerLevel sl, NpcDef d) {
        List<UUID> ids = BORD.computeIfAbsent(d.id(), k -> Ritme.lijst());
        boolean bij = Ritme.spelerBij(sl, d.plek(), 64);
        if (!bij) {
            if (!ids.isEmpty()) {
                Ritme.weg(sl, ids);
                BELLEN.values().removeIf(v -> v.equals(d.id()));
            }
            return;
        }
        if (Ritme.leeft(sl, ids)) {
            return;
        }
        Ritme.weg(sl, ids);
        BELLEN.values().removeIf(v -> v.equals(d.id()));
        BlockPos p = Npcs.staanPlek(sl, d.plek());
        double x = p.getX() + 0.5, y = p.getY(), z = p.getZ() + 0.5;
        var t = Ritme.tekst(sl, x, y + 2.1, z, Component.translatable(Ritme.P + "dicht.bord").withStyle(ChatFormatting.LIGHT_PURPLE), 0.8f, 0x80301822);
        var t2 = Ritme.tekst(sl, x, y + 1.75, z, Component.translatable(Ritme.P + "dicht.bel"), 0.55f, 0x60301822);
        var bel = Ritme.blok(sl, x, y + 0.9, z, Blocks.BELL.defaultBlockState(), 0.5f, d.yaw(), true);
        Interaction klik = Ritme.klikvak(sl, x, y, z, 0.9f, 1.6f, "bel");
        for (var e : new net.minecraft.world.entity.Entity[] {t, t2, bel, klik}) {
            if (e != null) {
                ids.add(e.getUUID());
            }
        }
        if (klik != null) {
            BELLEN.put(klik.getUUID(), d.id());
        }
    }

    // ---- clicks ---------------------------------------------------------------------------------------------------------------

    /** The night bell: ring, a sleepy voice, and the shop opens as by day. True = it was one of our bells. */
    static boolean belKlik(ServerPlayer p, Interaction i) {
        String id = BELLEN.get(i.getUUID());
        if (id == null) {
            return false;
        }
        NpcDef d = Npcs.van(id);
        p.level().playSound(null, i.getX(), i.getY() + 1, i.getZ(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (d == null || d.rol() == null) {
            return true;
        }
        p.sendOverlayMessage(Component.translatable(Ritme.P + "dicht.slaperig", Component.translatable(d.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!NpcRollen.doe(d.rol(), p)) {
            p.sendSystemMessage(Component.translatable(Ritme.P + "dicht.niemand", Component.translatable(d.naamKey())));
        }
        return true;
    }

    /** QuestMotor.KLIK_VOOR: clicking a sleeping shopkeeper wakes it a little and opens its shop. Never blocks anything else. */
    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        if (!SLAPEND.contains(g.getUUID()) || !SLAAP.containsKey(g.npcId())) {
            return false;
        }
        NpcDef d = Npcs.van(g.npcId());
        if (d == null || d.rol() == null) {
            return false;
        }
        p.sendOverlayMessage(Component.translatable(Ritme.P + "dicht.gewekt", Component.translatable(d.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        return NpcRollen.doe(d.rol(), p);   // (false: the normal click runs after all)
    }

    // ---- dev / tests ---------------------------------------------------------------------------------------------------------

    /** Tests: pretend this guh is the sleeping shopkeeper {@code id}. */
    static void testSlaap(String id, RijkGuh g, boolean aan) {
        if (aan) {
            SLAAP.put(id, new Slaap(null, 0, true));
            SLAPEND.add(g.getUUID());
        } else {
            SLAAP.remove(id);
            SLAPEND.remove(g.getUUID());
        }
    }

    static int aantalSlapend() {
        return SLAAP.size();
    }

    static int aantalBellen() {
        return BELLEN.size();
    }

    @Nullable
    static Bedden.Bed bedVan(String id) {
        Slaap s = SLAAP.get(id);
        return s == null ? null : s.bed();
    }

    /** Dev: everyone back now (used when the schedule is forced to day). */
    static void allesWakker(ServerLevel sl) {
        for (String id : List.copyOf(SLAAP.keySet())) {
            NpcDef d = Npcs.van(id);
            if (d != null) {
                wakker(sl, d, Npcs.levend(sl, id));
            }
        }
    }

    static void wis() {
        SLAAP.clear();
        BORD.clear();
        BELLEN.clear();
        SLAPEND.clear();
        questNpcs = null;
    }
}
