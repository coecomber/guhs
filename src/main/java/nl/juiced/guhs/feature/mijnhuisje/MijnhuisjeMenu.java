package nl.juiced.guhs.feature.mijnhuisje;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Mijn-guhhuisje's menu: the two slots (kaasknabbels, Banksleutel), your inventory, the numbers of {@link
 * MijnhuisjeBlockEntity#data} and the buttons ({@link #clickMenuButton}, vanilla menu buttons, only for the owner):
 * <ul>
 *   <li>{@link #KNOP_RX} + n, {@link #KNOP_RZ} + n: n blocks to each side (sliders), {@link #KNOP_BODEM} + y: down to y;</li>
 *   <li>{@link #KNOP_TOON}: "laat zien" (the pink outline), {@link #KNOP_AAN}: work on/off, {@link #KNOP_RUGZAK}: the
 *       Rugzakzadels into the chest, {@link #KNOP_UIT} + i: resident i moves out (the menu opens again).</li>
 * </ul>
 * What the client knows at the start (the open buffer): the position, the settings, the y range, the resident names.
 */
public class MijnhuisjeMenu extends AbstractContainerMenu {
    public static final int DATA = 12;
    public static final int KNOP_RX = 0, KNOP_RZ = 100, KNOP_TOON = 50, KNOP_AAN = 51, KNOP_RUGZAK = 52, KNOP_UIT = 60, KNOP_BODEM = 1000;
    /** The y range a slider can send (KNOP_BODEM + y + BODEM_VERSCHUIF). */
    public static final int BODEM_VERSCHUIF = 100;
    public static final int BREED = 176, HOOG = 244, VAK_X = 8, VAK_Y = 102, INV_X = 8, INV_Y = 162;

    private final Container vakken;
    private final ContainerData data;
    @Nullable
    private final MijnhuisjeBlockEntity be;
    public final BlockPos pos;
    public final int startRx, startRz, startBodem;
    public final List<String> namen;

    /** The client's side (from the open buffer). */
    public static MijnhuisjeMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int rx = buf.readVarInt(), rz = buf.readVarInt(), bodem = buf.readInt();
        int n = buf.readVarInt();
        List<String> namen = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            namen.add(buf.readUtf(64));
        }
        return new MijnhuisjeMenu(id, inventory, pos, null, new SimpleContainer(MijnhuisjeBlockEntity.VAKKEN), new SimpleContainerData(DATA),
                rx, rz, bodem, namen);
    }

    /** The server's side. */
    public MijnhuisjeMenu(int id, Inventory inventory, MijnhuisjeBlockEntity be) {
        this(id, inventory, be.getBlockPos(), be, be.vakken, be.data, be.gebied().rx(), be.gebied().rz(), be.gebied().bodem(), be.namen());
    }

    /** What the server writes into the open buffer (read by {@link #client}). */
    public static void schrijf(RegistryFriendlyByteBuf buf, MijnhuisjeBlockEntity be) {
        buf.writeBlockPos(be.getBlockPos());
        MijnGebied g = be.gebied();
        buf.writeVarInt(g.rx());
        buf.writeVarInt(g.rz());
        buf.writeInt(g.bodem());
        List<String> namen = be.namen();
        buf.writeVarInt(namen.size());
        for (String n : namen) {
            buf.writeUtf(n.length() > 64 ? n.substring(0, 64) : n, 64);
        }
    }

    private MijnhuisjeMenu(int id, Inventory inventory, BlockPos pos, @Nullable MijnhuisjeBlockEntity be, Container vakken, ContainerData data,
                           int rx, int rz, int bodem, List<String> namen) {
        super(MijnhuisjeFeature.MENU.get(), id);
        this.pos = pos;
        this.be = be;
        this.vakken = vakken;
        this.data = data;
        this.startRx = rx;
        this.startRz = rz;
        this.startBodem = bodem;
        this.namen = List.copyOf(namen);
        for (int i = 0; i < MijnhuisjeBlockEntity.VAKKEN; i++) {
            final int vak = i;
            addSlot(new Slot(vakken, i, VAK_X, VAK_Y + i * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return MijnhuisjeBlockEntity.magInVak(vak, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return vak == MijnhuisjeBlockEntity.VAK_SLEUTEL ? 1 : 64;
                }
            });
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(data);
    }

    public int rx() {
        return data.get(0);
    }

    public int rz() {
        return data.get(1);
    }

    public int bodem() {
        return data.get(2);
    }

    public int status() {
        return data.get(3);
    }

    public int aantal() {
        return data.get(4);
    }

    public int werkers() {
        return data.get(5);
    }

    public int gemijnd() {
        return data.get(6);
    }

    public int rugzak() {
        return data.get(7);
    }

    public int boven() {
        return data.get(8);
    }

    public boolean vlag(int bit) {
        return (data.get(9) & bit) != 0;
    }

    public int minY() {
        return data.get(10);
    }

    public int laag() {
        return data.get(11);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp) || !be.mag(player)) {
            return false;
        }
        MijnGebied g = be.gebied();
        if (id >= KNOP_BODEM) {
            be.stel(g.rx(), g.rz(), id - KNOP_BODEM - BODEM_VERSCHUIF);
        } else if (id >= KNOP_RZ && id <= KNOP_RZ + MijnGebied.MAX_ZIJ) {
            be.stel(g.rx(), id - KNOP_RZ, g.bodem());
        } else if (id >= KNOP_RX && id <= KNOP_RX + MijnGebied.MAX_ZIJ) {
            be.stel(id - KNOP_RX, g.rz(), g.bodem());
        } else if (id == KNOP_TOON) {
            be.laatZien();
        } else if (id == KNOP_AAN) {
            be.zetAan(!be.aan());
        } else if (id == KNOP_RUGZAK) {
            be.rugzakNaarKist();
        } else if (id >= KNOP_UIT && id < KNOP_UIT + MijnhuisjeBlockEntity.MAX_BEWONERS) {
            List<java.util.UUID> b = be.bewoners();
            int i = id - KNOP_UIT;
            if (i < b.size()) {
                be.trekUit(b.get(i));
                sp.level().getServer().execute(() -> MijnhuisjeBlock.open(sp, be));   // (fresh names)
            }
        } else {
            return false;
        }
        return true;
    }

    /** Shift-click: the inventory's kaasknabbels / Banksleutel into their slot, the slots' things back. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack kopie = stack.copy();
        int vakken = MijnhuisjeBlockEntity.VAKKEN;
        if (index < vakken) {
            if (!moveItemStackTo(stack, vakken, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int doel = MijnhuisjeBlockEntity.magInVak(MijnhuisjeBlockEntity.VAK_KNABBELS, stack) ? MijnhuisjeBlockEntity.VAK_KNABBELS
                    : MijnhuisjeBlockEntity.magInVak(MijnhuisjeBlockEntity.VAK_SLEUTEL, stack) ? MijnhuisjeBlockEntity.VAK_SLEUTEL : -1;
            if (doel < 0 || !moveItemStackTo(stack, doel, doel + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return kopie;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
