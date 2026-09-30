package nl.juiced.guhs.feature.katapult;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps the Knabbelkatapult fair and tidy: players at the catapult can't get hurt, and the loaned pluisballen never leave the game (no
 * dropping, gone when you log out, die or change dimension; storage is handled by feature.Loaned via the tag guhs:loaned).
 */
public final class KatapultEvents {

    private static boolean isBalls(ItemStack stack) {
        return stack.is(KatapultFeature.PLUISBALLEN.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && KatapultGame.isPlaying(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (isBalls(event.getEntity().getItem())) {
            event.setCanceled(true);
            if (KatapultGame.isPlaying(event.getPlayer())) {
                event.getPlayer().getInventory().add(event.getEntity().getItem().copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KatapultGame.isPlaying(player)) {
            KatapultGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> isBalls(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KatapultGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!KatapultGame.isPlaying(event.getEntity())) {
            KatapultGame.removeStacks(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KatapultGame.leave(player);
        }
    }

    /** With the pluisballen in hand, blocks with contents don't react (the shot still works). */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBalls(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    /** A player whose Kapitein isn't there any more (his chunk unloaded): the run is over. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && KatapultGame.isPlaying(player)
                && !KatapultGame.hasKapiteinNearby(player)) {
            KatapultGame.leave(player);
        }
    }

    private KatapultEvents() {
    }
}
