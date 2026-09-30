package nl.juiced.guhs.feature.baltoslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the sled (3.0, balto-slee), on a made-up route over a snowy test field (the real Nomguh route is balto's):
 * the track and the route tag (the same on every side), the medicine ride's moments in order with its pauses (verder, the
 * storm clearing), a reset (avalanche, ice bridge) that never counts twice and never loses against an old report of the
 * rider's game, the reports checked against the dogs' speed, resting at a vuurkorf, the time limit of the way back, the
 * sledesprint (score on the board, sledebelletjes, beating Steele-Mika), Steele-Mika's role and shop, and your own sneeuwslee
 * that only runs on snow.
 */
public class BaltoSleeGameTests {
    private static final String BAAN = "baltoslee_test_baan";
    private static final String VELD = "baltoslee_test_veld";
    private static final String BATCH = "baltoslee";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            SleeRit.spelerWeg(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * The test route over the field (12 points along x, every 4 blocks, at helper y 2): a rest point at 3, an ice bridge 4-6
     * (narrow), the dieptepunt at 7, an avalanche 8-9 from the left; the hospital next to the start.
     */
    static NomguhRoute route(GameTestHelper helper) {
        List<Vec3> punten = new ArrayList<>();
        List<Double> breedte = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            BlockPos p = helper.absolutePos(new BlockPos(2 + i * 4, 2, 6));
            punten.add(new Vec3(p.getX(), p.getY(), p.getZ()));
            breedte.add(i >= 4 && i <= 6 ? 1.3 : 2.5);
        }
        List<Vec3> terug = new ArrayList<>(punten);
        java.util.Collections.reverse(terug);
        BlockPos start = BlockPos.containing(punten.get(0));
        return new NomguhRoute(start, List.copyOf(punten), List.copyOf(terug), List.copyOf(breedte), start.offset(0, 0, -3), start.offset(0, 0, 3),
                BlockPos.containing(punten.get(11)), List.of(3), List.of(new int[]{4, 6}), List.of(new NomguhRoute.Lawine(8, 9, true)), 7, 44,
                Map.of("makkelijk", 2400, "medium", 1800, "lastig", 1400));
    }

    /** Rides until the condition holds (or the ride ends / n ticks). */
    private static void rijd(SleeRit rit, ServerLevel level, int n, java.util.function.BooleanSupplier klaar) {
        for (int i = 0; i < n && rit.bezig() && !klaar.getAsBoolean(); i++) {
            rit.tick(level);
        }
    }

    // =================================================================================================================
    // the track
    // =================================================================================================================

    /** The track is smooth and follows its points; the route tag gives every side exactly the same track and zones. */
    @GuhTest(template = BAAN, batch = BATCH)
    public static void baltosleeBaanEnRouteTag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RitRoute r = RitRoute.maak(level, route(helper));
        RitRoute kopie = RitRoute.lees(r.tag().copy());
        for (int been = 0; been < 2; been++) {
            SleeBaan b = r.baan(been), k = kopie.baan(been);
            helper.assertTrue(Math.abs(b.lengte - 44) < 1.0, "a leg of about 44 blocks: " + b.lengte);
            Vec3 prev = null;
            for (double s = 0; s <= b.lengte; s += 0.25) {
                Vec3 p = b.op(s, 1.0);
                helper.assertTrue(p.equals(k.op(s, 1.0)), "the same spot from the tag at " + s);
                helper.assertTrue(prev == null || p.distanceTo(prev) < 0.4, "no jumps at " + s);
                helper.assertTrue(Math.abs(b.richting(s).length() - 1) < 1e-6, "a unit direction at " + s);
                prev = p;
            }
            helper.assertTrue(r.zones(been).size() == (been == 0 ? 3 : 4) && r.zones(been).equals(kopie.zones(been)), "the zones of leg " + been + ": " + r.zones(been));
        }
        // on the snow field: the track lies on the snow (helper y 2 = the air above the snow blocks)
        double y = r.baan(0).midden(10).y;
        helper.assertTrue(Math.abs(y - helper.absolutePos(new BlockPos(0, 2, 0)).getY()) < 0.01, "the track lies on the snow: " + y);
        // the avalanche comes from the left on the way there and (the same slope) from the right on the way back
        helper.assertTrue(r.zones(0, RitRoute.Soort.LAWINE).get(0).kant() == -1 && r.zones(1, RitRoute.Soort.LAWINE).get(0).kant() == 1,
                "the avalanche's side flips on the way back");
        helper.assertTrue(r.zones(1, RitRoute.Soort.DIEPTEPUNT).size() == 1 && r.zones(0, RitRoute.Soort.DIEPTEPUNT).isEmpty(), "the dieptepunt is on the way back");
        // gusts are part of the track: the same everywhere for the same ride
        int seed = 12345;
        int vlagen = 0;
        for (double s = 0; s < 400; s += 0.5) {
            double g = SleeRijden.windvlaag(seed, 0, s, 400);
            helper.assertTrue(g == SleeRijden.windvlaag(seed, 0, s, 400) && Math.abs(g) <= 1, "a gust is fixed");
            vlagen += g != 0 ? 1 : 0;
        }
        helper.assertTrue(vlagen > 20, "gusts on a long track: " + vlagen);
        helper.succeed();
    }

    // =================================================================================================================
    // the medicine ride
    // =================================================================================================================

    /** START, BERGHUT (waits for verder), DIEPTEPUNT (waits; the storm clears), AANKOMST: in this order, each once. */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleeTochtMomentenOpVolgorde(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        List<SleeTocht.Moment> momenten = new ArrayList<>();
        SleeTocht.Luisteraar l = (pl, m) -> {
            if (pl == p) {
                momenten.add(m);
            }
        };
        SleeTocht.luister(l);
        try {
            helper.assertTrue(SleeTocht.startMet(p, route(helper), null), "the ride starts");
            helper.assertFalse(SleeTocht.startMet(p, route(helper), null), "not twice at once");
            SleeRit rit = SleeRit.van(p);
            helper.assertTrue(rit != null && SleeTocht.bezig(p) && p.getVehicle() instanceof SleeEntity, "riding the sled");
            helper.assertTrue(SleeRit.GAME_TOCHT.equals(Minigames.playing(p)) && Minigames.invulnerable(p), "a protected minigame");
            rit.autopiloot = 1;
            rit.slaAftellenOver(level);
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            helper.assertTrue(rit.pauze() == SleeEntity.BERGHUT && rit.kist(), "at the berghut, the medicine chest on the sled");
            for (int i = 0; i < 100; i++) {
                rit.tick(level);
            }
            helper.assertTrue(rit.fase() == SleeEntity.PAUZE && rit.been() == 0, "it waits for balto");
            SleeTocht.verder(p);
            helper.assertTrue(rit.fase() == SleeEntity.RIJDT && rit.been() == 1 && rit.stand().s == 0, "back we go");
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            helper.assertTrue(rit.pauze() == SleeEntity.DIEPTEPUNT, "the dieptepunt: " + rit.pauze());
            for (int i = 0; i < 60; i++) {
                rit.tick(level);
            }
            float zwaar = rit.storm();
            helper.assertTrue(zwaar > SleeRit.TOCHT_STORM_TERUG - 0.05f, "the storm is at its worst: " + zwaar);
            helper.assertTrue(rit.tijdTerug() < 200, "the clock stops while it waits: " + rit.tijdTerug());
            SleeTocht.stormKlaartOp(p);
            SleeTocht.verder(p);
            rijd(rit, level, 400, () -> false);
            helper.assertFalse(rit.bezig() || SleeTocht.bezig(p), "the ride is over");
            helper.assertTrue(rit.helder() && rit.storm() < zwaar, "the storm cleared");
            helper.assertTrue(momenten.equals(List.of(SleeTocht.Moment.START, SleeTocht.Moment.BERGHUT, SleeTocht.Moment.DIEPTEPUNT,
                    SleeTocht.Moment.AANKOMST)), "the moments in order: " + momenten);
            helper.assertTrue(p.getVehicle() == null && p.blockPosition().closerThan(BlockPos.containing(rit.route.ziekenhuis), 2), "off at the hospital");
            helper.assertTrue(advancement(p, "balto_slee_tocht") && advancement(p, "verhalen/balto_slee_tocht"), "the advancement");
            helper.assertTrue(level.getEntitiesOfClass(SleeEntity.class, p.getBoundingBox().inflate(80)).stream().noneMatch(Entity::isAlive), "the sled is gone");
        } finally {
            SleeTocht.vergeet(l);
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Without balto the pauses go on by themselves (and the storm clears at the dieptepunt). */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleePauzeGaatVanzelfVerder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        try {
            SleeTocht.startMet(p, route(helper), null);
            SleeRit rit = SleeRit.van(p);
            rit.autopiloot = 1;
            rit.slaAftellenOver(level);
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            for (int i = 0; i <= SleeRit.PAUZE_MAX; i++) {
                rit.tick(level);
            }
            helper.assertTrue(rit.been() == 1 && rit.fase() == SleeEntity.RIJDT, "on by itself after the berghut");
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            for (int i = 0; i <= SleeRit.PAUZE_MAX; i++) {
                rit.tick(level);
            }
            helper.assertTrue(rit.fase() == SleeEntity.RIJDT && rit.helder(), "on by itself after the dieptepunt, the storm clears");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Too late on the way back: TE_LAAT, off at the stable. "Njeg, nog een keer!" */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleeTeLaat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        List<SleeTocht.Moment> momenten = new ArrayList<>();
        SleeTocht.Luisteraar l = (pl, m) -> {
            if (pl == p) {
                momenten.add(m);
            }
        };
        SleeTocht.luister(l);
        try {
            SleeTocht.startMet(p, route(helper), null);
            SleeRit rit = SleeRit.van(p);
            rit.slaAftellenOver(level);
            rit.autopiloot = 1;
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            rit.zetLimiet(30);
            SleeTocht.verder(p);
            rit.autopiloot = 0;                                   // (trotting: too slow for 30 ticks)
            rijd(rit, level, 400, () -> false);
            helper.assertTrue(momenten.contains(SleeTocht.Moment.TE_LAAT) && !momenten.contains(SleeTocht.Moment.AANKOMST), "too late: " + momenten);
            helper.assertTrue(!rit.bezig() && p.blockPosition().closerThan(BlockPos.containing(rit.route.stal), 2), "back at the stable");
        } finally {
            SleeTocht.vergeet(l);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // obstacles and the rider's reports
    // =================================================================================================================

    /**
     * Into the avalanche: buried once, the sled goes back before it (a new generation). An old report of the rider's game
     * (the old generation, far past the avalanche) changes nothing; the next time past it (dodging) counts as dodged.
     */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleeResetNooitDubbel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        try {
            SleeRit.data(p).putBoolean("Vrij", true);
            SleeRit rit = SleeRit.start(p, route(helper), SleeRit.Modus.SPRINT, Niveau.MAKKELIJK, null, null);
            helper.assertTrue(rit != null, "the race starts");
            rit.slaAftellenOver(level);
            rit.autopiloot = 2;                                   // (steers into the avalanche, off the ice bridge)
            int gen0 = rit.gen(level);
            rijd(rit, level, 400, () -> rit.plof() > 0);
            helper.assertTrue(rit.plof() == 1 && rit.gen(level) == gen0 + 1, "fell off the ice bridge once");
            RitRoute.Zone brug = rit.route.zones(0, RitRoute.Soort.IJSBRUG).get(0);
            helper.assertTrue(rit.stand().s < brug.s0() && rit.fase() == SleeEntity.VAST, "back before the bridge, digging out");
            rit.graafUit();
            rit.autopiloot = 1;
            rijd(rit, level, 200, () -> rit.stand().s > brug.s1() + 1);
            rit.autopiloot = 2;
            rijd(rit, level, 400, () -> rit.bedolven() > 0);
            helper.assertTrue(rit.bedolven() == 1 && rit.gen(level) == gen0 + 2, "buried once: " + rit.bedolven());
            RitRoute.Zone lawine = rit.route.zones(0, RitRoute.Soort.LAWINE).get(0);
            double terug = rit.stand().s;
            helper.assertTrue(terug < lawine.s0(), "back before the avalanche: " + terug);
            // an old report of the rider's game: the old generation, past the avalanche - ignored
            rit.meld(p, gen0 + 1, 0, lawine.s1() + 5, 0, 0.4, 0);
            rit.graafUit();
            rit.tick(level);
            rit.meld(p, gen0 + 1, 0, lawine.s1() + 5, 0, 0.4, 0);
            helper.assertTrue(rit.stand().s < lawine.s0() + 1 && rit.bedolven() == 1, "an old report changes nothing: " + rit.stand().s);
            // a report of the current generation can't jump ahead either
            double voor = rit.stand().s;
            rit.meld(p, gen0 + 2, 0, voor + 40, 0, 0.4, 0);
            helper.assertTrue(rit.stand().s <= voor + SleeRijden.TOP * 1.25 * 10 + 0.3 + 1e-6, "no faster than the dogs: " + (rit.stand().s - voor));
            rit.vergeetClient();                                  // (a test: the server rides by itself again)
            rit.autopiloot = 1;
            rijd(rit, level, 400, () -> rit.ontweken() > 0);
            helper.assertTrue(rit.ontweken() == 1 && rit.bedolven() == 1, "dodged the second time, buried still once");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Stop at a vuurkorf: the dogs rest and warm up (once per rest point per leg). */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleeRustBijVuurkorf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        try {
            SleeTocht.startMet(p, route(helper), null);
            SleeRit rit = SleeRit.van(p);
            rit.slaAftellenOver(level);
            rit.autopiloot = 1;
            rit.autoRust = true;
            rijd(rit, level, 400, () -> rit.fase() == SleeEntity.PAUZE);
            helper.assertTrue(rit.pauze() == SleeEntity.RUST && rit.gerust() == 1, "resting at the vuurkorf: " + rit.pauze());
            float koud = rit.warmte();
            for (int i = 0; i < SleeRit.RUST_TICKS + 2; i++) {
                rit.tick(level);
            }
            helper.assertTrue(rit.fase() == SleeEntity.RIJDT && rit.warmte() > koud && rit.warmte() >= 99f, "warm again: " + rit.warmte());
            helper.assertTrue(advancement(p, "balto_slee_rustpunt"), "the advancement");
            rijd(rit, level, 60, () -> rit.fase() == SleeEntity.PAUZE && rit.pauze() == SleeEntity.RUST);
            helper.assertTrue(rit.gerust() == 1, "that rest point only once on this leg");
            // cold dogs are slower
            helper.assertTrue(SleeRijden.warmteFactor(5) < SleeRijden.warmteFactor(25) && SleeRijden.warmteFactor(25) < SleeRijden.warmteFactor(80),
                    "cold dogs run slower");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the sledesprint
    // =================================================================================================================

    /** A race on makkelijk flat out: the time on the board, sledebelletjes, Steele-Mika beaten, his sled gone after. */
    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 200)
    public static void baltosleeSprintScore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        try {
            SleeRit rit = SleeRit.start(p, route(helper), SleeRit.Modus.SPRINT, Niveau.MAKKELIJK, null, null);
            helper.assertTrue(rit != null && SleeRit.GAME_SPRINT.equals(Minigames.playing(p)), "racing");
            long sleden = level.getEntitiesOfClass(SleeEntity.class, p.getBoundingBox().inflate(40)).size();
            helper.assertTrue(sleden == 2, "your sled and Steele-Mika's: " + sleden);
            rit.slaAftellenOver(level);
            rit.autopiloot = 1;
            rijd(rit, level, 600, () -> false);
            helper.assertFalse(rit.bezig(), "finished");
            int tijd = rit.rijTijd();
            List<Scorebord.Entry> top = Scorebord.top(p.level().getServer(), SleeRit.bord(Niveau.MAKKELIJK));
            helper.assertTrue(top.stream().anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == tijd), "the time on the board: " + top);
            helper.assertTrue(SleeRit.best(p, Niveau.MAKKELIJK) == tijd, "your own best");
            int bellen = GuhQuests.count(p, BaltoSleeFeature.SLEDEBELLETJE.get());
            helper.assertTrue(bellen == SleeRit.MUNTEN_BASIS + SleeRit.MUNTEN_STEELE + SleeRit.MUNTEN_EERSTE, "sledebelletjes: " + bellen);
            helper.assertTrue(advancement(p, "balto_slee_sprint") && advancement(p, "balto_slee_steele") && advancement(p, "balto_slee_sprint_makkelijk"),
                    "the advancements");
            helper.assertTrue(level.getEntitiesOfClass(SleeEntity.class, p.getBoundingBox().inflate(80)).stream().noneMatch(Entity::isAlive), "the sleds are gone");
            // a second, slower race: no record, no first-time bonus
            SleeRit rit2 = SleeRit.start(p, route(helper), SleeRit.Modus.SPRINT, Niveau.MAKKELIJK, null, null);
            rit2.slaAftellenOver(level);
            rijd(rit2, level, 2000, () -> false);
            helper.assertTrue(SleeRit.best(p, Niveau.MAKKELIJK) == tijd && rit2.rijTijd() > tijd, "the best stays");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Steele-Mika at the start line (plek "sledesprint") is ours: the race role, the deco shop for sledebelletjes. */
    @GuhTest(template = BAAN, batch = BATCH)
    public static void baltosleeSteeleEnWinkel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.STEELE_MIKA);
        npc.roleData.putString(NpcRollen.PLEK, SteeleSprint.PLEK);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 3));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(npc);
        try {
            helper.assertTrue(NpcRollen.van(npc) instanceof SteeleSprint, "Steele-Mika of the sledesprint: " + NpcRollen.van(npc));
            MerchantOffers offers = NpcRollen.van(npc).offers(npc);
            helper.assertTrue(offers != null && offers.size() == SteeleSprint.aanbod().size(), "the shop");
            offers.forEach(o -> helper.assertTrue(o.getCostA().is(BaltoSleeFeature.SLEDEBELLETJE.get()) && o.getCostA().getCount() > 0
                    && o.getResult().getItem() instanceof net.minecraft.world.item.BlockItem, "deco for sledebelletjes: " + o.getResult()));
            ServerPlayer p = speler(helper);
            try {
                helper.assertFalse(SteeleSprint.magRacen(p), "not before the story");
                SleeRit.data(p).putBoolean("Vrij", true);
                helper.assertTrue(SteeleSprint.magRacen(p), "after it");
                npc.roleData.putLong(SteeleSprint.ANKER, helper.absolutePos(new BlockPos(2, 2, 6)).asLong());
                SleeRit rit = SteeleSprint.start(npc, p, Niveau.LASTIG);
                helper.assertTrue(rit != null && rit.niveau == Niveau.LASTIG && rit.modus == SleeRit.Modus.SPRINT, "the race from Steele-Mika");
            } finally {
                weg(helper, p);
            }
        } finally {
            npc.discard();
        }
        helper.succeed();
    }

    // =================================================================================================================
    // your own sneeuwslee
    // =================================================================================================================

    /** Your own sneeuwslee runs on snow and hardly moves on grass. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void baltosleeEigenSleeAlleenOpSneeuw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        double[] afstand = new double[2];
        for (int k = 0; k < 2; k++) {
            SneeuwsleeEntity s = BaltoSleeFeature.SNEEUWSLEE_ENTITY.get().create(level, EntitySpawnReason.TRIGGERED);
            BlockPos at = helper.absolutePos(new BlockPos(2, 2, k == 0 ? 3 : 10));
            s.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90f, 0);      // (facing +x, along its strip)
            level.addFreshEntity(s);
            Vec3 start = s.position();
            for (int i = 0; i < 40; i++) {
                s.rijd(new SleeRijden.Invoer(1, 0));
            }
            afstand[k] = s.position().distanceTo(start);
            helper.assertTrue(k == 0 ? s.opSneeuw() : !s.opSneeuw(), "on snow: " + (k == 0));
            s.discard();
        }
        helper.assertTrue(afstand[0] > 8, "on snow it runs: " + afstand[0]);
        helper.assertTrue(afstand[1] < 2.5, "on grass it hardly moves: " + afstand[1]);
        helper.succeed();
    }

    /** 3.0 crash fix: the mod's own entity-event ids never collide with a vanilla one (63 = Sniffer cast in the client). */
    @GuhTest(template = BAAN, batch = BATCH)
    public static void baltosleeEventIdsBotsenNiet(GameTestHelper helper) {
        var botsingen = nl.juiced.guhs.entity.EntiteitEvents.botsingen();
        helper.assertTrue(botsingen.isEmpty(), "custom entity events collide with vanilla: " + botsingen);
        for (int id : nl.juiced.guhs.entity.EntiteitEvents.CLIENT_LISTENER) {
            helper.assertFalse(id >= SleeEntity.EV_BEDOLVEN && id <= SleeEntity.EV_KEER, "sled event range holds vanilla id " + id);
        }
        helper.assertTrue(nl.juiced.guhs.entity.EntiteitEvents.gereserveerd(SleeEntity.class).contains(63), "63 is reserved");
        // a collision is found (the old 3.0 ids)
        helper.assertTrue(nl.juiced.guhs.entity.EntiteitEvents.gereserveerd(SleeEntity.class).contains(63)
                && nl.juiced.guhs.entity.EntiteitEvents.gereserveerd(nl.juiced.guhs.entity.GuhEntity.class).containsAll(java.util.List.of(6, 7, 18, 20, 60)),
                "superclass ids are reserved");
        helper.succeed();
    }
}
