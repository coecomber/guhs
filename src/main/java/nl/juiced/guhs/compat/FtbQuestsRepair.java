package nl.juiced.guhs.compat;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.1.3: ticks off Guhs quest tasks that FTB Quests left stuck. In a "flexible" chapter FTB Quests remembers the progress of
 * a task whose quest still waits for its dependencies, but only marks it done through a chain that runs at the moment the
 * dependency is completed; submitting the same progress again does nothing. So in worlds from before 1.1.3 (when every
 * quest still hung behind "Guh!"), a seen variant could stay at "1/1, not done" forever. Since 1.1.3 our quests have no
 * dependencies (only the stomach sizes), and this finishes what was already full: 5 s after joining, then every 2 minutes.
 * Only loaded when FTB Quests is installed (see Guhs).
 */
public final class FtbQuestsRepair {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String OURS = "475548";   // all our quest ids start with "GUH" in hex (tools/make_ftbquests.py)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 2400 == 100) {
            repair(player);
        }
    }

    /** Returns how many tasks it ticked off. */
    public static int repair(ServerPlayer player) {
        ServerQuestFile file = ServerQuestFile.getInstance();
        if (file == null) {
            return 0;
        }
        TeamData data = file.getTeamData(player).orElse(null);
        if (data == null || data.isLocked()) {
            return 0;
        }
        int done = 0;
        for (Quest quest : file.collect(Quest.class, q -> QuestObjectBase.getCodeString(q).startsWith(OURS))) {
            if (data.isCompleted(quest) || !data.areDependenciesComplete(quest)) {
                continue;
            }
            for (Task task : quest.getTasksAsList()) {
                if (!data.isCompleted(task) && data.getProgress(task) >= task.getMaxProgress()) {
                    data.markTaskCompleted(task);
                    done++;
                }
            }
        }
        if (done > 0) {
            LOGGER.info("FTB Quests: ticked off {} stuck Guhs quest task(s) for {}", done, player.getName().getString());
        }
        return done;
    }

    private FtbQuestsRepair() {
    }
}
