package nl.juiced.guhs.feature;

import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Loaned items ({@link Features#isLoaned}) stay with the player: they don't go into item frames, armour stands or allays,
 * and whatever still sits in a chest (or any other container) when it closes is gone (the guh who lent it gives a new one).
 * The guh backpack and the Bank Guh refuse them themselves.
 */
public final class Loaned {
    static void register() {
        // last: a feature that gives its own item back into your pockets (the Visguh's rod) goes first
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, Loaned::onContainerClose);
        NeoForge.EVENT_BUS.addListener(Loaned::onInteract);
        NeoForge.EVENT_BUS.addListener(Loaned::onInteractAt);
    }

    public static void onContainerClose(PlayerContainerEvent.Close event) {
        for (Slot slot : event.getContainer().slots) {
            if (slot.container != event.getEntity().getInventory() && Features.isLoaned(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }

    /** Would this entity keep the item? */
    private static boolean keeper(Entity target) {
        return target instanceof HangingEntity || target instanceof ArmorStand || target instanceof Allay;
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (Features.isLoaned(event.getItemStack()) && keeper(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (Features.isLoaned(event.getItemStack()) && keeper(event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private Loaned() {
    }
}
