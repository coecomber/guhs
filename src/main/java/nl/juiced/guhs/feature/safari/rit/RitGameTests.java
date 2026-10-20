package nl.juiced.guhs.feature.safari.rit;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.safari.momenten.MomentSchema;
import nl.juiced.guhs.gametest.GuhTest;

/** Op Safari! step 2 (S3): the truck and gondola timetables and the day plan of the moments (pure arithmetic). */
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

    /** The gondolas wait in a station, travel, wait at the other end; the two are always at opposite ends. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void gondels(GameTestHelper helper) {
        double lengte = 180;
        long ronde = RitSchema.gondelRonde(lengte);
        double[] g0 = RitSchema.gondel(0, 0, lengte);
        helper.assertTrue(g0[0] == 0 && g0[1] == 0, "gondola 0 starts waiting at station A");
        double[] g1 = RitSchema.gondel(0, 1, lengte);
        helper.assertTrue(g1[0] == 1 && g1[1] == 1, "gondola 1 starts waiting at station B");
        double[] onderweg = RitSchema.gondel(RitSchema.GONDEL_WACHT + 100, 0, lengte);
        helper.assertTrue(onderweg[1] < 0 && onderweg[0] > 0 && onderweg[0] < 1, "under way in between");
        for (long t = 0; t < ronde; t += 37) {
            double a = RitSchema.gondel(t, 0, lengte)[0], b = RitSchema.gondel(t, 1, lengte)[0];
            helper.assertTrue(Math.abs(a + b - 1) < 0.02, "always mirrored (t=" + t + ")");
        }
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
        helper.assertTrue(d.kabelA != null && d.kabelB != null && d.kabelA.distanceTo(d.kabelB) > 50, "a cable car");
        helper.assertTrue(d.race != null && d.raceWacht != null && d.raceBord != null, "a race track");
        helper.assertTrue(d.schemabord != null, "a schemabord");
        helper.assertTrue(RitSchema.trucks(d.route.lengte(), d.haltes.size()) >= 3, "several trucks run");
        helper.succeed();
    }
}
