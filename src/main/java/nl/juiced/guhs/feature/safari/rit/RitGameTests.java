package nl.juiced.guhs.feature.safari.rit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.momenten.MomentSchema;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Op Safari! step 2 (S3) and round 3 (J2): the truck and gondola timetables, the truck seats, the safaripoort (shuts by
 * itself, opens for a truck, never lets an animal through), the ride data and the day plan of the moments.
 */
public class RitGameTests {
    private static final String BATCH = "safari";

    /** A truck drives, stops at every stop for HALTE_WACHT ticks, never jumps back, and the trucks are spread out. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void truckHaltes(GameTestHelper helper) {
        double lengte = 1000;
        List<Double> haltes = List.of(0.0, 250.0, 600.0);
        long ronde = RitSchema.truckRonde(lengte, haltes.size());
        helper.assertTrue(ronde == Math.round(1000 / RitSchema.TRUCK_SNELHEID) + 3L * RitSchema.HALTE_WACHT, "one loop = driving + 3 stops");
        RitSchema.Stand s0 = RitSchema.truck(0, 0, 1, lengte, haltes);
        helper.assertTrue(s0.staatStil() && s0.halte() == 0 && s0.wachtNog() == RitSchema.HALTE_WACHT, "time 0: standing at the first stop");
        RitSchema.Stand s1 = RitSchema.truck(RitSchema.HALTE_WACHT + 10, 0, 1, lengte, haltes);
        helper.assertTrue(!s1.staatStil() && Math.abs(s1.afstand() - 10 * RitSchema.TRUCK_SNELHEID) < 1e-6, "then it drives on");
        long aanTweede = RitSchema.HALTE_WACHT + Math.round(250 / RitSchema.TRUCK_SNELHEID) + 5;
        RitSchema.Stand s2 = RitSchema.truck(aanTweede, 0, 1, lengte, haltes);
        helper.assertTrue(s2.staatStil() && s2.halte() == 1 && Math.abs(s2.afstand() - 250) < 1e-6, "it stands at the second stop");
        double vorige = -1;
        int stops = 0;
        boolean stil = false;
        for (long t = 0; t < ronde; t++) {
            RitSchema.Stand s = RitSchema.truck(t, 0, 1, lengte, haltes);
            helper.assertTrue(s.afstand() >= vorige - 1e-9, "never backwards within a loop (t=" + t + ")");
            vorige = s.afstand();
            if (s.staatStil() && !stil) {
                stops++;
            }
            stil = s.staatStil();
        }
        helper.assertTrue(stops == 3, "every stop once per loop (" + stops + ")");
        int n = RitSchema.trucks(lengte, haltes.size());
        helper.assertTrue(n == Math.max(1, Math.round((double) ronde / RitSchema.TRUCK_ELKE)), "about one truck a minute");
        helper.assertTrue(Math.abs(RitSchema.truck(7000, 1, 2, lengte, haltes).afstand()
                - RitSchema.truck(7000 + ronde / 2, 0, 2, lengte, haltes).afstand()) < 1e-6, "truck 1 = truck 0 half a loop later");
        helper.succeed();
    }

    /** The gondolas go round the loop, stand GONDEL_WACHT ticks in both stations, and are spread evenly along it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void gondels(GameTestHelper helper) {
        double lengte = 400;
        List<Double> stations = List.of(100.0, 300.0);
        long ronde = RitSchema.ronde(lengte, 2, RitSchema.GONDEL_SNELHEID, RitSchema.GONDEL_WACHT);
        int stil = 0;
        boolean was = false;
        double vorige = -1;
        for (long t = 0; t < ronde; t++) {
            RitSchema.Stand s = RitSchema.gondel(t, 0, lengte, stations);
            helper.assertTrue(s.afstand() >= vorige - 1e-9, "never backwards within a loop (t=" + t + ")");
            vorige = s.afstand();
            if (s.staatStil()) {
                helper.assertTrue(Math.abs(s.afstand() - stations.get(s.halte())) < 1e-9, "it stands in a station");
                stil++;
            }
            if (s.staatStil() && !was) {
                helper.assertTrue(s.wachtNog() == RitSchema.GONDEL_WACHT, "a full stop in the station");
            }
            was = s.staatStil();
        }
        helper.assertTrue(stil == 2 * RitSchema.GONDEL_WACHT, "about 5 s in each of the two stations (" + stil + ")");
        long t0 = 777;
        helper.assertTrue(Math.abs(RitSchema.gondel(t0, 1, lengte, stations).afstand()
                - RitSchema.gondel(t0 + ronde / RitSchema.GONDELS, 0, lengte, stations).afstand()) < 0.5, "gondola 1 = gondola 0 a sixth of a loop later");
        helper.succeed();
    }

    /** The truck seats: players from the front, the visitors from the back, never the same seat, two always free for players. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void truckStoelen(GameTestHelper helper) {
        helper.assertTrue(TruckEntity.ZIT.length == 1 + TruckEntity.STOELEN, "the guide + three rows of two");
        for (int basis = 2; basis <= 4; basis++) {
            for (int spelers = 0; spelers <= TruckEntity.STOELEN; spelers++) {
                int bezoekers = TruckEntity.bezoekers(basis, spelers);
                List<Integer> hun = TruckEntity.bezoekerStoelen(spelers, bezoekers);
                Set<Integer> bezet = new HashSet<>();
                for (int i = 0; i < spelers; i++) {
                    int st = TruckEntity.spelerStoel(i);
                    helper.assertTrue(st >= 1 && st <= TruckEntity.STOELEN && bezet.add(st), "player " + i + " has a seat of their own");
                }
                for (int st : hun) {
                    helper.assertTrue(st >= 1 && st <= TruckEntity.STOELEN && bezet.add(st), "a visitor never sits in a player seat");
                }
                helper.assertTrue(hun.size() == bezoekers, "every visitor has a seat");
                if (spelers <= TruckEntity.STOELEN - TruckEntity.VRIJ) {
                    helper.assertTrue(TruckEntity.STOELEN - bezet.size() >= TruckEntity.VRIJ,
                            "two seats stay free for players (" + basis + " visitors, " + spelers + " players)");
                }
            }
        }
        helper.assertTrue(TruckEntity.spelerStoel(0) == 1, "the first player sits in front, behind the guide");
        helper.succeed();
    }

    // --- the safaripoort -----------------------------------------------------------------------------------------------------

    private static final BlockPos POORT = new BlockPos(6, 1, 8);

    private static SafariPoortBlockEntity poort(GameTestHelper helper) {
        BlockPos hoek = helper.absolutePos(POORT);
        SafariPoortBlock.zet(helper.getLevel(), hoek, Direction.Axis.X);
        helper.assertTrue(helper.getLevel().getBlockState(SafariPoortBlock.deel(hoek, Direction.Axis.X, 3, 5)).is(SafariRitten.POORT.get()),
                "all 4 x 6 parts are there");
        SafariPoortBlockEntity be = (SafariPoortBlockEntity) helper.getLevel().getBlockEntity(hoek);
        helper.assertTrue(be != null && !be.open(), "a shut gate with its block entity in the corner");
        return be;
    }

    /** Opened by hand, the gate shuts itself after OPEN_MAX ticks. */
    @GuhTest(template = "px_test_16", batch = BATCH, timeoutTicks = 300)
    public static void poortSluitVanzelf(GameTestHelper helper) {
        SafariPoortBlockEntity be = poort(helper);
        be.openMetDeHand();
        helper.assertTrue(be.open(), "a click opens it");
        helper.runAfterDelay(SafariPoortBlockEntity.OPEN_MAX - 20, () -> {
            helper.assertTrue(be.open(), "still open after " + (SafariPoortBlockEntity.OPEN_MAX - 20) + " ticks");
            helper.succeedWhen(() -> helper.assertTrue(!be.open(), "shut again after about 10 s"));
        });
    }

    /** A truck coming towards the gate opens it in time; when it has gone through and on, the gate shuts again. */
    // (its own batch: in the big shared safari batch the gate sometimes never got its check in 200 ticks)
    @GuhTest(template = "px_test_16", batch = BATCH + "_poorttruck", timeoutTicks = 200)
    public static void poortTruck(GameTestHelper helper) {
        SafariPoortBlockEntity be = poort(helper);
        Vec3 m = be.midden();
        TruckEntity truck = SafariRitten.TRUCK.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(truck != null, "a truck");
        truck.zetVast();
        truck.snapTo(m.x, m.y, m.z - 15, 0, 0);       // yaw 0: driving towards +z, the gate 15 blocks ahead
        helper.getLevel().addFreshEntity(truck);
        helper.assertTrue(SafariPoortBlockEntity.truckWilDoor(truck.position(), 0, m), "it wants through");
        helper.assertFalse(SafariPoortBlockEntity.truckWilDoor(truck.position(), 180, m), "not when it drives away");
        // (polled, not a fixed delay: in a busy batch the gate's first check may fall a few ticks later)
        java.util.concurrent.atomic.AtomicBoolean doorgereden = new java.util.concurrent.atomic.AtomicBoolean();
        helper.succeedWhen(() -> {
            if (!doorgereden.get()) {
                helper.assertTrue(be.open(), "open before the truck gets there");
                doorgereden.set(true);
                truck.snapTo(m.x, m.y, m.z + 12, 0, 0);    // through and on: 12 blocks past it, driving away
            }
            helper.assertTrue(!be.open(), "shut behind the truck");
            truck.discard();
        });
    }

    /** The hard rule: through an open gate players and guhs may pass, a park animal or a race ostrich never. */
    @GuhTest(template = "px_test_16", batch = BATCH, timeoutTicks = 40)
    public static void poortDierTegen(GameTestHelper helper) {
        SafariPoortBlockEntity be = poort(helper);
        be.openMetDeHand();
        ServerLevel level = helper.getLevel();
        BlockPos p = SafariPoortBlock.deel(be.getBlockPos(), be.as(), 1, 1);
        BlockState st = level.getBlockState(p);
        helper.assertTrue(st.getValue(SafariPoortBlock.OPEN), "the part is open");
        Entity dier = SafariFeature.DIER.get().create(level, EntitySpawnReason.EVENT);
        Entity struis = SafariRitten.STRUISVOGEL.get().create(level, EntitySpawnReason.EVENT);
        Entity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.EVENT);
        Player speler = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(st.getCollisionShape(level, p, CollisionContext.of(dier)).isEmpty(), "an animal bumps into it");
        helper.assertFalse(st.getCollisionShape(level, p, CollisionContext.of(struis)).isEmpty(), "an ostrich too");
        helper.assertTrue(st.getCollisionShape(level, p, CollisionContext.of(guh)).isEmpty(), "a guh walks through");
        helper.assertTrue(st.getCollisionShape(level, p, CollisionContext.of(speler)).isEmpty(), "a player walks through");
        helper.assertFalse(st.getCollisionShape(level, p, CollisionContext.empty()).isEmpty(), "anything else: a wall");
        helper.succeed();
    }

    /** The day plan: a feeding time per zone, two talks in zones (not the village) that change per day, night safari 1 night in 3. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void momentenSchema(GameTestHelper helper) {
        List<MomentSchema.Moment> dag0 = MomentSchema.dag(0), dag1 = MomentSchema.dag(1);
        long voeder = dag0.stream().filter(m -> m.soort() == MomentSchema.Soort.VOEDERTIJD).count();
        helper.assertTrue(voeder == 9, "nine feeding times (" + voeder + ")");
        helper.assertTrue(dag0.stream().anyMatch(m -> m.soort() == MomentSchema.Soort.NACHTSAFARI), "day 0 has a night safari");
        helper.assertTrue(dag1.stream().noneMatch(m -> m.soort() == MomentSchema.Soort.NACHTSAFARI), "day 1 not");
        for (MomentSchema.Moment m : dag0) {
            if (m.soort() == MomentSchema.Soort.PRAATJE) {
                helper.assertTrue(!m.zone().equals("entree"), "no talk in the village");
            }
            if (m.soort() == MomentSchema.Soort.VOEDERTIJD) {
                helper.assertTrue(m.van() >= MomentSchema.MELDING && m.tot() < 13000, "feeding by daylight with a notice before (" + m.zone() + ")");
            }
        }
        helper.assertFalse(MomentSchema.praatjeZone(0, 0).equals(MomentSchema.praatjeZone(1, 0)), "the talk moves to another zone the next day");
        for (int i = 1; i < dag0.size(); i++) {
            helper.assertTrue(dag0.get(i - 1).van() <= dag0.get(i).van(), "in time order");
        }
        helper.assertTrue(MomentSchema.nachtsafari(0, 15000) && !MomentSchema.nachtsafari(0, 12000) && !MomentSchema.nachtsafari(1, 15000),
                "night safari hours");
        helper.assertTrue("07:00".equals(MomentSchema.klok(1000)), "1000 ticks = 07:00 (" + MomentSchema.klok(1000) + ")");
        String a = MomentSchema.jarig(100, List.of("zebra", "leeuw", "olifant"));
        helper.assertTrue(a.equals(MomentSchema.jarig(100, List.of("olifant", "zebra", "leeuw"))), "the birthday does not depend on the list order");
        helper.succeed();
    }

    /** The park's ride data (assets/guhs/safari) is there: a route loop with a stop per zone, the canoe, the cable car, the race. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ritData(GameTestHelper helper) {
        RitData.vergeet();
        RitData d = RitData.get();
        helper.assertTrue(d.route != null && d.route.lengte() > 500, "a truck route");
        helper.assertTrue(d.haltes.size() == 9, "a stop per zone (" + d.haltes.size() + ")");
        helper.assertTrue("entree".equals(d.haltes.get(0).zone()) || d.haltes.stream().anyMatch(h -> h.zone().equals("entree")), "the village has a stop");
        helper.assertTrue(d.kano != null && d.kanoStart != null && d.kano.lengte() > 100, "a canoe route");
        helper.assertTrue(d.kabel != null && d.kabel.lengte() > 200 && d.stations.size() == 2, "a kabelbaan loop with two stations");
        helper.assertTrue(d.boten.size() == 1, "one glass-bottom boat line (" + d.boten.size() + ")");
        RitData.Bootroute lijn = d.boten.get(0);
        helper.assertTrue(lijn.haltes().stream().map(RitData.Halte::naam).distinct().count() >= 5,
                "the boat line has the hoofdsteiger and 4 jetties (" + lijn.haltes() + ")");
        helper.assertTrue(lijn.haltes().get(0).afstand() < 1e-6 && "hoofdsteiger".equals(lijn.haltes().get(0).naam()),
                "the boat line starts at the hoofdsteiger");
        helper.assertTrue(RitSchema.boten(lijn.pad().lengte(), lijn.haltes().size()) >= 2, "several boats run the line");
        // hop on, hop off: in one round a boat stands still at every jetty of the line
        int n = RitSchema.boten(lijn.pad().lengte(), lijn.haltes().size());
        long ronde = RitSchema.ronde(lijn.pad().lengte(), lijn.haltes().size(), RitSchema.BOOT_LIJN_SNELHEID, RitSchema.BOOT_LIJN_WACHT);
        java.util.Set<Integer> gezien = new java.util.HashSet<>();
        for (long t = 0; t < ronde; t += 20) {
            RitSchema.Stand st = RitSchema.bootLijn(t, 1, n, lijn.pad().lengte(), lijn.halteAfstanden());
            if (st.staatStil()) {
                gezien.add(st.halte());
            }
        }
        helper.assertTrue(gezien.size() == lijn.haltes().size(), "the boat stops at every jetty (" + gezien.size() + "/" + lijn.haltes().size() + ")");
        helper.assertTrue(d.poorten.size() >= 5, "safaripoorten on the route (" + d.poorten.size() + ")");
        helper.assertTrue(d.race != null && d.raceWacht != null && d.raceBord != null, "a race track");
        helper.assertTrue(d.schemabord != null, "a schemabord");
        helper.assertTrue(RitSchema.trucks(d.route.lengte(), d.haltes.size()) >= 3, "several trucks run");
        helper.succeed();
    }
}
