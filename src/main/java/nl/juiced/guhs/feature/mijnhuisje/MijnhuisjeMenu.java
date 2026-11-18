package nl.juiced.guhs.feature.mijnhuisje;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
    public static final int BREED = 176, HOOG = 258, VAK_X = 8, VAK_Y = 102, INV_X = 8, INV_Y = 176;
    /** (1.10.7) The residents' slots: 2 x 2 at the right, next to the two slots; slot index VAKKEN + i is resident i. */
    public static final int BEWONER_X = 134, BEWONER_Y = 102, BEWONER_STAP = 19;
    /** The first slot of your inventory. */
    public static final int INV_START = MijnhuisjeBlockEntity.VAKKEN + MijnhuisjeBlockEntity.MAX_BEWONERS;

    private final Container vakken;
    /** (1.10.7) Look-alikes of the residents as picked-up guhs (the server fills them; taking one picks the guh up). */
    private final net.minecraft.world.SimpleContainer bewonerVakken = new net.minecraft.world.SimpleContainer(MijnhuisjeBlockEntity.MAX_BEWONERS);
    private int verversTik;
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
        return new MijnhuisjeMenu(id, inventory, pos, null, new MijnhuisjeBlockEntity.Vakken(), new SimpleContainerData(DATA),
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
                    return vak == MijnhuisjeBlockEntity.VAK_SLEUTEL ? 1 : MijnhuisjeBlockEntity.KNABBEL_STAPEL;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return vak == MijnhuisjeBlockEntity.VAK_SLEUTEL ? 1 : MijnhuisjeBlockEntity.KNABBEL_STAPEL;
                }

                /** (1.10.5) A big knabbel stack hands out at most a normal stack at a time (a cursor never holds thousands). */
                @Override
                public java.util.Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
                    int normaal = getItem().isEmpty() ? amount : getItem().getMaxStackSize();
                    return super.tryRemove(Math.min(amount, normaal), Math.min(maxAmount, normaal), player);
                }
            });
        }
        for (int i = 0; i < MijnhuisjeBlockEntity.MAX_BEWONERS; i++) {
            addSlot(new Slot(bewonerVakken, i, BEWONER_X + (i % 2) * BEWONER_STAP, BEWONER_Y + (i / 2) * BEWONER_STAP) {
                // (vanilla never moves these: clicked() handles them, on the server)
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, INV_X, INV_Y);
        addDataSlots(data);
        ververs();
    }

    /** (1.10.7) The residents' look-alikes, fresh (server). */
    private void ververs() {
        if (be == null || !(be.getLevel() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        List<java.util.UUID> b = be.bewoners();
        for (int i = 0; i < MijnhuisjeBlockEntity.MAX_BEWONERS; i++) {
            ItemStack v = ItemStack.EMPTY;
            if (i < b.size()) {
                v = sl.getEntity(b.get(i)) instanceof nl.juiced.guhs.entity.GuhEntity g && g.isAlive()
                        ? nl.juiced.guhs.item.PickedUpGuhItem.voorbeeld(g) : nl.juiced.guhs.item.PickedUpGuhItem.voorbeeld(be.naamVan(b.get(i)));
            }
            bewonerVakken.setItem(i, v);
        }
    }

    @Override
    public void broadcastChanges() {
        if (++verversTik >= 20) {                                    // (once a second: names, hearts, health)
            verversTik = 0;
            ververs();
        }
        super.broadcastChanges();
    }

    /**
     * (1.10.7) The residents' slots: drag one out (click, shift-click, a hotbar key, Q) and the guh is picked up, out of the
     * house; right-click: it moves out (stays in the world); a picked-up guh dropped on an empty slot moves in. Only the
     * server does it (the client waits for the result: no look-alike ever becomes a real item).
     */
    @Override
    public void clicked(int slotIndex, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
        int i = slotIndex - MijnhuisjeBlockEntity.VAKKEN;
        if (slotIndex >= 0 && i >= 0 && i < MijnhuisjeBlockEntity.MAX_BEWONERS) {
            if (be != null && player instanceof ServerPlayer sp && be.mag(sp)) {
                bewonerKlik(sp, i, button, input);
                ververs();
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void bewonerKlik(ServerPlayer sp, int i, int button, net.minecraft.world.inventory.ContainerInput input) {
        List<java.util.UUID> b = be.bewoners();
        ItemStack carried = getCarried();
        if (input == net.minecraft.world.inventory.ContainerInput.PICKUP && !carried.isEmpty()) {
            if (i >= b.size() && carried.is(nl.juiced.guhs.registry.ModItems.PICKED_UP_GUH.get())
                    && sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                MijnhuisjeBlock.zetIn(sl, sp, be, carried);
            }
            return;
        }
        if (i >= b.size() || !carried.isEmpty()) {
            return;
        }
        java.util.UUID id = b.get(i);
        switch (input) {
            case PICKUP -> {
                if (button == 1) {                                     // right-click: out of the house, it stays where it is
                    sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.mijnhuisje.uit_huis", be.naamVan(id))
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                    be.trekUit(id);
                    return;
                }
                ItemStack guh = be.pakOp(sp, id);
                if (!guh.isEmpty()) {
                    setCarried(guh);
                }
            }
            case QUICK_MOVE -> {
                ItemStack guh = be.pakOp(sp, id);
                if (!guh.isEmpty()) {
                    sp.getInventory().placeItemBackInInventory(guh);
                }
            }
            case SWAP -> {
                if (button < 0 || button >= 9 && button != 40) {
                    return;
                }
                ItemStack guh = be.pakOp(sp, id);
                if (guh.isEmpty()) {
                    return;
                }
                if (sp.getInventory().getItem(button).isEmpty()) {
                    sp.getInventory().setItem(button, guh);
                } else {
                    sp.getInventory().placeItemBackInInventory(guh);
                }
            }
            case THROW -> {
                ItemStack guh = be.pakOp(sp, id);
                if (!guh.isEmpty()) {
                    sp.drop(guh, true);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != bewonerVakken && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != bewonerVakken && super.canDragTo(slot);
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

    /** Shift-click: the inventory's kaasknabbels / Banksleutel into their slot, (1.10.7) a picked-up guh into the house, the slots' things back. */
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
            if (!moveItemStackTo(stack, INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INV_START) {
            return ItemStack.EMPTY;                                    // (the residents: clicked() handles them)
        } else if (stack.is(nl.juiced.guhs.registry.ModItems.PICKED_UP_GUH.get())) {
            if (be != null && player instanceof ServerPlayer sp && be.mag(sp) && sp.level() instanceof net.minecraft.server.level.ServerLevel sl
                    && MijnhuisjeBlock.zetIn(sl, sp, be, stack)) {
                slot.setChanged();
                ververs();
            }
            return ItemStack.EMPTY;
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
