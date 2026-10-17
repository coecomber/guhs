package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 "Sponsguh", slice sp_d2_weg (CONTRACT_E2_C 8.3): the slice's core state machines without a world, and its wiring.
 * <ul>
 *   <li>{@link #spD2WegJacht}: Dennis' chase: standing still gets you caught and sent back to the checkpoint (Dennis reset
 *       behind it), the halfway checkpoint counts once and is then where a catch sends you, the gate finishes, walking far off
 *       the road stops it, and beside the road Dennis never drives past you;</li>
 *   <li>{@link #spD2WegTraan}: the tear: it stays on the trail by itself, a hard tilt sideways drops it off (a new tear at
 *       the start), following the trail brings it into the socket;</li>
 *   <li>{@link #spD2WegStappen}: the four steps are real and have a hint, the variants follow the flags, the scenes exist
 *       with their lengths, the moustache has its clothes source.</li>
 * </ul>
 */
public class E2SpD2WegGameTests {
    private static final String BATCH = "einde2_sp_d2_weg";
    private static final String EMPTY = "empty";

    /** A straight test road: 13 points 5 blocks apart along x (start 10, halfway 30, the gate 45). */
    private static Jacht.Route rechteWeg() {
        List<Vec3> p = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            p.add(new Vec3(i * 5.0, 0, 0));
        }
        return new Jacht.Route(p);
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spD2WegJacht(GameTestHelper helper) {
        Jacht.Route r = rechteWeg();
        helper.assertTrue(r.bij(Jacht.START_PUNT) == 10 && r.bij(Jacht.HALF_PUNT) == 30 && r.bij(E2SpD2Weg.POORT_PUNT) == 45, "the test road");
        // 1. standing still at the start: caught, back to the start, Dennis reset behind it
        Jacht.Rit rit = new Jacht.Rit(r, 10);
        helper.assertTrue(rit.dennis == 2 && rit.checkpoint == 10 && !rit.half, "Dennis starts " + Jacht.VOORSPRONG + " behind the start");
        Jacht.Uit uit = Jacht.Uit.NIETS;
        int t = 0;
        for (; t < 400 && uit != Jacht.Uit.GEPAKT; t++) {
            uit = rit.stap(new Vec3(10, 0, 0.5), true);
        }
        helper.assertTrue(uit == Jacht.Uit.GEPAKT && t > Jacht.START_WACHT, "standing still gets you caught (after " + t + " ticks)");
        helper.assertTrue(rit.dennis == 2 && rit.checkpoint == 10 && rit.wacht == Jacht.START_WACHT && rit.gepakt == 1, "caught: Dennis behind the start again, revving");
        // 2. sprinting: past halfway (counted once), then standing still: caught back to halfway
        double x = 10;
        int half = 0;
        for (int i = 0; i < 90; i++) {
            x += 0.28;
            if (rit.stap(new Vec3(x, 0, 0), true) == Jacht.Uit.HALF) {
                half++;
            }
        }
        helper.assertTrue(half == 1 && rit.half && rit.checkpoint == 30 && x > 30, "halfway counts once (at x " + x + ")");
        uit = Jacht.Uit.NIETS;
        for (t = 0; t < 600 && uit != Jacht.Uit.GEPAKT; t++) {
            uit = rit.stap(new Vec3(x, 0, 0), true);
        }
        helper.assertTrue(uit == Jacht.Uit.GEPAKT && rit.dennis == 22 && rit.checkpoint == 30, "caught after halfway: back to halfway");
        // 3. a sprint from halfway reaches the gate without being caught
        x = 30;
        uit = Jacht.Uit.NIETS;
        for (t = 0; t < 200 && uit != Jacht.Uit.POORT; t++) {
            x += 0.28;
            uit = rit.stap(new Vec3(x, 0, 0), true);
            helper.assertTrue(uit != Jacht.Uit.GEPAKT, "a sprint is faster than Dennis (tick " + t + ")");
        }
        helper.assertTrue(uit == Jacht.Uit.POORT, "the gate finishes the chase");
        helper.assertTrue(new Jacht.Rit(r, 10).stap(new Vec3(20, 0, 20), true) == Jacht.Uit.KWIJT, "far off the road: the chase stops");
        // 4. beside the road (6 blocks): Dennis waits on the road level with you, never drives past, never catches
        Jacht.Rit naast = new Jacht.Rit(r, 10);
        for (t = 0; t < 400; t++) {
            helper.assertTrue(naast.stap(new Vec3(15, 0, 6), true) != Jacht.Uit.GEPAKT && naast.dennis < naast.speler, "beside the road: not caught");
        }
        // 5. a hungry player (no sprint) gets the slower Dennis, still slower than walking
        helper.assertTrue(Jacht.TRAAG < 0.216 && Jacht.SNEL < 0.28 && Jacht.SNEL > 0.216, "slower than a sprint, faster than walking; hungry: slower than walking");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spD2WegTraan(GameTestHelper helper) {
        List<Vec3> spoor = new ArrayList<>();
        for (double[] q : Traan.SPOOR) {
            spoor.add(new Vec3(q[0], q[1], q[2]));
        }
        // 1. no tilt: the wobble moves it, but it stays on the trail
        Traan.Bord b = new Traan.Bord(spoor);
        for (int t = 0; t < 400; t++) {
            helper.assertTrue(b.stap(0, 0) == Traan.Uit.NIETS, "untouched, the tear stays on the trail (tick " + t + ")");
        }
        // 2. a hard tilt sideways: off the trail; a new tear at Patguh's eye
        Traan.Uit uit = Traan.Uit.NIETS;
        int t = 0;
        for (; t < 200 && uit != Traan.Uit.VAN_DE_RAND; t++) {
            uit = b.stap(0, 1);
        }
        helper.assertTrue(uit == Traan.Uit.VAN_DE_RAND, "a hard tilt sideways drops it off the trail");
        b.nieuw();
        b.tranen++;
        helper.assertTrue(b.seg == 0 && b.x == spoor.get(0).x && b.z == spoor.get(0).z && b.vx == 0 && b.tranen == 2 && b.voortgang() == 0,
                "a new tear at the start, at rest");
        // 3. tilting along the trail: into the socket, and the height follows the table and the floor
        boolean opTafel = b.plek().y > 15.0, opVloer = false;
        uit = Traan.Uit.NIETS;
        for (t = 0; t < 3000 && uit == Traan.Uit.NIETS; t++) {
            double[] f = b.naarVolgende();
            uit = b.stap(f[0], f[1]);
            opVloer |= b.plek().y < 14.1;
        }
        helper.assertTrue(uit == Traan.Uit.BINNEN, "following the trail brings it into the socket (" + uit + " after " + t + " ticks, at " + b.voortgang() + ")");
        helper.assertTrue(opTafel && opVloer && t > 200, "from the table down onto the floor, and it takes a while (" + t + " ticks)");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spD2WegStappen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        try {
            for (int s = E2SpD2Weg.SNOR; s <= E2SpD2Weg.CYCLOOP; s++) {
                helper.assertTrue(SpStappen.isEcht(E2SpD2Weg.KROON, s), "step " + s + " is real");
                helper.assertTrue(StapHint.hint(E2SpD2Weg.KROON, s) != null, "step " + s + " has a hint");
            }
            String[] vlag = {null, E2SpD2Weg.V_GEZONGEN, E2SpD2Weg.V_JACHT, E2SpD2Weg.V_DROOG};
            for (int s = E2SpD2Weg.SNOR; s <= E2SpD2Weg.CYCLOOP; s++) {
                SpVerhaalDev.zetTot(p, E2SpD2Weg.KROON, s);
                helper.assertTrue(E2SpD2Weg.lijn().sleutel(p).equals(String.valueOf(s)), "step " + s + ": its own text");
                String v = vlag[s - E2SpD2Weg.SNOR];
                if (v != null) {
                    E2SpD2Weg.lijn().vlag(p, v, true);
                    helper.assertTrue(E2SpD2Weg.lijn().sleutel(p).equals(s + "_b"), "step " + s + " with its flag: variant " + s + "_b");
                    E2SpD2Weg.lijn().vlag(p, v, false);
                }
            }
            for (Map.Entry<String, int[]> e : D2WegScenes.LENGTES.entrySet()) {
                Cutscene s = Cutscene.van(D2WegScenes.ID + e.getKey());
                helper.assertTrue(s != null && s.lijn() != null && s.lijn().equals(E2SpD2Weg.KROON), "scene " + e.getKey() + " registered for the line");
                double sec = s.duur() / 20.0;
                boolean lied = e.getKey().equals("monsters");
                helper.assertTrue(sec >= e.getValue()[0] && (lied || sec <= e.getValue()[1]), "scene " + e.getKey() + ": " + sec + " s, wanted " + e.getValue()[0]
                        + "-" + e.getValue()[1]);
                helper.assertTrue(s.pauzes().length > 0 && E2SpD2WegCommando.ANKER.containsKey(e.getKey()), "scene " + e.getKey() + ": click lines and a dev anchor");
            }
            helper.assertTrue(Cutscene.van(D2WegScenes.ID + "kijk_rand") != null && Cutscene.van(D2WegScenes.ID + "kijk_pad") != null
                    && Cutscene.van(D2WegScenes.ID + "kijk_winkel") != null, "the three look scenes");
            helper.assertTrue(E2SpD2Weg.BRON.equals(KledingBronnen.bron(GuhClothes.EINDE2_SP_D2_WEG_SNOR))
                    && GuhClothes.EINDE2_SP_D2_WEG_SNOR.slot == GuhClothes.Slot.EYES, "the moustache: its source and slot");
        } finally {
            SpVerhaalDev.wis(p);
            E2SpD2Weg.wisEigen(p);
        }
        helper.succeed();
    }
}
