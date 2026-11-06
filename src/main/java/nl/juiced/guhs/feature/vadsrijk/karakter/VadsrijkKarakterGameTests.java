package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-KARAKTER: the migration (an old save becomes slot 0 unchanged), slot isolation (levels, coins, quests, flags, the Vadsrijk
 * inventory), account-wide sharing (pets, mounts, titles, the account stores, tips), the game modes (only lower, xp factor) and
 * the Eénvads retirement (Normaal + badge + an entry in the hall). Run: {@code runGameTestServer -Pgt=VadsrijkKarakter}.
 */
public class VadsrijkKarakterGameTests {
    private static final String BATCH = "vadsrijk_karakter";

    @GuhTest(template = "empty", batch = BATCH)
    public static void migratie(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        Spelers.zetLevel(p, 12);
        d.munten = 4321;
        d.extra("npc").putBoolean("vlag_test", true);
        d.extra.remove(Karakter.LIJST);   // = an old save from before the character slots
        helper.assertTrue(Karakter.slot(p) == 0, "an old save is slot 0");
        CompoundTag l = Karakter.lijst(p);
        helper.assertTrue(l.getIntOr("actief", -1) == 0 && Karakter.bestaat(p, 0) && !Karakter.bestaat(p, 1) && !Karakter.bestaat(p, 2),
                "migrated: slot 0 active, 1 and 2 free");
        helper.assertTrue(Karakter.modus(p) == Modus.NORMAAL && Karakter.naam(p).equals(p.getGameProfile().name()), "slot 0 = Normaal, named after the player");
        helper.assertTrue(d.level() == Spelers.level(p) && Spelers.level(p) >= 12 && d.munten == 4321 && d.extra("npc").getBooleanOr("vlag_test", false),
                "the data itself is untouched");
        helper.assertTrue(Karakter.sleutel(p).tekst().equals(p.getUUID() + "/0") && Karakter.Sleutel.van(Karakter.sleutel(p).tekst()).equals(Karakter.sleutel(p)),
                "the key");
        for (String r : KarakterTest.migratie(p)) {
            helper.assertTrue(r.startsWith("OK"), r);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void slotIsolatie(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        Spelers.zetLevel(p, 15);
        d.munten = 500;
        d.extra("npc").putBoolean("zone1_klaar", true);
        d.extra("wereld").putBoolean("deur_test", true);
        d.gezien.add("npc:opening");
        d.boom.computeIfAbsent("knuffelridder", k -> new java.util.LinkedHashSet<>()).add("test_knoop");
        // the Vadsrijk inventory is live (as inside): one item for knight A
        Inventaris.naarRijk(p);
        p.getInventory().setItem(3, new ItemStack(Items.GOLDEN_APPLE, 2));
        try {
            helper.assertTrue(Karakters.maak(p, 1, "Tweede Ridder", Modus.NORMAAL), "create slot 1");
            helper.assertTrue(!Karakters.maak(p, 1, "Nog een", Modus.NORMAAL), "slot 1 is taken now");
            helper.assertTrue(Karakters.wissel(p, 1, true), "switch to slot 1");
            SpelerData n = Spelers.data(p);
            helper.assertTrue(Karakter.slot(p) == 1 && Karakter.naam(p).equals("Tweede Ridder"), "slot 1 active");
            helper.assertTrue(n.level() == 1 && n.munten == 0, "a fresh knight: level 1, no coins (got " + n.level() + ", " + n.munten + ")");
            helper.assertTrue(!n.extra("npc").getBooleanOr("zone1_klaar", false) && !n.extra("wereld").getBooleanOr("deur_test", false),
                    "no quests or zone flags of knight A");
            helper.assertTrue(!n.gezien.contains("npc:opening") && n.boom.isEmpty(), "no story scenes or skill tree of knight A");
            helper.assertTrue(n.binnen && p.getInventory().getItem(3).isEmpty() && p.getInventory().countItem(Items.GOLDEN_APPLE) == 0,
                    "knight B has an empty Vadsrijk inventory");
            p.getInventory().setItem(5, new ItemStack(Items.CARROT, 7));
            Spelers.geefMunten(p, 77);
            n.extra("npc").putBoolean("eigen_b", true);
            helper.assertTrue(Karakters.wissel(p, 0, true), "back to slot 0");
            SpelerData a = Spelers.data(p);
            helper.assertTrue(a.level() >= 15 && a.munten == 500, "knight A again: level + coins");
            helper.assertTrue(a.extra("npc").getBooleanOr("zone1_klaar", false) && !a.extra("npc").getBooleanOr("eigen_b", false)
                    && a.extra("wereld").getBooleanOr("deur_test", false) && a.gezien.contains("npc:opening"), "A's quests/flags back, none of B's");
            helper.assertTrue(p.getInventory().countItem(Items.GOLDEN_APPLE) == 2 && p.getInventory().countItem(Items.CARROT) == 0,
                    "A's inventory back, without B's carrots");
            CompoundTag b = Karakter.info(p, 1);
            helper.assertTrue(b.getLongOr("munten", -1) == 77 && b.contains("data") && !b.getBooleanOr("nieuw", false), "B is saved in its slot");
            helper.assertTrue(Karakters.wissel(p, 1, true) && p.getInventory().countItem(Items.CARROT) == 7 && Spelers.data(p).munten == 77,
                    "and B again: the carrots + coins are still B's");
            helper.assertTrue(Karakters.wissel(p, 0, true), "back to A");
        } finally {
            Inventaris.naarThuis(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void accountBreed(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Huisdieren.reset(p);
        helper.assertTrue(Huisdieren.geef(p, "galmwolf"), "a pet");
        helper.assertTrue(Rijdieren.geef(p, RijdierSoort.PLUIMGUH), "a mount");
        Cosmetica.geefTitel(p, Cosmetica.RIDDER);
        Karakter.account(p, "bank").putInt("munten_test", 9);
        Spelers.data(p).tips.add("vadsrijk.kern.tip.welkom");
        Spelers.data(p).extra("economie").putInt("per_karakter_test", 3);
        helper.assertTrue(Karakters.maak(p, 2, "Derde", Modus.KNUFFEL) && Karakters.wissel(p, 2, true), "create + switch to slot 2");
        helper.assertTrue(Huisdieren.heeft(p, "galmwolf") && Huisdieren.actief(p) == Huisdieren.GALMWOLF, "pets are account-wide");
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.PLUIMGUH), "mounts are account-wide");
        helper.assertTrue(Cosmetica.heeftTitel(p, Cosmetica.RIDDER), "titles are account-wide");
        helper.assertTrue(Karakter.account(p, "bank").getIntOr("munten_test", 0) == 9, "Karakter.account stores are account-wide");
        helper.assertTrue(Spelers.data(p).tips.contains("vadsrijk.kern.tip.welkom"), "seen tips are account-wide");
        helper.assertTrue(Spelers.data(p).extra("economie").getIntOr("per_karakter_test", 0) == 0, "other economie keys stay per character");
        // a change on B is seen by A
        Karakter.account(p, "bank").putInt("munten_test", 11);
        helper.assertTrue(Huisdieren.geef(p, "koning_klodder"), "a pet on B");
        helper.assertTrue(Karakters.wissel(p, 0, true), "back to A");
        helper.assertTrue(Karakter.account(p, "bank").getIntOr("munten_test", 0) == 11 && Huisdieren.heeft(p, "koning_klodder"),
                "A sees what B changed in the account part");
        helper.assertTrue(Spelers.data(p).extra("economie").getIntOr("per_karakter_test", 0) == 3, "A's own economie key is back");
        Huisdieren.reset(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void modiEnPensioen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        // only lower
        helper.assertTrue(Karakters.maak(p, 1, "Held", Modus.EENVADS), "an Eénvads knight");
        helper.assertTrue(!Karakters.zetModus(p, 0, Modus.EENVADS, false), "Normaal can't go up to Eénvads");
        helper.assertTrue(Karakters.zetModus(p, 0, Modus.KNUFFEL, false) && !Karakters.zetModus(p, 0, Modus.NORMAAL, false),
                "Normaal -> Knuffelmodus yes, back up no");
        helper.assertTrue(Modus.EENVADS.magNaar(Modus.NORMAAL) && Modus.EENVADS.magNaar(Modus.KNUFFEL) && !Modus.KNUFFEL.magNaar(Modus.NORMAAL),
                "the order");
        // xp factor: Knuffelmodus 0.9 x, Eénvads 1.1 x of the same xp
        Spelers.zetLevel(p, 1);
        long voor = Spelers.data(p).generiekXp;
        Spelers.geefXp(p, 1000, "test", null);
        long knuffel = Spelers.data(p).generiekXp - voor;
        helper.assertTrue(Karakters.wissel(p, 1, true) && Karakter.modus(p) == Modus.EENVADS, "switch to the Eénvads knight");
        voor = Spelers.data(p).generiekXp;
        Spelers.geefXp(p, 1000, "test", null);
        long eenvads = Spelers.data(p).generiekXp - voor;
        helper.assertTrue(Math.abs(eenvads * 0.9 - knuffel * 1.1) <= 2, "xp: Eénvads " + eenvads + " vs Knuffel " + knuffel);
        helper.assertTrue(Modus.KNUFFEL.schadeFactor == 0.5 && Modus.NORMAAL.schadeFactor == 1.0, "Knuffelmodus: half damage");
        // retire
        RustendeRidders hal = RustendeRidders.get(helper.getLevel().getServer());
        int was = hal.ridders().size();
        helper.assertTrue(Badges.code(p).equals("eenvads"), "a gold heart while alive");
        helper.assertTrue(Karakters.pensioen(p, "gametest"), "fainting = retiring");
        helper.assertTrue(Karakter.modus(p) == Modus.NORMAAL && Karakter.gepensioneerd(p), "plays on as Normaal, retired");
        helper.assertTrue(Karakter.bestaat(p, 1) && Karakter.slot(p) == 1, "never deleted");
        helper.assertTrue(Badges.code(p).equals("p"), "the retired badge");
        List<RustendeRidders.Ridder> l = hal.ridders();
        helper.assertTrue(l.size() == was + 1 && l.get(l.size() - 1).naam().equals("Held") && l.get(l.size() - 1).speler().equals(p.getUUID()),
                "a statue entry in the hall");
        helper.assertTrue(!Karakters.pensioen(p, "nog eens"), "a retired knight doesn't retire twice");
        hal.ridders.removeIf(r -> r.speler().equals(p.getUUID()));
        hal.setDirty();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void namen(GameTestHelper helper) {
        helper.assertTrue("Sir Pluis".equals(Karakters.schoon("  Sir   Pluis ")), "spaces cleaned");
        helper.assertTrue(Karakters.schoon("<>!!") == null, "nothing usable = no name");
        helper.assertTrue(Karakters.schoon("Abcdefghijklmnopqrstuvwxyz").length() == Karakters.NAAM_MAX, "max length");
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(!Karakters.maak(p, 1, "   ", Modus.NORMAAL), "no empty name");
        helper.assertTrue(Karakters.maak(p, 1, "Pluis", Modus.NORMAAL) && Karakters.hernoem(p, 1, "Pluisje") && Karakter.info(p, 1).getStringOr("naam", "")
                .equals("Pluisje"), "rename");
        helper.assertTrue(Karakters.vrijSlot(p) == 2, "the next free slot");
        helper.succeed();
    }
}
