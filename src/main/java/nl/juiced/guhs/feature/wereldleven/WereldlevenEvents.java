package nl.juiced.guhs.feature.wereldleven;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Game events of the wereldleven feature: a plushie you pick up goes into your knuffelkast. */
public final class WereldlevenEvents {
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            Knuffels.ontdek(player, event.getOriginalStack());
        }
    }

    private WereldlevenEvents() {
    }
}
