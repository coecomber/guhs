package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;

/**
 * U-GROTKERKER (round U, user decision): every zone cave is a REPEATABLE mini-dungeon. One run per cave in the shared world:
 * <ol>
 *   <li><b>SLAAPT</b> (asleep): nothing spawned, the gate open. The first player who walks into the cave box starts a run.</li>
 *   <li><b>BEZIG</b> (running): two pack rooms ({@link KerkerData.Kerker#kamers}): room 1 one pack, room 2 a bigger pack + the
 *       mid-elite (the cave's old elite). Pack size and elite affixes grow with the player count at the START of the run. The
 *       GATE into the boss room is shut (barrier blocks in the tunnel's air, pink sparkles + a board with how many are left;
 *       it never shuts on anyone behind it or standing in it).</li>
 *   <li><b>BAAS</b>: everything in both rooms beaten: the gate opens and the end boss ({@link GrotBaas}) wakes up in the boss
 *       room, its HP scaled by the players in the cave at that moment (the start of the boss encounter).</li>
 *   <li><b>KLAAR</b> (cleared): every helper above 5 % gets a personal end chest (kern's model, {@link KerkerLoot}); the board counts
 *       down {@link #RESET_NA} (10 min), then the cave falls asleep again (mobs gone, gate shut on the next run).</li>
 * </ol>
 * <b>Reset rules:</b> a run that is not finished resets {@link #LEEG_NA} (1 min) after the last player left the cave (mobs respawn,
 * the boss resets, the gate re-closes on the next run). A cleared cave resets 10 min after the clear (also when everyone left
 * earlier: {@link #LEEG_RESET_NA_KLAAR} = false, so a clear can't be farmed by stepping out). <b>Shared world:</b> a newcomer
 * during a run simply joins it (they fight what is left and get a chest when they help more than 5 % against the boss); nobody
 * waits, there is never a second copy of a cave. <b>No doubles:</b> every run creature has a U-DUBBEL key
 * {@code grotkerker:<cave>:<slot>} and a run tag {@code guhs_gk_<cave>_<nonce>}; a creature from an older run (a chunk saved
 * mid-run, a restart) is refused when it loads.
 * <p><b>W-GROT (round W, user request):</b> the shared-world model above is replaced by a CLOSED dungeon door per cave ({@link nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur}) and
 * one group per cave ({@link DungeonRij}, as a {@link GrotDungeon}): a run only starts for the cave's group (solo / party / queue, through the door), once per
 * group; the run scales with the group's size; nobody else can be in the cave; a clear gives a cooldown per player instead of the
 * 10-min shared wait, and the cave closes when the group left.
 * <p>Dev: {@code /guhs vadsrijk grot run [id] | reset <id> | baas <id> | kist [id]}.
 */
public final class GrotKerker {
    public static final String DATA = "grotkerker";
    public static final String TAG = "guhs_grotkerker";
    /** Ticks after a clear until the cave falls asleep again (10 min). */
    public static int RESET_NA = 20 * 60 * 10;
    /** Ticks after the last player left until an unfinished run resets (1 min). */
    public static int LEEG_NA = 20 * 60;
    /** Also reset a CLEARED cave as soon as everyone left (false: a clear always waits the 10 min). */
    public static boolean LEEG_RESET_NA_KLAAR = false;

    public enum Staat {
        SLAAPT, BEZIG, BAAS, KLAAR
    }

    /** One planned creature of a run. */
    record Plan(String slot, int kamer, boolean elite) {
    }

    /** The run of one cave. */
    static final class Run {
        final String id;
        Staat staat = Staat.SLAAPT;
        long nonce;
        int spelers = 1, baasSpelers = 1;
        long start, klaarOp = -1, leegSinds = -1;
        final Map<String, Plan> plan = new LinkedHashMap<>();
        final Map<String, UUID> levend = new HashMap<>();
        final Map<String, Long> gespawnd = new HashMap<>();
        final Set<String> verslagen = new HashSet<>();
        final Set<Integer> kamerGemeld = new HashSet<>();
        final Set<UUID> deelnemers = new HashSet<>();
        final Set<UUID> introGehad = new HashSet<>();
        @Nullable
        UUID baas;
        long baasGespawnd = -1;
        final List<BlockPos> poortGezet = new ArrayList<>();
        boolean dicht, hersteld;
        @Nullable
        Display.TextDisplay bord;
        int hulpTeller;
        final Map<UUID, Long> botsMelding = new HashMap<>();

        Run(String id) {
            this.id = id;
        }
    }

    static final Map<String, Run> RUNS = new LinkedHashMap<>();
    private static long sessie = System.nanoTime() & 0xFFFFFL, teller;

    private GrotKerker() {
    }

    public static void register(IEventBus modBus) {
        GrotBazen.register(modBus);
        GrotDungeon.register();   // W-GROT: every cave run is a dungeon (type GROT) behind a closed door (dungeon.DungeonRij)
        Haken.VERSLAGEN.add(GrotKerker::verslagen);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            RUNS.clear();
            sessie = (System.nanoTime() ^ e.getServer().overworld().getSeed()) & 0xFFFFFL;
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(GrotKerker::binnenkomst);
        NeoForge.EVENT_BUS.addListener(GrotKerker::commando);
        // zone 6: the cave elite of u-z6wezens' own spawner lives in what is now the boss room: switched off while the run exists
        Spawners.VERBODEN_VOOR.add((s, pos) -> s.id().equals("z6wezens:grot_zoutslang") && Kerkers.van("zoutgrot") != null);
        // the book: zone 1-5 add "grotbaas verslagen" to U-GROTTEN's cave category (Grotten.categorie); zone 6 gets its own
        if (Kerkers.van("zoutgrot") != null) {
            Voltooiing.extraCategorie("zone6", () -> new Voltooiing.Categorie("grotkerker", "vadsrijk.grotkerker.boek.cat6", "minecraft:glow_lichen",
                    List.of(boekDing("zoutgrot", Kerkers.van("zoutgrot").baas().pos()))));
        }
    }

    /** Gametests: the test spot's chunks are loaded but not "entity ticking" (no real player): skip that check there. */
    static boolean TEST_KLAAR;

    /** U-DUBBEL's "are this chunk's entities really back" (only then may we judge "missing -> spawn"). */
    static boolean klaar(ServerLevel sl, BlockPos pos) {
        return (TEST_KLAAR && sl.isLoaded(pos)) || Dubbel.klaar(sl, pos);
    }

    public static Run run(String id) {
        return RUNS.computeIfAbsent(id, Run::new);
    }

    public static Staat staat(String id) {
        Run r = RUNS.get(id);
        return r == null ? Staat.SLAAPT : r.staat;
    }

    // ---- per player ----------------------------------------------------------------------------------------------------------

    public static int clears(ServerPlayer p, String grot) {
        return Spelers.data(p).extra(DATA).getIntOr(grot + ".clears", 0);
    }

    /** The book entry "grotbaas verslagen" of a cave (U-GROTTEN's category for zone 1-5 adds it, zone 6 its own). */
    public static Voltooiing.Ding boekDing(String grot, @Nullable BlockPos plek) {
        return new Voltooiing.Ding("grot_" + grot + "_baas", "vadsrijk.grotten.boek.baas." + grot, "", "vadsrijk.grotkerker.boek.hint.baas",
                "vadsrijk.grotkerker.grot." + grot, false, p -> clears(p, grot) > 0 ? Voltooiing.Staat.KLAAR : Voltooiing.Staat.NIETS, plek);
    }

    // ---- the tick --------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 10 != 0) {
            return;
        }
        for (Kerker k : Kerkers.alle()) {
            if (!Kerkers.TEST.containsKey(k.id())) {
                tick(sl, k, run(k.id()));
            }
        }
    }

    static List<ServerPlayer> inGrot(ServerLevel sl, Kerker k) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && k.bevat(p.getX(), p.getY(), p.getZ())) {
                l.add(p);
            }
        }
        return l;
    }

    static void tick(ServerLevel sl, Kerker k, Run r) {
        long nu = sl.getGameTime();
        List<ServerPlayer> binnen = inGrot(sl, k);
        if (binnen.isEmpty() && r.staat == Staat.SLAAPT && !r.dicht) {
            r.bord = null;
            return;
        }
        // after a restart / crash: a gate left shut in the world is opened once (only our barrier blocks)
        if (!r.hersteld && !k.poort().isEmpty() && sl.isLoaded(k.bord())) {
            r.hersteld = true;
            herstelPoort(sl, k, r);
        }
        // nobody here
        if (binnen.isEmpty()) {
            if (r.leegSinds < 0) {
                r.leegSinds = nu;
            }
            boolean onaf = r.staat == Staat.BEZIG || r.staat == Staat.BAAS;
            if ((onaf || (r.staat == Staat.KLAAR && LEEG_RESET_NA_KLAAR)) && nu - r.leegSinds >= LEEG_NA) {
                reset(sl, k, r, null);
            }
        } else {
            r.leegSinds = -1;
        }
        switch (r.staat) {
            case SLAAPT -> {
                // W-GROT: the run only starts for the cave's group (through the door), once per group
                List<ServerPlayer> leden = binnen.stream().filter(p -> DungeonRij.lid(p, k.id())).toList();
                if (!leden.isEmpty() && DungeonRij.magStarten(k.id()) && klaar(sl, k.kamers().get(0).pos()) && klaar(sl, k.baas().pos())) {
                    start(sl, k, r, leden);
                    DungeonRij.gestart(k.id());
                }
            }
            case BEZIG -> {
                meedoen(k, r, binnen);
                spawnOntbrekend(sl, k, r, nu);
                meldKamers(sl, k, r);
                if (alleGedaan(r)) {
                    naarBaas(sl, k, r, binnen);
                }
            }
            case BAAS -> {
                meedoen(k, r, binnen);
                GrotBaas b = baas(sl, k, r);
                if (b == null && klaar(sl, k.baas().pos()) && nu - r.baasGespawnd > 100) {
                    spawnBaas(sl, k, r, Math.max(1, binnen.size()));
                    b = baas(sl, k, r);
                }
                if (b != null) {
                    intro(sl, k, r, b);
                }
            }
            case KLAAR -> {
                if (nu - r.klaarOp >= RESET_NA && !iemandAchter(sl, k)) {
                    reset(sl, k, r, "vadsrijk.grotkerker.reset");
                }
            }
        }
        poort(sl, k, r, nu);
        bord(sl, k, r, nu);
    }

    // ---- the run ---------------------------------------------------------------------------------------------------------------

    static void start(ServerLevel sl, Kerker k, Run r, List<ServerPlayer> binnen) {
        r.staat = Staat.BEZIG;
        r.nonce = (sessie << 20) + (++teller);
        DungeonRij.Instantie gi = DungeonRij.instantie(k.id());   // W-GROT: the group's size scales the run (not who happens to be in the box)
        r.spelers = gi != null && !gi.leden.isEmpty() ? DungeonRij.groepsGrootte(k.id()) : Math.max(1, binnen.size());   // (+ companions)
        r.start = sl.getGameTime();
        r.klaarOp = -1;
        r.plan.clear();
        r.levend.clear();
        r.gespawnd.clear();
        r.verslagen.clear();
        r.kamerGemeld.clear();
        r.deelnemers.clear();
        r.introGehad.clear();
        r.baas = null;
        r.baasGespawnd = -1;
        r.hulpTeller = 0;
        int extra = r.spelers - 1;
        int n1 = Math.min(6, 3 + extra), n2 = Math.min(7, 4 + extra);
        for (int i = 0; i < n1; i++) {
            r.plan.put("k1_" + i, new Plan("k1_" + i, 0, false));
        }
        for (int i = 0; i < n2; i++) {
            r.plan.put("k2_" + i, new Plan("k2_" + i, 1, false));
        }
        r.plan.put("elite", new Plan("elite", 1, true));
        for (ServerPlayer p : binnen) {
            r.deelnemers.add(p.getUUID());
            p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.start").withStyle(ChatFormatting.GOLD));
            if (r.spelers > 1) {
                p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.start.groep", r.spelers).withStyle(ChatFormatting.GRAY));
            }
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 0.6f);
        }
        spawnOntbrekend(sl, k, r, sl.getGameTime());
    }

    static void meedoen(Kerker k, Run r, List<ServerPlayer> binnen) {
        for (ServerPlayer p : binnen) {
            if (r.deelnemers.add(p.getUUID())) {
                p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.meedoen", Component.translatable("vadsrijk.grotkerker.grot." + k.id()))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    static String sleutel(Kerker k, String slot) {
        return "grotkerker:" + k.id() + ":" + slot;
    }

    static String runTag(Kerker k, long nonce) {
        return "guhs_gk_" + k.id() + "_" + nonce;
    }

    static boolean alleGedaan(Run r) {
        for (String s : r.plan.keySet()) {
            if (!r.verslagen.contains(s)) {
                return false;
            }
        }
        return true;
    }

    static int over(Run r) {
        int n = 0;
        for (String s : r.plan.keySet()) {
            if (!r.verslagen.contains(s)) {
                n++;
            }
        }
        return n;
    }

    /** Spawns every planned creature that is not beaten and not in the world (first spawn, or lost). */
    static void spawnOntbrekend(ServerLevel sl, Kerker k, Run r, long nu) {
        GrotBazen.Pak pak = GrotBazen.PAKKEN.get(k.id());
        if (pak == null) {
            return;
        }
        int i = 0;
        for (Plan p : r.plan.values()) {
            i++;
            if (r.verslagen.contains(p.slot())) {
                continue;
            }
            Kamer kamer = k.kamers().get(Math.min(p.kamer(), k.kamers().size() - 1));
            if (!klaar(sl, kamer.pos())) {
                continue;
            }
            UUID u = r.levend.get(p.slot());
            if (u != null && (sl.getEntity(u) != null || nu - r.gespawnd.getOrDefault(p.slot(), 0L) < 100)) {
                continue;
            }
            Entity bestaand = Dubbel.zoek(sl, sleutel(k, p.slot()));
            if (bestaand != null) {
                r.levend.put(p.slot(), bestaand.getUUID());
                continue;
            }
            EntityType<? extends VadsrijkMob> type = (p.elite() ? pak.elite() : (i % 2 == 0 ? pak.b() : pak.a())).get();
            int lv = p.elite() ? k.eliteLv() : k.minLv() + sl.getRandom().nextInt(Math.max(1, k.maxLv() - k.minLv() + 1));
            Set<Affix> af = EnumSet.noneOf(Affix.class);
            if (p.elite()) {
                Affix[] alle = Affix.values();
                for (int a = 1; a < Math.min(3, r.spelers); a++) {   // 2 players: 1 affix, 3+: 2
                    af.add(alle[sl.getRandom().nextInt(alle.length)]);
                }
            }
            VadsrijkMob m = maak(sl, k, r, type, kamer, p.slot(), lv, p.elite(), af, p.elite() ? "grot:" + k.id() + "#elite" : "grot:" + k.id() + "#pak");
            if (m != null) {
                r.levend.put(p.slot(), m.getUUID());
                r.gespawnd.put(p.slot(), nu);
            }
        }
    }

    /** Makes one run creature on a free floor spot in the room (null: no spot found this time). */
    @Nullable
    static VadsrijkMob maak(ServerLevel sl, Kerker k, Run r, EntityType<? extends VadsrijkMob> type, Kamer kamer, String slot, int lv, boolean elite,
            Set<Affix> af, String spawner) {
        BlockPos plek = vrijePlek(sl, kamer, kamer.r());
        if (plek == null) {
            return null;
        }
        VadsrijkMob m = type.create(sl, EntitySpawnReason.SPAWNER);
        if (m == null) {
            return null;
        }
        m.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, sl.getRandom().nextFloat() * 360, 0);
        m.stelIn(lv, null, elite, af);
        m.spawner = spawner;
        m.addTag(TAG);
        m.addTag(runTag(k, r.nonce));
        Dubbel.merk(m, sleutel(k, slot));
        sl.addFreshEntity(m);
        return m;
    }

    /** A standable floor spot (solid below, 2 free above, no fluid) within r of the room centre, or null. */
    @Nullable
    static BlockPos vrijePlek(ServerLevel sl, Kamer kamer, int r) {
        var rnd = sl.getRandom();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int poging = 0; poging < 24; poging++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * Math.max(1, r);
            int x = kamer.x() + (int) Math.round(Math.cos(a) * d), z = kamer.z() + (int) Math.round(Math.sin(a) * d);
            for (int y = kamer.y() + 3; y >= kamer.y() - 3; y--) {
                m.set(x, y - 1, z);
                if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty() || sl.getBlockState(m).is(Blocks.BARRIER)) {
                    continue;
                }
                m.set(x, y, z);
                boolean v1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
                m.set(x, y + 1, z);
                boolean v2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
                m.set(x, y + 2, z);
                boolean v3 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
                if (v1 && v2 && v3) {
                    return new BlockPos(x, y, z);
                }
                break;
            }
        }
        return null;
    }

    static void meldKamers(ServerLevel sl, Kerker k, Run r) {
        for (int i = 0; i < k.kamers().size(); i++) {
            if (r.kamerGemeld.contains(i)) {
                continue;
            }
            boolean leeg = true, iets = false;
            for (Plan p : r.plan.values()) {
                if (p.kamer() == i) {
                    iets = true;
                    leeg &= r.verslagen.contains(p.slot());
                }
            }
            if (iets && leeg) {
                r.kamerGemeld.add(i);
                for (ServerPlayer p : inGrot(sl, k)) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.kamer", i + 1, r.kamerGemeld.size(), k.kamers().size())
                            .withStyle(ChatFormatting.GREEN));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                }
            }
        }
    }

    /** Both rooms beaten: the gate opens, the boss wakes up. */
    static void naarBaas(ServerLevel sl, Kerker k, Run r, List<ServerPlayer> binnen) {
        r.staat = Staat.BAAS;
        for (ServerPlayer p : binnen) {
            p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.poort.open").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.7f);
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.9f);
        }
        if (klaar(sl, k.baas().pos())) {
            spawnBaas(sl, k, r, Math.max(1, binnen.size()));
        }
    }

    @Nullable
    static GrotBaas baas(ServerLevel sl, Kerker k, Run r) {
        if (r.baas != null && sl.getEntity(r.baas) instanceof GrotBaas b && !b.isRemoved() && !b.geplofd()) {
            return b;
        }
        if (Dubbel.zoek(sl, sleutel(k, "baas")) instanceof GrotBaas b && !b.geplofd()) {
            r.baas = b.getUUID();
            return b;
        }
        return null;
    }

    @Nullable
    static GrotBaas spawnBaas(ServerLevel sl, Kerker k, Run r, int spelers) {
        var t = GrotBazen.TYPES.get(k.id());
        if (t == null) {
            return null;
        }
        GrotBaas b = t.get().create(sl, EntitySpawnReason.SPAWNER);
        if (b == null) {
            return null;
        }
        Kamer kamer = k.baas();
        b.snapTo(kamer.x() + 0.5, kamer.y(), kamer.z() + 0.5, sl.getRandom().nextFloat() * 360, 0);
        b.stelIn(k.baasLv(), null, false, EnumSet.noneOf(Affix.class));
        b.vasteSpelers = r.baasSpelers = Math.max(1, spelers);
        b.schaal(b.vasteSpelers);
        b.spawner = "grot:" + k.id() + "#baas";
        b.addTag(TAG);
        b.addTag(runTag(k, r.nonce));
        Dubbel.merk(b, sleutel(k, "baas"));
        sl.addFreshEntity(b);
        r.baas = b.getUUID();
        r.baasGespawnd = sl.getGameTime();
        r.introGehad.clear();
        return b;
    }

    /** The boss's intro for whoever steps into the boss room (the camera swoop the first time ever, else a short title card). */
    static void intro(ServerLevel sl, Kerker k, Run r, GrotBaas b) {
        for (ServerPlayer p : inGrot(sl, k)) {
            if (k.baas().binnen(p.getX(), p.getY(), p.getZ(), 0) && r.introGehad.add(p.getUUID())) {
                boolean eerste = Spelers.data(p).gezien.add("baas:grot_" + k.id());
                b.intro(p, eerste);
            }
        }
    }

    /** (GrotBaas, phase 2) Two cave creatures come to help (they belong to the run: a reset takes them away). */
    static void hulp(ServerLevel sl, GrotBaas b) {
        Kerker k = Kerkers.van(b.grot());
        GrotBazen.Def d = b.def();
        if (k == null || d == null) {
            return;
        }
        Run r = run(k.id());
        for (int i = 0; i < 2; i++) {
            String slot = "hulp" + (r.hulpTeller++);
            maak(sl, k, r, d.hulp().get(), k.baas(), slot, k.maxLv(), false, EnumSet.noneOf(Affix.class), "grot:" + k.id() + "#hulp");
        }
        for (ServerPlayer p : inGrot(sl, k)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.bijroep", Component.translatable(b.titelKey())).withStyle(ChatFormatting.RED));
        }
    }

    /** (GrotBaas.beloon) The boss is beaten: the run is cleared, the helpers' clears + book, the 10 min countdown. */
    static void baasVerslagen(ServerLevel sl, GrotBaas b, Set<ServerPlayer> helpers, Set<UUID> kist) {
        Kerker k = Kerkers.van(b.grot());
        if (k == null) {
            return;
        }
        Run r = run(k.id());
        r.staat = Staat.KLAAR;
        r.klaarOp = sl.getGameTime();
        r.baas = null;
        Set<ServerPlayer> iedereen = new HashSet<>(helpers);
        for (UUID id : b.bijdragen().spelers()) {
            if (sl.getEntity(id) instanceof ServerPlayer p) {
                iedereen.add(p);
            }
        }
        for (ServerPlayer p : iedereen) {
            int n = clears(p, k.id()) + 1;
            Spelers.data(p).extra(DATA).putInt(k.id() + ".clears", n);
            Component naam = Component.translatable(b.titelKey());
            p.sendSystemMessage(Component.translatable(kist.contains(p.getUUID()) ? "vadsrijk.grotkerker.verslagen" : "vadsrijk.grotkerker.verslagen.geen", naam)
                    .withStyle(ChatFormatting.GOLD));
            RijkScenes.titel(p, Component.translatable("vadsrijk.grotkerker.titel.klaar").withStyle(ChatFormatting.GOLD),
                    Component.translatable("vadsrijk.grotkerker.titel.klaar.onder", n), 70, 0xFFD24A);
        }
        DungeonRij.klaar(sl, k.id(), iedereen);   // W-GROT: the cooldown, "out through the door", the window
    }

    static String tijd(long ticks) {
        long s = Math.max(0, ticks / 20);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** The cave falls asleep: every run creature away, the gate open, the board gone. {@code melding}: told to whoever is inside. */
    public static void reset(ServerLevel sl, Kerker k, Run r, @Nullable String melding) {
        for (UUID u : r.levend.values()) {
            Entity e = sl.getEntity(u);
            if (e != null) {
                e.discard();
            }
        }
        if (r.baas != null && sl.getEntity(r.baas) != null) {
            sl.getEntity(r.baas).discard();
        }
        AABB doos = new AABB(k.minX(), k.minY(), k.minZ(), k.maxX() + 1, k.maxY() + 1, k.maxZ() + 1);
        for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, doos, m -> m.entityTags().contains(TAG))) {
            m.discard();
        }
        // V-INT: unopened end chests of the old run go too (the next run places fresh ones)
        for (int cx = k.minX() >> 4; cx <= k.maxX() >> 4; cx++) {
            for (int cz = k.minZ() >> 4; cz <= k.maxZ() >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk ch = sl.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) {
                    continue;
                }
                for (net.minecraft.core.BlockPos bp : new java.util.ArrayList<>(ch.getBlockEntities().keySet())) {
                    if (doos.contains(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5) && ch.getBlockEntity(bp) instanceof BaasKist.Inhoud) {
                        sl.removeBlock(bp, false);
                    }
                }
            }
        }
        if (r.dicht) {
            open(sl, r);
        }
        if (r.bord != null) {
            r.bord.discard();
            r.bord = null;
        }
        r.staat = Staat.SLAAPT;
        r.nonce = 0;
        r.plan.clear();
        r.levend.clear();
        r.verslagen.clear();
        r.baas = null;
        r.klaarOp = -1;
        r.leegSinds = -1;
        DungeonRij.runGereset(k.id());   // W-GROT: an unfinished run of a group that is still in may start again
        if (melding != null) {
            for (ServerPlayer p : inGrot(sl, k)) {
                p.sendSystemMessage(Component.translatable(melding).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    // ---- creatures beaten, stale ones refused ---------------------------------------------------------------------------------

    static void verslagen(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (!m.entityTags().contains(TAG) || m instanceof GrotBaas) {
            return;
        }
        String s = Dubbel.sleutel(m);
        if (s == null || !s.startsWith("grotkerker:")) {
            return;
        }
        String[] d = s.split(":");
        if (d.length < 3) {
            return;
        }
        Run r = RUNS.get(d[1]);
        Kerker k = Kerkers.van(d[1]);
        if (r == null || k == null || !m.entityTags().contains(runTag(k, r.nonce))) {
            return;
        }
        r.verslagen.add(d[2]);
        r.levend.remove(d[2]);
        if (d[2].equals("elite") && m.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : inGrot(sl, k)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.grotkerker.elite").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /** A run creature from an older run (or a run that already reset) does not come back when its chunk loads. */
    static void binnenkomst(EntityJoinLevelEvent ev) {
        Entity e = ev.getEntity();
        if (!(ev.getLevel() instanceof ServerLevel) || !e.entityTags().contains(TAG)) {
            return;
        }
        for (String t : e.entityTags()) {
            if (!t.startsWith("guhs_gk_")) {
                continue;
            }
            String rest = t.substring("guhs_gk_".length());
            int i = rest.lastIndexOf('_');
            if (i <= 0) {
                break;
            }
            Run r = RUNS.get(rest.substring(0, i));
            long n;
            try {
                n = Long.parseLong(rest.substring(i + 1));
            } catch (NumberFormatException x) {
                n = -1;
            }
            if (r == null || r.staat == Staat.SLAAPT || r.nonce != n) {
                ev.setCanceled(true);
            }
            return;
        }
        ev.setCanceled(true);   // (tagged but without a run tag: never ours to keep)
    }

    // ---- the gate ----------------------------------------------------------------------------------------------------------------

    static boolean iemandAchter(ServerLevel sl, Kerker k) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && k.achter(p.getX(), p.getY(), p.getZ())) {
                return true;
            }
        }
        return false;
    }

    static boolean iemandInPoort(ServerLevel sl, Kerker k) {
        for (ServerPlayer p : sl.players()) {
            for (int[] c : k.poort()) {
                double dx = p.getX() - (c[0] + 0.5), dz = p.getZ() - (c[2] + 0.5);
                if (dx * dx + dz * dz <= 2.2 * 2.2 && p.getY() > c[1] - 2 && p.getY() < c[3] + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    static void poort(ServerLevel sl, Kerker k, Run r, long nu) {
        if (k.poort().isEmpty()) {
            return;
        }
        boolean wil = r.staat == Staat.BEZIG;
        if (wil && !r.dicht && sl.isLoaded(k.bord()) && !iemandAchter(sl, k) && !iemandInPoort(sl, k)) {
            sluit(sl, k, r);
        } else if (!wil && r.dicht) {
            open(sl, r);
        }
        if (r.dicht) {
            DustParticleOptions roze = new DustParticleOptions(0xFF7FC8, 1.2f);
            for (int i = 0; i < 4; i++) {
                int[] c = k.poort().get(sl.getRandom().nextInt(k.poort().size()));
                double y = c[1] + sl.getRandom().nextDouble() * Math.max(1, c[3] - c[1]);
                sl.sendParticles(roze, c[0] + sl.getRandom().nextDouble(), y, c[2] + sl.getRandom().nextDouble(), 1, 0.1, 0.1, 0.1, 0);
            }
            for (ServerPlayer p : sl.players()) {
                for (int[] c : k.poort()) {
                    double dx = p.getX() - (c[0] + 0.5), dz = p.getZ() - (c[2] + 0.5);
                    if (dx * dx + dz * dz <= 1.6 * 1.6 && Math.abs(p.getY() - c[1]) < 3) {
                        if (nu - r.botsMelding.getOrDefault(p.getUUID(), -1000L) > 60) {
                            r.botsMelding.put(p.getUUID(), nu);
                            p.sendOverlayMessage(Component.translatable("vadsrijk.grotkerker.poort.botst").withStyle(ChatFormatting.LIGHT_PURPLE));
                        }
                        break;
                    }
                }
            }
        }
    }

    /** Shuts the gate: barriers in the AIR of the gate columns only (nothing else is ever replaced). */
    static void sluit(ServerLevel sl, Kerker k, Run r) {
        r.poortGezet.clear();
        for (int[] c : k.poort()) {
            for (int y = c[1]; y < c[3]; y++) {
                BlockPos p = new BlockPos(c[0], y, c[2]);
                if (sl.getBlockState(p).isAir()) {
                    sl.setBlock(p, Blocks.BARRIER.defaultBlockState(), 2);
                    r.poortGezet.add(p);
                }
            }
        }
        r.dicht = true;
        sl.playSound(null, k.bord(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.7f);
    }

    static void open(ServerLevel sl, Run r) {
        for (BlockPos p : r.poortGezet) {
            if (sl.getBlockState(p).is(Blocks.BARRIER)) {
                sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        r.poortGezet.clear();
        r.dicht = false;
    }

    /** Opens a gate that was left shut in the world (a crash mid-run): only our barriers in the gate columns go. */
    static void herstelPoort(ServerLevel sl, Kerker k, Run r) {
        if (r.dicht) {
            return;
        }
        for (int[] c : k.poort()) {
            for (int y = c[1]; y < c[3]; y++) {
                BlockPos p = new BlockPos(c[0], y, c[2]);
                if (sl.getBlockState(p).is(Blocks.BARRIER)) {
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    static void bord(ServerLevel sl, Kerker k, Run r, long nu) {
        if (k.bord() == null || nu % 20 != 0) {
            return;
        }
        if (r.staat == Staat.SLAAPT || !sl.isLoaded(k.bord())) {
            if (r.bord != null) {
                r.bord.discard();
                r.bord = null;
            }
            return;
        }
        Component regel = switch (r.staat) {
            case BEZIG -> r.dicht ? Component.translatable("vadsrijk.grotkerker.poort.nog", over(r)).withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("vadsrijk.grotkerker.poort.wacht").withStyle(ChatFormatting.GRAY);
            case BAAS -> Component.translatable("vadsrijk.grotkerker.poort.open").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("vadsrijk.dungeon.klaar.popup").withStyle(ChatFormatting.GREEN);   // W-GROT: no "next run in" (the door decides)
        };
        Component tekst = Component.empty().append(Component.translatable("vadsrijk.grotkerker.poort.dicht").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append("\n").append(regel);
        if (r.bord == null || r.bord.isRemoved()) {
            r.bord = Getallen.hologram(sl, k.bord().getX() + 0.5, k.bord().getY() + 2.4, k.bord().getZ() + 0.5, tekst);
        } else {
            Getallen.zetTekst(r.bord, tekst);
        }
    }

    // ---- dev commands --------------------------------------------------------------------------------------------------------

    static void naar(ServerPlayer p, BlockPos b) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
    }

    private static Kerker kerkerVan(CommandSourceStack src, String id) {
        Kerker k = Kerkers.van(id);
        if (k == null) {
            src.sendFailure(Component.literal("Onbekende grot (of zone 6 zit nog niet in deze build): " + id));
        }
        return k;
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal("grot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(
                Kerkers.alle().stream().map(Kerker::id), b);
        g.then(Commands.literal("run").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            StringBuilder s = new StringBuilder("Grotkerkers (reset " + tijd(RESET_NA) + " na een clear, " + tijd(LEEG_NA) + " na leeg):");
            for (Kerker k : Kerkers.alle()) {
                Run r = run(k.id());
                s.append("\n - z").append(k.zone()).append(" ").append(k.id()).append(": ").append(r.staat);
                if (r.staat == Staat.BEZIG) {
                    s.append(" (nog ").append(over(r)).append(" van ").append(r.plan.size()).append(", spelers ").append(r.spelers).append(r.dicht ? ", poort dicht)" : ", poort open)");
                } else if (r.staat == Staat.KLAAR && sl != null) {
                    s.append(" (reset over ").append(tijd(RESET_NA - (sl.getGameTime() - r.klaarOp))).append(")");
                }
            }
            if (c.getSource().getPlayer() != null) {
                for (Kerker k : Kerkers.alle()) {
                    s.append("\n   jouw clears ").append(k.id()).append(": ").append(clears(c.getSource().getPlayer(), k.id()));
                }
            }
            String t = s.toString();
            c.getSource().sendSuccess(() -> Component.literal(t), false);
            return 1;
        }).then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Kerker k = kerkerVan(c.getSource(), StringArgumentType.getString(c, "id"));
            if (k == null) {
                return 0;
            }
            // W-GROT: a solo start through the door (rules + cooldown skipped); the run starts when you walk into the cave
            boolean ok = DungeonRij.solo(p, k.id(), true);
            p.sendSystemMessage(Component.literal(ok ? "Solo naar binnen in " + k.id() + " (dev: geen wachttijd/regels). De run start zodra je in de grot staat."
                    : "Kon niet naar binnen (bezet door een andere groep?)").withStyle(ChatFormatting.GRAY));
            return ok ? 1 : 0;
        })));
        g.then(Commands.literal("reset").then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            Kerker k = kerkerVan(c.getSource(), StringArgumentType.getString(c, "id"));
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (k == null || sl == null) {
                return 0;
            }
            reset(sl, k, run(k.id()), "vadsrijk.grotkerker.reset");
            if (DungeonRij.instantie(k.id()) != null) {
                DungeonRij.sluit(c.getSource().getServer(), k.id(), "vadsrijk.dungeon.msg.gesloten");   // W-GROT: the group's cave closes too
            }
            c.getSource().sendSuccess(() -> Component.literal("Grotkerker " + k.id() + " gereset (dicht; start hem weer via de deur of /guhs vadsrijk grot run " + k.id() + ")"), false);
            return 1;
        })));
        g.then(Commands.literal("baas").then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            Kerker k = kerkerVan(c.getSource(), StringArgumentType.getString(c, "id"));
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (k == null || sl == null) {
                return 0;
            }
            int n = devBaas(sl, k, c.getSource().getPlayer());
            c.getSource().sendSuccess(() -> Component.literal(n > 0 ? "De grotbaas van " + k.id() + " is wakker (poort open). Ga erheen: /guhs vadsrijk grot tp "
                    + k.id() + " elite" : "Kon de baas niet spawnen (is de grot geladen? sta dichterbij)"), false);
            return n;
        })));
        g.then(Commands.literal("kist").executes(c -> devKist(c.getSource(), null))
                .then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> devKist(c.getSource(), StringArgumentType.getString(c, "id")))));
        event.getDispatcher().register(VadsrijkCommando.wortel(g));
    }

    /** Dev: skip the packs (the run starts if needed, every pack creature away), open the gate, the boss wakes up. */
    public static int devBaas(ServerLevel sl, Kerker k, @Nullable ServerPlayer p) {
        if (p != null) {
            DungeonRij.devToegang(p, k.id());   // W-GROT: the dev is the cave's group (else the box would put them outside)
        }
        Run r = run(k.id());
        if (r.staat == Staat.KLAAR) {
            reset(sl, k, r, null);
        }
        if (r.staat == Staat.SLAAPT) {
            start(sl, k, r, p == null ? List.of() : List.of(p));
            DungeonRij.gestart(k.id());
        }
        for (UUID u : r.levend.values()) {
            Entity e = sl.getEntity(u);
            if (e != null) {
                e.discard();
            }
        }
        r.levend.clear();
        r.verslagen.addAll(r.plan.keySet());
        if (r.staat == Staat.BEZIG) {
            naarBaas(sl, k, r, inGrot(sl, k));
        }
        if (baas(sl, k, r) == null) {
            spawnBaas(sl, k, r, Math.max(1, inGrot(sl, k).size()));
        }
        if (r.dicht) {
            open(sl, r);
        }
        return baas(sl, k, r) != null ? 1 : 0;
    }

    /** Dev (U-GROTTEN's {@code grot elite <id>}): the run's mid-elite now (the run starts if the cave sleeps). */
    @Nullable
    public static VadsrijkMob devElite(ServerLevel sl, String id) {
        Kerker k = Kerkers.van(id);
        if (k == null) {
            return null;
        }
        Run r = run(id);
        if (r.staat != Staat.BEZIG) {
            reset(sl, k, r, null);
            start(sl, k, r, List.of());
        }
        r.verslagen.remove("elite");
        r.levend.remove("elite");
        spawnOntbrekend(sl, k, r, sl.getGameTime());
        return Dubbel.zoek(sl, sleutel(k, "elite")) instanceof VadsrijkMob m ? m : null;
    }

    private static int devKist(CommandSourceStack src, @Nullable String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Kerker k = id != null ? kerkerVan(src, id) : Kerkers.op(p.getX(), p.getY(), p.getZ());
        if (k == null) {
            if (id == null) {
                src.sendFailure(Component.literal("Sta in een grot, of geef een id: /guhs vadsrijk grot kist <id>"));
            }
            return 0;
        }
        BlockPos plek = p.blockPosition().relative(p.getDirection(), 2);
        BaasKist.plaats((ServerLevel) p.level(), p, plek, KerkerLoot.kist(k.id(), k.baasLv(), p, true));
        src.sendSuccess(() -> Component.literal("Eindkist van " + k.id() + " voor je neergezet (met de zeldzame rollen: huisdier + Pluimguh-kleur)"), false);
        return 1;
    }
}
