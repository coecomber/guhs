package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * T-OPSLAG: knapzak / bank / materiaaltas safety. Dupe safety (every click path keeps the total), the inventory swap with a
 * storage screen open, persistence (save + load, unreadable items kept), tabs/unlocks per character, the bag (kinds, counts,
 * the Smid hook), no smuggling outside the Vadsrijk. Run: {@code runGameTestServer -Pgt=VadsrijkTOpslag}.
 */
public class VadsrijkTOpslagGameTests {
    private static final String BATCH = "vadsrijk_t_opslag";

    /** Everything of this item: inventory + cursor + all knapzak/bank tabs + the bag. */
    private static long overal(ServerPlayer p, net.minecraft.world.item.Item item) {
        long n = Materiaaltas.tel(p, item);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        if (p.containerMenu.getCarried().is(item)) {
            n += p.containerMenu.getCarried().getCount();
        }
        OpslagData d = Opslag.data(p);
        for (OpslagData.Zak z : d.knapzakken.values()) {
            for (Vak v : z.tabs) {
                n += telVak(v, item);
            }
        }
        for (Vak v : d.bank) {
            n += telVak(v, item);
        }
        return n;
    }

    private static long telVak(Vak v, net.minecraft.world.item.Item item) {
        long n = 0;
        for (int i = 0; i < v.getContainerSize(); i++) {
            if (v.getItem(i).is(item)) {
                n += v.getItem(i).getCount();
            }
        }
        return n;
    }

    /** A knapzak menu like Knapzak.open makes, but without the dimension check (the tests run in the overworld). */
    private static OpslagMenu openZak(ServerPlayer p, int tabs) {
        OpslagData.Zak z = Knapzak.zak(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new OpslagMenu(id, inv, OpslagMenu.KNAPZAK, 3, Knapzak.MAX_TABS, z::tab,
                () -> tabs, sp -> true, new CompoundTag(), null, 0), Component.literal("test")));
        return (OpslagMenu) p.containerMenu;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geenDupeInHetScherm(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
        p.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 10));
        OpslagMenu m = openZak(p, 2);
        int n = m.vakken();   // 27 grid slots, then 27 main inventory slots, then 9 hotbar slots
        int hotbar0 = n + 27;
        helper.assertTrue(overal(p, Items.DIAMOND) == 74, "start 74");
        // shift-click into the knapzak
        m.quickMoveStack(p, hotbar0);
        helper.assertTrue(overal(p, Items.DIAMOND) == 74 && telVak(Knapzak.zak(p).tab(0), Items.DIAMOND) == 64, "shift-click in");
        // pick up from the grid, switch tab while carrying, put down in tab 2
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 64 && overal(p, Items.DIAMOND) == 74, "carrying 64");
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 1) && m.huidigeTab() == 1, "switched to tab 2");
        m.clicked(5, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().isEmpty() && telVak(Knapzak.zak(p).tab(1), Items.DIAMOND) == 64 && overal(p, Items.DIAMOND) == 74,
                "put down in tab 2");
        // a locked tab is refused
        helper.assertTrue(!m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 3) && m.huidigeTab() == 1, "locked tab refused");
        // right-click split, throw-away-safe: quick-move the rest back, then the half
        m.clicked(5, 1, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 32 && overal(p, Items.DIAMOND) == 74, "half on the cursor");
        m.quickMoveStack(p, 5);
        helper.assertTrue(overal(p, Items.DIAMOND) == 74, "rest moved out while carrying");
        // pick-all (double click) over grid + inventory
        m.clicked(n, 0, ContainerInput.PICKUP_ALL, p);
        helper.assertTrue(overal(p, Items.DIAMOND) == 74, "pick-all keeps the total");
        // hotbar-swap key on a grid slot
        m.clicked(7, 2, ContainerInput.SWAP, p);
        helper.assertTrue(overal(p, Items.DIAMOND) == 74, "hotbar swap keeps the total");
        // sorting keeps everything
        Knapzak.zak(p).tab(1).setItem(10, new ItemStack(Items.DIAMOND, 3));
        Knapzak.zak(p).tab(1).setItem(20, new ItemStack(Items.DIAMOND, 3));
        long voorSort = overal(p, Items.DIAMOND);
        m.clickMenuButton(p, OpslagMenu.KNOP_SORTEER);
        helper.assertTrue(overal(p, Items.DIAMOND) == voorSort, "sorting keeps the total");
        // close with an item on the cursor: it goes back into the inventory
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        p.closeContainer();
        helper.assertTrue(p.containerMenu == p.inventoryMenu && overal(p, Items.DIAMOND) == voorSort, "closing returns the cursor item");
        // the screen edits the live containers: a save right now has it all
        OpslagData d = Opslag.data(p);
        OpslagData terug = new OpslagData();
        terug.deserialize(Nbt.input(p.registryAccess(), Nbt.write(p.registryAccess(), d::serialize)));
        long opgeslagen = 0;
        for (Vak v : terug.zak("1").tabs) {
            opgeslagen += telVak(v, Items.DIAMOND);
        }
        helper.assertTrue(opgeslagen == telVak(d.zak("1").tab(0), Items.DIAMOND) + telVak(d.zak("1").tab(1), Items.DIAMOND), "saved = live");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void wisselMetSchermOpen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        Inventaris.naarRijk(p);   // (the overworld test: just the swap itself; the Vadsrijk inventory is now live)
        p.getInventory().setItem(3, new ItemStack(Items.EMERALD, 20));
        OpslagMenu m = openZak(p, 1);
        m.quickMoveStack(p, m.vakken() + 27 + 3);   // 20 emeralds into the knapzak
        Knapzak.zak(p).tab(0).setItem(1, new ItemStack(Items.EMERALD, 7));
        m.clicked(1, 0, ContainerInput.PICKUP, p);   // 7 on the cursor
        helper.assertTrue(m.getCarried().getCount() == 7, "carrying 7");
        // kern swaps home (the reconciler does this on a dimension change): the screen must close first
        Inventaris.naarThuis(p);
        helper.assertTrue(p.containerMenu == p.inventoryMenu, "the storage screen was closed by the swap");
        int inStash = 0;
        for (CompoundTag t : d.rijkInventaris) {
            ItemStack s = Nbt.parseStack(p.registryAccess(), t);
            if (s.is(Items.EMERALD)) {
                inStash += s.getCount();
            }
        }
        helper.assertTrue(inStash == 7, "the cursor's 7 went into the Vadsrijk stash with the inventory (" + inStash + ")");
        helper.assertTrue(telVak(Knapzak.zak(p).tab(0), Items.EMERALD) == 20, "the knapzak keeps its 20");
        helper.assertTrue(!Opslag.magOpenen(p) && !Knapzak.open(p) && !Bank.open(p) && !MateriaalMenu.open(p), "outside: no storage opens");
        // outside the Vadsrijk a material never goes into the (Vadsrijk) bag: no smuggling
        ItemStack steen = new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 5);
        Materiaaltas.geef(p, steen);
        helper.assertTrue(Materiaaltas.tel(p, EcoSpul.VERBETERSTEENTJE.get()) == 0 && p.getInventory().countItem(EcoSpul.VERBETERSTEENTJE.get()) == 5,
                "outside: a material goes into the normal inventory");
        helper.assertTrue(Materiaaltas.allesOpbergen(p) == 0 && p.getInventory().countItem(EcoSpul.VERBETERSTEENTJE.get()) == 5,
                "outside: 'store all' does nothing");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bewarenEnLaden(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagData d = Opslag.data(p);
        ItemStack naam = new ItemStack(Items.IRON_SWORD);
        naam.set(DataComponents.CUSTOM_NAME, Component.literal("Knuffelzwaard"));
        naam.setDamageValue(9);
        d.zak("1").tab(0).setItem(4, naam.copy());
        d.zak("1").tab(2).setItem(26, new ItemStack(Items.COOKIE, 33));
        d.zak("1").bronnen.add("zone4");
        d.zak("1").gekocht = true;
        d.zak("2").tab(0).setItem(0, new ItemStack(Items.APPLE, 2));
        d.bankTab(1).setItem(53, new ItemStack(Items.GOLD_INGOT, 64));
        d.bankGekocht = 2;
        d.autoUit = true;
        Materiaaltas.stop(d, Kristallen.stack(Kristallen.SOORTEN.keySet().iterator().next(), 0, 50));
        Materiaaltas.stop(d, Kristallen.stack(Kristallen.SOORTEN.keySet().iterator().next(), 2, 3));
        OpslagData.Materiaal veel = new OpslagData.Materiaal(new ItemStack(EcoSpul.PLUISDRAAD.get()), 5_000_000_000L);
        d.materialen.add(veel);
        // an item of a removed mod in the bank: kept as it was
        CompoundTag weg = new CompoundTag();
        weg.putString("id", "verdwenenmod:ding");
        weg.putInt("count", 3);
        weg.putByte("Slot", (byte) 2);
        CompoundTag tag = Nbt.write(p.registryAccess(), d::serialize);
        CompoundTag bank = tag.getCompoundOrEmpty("bank");
        ListTag tab0 = bank.getListOrEmpty("tab0");
        tab0.add(weg);
        bank.put("tab0", tab0);
        tag.put("bank", bank);

        OpslagData t = new OpslagData();
        t.deserialize(Nbt.input(p.registryAccess(), tag));
        ItemStack z = t.zak("1").tab(0).getItem(4);
        helper.assertTrue(ItemStack.matches(z, naam) && z.getDamageValue() == 9, "knapzak item with components");
        helper.assertTrue(t.zak("1").tab(2).getItem(26).getCount() == 33 && t.zak("2").tab(0).getItem(0).is(Items.APPLE), "both characters' knapzak");
        helper.assertTrue(t.zak("1").gekocht && t.zak("1").bronnen.contains("zone4"), "unlocks kept");
        helper.assertTrue(t.bankTab(1).getItem(53).getCount() == 64 && t.bankGekocht == 2 && t.autoUit, "bank + settings");
        helper.assertTrue(t.materialen.size() == 3, "3 material kinds (crystal qualities apart): " + t.materialen.size());
        helper.assertTrue(t.materialen.stream().anyMatch(m -> m.aantal == 5_000_000_000L), "a count beyond int");
        helper.assertTrue(t.onleesbaar.getOrDefault("bank/0", List.of()).size() == 1, "the unreadable item is kept aside");
        CompoundTag weer = Nbt.write(p.registryAccess(), t::serialize);
        helper.assertTrue(weer.getCompoundOrEmpty("bank").getListOrEmpty("tab0").stream()
                .anyMatch(x -> x instanceof CompoundTag c && c.getStringOr("id", "").equals("verdwenenmod:ding")), "and written back unchanged");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void materiaaltas(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagData d = Opslag.data(p);
        var steen = EcoSpul.VERBETERSTEENTJE.get();
        helper.assertTrue(Materiaaltas.isMateriaal(new ItemStack(steen)) && !Materiaaltas.isMateriaal(new ItemStack(Items.BREAD)), "what is a material");
        Materiaaltas.stop(d, new ItemStack(steen, 64));
        Materiaaltas.stop(d, new ItemStack(steen, 64));
        Materiaaltas.stop(d, new ItemStack(steen, 2));
        helper.assertTrue(d.materialen.size() == 1 && Materiaaltas.tel(p, steen) == 130, "one kind, 130");
        // the Smid's counting (EcoUtil, hooked) sees the bag; taking uses the inventory first
        p.getInventory().setItem(0, new ItemStack(steen, 10));
        helper.assertTrue(EcoUtil.tel(p.getInventory(), steen) == 140, "EcoUtil.tel = inventory + bag");
        EcoUtil.neem(p.getInventory(), steen, 15);
        helper.assertTrue(p.getInventory().getItem(0).isEmpty() && Materiaaltas.tel(p, steen) == 125, "EcoUtil.neem: 10 from the inventory, 5 from the bag");
        helper.assertTrue(!Opslag.neem(p, steen, 126) && Materiaaltas.tel(p, steen) == 125, "not enough: nothing taken");
        helper.assertTrue(Opslag.neem(p, steen, 125) && Materiaaltas.tel(p, steen) == 0 && d.materialen.isEmpty(), "all taken, kind gone");
        // taking out through the screen: max a stack; a full inventory -> straight back
        Materiaaltas.stop(d, new ItemStack(steen, 200));
        MateriaalMenu m = new MateriaalMenu(0, p.getInventory(), d, sp -> true);
        m.actie(p, MateriaalMenu.NEEM, new ItemStack(steen), 0, false);
        helper.assertTrue(m.getCarried().getCount() == steen.getDefaultMaxStackSize() && Materiaaltas.tel(p, steen) == 200 - steen.getDefaultMaxStackSize(),
                "one stack on the cursor");
        m.actie(p, MateriaalMenu.NEEM, ItemStack.EMPTY, 0, false);   // (clicking the grid with a full cursor puts it back)
        helper.assertTrue(m.getCarried().isEmpty() && Materiaaltas.tel(p, steen) == 200, "cursor back in");
        for (int i = 0; i < 36; i++) {
            p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        m.actie(p, MateriaalMenu.NEEM, new ItemStack(steen), 0, true);
        helper.assertTrue(Materiaaltas.tel(p, steen) == 200, "shift-take into a full inventory: nothing lost, all still in the bag");
        // non-materials are refused from the cursor
        m.setCarried(new ItemStack(Items.BREAD, 3));
        m.actie(p, MateriaalMenu.STOP_HAND, ItemStack.EMPTY, 0, false);
        helper.assertTrue(m.getCarried().getCount() == 3 && d.materialen.size() == 1, "bread stays on the cursor");
        m.setCarried(ItemStack.EMPTY);
        // shift-click a material from the inventory
        p.getInventory().setItem(5, new ItemStack(EcoSpul.PLUISDRAAD.get(), 12));
        m.quickMoveStack(p, 5);
        helper.assertTrue(p.getInventory().getItem(5).isEmpty() && Materiaaltas.tel(p, EcoSpul.PLUISDRAAD.get()) == 12, "shift-click stores");
        // crystal qualities stay apart
        String soort = Kristallen.SOORTEN.keySet().iterator().next();
        Materiaaltas.stop(d, Kristallen.stack(soort, 0, 4));
        Materiaaltas.stop(d, Kristallen.stack(soort, 1, 1));
        helper.assertTrue(Materiaaltas.tel(p, Kristallen.stack(soort, 0, 1)) == 4 && Materiaaltas.tel(p, Kristallen.stack(soort, 1, 1)) == 1, "qualities apart");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void tabsPerKarakter(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var oud = Opslag.KARAKTER;
        try {
            int basis = Knapzak.tabs(p);   // (1 + any story flags of a fresh player: none)
            helper.assertTrue(basis == 1, "a new character has 1 tab: " + basis);
            helper.assertTrue(Knapzak.ontgrendel(p, "test_a") && !Knapzak.ontgrendel(p, "test_a") && Knapzak.tabs(p) == 2, "a source counts once");
            Knapzak.zak(p).gekocht = true;
            Knapzak.ontgrendel(p, "test_b");
            Knapzak.ontgrendel(p, "test_c");
            Knapzak.ontgrendel(p, "test_d");
            helper.assertTrue(Knapzak.tabs(p) == Knapzak.MAX_TABS, "capped at " + Knapzak.MAX_TABS);
            Knapzak.zak(p).tab(0).setItem(0, new ItemStack(Items.CAKE));
            Opslag.KARAKTER = sp -> "2";
            helper.assertTrue(Knapzak.tabs(p) == 1 && Knapzak.zak(p).tab(0).isEmpty(), "character 2 has its own knapzak");
            Opslag.data(p).bankTab(0).setItem(0, new ItemStack(Items.BELL));
            Opslag.KARAKTER = oud;
            helper.assertTrue(Opslag.data(p).bankTab(0).getItem(0).is(Items.BELL), "the bank is shared by the characters");
            helper.assertTrue(Bank.tabs(p) == 1, "bank: 1 tab");
            Opslag.data(p).bankGekocht = 99;
            helper.assertTrue(Bank.tabs(p) == Bank.MAX_TABS, "bank capped");
            helper.assertTrue(Bank.isBankier("bankier") && Bank.isBankier("z2_veilingmeester"), "the hub bankers are registered");
        } finally {
            Opslag.KARAKTER = oud;
        }
        helper.succeed();
    }
}
