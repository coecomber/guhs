package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk economie: only the risky logic. Stars never lose an item (and verpluisd always repairs), the market moves an item
 * and its munten together or not at all, the trade window only swaps after both confirm and re-validates, mount xp levels up.
 * Run: {@code runGameTestServer -Pgt=VadsrijkEconomie}.
 */
public class VadsrijkEconomieGameTests {
    private static final String BATCH = "vadsrijk_economie";

    private static ItemStack gear(String type, int level, Zeldzaamheid z, long seed) {
        return Rollen.stack(Rollen.rol(BasisTypes.van(type), level, z, RandomSource.create(seed)));
    }

    private static int telAlles(ServerPlayer p, net.minecraft.world.item.Item item) {
        return EcoUtil.tel(p.getInventory(), item);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void sterrenNooitPermanentVerlies(GameTestHelper helper) {
        // 1. the pure rules: every outcome from every star count keeps the piece, moves at most one star, verpluisd only from +6
        RandomSource r = RandomSource.create(1);
        for (int s = 0; s < SterRegels.MAX; s++) {
            Uitrusting u = Rollen.rol(BasisTypes.van("kussenzwaard"), 10, Zeldzaamheid.EPISCH, r).metSterren(s);
            for (int i = 0; i < 2000; i++) {
                SterRegels.Uitkomst k = SterRegels.uitkomst(s, false, r.nextDouble(), r.nextDouble(), r.nextDouble());
                Uitrusting na = SterRegels.pasToe(u, k);
                helper.assertTrue(na != null && na.type().equals(u.type()) && na.bonussen().equals(u.bonussen()), "the piece stays the same piece");
                helper.assertTrue(Math.abs(na.sterren() - s) <= 1 && na.sterren() >= 0 && na.sterren() <= SterRegels.MAX, "at most one star moves");
                helper.assertTrue(k != SterRegels.Uitkomst.VERPLUISD || s >= SterRegels.PLUIS_VANAF, "verpluisd only from +6");
                helper.assertTrue(k != SterRegels.Uitkomst.STER_KWIJT || s >= SterRegels.KWIJT_VANAF, "star loss only from +3");
                if (na.verpluisd()) {
                    Uitrusting heel = SterRegels.repareer(na);
                    helper.assertTrue(!heel.verpluisd() && heel.sterren() == na.sterren() && heel.bonussen().equals(u.bonussen()), "repair restores all");
                    helper.assertTrue(Rollen.stats(na).alles().isEmpty(), "verpluisd gives nothing");
                }
            }
        }
        helper.assertTrue(SterRegels.uitkomst(SterRegels.MAX, false, 0, 0, 0) == SterRegels.Uitkomst.KAN_NIET, "no 11th star");
        helper.assertTrue(SterRegels.uitkomst(7, true, 0, 0, 0) == SterRegels.Uitkomst.KAN_NIET, "no stars on a verpluisd piece");
        // more stars = more stats
        Uitrusting basis = Rollen.rol(BasisTypes.van("pluishelm"), 5, Zeldzaamheid.GEWOON, r);
        double vorig = -1;
        for (int s = 0; s <= SterRegels.MAX; s++) {
            double v = SterRegels.bonus(basis.metSterren(s)).get(nl.juiced.guhs.feature.vadsrijk.kern.Stat.VERDEDIGING);
            helper.assertTrue(v > vorig || s == 0, "each star adds verdediging");
            vorig = v;
        }
        // 2. through the smid on a real stack: hundreds of attempts, the item never leaves its slot
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).munten = 1_000_000_000L;
        p.getInventory().setItem(0, gear("kussenzwaard", 8, Zeldzaamheid.ZELDZAAM, 7));
        for (int i = 1; i <= 8; i++) {
            p.getInventory().setItem(i, new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 99));
        }
        p.getInventory().setItem(9, new ItemStack(EcoSpul.PLUISDRAAD.get(), 99));
        p.getInventory().setItem(10, new ItemStack(EcoSpul.PLUISDRAAD.get(), 99));
        int verpluisd = 0, max = 0;
        RandomSource rs = RandomSource.create(42);
        for (int i = 0; i < 300; i++) {
            Uitrusting voor = Rollen.van(p.getInventory().getItem(0));
            helper.assertTrue(voor != null, "the piece is still in slot 0");
            if (voor.verpluisd()) {
                verpluisd++;
                Smid.repareer(p, 0);
                Uitrusting na = Rollen.van(p.getInventory().getItem(0));
                helper.assertTrue(na != null && !na.verpluisd() && na.sterren() == voor.sterren(), "repaired with its stars");
                continue;
            }
            if (voor.sterren() >= SterRegels.MAX) {
                max++;
                p.getInventory().getItem(0).set(KernRegistry.UITRUSTING.get(), voor.metSterren(6));
                continue;
            }
            int stenen = telAlles(p, EcoSpul.VERBETERSTEENTJE.get());
            if (stenen < 5) {
                for (int s = 1; s <= 8; s++) {
                    p.getInventory().setItem(s, new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 99));
                }
                stenen = telAlles(p, EcoSpul.VERBETERSTEENTJE.get());
            }
            long munten = Spelers.data(p).munten;
            Smid.ster(p, 0, rs);
            Uitrusting na = Rollen.van(p.getInventory().getItem(0));
            helper.assertTrue(na != null, "a star attempt never removes the piece");
            helper.assertTrue(stenen - telAlles(p, EcoSpul.VERBETERSTEENTJE.get()) == SterRegels.steentjes(voor.sterren()), "the right steentjes were used");
            helper.assertTrue(munten - Spelers.data(p).munten == SterRegels.prijs(voor.level(), voor.sterren()), "the right price was paid");
        }
        helper.assertTrue(verpluisd + max >= 0, "(stats)");
        // 3. reroll with a kept bonus: same count, the kept bonus stays, no duplicate stats
        Uitrusting ep = Rollen.rol(BasisTypes.van("klodderhesje"), 12, Zeldzaamheid.LEGENDARISCH, RandomSource.create(3));
        for (int i = 0; i < 200; i++) {
            int houd = i % ep.bonussen().size();
            var nieuw = Smid.herrolBonussen(ep, houd, rs);
            helper.assertTrue(nieuw.size() == ep.bonussen().size(), "same number of bonuses");
            helper.assertTrue(nieuw.get(houd).equals(ep.bonussen().get(houd)), "the kept bonus stays in its place");
            helper.assertTrue(nieuw.stream().map(b -> b.stat()).distinct().count() == nieuw.size(), "no stat twice");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void marktKoopVerkoopAtomisch(GameTestHelper helper) {
        MarktData m = new MarktData();
        ServerPlayer verkoper = GuhMockPlayer.of(helper), koper = GuhMockPlayer.of(helper);
        ItemStack zwaard = gear("klodderkling", 6, Zeldzaamheid.EPISCH, 11);
        verkoper.getInventory().setItem(3, zwaard.copy());
        Spelers.data(verkoper).munten = 1000;
        // gebonden can't be listed
        ItemStack gebonden = gear("pluishelm", 3, Zeldzaamheid.ZELDZAAM, 12);
        gebonden.set(KernRegistry.UITRUSTING.get(), Rollen.van(gebonden).metGebonden(true));
        verkoper.getInventory().setItem(4, gebonden);
        helper.assertTrue(!Markt.zetTeKoop(verkoper, m, 4, 500).isEmpty() && !verkoper.getInventory().getItem(4).isEmpty(), "gebonden stays");
        helper.assertTrue(m.alle().isEmpty(), "nothing listed");
        // list: the item leaves the bag, the fee is paid, the listing has it
        helper.assertTrue(Markt.zetTeKoop(verkoper, m, 3, 2000).isEmpty(), "listed");
        helper.assertTrue(verkoper.getInventory().getItem(3).isEmpty(), "out of the bag");
        helper.assertTrue(Spelers.data(verkoper).munten == 1000 - Markt.fee(2000), "fee paid");
        long id = m.alle().get(0).id();
        // a buyer without enough munten: nothing moves
        Spelers.data(koper).munten = 1999;
        helper.assertTrue(!Markt.koop(koper, m, id).isEmpty(), "too poor");
        helper.assertTrue(m.aanbod(id) != null && Spelers.data(koper).munten == 1999 && m.opbrengst(verkoper.getUUID()) == 0, "nothing moved (poor)");
        // a buyer with a full bag: nothing moves
        Spelers.data(koper).munten = 5000;
        for (int i = 0; i < EcoUtil.TAS; i++) {
            koper.getInventory().setItem(i, new ItemStack(Items.DIRT));
        }
        helper.assertTrue(!Markt.koop(koper, m, id).isEmpty(), "bag full");
        helper.assertTrue(m.aanbod(id) != null && Spelers.data(koper).munten == 5000, "nothing moved (full)");
        koper.getInventory().setItem(20, ItemStack.EMPTY);
        // buying your own: no
        helper.assertTrue(!Markt.koop(verkoper, m, id).isEmpty(), "not your own");
        // the buy: item to the buyer, munten to the seller's proceeds, listing gone
        helper.assertTrue(Markt.koop(koper, m, id).isEmpty(), "bought");
        helper.assertTrue(ItemStack.matches(koper.getInventory().getItem(20), zwaard), "the buyer has exactly that item");
        helper.assertTrue(Spelers.data(koper).munten == 3000 && m.opbrengst(verkoper.getUUID()) == 2000 && m.aanbod(id) == null, "munten moved");
        // a second buy of the same listing: nothing (no dupe, no double charge)
        koper.getInventory().setItem(21, ItemStack.EMPTY);
        helper.assertTrue(!Markt.koop(koper, m, id).isEmpty() && Spelers.data(koper).munten == 3000 && koper.getInventory().getItem(21).isEmpty(),
                "no second item");
        // proceeds: once
        long voor = Spelers.data(verkoper).munten;
        helper.assertTrue(Markt.haalOp(verkoper, m) == 2000 && Spelers.data(verkoper).munten == voor + 2000, "proceeds collected");
        helper.assertTrue(Markt.haalOp(verkoper, m) == 0 && Spelers.data(verkoper).munten == voor + 2000, "only once");
        // withdraw
        verkoper.getInventory().setItem(5, new ItemStack(EcoSpul.VADSDRANKJE.get(), 3));
        helper.assertTrue(Markt.zetTeKoop(verkoper, m, 5, 100).isEmpty(), "potions listed");
        long id2 = m.alle().get(0).id();
        helper.assertTrue(!Markt.trekIn(koper, m, id2).isEmpty(), "someone else can't withdraw");
        helper.assertTrue(Markt.trekIn(verkoper, m, id2).isEmpty() && m.alle().isEmpty() && telAlles(verkoper, EcoSpul.VADSDRANKJE.get()) == 3, "withdrawn");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void handelBevestigTweezijdig(GameTestHelper helper) {
        Handel.alleenInRijk = false;
        try {
            ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
            ItemStack zwaard = gear("kussenzwaard", 4, Zeldzaamheid.ZELDZAAM, 21), boog = gear("pluisboog", 4, Zeldzaamheid.EPISCH, 22);
            a.getInventory().setItem(0, zwaard.copy());
            b.getInventory().setItem(0, boog.copy());
            Spelers.data(a).munten = 500;
            Spelers.data(b).munten = 0;
            Handel.Sessie s = Handel.start(a, b);
            helper.assertTrue(Handel.leg(a, 0).isEmpty() && Handel.leg(b, 0).isEmpty(), "offered");
            Handel.munten(a, 300);
            // confirm before ready: refused
            helper.assertTrue(!Handel.bevestig(a).isEmpty(), "first both ready");
            Handel.klaar(a);
            Handel.klaar(b);
            Handel.bevestig(a);
            helper.assertTrue(ItemStack.matches(a.getInventory().getItem(0), zwaard), "one confirm moves nothing");
            // a change resets both
            Handel.munten(a, 200);
            helper.assertTrue(!s.a.klaar && !s.b.klaar && !s.a.bevestigd, "a change resets both");
            // tamper: a removes the offered item behind the window's back, then both confirm -> nothing happens
            Handel.klaar(a);
            Handel.klaar(b);
            ItemStack weg = a.getInventory().getItem(0).copy();
            a.getInventory().setItem(0, ItemStack.EMPTY);
            a.getInventory().setItem(7, weg);
            Handel.bevestig(a);
            helper.assertTrue(!Handel.bevestig(b).isEmpty(), "re-validation caught the change");
            helper.assertTrue(ItemStack.matches(b.getInventory().getItem(0), boog) && Spelers.data(a).munten == 500 && Spelers.data(b).munten == 0,
                    "nothing moved after the failed check");
            helper.assertTrue(ItemStack.matches(a.getInventory().getItem(7), zwaard), "a still has the sword (no dupe)");
            // put it back properly: offer again and do the real swap
            a.getInventory().setItem(0, a.getInventory().getItem(7).copy());
            a.getInventory().setItem(7, ItemStack.EMPTY);
            helper.assertTrue(Handel.sessie(a) == s, "session still open");
            Handel.klaar(a);
            Handel.klaar(b);
            Handel.bevestig(a);
            helper.assertTrue(Handel.bevestig(b).isEmpty(), "swapped");
            int zwaarden = 0, bogen = 0;
            for (ServerPlayer x : List.of(a, b)) {
                for (int i = 0; i < EcoUtil.TAS; i++) {
                    ItemStack it = x.getInventory().getItem(i);
                    zwaarden += ItemStack.matches(it, zwaard) ? 1 : 0;
                    bogen += ItemStack.matches(it, boog) ? 1 : 0;
                }
            }
            helper.assertTrue(zwaarden == 1 && bogen == 1, "exactly one of each after the swap");
            helper.assertTrue(EcoUtil.tel(b.getInventory(), zwaard.getItem()) == 1 && EcoUtil.tel(a.getInventory(), boog.getItem()) == 1, "each got the other's");
            helper.assertTrue(Spelers.data(a).munten == 300 && Spelers.data(b).munten == 200, "munten moved");
            helper.assertTrue(Handel.sessie(a) == null && Handel.sessie(b) == null, "session closed");
            // gebonden can't be offered
            ItemStack gb = gear("pluishelm", 2, Zeldzaamheid.GEWOON, 23);
            gb.set(KernRegistry.UITRUSTING.get(), Rollen.van(gb).metGebonden(true));
            a.getInventory().setItem(30, gb);
            Handel.start(a, b);
            helper.assertTrue(!Handel.leg(a, 30).isEmpty() && Handel.sessie(a).a.slots.isEmpty(), "gebonden refused");
            Handel.annuleer(a, "x");
        } finally {
            Handel.alleenInRijk = true;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void rijdierXp(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(Rijdieren.actief(p) == null, "no mount yet");
        helper.assertTrue(Rijdieren.geefEerste(p) && !Rijdieren.geefEerste(p), "the first mount once");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.HOEDHUPPER, "hoedhupper (G-MOUNTS: the key calls it, no rijfluitje needed)");
        helper.assertTrue(Rijdieren.level(p, RijdierSoort.HOEDHUPPER) == 1, "level 1");
        Rijdieren.geefXp(p, RijdierSoort.HOEDHUPPER, RijdierSoort.xpNodig(1) - 1);
        helper.assertTrue(Rijdieren.level(p, RijdierSoort.HOEDHUPPER) == 1, "not yet");
        Rijdieren.geefXp(p, RijdierSoort.HOEDHUPPER, 1);
        helper.assertTrue(Rijdieren.level(p, RijdierSoort.HOEDHUPPER) == 2 && Rijdieren.xp(p, RijdierSoort.HOEDHUPPER) == 0, "level 2");
        Rijdieren.geefXp(p, RijdierSoort.HOEDHUPPER, 1_000_000);
        helper.assertTrue(Rijdieren.level(p, RijdierSoort.HOEDHUPPER) == RijdierSoort.MAX_LEVEL, "capped at max");
        helper.assertTrue(Cosmetica.heeftTitel(p, Cosmetica.RIJDIERMEESTER), "title at max level");
        helper.assertTrue(RijdierSoort.HOEDHUPPER.snelheid(10) > RijdierSoort.HOEDHUPPER.snelheid(1)
                && RijdierSoort.HOEDHUPPER.sprong(10) > RijdierSoort.HOEDHUPPER.sprong(1), "levels make it faster and bouncier");
        helper.assertTrue(Rijdieren.geefXp(p, RijdierSoort.KLODDERROS, 100) == 0, "no xp for a mount you don't have");
        Rijdieren.geef(p, RijdierSoort.KLODDERROS);
        Rijdieren.kies(p, RijdierSoort.KLODDERROS);
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.KLODDERROS, "pick in the stal");
        helper.succeed();
    }
}
