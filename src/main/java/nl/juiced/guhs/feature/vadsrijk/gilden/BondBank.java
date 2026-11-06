package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.gilden.Rang.Recht;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.opslag.Vak;

/**
 * U-GILDEN: the bond vault in the castle (V1 "guild-bank"): {@link Bond#BANK_TABS} tabs of {@link Bond#BANK_VAKKEN}, unlocked by bond
 * level ({@link BondXp#bankTabs}). Shared by all members, with permissions: everybody may put things in, a lid may take from tab 1,
 * an officier/leider from every tab.
 * <p><b>Dupe-safe like t-opslag's bank:</b> the menu's slots point straight at the LIVE containers inside {@link Bonden} (no copy on
 * open, no write-back on close), everything runs on the server thread, and every way to take an item out (click, shift-click,
 * hotbar swap, throw, double-click collect, bundles) goes through {@link Slot#mayPickup}, which checks the rank on the server.
 * Two members on the same tab see each other's moves at once (vanilla chest behaviour). Bundles/shulkers and bound gear are
 * refused. The screen is the vanilla 6-row chest (no client code): the bottom row holds the tab buttons (server-side clicks).
 */
public final class BondBank {
    public static final double BEREIK = 8;
    /** Tests: the vault works outside the Vadsrijk too (gametests run in the overworld). */
    static volatile boolean OVERAL = false;
    static final int RIJ = 45, KNOP_SORTEER = 52, KNOP_INFO = 51;

    private BondBank() {
    }

    public static boolean open(ServerPlayer p, Bond b, BlockPos waar) {
        if (!Vadsrijk.in(p) && !p.isCreative()) {
            return false;
        }
        Vec3 plek = Vec3.atCenterOf(waar);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, b, sp -> sp.position().distanceTo(plek) <= BEREIK),
                Component.translatable("vadsrijk.gilden.bank.titel", b.naam)));
        p.level().playSound(null, waar, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.1f);
        nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.gilden.tip.bank");
        return true;
    }

    /** Closes every open vault screen of this bond (castle rebuild, disband). */
    static void sluitAlles(MinecraftServer s, Bond b) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.containerMenu instanceof Menu m && m.bond == b) {
                p.closeContainer();
            }
        }
    }

    /** May this item go into a bond vault? (no containers, no bound gear) */
    public static boolean toegestaan(ItemStack s) {
        return !s.has(DataComponents.BUNDLE_CONTENTS) && !s.has(DataComponents.CONTAINER) && EcoUtil.verhandelbaar(s);
    }

    /** May this character take things out of this tab? */
    public static boolean magOpnemen(Bond b, String sleutel, int tab) {
        Bond.Lid l = b.lid(sleutel);
        if (l == null) {
            return false;
        }
        return l.rang.mag(Recht.BANK_ALLES) || (tab == 0 && l.rang.mag(Recht.BANK_OPNEMEN));
    }

    /** The menu (server side; the client shows a plain 6-row chest). */
    public static final class Menu extends AbstractContainerMenu {
        final Bond bond;
        private final String sleutel;
        private final java.util.function.Predicate<ServerPlayer> dichtbij;
        private final SimpleContainer iconen = new SimpleContainer(9);
        private final Container zicht;
        private int tab;

        public Menu(int id, Inventory inv, Bond bond, java.util.function.Predicate<ServerPlayer> dichtbij) {
            super(MenuType.GENERIC_9x6, id);
            this.bond = bond;
            this.sleutel = Bonden.sleutel(inv.player);
            this.dichtbij = dichtbij;
            List<Vak> tabs = bond.bank(inv.player.level().registryAccess());
            this.zicht = new Zicht(tabs);
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 9; c++) {
                    addSlot(new BankSlot(zicht, c + r * 9, 8 + c * 18, 18 + r * 18));
                }
            }
            for (int c = 0; c < 9; c++) {
                addSlot(new IcoonSlot(iconen, c, 8 + c * 18, 18 + 5 * 18));
            }
            addStandardInventorySlots(inv, 8, 18 + 6 * 18 + 13);
            iconen();
        }

        public int tab() {
            return tab;
        }

        int open() {
            return Math.min(Bond.BANK_TABS, BondXp.bankTabs(bond.level()));
        }

        private boolean opnemen() {
            return magOpnemen(bond, sleutel, tab);
        }

        private void iconen() {
            int open = open();
            for (int i = 0; i < 9; i++) {
                ItemStack s = ItemStack.EMPTY;
                if (i < Bond.BANK_TABS) {
                    boolean los = i < open;
                    s = new ItemStack(i == tab ? Items.LIME_STAINED_GLASS_PANE : los ? Items.PINK_STAINED_GLASS_PANE : Items.GRAY_STAINED_GLASS_PANE);
                    s.set(DataComponents.CUSTOM_NAME, los ? Component.translatable("vadsrijk.gilden.bank.tab", i + 1).withStyle(i == tab ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE)
                            : Component.translatable("vadsrijk.gilden.bank.tab_dicht", i + 1, levelVoorTab(i)).withStyle(ChatFormatting.GRAY));
                } else if (i == KNOP_INFO - RIJ) {
                    s = new ItemStack(Items.PAPER);
                    Bond.Lid l = bond.lid(sleutel);
                    s.set(DataComponents.CUSTOM_NAME, Component.translatable(opnemen() ? "vadsrijk.gilden.bank.info_ja" : "vadsrijk.gilden.bank.info_nee",
                            Component.translatable(l == null ? Rang.REKRUUT.naamKey() : l.rang.naamKey())).withStyle(ChatFormatting.YELLOW));
                } else if (i == KNOP_SORTEER - RIJ && opnemen()) {
                    s = new ItemStack(Items.HOPPER);
                    s.set(DataComponents.CUSTOM_NAME, Component.translatable("vadsrijk.gilden.bank.sorteer").withStyle(ChatFormatting.AQUA));
                }
                iconen.setItem(i, s);
            }
        }

        static int levelVoorTab(int i) {
            for (int lv = 1; lv <= BondXp.MAX; lv++) {
                if (BondXp.bankTabs(lv) > i) {
                    return lv;
                }
            }
            return BondXp.MAX;
        }

        @Override
        public void clicked(int slot, int knop, ContainerInput soort, Player speler) {
            if (slot >= RIJ && slot < RIJ + 9) {
                if (soort == ContainerInput.PICKUP && speler instanceof ServerPlayer sp && stillValid(sp)) {
                    int i = slot - RIJ;
                    if (i < Bond.BANK_TABS && i < open()) {
                        tab = i;
                        resetQuickCraft();
                    } else if (slot == KNOP_SORTEER && opnemen()) {
                        bond.bank(sp.level().registryAccess()).get(tab).sorteer();
                    }
                    iconen();
                    broadcastChanges();
                }
                return;   // (the icon row never moves items; the client's guess is corrected by the next sync)
            }
            super.clicked(slot, knop, soort, speler);
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
            return target.container != iconen && super.canTakeItemForPickAll(carried, target);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem() || slot.container == iconen) {
                return ItemStack.EMPTY;
            }
            ItemStack s = slot.getItem();
            ItemStack terug = s.copy();
            if (index < RIJ) {
                if (!slot.mayPickup(player) || !moveItemStackTo(s, RIJ + 9, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!toegestaan(s) || !moveItemStackTo(s, 0, RIJ, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (s.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return terug;
        }

        @Override
        public boolean stillValid(Player player) {
            if (!(player instanceof ServerPlayer sp)) {
                return true;
            }
            return bond.leden.containsKey(Bonden.sleutel(sp)) && Bonden.van(sp) == bond && (Vadsrijk.in(sp) || sp.isCreative() || OVERAL) && dichtbij.test(sp);
        }

        @Override
        public void broadcastChanges() {
            if (tab >= open()) {
                tab = 0;
                iconen();
            }
            super.broadcastChanges();
        }

        final class BankSlot extends Slot {
            BankSlot(Container c, int i, int x, int y) {
                super(c, i, x, y);
            }

            @Override
            public boolean mayPickup(Player player) {
                return player instanceof ServerPlayer sp ? magOpnemen(bond, Bonden.sleutel(sp), tab) : true;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return toegestaan(stack);
            }
        }

        static final class IcoonSlot extends Slot {
            IcoonSlot(Container c, int i, int x, int y) {
                super(c, i, x, y);
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }

        /** The 45 grid slots: the current tab's live container. */
        private final class Zicht implements Container {
            private final List<Vak> tabs;

            Zicht(List<Vak> tabs) {
                this.tabs = tabs;
            }

            private Vak c() {
                return tabs.get(Math.max(0, Math.min(tab, tabs.size() - 1)));
            }

            @Override
            public int getContainerSize() {
                return RIJ;
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
                MinecraftServer s = Bonden.server;
                if (s != null) {
                    Bonden.get(s).setDirty();
                }
            }

            @Override
            public boolean stillValid(Player player) {
                return Menu.this.stillValid(player);
            }

            @Override
            public void clearContent() {
                c().clearContent();
            }
        }
    }

    /** Dev/tests: a vault menu without the "near the chest" rule. */
    static Menu menuVoorTest(ServerPlayer p, Bond b) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, b, sp -> true), Component.literal("test")));
        return (Menu) p.containerMenu;
    }

    @Nullable
    static Menu open(ServerPlayer p) {
        return p.containerMenu instanceof Menu m ? m : null;
    }
}
