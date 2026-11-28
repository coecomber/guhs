package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * T-OPSLAG: the knapzak / bank screen's menu: a chest-like grid of {@code rijen} x 9 that shows ONE tab at a time, with the
 * player's inventory below. The grid's slots point at a view ({@link Zicht}) that forwards to the current tab's {@link Vak},
 * the live container inside {@link OpslagData}: there is no copy, so there is nothing to duplicate.
 * <p>Buttons (vanilla {@code clickMenuButton}, server-checked): {@link #KNOP_TAB}+i switch to tab i (only unlocked ones),
 * {@link #KNOP_KOOP} buy the next tab, {@link #KNOP_SORTEER} sort this tab, {@link #KNOP_MATERIALEN} all materials from the
 * inventory and from every knapzak tab into the materiaaltas, {@link #KNOP_TAS} open the materiaaltas.
 * <p>Validity ({@link #stillValid}, checked by vanilla before every click): in the Vadsrijk with the Vadsrijk inventory live (and
 * for the bank: still near the banker). Kern's inventory swap closes the screen first (its cursor item goes back), so the swap
 * never sees a half-moved item.
 */
public final class OpslagMenu extends AbstractContainerMenu {
    public static final int KNAPZAK = 0, BANK = 1;
    public static final int KNOP_TAB = 0, KNOP_KOOP = 20, KNOP_SORTEER = 40, KNOP_MATERIALEN = 41, KNOP_TAS = 42;
    /** U-OPSLAGVIP: the client showed the "WORD GUH VIP++" joke: remember it for the account (the joke never comes back). */
    public static final int KNOP_VIP_GEZIEN = 43;

    public final int soort, rijen, maxTabs;
    /** Knapzak-groter: how many normal stacks one grid slot holds (the knapzak: {@link Knapzak#STAPEL_FACTOR}; the bank: 1). */
    public final int stapelFactor;
    /** Client: what the screen shows about locked tabs (price, ways to unlock). Server: the same, sent once. */
    public final CompoundTag info;
    private final Zicht zicht;
    private final DataSlot huidig = DataSlot.standalone(), open = DataSlot.standalone();
    // server only
    @Nullable
    private final IntFunction<Vak> tabVan;
    @Nullable
    private final IntSupplier ontgrendeld;
    @Nullable
    private final Predicate<ServerPlayer> geldig;
    @Nullable
    private final Runnable koop;

    /** Server side. */
    public OpslagMenu(int id, Inventory inv, int soort, int rijen, int maxTabs, IntFunction<Vak> tabVan, IntSupplier ontgrendeld,
            Predicate<ServerPlayer> geldig, CompoundTag info, @Nullable Runnable koop, int startTab) {
        super(OpslagRegistry.OPSLAG_MENU.get(), id);
        this.soort = soort;
        this.rijen = rijen;
        this.maxTabs = maxTabs;
        this.tabVan = tabVan;
        this.ontgrendeld = ontgrendeld;
        this.geldig = geldig;
        this.info = info;
        this.koop = koop;
        this.stapelFactor = soort == KNAPZAK ? Knapzak.STAPEL_FACTOR : 1;
        this.zicht = new Zicht(rijen * 9, null);
        huidig.set(Math.max(0, Math.min(startTab, ontgrendeld.getAsInt() - 1)));
        open.set(ontgrendeld.getAsInt());
        bouw(inv);
    }

    /** Client side (from the open packet). */
    public OpslagMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(OpslagRegistry.OPSLAG_MENU.get(), id);
        this.soort = buf.readVarInt();
        this.rijen = buf.readVarInt();
        this.maxTabs = buf.readVarInt();
        this.info = net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG.decode(buf);
        this.tabVan = null;
        this.ontgrendeld = null;
        this.geldig = null;
        this.koop = null;
        this.stapelFactor = soort == KNAPZAK ? Knapzak.STAPEL_FACTOR : 1;
        this.zicht = new Zicht(rijen * 9, new Vak(rijen * 9, stapelFactor));   // (a plain container would cut big stacks to 64)
        bouw(inv);
    }

    /** What the open packet carries (the client constructor reads it back in this order). */
    public static void schrijf(RegistryFriendlyByteBuf buf, int soort, int rijen, int maxTabs, CompoundTag info) {
        buf.writeVarInt(soort);
        buf.writeVarInt(rijen);
        buf.writeVarInt(maxTabs);
        net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG.encode(buf, info);
    }

    private void bouw(Inventory inv) {
        for (int r = 0; r < rijen; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(stapelFactor > 1 ? new GrootSlot(zicht, c + r * 9, 8 + c * 18, 18 + r * 18)
                        : new Slot(zicht, c + r * 9, 8 + c * 18, 18 + r * 18));
            }
        }
        addStandardInventorySlots(inv, 8, 18 + rijen * 18 + 13);
        addDataSlot(huidig);
        addDataSlot(open);
    }

    public int huidigeTab() {
        return huidig.get();
    }

    public int openTabs() {
        return open.get();
    }

    public int vakken() {
        return rijen * 9;
    }

    @Override
    public boolean clickMenuButton(Player player, int knop) {
        if (!(player instanceof ServerPlayer sp) || tabVan == null || ontgrendeld == null || !stillValid(player)) {
            return false;
        }
        if (knop >= KNOP_TAB && knop < KNOP_TAB + maxTabs) {
            if (knop - KNOP_TAB >= ontgrendeld.getAsInt()) {
                return false;   // (a locked tab: the screen shows how to get it)
            }
            resetQuickCraft();
            huidig.set(knop - KNOP_TAB);
            broadcastChanges();
            return true;
        }
        if (knop == KNOP_KOOP && koop != null) {
            koop.run();
            return true;
        }
        if (knop == KNOP_SORTEER) {
            tabVan.apply(huidig.get()).sorteer();
            broadcastChanges();
            return true;
        }
        if (knop == KNOP_MATERIALEN) {
            Materiaaltas.meldOpgeborgen(sp, Materiaaltas.allesOpbergen(sp));   // (inventory + every knapzak tab)
            broadcastChanges();
            return true;
        }
        if (knop == KNOP_VIP_GEZIEN) {   // U-OPSLAGVIP
            VipGrap.markeer(sp);
            return true;
        }
        if (knop == KNOP_TAS) {
            sp.level().getServer().execute(() -> MateriaalMenu.open(sp));   // (after this click is done)
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        if (ontgrendeld != null) {
            open.set(ontgrendeld.getAsInt());
        }
        super.broadcastChanges();
    }

    /**
     * Knapzak-groter: a grid slot may hold more than a normal stack, but nothing outside the grid ever may (the cursor, the
     * inventory and a dropped item are saved with the item codec, which stops at 99). Taking by hand, Q and double-click go
     * through {@link GrootSlot#tryRemove} (a normal stack at a time); the two vanilla paths that move a slot's WHOLE stack
     * are handled here: the hotbar/off-hand swap key hands out one normal stack (into an empty hotbar slot only), and a click
     * with another item on the cursor does not swap with a big stack. (The client runs this too, so it predicts the same.)
     */
    @Override
    public void clicked(int slotIndex, int buttonNum, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (stapelFactor > 1 && slotIndex >= 0 && slotIndex < vakken()) {
            Slot slot = slots.get(slotIndex);
            ItemStack in = slot.getItem();
            if (in.getCount() > in.getMaxStackSize()) {
                if (input == net.minecraft.world.inventory.ContainerInput.SWAP) {
                    Inventory inv = player.getInventory();
                    if ((buttonNum >= 0 && buttonNum < 9 || buttonNum == 40) && inv.getItem(buttonNum).isEmpty()) {
                        inv.setItem(buttonNum, in.split(in.getMaxStackSize()));
                        slot.setChanged();
                    }
                    return;
                }
                if (input == net.minecraft.world.inventory.ContainerInput.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameComponents(in, getCarried())) {
                    return;
                }
            }
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack terug = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack s = slot.getItem();
            terug = s.copy();
            int n = vakken();
            if (index < n && s.getCount() > s.getMaxStackSize()) {
                // Knapzak-groter: out of a big stack: one normal stack per shift-click; what does not fit goes straight back
                ItemStack deel = s.split(s.getMaxStackSize());
                moveItemStackTo(deel, n, slots.size(), true);
                s.grow(deel.getCount());
                slot.setChanged();
                return ItemStack.EMPTY;   // (empty = vanilla does not repeat the move)
            }
            if (index < n) {
                if (!moveItemStackTo(s, n, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(s, 0, n, false)) {
                return ItemStack.EMPTY;
            }
            if (s.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return terug;
    }

    @Override
    public boolean stillValid(Player player) {
        if (geldig == null) {
            return true;   // (client)
        }
        return player instanceof ServerPlayer sp && geldig.test(sp);
    }

    /**
     * Knapzak-groter: a grid slot that holds up to {@link #stapelFactor} normal stacks, but hands out at most a normal stack at
     * a time (so the cursor, a thrown item and a pick-all never carry more than the item's own stack size).
     */
    private final class GrootSlot extends Slot {
        GrootSlot(Container c, int slot, int x, int y) {
            super(c, slot, x, y);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Vak.plafond(stack, stapelFactor);
        }

        @Override
        public java.util.Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            return super.tryRemove(getItem().isEmpty() ? amount : Math.min(amount, getItem().getMaxStackSize()), maxAmount, player);
        }
    }

    /** The grid's container: the current tab on the server, a plain mirror on the client. */
    private final class Zicht implements Container {
        private final int grootte;
        @Nullable
        private final SimpleContainer client;

        Zicht(int grootte, @Nullable SimpleContainer client) {
            this.grootte = grootte;
            this.client = client;
        }

        private Container c() {
            return client != null ? client : tabVan.apply(huidig.get());
        }

        @Override
        public int getContainerSize() {
            return grootte;
        }

        @Override
        public int getMaxStackSize() {
            return Container.super.getMaxStackSize() * stapelFactor;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Vak.plafond(stack, stapelFactor);
        }

        @Override
        public boolean isEmpty() {
            return c().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return c().getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return c().removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return c().removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
            c().setItem(slot, itemStack);
        }

        @Override
        public void setChanged() {
            c().setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return OpslagMenu.this.stillValid(player);
        }

        @Override
        public void clearContent() {
            c().clearContent();
        }
    }
}
