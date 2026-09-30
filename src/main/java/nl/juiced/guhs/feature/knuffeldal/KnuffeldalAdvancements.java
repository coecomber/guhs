package nl.juiced.guhs.feature.knuffeldal;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;

/** Grants the shown advancements of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;) that the game itself can't detect. */
public final class KnuffeldalAdvancements {
    public static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private KnuffeldalAdvancements() {
    }
}
