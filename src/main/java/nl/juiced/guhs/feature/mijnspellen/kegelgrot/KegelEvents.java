package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps guh bowling fair and tidy: bowlers can't get hurt, and the loaned wool ball never leaves the game (no dropping,
 * gone when you log out, die or change dimension; storage is handled by feature.Loaned via the tag guhs:loaned).
 */
public final class KegelEvents {
    private static boolean isBal(ItemStack stack) {
        return stack.is(KegelgrotFeature.WOLBAL.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && KegelGame.isPlaying(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (isBal(event.getEntity().getItem())) {
            event.setCanceled(true);
            if (KegelGame.isPlaying(event.getPlayer())) {
                event.getPlayer().getInventory().add(event.getEntity().getItem().copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KegelGame.isPlaying(player)) {
            KegelGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> isBal(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KegelGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!KegelGame.isPlaying(event.getEntity())) {
            KegelGame.removeBal(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KegelGame.leave(player);
        }
    }

    /** With the ball in hand, blocks with contents don't react (the throw still works). */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBal(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && KegelGame.isPlaying(player)
                && !KegelGame.hasGuhdeNearby(player)) {
            KegelGame.leave(player);
        }
    }

    private KegelEvents() {
    }
}
