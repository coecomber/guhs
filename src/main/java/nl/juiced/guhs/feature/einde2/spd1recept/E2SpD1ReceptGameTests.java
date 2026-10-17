package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 "Sponsguh", slice sp_d1_recept (CONTRACT_E2_C 8.3): the two minigames' rules, played without a world, and the slice's
 * hold on its steps.
 * <ul>
 *   <li>{@link #spD1ReceptTrommel}: the drum round: clicks on every pop win, a click one tick past the window misses, a click far
 *       too early spends that bubble, a double click counts once, nine hits lose (and a new round starts empty), a round ends
 *       after its last bubble;</li>
 *   <li>{@link #spD1ReceptSluipen}: the sneak: rest points only forward; a moment in a camera's sight is forgiven, a full meter
 *       sends you back (and gives you breathing time), a laser at once; every laser is off long enough to walk through, every
 *       camera looks away from its corridor long enough, its blind spot and the crouch range hold; the slice owns steps 7-10
 *       with a hint each, its markers and cameras are in the build, its scenes are registered for the line.</li>
 * </ul>
 */
public class E2SpD1ReceptGameTests {
    private static final String BATCH = "einde2_sp_d1_recept";

    @GuhTest(template = "empty", batch = BATCH)
    public static void spD1ReceptTrommel(GameTestHelper helper) {
        // every pop hit on the beat: all sixteen, won
        Trommel.Ronde alle = new Trommel.Ronde();
        for (int i = 0; i < Trommel.NOTEN; i++) {
            helper.assertTrue(alle.klik(Trommel.noot(i) + (i % 3) - 1) == i, "bubble " + i + " hit near its beat");
        }
        helper.assertTrue(alle.hits == Trommel.NOTEN && alle.gewonnen(), "sixteen hits win");
        helper.assertTrue(!alle.voorbij(Trommel.noot(Trommel.NOTEN - 1)) && alle.voorbij(Trommel.noot(Trommel.NOTEN - 1) + Trommel.UITLOOP + 1),
                "the round ends a little after its last bubble");
        // the window, a double click, a click far too early, a click into nothing
        Trommel.Ronde r = new Trommel.Ronde();
        helper.assertTrue(r.klik(Trommel.noot(0) + Trommel.RAAM + 1) == -1 && r.hits == 0, "a click just past the window is no hit");
        helper.assertTrue(r.klik(Trommel.noot(1) - Trommel.RAAM) == 1 && r.klik(Trommel.noot(1)) != 1 && r.hits == 1,
                "the edge of the window hits; a second click on the same bubble counts nothing");
        helper.assertTrue(r.klik(Trommel.noot(2) - Trommel.TE_VROEG) == -2 && r.klik(Trommel.noot(2)) != 2 && r.hits == 1,
                "a click far too early spends that bubble: drumming wildly does not win");
        helper.assertTrue(new Trommel.Ronde().klik(-40) == -1, "a click during the count-in is nothing at all");
        // nine hits lose; the next round starts empty
        Trommel.Ronde negen = new Trommel.Ronde();
        for (int i = 0; i < Trommel.NODIG - 1; i++) {
            negen.klik(Trommel.noot(i));
        }
        negen.voorbij(Trommel.noot(Trommel.NOTEN - 1) + Trommel.UITLOOP + 1);
        helper.assertTrue(negen.hits == Trommel.NODIG - 1 && !negen.gewonnen() && negen.geweest() == Trommel.NOTEN,
                "nine hits do not win; every bubble came by");
        helper.assertTrue(new Trommel.Ronde().hits == 0 && Trommel.NODIG <= Trommel.NOTEN * 2 / 3, "a new round is empty; ten of sixteen is forgiving");
        // the beat: 112 BPM from tick 0, the notes in order and at least half a second apart on average
        helper.assertTrue(Trommel.BPM == 112 && Trommel.noot(0) == Math.round(4 * 1200.0 / 112) && Trommel.PATROON.length == Trommel.NOTEN,
                "the notes sit on the beats of deuntje.trommel");
        for (int i = 1; i < Trommel.NOTEN; i++) {
            helper.assertTrue(Trommel.noot(i) - Trommel.noot(i - 1) >= 2 * Trommel.RAAM, "two bubbles never overlap more than one tick (" + i + ")");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void spD1ReceptSluipen(GameTestHelper helper) {
        // rest points: only forward
        Sluipen.Voortgang v = new Sluipen.Voortgang();
        helper.assertTrue(v.bereikt(1) && !v.bereikt(1) && !v.bereikt(0) && v.rust == 1 && v.bereikt(2) && v.rust == 2, "rest points only forward");
        // a moment in sight is forgiven; a full meter is seen; then a breather; a laser at once
        Sluipen.Voortgang m = new Sluipen.Voortgang();
        long t = 1000;
        for (int i = 0; i < Sluipen.METER - 1; i++) {
            helper.assertTrue(!m.tik(t++, true, false), "still unseen after " + (i + 1) + " ticks in sight");
        }
        helper.assertTrue(!m.tik(t++, false, false) && m.meter == Sluipen.METER - 2, "out of sight the meter runs down");
        helper.assertTrue(!m.tik(t++, true, false) && m.tik(t++, true, false) && m.gezien == 1, "a full meter: seen");
        long gezienOp = t - 1;
        helper.assertTrue(!m.tik(gezienOp + 1, true, true) && !m.tik(gezienOp + Sluipen.RUST - 1, true, true), "after being sent back nothing sees you for a while");
        helper.assertTrue(m.tik(gezienOp + Sluipen.RUST, false, true) && m.gezien == 2, "a laser that is on: seen at once");
        // the lasers: on half the time, and off long enough to walk through (a block and a player in under 15 ticks)
        for (int nr = 0; nr < Sluipen.LASERS.size(); nr++) {
            int uit = 0, langste = 0;
            for (long k = 0; k < Sluipen.LASER_PERIODE * 2L; k++) {
                uit = Sluipen.laserAan(nr, k) ? 0 : uit + 1;
                langste = Math.max(langste, uit);
            }
            helper.assertTrue(langste >= Sluipen.LASER_UIT && Sluipen.LASER_UIT - Sluipen.WAARSCHUW >= 30, "laser " + (nr + 1) + " is off long enough");
        }
        // the cameras: each looks away from the middle of its corridor for a good while every sweep
        Vec3 cam = Vec3.ZERO;
        for (int nr = 0; nr < Sluipen.CAMS.size(); nr++) {
            float basis = Sluipen.CAMS.get(nr).basis();
            Vec3 gang = cam.add(Vec3.directionFromRotation(0, basis).scale(4.5));
            int vrij = 0, langste = 0;
            for (long k = 0; k < Sluipen.CAMS.get(nr).periode() * 2L; k++) {
                vrij = Sluipen.cameraZiet(cam, Sluipen.cameraYaw(nr, k), gang, false) ? 0 : vrij + 1;
                langste = Math.max(langste, vrij);
            }
            helper.assertTrue(langste >= 40, "camera " + (nr + 1) + " looks away from its corridor for two seconds or more (" + langste + ")");
            helper.assertTrue(!Sluipen.cameraZiet(cam, basis, cam.add(Vec3.directionFromRotation(0, basis).scale(Sluipen.DODE_HOEK - 0.3)), false),
                    "right under camera " + (nr + 1) + " is its blind spot");
            Vec3 midden = cam.add(Vec3.directionFromRotation(0, basis).scale(4.5));
            helper.assertTrue(Sluipen.cameraZiet(cam, basis, midden, false) && !Sluipen.cameraZiet(cam, basis, midden, true),
                    "camera " + (nr + 1) + " sees a walker at 4.5 blocks, not a crouching one");
        }
        // the slice holds its steps: real, a hint each with a resume spot, its markers and cameras in the build, its scenes
        for (int s : E2SpD1Recept.STAPPEN) {
            helper.assertTrue(SpStappen.isEcht(SpVerhaal.RECEPT, s) && StapHint.hint(SpVerhaal.RECEPT, s) != null, "step " + s + " is ours and has a hint");
        }
        for (String id : List.of("d1r_kk_deur", "d1r_rust_2", "d1r_rust_3", "d1r_trommel_speler", "d1r_publiek_1", "d1r_kantoor_camera",
                "d1r_kantoor_2_camera", "d1r_lab_camera", "d1r_straat_camera", "d1r_vetemmer_deur_camera", "d1r_recept_camera",
                "d1r_zege_wijd_camera", "d1r_zege_sponsguh_camera", "d1r_zege_trommel_camera", "d1r_zege_octo_camera",
                "d1r_zege_octoliam_camera", "d1r_zege_publiek_camera", "sluip_start", "receptkluis", "kantoor", "lab", "vetemmer", "bellenkom")) {
            helper.assertTrue(Plekken.plek(SpVerhaal.BIKINIBODEM, id) != null, "Bikinibodem has the marker " + id);
        }
        for (D1rScenes.Scene s : D1rScenes.ALLE.values()) {
            Cutscene c = Cutscene.van(s.id());
            helper.assertTrue(c == s.cutscene() && SpVerhaal.RECEPT.equals(c.lijn()), s.id() + " is registered for part 1");
        }
        helper.assertTrue(D1rScenes.ALLE.size() == 6 && D1rScenes.PLAN_Z.cutscene().duur() >= 45 * 20 && D1rScenes.ZOETE_ZEGE.cutscene().duur() >= 45 * 20,
                "six story scenes; the two big ones are long enough");
        helper.succeed();
    }
}
