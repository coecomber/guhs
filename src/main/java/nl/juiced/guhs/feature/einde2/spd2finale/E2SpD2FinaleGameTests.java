package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.sponsguh.Gerrit;
import nl.juiced.guhs.feature.einde2.sponsguh.Sponsguh;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 "Sponsguh", slice sp_d2_finale (CONTRACT_E2_C 8.3): the core of the slice without the world (the test level has no
 * Bikinibodem): the bucket game's state (each guh counts once, the sleutel turns "10_b" at six, the Bellenkom ends the step only
 * then), and the end of the story (step 11 -> done, the rewards exactly once: the buddy, Gerrit, the antennas, the title), plus
 * the scenes' shape (the lengths of the contract, every spoken line before the scene's end, the song placed by SpAudio.lied).
 */
public class E2SpD2FinaleGameTests {
    private static final String BATCH = "einde2_sp_d2_finale";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spD2FinaleEmmers(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            helper.assertTrue(SpStappen.isEcht(SpVerhaal.KROON, SpVerhaal.K_HASSELGUH) && SpStappen.isEcht(SpVerhaal.KROON, SpVerhaal.K_EMMERS)
                    && SpStappen.isEcht(SpVerhaal.KROON, SpVerhaal.K_GUHFY), "steps 9-11 are this slice's (no stand-in)");
            SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_HASSELGUH);
            helper.assertTrue(!D2fEmmers.bevrijd(p, 0) && D2fEmmers.vrij(p) == 0, "no bucket game before step 10");
            SpVerhaal.KROON_LIJN.verder(p, SpVerhaal.K_HASSELGUH);                   // (the landing)
            helper.assertTrue(SpVerhaal.stap(p, SpVerhaal.KROON) == SpVerhaal.K_EMMERS && "10".equals(SpVerhaal.KROON_LIJN.sleutel(p)), "step 10: sleutel 10");
            helper.assertTrue(!D2fEmmers.inBellenkom(p), "the Bellenkom does nothing while buckets are left");
            for (int i = 0; i < D2fEmmers.AANTAL; i++) {
                helper.assertTrue(D2fEmmers.bevrijd(p, i) && !D2fEmmers.bevrijd(p, i) && D2fEmmers.vrij(p) == i + 1, "guh " + i + " counts once");
                if (i < D2fEmmers.AANTAL - 1) {
                    helper.assertTrue("10".equals(SpVerhaal.KROON_LIJN.sleutel(p)) && !D2fEmmers.inBellenkom(p), "still buckets after " + (i + 1));
                }
            }
            helper.assertTrue(!D2fEmmers.bevrijd(p, D2fEmmers.AANTAL) && !D2fEmmers.bevrijd(p, -1), "no seventh guh");
            helper.assertTrue(D2fEmmers.alleVrij(p) && "10_b".equals(SpVerhaal.KROON_LIJN.sleutel(p)), "all six free: sleutel 10_b (walk to the Bellenkom)");
            helper.assertTrue(D2fEmmers.inBellenkom(p) && !D2fEmmers.inBellenkom(p) && SpVerhaal.stap(p, SpVerhaal.KROON) == SpVerhaal.K_GUHFY,
                    "the Bellenkom ends step 10 once");
            helper.succeed();
        } finally {
            SpVerhaalDev.wis(p);
            D2fStoet.ruimOp(p);
        }
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spD2FinaleKlaar(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_GUHFY);
            helper.assertTrue(D2fEmmers.alleVrij(p), "a jump to step 11: the six count as free");
            Titels.Titel titel = Titels.van(E2SpD2Finale.TITEL);
            helper.assertTrue(titel != null && !Titels.heeft(p, titel) && !KledingUnlocks.heeft(p, GuhClothes.SPONSGUH_ANTENNES)
                    && !Sponsguh.heeft(p) && !Gerrit.heeft(p), "nothing before the song");
            E2SpD2Finale.klaar(p);
            helper.assertTrue(SpVerhaal.klaar(p) && Titels.heeft(p, titel) && KledingUnlocks.heeft(p, GuhClothes.SPONSGUH_ANTENNES)
                    && Sponsguh.heeft(p) && Gerrit.heeft(p) && SpVerhaal.KROON_LIJN.vlag(p, E2SpD2Finale.BELOOND), "the line done, every reward given");
            E2SpD2Finale.klaar(p);
            E2SpD2Finale.beloon(p);
            helper.assertTrue(SpVerhaal.KROON_LIJN.stap(p) == SpVerhaal.K_STAPPEN, "a second time changes nothing");
            helper.assertTrue(E2SpD2Finale.beloningen(p).size() == 4 && E2SpD2Finale.beloningen(p).stream().allMatch(b -> b.binnen()),
                    "the Guhdex shows the four rewards as received");
            // the scenes: the contract's lengths, every spoken line inside the scene, the song inside the big one
            for (Map.Entry<String, int[]> e : D2fScenes.LENGTES.entrySet()) {
                Cutscene c = Cutscene.van(D2fScenes.ID + e.getKey());
                helper.assertTrue(c != null, "scene " + e.getKey() + " is registered");
                double s = D2fScenes.seconden(c) - (e.getKey().equals("guhfy_guhber") ? D2fScenes.liedDuur / 20.0 : 0);
                helper.assertTrue(s >= e.getValue()[0] && s <= e.getValue()[1], "scene " + e.getKey() + " lasts " + s + " s (without a song)");
                for (Cutscene.Zeg z : c.zinnen()) {
                    helper.assertTrue(z.t() + 10 < c.duur(), "a line of " + e.getKey() + " starts before its end: " + z.key());
                    helper.assertTrue(z.spreker().isEmpty() || z.spreker().equals("verteller") || z.spreker().equals("karen")
                            || z.spreker().equals("plankmika_stem") || c.acteur(z.spreker()) != null, "a speaker is an actor: " + e.getKey() + "." + z.key());
                }
            }
            helper.assertTrue(D2fScenes.liedStart > 0 && D2fScenes.liedStart + D2fScenes.liedDuur < D2fScenes.GUHFY_GUHBER.duur(), "the song fits in its scene");
            helper.succeed();
        } finally {
            SpVerhaalDev.wis(p);
        }
    }
}
