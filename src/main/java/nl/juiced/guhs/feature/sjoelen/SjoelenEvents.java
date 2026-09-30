package nl.juiced.guhs.feature.sjoelen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps guh-sjoelen fair and tidy: players at the sjoelbak can't get hurt, and the loaned pucks never leave the game (no
 * dropping, gone when you log out, die or change dimension; storage is handled by feature.Loaned via the tag guhs:loaned).
 */
public final class SjoelenEvents {

    private static boolean isPucks(ItemStack stack) {
        return stack.is(SjoelenFeature.SCHIJVEN.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && SjoelGame.isPlaying(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (isPucks(event.getEntity().getItem())) {
            event.setCanceled(true);
            if (SjoelGame.isPlaying(event.getPlayer())) {
                event.getPlayer().getInventory().add(event.getEntity().getItem().copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SjoelGame.isPlaying(player)) {
            SjoelGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> isPucks(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SjoelGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SjoelGame.isPlaying(event.getEntity())) {
            SjoelGame.removeStacks(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SjoelGame.leave(player);
        }
    }

    /** With the pucks in hand, blocks with contents don't react (the slide still works). */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isPucks(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    /** A player whose Opoe isn't there any more (her chunk unloaded): the turn is over. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && SjoelGame.isPlaying(player)
                && !SjoelGame.hasOpoeNearby(player)) {
            SjoelGame.leave(player);
        }
    }

    private SjoelenEvents() {
    }
}
