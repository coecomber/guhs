package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.GearVergelijk;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.1 "Opruimen": salvage yields per rarity, what may never be salvaged (worn, locked, survival items), the "alle slechtere"
 * selection, the tooltip comparison (shared with the server), and the tutorial step hq03c (by hand and through the quest robot).
 * Run: {@code runGameTestServer -Pgt=VadsrijkOpruimen}.
 */
public class VadsrijkOpruimenGameTests {
    private static final String BATCH = "vadsrijk_opruimen";

    /** A piece without random bonuses (deterministic stats). */
    private static Uitrusting kaal(String type, int level, Zeldzaamheid z) {
        BasisType t = BasisTypes.van(type);
        return new Uitrusting(t.id(), z, level, List.of(), t.element(), 0, 0, false, false, 1L);
    }

    private static ItemStack stack(Uitrusting u) {
        return Rollen.stack(u);
    }

    private static ServerPlayer leeg(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        p.getInventory().setSelectedSlot(0);
        Spelers.data(p).klasse = "knuffelridder";
        return p;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void opbrengstPerZeldzaamheid(GameTestHelper helper) {
        // the material by rarity
        helper.assertTrue(Opruimen.materiaal(Zeldzaamheid.GEWOON) == EcoSpul.SMEEDSTOFJE.get(), "grey -> stofjes");
        helper.assertTrue(Opruimen.materiaal(Zeldzaamheid.ONGEWOON) == EcoSpul.SMEEDSPLINTER.get(), "green -> splinters");
        for (Zeldzaamheid z : new Zeldzaamheid[] {Zeldzaamheid.ZELDZAAM, Zeldzaamheid.EPISCH, Zeldzaamheid.LEGENDARISCH, Zeldzaamheid.GUHDDELIJK}) {
            helper.assertTrue(Opruimen.materiaal(z) == EcoSpul.SMEEDKRISTAL.get(), z + " -> kristallen");
        }
        // the documented numbers (NOTITIES_1101_ITEMS.md)
        int[] lv1 = {2, 1, 1, 2, 4, 8};
        for (Zeldzaamheid z : Zeldzaamheid.values()) {
            Uitrusting u = kaal("kussenzwaard", 1, z);
            helper.assertTrue(Opruimen.aantal(u) == lv1[z.ordinal()], z + " lv1: " + Opruimen.aantal(u));
            helper.assertTrue(Opruimen.opbrengst(u).getCount() == lv1[z.ordinal()], z + " stack count");
        }
        helper.assertTrue(Opruimen.aantal(kaal("kussenzwaard", 40, Zeldzaamheid.GEWOON)) == 4, "grey lv40: 2 + 2");
        helper.assertTrue(Opruimen.aantal(kaal("kussenzwaard", 40, Zeldzaamheid.ONGEWOON)) == 3, "green lv40: 1 + 2");
        helper.assertTrue(Opruimen.aantal(kaal("kussenzwaard", 50, Zeldzaamheid.EPISCH)) == 4, "purple lv50: 2 + 2");
        helper.assertTrue(Opruimen.aantal(kaal("kussenzwaard", 10, Zeldzaamheid.ZELDZAAM).metSterren(5)) == 3, "stars: + 1 per 2");
        // blue+ and starred pieces ask first; grey/green not
        helper.assertFalse(Opruimen.kostbaar(kaal("kussenzwaard", 5, Zeldzaamheid.ONGEWOON)), "green: no confirm");
        helper.assertTrue(Opruimen.kostbaar(kaal("kussenzwaard", 5, Zeldzaamheid.ZELDZAAM)), "blue: confirm");
        helper.assertTrue(Opruimen.kostbaar(kaal("kussenzwaard", 5, Zeldzaamheid.GEWOON).metSterren(1)), "stars: confirm");
        // through a real bag: grey gives stofjes, a blue piece only after the confirm
        ServerPlayer p = leeg(helper);
        p.getInventory().setItem(3, stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON)));
        String[] r = Opruimen.ontmantel(p, 3, false);
        helper.assertTrue(r[0].equals(Opruimen.K + "ontmanteld") && p.getInventory().getItem(3).isEmpty(), "grey salvaged: " + r[0]);
        helper.assertTrue(p.getInventory().countItem(EcoSpul.SMEEDSTOFJE.get()) == 2, "2 stofjes");
        p.getInventory().setItem(4, stack(kaal("kussenzwaard", 1, Zeldzaamheid.ZELDZAAM)));
        helper.assertTrue(Opruimen.ontmantel(p, 4, false)[0].equals(Opruimen.K + "bevestig") && !p.getInventory().getItem(4).isEmpty(),
                "blue waits for the confirm");
        helper.assertTrue(Opruimen.ontmantel(p, 4, true)[0].equals(Opruimen.K + "ontmanteld") && p.getInventory().countItem(EcoSpul.SMEEDKRISTAL.get()) == 1,
                "blue salvaged after the confirm");
        // melting: 3 stofjes -> 1 steentje (only 2: refused, nothing taken)
        helper.assertTrue(Opruimen.omsmelt(p, "stofje_steentje")[0].equals(Opruimen.K + "te_weinig")
                && p.getInventory().countItem(EcoSpul.SMEEDSTOFJE.get()) == 2, "not enough: nothing taken");
        p.getInventory().add(new ItemStack(EcoSpul.SMEEDSTOFJE.get(), 1));
        Opruimen.omsmelt(p, "stofje_steentje");
        helper.assertTrue(p.getInventory().countItem(EcoSpul.SMEEDSTOFJE.get()) == 0 && p.getInventory().countItem(EcoSpul.VERBETERSTEENTJE.get()) == 1,
                "3 stofjes became a Verbetersteentje");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void weigertGedragenVergrendeldSurvival(GameTestHelper helper) {
        ServerPlayer p = leeg(helper);
        // the main hand (selected slot 0) is worn
        p.getInventory().setItem(0, stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON)));
        helper.assertTrue(Opruimen.ontmantel(p, 0, true)[0].equals(Opruimen.K + "gedragen") && !p.getInventory().getItem(0).isEmpty(), "main hand: refused");
        helper.assertTrue(Opruimen.verkoop(p, 0)[0].equals(Opruimen.K + "gedragen") && !p.getInventory().getItem(0).isEmpty(), "main hand: not sold");
        // worn armour (slot 39 = head) is outside the bag
        p.getInventory().setItem(39, stack(kaal("pluishelm", 1, Zeldzaamheid.GEWOON)));
        helper.assertTrue(Opruimen.ontmantel(p, 39, true)[0].equals(Opruimen.K + "gedragen") && !p.getInventory().getItem(39).isEmpty(), "worn helm: refused");
        // a survival item
        p.getInventory().setItem(5, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue(Opruimen.ontmantel(p, 5, true)[0].equals(Opruimen.K + "geen_uitrusting") && p.getInventory().getItem(5).is(Items.DIAMOND_SWORD),
                "survival item: refused");
        // locked (and the lock toggles back)
        p.getInventory().setItem(6, stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON)));
        Opruimen.vergrendel(p, 6);
        helper.assertTrue(Opruimen.vergrendeld(p.getInventory().getItem(6)), "locked");
        helper.assertTrue(Opruimen.ontmantel(p, 6, true)[0].equals(Opruimen.K + "vergrendeld") && !p.getInventory().getItem(6).isEmpty(), "locked: no salvage");
        helper.assertTrue(Opruimen.verkoop(p, 6)[0].equals(Opruimen.K + "vergrendeld") && !p.getInventory().getItem(6).isEmpty(), "locked: no sale");
        helper.assertFalse(EcoUtil.verhandelbaar(p.getInventory().getItem(6)), "locked: no market / trade");
        long voor = Spelers.data(p).munten;
        Winkels.verkoopGrijs(p, Winkels.winkel(Winkels.KAMPWINKEL));
        helper.assertTrue(!p.getInventory().getItem(6).isEmpty() && !p.getInventory().getItem(0).isEmpty(), "Verkoop alle grijze skips locked + worn");
        helper.assertTrue(Spelers.data(p).munten == voor, "nothing else grey to sell");
        Opruimen.vergrendel(p, 6);
        helper.assertFalse(Opruimen.vergrendeld(p.getInventory().getItem(6)), "unlocked again");
        helper.assertTrue(Opruimen.ontmantel(p, 6, false)[0].equals(Opruimen.K + "ontmanteld"), "unlocked: salvage works");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void slechtereSelectie(GameTestHelper helper) {
        ServerPlayer p = leeg(helper);
        p.getInventory().setItem(0, stack(kaal("kussenzwaard", 10, Zeldzaamheid.EPISCH)));   // in the hand
        p.setItemSlot(EquipmentSlot.HEAD, stack(kaal("pluishelm", 20, Zeldzaamheid.EPISCH)));    // worn helm
        p.getInventory().setItem(1, stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON)));   // worse weapon: yes
        p.getInventory().setItem(2, stack(kaal("pluishelm", 1, Zeldzaamheid.GEWOON)));      // worse helm: yes
        ItemStack slot = stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON));
        slot.set(KernRegistry.VERGRENDELD.get(), true);
        p.getInventory().setItem(3, slot);                                                     // locked: no
        p.getInventory().setItem(4, stack(kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON).metSterren(2)));   // stars: no
        p.getInventory().setItem(5, stack(kaal("kussenzwaard", 30, Zeldzaamheid.LEGENDARISCH)));         // better: no
        p.getInventory().setItem(6, stack(kaal("plakpantoffels", 1, Zeldzaamheid.GEWOON)));              // empty feet slot: no
        List<Integer> l = Opruimen.slechtere(p);
        helper.assertTrue(l.equals(List.of(1, 2)), "only the worse, unlocked, starless pieces: " + l);
        helper.assertTrue(Opruimen.oordeel(p, Rollen.van(p.getInventory().getItem(5))) == 1, "better weapon");
        helper.assertTrue(Opruimen.oordeel(p, Rollen.van(p.getInventory().getItem(6))) == Opruimen.LEEG, "empty slot");
        var sam = Opruimen.samenvatting(p, l);
        helper.assertTrue(sam.getIntOr("aantal", 0) == 2 && sam.getIntOr("stofjes", 0) == 4, "summary for the confirm window");
        // sell them all: only those two go, the coins arrive, the rest stays
        long voor = Spelers.data(p).munten;
        Opruimen.verkoopSlechtere(p);
        helper.assertTrue(p.getInventory().getItem(1).isEmpty() && p.getInventory().getItem(2).isEmpty(), "the two worse ones are sold");
        helper.assertTrue(!p.getInventory().getItem(0).isEmpty() && !p.getInventory().getItem(3).isEmpty() && !p.getInventory().getItem(4).isEmpty()
                && !p.getInventory().getItem(5).isEmpty() && !p.getInventory().getItem(6).isEmpty(), "the rest stays");
        helper.assertTrue(Spelers.data(p).munten - voor == sam.getLongOr("waarde", -1), "paid the summary's value");
        helper.assertTrue(Opruimen.ontmantelSlechtere(p)[0].equals(Opruimen.K + "geen_slechtere"), "nothing worse left");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void tooltipVergelijk(GameTestHelper helper) {
        Uitrusting sterk = kaal("kussenzwaard", 20, Zeldzaamheid.EPISCH), zwak = kaal("kussenzwaard", 1, Zeldzaamheid.GEWOON);
        Map<Slot, Uitrusting> sier = new EnumMap<>(Slot.class);
        Map<EquipmentSlot, ItemStack> harnas = new EnumMap<>(EquipmentSlot.class);
        boolean[] leeg = {false};
        // weapon: the main hand
        Uitrusting oud = GearVergelijk.gedragen(zwak, stack(sterk), s -> harnas.getOrDefault(s, ItemStack.EMPTY), sier::get, "knuffelridder", leeg);
        helper.assertTrue(sterk.equals(oud) && !leeg[0], "the weapon in the hand is what it compares with");
        helper.assertTrue(GearVergelijk.vergelijk(zwak, oud, "knuffelridder").oordeel() == -1, "worse than what you wear");
        helper.assertTrue(GearVergelijk.vergelijk(sterk, zwak, "knuffelridder").oordeel() == 1, "better than what you wear");
        GearVergelijk.Uitslag u = GearVergelijk.vergelijk(zwak, sterk, "knuffelridder");
        helper.assertTrue(!u.regels().isEmpty() && u.regels().get(0).verschil() < 0, "the main difference (first line) is negative");
        // another class's weapon in the hand: nothing to compare
        BasisType boog = null;
        for (BasisType t : BasisTypes.alle()) {
            if (t.slot() == Slot.WAPEN && !java.util.Objects.equals(Spelers.wapenKlasse(t), Spelers.wapenKlasse(BasisTypes.van("kussenzwaard")))) {
                boog = t;
                break;
            }
        }
        if (boog != null) {
            Uitrusting b = new Uitrusting(boog.id(), Zeldzaamheid.GEWOON, 5, List.of(), boog.element(), 0, 0, false, false, 2L);
            leeg[0] = false;
            helper.assertTrue(GearVergelijk.gedragen(zwak, stack(b), s -> ItemStack.EMPTY, sier::get, null, leeg) == null && !leeg[0],
                    "another class's weapon: not comparable");
        }
        // armour: the worn piece; empty slot
        leeg[0] = false;
        helper.assertTrue(GearVergelijk.gedragen(kaal("pluishelm", 1, Zeldzaamheid.GEWOON), ItemStack.EMPTY, s -> ItemStack.EMPTY, sier::get, null, leeg) == null
                && leeg[0], "empty head slot");
        // rings: the weaker of the two worn rings
        BasisType ring = null;
        for (BasisType t : BasisTypes.alle()) {
            if (t.slot() == Slot.RING) {
                ring = t;
                break;
            }
        }
        if (ring != null) {
            Uitrusting r1 = new Uitrusting(ring.id(), Zeldzaamheid.EPISCH, 30, List.of(), ring.element(), 0, 0, false, false, 3L);
            Uitrusting r2 = new Uitrusting(ring.id(), Zeldzaamheid.GEWOON, 2, List.of(), ring.element(), 0, 0, false, false, 4L);
            sier.put(Slot.RING, r1);
            sier.put(Slot.RING2, r2);
            Uitrusting nieuw = new Uitrusting(ring.id(), Zeldzaamheid.ONGEWOON, 10, List.of(), ring.element(), 0, 0, false, false, 5L);
            leeg[0] = false;
            helper.assertTrue(r2.equals(GearVergelijk.gedragen(nieuw, ItemStack.EMPTY, s -> ItemStack.EMPTY, sier::get, "knuffelridder", leeg)),
                    "a ring compares with the weaker worn ring");
        }
        // the tooltip's sale value is the shop's
        helper.assertTrue(Waarde.gear(zwak) == Waarde.verkoop(stack(zwak)), "tooltip value = shop value");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void tutorialOpruimen(GameTestHelper helper) {
        ServerPlayer p = leeg(helper);
        Questlog.wisAlles(p);
        Quest q = Quests.van("hq03c_opruimen");
        helper.assertTrue(q != null && q.hoofd() && q.na().contains("hq03b_elementen"), "hq03c follows hq03b");
        Quest kampvuur = Quests.van("hq04_kampvuur");
        helper.assertTrue(kampvuur.na().contains("hq03c_opruimen"), "hq04 follows hq03c");
        // an existing player who already started hq04: hq03c counts as done, nothing new is offered
        Questlog.neemAan(p, kampvuur);
        helper.assertTrue(Questlog.klaar(p, q.id()) && !Questlog.beschikbaar(p, q), "past the tutorial: not affected");
        Questlog.wisAlles(p);
        helper.assertTrue(Questlog.status(p, q.id()) == 0, "a new player has it open");
        // by hand: take it, two old weapons, sell one, salvage the other
        p.getInventory().setItem(0, stack(kaal("kussenzwaard", 1, Zeldzaamheid.ONGEWOON)));   // the hq03 weapon in the hand
        Questlog.neemAan(p, q);
        Opruimen.geefOudeSpullen(p);
        List<Integer> oud = new java.util.ArrayList<>();
        for (int i = 1; i < EcoUtil.TAS; i++) {
            Uitrusting u = Rollen.van(p.getInventory().getItem(i));
            if (u != null) {
                oud.add(i);
                helper.assertTrue(Opruimen.oordeel(p, u) == -1, "the old weapon is worse than the starter gear");
            }
        }
        helper.assertTrue(oud.size() == 2, "two old weapons: " + oud);
        helper.assertTrue(Opruimen.verkoop(p, oud.get(0))[0].equals("vadsrijk.economie.winkel.verkocht"), "sold one");
        helper.assertTrue(Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1), "step 1 done");
        Opruimen.ontmantel(p, oud.get(1), false);
        helper.assertTrue(Questlog.allesKlaar(p, q), "both steps done");
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** The quest robot plays zone 1 through the real quest engine: hq03c is "gehaald" and the chain goes on to hq04. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 1200)
    public static void questrobotZone1(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var r = nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot.speel(p, 1);
        var u = r.quests.get("hq03c_opruimen");
        helper.assertTrue(u != null && u.status.equals("gehaald"), "hq03c: " + (u == null ? "missing" : u.status + " " + u.stap + " " + u.waarom));
        var k = r.quests.get("hq04_kampvuur");
        helper.assertTrue(k != null && k.status.equals("gehaald"), "hq04 after it: " + (k == null ? "missing" : k.status + " " + k.waarom));
        Questlog.wisAlles(p);
        helper.succeed();
    }
}
