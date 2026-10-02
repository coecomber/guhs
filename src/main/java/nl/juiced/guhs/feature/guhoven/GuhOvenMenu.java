package nl.juiced.guhs.feature.guhoven;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The Guhoven's menu: a furnace menu (input, result, the furnace recipe book) whose fuel slot is closed: it is hidden, takes
 * nothing, and shift-clicking never sends anything there (non-smeltables just hop between inventory and hotbar).
 */
public class GuhOvenMenu extends AbstractFurnaceMenu {
    public GuhOvenMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public GuhOvenMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(GuhovenFeature.GUH_OVEN_MENU.get(), RecipeType.SMELTING, RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE,
                containerId, inventory, container, data);
        Slot dicht = new GeenBrandstof(container);
        dicht.index = FUEL_SLOT;
        this.slots.set(FUEL_SLOT, dicht);
    }

    /** Is the oven baking (on guh power) right now? */
    public boolean bakt() {
        return isLit();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slotIndex < 3 || !slot.hasItem() || canSmelt(slot.getItem())) {
            return super.quickMoveStack(player, slotIndex);
        }
        // not smeltable (coal too): never to the closed fuel slot, just between inventory and hotbar
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = slotIndex < 30 ? moveItemStackTo(stack, 30, 39, false) : moveItemStackTo(stack, 3, 30, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    /** The closed fuel slot: off-screen, inactive, takes nothing. */
    private static final class GeenBrandstof extends Slot {
        GeenBrandstof(Container container) {
            super(container, FUEL_SLOT, -10000, -10000);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
