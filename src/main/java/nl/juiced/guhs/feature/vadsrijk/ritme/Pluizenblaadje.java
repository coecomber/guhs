package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-LEVEN-RITME: "Het &lt;stad&gt;se Pluizenblaadje", a notice board per town (next to the town's quest board) with today's silly news
 * and two small chores. Light and silly on purpose: the Bestelbord (crafting orders) and the Premiebord (bounties) stay the real
 * work boards; this one has fetch/deliver/find errands for the town's citizens, small coins and a knuffelmeter point.
 * <ul>
 *   <li><b>News</b> (3 items + the weather forecast + your knuffelmeter): picked per town per day from the town's own lines, the
 *   general lines (filled with a citizen and an object) and what happened yesterday (slimes cheered up in the zone, yesterday's
 *   biggest hugger). Same for everybody.</li>
 *   <li><b>Chores</b> (2 per town per day, the same for everybody, progress per player): ZOEK (find the lost thing near a citizen,
 *   it sparkles for you; hand in at the requester or the board), BEZORG (bring a parcel to a citizen), HAAL (pick a thing up at a
 *   citizen, bring it back to the requester or the board). Only citizens without any quest role take part (so quest clicks are
 *   never taken over). Reset at sunrise.</li>
 * </ul>
 * Data: {@code SpelerData.extra("uritme").klus}: {dag, "stad.i": {s: state, x/y/z: the find spot}}.
 */
public final class Pluizenblaadje {
    static final String SLEUTEL = "vr_uritme_krant";
    static final int KLUSJES = 2, NIEUWS_STAD = 6, NIEUWS_ALG = 16, DINGEN = 24, BELONING_MIN = 20, BELONING_MAX = 50;
    static final int NEEM = 10, LEVER = 20, SLUIT = 1;
    static final int BORD_STRAAL = 48;

    public enum Soort {
        ZOEK, BEZORG, HAAL
    }

    /** States of a chore for a player. */
    static final int OPEN = 0, BEZIG = 1, KLAAR_OM_IN_TE_LEVEREN = 2, GEDAAN = 3;

    /** Today's chore i of a town: who asks, what, the other citizen (where it lies / to whom / at whom), the reward. */
    public record Klus(String stad, int nr, Soort soort, String vrager, String ander, int ding, int munten) {
        public String sleutel() {
            return stad + "." + nr;
        }
    }

    private static final Map<String, List<UUID>> BORDEN = new HashMap<>();
    private static final Map<UUID, String> KLIKVAKKEN = new ConcurrentHashMap<>();
    private static final Map<UUID, String> GESPREK = new ConcurrentHashMap<>();
    /** zone -> day -> slimes cheered up (for the news). */
    private static final Map<Integer, Map<Long, Integer>> VERSLAGEN = new ConcurrentHashMap<>();
    /** town -> day -> (player name, happy guhs that day): the day's biggest hugger. */
    private static final Map<String, Map<Long, Map<String, Integer>>> KNUFFELAARS = new ConcurrentHashMap<>();
    /** Dev: shift "today" by this many days (to test the reset). */
    static long dagVerschuiving;

    private Pluizenblaadje() {
    }

    static void init() {
        Praat.luister(SLEUTEL, Pluizenblaadje::antwoord);
        Haken.VERSLAGEN.add(v -> {
            if (v.mob().level() instanceof ServerLevel sl) {
                int z = Steden.zone(v.mob().getX(), v.mob().getY(), v.mob().getZ());
                VERSLAGEN.computeIfAbsent(z, k -> new ConcurrentHashMap<>()).merge(dag(sl), 1, Integer::sum);
            }
        });
    }

    static long dag(ServerLevel sl) {
        return Ritme.dag(sl) + dagVerschuiving;
    }

    /** (Knuffelmeter) note a happy guh for yesterday's-hugger news. */
    static void knuffelNieuws(ServerPlayer p, String stad, long dag) {
        KNUFFELAARS.computeIfAbsent(stad, k -> new ConcurrentHashMap<>()).computeIfAbsent(dag, k -> new ConcurrentHashMap<>())
                .merge(p.getGameProfile().name(), 1, Integer::sum);
    }

    // ---- the chores of the day (deterministic) -------------------------------------------------------------------------------

    /** The citizens of a town that may take part: no role, not in any quest. */
    static List<NpcDef> burgers(Steden.Stad s) {
        List<NpcDef> l = new ArrayList<>();
        for (NpcDef d : Npcs.alle()) {
            if (d.rol() == null && Steden.op(d.plek()) == s && !Rooster.questNpcs().contains(d.id())) {
                l.add(d);
            }
        }
        l.sort((a, b) -> a.id().compareTo(b.id()));
        return l;
    }

    private static RandomSource rng(String stad, long dag, int salt) {
        return RandomSource.create(dag * 341873128712L + stad.hashCode() * 132897987541L + salt * 7919L);
    }

    /** Today's chores of this town (empty when the town has fewer than 2 citizens without quest roles). */
    public static List<Klus> klusjes(Steden.Stad s, long dag) {
        List<NpcDef> b = burgers(s);
        List<Klus> l = new ArrayList<>();
        if (b.size() < 2 || s.bord() == null) {
            return l;
        }
        RandomSource r = rng(s.id(), dag, 1);
        Soort[] soorten = Soort.values();
        int eerste = r.nextInt(soorten.length);
        for (int i = 0; i < KLUSJES; i++) {
            Soort so = soorten[(eerste + i) % soorten.length];
            int v = r.nextInt(b.size());
            int a = (v + 1 + r.nextInt(b.size() - 1)) % b.size();
            l.add(new Klus(s.id(), i, so, b.get(v).id(), b.get(a).id(), r.nextInt(DINGEN), BELONING_MIN + r.nextInt(BELONING_MAX - BELONING_MIN + 1)));
        }
        return l;
    }

    // ---- player progress -----------------------------------------------------------------------------------------------------

    private static CompoundTag klusData(ServerPlayer p, long dag) {
        CompoundTag d = Knuffelmeter.data(p);
        CompoundTag k = d.getCompoundOrEmpty("klus");
        if (k.getLongOr("dag", Long.MIN_VALUE) != dag) {
            k = new CompoundTag();
            k.putLong("dag", dag);
            d.put("klus", k);
        }
        return k;
    }

    static int staat(ServerPlayer p, Klus k, long dag) {
        return klusData(p, dag).getCompoundOrEmpty(k.sleutel()).getIntOr("s", OPEN);
    }

    private static void zetStaat(ServerPlayer p, Klus k, long dag, int s, @Nullable BlockPos plek) {
        CompoundTag kd = klusData(p, dag);
        CompoundTag e = kd.getCompoundOrEmpty(k.sleutel()).copy();
        e.putInt("s", s);
        if (plek != null) {
            e.putInt("x", plek.getX());
            e.putInt("y", plek.getY());
            e.putInt("z", plek.getZ());
        }
        kd.put(k.sleutel(), e);
        Knuffelmeter.data(p).put("klus", kd);
        Spelers.opgeslagen(p);
    }

    @Nullable
    private static BlockPos zoekPlek(ServerPlayer p, Klus k, long dag) {
        CompoundTag e = klusData(p, dag).getCompoundOrEmpty(k.sleutel());
        return e.contains("x") ? new BlockPos(e.getIntOr("x", 0), e.getIntOr("y", 0), e.getIntOr("z", 0)) : null;
    }

    /** Accept a chore (true when it was open). */
    static boolean neem(ServerPlayer p, ServerLevel sl, Klus k) {
        long dag = dag(sl);
        if (staat(p, k, dag) != OPEN) {
            return false;
        }
        BlockPos plek = null;
        if (k.soort() == Soort.ZOEK) {
            NpcDef a = Npcs.van(k.ander());
            BlockPos basis = a == null ? Steden.van(k.stad()).bord().get() : a.plek();
            RandomSource r = rng(k.stad(), dag, 50 + k.nr());
            BlockPos rond = basis.offset(r.nextInt(13) - 6, 0, r.nextInt(13) - 6);
            plek = Ritme.vrijeplek(sl, rond, 0, 3);
            if (plek == null) {
                plek = Npcs.staanPlek(sl, basis);
            }
        }
        zetStaat(p, k, dag, k.soort() == Soort.BEZORG ? BEZIG : BEZIG, plek);
        p.sendSystemMessage(Component.translatable(Ritme.P + "klus.genomen." + k.soort().name().toLowerCase(java.util.Locale.ROOT), naam(k.vrager()),
                ding(k), naam(k.ander())).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Hand in (the chore is ready): coins, a knuffelmeter point for the town, a happy sound. */
    static boolean lever(ServerPlayer p, ServerLevel sl, Klus k) {
        long dag = dag(sl);
        if (staat(p, k, dag) != KLAAR_OM_IN_TE_LEVEREN) {
            return false;
        }
        zetStaat(p, k, dag, GEDAAN, null);
        Spelers.geefMunten(p, k.munten());
        Knuffelmeter.tel(p, k.stad(), "klus:" + k.vrager(), dag);
        p.sendSystemMessage(Component.translatable(Ritme.P + "klus.gedaan", naam(k.vrager()), k.munten()).withStyle(ChatFormatting.LIGHT_PURPLE));
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.8f);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2, p.getZ(), 4, 0.3, 0.2, 0.3, 0.01);
        return true;
    }

    static Component naam(String npc) {
        NpcDef d = Npcs.van(npc);
        return d == null ? Component.literal(npc) : Component.translatable(d.naamKey());
    }

    static Component ding(Klus k) {
        return Component.translatable(Ritme.P + "krant.ding." + (1 + k.ding()));
    }

    /** QuestMotor.KLIK_VOOR: a citizen with chore business for this player (only citizens without quest roles take part). */
    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        if (!(p.level() instanceof ServerLevel sl) || g.npcId().isEmpty()) {
            return false;
        }
        Steden.Stad s = Steden.op(g.getX(), g.getY(), g.getZ());
        NpcDef eigen = Npcs.van(g.npcId());
        if (s == null || eigen == null) {
            return false;
        }
        long dag = dag(sl);
        for (Klus k : klusjes(s, dag)) {
            int st = staat(p, k, dag);
            String id = g.npcId();
            if (st == BEZIG && k.soort() == Soort.BEZORG && id.equals(k.ander())) {
                zetStaat(p, k, dag, KLAAR_OM_IN_TE_LEVEREN, null);
                zeg(p, g, "klus.bezorgd", naam(k.vrager()), ding(k));
                lever(p, sl, k);
                return true;
            }
            if (st == BEZIG && k.soort() == Soort.HAAL && id.equals(k.ander())) {
                zetStaat(p, k, dag, KLAAR_OM_IN_TE_LEVEREN, null);
                zeg(p, g, "klus.opgehaald", ding(k), naam(k.vrager()));
                return true;
            }
            if (st == KLAAR_OM_IN_TE_LEVEREN && id.equals(k.vrager())) {
                zeg(p, g, "klus.dankjewel." + k.soort().name().toLowerCase(java.util.Locale.ROOT), ding(k));
                lever(p, sl, k);
                return true;
            }
        }
        return false;
    }

    private static void zeg(ServerPlayer p, RijkGuh g, String key, Object... args) {
        Praat.scene(p, SLEUTEL + "_zeg", List.of(new Praat.Regel(g, "", Ritme.P + key, args)));
    }

    /** Every second: a ZOEK spot sparkles for its player; standing on it finds the thing. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 9) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long dag = dag(sl);
        for (ServerPlayer p : sl.players()) {
            Steden.Stad s = Steden.op(p.getX(), p.getY(), p.getZ());
            if (s == null) {
                continue;
            }
            for (Klus k : klusjes(s, dag)) {
                if (k.soort() != Soort.ZOEK || staat(p, k, dag) != BEZIG) {
                    continue;
                }
                BlockPos plek = zoekPlek(p, k, dag);
                if (plek == null) {
                    continue;
                }
                double d2 = p.distanceToSqr(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
                if (d2 < 2.5 * 2.5) {
                    zetStaat(p, k, dag, KLAAR_OM_IN_TE_LEVEREN, null);
                    sl.playSound(null, plek, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
                    sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, plek.getX() + 0.5, plek.getY() + 0.5, plek.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0);
                    p.sendSystemMessage(Component.translatable(Ritme.P + "klus.gevonden", ding(k), naam(k.vrager())).withStyle(ChatFormatting.LIGHT_PURPLE));
                } else if (d2 < 48 * 48) {
                    sl.sendParticles(p, ParticleTypes.END_ROD, true, false, plek.getX() + 0.5, plek.getY() + 0.4, plek.getZ() + 0.5, 3, 0.2, 0.3, 0.2, 0.01);
                    if (d2 < 12 * 12 && server.getTickCount() % 60 == 9) {
                        p.sendSystemMessage(Component.translatable(Ritme.P + "klus.warm." + (d2 < 5 * 5 ? "heet" : "warm")), true);
                    }
                }
            }
        }
    }

    // ---- the boards ----------------------------------------------------------------------------------------------------------

    static void tickBorden(MinecraftServer server) {
        if (server.getTickCount() % 20 != 5) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (Steden.Stad s : Steden.ALLE) {
            if (s.bord() == null) {
                continue;
            }
            BlockPos a = s.bord().get();
            List<UUID> ids = BORDEN.computeIfAbsent(s.id(), k -> Ritme.lijst());
            boolean bij = sl.hasChunk(a.getX() >> 4, a.getZ() >> 4) && Ritme.spelerBij(sl, a, BORD_STRAAL);
            if (!bij) {
                if (!ids.isEmpty()) {
                    Ritme.weg(sl, ids);
                    KLIKVAKKEN.values().removeIf(v -> v.equals(s.id()));
                }
                continue;
            }
            if (!Ritme.leeft(sl, ids)) {
                Ritme.weg(sl, ids);
                KLIKVAKKEN.values().removeIf(v -> v.equals(s.id()));
                bouwBord(sl, s, a, ids);
            }
        }
    }

    /** Where the board of this town stands (next to its quest board, on a free floor cell). */
    @Nullable
    static BlockPos bordPlek(ServerLevel sl, Steden.Stad s) {
        BlockPos a = s.bord().get();
        BlockPos p = Ritme.vrijeplek(sl, a, 2, 4);
        return p == null ? Ritme.vrijeplek(sl, a, 0, 6) : p;
    }

    private static void bouwBord(ServerLevel sl, Steden.Stad s, BlockPos a, List<UUID> ids) {
        BlockPos p = bordPlek(sl, s);
        if (p == null) {
            return;
        }
        double x = p.getX() + 0.5, y = p.getY(), z = p.getZ() + 0.5;
        int rot = Math.floorMod(Math.round((float) Math.toDegrees(Math.atan2(a.getX() - p.getX(), -(a.getZ() - p.getZ()))) / 22.5f) + 8, 16);
        Entity bord = Ritme.blok(sl, x, y, z, Blocks.BIRCH_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), 1.4f, 0, false);
        Entity titel = Ritme.tekst(sl, x, y + 2.55, z, Component.translatable(s.krantKey()).withStyle(ChatFormatting.GOLD), 0.75f, 0x70301822);
        Entity sub = Ritme.tekst(sl, x, y + 2.25, z, Component.translatable(Ritme.P + "krant.bord.sub"), 0.5f, 0x50301822);
        Interaction klik = Ritme.klikvak(sl, x, y, z, 1.1f, 2.1f, "krant");
        for (Entity e : new Entity[] {bord, titel, sub, klik}) {
            if (e != null) {
                ids.add(e.getUUID());
            }
        }
        if (klik != null) {
            KLIKVAKKEN.put(klik.getUUID(), s.id());
        }
    }

    /** A click on a board's click box. True = one of ours. */
    static boolean klik(ServerPlayer p, Interaction i) {
        String stad = KLIKVAKKEN.get(i.getUUID());
        if (stad == null) {
            return false;
        }
        Steden.Stad s = Steden.van(stad);
        if (s != null && p.level() instanceof ServerLevel sl) {
            sl.playSound(null, i.getX(), i.getY() + 1, i.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.1f);
            open(p, sl, s);
        }
        return true;
    }

    // ---- the newspaper (talking screen pages) --------------------------------------------------------------------------------

    /** The news lines of a town today: lang key + args (same for everybody). */
    public static List<Praat.Regel> nieuws(ServerLevel sl, Steden.Stad s, long dag) {
        List<Praat.Regel> l = new ArrayList<>();
        RandomSource r = rng(s.id(), dag, 7);
        l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.kop", Component.translatable(s.krantKey()), dag + 1));
        // 1. the town's own news
        l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.nieuws." + s.id() + "." + (1 + r.nextInt(NIEUWS_STAD))));
        // 2. a general item, filled with a citizen and an object
        List<NpcDef> b = burgers(s);
        Component wie = b.isEmpty() ? Component.translatable(Ritme.P + "krant.iemand") : Component.translatable(b.get(r.nextInt(b.size())).naamKey());
        l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.nieuws.alg." + (1 + r.nextInt(NIEUWS_ALG)), wie,
                Component.translatable(Ritme.P + "krant.ding." + (1 + r.nextInt(DINGEN))), 2 + r.nextInt(40)));
        // 3. yesterday
        int klodders = VERSLAGEN.getOrDefault(s.zone(), Map.of()).getOrDefault(dag - 1, 0);
        Map<String, Integer> gisteren = KNUFFELAARS.getOrDefault(s.id(), Map.of()).getOrDefault(dag - 1, Map.of());
        if (!gisteren.isEmpty()) {
            Map.Entry<String, Integer> top = gisteren.entrySet().stream().max(Map.Entry.comparingByValue()).get();
            l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.gisteren.knuffel", top.getKey(), top.getValue()));
        } else if (klodders > 0) {
            l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.gisteren.klodders", klodders));
        } else {
            l.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.gisteren.rustig." + (1 + r.nextInt(4))));
        }
        // 4. the weather forecast
        l.add(weerbericht(sl, s, dag));
        return l;
    }

    static Praat.Regel weerbericht(ServerLevel sl, Steden.Stad s, long dag) {
        int zone = s.zone();
        long start = zone == 1 ? WeerKlok.buiStartZone1(sl) : WeerKlok.buiStart(dag - dagVerschuiving, zone);
        boolean mist = WeerKlok.mistDag(dag + 1);
        if (start < 0 || zone == 0) {
            return new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.weer.droog" + (mist ? ".mist" : ""));
        }
        long uur = (start / 1000 + 6) % 24, min = (start % 1000) * 60 / 1000;
        String tijd = String.format(java.util.Locale.ROOT, "%d:%02d", uur, min);
        return new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.weer." + WeerKlok.soort(zone) + (mist ? ".mist" : ""), tijd);
    }

    static void open(ServerPlayer p, ServerLevel sl, Steden.Stad s) {
        long dag = dag(sl);
        List<Praat.Regel> regels = nieuws(sl, s, dag);
        // your knuffelmeter here
        int n = Knuffelmeter.stand(p, s.id());
        int volgende = -1;
        for (int d : Knuffelmeter.DREMPELS) {
            if (n < d) {
                volgende = d;
                break;
            }
        }
        regels.add(volgende > 0 ? new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.knuffel", Component.translatable(s.naamKey()), n, volgende)
                : new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.knuffel.vol", Component.translatable(s.naamKey()), n));
        // the chores
        List<Klus> kl = klusjes(s, dag);
        List<Praat.Optie> opties = new ArrayList<>();
        if (kl.isEmpty()) {
            regels.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.geen_klusjes"));
        }
        for (Klus k : kl) {
            int st = staat(p, k, dag);
            String soort = k.soort().name().toLowerCase(java.util.Locale.ROOT);
            regels.add(new Praat.Regel(null, s.krantKey(), Ritme.P + "krant.klus." + soort, k.nr() + 1, naam(k.vrager()), ding(k), naam(k.ander()), k.munten(),
                    Component.translatable(Ritme.P + "krant.staat." + st)));
            if (st == OPEN) {
                opties.add(new Praat.Optie(NEEM + k.nr(), Ritme.P + "krant.optie.neem." + (k.nr() + 1)));
            } else if (st == KLAAR_OM_IN_TE_LEVEREN) {
                opties.add(new Praat.Optie(LEVER + k.nr(), Ritme.P + "krant.optie.lever." + (k.nr() + 1)));
            }
        }
        opties.add(new Praat.Optie(SLUIT, Ritme.P + "krant.optie.sluit"));
        GESPREK.put(p.getUUID(), s.id());
        Praat.scene(p, SLEUTEL, regels, opties.toArray(new Praat.Optie[0]));
    }

    private static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        String stad = GESPREK.get(p.getUUID());
        if (optie < 0) {
            GESPREK.remove(p.getUUID());
        }
        if (stad == null || optie == SLUIT || optie < 0 || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        GESPREK.remove(p.getUUID());
        Steden.Stad s = Steden.van(stad);
        if (s == null) {
            return;
        }
        List<Klus> kl = klusjes(s, dag(sl));
        for (Klus k : kl) {
            if (optie == NEEM + k.nr()) {
                neem(p, sl, k);
            } else if (optie == LEVER + k.nr()) {
                lever(p, sl, k);
            }
        }
    }

    static int aantalBorden() {
        int n = 0;
        for (List<UUID> l : BORDEN.values()) {
            n += l.isEmpty() ? 0 : 1;
        }
        return n;
    }

    static void wis() {
        BORDEN.clear();
        KLIKVAKKEN.clear();
        GESPREK.clear();
    }
}
