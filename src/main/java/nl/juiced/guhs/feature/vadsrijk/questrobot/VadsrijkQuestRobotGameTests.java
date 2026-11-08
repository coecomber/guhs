package nl.juiced.guhs.feature.vadsrijk.questrobot;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-QUESTROBOT: the robot plays every quest of every zone (headless) and writes {@code <run>/questrobot/robot.json}; then
 * {@code python tools/vadsrijk/questrobot/rapport.py <that json>} makes QUESTROBOT_RAPPORT.md. The test itself only fails when the
 * robot cannot run at all or zone 1 (the oldest, fully played chain) gets stuck: the report is the real output.
 */
public class VadsrijkQuestRobotGameTests {
    @GuhTest(template = "empty", batch = "vadsrijk_questrobot", timeoutTicks = 1200)
    public static void robotSpeeltAlles(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        QuestRobot.Rapport r = QuestRobot.speel(p, 6);
        try {
            RobotRapport.schrijf(r);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not write the robot report", e);
        }
        com.mojang.logging.LogUtils.getLogger().info("QUESTROBOT {} ms\n{}", r.ms, RobotRapport.samenvatting(r));
        helper.assertTrue(r.quests.size() > 100, "the robot saw the quests: " + r.quests.size());
        nl.juiced.guhs.feature.vadsrijk.npc.Questlog.wisAlles(p);
        helper.succeed();
    }
}
