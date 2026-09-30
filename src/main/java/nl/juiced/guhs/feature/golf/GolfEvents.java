package nl.juiced.guhs.feature.golf;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.util.TriState;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * Keeps guh golf fair and tidy: golfers can't get hurt, and the loaned club never leaves the game (no dropping, no
 * chests, no item frames; gone when you log out, die or change dimension).
 */
public final class GolfEvents {

    private static boolean isClub(ItemStack stack) {
        return stack.is(GolfFeature.GOLFCLUB.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && GolfGame.isGolfing(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** (Normally the club refuses to be dropped at all; if something drops it anyway, it goes straight back.) */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (isClub(stack)) {
            event.setCanceled(true);
            if (GolfGame.isGolfing(event.getPlayer())) {
                event.getPlayer().getInventory().add(stack.copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GolfGame.isGolfing(player)) {
            GolfGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> isClub(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GolfGame.leave(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!GolfGame.isGolfing(event.getEntity())) {
            GolfGame.removeClubs(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GolfGame.leave(player);
        }
    }

    /** Whatever got put into a chest, a guh's backpack...: a club in it disappears (the golfer gets a new one). */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        for (Slot slot : event.getContainer().slots) {
            if (slot.container != event.getEntity().getInventory() && isClub(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }

    /**
     * No putting the club into a block that keeps things without a screen (a decorated pot, a shelf...): with the club in
     * hand, blocks with contents don't react (the swing still works).
     */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isClub(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    /** No hanging the club in an item frame, giving it to an armour stand or an allay... */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (isClub(event.getItemStack()) && !(event.getTarget() instanceof GuhNpcEntity) && !(event.getTarget() instanceof GolfBallEntity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isClub(event.getItemStack()) && !(event.getTarget() instanceof GuhNpcEntity) && !(event.getTarget() instanceof GolfBallEntity)) {
            event.setCanceled(true);
        }
    }

    /** A golfer whose Golfguh isn't there any more (her chunk unloaded, she's gone): the round is over. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && GolfGame.isGolfing(player)
                && !GolfGame.hasGolfguhNearby(player)) {
            GolfGame.leave(player);
        }
    }

    private GolfEvents() {
    }
}
