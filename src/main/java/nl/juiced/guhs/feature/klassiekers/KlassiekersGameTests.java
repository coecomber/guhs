package nl.juiced.guhs.feature.klassiekers;

import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;

/**
 * GameTests of the shared part of the klassiekers' levels (2.9): the level travels with a screen action, the floating
 * board shows all three levels, and the advancements (makkelijk, medium, lastig per game, the Klassiekers-kampioen).
 * The games' own level tests are in their own GameTests classes (MeppenGameTests, GolfGameTests, SmulGameTests,
 * VissenGameTests, BeautyGameTests).
 */
public class KlassiekersGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY)
    public static void klassiekersActionsCarryTheLevel(GameTestHelper helper) {
        for (int action = 0; action < 8; action++) {
            helper.assertTrue(Klassiekers.niveau(action) == Niveau.MEDIUM && Klassiekers.actie(action) == action, "a plain action is medium: " + action);
            for (Niveau n : Niveau.values()) {
                int sent = Klassiekers.metNiveau(action, n);
                helper.assertTrue(Klassiekers.niveau(sent) == n && Klassiekers.actie(sent) == action, "action " + action + " on " + n + ": " + sent);
            }
        }
        helper.assertTrue(Klassiekers.sleutel("guhs_mep_best", Niveau.MEDIUM).equals("guhs_mep_best")
                && Klassiekers.sleutel("guhs_mep_best", Niveau.LASTIG).equals("guhs_mep_best_lastig"), "medium keeps the old record key");
        helper.assertTrue(Niveau.LASTIG.munten(2) == 3 && Niveau.LASTIG.munten(6) == 9 && Niveau.MAKKELIJK.munten(6) == 6, "lastig: half as much more");
        // every level of every game has its Highscores row (and so a place in the Guhdex)
        for (String board : new String[]{"beauty_show", "meppen_score", "golf_rondje", "smul_punten", "vissen_punten", "vissen_zwaarste"}) {
            for (Niveau n : Niveau.values()) {
                String b = n.board(board);
                helper.assertTrue(Highscores.GAMES.stream().anyMatch(g -> g.board().equals(b)), "a Highscores row for " + b);
            }
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void klassiekersBoardShowsAllThreeLevels(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Scorebord.submit(player, "klassiekers_testbord_lastig", 4242, false);
        Component text = Klassiekers.bord(helper.getLevel().getServer(), Component.literal("Test"), "klassiekers_testbord",
                n -> Component.literal("extra" + n), s -> s + " pt");
        String json = net.minecraft.network.chat.ComponentSerialization.CODEC
                .encodeStart(helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), text).getOrThrow().toString();
        helper.assertTrue(json.contains("gui.guhs.niveau.makkelijk") && json.contains("gui.guhs.niveau.medium") && json.contains("gui.guhs.niveau.lastig"),
                "three levels: " + json);
        helper.assertTrue(json.contains("extra0") && json.contains("extra2"), "with their extra line");
        helper.assertTrue(json.contains(player.getGameProfile().name() + "  4242 pt"), "and the lastig score");
        helper.assertTrue(json.indexOf("gui.guhs.niveau.lastig") < json.indexOf("4242"), "under the lastig heading");
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void klassiekersKampioenAfterAllFiveOnLastig(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(!Klassiekers.done(player, Klassiekers.ADV_MAKKELIJK), "nothing yet");
        Klassiekers.gespeeld(player, "golf", Niveau.MAKKELIJK);
        Klassiekers.gespeeld(player, "golf", Niveau.MEDIUM);
        helper.assertTrue(Klassiekers.done(player, Klassiekers.ADV_MAKKELIJK) && Klassiekers.done(player, Klassiekers.ADV_MEDIUM),
                "makkelijk and medium");
        helper.assertTrue(!Klassiekers.done(player, Klassiekers.lastigAdvancement("golf")), "but not lastig");
        for (int i = 0; i < Klassiekers.SPELLEN.size(); i++) {
            helper.assertTrue(!Klassiekers.done(player, Klassiekers.ADV_KAMPIOEN), "no champion before all five: " + i);
            Klassiekers.gespeeld(player, Klassiekers.SPELLEN.get(i), Niveau.LASTIG);
            helper.assertTrue(Klassiekers.done(player, Klassiekers.lastigAdvancement(Klassiekers.SPELLEN.get(i))), "lastig " + Klassiekers.SPELLEN.get(i));
        }
        helper.assertTrue(Klassiekers.done(player, Klassiekers.ADV_KAMPIOEN), "all five on lastig: Klassiekers-kampioen!");
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
