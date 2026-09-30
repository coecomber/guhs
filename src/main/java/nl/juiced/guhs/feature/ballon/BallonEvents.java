package nl.juiced.guhs.feature.ballon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * No jumping out of the basket halfway (sneaking doesn't get you off during a flight: "Njeg!"), and whoever logs out
 * during a flight lands at once (the balloon goes home, the player stands on the steiger again).
 */
public final class BallonEvents {
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof LuchtballonEntity ballon
                && ballon.vliegt() && !ballon.uitstappen && event.getEntityMounting() instanceof ServerPlayer player && player.isAlive()
                && !player.isRemoved() && player.isShiftKeyDown()) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("gui.guhs.ballon.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getVehicle() instanceof LuchtballonEntity ballon && ballon.vliegt()) {
            ballon.land(false);
        }
    }

    private BallonEvents() {
    }
}
