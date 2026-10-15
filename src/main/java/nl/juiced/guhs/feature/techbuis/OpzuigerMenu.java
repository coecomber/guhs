package nl.juiced.guhs.feature.techbuis;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Opzuiger's menu: its nine slots (3 x 3, where a dispenser has them) and your inventory. You can only take things
 * out: what goes in, the Opzuiger slurps up itself.
 */
public class OpzuigerMenu extends AbstractContainerMenu {
    private static final int VAKKEN = OpzuigerBlockEntity.VAKKEN;
    private final ContainerLevelAccess access;

    /** The client's side. */
    public OpzuigerMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStacksResourceHandler(VAKKEN), ContainerLevelAccess.NULL);
    }

    public OpzuigerMenu(int id, Inventory inventory, ItemStacksResourceHandler vakken, ContainerLevelAccess access) {
        super(TechbuisFeature.OPZUIGER_MENU.get(), id);
        this.access = access;
        for (int i = 0; i < VAKKEN; i++) {
            addSlot(new ResourceHandlerSlot(vakken, vakken::set, i, 62 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 84);
    }

    /** Shift-click: out of the Opzuiger into your inventory (never the other way). */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slotIndex >= VAKKEN || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack kopie = stack.copy();
        if (!moveItemStackTo(stack, VAKKEN, slots.size(), true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == kopie.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return kopie;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TechbuisFeature.OPZUIGER.get());
    }
}
