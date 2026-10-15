package nl.juiced.guhs.feature.techbuis;

import javax.annotation.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The menu of a Filterstuk (nine examples) and of a Voorraadmeter (one example): the list of a {@link BuisFilter} as ghost
 * slots, its three settings as menu data, and your inventory. Nothing real ever goes into a ghost slot: clicking one with
 * an item in your hand copies the item as an example, clicking it with an empty hand clears it, and shift-clicking
 * something in your inventory adds it to the list. The buttons of the screen ({@code client.FilterScreen}) arrive as
 * {@link #clickMenuButton}.
 */
public class FilterMenu extends AbstractContainerMenu {
    public static final int VAKKEN_FILTER = 9, VAKKEN_METER = 1;
    /** Where things are on the screen (176 wide like a chest screen, and a row higher: {@link #HOOGTE}). */
    public static final int HOOGTE = 184, FILTER_X = 8, FILTER_Y = 18, METER_X = 26, METER_Y = 36, INV_X = 8, INV_Y = 102;
    public static final int KNOP_BEHALVE = 0, KNOP_PRECIES = 1, KNOP_MIN_1 = 2, KNOP_PLUS_1 = 3, KNOP_MIN_8 = 4, KNOP_PLUS_8 = 5,
            KNOP_MIN_64 = 6, KNOP_PLUS_64 = 7;

    private final Container voorbeelden;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    @Nullable
    private final Block block;
    private final int vakken;

    /** The client's side: an empty list that the server fills. */
    public FilterMenu(MenuType<?> type, int id, Inventory inventory, int vakken) {
        this(type, id, inventory, new SimpleContainer(vakken), new SimpleContainerData(BuisFilter.DATA_AANTAL), ContainerLevelAccess.NULL, null);
    }

    public FilterMenu(MenuType<?> type, int id, Inventory inventory, Container voorbeelden, ContainerData data, ContainerLevelAccess access,
                      @Nullable Block block) {
        super(type, id);
        this.voorbeelden = voorbeelden;
        this.data = data;
        this.access = access;
        this.block = block;
        this.vakken = voorbeelden.getContainerSize();
        if (vakken == VAKKEN_METER) {
            addSlot(new Voorbeeld(voorbeelden, 0, METER_X, METER_Y));
        } else {
            for (int i = 0; i < vakken; i++) {
                addSlot(new Voorbeeld(voorbeelden, i, FILTER_X + (i % 3) * 18, FILTER_Y + (i / 3) * 18));
            }
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(data);
    }

    /** How many examples the list holds (9: a Filterstuk, 1: a Voorraadmeter). */
    public int vakken() {
        return vakken;
    }

    public boolean behalve() {
        return data.get(BuisFilter.DATA_BEHALVE) != 0;
    }

    public boolean precies() {
        return data.get(BuisFilter.DATA_PRECIES) != 0;
    }

    public int getal() {
        return data.get(BuisFilter.DATA_GETAL);
    }

    @Override
    public boolean clickMenuButton(Player player, int knop) {
        switch (knop) {
            case KNOP_BEHALVE -> data.set(BuisFilter.DATA_BEHALVE, behalve() ? 0 : 1);
            case KNOP_PRECIES -> data.set(BuisFilter.DATA_PRECIES, precies() ? 0 : 1);
            case KNOP_MIN_1 -> zetGetal(getal() - 1);
            case KNOP_PLUS_1 -> zetGetal(getal() + 1);
            case KNOP_MIN_8 -> zetGetal(getal() - 8);
            case KNOP_PLUS_8 -> zetGetal(getal() + 8);
            case KNOP_MIN_64 -> zetGetal(getal() - 64);
            case KNOP_PLUS_64 -> zetGetal(getal() + 64);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void zetGetal(int n) {
        data.set(BuisFilter.DATA_GETAL, Math.max(0, Math.min(BuisFilter.MAX_GETAL, n)));
    }

    /** A ghost slot: the item in your hand becomes the example (you keep the item); an empty hand or shift clears it. */
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < vakken) {
            if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) {
                ItemStack hand = getCarried();
                voorbeelden.setItem(slotIndex, input == ContainerInput.QUICK_MOVE || hand.isEmpty() ? ItemStack.EMPTY : hand.copyWithCount(1));
                broadcastChanges();
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    /** Shift-click in your inventory: the item is added to the list as an example (it stays where it is). */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex >= vakken && slotIndex < slots.size()) {
            ItemStack stack = slots.get(slotIndex).getItem();
            if (!stack.isEmpty()) {
                boolean staatErAl = false;
                int vrij = -1;
                for (int i = 0; i < vakken; i++) {
                    ItemStack voorbeeld = voorbeelden.getItem(i);
                    if (voorbeeld.isEmpty()) {
                        vrij = vrij < 0 ? i : vrij;
                    } else if (ItemStack.isSameItemSameComponents(voorbeeld, stack)) {
                        staatErAl = true;
                    }
                }
                if (!staatErAl && vrij >= 0) {
                    voorbeelden.setItem(vrij, stack.copyWithCount(1));
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return block == null || stillValid(access, player, block);
    }

    /** An example: shows an item, holds nothing. */
    private static final class Voorbeeld extends Slot {
        Voorbeeld(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
