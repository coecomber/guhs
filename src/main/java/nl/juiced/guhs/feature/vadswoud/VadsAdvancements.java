package nl.juiced.guhs.feature.vadswoud;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;

/** Granting the Vadswoud's advancements: hidden quest ones (for FTB Quests) and shown ones that the mod decides. */
final class VadsAdvancements {
    /** A hidden quest advancement (data/guhs/advancement/quest/&lt;name&gt;.json). */
    static void grant(ServerPlayer player, String name) {
        GuhAdvancements.grant(player, name);
    }

    /** A shown advancement with an "impossible" criterion "done" (for example guhmension/vadswoud_nestje). */
    static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private VadsAdvancements() {
    }
}
