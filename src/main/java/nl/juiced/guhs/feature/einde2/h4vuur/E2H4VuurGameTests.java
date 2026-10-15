package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 phase B, slice h4_vuur: the state machine of chapter 4 and the finale for one of two players (CONTRACT_E2_B 8), and
 * the scripts of the cutscenes against the contract's lengths. The headless test server has no Guheinde, so the places
 * themselves (zones, the duel, the fleet) are played through the dev command in a real world; here the steps, the flags,
 * the rewards and the scenes.
 */
public class E2H4VuurGameTests {
    private static final String BATCH = "einde2_h4_vuur";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static boolean heeftBeeld(ServerPlayer p) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(E2H4VuurFeature.BEELD_ITEM.get()) && s.get(DataComponents.PROFILE) != null) {
                return true;
            }
        }
        return false;
    }

    /** The two lines step by step for one player: the disguise, the making-up, the dragons' flags, the rewards; the other player untouched. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void einde2H4VuurKeten(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        try {
            // chapter 4 begins (chapter 3 done): not in disguise before the arrival, in disguise from the school to the Black Sun
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_AANKOMST);
            E2VerhaalDev.zetTot(q, E2Verhaal.VUUR, E2H4Vuur.V_AANKOMST);
            helper.assertTrue(!E2H4Vuur.vermomd(p) && Look.van(p) == Look.GEEN, "no disguise before the arrival");
            E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_AANKOMST);
            helper.assertTrue(E2H4Vuur.vermomd(p) && Look.van(p) == Look.VERMOMD && !E2H4Vuur.vermomd(q), "in disguise after the arrival (only p)");
            // the dance party: won
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_DANS);
            Dansfeest.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_EENDJES && !Dansfeest.actief(p), "the party won: the turtle-ducks are next");
            // after the Black Sun: no disguise any more
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_GEVANGENIS);
            helper.assertTrue(!E2H4Vuur.vermomd(p), "no disguise after the Black Sun");
            // making it up: a wrong answer changes nothing, Toph then Katara moves the step on
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_GOEDMAKEN);
            E2VerhaalDev.zetTot(q, E2Verhaal.VUUR, E2H4Vuur.V_GOEDMAKEN);
            Goedmaken.antwoordToph(p, null, 2);
            helper.assertTrue(!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH) && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_GOEDMAKEN, "a wrong answer: nothing");
            Goedmaken.antwoordToph(p, null, 1);
            helper.assertTrue(E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH) && E2H4Vuur.VUUR.sleutel(p).equals("7_katara")
                    && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_GOEDMAKEN, "Toph made up: Katara is next");
            Goedmaken.antwoordKatara(p, null, 3);
            helper.assertTrue(!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_KATARA), "a silly answer for Katara: nothing");
            Goedmaken.antwoordKatara(p, null, 1);
            helper.assertTrue(E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_ROTSBLOK && E2H4Vuur.VUUR.stap(q) == E2H4Vuur.V_GOEDMAKEN
                    && !E2H4Vuur.VUUR.vlag(q, E2H4Vuur.V_TOPH), "both made up: Het Kokend Rotsblok; the other player untouched");
            // the dragons: Zuka joins (zuka_team) and fire is learnt (geleerd), for p only
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_DRAKENDANS);
            helper.assertTrue(!E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR), "fire not learnt before the dragons");
            Drakendans.winVoorTest(p);
            helper.assertTrue(E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM) && E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_GELEERD)
                    && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR) && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_TONEEL
                    && !E2Verhaal.vlag(q, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM), "the dragons: zuka_team and geleerd (p only); the theatre is next");
            // chapter 4 done: the disguise as guh clothes
            E2H4Vuur.VUUR.zet(p, E2H4Vuur.V_STAPPEN);
            helper.assertTrue(E2Verhaal.klaar(p, E2Verhaal.VUUR) && KledingUnlocks.heeft(p, GuhClothes.VUURNATIE_VERMOMMING)
                    && !KledingUnlocks.heeft(q, GuhClothes.VUURNATIE_VERMOMMING), "chapter 4 done: the Vuurnatie disguise for p");
            // the finale: the dev command's jump, the fleet and the duel as if won, then the end
            helper.assertTrue(E2H4VuurCommando.zet(p, "luchtvloot") && E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_VLOOT
                    && E2Verhaal.vlag(p, E2Verhaal.FINALE, E2Verhaal.V_WITTE_LOTUS) && E2Verhaal.klaar(p, E2Verhaal.VUUR), "the dev jump to the fleet");
            Luchtvloot.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_AGNI_KAI, "the fleet flown: the Agni Kai");
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, E2H4Vuur.F_DUEL);
            Duel.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_KRONING && !Duel.actief(p) && Look.van(p) == Look.GEEN, "the duel won: the coronation");
            helper.assertTrue(!E2Verhaal.finaleKlaar(p) && !heeftBeeld(p), "no reward before the end");
            E2H4Vuur.FINALE.zet(p, E2H4Vuur.F_STAPPEN);
            Titels.Titel titel = Titels.van(E2H4VuurFeature.TITEL);
            helper.assertTrue(E2Verhaal.finaleKlaar(p) && titel != null && Titels.heeft(p, titel) && !Titels.heeft(q, titel), "the title Guhvatar (p only)");
            helper.assertTrue(heeftBeeld(p) && KledingUnlocks.heeft(p, GuhClothes.ZUKA_LITTEKEN) && !heeftBeeld(q), "the statue and the scar");
            int beelden = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                beelden += s.is(E2H4VuurFeature.BEELD_ITEM.get()) ? s.getCount() : 0;
            }
            E2H4Vuur.FINALE.zet(p, E2H4Vuur.F_STAPPEN);
            int daarna = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                daarna += s.is(E2H4VuurFeature.BEELD_ITEM.get()) ? s.getCount() : 0;
            }
            helper.assertTrue(beelden == 1 && daarna == 1, "the statue only once");
        } finally {
            for (ServerPlayer s : new ServerPlayer[] {p, q}) {
                E2H4Vuur.wisEigen(s);
                for (String l : E2Verhaal.HOOFDSTUKKEN) {
                    E2VerhaalDev.wis(s, l);
                }
                helper.getLevel().removePlayerImmediately(s, Entity.RemovalReason.DISCARDED);
            }
        }
        helper.succeed();
    }

    /** Every scene of the slice is registered, lasts what the contract allows, and only the credits run on without clicks. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void einde2H4VuurScenes(GameTestHelper helper) {
        for (Map.Entry<String, int[]> e : Scenes.LENGTES.entrySet()) {
            Cutscene s = Cutscene.van(Scenes.ID + e.getKey());
            helper.assertTrue(s != null, "scene " + e.getKey() + " registered");
            double sec = s.duur() / 20.0;
            helper.assertTrue(sec >= e.getValue()[0] && sec <= e.getValue()[1], "scene " + e.getKey() + " lasts " + sec + " s, not "
                    + e.getValue()[0] + "-" + e.getValue()[1]);
            helper.assertTrue(s.lijn() != null && (s.lijn().equals(E2Verhaal.VUUR) || s.lijn().equals(E2Verhaal.FINALE)), "scene " + e.getKey() + " replayable");
            for (Cutscene.Zeg z : s.zinnen()) {
                helper.assertTrue(!z.lied() || e.getKey().equals("aftiteling"), "only the credits run on: " + e.getKey() + "." + z.key());
                helper.assertTrue(z.spreker().isEmpty() || s.acteur(z.spreker()) != null, "a speaker is an actor: " + e.getKey() + "." + z.key());
                helper.assertTrue(z.t() + z.ticks() <= s.duur(), "the line ends in time: " + e.getKey() + "." + z.key());
            }
        }
        long eigen = Cutscene.alle().stream().filter(s -> s.id().startsWith(Scenes.ID)).count();
        helper.assertTrue(eigen == Scenes.LENGTES.size(), "18 scenes, not " + eigen);
        helper.succeed();
    }
}
