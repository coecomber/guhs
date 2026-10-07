package nl.juiced.guhs.feature.einde2.mikas;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The menu of a {@link SchuildoosBlock}: three rows of nine above the slots of the player (the layout of a shulker box).
 * Its slots refuse anything that may not go inside a container item: another Schuildoos, a shulker box.
 */
public class SchuildoosMenu extends AbstractContainerMenu {
    private final Container container;

    /** (the copy on the client) */
    public SchuildoosMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SchuildoosBlockEntity.SLOTS));
    }

    public SchuildoosMenu(int id, Inventory inventory, Container container) {
        super(E2MikasFeature.SCHUILDOOS_MENU.get(), id);
        checkContainerSize(container, SchuildoosBlockEntity.SLOTS);
        this.container = container;
        container.startOpen(inventory.player);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new ShulkerBoxSlot(container, x + y * 9, 8 + x * 18, 18 + y * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 84);
    }

    public Container container() {
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (index < container.getContainerSize()) {
                if (!moveItemStackTo(stack, container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, container.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
