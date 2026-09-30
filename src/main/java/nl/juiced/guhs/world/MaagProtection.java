package nl.juiced.guhs.world;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaBaasEntity;

/**
 * In the guh stomachs only the owner (and whoever the owner allows in the Maagenzym-guh's settings) may build, break
 * and use things. Visitors can look around. In the mouth nobody builds (except operators in creative).
 */
public final class MaagProtection {

    private static boolean denied(Player player, net.minecraft.core.BlockPos pos) {
        if (!MaagManager.isGuhmaag(player.level()) || MaagManager.mayBuild(player, pos)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.maag.no_build").withStyle(ChatFormatting.RED));
        }
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && denied(player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (MaagManager.isGuhmaag(event.getLevel()) && !event.getLevel().getBlockState(event.getPos()).is(net.minecraft.tags.BlockTags.ALL_SIGNS)
                && denied(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof GuhNpcEntity || event.getTarget() instanceof MikaBaasEntity) {
            return; // the stomach characters always talk to you
        }
        if (denied(event.getEntity(), event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (denied(event.getEntity(), event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }

    private MaagProtection() {
    }
}
