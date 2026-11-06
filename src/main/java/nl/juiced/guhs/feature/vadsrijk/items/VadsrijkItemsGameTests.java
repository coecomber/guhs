package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.SterRegels;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.storage.Nbt;

/**
 * T-ITEMS (itemisatie 2.0): the new slots (wear / take off / survive a save and the inventory swap / the 2nd ring), the slot
 * budget, the new stats' maths and slot pools, the smid's pity and talisman, Erfstukken (Vertrouwd, doorgroeien, overvadsen,
 * polijsten) and the drankjesriem. Run: {@code runGameTestServer -Pgt=VadsrijkItems}.
 */
public class VadsrijkItemsGameTests {
    private static final String BATCH = "vadsrijk_items";

    private static ItemStack stuk(String id, Zeldzaamheid z, int level, long seed) {
        return Rollen.stack(Rollen.rol(BasisTypes.van(id), level, z, RandomSource.create(seed)));
    }

    private static void allesAf(ServerPlayer p) {
        for (Slot s : Slot.SIERADEN) {
            Sieraden.zet(p, s, ItemStack.EMPTY);
        }
    }

    /** Gloves, cape, armband and two rings go on by shift-click, count in the stats, survive a save and the inventory swap, come off. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void slotsDragenEnBewaren(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Inventory inv = p.getInventory();
        try {
            allesAf(p);
            double vdVoor = Spelers.stats(p).get(Stat.VERDEDIGING);
            ItemStack h = stuk("pluiswantjes", Zeldzaamheid.GEWOON, 1, 1), c = stuk("knuffelcapeje", Zeldzaamheid.GEWOON, 1, 2),
                    a = stuk("pluisarmband", Zeldzaamheid.GEWOON, 1, 3), r1 = stuk("roze_ring", Zeldzaamheid.GEWOON, 1, 4),
                    r2 = stuk("roze_ring", Zeldzaamheid.ZELDZAAM, 1, 5);
            List<ItemStack> l = List.of(h, c, a, r1, r2);
            for (int i = 0; i < l.size(); i++) {
                inv.setItem(i, l.get(i).copy());
                helper.assertTrue(Sieraden.wisselMetInventaris(p, i), "shift-click wears piece " + i);
                helper.assertTrue(inv.getItem(i).isEmpty(), "piece " + i + " left the bag");
            }
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.HANDSCHOENEN), h), "gloves in the gloves slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.CAPE), c), "cape in the cape slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.ARMBAND), a), "armband in the armband slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.RING), r1), "first ring in RING");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.RING2), r2), "second ring in RING2");
            Stats s = Spelers.stats(p);
            helper.assertTrue(s.get(Stat.VERDEDIGING) > vdVoor, "the gloves add Verdediging");
            double krit = s.get(Stat.KRITIEK);
            // both rings count (each roze_ring gives Kritiek x the jewellery budget)
            double eenRing = Rollen.stats(Rollen.van(r1)).get(Stat.KRITIEK) + Rollen.stats(Rollen.van(r2)).get(Stat.KRITIEK);
            helper.assertTrue(eenRing > 0 && krit >= eenRing - 1e-6, "both rings count (" + krit + " >= " + eenRing + ")");
            // a save
            CompoundTag tag = Nbt.write(p.registryAccess(), Spelers.data(p)::serialize);
            Spelers.data(p).deserialize(Nbt.input(p.registryAccess(), tag));
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.CAPE), c), "the cape survives a save");
            helper.assertTrue(Spelers.stats(p).get(Stat.KRITIEK) == krit, "the stats survive a save");
            // the inventory swap (in and out twice): the virtual slots never touch the bag
            SpelerData d = Spelers.data(p);
            Inventaris.naarRijk(p);
            Inventaris.naarThuis(p);
            Inventaris.naarRijk(p);
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.HANDSCHOENEN), h), "gloves survive the swap");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.RING2), r2), "2nd ring survives the swap");
            Inventaris.naarThuis(p);
            d.rijkInventaris.clear();
            // a third ring swaps with RING; a glove may not go into the cape slot
            inv.clearContent();
            ItemStack r3 = stuk("roze_ring", Zeldzaamheid.EPISCH, 1, 6);
            inv.setItem(3, r3.copy());
            helper.assertTrue(Sieraden.wisselMetInventaris(p, 3), "a third ring swaps");
            helper.assertTrue(ItemStack.isSameItemSameComponents(Sieraden.get(p, Slot.RING), r3), "the third ring is in RING");
            helper.assertTrue(ItemStack.isSameItemSameComponents(inv.getItem(3), r1), "the first ring went back to the bag");
            helper.assertTrue(Slot.RING2.past(Slot.RING) && !Slot.CAPE.past(Slot.HANDSCHOENEN), "slot fits");
            inv.clearContent();
            helper.assertTrue(Sieraden.afZonderCheck(p, Slot.CAPE), "the cape comes off");
            helper.assertTrue(Sieraden.get(p, Slot.CAPE).isEmpty(), "cape slot empty");
            boolean terug = false;
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                terug |= ItemStack.isSameItemSameComponents(inv.getItem(i), c);
            }
            helper.assertTrue(terug, "the cape is back in the bag");
        } finally {
            allesAf(p);
            inv.clearContent();
            Spelers.data(p).rijkInventaris.clear();
        }
        helper.succeed();
    }

    /** The slot budget: a virtual-slot piece gives (base + bonuses) x its budget; weapons and armour x1. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void slotBudget(GameTestHelper helper) {
        Uitrusting oorbel = Rollen.rol(BasisTypes.van("pluisoorbel"), 10, Zeldzaamheid.EPISCH, RandomSource.create(9));
        BasisType t = oorbel.basisType();
        Stats ruw = t.statsOp(oorbel.level()).maal(oorbel.zeldzaamheid().factor);
        for (Bonus b : oorbel.bonussen()) {
            ruw.plus(b.stat(), b.waarde());
        }
        double budget = ItemsBalans.budget(Slot.OORBEL);
        helper.assertTrue(budget < 1 && Rollen.budget(oorbel) == budget, "earrings have a budget below 1: " + budget);
        Stats echt = Rollen.stats(oorbel);
        for (Stat s : Stat.values()) {
            helper.assertTrue(Math.abs(echt.get(s) - ruw.get(s) * budget) < 1e-6, s + ": " + echt.get(s) + " = " + ruw.get(s) + " x " + budget);
        }
        helper.assertTrue(ItemsBalans.budget(Slot.WAPEN) == 1 && ItemsBalans.budget(Slot.BORST) == 1, "weapon/armour full budget");
        helper.assertTrue(ItemsBalans.budget(Slot.HANDSCHOENEN) < 1 && ItemsBalans.budget(Slot.CAPE) < 1, "gloves/cape budget");
        helper.succeed();
    }

    /** The new stats: slot pools, element/negeren maths, the status per element, Leven per treffer heals. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void nieuweStats(GameTestHelper helper) {
        helper.assertTrue(Bonussen.mag(Stat.BAZENSCHADE, Slot.RING) && !Bonussen.mag(Stat.BAZENSCHADE, Slot.HOOFD), "boss damage on rings, not helmets");
        helper.assertTrue(!Bonussen.mag(Stat.VERDEDIGING_NEGEREN, null), "the old pool (no slot) has no new stats");
        helper.assertTrue(Bonussen.mag(Stat.KRITIEK, Slot.HOOFD), "old stats roll everywhere");
        RandomSource r = RandomSource.create(42);
        for (int i = 0; i < 300; i++) {
            for (Bonus b : Bonussen.rol(5, 20, Zeldzaamheid.LEGENDARISCH, r, Slot.BORST)) {
                helper.assertTrue(b.stat().ordinal() < Stat.VERDEDIGING_NEGEREN.ordinal(),
                        "no new stat on a chest piece: " + b.stat());
            }
        }
        boolean gezien = false;
        for (int i = 0; i < 300 && !gezien; i++) {
            for (Bonus b : Bonussen.rol(5, 20, Zeldzaamheid.LEGENDARISCH, r, Slot.WAPEN)) {
                gezien |= b.stat() == Stat.LEVEN_PER_TREFFER || b.stat() == Stat.VERDEDIGING_NEGEREN;
            }
        }
        helper.assertTrue(gezien, "weapons do roll the new stats");
        // negeren: 50 % of a x0.7 penalty back -> x0.85; a good match is unchanged; capped at negeren_max
        helper.assertTrue(Math.abs(NieuweStats.elementNa(0.7, 0.5) - 0.85) < 1e-9, "x0.7 -> x0.85");
        helper.assertTrue(NieuweStats.elementNa(1.5, 0.5) == 1.5, "a strong match unchanged");
        helper.assertTrue(NieuweStats.negeren(Stats.van(Stat.VERDEDIGING_NEGEREN, 90)) == ItemsBalans.get("negeren_max") / 100.0, "negeren capped");
        // the attacker part on a normal creature: no boss bonus, only the element part
        helper.assertTrue(NieuweStats.effectVan(Element.VUUR, false) == Status.BRANDEND && NieuweStats.effectVan(Element.WATER, true) == Status.PLAKKERIG
                && NieuweStats.effectVan(Element.ROZE, true) == null && NieuweStats.effectVan(Element.GEEN, false) == null, "status per element");
        helper.succeed();
    }

    /** Pity: +5 % per failure below star 7, reset on success; the talisman turns a lost star into a plain failure (and is used). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void pityEnTalisman(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Uitrusting u = Rollen.rol(BasisTypes.van("kussenzwaard"), 5, Zeldzaamheid.ZELDZAAM, RandomSource.create(1)).metSterren(4);
        helper.assertTrue(Vangnet.kans(u) == SterRegels.KANS[4], "no pity yet");
        Uitrusting na = Vangnet.naPoging(u, u, SterRegels.Uitkomst.MISLUKT);
        na = Vangnet.naPoging(na, na, SterRegels.Uitkomst.STER_KWIJT);
        helper.assertTrue(na.erf().pity() == 2, "two failures = pity 2: " + na.erf().pity());
        Uitrusting op4 = na.metSterren(4);
        helper.assertTrue(Math.abs(Vangnet.kans(op4) - (SterRegels.KANS[4] + 0.10)) < 1e-9, "+10 % after two failures");
        // a die just above the base chance fails without pity but succeeds with it
        double d = SterRegels.KANS[4] + 0.05;
        helper.assertTrue(SterRegels.uitkomst(4, false, d, 0.99, 0.99) == SterRegels.Uitkomst.MISLUKT, "fails without pity");
        helper.assertTrue(Vangnet.uitkomst(op4, d, 0.99, 0.99) == SterRegels.Uitkomst.GELUKT, "succeeds with pity");
        helper.assertTrue(Vangnet.naPoging(op4.metSterren(5), op4, SterRegels.Uitkomst.GELUKT).erf().pity() == 0, "success resets");
        Uitrusting op7 = u.metSterren(7).metErf(u.erf().metPity(5));
        helper.assertTrue(Vangnet.kans(op7) == SterRegels.KANS[7], "no pity help from star 7");
        helper.assertTrue(Vangnet.naPoging(op7, op7, SterRegels.Uitkomst.MISLUKT).erf().pity() == 5, "no pity growth from star 7");
        helper.assertTrue(Vangnet.kans(u.metSterren(2).metErf(u.erf().metPity(30))) == 1.0, "never above 100 %");
        // the talisman: find a seed that loses a star at +5, then roll it with a talisman switched on
        Uitrusting op5 = u.metSterren(5);
        long seed = -1;
        for (long s = 0; s < 20000 && seed < 0; s++) {
            RandomSource r = RandomSource.create(s);
            if (Vangnet.uitkomst(op5, r.nextDouble(), r.nextDouble(), r.nextDouble()) == SterRegels.Uitkomst.STER_KWIJT) {
                seed = s;
            }
        }
        helper.assertTrue(seed >= 0, "a star-loss seed exists");
        p.getInventory().clearContent();
        helper.assertTrue(Vangnet.rol(p, op5, RandomSource.create(seed)) == SterRegels.Uitkomst.STER_KWIJT, "without talisman: star lost");
        p.getInventory().setItem(0, new ItemStack(ItemsRegistry.KNUFFELTALISMAN.get(), 2));
        helper.assertTrue(Vangnet.rol(p, op5, RandomSource.create(seed)) == SterRegels.Uitkomst.STER_KWIJT, "switch off: talisman not used");
        helper.assertTrue(p.getInventory().getItem(0).getCount() == 2, "switch off: still 2");
        Vangnet.zetTalisman(p, true);
        try {
            helper.assertTrue(Vangnet.rol(p, op5, RandomSource.create(seed)) == SterRegels.Uitkomst.MISLUKT, "with talisman: no star lost");
            helper.assertTrue(p.getInventory().getItem(0).getCount() == 1, "one talisman used");
        } finally {
            Vangnet.zetTalisman(p, false);
            p.getInventory().clearContent();
        }
        helper.succeed();
    }

    /** Erfstukken: Vertrouwd levels + growth, worn pieces get xp, doorgroeien (rules + rescale), overvadsen, polijsten. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void erfstukGroei(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Uitrusting u = Rollen.rol(BasisTypes.van("kussenzwaard"), 10, Zeldzaamheid.LEGENDARISCH, RandomSource.create(7));
        helper.assertTrue(Erfstukken.vertrouwd(u) == 0 && Erfstukken.factor(u) == 1.0, "fresh: Vertrouwd 0");
        Uitrusting v3 = Erfstukken.metXp(u, Erfstukken.DREMPELS[2]);
        helper.assertTrue(Erfstukken.vertrouwd(v3) == 3, "Vertrouwd 3 at its threshold");
        double k0 = Rollen.stats(u).get(Stat.KRACHT) - bonus(u, Stat.KRACHT), k3 = Rollen.stats(v3).get(Stat.KRACHT) - bonus(v3, Stat.KRACHT);
        helper.assertTrue(Math.abs(k3 - k0 * 1.06) < 1e-6, "Vertrouwd 3 = +6 % base Kracht (" + k0 + " -> " + k3 + ")");
        helper.assertTrue(Math.abs(Rollen.wapenSchade(v3) - Rollen.wapenSchade(u) * 1.06) < 1e-6, "and +6 % weapon damage");
        helper.assertTrue(Erfstukken.vertrouwd(Erfstukken.metXp(u, 999999)) == 5, "max Vertrouwd 5");
        // worn pieces get xp (a ring in RING2)
        try {
            allesAf(p);
            Sieraden.zet(p, Slot.RING2, stuk("roze_ring", Zeldzaamheid.GEWOON, 1, 3));
            Erfstukken.geefXp(p, Erfstukken.xpVoor(false, true));
            helper.assertTrue(Rollen.van(Sieraden.get(p, Slot.RING2)).erf().xp() == 15, "a boss gives a worn ring 15 item xp");
        } finally {
            allesAf(p);
        }
        // doorgroeien
        helper.assertTrue(!Erfstukken.kanDoorgroeien(u).isEmpty(), "not without Vertrouwd 5");
        Uitrusting klaar = Erfstukken.metXp(u, 999999);
        helper.assertTrue(Erfstukken.kanDoorgroeien(klaar).isEmpty(), "Legendarisch + Vertrouwd 5 can grow");
        Uitrusting g1 = Erfstukken.doorgroei(klaar);
        helper.assertTrue(g1.level() == 20 && g1.gebonden() && g1.erf().groei() == 1 && g1.erf().xp() == 0, "grown: lv 20, bound, Vertrouwd anew");
        for (Bonus b : g1.bonussen()) {
            double[] br = Bonussen.bereik(b.stat(), 20);
            helper.assertTrue(b.waarde() >= br[0] - 1e-9 && b.waarde() <= br[1] + 1e-9, b.stat() + " in the lv-20 range: " + b.waarde());
        }
        helper.assertTrue(Rollen.wapenSchade(g1) > Rollen.wapenSchade(klaar) / Erfstukken.factor(klaar), "more damage after growing");
        Uitrusting g2 = Erfstukken.doorgroei(Erfstukken.metXp(g1, 999999));
        helper.assertTrue(!Erfstukken.kanDoorgroeien(Erfstukken.metXp(g2, 999999)).isEmpty(), "Legendarisch grows only twice");
        Uitrusting guhd = Erfstukken.metXp(Rollen.rol(BasisTypes.van("kussenzwaard"), 10, Zeldzaamheid.GUHDDELIJK, RandomSource.create(8)), 999999);
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(Erfstukken.kanDoorgroeien(guhd).isEmpty(), "Guhddelijk keeps growing (" + i + ")");
            guhd = Erfstukken.metXp(Erfstukken.doorgroei(guhd), 999999);
        }
        helper.assertTrue(guhd.level() == 40, "3x +10 from lv 10 = 40: " + guhd.level());
        helper.assertTrue(!Erfstukken.kanDoorgroeien(Rollen.rol(BasisTypes.van("kussenzwaard"), 10, Zeldzaamheid.EPISCH, RandomSource.create(9))).isEmpty(),
                "Episch never grows");
        // overvadsen: a bonus of an old piece onto a new one, at the same place in the new range; never a double stat
        Uitrusting oud = Rollen.rol(BasisTypes.van("roze_ring"), 5, Zeldzaamheid.GEWOON, RandomSource.create(10))
                .metBonussen(List.of(new Bonus(Stat.KRITIEKSCHADE, Bonussen.bereik(Stat.KRITIEKSCHADE, 5)[1])));
        Uitrusting nieuw = Rollen.rol(BasisTypes.van("roze_ring"), 30, Zeldzaamheid.ZELDZAAM, RandomSource.create(11))
                .metBonussen(List.of(new Bonus(Stat.VADS, 20)));
        helper.assertTrue(Erfstukken.kanOvervadsen(nieuw, oud, 0, -1).isEmpty(), "may add (room for 3)");
        Uitrusting over = Erfstukken.overvads(nieuw, oud, 0, -1, false);
        helper.assertTrue(over.bonussen().size() == 2 && Math.abs(over.bonussen().get(1).waarde() - Bonussen.bereik(Stat.KRITIEKSCHADE, 30)[1]) < 0.051,
                "a top roll stays a top roll at lv 30: " + over.bonussen());
        Uitrusting lief = Erfstukken.overvads(nieuw, oud, 0, -1, true);
        helper.assertTrue(lief.bonussen().get(1).waarde() < over.bonussen().get(1).waarde(), "the sweet miss is lower");
        helper.assertTrue(!Erfstukken.kanOvervadsen(over, oud, 0, 0).isEmpty(), "no second Kritiekschade");
        // polijsten stays inside the range
        Uitrusting pol = over;
        RandomSource r = RandomSource.create(12);
        for (int i = 0; i < 50; i++) {
            pol = Erfstukken.polijst(pol, 0, r);
            double[] br = Bonussen.bereik(Stat.VADS, 30);
            helper.assertTrue(pol.bonussen().get(0).waarde() >= br[0] && pol.bonussen().get(0).waarde() <= br[1], "polished in range");
            helper.assertTrue(pol.bonussen().get(0).stat() == Stat.VADS && pol.bonussen().size() == 2, "same stat, same count");
        }
        helper.succeed();
    }

    private static double bonus(Uitrusting u, Stat s) {
        double n = 0;
        for (Bonus b : u.bonussen()) {
            if (b.stat() == s) {
                n += b.waarde();
            }
        }
        return n + nl.juiced.guhs.feature.vadsrijk.economie.SterRegels.bonus(u).get(s);
    }

    /** The drankjesriem: only potions, stacking, a save, drinking uses one and the shared cooldown blocks the next. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void drankjesriem(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);   // (creative never uses up a potion)
        try {
            for (int i = 0; i < Drankjesriem.VAKKEN; i++) {
                Drankjesriem.zet(p, i, ItemStack.EMPTY);
            }
            helper.assertTrue(Drankjesriem.leg(p, new ItemStack(Items.COBBLESTONE, 3)).getCount() == 3, "cobblestone doesn't fit");
            helper.assertTrue(Drankjesriem.leg(p, new ItemStack(EcoSpul.VADSDRANKJE.get(), 5)).isEmpty(), "potions fit");
            helper.assertTrue(Drankjesriem.leg(p, new ItemStack(EcoSpul.VADSDRANKJE.get(), 3)).isEmpty(), "same potions stack");
            helper.assertTrue(Drankjesriem.get(p, 0).getCount() == 8 && Drankjesriem.get(p, 1).isEmpty(), "8 in slot 1");
            Drankjesriem.leg(p, new ItemStack(EcoSpul.ENERGIEDRANKJE.get(), 2));
            helper.assertTrue(Drankjesriem.get(p, 1).is(EcoSpul.ENERGIEDRANKJE.get()), "another kind goes to slot 2");
            CompoundTag tag = Nbt.write(p.registryAccess(), Spelers.data(p)::serialize);
            Spelers.data(p).deserialize(Nbt.input(p.registryAccess(), tag));
            helper.assertTrue(Drankjesriem.get(p, 0).getCount() == 8, "the belt survives a save");
            helper.assertTrue(Drankjesriem.drinkZonderCheck(p, 0), "drink from slot 1");
            helper.assertTrue(Drankjesriem.get(p, 0).getCount() == 7, "one used: " + Drankjesriem.get(p, 0).getCount());
            helper.assertFalse(Drankjesriem.drinkZonderCheck(p, 0), "the shared potion cooldown blocks the next");
            helper.assertFalse(Drankjesriem.drinkZonderCheck(p, 2), "an empty slot does nothing");
            p.getInventory().clearContent();
            p.getInventory().setItem(4, new ItemStack(EcoSpul.VAHOEGDRANKJE.get(), 2));
            helper.assertTrue(Drankjesriem.legUitInventaris(p, 4) && p.getInventory().getItem(4).isEmpty(), "shift-click into the belt");
            helper.assertTrue(Drankjesriem.get(p, 2).is(EcoSpul.VAHOEGDRANKJE.get()), "into the free slot 3");
        } finally {
            for (int i = 0; i < Drankjesriem.VAKKEN; i++) {
                Drankjesriem.zet(p, i, ItemStack.EMPTY);
            }
            p.getCooldowns().removeCooldown(nl.juiced.guhs.feature.vadsrijk.economie.Drinken.GROEP);
            p.getInventory().clearContent();
        }
        helper.succeed();
    }
}
