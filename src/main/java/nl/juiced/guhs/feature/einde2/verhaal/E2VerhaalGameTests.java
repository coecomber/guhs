package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 phase B, the skelet (CONTRACT_E2_B 9): the gating chain of 2.3 for a player put at four points of the story with
 * {@link E2VerhaalDev#zetTot}, the veil asking the same, a guest who may in with a host (a stand-in host), the big story of
 * the Guhpad, the hooks, and the sung line of a cutscene that does not wait. Lines that no slice has registered yet are
 * stood in for with main's own keys ({@link E2Verhaal#STAND_IN}), so this test holds before and after the wave.
 */
public class E2VerhaalGameTests {
    private static final String BATCH = "einde2_verhaal";
    private static final String EMPTY = "empty";
    /** The step count of a stand-in line. */
    private static final int STAND_IN_STAPPEN = 4;

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2VerhaalKeten(GameTestHelper helper) {
        List<String> standIn = E2Verhaal.HOOFDSTUKKEN.stream().filter(l -> E2Verhaal.lijn(l) == null).toList();
        standIn.forEach(l -> E2Verhaal.STAND_IN.put(l, STAND_IN_STAPPEN));
        ServerPlayer p = speler(helper), gast = speler(helper);
        String lucht = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, water = Einde2Ids.NOORDELIJKE_WATERSTAM, aarde = Einde2Ids.BA_SING_GUH,
                vuur = Einde2Ids.VUURNATIE_HOOFDSTAD;
        try {
            // a new player: nothing, the story may not even begin; a place that is not a story structure is always open
            helper.assertTrue(!E2Verhaal.magBeginnen(p) && E2Verhaal.ontgrendeld(p).isEmpty() && !E2Verhaal.open(p, lucht)
                    && Sluiers.isVerborgen(p, lucht) && E2Verhaal.open(p, Einde2Ids.LEEGTEHAVEN), "a new player: all four closed");
            // point 1: Opper-Mika won, chapter 1 begun
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, 1);
            helper.assertTrue(E2Verhaal.magBeginnen(p) && E2Verhaal.stap(p, E2Verhaal.LUCHT) == 1 && E2Verhaal.ontgrendeld(p).equals(List.of(lucht))
                    && E2Verhaal.open(p, lucht) && !E2Verhaal.open(p, water) && Sluiers.open(p, lucht) && !Sluiers.open(p, water),
                    "chapter 1 begun: only the air temple");
            helper.assertTrue(!E2Verhaal.elementGeleerd(p, Einde2Ids.Element.LUCHT) && !Einde2Haken.ELEMENT_GELEERD.test(p, Einde2Ids.Element.LUCHT),
                    "air not learnt yet");
            E2VerhaalDev.vlag(p, E2Verhaal.LUCHT, E2Verhaal.V_GELEERD, true);
            helper.assertTrue(E2Verhaal.elementGeleerd(p, Einde2Ids.Element.LUCHT) && E2Verhaal.vlag(p, E2Verhaal.LUCHT, E2Verhaal.V_GELEERD)
                    && !E2Verhaal.klaar(p, E2Verhaal.LUCHT), "the flag geleerd counts before the chapter is done");
            // point 2: chapter 2 at its start (chapter 1 done)
            E2VerhaalDev.zetTot(p, E2Verhaal.WATER, 0);
            helper.assertTrue(E2Verhaal.klaar(p, E2Verhaal.LUCHT) && E2Verhaal.ontgrendeld(p).equals(List.of(lucht, water)) && !E2Verhaal.open(p, aarde)
                    && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.LUCHT) && !E2Verhaal.elementGeleerd(p, Einde2Ids.Element.WATER)
                    && Einde2Haken.VERHAAL_KLAAR.test(p, E2Verhaal.LUCHT) && !Einde2Haken.VERHAAL_KLAAR.test(p, E2Verhaal.WATER),
                    "chapter 1 done: the water tribe opens");
            // point 3: chapter 4 (three chapters done)
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, 1);
            helper.assertTrue(E2Verhaal.ontgrendeld(p).equals(Einde2Ids.VERHAAL_STRUCTUREN) && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.AARDE)
                    && !E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR) && !E2Verhaal.finaleKlaar(p)
                    && E2Verhaal.bekendeGebouwen(p, true).equals(Einde2Ids.VERHAAL_STRUCTUREN), "chapter 4: every story structure, the finale not yet");
            // point 4: the finale done
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, STAND_IN_STAPPEN + 100);
            GroteVerhalen.GrootVerhaal groot = GroteVerhalen.van(E2Verhaal.GROOT);
            helper.assertTrue(E2Verhaal.finaleKlaar(p) && E2Verhaal.klaar(p, E2Verhaal.VUUR) && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR)
                    && groot != null && groot == E2Verhaal.grootVerhaal() && groot.klaar(p) && !groot.klaar(gast)
                    && groot.wereld() == GroteVerhalen.Wereld.GUHEINDE && groot.structuren().equals(Einde2Ids.VERHAAL_STRUCTUREN)
                    && groot.lijnen().equals(E2Verhaal.HOOFDSTUKKEN), "the finale: the big story of the Guhpad is done");
            helper.assertTrue(E2Verhaal.lijnVan(vuur).equals(E2Verhaal.VUUR) && E2Verhaal.structuurVan(E2Verhaal.FINALE).equals(vuur)
                    && E2Verhaal.lijnVan(Einde2Ids.OMASHGUH) == null && GroteVerhalen.wereldVan(E2Verhaal.WATER) == GroteVerhalen.Wereld.GUHEINDE,
                    "lines and structures");
            // a guest: closed alone, open with a host (reizen decides who is a guest; here a stand-in says so)
            helper.assertTrue(!E2Verhaal.open(gast, vuur) && E2Reizen.gastheer(gast) == null, "the guest alone: closed");
            E2Verhaal.gastheer = q -> q == gast ? p : null;
            helper.assertTrue(E2Verhaal.open(gast, vuur) && Sluiers.open(gast, vuur) && !E2Verhaal.ontgrendeld(gast, vuur) && E2Verhaal.stap(gast, E2Verhaal.VUUR) == 0,
                    "the guest with the host: open, and nothing of their own changed");
            E2Verhaal.gastheer = E2Reizen::gastheer;
            helper.assertTrue(!E2Verhaal.open(gast, vuur), "the host gone: closed again");
            // wiping a line
            E2VerhaalDev.wis(p, E2Verhaal.LUCHT);
            helper.assertTrue(E2Verhaal.stap(p, E2Verhaal.LUCHT) == 0 && !E2Verhaal.vlag(p, E2Verhaal.LUCHT, E2Verhaal.V_GELEERD) && !E2Verhaal.open(p, lucht),
                    "wiped: the air temple closes again");
        } finally {
            E2Verhaal.gastheer = E2Reizen::gastheer;
            standIn.forEach(E2Verhaal.STAND_IN::remove);
            for (String l : E2Verhaal.HOOFDSTUKKEN) {
                E2VerhaalDev.wis(p, l);
            }
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(gast, Entity.RemovalReason.DISCARDED);
        }
        // the sung line (12a: yes): it never waits for a click, the spoken lines round it do
        Cutscene s = Cutscene.maak("einde2_verhaal_lied_test").duur(100).zeg(10, "", "a", 20).lied(30, "b", 20).zeg(50, "", "c", 20).bouw();
        Cutscene.Zeg lied = s.zinOp(35);
        helper.assertTrue(java.util.Arrays.equals(s.pauzes(), new int[] {29, 69}) && lied != null && lied.lied() && lied.spreker().isEmpty()
                && !s.zinOp(15).lied(), "a sung line has no pause: " + java.util.Arrays.toString(s.pauzes()));
        helper.succeed();
    }
}
