package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The menu of every machine of this slice: the machine's slots where {@link MachineSoort} puts them, the player's
 * inventory below, and four numbers (progress, its maximum, vadskracht, the machine's own state). Out slots take nothing
 * from a player; in slots only what the machine wants ({@link TechBlockEntity#magErin}). Shift-click moves between the
 * machine and the inventory.
 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int GEGEVENS = 4;
    public static final int INV_X = 8, INV_Y = 84;

    public final MachineSoort soort;
    public final BlockPos pos;
    private final ContainerData gegevens;
    /** (server) the machine; null on the client. */
    @Nullable
    private final TechBlockEntity machine;

    /** Client side: the layout and the spot come with the packet, the slots are filled by the server. */
    public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readEnum(MachineSoort.class), buf.readBlockPos(), null, null, new SimpleContainerData(GEGEVENS));
    }

    /** Server side. */
    public MachineMenu(int id, Inventory inventory, TechBlockEntity machine, ContainerData gegevens) {
        this(id, inventory, machine.soort(), machine.getBlockPos(), machine, machine.vakken(), gegevens);
    }

    private MachineMenu(int id, Inventory inventory, MachineSoort soort, BlockPos pos, @Nullable TechBlockEntity machine,
                        @Nullable ItemStacksResourceHandler vakken, ContainerData gegevens) {
        super(TechmachineFeature.MACHINE_MENU.get(), id);
        this.soort = soort;
        this.pos = pos;
        this.machine = machine;
        this.gegevens = gegevens;
        ItemStacksResourceHandler inhoud = vakken != null ? vakken : new ItemStacksResourceHandler(soort.vakken);
        for (int i = 0; i < soort.vakken; i++) {
            addSlot(new Vak(inhoud, i));
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(gegevens);
    }

    /** A slot of the machine. */
    private final class Vak extends ResourceHandlerSlot {
        private final int vak;

        Vak(ItemStacksResourceHandler inhoud, int vak) {
            super(inhoud, inhoud::set, vak, soort.x(vak), soort.y(vak));
            this.vak = vak;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return machine != null ? machine.magErin(vak, ItemResource.of(stack)) : soort.lijkt(vak, stack, slots.get(0).getItem());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // (a Bouwtekening slot holds one drawing)
            return soort == MachineSoort.KNUTSELMACHINE && vak == 0 ? 1 : super.getMaxStackSize(stack);
        }

        @Override
        public int getMaxStackSize() {
            return soort == MachineSoort.KNUTSELMACHINE && vak == 0 ? 1 : super.getMaxStackSize();
        }
    }

    // --- what the screen reads ---------------------------------------------------------------------------------------

    public int voortgang() {
        return gegevens.get(0);
    }

    public int duur() {
        return gegevens.get(1);
    }

    public boolean kracht() {
        return gegevens.get(2) != 0;
    }

    public int stand() {
        return gegevens.get(3);
    }

    // --- moving items ------------------------------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack kopie = stack.copy();
        int n = soort.vakken;
        if (index < n) {
            if (!moveItemStackTo(stack, n, n + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, n, false)) {
            // nothing for the machine: between the inventory and the hotbar
            boolean verplaatst = index < n + 27 ? moveItemStackTo(stack, n + 27, n + 36, false) : moveItemStackTo(stack, n, n + 27, false);
            if (!verplaatst) {
                return ItemStack.EMPTY;
            }
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
        return machine == null || machine.dichtbij(player);
    }
}
