package nl.juiced.guhs.feature.doolhof;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The advancements of the doolhofspelen slice: the hidden quest one (guhs:quest/&lt;name&gt;, for FTB) and, when there
 * is one, the visible one in the tab De Grote Guhspelen (guhs:grote_guhspelen/&lt;name&gt;).
 */
public final class Adv {
    public static void grant(ServerPlayer player, String name) {
        GuhAdvancements.grant(player, name);
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        if (holder == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private Adv() {
    }
}
