package nl.juiced.guhs.feature.techbezorg;

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
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * The Stepstation's menu: the backpack ({@link Bezorgnet#RUGZAK} slots, only open while the guhtje is home or was whistled
 * to the player: {@link StepstationBlockEntity#rugzakOpen}), the player's inventory, and the numbers the screen draws (what
 * the guhtje is doing, and per stop where it is and what happens there), synced as container data (small numbers: the
 * stops are sent relative to the station). The buttons of the screen are vanilla menu buttons ({@link #clickMenuButton}):
 * stop i earlier / later in the round, and "Naar huis!".
 * <p>
 * Slot indices: 0-8 the backpack, 9-35 the inventory, 36-44 the hotbar.
 */
public class StepstationMenu extends AbstractContainerMenu {
    // layout (shared with client.StepstationScreen)
    public static final int BREED = 230, HOOG = 244;
    public static final int RUGZAK_X = 9, RUGZAK_Y = 53, INV_X = 35, INV_Y = 162;
    // container data
    public static final int D_FASE = 0, D_DOEL = 1, D_AANTAL = 2, D_OPEN = 3, D_KRACHT = 4, D_VAST = 5, D_KOERIER = 6, D_HALTE = 7, PER_HALTE = 4;
    public static final int DATA = D_HALTE + Bezorgnet.MAX_HALTES * PER_HALTE;
    /** Bits of a stop's fourth number. */
    public static final int V_OPHALEN = 1, V_KIST = 2, V_BEREIKBAAR = 4;
    // buttons: stop i -> i * 4 + EERDER / LATER
    public static final int EERDER = 0, LATER = 1, KNOP_NAAR_HUIS = 100;

    private final BlockPos pos;
    @Nullable
    private final StepstationBlockEntity station;   // server side only
    private final ContainerData data;

    /** Server side. */
    public StepstationMenu(int id, Inventory inventory, StepstationBlockEntity station) {
        this(id, inventory, station.getBlockPos(), station, station.vakken(), new StationData(station));
    }

    /** Client side (the server sends the station's position). */
    public StepstationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null, new ItemStacksResourceHandler(Bezorgnet.RUGZAK), new SimpleContainerData(DATA));
    }

    private StepstationMenu(int id, Inventory inventory, BlockPos pos, @Nullable StepstationBlockEntity station,
                            ItemStacksResourceHandler rugzak, ContainerData data) {
        super(TechbezorgFeature.STEPSTATION_MENU.get(), id);
        this.pos = pos;
        this.station = station;
        this.data = data;
        for (int i = 0; i < Bezorgnet.RUGZAK; i++) {
            addSlot(new RugzakSlot(rugzak, i, RUGZAK_X + (i % 3) * 18, RUGZAK_Y + (i / 3) * 18));
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(data);
    }

    /** A slot of the backpack: shut while the guhtje is on the road. */
    private final class RugzakSlot extends ResourceHandlerSlot {
        RugzakSlot(ItemStacksResourceHandler rugzak, int index, int x, int y) {
            super(rugzak, rugzak::set, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return open() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return open() && super.mayPickup(player);
        }
    }

    // --- what the screen reads ---

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    StepstationBlockEntity station() {
        return station;
    }

    public StepstationBlockEntity.Fase fase() {
        StepstationBlockEntity.Fase[] alle = StepstationBlockEntity.Fase.values();
        int i = data.get(D_FASE);
        return alle[i < 0 || i >= alle.length ? 0 : i];
    }

    /** The stop the guhtje is riding to or busy at (0-based), or -1. */
    public int doel() {
        return (short) data.get(D_DOEL);
    }

    public int aantal() {
        return Math.max(0, Math.min(Bezorgnet.MAX_HALTES, data.get(D_AANTAL)));
    }

    /** May the player reach into the backpack right now? */
    public boolean open() {
        return data.get(D_OPEN) != 0;
    }

    public boolean kracht() {
        return data.get(D_KRACHT) != 0;
    }

    /** The backpack did not get empty in the last round. */
    public boolean vast() {
        return data.get(D_VAST) != 0;
    }

    /** Is the guhtje there (false: it is being looked for / remade)? */
    public boolean koerier() {
        return data.get(D_KOERIER) != 0;
    }

    public BlockPos halte(int i) {
        int d = D_HALTE + i * PER_HALTE;
        return pos.offset((short) data.get(d), (short) data.get(d + 1), (short) data.get(d + 2));
    }

    public boolean ophalen(int i) {
        return (data.get(D_HALTE + i * PER_HALTE + 3) & V_OPHALEN) != 0;
    }

    public boolean heeftKist(int i) {
        return (data.get(D_HALTE + i * PER_HALTE + 3) & V_KIST) != 0;
    }

    public boolean bereikbaar(int i) {
        return (data.get(D_HALTE + i * PER_HALTE + 3) & V_BEREIKBAAR) != 0;
    }

    // --- the buttons ---

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (station == null || station.isRemoved()) {
            return false;
        }
        if (id == KNOP_NAAR_HUIS) {
            station.naarHuis();
            return true;
        }
        int i = id / 4;
        if (id < 0 || i >= station.haltes().size()) {
            return false;
        }
        return switch (id % 4) {
            case EERDER -> station.schuif(i, -1);
            case LATER -> station.schuif(i, 1);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        // (a shut backpack takes and gives nothing: vanilla's merge into a filled slot does not ask the slot)
        if (!slot.hasItem() || !slot.mayPickup(player) || !open()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack kopie = stack.copy();
        boolean gelukt = index < Bezorgnet.RUGZAK
                ? moveItemStackTo(stack, Bezorgnet.RUGZAK, slots.size(), true)
                : moveItemStackTo(stack, 0, Bezorgnet.RUGZAK, false);
        if (!gelukt) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setByPlayer(stack);
        }
        if (stack.getCount() == kopie.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return kopie;
    }

    /** Near the station, or near its guhtje (it came to the player with the whistle). */
    @Override
    public boolean stillValid(Player player) {
        if (station == null) {
            return true;
        }
        if (station.isRemoved() || player.level() != station.getLevel()) {
            return false;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
            return true;
        }
        BezorgguhtjeEntity k = station.koerier();
        return station.fase() == StepstationBlockEntity.Fase.BIJ_SPELER && k != null && k.distanceToSqr(player) <= 64.0;
    }

    /** The server's numbers, straight from the station. */
    private record StationData(StepstationBlockEntity station) implements ContainerData {
        @Override
        public int get(int index) {
            switch (index) {
                case D_FASE:
                    return station.fase().ordinal();
                case D_DOEL:
                    return station.doel();
                case D_AANTAL:
                    return station.haltes().size();
                case D_OPEN:
                    return station.rugzakOpen() ? 1 : 0;
                case D_KRACHT:
                    return station.heeftKracht() ? 1 : 0;
                case D_VAST:
                    return station.snoet() == nl.juiced.guhs.feature.vadskracht.Snoet.VOL ? 1 : 0;
                case D_KOERIER:
                    return station.koerier() != null ? 1 : 0;
                default:
                    break;
            }
            int i = (index - D_HALTE) / PER_HALTE;
            if (index < D_HALTE || i >= station.haltes().size()) {
                return 0;
            }
            BlockPos halte = station.haltes().get(i);
            BlockPos hier = station.getBlockPos();
            switch ((index - D_HALTE) % PER_HALTE) {
                case 0:
                    return halte.getX() - hier.getX();
                case 1:
                    return halte.getY() - hier.getY();
                case 2:
                    return halte.getZ() - hier.getZ();
                default:
                    int vlaggen = 0;
                    if (station.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
                        HaltepaaltjeBlockEntity h = station.halte(sl, i);
                        if (h != null) {
                            vlaggen |= V_BEREIKBAAR;
                            if (h.heeftKist()) {
                                vlaggen |= V_KIST;
                            }
                        }
                        if (sl.isLoaded(halte) && sl.getBlockEntity(halte) instanceof HaltepaaltjeBlockEntity paal && paal.ophalen()) {
                            vlaggen |= V_OPHALEN;
                        }
                    }
                    return vlaggen;
            }
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
