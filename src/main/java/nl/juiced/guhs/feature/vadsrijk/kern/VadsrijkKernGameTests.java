package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * Het Vadsrijk kern: only the risky logic. The inventory swap never loses an item (also through a save/load and with a stash
 * that was unexpectedly full), the loot roll ranges, the element triangle, the damage maths and the levels.
 * Run: {@code runGameTestServer -Pgt=VadsrijkKern}.
 */
public class VadsrijkKernGameTests {
    private static final String BATCH = "vadsrijk_kern";

    private static List<ItemStack> kopie(Inventory inv) {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            l.add(inv.getItem(i).copy());
        }
        return l;
    }

    private static boolean gelijk(List<ItemStack> a, Inventory inv) {
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), inv.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    private static int totaal(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            n += inv.getItem(i).getCount();
        }
        return n;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void inventarisWisselVerliestNiks(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        Inventory inv = p.getInventory();
        ItemStack naam = new ItemStack(Items.DIAMOND_SWORD);
        naam.set(DataComponents.CUSTOM_NAME, Component.literal("Opa's zwaard"));
        naam.setDamageValue(17);
        inv.setItem(0, naam);
        inv.setItem(4, new ItemStack(Items.COBBLESTONE, 64));
        inv.setItem(35, new ItemStack(Items.CAKE, 1));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        p.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));   // the 2x2 grid
        p.inventoryMenu.setCarried(new ItemStack(Items.APPLE, 5));                      // the cursor
        inv.setSelectedSlot(4);
        int voor = totaal(inv) + 3 + 5;

        helper.assertTrue(Inventaris.naarRijk(p) && d.binnen, "swapped in");
        helper.assertTrue(inv.isEmpty() && p.inventoryMenu.getCarried().isEmpty() && p.inventoryMenu.getCraftSlots().isEmpty(),
                "nothing of home comes in (not the cursor, not the grid)");
        helper.assertTrue(!Inventaris.naarRijk(p), "swapping in twice does nothing");
        // the Vadsrijk inventory gets a sword
        BasisType t = BasisTypes.van("kussenzwaard");
        ItemStack rijk = Rollen.stack(Rollen.rol(t, 3, Zeldzaamheid.EPISCH, RandomSource.create(5)));
        inv.setItem(2, rijk.copy());
        // a save + load in between (the attachment goes through player.dat)
        CompoundTag tag = Nbt.write(p.registryAccess(), d::serialize);
        SpelerData terug = new SpelerData();
        terug.deserialize(Nbt.input(p.registryAccess(), tag));
        helper.assertTrue(terug.binnen && terug.opgeborgen.size() == d.opgeborgen.size(), "the stash survives a save");
        d.deserialize(Nbt.input(p.registryAccess(), tag));

        helper.assertTrue(Inventaris.naarThuis(p) && !d.binnen, "swapped out");
        helper.assertTrue(totaal(inv) == voor, "every item home again (" + totaal(inv) + " of " + voor + ")");
        helper.assertTrue(inv.getItem(0).getHoverName().getString().equals("Opa's zwaard") && inv.getItem(0).getDamageValue() == 17,
                "components kept");
        helper.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET) && p.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD),
                "armour and offhand back in their slots");
        helper.assertTrue(inv.getSelectedSlot() == 4, "the selected slot back");
        helper.assertTrue(d.rijkInventaris.size() == 1, "the Vadsrijk sword waits in its own stash");
        List<ItemStack> thuis = kopie(inv);

        // back in: the sword comes back, the home stuff goes in the stash
        Inventaris.naarRijk(p);
        helper.assertTrue(ItemStack.matches(inv.getItem(2), rijk), "the Vadsrijk sword exactly as it was");
        // a stash that is (wrongly) not empty is never overwritten: it moves to the found items
        CompoundTag spook = d.opgeborgen.get(0).copy();
        d.rijkInventaris.add(spook);   // something already waiting in the Vadsrijk stash while we are inside
        Inventaris.naarThuis(p);
        helper.assertTrue(gelijk(thuis, inv), "home inventory identical after a second round");
        helper.assertTrue(Inventaris.gevondenAantal(p, true) == 1, "the unexpected stash item went to the found items, not lost");
        d.gevonden.clear();
        d.rijkInventaris.clear();
        d.opgeborgen.clear();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void lootRollenBinnenBereik(GameTestHelper helper) {
        RandomSource r = RandomSource.create(42);
        BasisType t = BasisTypes.van("pluishamer");
        helper.assertTrue(t != null, "kern base type registered");
        for (Zeldzaamheid z : Zeldzaamheid.values()) {
            for (int i = 0; i < 400; i++) {
                int lvl = 1 + r.nextInt(20);
                Uitrusting u = Rollen.rol(t, lvl, z, r);
                int n = u.bonussen().size();
                helper.assertTrue(n >= z.minBonus && n <= z.maxBonus && n >= 1 && n <= 5, z + ": bonus count " + n);
                Set<Object> stats = new HashSet<>();
                for (Bonus b : u.bonussen()) {
                    helper.assertTrue(stats.add(b.stat()), "no stat twice");
                    double[] br = Bonussen.bereik(b.stat(), lvl);
                    helper.assertTrue(b.waarde() >= br[0] - 1e-9 && b.waarde() <= br[1] + 1e-9, b + " in " + br[0] + ".." + br[1]);
                }
                helper.assertTrue(u.flavour() >= 0 && u.flavour() < t.flavours(), "flavour index");
                // the data component survives a codec round trip
                ItemStack s = Rollen.stack(u);
                helper.assertTrue(u.equals(Rollen.van(s)), "component on the stack");
            }
        }
        Uitrusting gewoon = Rollen.rol(t, 5, Zeldzaamheid.GEWOON, r), guhd = Rollen.rol(t, 5, Zeldzaamheid.GUHDDELIJK, r);
        helper.assertTrue(Rollen.wapenSchade(guhd) > Rollen.wapenSchade(gewoon), "rarer hits harder");
        helper.assertTrue(Rollen.wapenSchade(guhd.metVerpluisd(true)) == 1 && Rollen.stats(guhd.metVerpluisd(true)).alles().isEmpty(),
                "verpluisd = useless until repaired");
        int[] tel = new int[Zeldzaamheid.values().length];
        for (int i = 0; i < 20000; i++) {
            tel[Zeldzaamheid.rol(r, 1).ordinal()]++;
        }
        for (int i = 1; i < tel.length; i++) {
            helper.assertTrue(tel[i] < tel[i - 1], "each rarity is rarer than the one before");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void elementDriehoek(GameTestHelper helper) {
        helper.assertTrue(Element.factor(Element.VUUR, Element.NATUUR, false) == 1.5, "vuur > natuur");
        helper.assertTrue(Element.factor(Element.NATUUR, Element.WATER, false) == 1.5, "natuur > water");
        helper.assertTrue(Element.factor(Element.WATER, Element.VUUR, false) == 1.5, "water > vuur");
        helper.assertTrue(Element.factor(Element.NATUUR, Element.VUUR, false) == 0.7, "natuur weak against vuur");
        helper.assertTrue(Element.factor(Element.VUUR, Element.WATER, false) == 0.7, "vuur weak against water");
        helper.assertTrue(Element.factor(Element.WATER, Element.NATUUR, false) == 0.7, "water weak against natuur");
        helper.assertTrue(Element.factor(Element.VUUR, Element.VUUR, true) == 1.0, "same element: neutral");
        helper.assertTrue(Element.factor(Element.ROZE, Element.WATER, true) == 1.5 && Element.factor(Element.ROZE, Element.WATER, false) == 1.0,
                "roze: strong only against ontvadst");
        helper.assertTrue(Element.factor(Element.GEEN, Element.VUUR, true) == 1.0, "no element: neutral");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void schadeEnLevels(GameTestHelper helper) {
        Stats s = Stats.van(Stat.KRACHT, 50, Stat.KRITIEKSCHADE, 20, Stat.ELEMENTSCHADE, 10);
        double gewoon = Schade.aanval(10, s, Schade.Soort.MELEE, false, Element.GEEN, Element.WATER, true);
        helper.assertTrue(Math.abs(gewoon - 15) < 1e-9, "1101-balans: 10 x (1 + 1% x 50 kracht) = 15, got " + gewoon);
        helper.assertTrue(Math.abs(Schade.hoofdFactor(50) - 1.7 * 1.5) < 1e-9, "1101-balans: player main-stat factor 1.7 x (1 + 0.5)");
        double krit = Schade.aanval(10, s, Schade.Soort.MELEE, true, Element.GEEN, Element.WATER, true);
        helper.assertTrue(Math.abs(krit - 15 * 1.7) < 1e-9, "crit x(1.5 + 20%)");
        double vuur = Schade.aanval(10, s, Schade.Soort.MELEE, false, Element.VUUR, Element.NATUUR, false);
        helper.assertTrue(Math.abs(vuur - 15 * 1.5 * 1.1) < 1e-9, "element weakness x1.5 and +10% elementschade");
        double magie = Schade.aanval(10, s, Schade.Soort.MAGIE, false, Element.GEEN, null, false);
        helper.assertTrue(Math.abs(magie - 10) < 1e-9, "magic uses wijsheid (0 here)");
        // (balans 2026-11) level-relative: def / (def + 25 + 15 x attacker level), at most 60 % (VadsrijkBalansGameTests has more)
        helper.assertTrue(Math.abs(Schade.verdediging(100, Stats.van(Stat.VERDEDIGING, 50), Element.GEEN, 5) - 100 * (1 - 50.0 / 150)) < 1e-9,
                "50 def vs level 5 takes a third off");
        helper.assertTrue(Math.abs(Schade.verdediging(100, Stats.van(Stat.ELEMENTWEERSTAND, 200), Element.VUUR) - 25) < 1e-9, "resist capped at 75%");
        for (int l = 1; l <= Levels.MAX; l++) {
            helper.assertTrue(Levels.levelVan(Levels.totaalVoor(l)) == l, "level " + l + " round trip");
            helper.assertTrue(l == 1 || Levels.levelVan(Levels.totaalVoor(l) - 1) == l - 1, "one xp short of " + l);
            helper.assertTrue(l == Levels.MAX || Levels.xpVoor(l + 1) > Levels.xpVoor(l), "the curve rises");
        }
        helper.assertTrue(Levels.levelVan(Long.MAX_VALUE / 4) == Levels.MAX, "capped at " + Levels.MAX);
        var ridder = nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.van("knuffelridder");
        helper.assertTrue(ridder != null && ridder.basis(10).get(Stat.VADS) > ridder.basis(1).get(Stat.VADS)
                && ridder.basis(20).get(Stat.KRACHT) > ridder.basis(10).get(Stat.KRACHT), "stats grow by level");
        // a player: xp gives levels, level-up fills Vads
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.geefXp(p, Levels.totaalVoor(5), "test");
        helper.assertTrue(Spelers.level(p) == 5 && Spelers.vads(p) == Spelers.maxVads(p), "level 5, full Vads");
        helper.succeed();
    }

    /** Accessory slots (Curios/Accessories soft compat) go through the same swap: tested with a fake source of 3 slots. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void accessoireVakkenWisselMee(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        net.minecraft.world.SimpleContainer nep = new net.minecraft.world.SimpleContainer(3);
        nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.Bron bron = new nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.Bron() {
            @Override
            public String id() {
                return "test_accessoires";
            }

            @Override
            public List<nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.Vak> vakken(ServerPlayer sp) {
                List<nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.Vak> l = new ArrayList<>();
                for (int i = 0; i < nep.getContainerSize(); i++) {
                    int slot = i;
                    l.add(new nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.Vak() {
                        @Override
                        public ItemStack get() {
                            return nep.getItem(slot);
                        }

                        @Override
                        public void set(ItemStack st) {
                            nep.setItem(slot, st);
                        }
                    });
                }
                return l;
            }
        };
        nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.ALLE.add(bron);
        try {
            nep.setItem(1, new ItemStack(Items.TOTEM_OF_UNDYING));
            nep.setItem(2, new ItemStack(Items.ELYTRA));
            Inventaris.naarRijk(p);
            helper.assertTrue(nep.isEmpty(), "accessories are put away inside");
            nep.setItem(1, new ItemStack(Items.FEATHER));   // (something put in the slot inside: it must not be lost either)
            Inventaris.naarThuis(p);
            helper.assertTrue(nep.getItem(1).is(Items.TOTEM_OF_UNDYING) && nep.getItem(2).is(Items.ELYTRA), "accessories back in their own slots");
            Inventaris.naarRijk(p);
            helper.assertTrue(nep.getItem(1).is(Items.FEATHER) && nep.getItem(2).isEmpty(), "the inside accessory comes back inside");
            Inventaris.naarThuis(p);
            helper.assertTrue(nep.getItem(1).is(Items.TOTEM_OF_UNDYING), "and home again");
        } finally {
            nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken.ALLE.remove(bron);
            Spelers.data(p).rijkInventaris.clear();
        }
        helper.succeed();
    }

    /** Jewellery counts in the stats (with its level requirement), as do the slices' extra stat sources. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sieradenEnExtraStats(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        double voor = Spelers.stats(p).get(Stat.KRITIEK);
        BasisType ring = BasisTypes.van("roze_ring");
        Uitrusting u = Rollen.rol(ring, 1, Zeldzaamheid.GEWOON, RandomSource.create(3));
        Sieraden.zet(p, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING, Rollen.stack(u));
        double met = Spelers.stats(p).get(Stat.KRITIEK);
        helper.assertTrue(met > voor, "the ring adds kritiek (" + voor + " -> " + met + ")");
        // through a save + load
        CompoundTag tag = Nbt.write(p.registryAccess(), Spelers.data(p)::serialize);
        Spelers.data(p).deserialize(Nbt.input(p.registryAccess(), tag));
        helper.assertTrue(Spelers.stats(p).get(Stat.KRITIEK) == met, "the ring survives a save");
        Sieraden.zet(p, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING, Rollen.stack(Rollen.rol(ring, 9, Zeldzaamheid.GEWOON, RandomSource.create(3))));
        helper.assertTrue(Spelers.stats(p).get(Stat.KRITIEK) == voor, "a level 9 ring does nothing at level 1");
        java.util.function.Function<ServerPlayer, Stats> set = sp -> Stats.van(Stat.LEVENSROOF, 7);
        Spelers.EXTRA_STATS.add(set);
        try {
            helper.assertTrue(Spelers.stats(p).get(Stat.LEVENSROOF) == 7, "an extra stat source counts");
        } finally {
            Spelers.EXTRA_STATS.remove(set);
        }
        helper.succeed();
    }

    /** (fix round 2, g-ui) Shift-click on jewellery: into its slot, swap with the worn one, level gate, only jewellery, and off again. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sieraadShiftKlik(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var RING = nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING;
        Inventory inv = p.getInventory();
        BasisType ring = BasisTypes.van("roze_ring");
        try {
            ItemStack a = Rollen.stack(Rollen.rol(ring, 1, Zeldzaamheid.GEWOON, RandomSource.create(3)));
            ItemStack b = Rollen.stack(Rollen.rol(ring, 1, Zeldzaamheid.ZELDZAAM, RandomSource.create(4)));
            inv.setItem(5, a.copy());
            helper.assertTrue(Sieraden.wisselMetInventaris(p, 5), "shift-click wears the ring");
            helper.assertTrue(inv.getItem(5).isEmpty(), "the ring left the inventory slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, RING), a), "the ring is worn");
            // T-ITEMS: a second ring first fills the 2nd ring slot; a third one swaps with the first
            ItemStack c2 = Rollen.stack(Rollen.rol(ring, 1, Zeldzaamheid.GEWOON, RandomSource.create(6)));
            inv.setItem(6, c2.copy());
            helper.assertTrue(Sieraden.wisselMetInventaris(p, 6), "a second ring goes into the 2nd ring slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING2), c2),
                    "the 2nd ring slot holds it");
            inv.setItem(7, b.copy());
            helper.assertTrue(Sieraden.wisselMetInventaris(p, 7), "a third ring swaps");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, RING), b), "the new ring is worn");
            helper.assertTrue(ItemStack.isSameItemSameComponents(inv.getItem(7), a), "the old ring took its inventory slot");
            ItemStack hoog = Rollen.stack(Rollen.rol(ring, 9, Zeldzaamheid.GEWOON, RandomSource.create(5)));
            inv.setItem(8, hoog.copy());
            helper.assertFalse(Sieraden.wisselMetInventaris(p, 8), "a level 9 ring is refused at level 1");
            helper.assertTrue(ItemStack.isSameItemSameComponents(inv.getItem(8), hoog), "and stays in the inventory");
            inv.setItem(9, new ItemStack(Items.COBBLESTONE, 3));
            helper.assertFalse(Sieraden.wisselMetInventaris(p, 9), "cobblestone is no jewellery");
            helper.assertTrue(inv.getItem(9).getCount() == 3, "cobblestone untouched");
            helper.assertFalse(Sieraden.wisselMetInventaris(p, 40), "an index outside the 36 slots does nothing");
            inv.clearContent();
            helper.assertTrue(Sieraden.afZonderCheck(p, RING), "clicking the worn slot takes it off");
            helper.assertTrue(Sieraden.get(p, RING).isEmpty(), "nothing worn any more");
            boolean terug = false;
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                terug |= ItemStack.isSameItemSameComponents(inv.getItem(i), b);
            }
            helper.assertTrue(terug, "the ring is back in the inventory");
        } finally {
            Sieraden.zet(p, RING, ItemStack.EMPTY);
            Sieraden.zet(p, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING2, ItemStack.EMPTY);   // T-ITEMS
        }
        helper.succeed();
    }
}
