package nl.juiced.guhs.feature.techbezorg;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Haltepaaltje's menu: its filter as {@link Bezorgnet#FILTER} ghost slots (a click with an item puts a copy of ONE of it
 * in the slot and takes nothing; a click with an empty hand or a shift-click clears it; a shift-click on an item in your
 * inventory adds it to the filter), the player's inventory, and what the screen shows as container data (what happens here,
 * which Stepstation it belongs to and which stop it is). Buttons ({@link #clickMenuButton}): ophalen / afleveren, "Ander
 * station", and clearing the filter.
 * <p>
 * Slot indices: 0-8 the filter, 9-35 the inventory, 36-44 the hotbar.
 */
public class HalteMenu extends AbstractContainerMenu {
    // layout (shared with client.HalteScreen)
    public static final int BREED = 176, HOOG = 186;
    public static final int FILTER_X = 8, FILTER_Y = 66, INV_X = 8, INV_Y = 104;
    // container data
    public static final int D_OPHALEN = 0, D_GEKOPPELD = 1, D_DX = 2, D_DY = 3, D_DZ = 4, D_NUMMER = 5, D_AANTAL = 6, D_KIST = 7, DATA = 8;
    // buttons
    public static final int KNOP_SOORT = 0, KNOP_STATION = 1, KNOP_WIS = 2;

    private final BlockPos pos;
    @Nullable
    private final HaltepaaltjeBlockEntity halte;   // server side only
    private final ContainerData data;

    /** Server side. */
    public HalteMenu(int id, Inventory inventory, HaltepaaltjeBlockEntity halte) {
        this(id, inventory, halte.getBlockPos(), halte, halte.filterVakken(), new HalteData(halte));
    }

    /** Client side (the server sends the pole's position). */
    public HalteMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null, new SimpleContainer(Bezorgnet.FILTER), new SimpleContainerData(DATA));
    }

    private HalteMenu(int id, Inventory inventory, BlockPos pos, @Nullable HaltepaaltjeBlockEntity halte, Container filter, ContainerData data) {
        super(TechbezorgFeature.HALTE_MENU.get(), id);
        this.pos = pos;
        this.halte = halte;
        this.data = data;
        for (int i = 0; i < Bezorgnet.FILTER; i++) {
            addSlot(new SpookSlot(filter, i, FILTER_X + i * 18, FILTER_Y));
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(data);
    }

    /** A filter slot: it shows an item but never holds one (vanilla's own click handling may not touch it). */
    private static final class SpookSlot extends Slot {
        SpookSlot(Container filter, int index, int x, int y) {
            super(filter, index, x, y);
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

    // --- what the screen reads ---

    public BlockPos pos() {
        return pos;
    }

    public boolean ophalen() {
        return data.get(D_OPHALEN) != 0;
    }

    public boolean gekoppeld() {
        return data.get(D_GEKOPPELD) != 0;
    }

    public BlockPos station() {
        return pos.offset((short) data.get(D_DX), (short) data.get(D_DY), (short) data.get(D_DZ));
    }

    /** Which stop of its station's round this is (1-based; 0: none). */
    public int nummer() {
        return data.get(D_NUMMER);
    }

    public int aantal() {
        return data.get(D_AANTAL);
    }

    public boolean heeftKist() {
        return data.get(D_KIST) != 0;
    }

    public boolean filterLeeg() {
        for (int i = 0; i < Bezorgnet.FILTER; i++) {
            if (slots.get(i).hasItem()) {
                return false;
            }
        }
        return true;
    }

    // --- clicks ---

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < Bezorgnet.FILTER) {
            if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) {
                ItemStack hand = input == ContainerInput.QUICK_MOVE ? ItemStack.EMPTY : getCarried();
                slots.get(slotIndex).set(hand.isEmpty() ? ItemStack.EMPTY : hand.copyWithCount(1));
            }
            return;   // (dragging over it, number keys, pick-all: nothing)
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    /** A shift-click on an item in the inventory: that kind of item goes into the first free filter slot. Nothing moves. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < Bezorgnet.FILTER) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int vrij = -1;
        for (int i = 0; i < Bezorgnet.FILTER; i++) {
            ItemStack f = slots.get(i).getItem();
            if (ItemStack.isSameItem(f, stack)) {
                return ItemStack.EMPTY;   // (already in the filter)
            }
            if (f.isEmpty() && vrij < 0) {
                vrij = i;
            }
        }
        if (vrij >= 0) {
            slots.get(vrij).set(stack.copyWithCount(1));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index >= Bezorgnet.FILTER;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (halte == null || halte.isRemoved() || !(halte.getLevel() instanceof ServerLevel sl)) {
            return false;
        }
        switch (id) {
            case KNOP_SOORT -> halte.zetOphalen(!halte.ophalen());
            case KNOP_STATION -> halte.volgendStation(sl);
            case KNOP_WIS -> halte.wisFilter();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (halte == null) {
            return true;
        }
        return !halte.isRemoved() && player.level() == halte.getLevel()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /** The server's numbers, straight from the pole. */
    private record HalteData(HaltepaaltjeBlockEntity halte) implements ContainerData {
        @Override
        public int get(int index) {
            StepstationBlockEntity thuis = halte.thuis();
            BlockPos s = thuis == null ? halte.getBlockPos() : thuis.getBlockPos();
            BlockPos hier = halte.getBlockPos();
            return switch (index) {
                case D_OPHALEN -> halte.ophalen() ? 1 : 0;
                case D_GEKOPPELD -> thuis != null ? 1 : 0;
                case D_DX -> s.getX() - hier.getX();
                case D_DY -> s.getY() - hier.getY();
                case D_DZ -> s.getZ() - hier.getZ();
                case D_NUMMER -> thuis == null ? 0 : thuis.nummer(hier) + 1;
                case D_AANTAL -> thuis == null ? 0 : thuis.haltes().size();
                case D_KIST -> halte.heeftKist() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA;
        }
    }
}
