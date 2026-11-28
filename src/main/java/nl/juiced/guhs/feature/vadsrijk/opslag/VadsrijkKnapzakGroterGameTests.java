package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * Knapzak-groter: two more tabs (5 -> 7, three free instead of one) and slots that hold ten normal stacks (64 -> 640).
 * A save from before loads unchanged (and is written back in the same form), the new tabs work, a slot takes ten stacks and
 * no more, big stacks survive save + load, and no click path makes or loses an item or lets more than a normal stack out of
 * the grid. Run: {@code runGameTestServer -Pgt=VadsrijkKnapzakGroter}.
 */
public class VadsrijkKnapzakGroterGameTests {
    private static final String BATCH = "vadsrijk_knapzak_groter";
    private static final int GROOT = 64 * Knapzak.STAPEL_FACTOR;

    private static long telVak(Vak v, Item item) {
        long n = 0;
        for (int i = 0; i < v.getContainerSize(); i++) {
            if (v.getItem(i).is(item)) {
                n += v.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Everything of this item the player has: inventory + cursor + every knapzak tab. */
    private static long overal(ServerPlayer p, Item item) {
        long n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        if (p.containerMenu.getCarried().is(item)) {
            n += p.containerMenu.getCarried().getCount();
        }
        for (OpslagData.Zak z : Opslag.data(p).knapzakken.values()) {
            for (Vak v : z.tabs) {
                n += telVak(v, item);
            }
        }
        return n;
    }

    /** How many of this item in the whole inventory (hotbar, main, armour, off-hand). */
    private static long inInv(ServerPlayer p, Item item) {
        long n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Nothing outside the grid is ever bigger than a normal stack (the inventory and the cursor are saved with the item codec). */
    private static boolean buitenNormaal(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getCount() > inv.getItem(i).getMaxStackSize()) {
                return false;
            }
        }
        ItemStack c = p.containerMenu.getCarried();
        return c.getCount() <= c.getMaxStackSize();
    }

    private static void klopt(GameTestHelper helper, ServerPlayer p, Item item, long totaal, String wat) {
        helper.assertTrue(overal(p, item) == totaal, wat + ": total " + overal(p, item) + " (must be " + totaal + ")");
        helper.assertTrue(buitenNormaal(p), wat + ": more than a normal stack outside the grid");
    }

    /** A knapzak menu like Knapzak.open makes, but without the dimension check (the tests run in the overworld). */
    private static OpslagMenu openZak(ServerPlayer p, int tabs) {
        OpslagData.Zak z = Knapzak.zak(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new OpslagMenu(id, inv, OpslagMenu.KNAPZAK, 3, Knapzak.MAX_TABS, z::tab,
                () -> tabs, sp -> true, new CompoundTag(), null, 0), Component.literal("test")));
        return (OpslagMenu) p.containerMenu;
    }

    private static CompoundTag oud(RegistryOps<Tag> ops, int slot, ItemStack s) {
        return (CompoundTag) ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(slot, s)).getOrThrow();
    }

    /** A save exactly as 1.10.28 wrote it: every item is back in its tab and slot, the unlocks count as before plus two, and writing gives the same tags. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void oudeSaveLaadtOngewijzigd(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        RegistryOps<Tag> ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack zwaard = new ItemStack(Items.IRON_SWORD);
        zwaard.set(DataComponents.CUSTOM_NAME, Component.literal("Knuffelzwaard"));
        zwaard.setDamageValue(9);
        // the old writer: per tab a list of ItemStackWithSlot, "tabs" = how many lists, the unlocks next to it
        ListTag tab0 = new ListTag(), tab1 = new ListTag(), tab3 = new ListTag();
        tab0.add(oud(ops, 0, new ItemStack(Items.COOKIE, 64)));
        tab0.add(oud(ops, 4, zwaard));
        tab0.add(oud(ops, 26, new ItemStack(Items.ENDER_PEARL, 16)));
        tab1.add(oud(ops, 13, new ItemStack(Items.DIAMOND, 7)));
        tab3.add(oud(ops, 1, new ItemStack(Items.COOKIE, 33)));
        tab3.add(oud(ops, 2, new ItemStack(Items.COOKIE, 64)));   // (two stacks of the same thing stay two stacks in their slots)
        CompoundTag zak = new CompoundTag();
        zak.put("tab0", tab0.copy());
        zak.put("tab1", tab1.copy());
        zak.put("tab2", new ListTag());
        zak.put("tab3", tab3.copy());
        zak.putInt("tabs", 4);
        zak.putString("bronnen", "zone4,test_x");
        zak.putBoolean("gekocht", true);
        zak.putInt("gekocht_n", 1);
        CompoundTag zakken = new CompoundTag();
        zakken.put("1", zak);
        CompoundTag oudeSave = Nbt.write(p.registryAccess(), new OpslagData()::serialize);   // (the other keys, empty)
        oudeSave.put("knapzakken", zakken);

        OpslagData d = Opslag.data(p);
        d.deserialize(Nbt.input(p.registryAccess(), oudeSave));
        OpslagData.Zak z = d.zak("1");
        helper.assertTrue(z.tab(0).getItem(0).is(Items.COOKIE) && z.tab(0).getItem(0).getCount() == 64, "tab 1 slot 0: 64 cookies");
        helper.assertTrue(ItemStack.matches(z.tab(0).getItem(4), zwaard), "tab 1 slot 4: the named, damaged sword");
        helper.assertTrue(z.tab(0).getItem(26).is(Items.ENDER_PEARL) && z.tab(0).getItem(26).getCount() == 16, "tab 1 slot 26: 16 pearls");
        helper.assertTrue(z.tab(1).getItem(13).is(Items.DIAMOND) && z.tab(1).getItem(13).getCount() == 7, "tab 2 slot 13: 7 diamonds");
        helper.assertTrue(z.tab(3).getItem(1).getCount() == 33 && z.tab(3).getItem(2).getCount() == 64, "tab 4: 33 and 64, each in its slot");
        helper.assertTrue(z.tab(0).gevuld() == 3 && z.tab(1).gevuld() == 1 && z.tab(2).gevuld() == 0 && z.tab(3).gevuld() == 2, "nothing more, nothing less");
        helper.assertTrue(d.onleesbaar.isEmpty(), "nothing was put aside: " + d.onleesbaar.keySet());
        helper.assertTrue(z.gekochtAantal() == 1 && z.bronnen.equals(java.util.Set.of("zone4", "test_x")), "the unlocks are kept");
        // 1.10.28: 1 free + 1 bought + 2 sources = 4 tabs. Now 3 free: 6, so tabs 5 and 6 are new, and empty.
        helper.assertTrue(Knapzak.tabs(p) == 6, "four tabs became six: " + Knapzak.tabs(p));
        helper.assertTrue(z.tab(4).isEmpty() && z.tab(5).isEmpty(), "the two new tabs start empty");
        OpslagMenu m = openZak(p, Knapzak.tabs(p));
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 5) && m.huidigeTab() == 5, "the new sixth tab opens");
        helper.assertTrue(!m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 6), "the seventh is still locked for this character");
        p.closeContainer();
        // writing it again gives the very same item tags (normal stacks are saved exactly as before)
        CompoundTag weer = Nbt.write(p.registryAccess(), d::serialize).getCompoundOrEmpty("knapzakken").getCompoundOrEmpty("1");
        helper.assertTrue(weer.getListOrEmpty("tab0").equals(tab0) && weer.getListOrEmpty("tab1").equals(tab1) && weer.getListOrEmpty("tab3").equals(tab3),
                "written back unchanged: " + weer.getListOrEmpty("tab0") + " / " + tab0);
        helper.assertTrue(weer.getStringOr("bronnen", "").equals("zone4,test_x") && weer.getIntOr("gekocht_n", 0) == 1, "and the unlocks too");
        helper.succeed();
    }

    /** Three free tabs, seven at most; the new ones take and give items like the first. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tweeVakkenErbij(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(Knapzak.GRATIS == 3 && Knapzak.MAX_TABS == 7, "3 free tabs, 7 at most");
        helper.assertTrue(Knapzak.tabs(p) == 3, "a new character has 3 tabs: " + Knapzak.tabs(p));
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 40));
        OpslagMenu m = openZak(p, Knapzak.tabs(p));
        int hotbar0 = m.vakken() + 27;
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 2) && m.huidigeTab() == 2, "tab 3 opens");
        m.clicked(hotbar0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(telVak(Knapzak.zak(p).tab(2), Items.DIAMOND) == 40 && p.getInventory().getItem(0).isEmpty(), "40 diamonds into tab 3");
        helper.assertTrue(!m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 3) && m.huidigeTab() == 2, "tab 4 is locked for a new character");
        m.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(Knapzak.zak(p).tab(2).isEmpty() && p.getInventory().getItem(8).getCount() == 40, "and out again (the last hotbar slot)");
        p.closeContainer();
        // every source at once: capped at 7, and the seventh tab works
        OpslagData.Zak z = Knapzak.zak(p);
        z.gekochtN = 1;
        for (String bron : List.of("a", "b", "c", "d", "e")) {
            z.bronnen.add(bron);
        }
        helper.assertTrue(Knapzak.tabs(p) == 7 && Knapzak.binnenkortVanaf(p) == 7 && !Knapzak.koopbaar(p), "capped at 7: " + Knapzak.tabs(p));
        m = openZak(p, Knapzak.tabs(p));
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 6) && m.huidigeTab() == 6, "tab 7 opens");
        helper.assertTrue(!m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 7), "there is no tab 8");
        m.clicked(hotbar0 + 8, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(telVak(z.tab(6), Items.DIAMOND) == 40, "40 diamonds into tab 7");
        p.closeContainer();
        helper.assertTrue(overal(p, Items.DIAMOND) == 40, "still 40 in all");
        helper.succeed();
    }

    /** A slot takes ten normal stacks (640, pearls 160), not one item more; things that do not stack stay one per slot. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tienKeerZoveel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagMenu m = openZak(p, 3);
        Vak tab = Knapzak.zak(p).tab(0);
        int hotbar0 = m.vakken() + 27;
        long totaal = 0;
        for (int i = 0; i < 10; i++) {   // ten stacks, shift-clicked in one by one: all into slot 0
            p.getInventory().setItem(0, new ItemStack(Items.COOKIE, 64));
            totaal += 64;
            m.clicked(hotbar0, 0, ContainerInput.QUICK_MOVE, p);
            klopt(helper, p, Items.COOKIE, totaal, "stack " + (i + 1) + " in");
        }
        helper.assertTrue(tab.getItem(0).getCount() == GROOT && tab.gevuld() == 1, "slot 0 holds 640: " + tab.getItem(0).getCount());
        // the eleventh stack: slot 0 refuses, it starts slot 1
        p.getInventory().setItem(0, new ItemStack(Items.COOKIE, 64));
        totaal += 64;
        m.clicked(hotbar0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(tab.getItem(0).getCount() == GROOT && tab.getItem(1).getCount() == 64, "the 11th stack goes to slot 1");
        klopt(helper, p, Items.COOKIE, totaal, "11th stack");
        // by hand onto the full slot: the cursor keeps everything
        p.getInventory().setItem(0, new ItemStack(Items.COOKIE, 20));
        totaal += 20;
        m.clicked(hotbar0, 0, ContainerInput.PICKUP, p);
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(tab.getItem(0).getCount() == GROOT && m.getCarried().getCount() == 20, "a full slot takes nothing from the cursor");
        m.clicked(0, 1, ContainerInput.PICKUP, p);
        helper.assertTrue(tab.getItem(0).getCount() == GROOT && m.getCarried().getCount() == 20, "nor one with a right-click");
        m.clicked(1, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(tab.getItem(1).getCount() == 84 && m.getCarried().isEmpty(), "onto slot 1: 64 + 20 = 84");
        klopt(helper, p, Items.COOKIE, totaal, "by hand");
        // a slot that is nearly full takes only what fits
        tab.setItem(5, new ItemStack(Items.COOKIE, GROOT - 10));
        totaal += GROOT - 10;
        p.getInventory().setItem(0, new ItemStack(Items.COOKIE, 64));
        totaal += 64;
        m.clicked(hotbar0, 0, ContainerInput.PICKUP, p);
        m.clicked(5, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(tab.getItem(5).getCount() == GROOT && m.getCarried().getCount() == 54, "10 fit, 54 stay on the cursor");
        m.clicked(hotbar0, 0, ContainerInput.PICKUP, p);
        klopt(helper, p, Items.COOKIE, totaal, "nearly full");
        // pearls stack to 16: ten times that
        for (int i = 0; i < 11; i++) {
            p.getInventory().setItem(1, new ItemStack(Items.ENDER_PEARL, 16));
            m.clicked(hotbar0 + 1, 0, ContainerInput.QUICK_MOVE, p);
        }
        helper.assertTrue(tab.getItem(2).getCount() == 160 && tab.getItem(3).getCount() == 16, "pearls: 160 in a slot, the rest in the next");
        klopt(helper, p, Items.ENDER_PEARL, 176, "pearls");
        // things that do not stack: one per slot, as before
        p.getInventory().setItem(2, new ItemStack(Items.IRON_SWORD));
        p.getInventory().setItem(3, new ItemStack(Items.IRON_SWORD));
        m.clicked(hotbar0 + 2, 0, ContainerInput.QUICK_MOVE, p);
        m.clicked(hotbar0 + 3, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(tab.getItem(4).getCount() == 1 && tab.getItem(6).getCount() == 1, "two swords, two slots");
        p.getInventory().setItem(2, new ItemStack(Items.IRON_SWORD));
        m.clicked(hotbar0 + 2, 0, ContainerInput.PICKUP, p);
        m.clicked(4, 0, ContainerInput.PICKUP, p);   // (an equal sword on a sword: nothing happens)
        helper.assertTrue(tab.getItem(4).getCount() == 1 && m.getCarried().getCount() == 1, "a sword does not stack on a sword");
        m.clicked(hotbar0 + 2, 0, ContainerInput.PICKUP, p);
        klopt(helper, p, Items.IRON_SWORD, 3, "swords");
        // a tab that is full to the brim refuses a shift-click: the stack stays in the inventory
        for (int i = 0; i < tab.getContainerSize(); i++) {
            tab.setItem(i, new ItemStack(Items.DIAMOND, GROOT));
        }
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
        m.clicked(hotbar0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(p.getInventory().getItem(0).getCount() == 64 && telVak(tab, Items.DIAMOND) == 27L * GROOT, "a full tab takes nothing");
        // the bank keeps normal stacks
        Vak bank = Opslag.data(p).bankTab(0);
        helper.assertTrue(bank.getMaxStackSize(new ItemStack(Items.COOKIE)) == 64 && tab.getMaxStackSize(new ItemStack(Items.COOKIE)) == GROOT,
                "the bank: 64 per slot; the knapzak: 640");
        p.closeContainer();
        helper.succeed();
    }

    /** Big stacks through save + load (also the copy on death, which is this same save + load), entries of normal size only. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void groteStapelsBewaren(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        RegistryOps<Tag> ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        OpslagData d = Opslag.data(p);
        ItemStack koek = new ItemStack(Items.COOKIE, GROOT);
        koek.set(DataComponents.CUSTOM_NAME, Component.literal("Feestkoek"));
        d.zak("1").tab(0).setItem(3, koek.copy());
        d.zak("1").tab(0).setItem(4, new ItemStack(Items.COOKIE, 300));   // (no name: another kind, its own slot)
        d.zak("1").tab(6).setItem(26, new ItemStack(Items.ENDER_PEARL, 160));
        d.zak("1").tab(5).setItem(0, new ItemStack(Items.DIAMOND, 65));
        d.zak("1").tab(5).setItem(1, new ItemStack(Items.DIAMOND, 64));
        d.zak("k9").tab(1).setItem(7, new ItemStack(Items.EMERALD, GROOT));
        helper.assertTrue(d.zak("1").tab(0).getItem(3).getCount() == GROOT, "640 in a slot");
        CompoundTag tag = Nbt.write(p.registryAccess(), d::serialize);
        // every saved entry is a normal vanilla stack (the item codec refuses more than 99)
        int delen = 0;
        for (Tag t : tag.getCompoundOrEmpty("knapzakken").getCompoundOrEmpty("1").getListOrEmpty("tab0")) {
            ItemStackWithSlot s = ItemStackWithSlot.CODEC.parse(ops, t).getOrThrow();
            helper.assertTrue(s.stack().getCount() <= 64 && (s.slot() == 3 || s.slot() == 4), "a part of at most 64 in slot 3 or 4");
            delen++;
        }
        helper.assertTrue(delen == 10 + 5, "640 = 10 parts, 300 = 5 parts (4 x 64 + 44): " + delen);
        for (int ronde = 0; ronde < 2; ronde++) {   // twice: what was loaded saves the same again
            OpslagData t = new OpslagData();
            t.deserialize(Nbt.input(p.registryAccess(), tag));
            helper.assertTrue(ItemStack.matches(t.zak("1").tab(0).getItem(3), koek), "640 named cookies back in slot 3: " + t.zak("1").tab(0).getItem(3));
            helper.assertTrue(t.zak("1").tab(0).getItem(4).getCount() == 300 && t.zak("1").tab(0).gevuld() == 2, "300 plain cookies in slot 4");
            helper.assertTrue(t.zak("1").tab(6).getItem(26).is(Items.ENDER_PEARL) && t.zak("1").tab(6).getItem(26).getCount() == 160, "160 pearls in tab 7");
            helper.assertTrue(t.zak("1").tab(5).getItem(0).getCount() == 65 && t.zak("1").tab(5).getItem(1).getCount() == 64, "65 and 64 stay in their slots");
            helper.assertTrue(t.zak("k9").tab(1).getItem(7).getCount() == GROOT, "the other character's 640");
            helper.assertTrue(t.onleesbaar.isEmpty(), "nothing put aside: " + t.onleesbaar.keySet());
            tag = Nbt.write(p.registryAccess(), t::serialize);
        }
        // after a rollback to 1.10.28 and back, a part can find its slot taken by something else: it moves to a free slot
        CompoundTag zak = new CompoundTag();
        ListTag tab0 = new ListTag();
        tab0.add(oud(ops, 0, new ItemStack(Items.IRON_SWORD)));
        tab0.add(oud(ops, 0, new ItemStack(Items.COOKIE, 64)));
        tab0.add(oud(ops, 0, new ItemStack(Items.COOKIE, 64)));
        zak.put("tab0", tab0);
        zak.putInt("tabs", 1);
        CompoundTag zakken = new CompoundTag();
        zakken.put("1", zak);
        CompoundTag terug = Nbt.write(p.registryAccess(), new OpslagData()::serialize);
        terug.put("knapzakken", zakken);
        OpslagData t = new OpslagData();
        t.deserialize(Nbt.input(p.registryAccess(), terug));
        helper.assertTrue(t.zak("1").tab(0).getItem(0).is(Items.IRON_SWORD) && telVak(t.zak("1").tab(0), Items.COOKIE) == 128 && t.onleesbaar.isEmpty(),
                "the sword keeps slot 0, the 128 cookies sit in a free slot");
        helper.succeed();
    }

    /** Every click path on a big stack: the total never changes and nothing bigger than a normal stack leaves the grid. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void geenDupeMetGroteStapels(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagMenu m = openZak(p, 3);
        Vak tab = Knapzak.zak(p).tab(0);
        int n = m.vakken(), hotbar0 = n + 27;
        tab.setItem(0, new ItemStack(Items.COOKIE, GROOT));
        long totaal = GROOT;
        // left click: a normal stack on the cursor; and back
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 64 && tab.getItem(0).getCount() == GROOT - 64, "left click takes 64");
        klopt(helper, p, Items.COOKIE, totaal, "left click");
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().isEmpty() && tab.getItem(0).getCount() == GROOT, "and puts them back");
        // right click: "half" is at most a normal stack
        m.clicked(0, 1, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 64, "right click takes 64, not 320: " + m.getCarried().getCount());
        klopt(helper, p, Items.COOKIE, totaal, "right click");
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        // another item on the cursor: no swap with a big stack
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        m.clicked(hotbar0, 0, ContainerInput.PICKUP, p);
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().is(Items.DIAMOND) && m.getCarried().getCount() == 5 && tab.getItem(0).getCount() == GROOT, "no swap with a big stack");
        m.clicked(0, 1, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 5 && tab.getItem(0).getCount() == GROOT, "nor with a right-click");
        m.clicked(hotbar0, 0, ContainerInput.PICKUP, p);
        klopt(helper, p, Items.COOKIE, totaal, "other item on the cursor");
        // the hotbar key: onto a taken hotbar slot nothing happens, into an empty one goes one normal stack
        m.clicked(0, 0, ContainerInput.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND) && tab.getItem(0).getCount() == GROOT, "hotbar key on a taken slot: nothing");
        m.clicked(0, 1, ContainerInput.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(1).getCount() == 64 && tab.getItem(0).getCount() == GROOT - 64, "hotbar key: 64 into hotbar slot 2");
        klopt(helper, p, Items.COOKIE, totaal, "hotbar key");
        m.clicked(0, 40, ContainerInput.SWAP, p);
        helper.assertTrue(p.getInventory().getItem(40).getCount() == 64 && tab.getItem(0).getCount() == GROOT - 128, "off-hand key: 64");
        klopt(helper, p, Items.COOKIE, totaal, "off-hand key");
        // shift-click out: one normal stack per click
        m.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(tab.getItem(0).getCount() == GROOT - 192 && inInv(p, Items.COOKIE) == 192, "shift-click out: one stack");
        klopt(helper, p, Items.COOKIE, totaal, "shift-click out");
        // double click with a few on the cursor: the cursor fills to a normal stack
        m.clicked(0, 1, ContainerInput.PICKUP, p);
        m.clicked(n + 5, 0, ContainerInput.PICKUP, p);   // (64 into an empty inventory slot)
        p.getInventory().setItem(2, new ItemStack(Items.COOKIE, 3));
        totaal += 3;
        m.clicked(hotbar0 + 2, 0, ContainerInput.PICKUP, p);
        m.clicked(hotbar0 + 2, 0, ContainerInput.PICKUP_ALL, p);
        helper.assertTrue(m.getCarried().getCount() == 64, "double click: the cursor holds 64: " + m.getCarried().getCount());
        klopt(helper, p, Items.COOKIE, totaal, "double click");
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        // dragging over a big stack and two empty slots: the big stack is left alone
        long voor = tab.getItem(0).getCount();
        p.getInventory().setItem(3, new ItemStack(Items.COOKIE, 64));
        totaal += 64;
        m.clicked(hotbar0 + 3, 0, ContainerInput.PICKUP, p);
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ContainerInput.QUICK_CRAFT, p);
        m.clicked(0, AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, p);
        m.clicked(10, AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, p);
        m.clicked(11, AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, p);
        m.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ContainerInput.QUICK_CRAFT, p);
        helper.assertTrue(tab.getItem(0).getCount() == voor && tab.getItem(10).getCount() == 32 && tab.getItem(11).getCount() == 32,
                "drag: 32 + 32 in the empty slots, the big stack untouched");
        klopt(helper, p, Items.COOKIE, totaal, "drag");
        // switching tab while carrying, putting down in tab 2
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB + 1), "to tab 2");
        m.clicked(0, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(telVak(Knapzak.zak(p).tab(1), Items.COOKIE) == 64 && m.getCarried().isEmpty(), "64 into tab 2");
        klopt(helper, p, Items.COOKIE, totaal, "tab switch");
        helper.assertTrue(m.clickMenuButton(p, OpslagMenu.KNOP_TAB), "back to tab 1");
        // sorting merges into big stacks and keeps the count
        tab.setItem(20, new ItemStack(Items.COOKIE, 400));
        tab.setItem(21, new ItemStack(Items.COOKIE, 400));
        totaal += 800;
        long inTab = telVak(tab, Items.COOKIE);
        m.clickMenuButton(p, OpslagMenu.KNOP_SORTEER);
        helper.assertTrue(telVak(tab, Items.COOKIE) == inTab && tab.getItem(0).getCount() == GROOT && tab.getItem(1).getCount() == inTab - GROOT
                && tab.gevuld() == 2, "sorted: one full big stack and the rest, total kept (" + telVak(tab, Items.COOKIE) + ")");
        for (int i = 0; i < tab.getContainerSize(); i++) {
            helper.assertTrue(tab.getItem(i).getCount() <= GROOT, "no slot over 640 after sorting");
        }
        klopt(helper, p, Items.COOKIE, totaal, "sorting");
        // a full inventory: shift-click out moves nothing and loses nothing
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getItem(i).isEmpty()) {
                p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            }
        }
        long cookiesInv = inInv(p, Items.COOKIE);
        int vrij = 0;   // (room left on cookie stacks in the inventory)
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getItem(i).is(Items.COOKIE)) {
                vrij += 64 - p.getInventory().getItem(i).getCount();
            }
        }
        m.clicked(0, 0, ContainerInput.QUICK_MOVE, p);
        helper.assertTrue(inInv(p, Items.COOKIE) == cookiesInv + Math.min(64, vrij), "a full inventory takes only what fits");
        klopt(helper, p, Items.COOKIE, totaal, "full inventory");
        // Q and Ctrl+Q on a big stack: only normal stacks land on the ground
        long opDeGrond0 = opDeGrond(helper, p);
        long inSlot = tab.getItem(0).getCount();
        m.clicked(0, 0, ContainerInput.THROW, p);
        helper.assertTrue(tab.getItem(0).getCount() == inSlot - 1, "Q throws one");
        m.clicked(0, 1, ContainerInput.THROW, p);
        helper.assertTrue(tab.getItem(0).isEmpty(), "Ctrl+Q empties the slot");
        long gegooid = 0;
        for (ItemEntity e : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(12), e -> e.getItem().is(Items.COOKIE))) {
            helper.assertTrue(e.getItem().getCount() <= 64, "a thrown stack of " + e.getItem().getCount());
            gegooid += e.getItem().getCount();
        }
        helper.assertTrue(gegooid - opDeGrond0 == inSlot, "all " + inSlot + " thrown cookies lie on the ground: " + (gegooid - opDeGrond0));
        klopt(helper, p, Items.COOKIE, totaal - inSlot, "after throwing");
        // closing with a stack on the cursor: back into the inventory (or on the ground), never lost
        p.getInventory().setItem(0, ItemStack.EMPTY);
        m.clicked(1, 0, ContainerInput.PICKUP, p);
        helper.assertTrue(m.getCarried().getCount() == 64, "64 on the cursor");
        p.closeContainer();
        helper.assertTrue(p.containerMenu == p.inventoryMenu && p.getInventory().getItem(0).getCount() == 64, "closing returns the cursor stack");
        klopt(helper, p, Items.COOKIE, totaal - inSlot, "closed");
        // and what is in the knapzak now is what a save holds
        OpslagData t = new OpslagData();
        t.deserialize(Nbt.input(p.registryAccess(), Nbt.write(p.registryAccess(), Opslag.data(p)::serialize)));
        helper.assertTrue(telVak(t.zak("1").tab(0), Items.COOKIE) == telVak(tab, Items.COOKIE)
                && telVak(t.zak("1").tab(1), Items.COOKIE) == telVak(Knapzak.zak(p).tab(1), Items.COOKIE), "saved = live");
        helper.succeed();
    }

    /**
     * "Store all" (the knapzak's "Materialen ➜ tas" and the materiaaltas' "Alles opbergen" are one mover): materials from the
     * inventory and from every tab of this character's knapzak go into the bag, big stacks whole; everything else stays.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void allesNaarDeTas(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        OpslagData d = Opslag.data(p);
        Item steen = EcoSpul.VERBETERSTEENTJE.get(), draad = EcoSpul.PLUISDRAAD.get();
        OpslagData.Zak z = Knapzak.zak(p);
        int grootSteen = z.tab(0).getMaxStackSize(new ItemStack(steen));
        helper.assertTrue(grootSteen == new ItemStack(steen).getMaxStackSize() * Knapzak.STAPEL_FACTOR, "a big stack of stones: " + grootSteen);
        p.getInventory().setItem(0, new ItemStack(steen, 30));            // hotbar
        p.getInventory().setItem(20, new ItemStack(draad, 5));            // main inventory
        p.getInventory().setItem(1, new ItemStack(Items.BREAD, 5));       // not a material
        p.getInventory().setItem(40, new ItemStack(steen, 7));            // the off-hand is left alone (as before)
        z.tab(0).setItem(3, new ItemStack(steen, grootSteen));            // a big stack in tab 1
        z.tab(0).setItem(4, new ItemStack(Items.COOKIE, 100));            // not a material
        z.tab(0).setItem(5, new ItemStack(Items.IRON_SWORD));
        z.tab(1).setItem(26, new ItemStack(steen, 11));                   // tab 2
        z.tab(2).setItem(0, new ItemStack(draad, 12));                    // tab 3
        z.tab(6).setItem(13, new ItemStack(draad, 64));                   // tab 7
        d.zak("k9").tab(0).setItem(0, new ItemStack(steen, 9));           // another knight's knapzak: his own
        Materiaaltas.stop(d, new ItemStack(steen, 2));                    // already in the bag
        long steenVoor = alles(p, steen), draadVoor = alles(p, draad);
        helper.assertTrue(steenVoor == 30 + 7 + grootSteen + 11 + 9 + 2 && draadVoor == 5 + 12 + 64, "start: " + steenVoor + " / " + draadVoor);
        // outside the Vadsrijk the knapzak's button moves nothing (the gate of every storage action)
        OpslagMenu zak = openZak(p, 3);
        zak.clickMenuButton(p, OpslagMenu.KNOP_MATERIALEN);
        helper.assertTrue(Materiaaltas.tel(p, steen) == 2 && z.tab(0).getItem(3).getCount() == grootSteen, "outside: nothing moves");
        p.closeContainer();
        // the mover itself, through the materiaaltas screen's validated action
        MateriaalMenu m = new MateriaalMenu(0, p.getInventory(), d, sp -> true);
        m.actie(p, MateriaalMenu.ALLES, ItemStack.EMPTY, 0, false);
        helper.assertTrue(Materiaaltas.tel(p, steen) == 2 + 30 + grootSteen + 11, "stones in the bag: " + Materiaaltas.tel(p, steen));
        helper.assertTrue(Materiaaltas.tel(p, draad) == 5 + 12 + 64, "thread in the bag: " + Materiaaltas.tel(p, draad));
        helper.assertTrue(p.getInventory().getItem(0).isEmpty() && p.getInventory().getItem(20).isEmpty(), "the inventory's materials are gone");
        helper.assertTrue(z.tab(0).getItem(3).isEmpty() && z.tab(1).getItem(26).isEmpty() && z.tab(2).getItem(0).isEmpty()
                && z.tab(6).getItem(13).isEmpty(), "and those of every knapzak tab");
        helper.assertTrue(p.getInventory().getItem(1).getCount() == 5 && z.tab(0).getItem(4).getCount() == 100 && z.tab(0).getItem(5).is(Items.IRON_SWORD),
                "bread, cookies and the sword stay where they were");
        helper.assertTrue(p.getInventory().getItem(40).getCount() == 7 && d.zak("k9").tab(0).getItem(0).getCount() == 9,
                "the off-hand and another knight's knapzak are left alone");
        helper.assertTrue(alles(p, steen) == steenVoor && alles(p, draad) == draadVoor, "totals kept: " + alles(p, steen) + " / " + alles(p, draad));
        // again: nothing left to move, nothing changes
        helper.assertTrue(Materiaaltas.allesOpbergen(p, d) == 0 && alles(p, steen) == steenVoor && alles(p, draad) == draadVoor, "a second time: nothing");
        // and it is all in a save
        OpslagData t = new OpslagData();
        t.deserialize(Nbt.input(p.registryAccess(), Nbt.write(p.registryAccess(), d::serialize)));
        long steenInSave = t.materialen.stream().filter(x -> x.soort.is(steen)).mapToLong(x -> x.aantal).sum();
        helper.assertTrue(steenInSave == 2 + 30 + grootSteen + 11 && t.zak("1").tab(0).getItem(4).getCount() == 100, "saved: " + steenInSave);
        helper.succeed();
    }

    /** Inventory + cursor + every knapzak + the materiaaltas. */
    private static long alles(ServerPlayer p, Item item) {
        return overal(p, item) + Materiaaltas.tel(p, item);
    }

    private static long opDeGrond(GameTestHelper helper, ServerPlayer p) {
        long n = 0;
        for (ItemEntity e : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(12), e -> e.getItem().is(Items.COOKIE))) {
            n += e.getItem().getCount();
        }
        return n;
    }
}
