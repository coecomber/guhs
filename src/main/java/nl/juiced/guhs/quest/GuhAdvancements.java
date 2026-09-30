package nl.juiced.guhs.quest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;

/**
 * Hidden "quest" advancements (data/guhs/advancement/quest/*.json, no display): the mod grants them when something
 * happens that vanilla can't detect by itself (seeing a guh variant, a quest step...). FTB Quests' Guhs chapter
 * checks them with advancement tasks.
 */
public final class GuhAdvancements {
    public static void grant(ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("quest/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private GuhAdvancements() {
    }
}
