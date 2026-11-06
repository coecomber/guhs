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
        helper.assertTrue(Karakter.modus(p) == Modus.NORMAAL && Karakter.naam(p).equals(p.getGameProfile().name()) && Karakter.opslagSleutel(p).equals("1"),
                "slot 0 = the left slot, Normaal, named after the player, knapzak key 1");
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
            helper.assertTrue(Karakters.maak(p, 1, "Tweede Ridder"), "create slot 1");
            helper.assertTrue(!Karakters.maak(p, 1, "Nog een"), "slot 1 is taken now");
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
        helper.assertTrue(Karakters.maak(p, 2, "Derde") && Karakters.wissel(p, 2, true), "create + switch to slot 2");
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

    /** U-KARAKTER: every slot has its own fixed mode (left Normaal, middle Knuffelmodus, right Eénvads); nothing can change it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vasteModi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(Modus.vanSlot(0) == Modus.NORMAAL && Modus.vanSlot(1) == Modus.KNUFFEL && Modus.vanSlot(2) == Modus.EENVADS, "slot -> mode");
        helper.assertTrue(Modus.slotVan("knuffel") == 1 && Modus.slotVan("eenvads") == 2 && Modus.slotVan("0") == 0 && Modus.slotVan("x") == -1, "names");
        helper.assertTrue(Karakters.maak(p, 1, "Knuf") && Karakters.maak(p, 2, "Held"), "make middle + right");
        helper.assertTrue(Karakter.info(p, 1).getStringOr("modus", "").equals("knuffel") && Karakter.info(p, 2).getStringOr("modus", "").equals("eenvads"),
                "each gets its slot's mode");
        helper.assertTrue(Karakter.modus(p) == Modus.NORMAAL, "the left knight plays Normaal");
        // xp factor: Knuffelmodus 0.9 x, Eénvads 1.1 x of the same xp
        helper.assertTrue(Karakters.wissel(p, 1, true) && Karakter.modus(p) == Modus.KNUFFEL, "middle = Knuffelmodus");
        Spelers.zetLevel(p, 1);
        long voor = Spelers.data(p).generiekXp;
        Spelers.geefXp(p, 1000, "test", null);
        long knuffel = Spelers.data(p).generiekXp - voor;
        helper.assertTrue(Karakters.wissel(p, 2, true) && Karakter.modus(p) == Modus.EENVADS, "right = Eénvads");
        voor = Spelers.data(p).generiekXp;
        Spelers.geefXp(p, 1000, "test", null);
        long eenvads = Spelers.data(p).generiekXp - voor;
        helper.assertTrue(Math.abs(eenvads * 0.9 - knuffel * 1.1) <= 2, "xp: Eénvads " + eenvads + " vs Knuffel " + knuffel);
        helper.assertTrue(Modus.KNUFFEL.schadeFactor == 0.5 && Modus.NORMAAL.schadeFactor == 1.0, "Knuffelmodus: half damage");
        // the screen has no mode action any more: an old "modus" action does nothing
        CompoundTag a = new CompoundTag();
        a.putInt("slot", 2);
        a.putString("modus", "knuffel");
        KarakterScherm.doe(p, "modus", a);
        helper.assertTrue(Karakter.modus(p) == Modus.EENVADS && Karakter.info(p, 2).getStringOr("modus", "").equals("eenvads"), "the mode can't be changed");
        helper.assertTrue(!Karakters.maak(p, 1, "Nog een"), "a slot with a knight can't be made again");
        helper.assertTrue(Karakters.wissel(p, 0, true), "back to the left knight");
        helper.succeed();
    }

    /**
     * U-KARAKTER: an Eénvads knight retires: a statue in the hall, the right slot shows him resting (not playable), you go on as
     * the left knight, and the right slot can make a NEW Eénvads knight (the old one goes to the archive with his data).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void pensioenEnNieuweEenvads(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        RustendeRidders hal = RustendeRidders.get(helper.getLevel().getServer());
        int was = hal.ridders().size();
        helper.assertTrue(Karakters.maak(p, 2, "Held") && Karakters.wissel(p, 2, true), "an Eénvads knight, playing");
        Spelers.data(p).munten = 99;
        helper.assertTrue(Badges.code(p).equals("eenvads"), "a gold heart while alive");
        helper.assertTrue(Karakters.pensioen(p, "gametest"), "fainting = retiring");
        helper.assertTrue(Karakter.gepensioneerd(p) && Karakter.modus(p) == Modus.NORMAAL && Badges.code(p).equals("p"), "retired: star badge");
        List<RustendeRidders.Ridder> l = hal.ridders();
        helper.assertTrue(l.size() == was + 1 && l.get(l.size() - 1).naam().equals("Held"), "a statue entry in the hall");
        helper.assertTrue(!Karakters.pensioen(p, "nog eens"), "a retired knight doesn't retire twice");
        helper.assertTrue(Karakters.naRust(p), "after retiring you are switched away");
        helper.assertTrue(Karakter.slot(p) == 0 && !Karakter.gepensioneerd(p), "... to the left (Normaal) knight");
        helper.assertTrue(Karakter.rust(p, 2) && Karakter.info(p, 2).getLongOr("munten", 0) == 99, "the right slot keeps him (resting, data kept)");
        helper.assertTrue(!Karakters.wissel(p, 2, true), "a resting knight can't be played");
        String oudeSleutel = Karakter.info(p, 2).getStringOr("opslag", "");
        helper.assertTrue(Karakters.maak(p, 2, "Held Twee"), "the right slot makes a new Eénvads knight");
        CompoundTag nieuw = Karakter.info(p, 2);
        helper.assertTrue(nieuw.getStringOr("naam", "").equals("Held Twee") && !nieuw.getBooleanOr("pensioen", false) && nieuw.getBooleanOr("nieuw", false)
                && !nieuw.getStringOr("opslag", "").equals(oudeSleutel), "a fresh knight with his own knapzak key");
        var archief = Karakter.lijst(p).getListOrEmpty("archief");
        helper.assertTrue(archief.size() == 1 && archief.getCompoundOrEmpty(0).getStringOr("naam", "").equals("Held")
                && archief.getCompoundOrEmpty(0).contains("data"), "the old one is in the archive, with his data");
        helper.assertTrue(Karakters.wissel(p, 2, true) && Karakter.modus(p) == Modus.EENVADS && Spelers.data(p).munten == 0, "the new one plays fresh");
        // no other knight at all: retiring makes a fresh Normaal knight on the left
        ServerPlayer q = GuhMockPlayer.of(helper);
        Spelers.data(q).extra.remove(Karakter.LIJST);
        CompoundTag lq = Karakter.lijst(q);
        lq.remove("s0");
        CompoundTag s2 = new CompoundTag();
        s2.putString("naam", "Eenling");
        s2.putString("modus", "eenvads");
        lq.put("s2", s2);
        lq.putInt("actief", 2);
        helper.assertTrue(Karakters.pensioen(q, "gametest") && Karakters.naRust(q), "retire the only knight");
        helper.assertTrue(Karakter.slot(q) == 0 && Karakter.naam(q).equals(q.getGameProfile().name()) && Karakter.modus(q) == Modus.NORMAAL,
                "a new Normaal knight named after the player");
        hal.ridders.removeIf(r -> r.speler().equals(p.getUUID()) || r.speler().equals(q.getUUID()));
        hal.setDirty();
        helper.succeed();
    }

    /** U-KARAKTER: a T-KARAKTER (v1) list is re-ordered by mode, on hand-made lists and on a live player. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void migratieV1(GameTestHelper helper) {
        for (String r : KarakterTest.herschik()) {
            helper.assertTrue(r.startsWith("OK"), r);
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.munten = 321;
        CompoundTag v1 = new CompoundTag();
        v1.putInt("actief", 1);
        v1.putInt("versie", 1);
        CompoundTag s0 = new CompoundTag();
        s0.putString("naam", "Oud");
        s0.putString("modus", "knuffel");
        s0.put("data", new CompoundTag());
        CompoundTag s1 = new CompoundTag();
        s1.putString("naam", "Actief");
        s1.putString("modus", "eenvads");
        v1.put("s0", s0);
        v1.put("s1", s1);
        d.extra.put(Karakter.LIJST, v1);
        CompoundTag l = Karakter.lijst(p);
        helper.assertTrue(l.getIntOr("versie", 0) == Karakter.VERSIE, "migrated to v2");
        helper.assertTrue(Karakter.slot(p) == 2 && Karakter.naam(p).equals("Actief") && Karakter.modus(p) == Modus.EENVADS, "the active Eénvads knight is right now");
        helper.assertTrue(Karakter.opslagSleutel(p).equals("2"), "and keeps his knapzak (key 2 = old slot 1)");
        helper.assertTrue(Karakter.info(p, 1).getStringOr("naam", "").equals("Oud") && !Karakter.bestaat(p, 0), "the Knuffel knight is in the middle, left is free");
        helper.assertTrue(d.munten == 321, "the live data is untouched");
        helper.succeed();
    }

    /**
     * U-KARAKTER, "the first time you could not create a character": a brand-new player (no list) gets the left Normaal knight
     * automatically, the screen offers the two empty slots with their fixed modes, switching is allowed before the welcome
     * (anywhere), and "Maken en spelen" through the screen's own action makes + switches in one go.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eersteKeer(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).extra.remove(Karakter.LIJST);
        CompoundTag d = KarakterScherm.data(p);
        var slots = d.getListOrEmpty("slots");
        helper.assertTrue(slots.size() == 3, "three cards");
        helper.assertTrue(slots.getCompoundOrEmpty(0).getStringOr("modus", "").equals("normaal") && !slots.getCompoundOrEmpty(0).getBooleanOr("leeg", false),
                "left: your own knight, Normaal");
        helper.assertTrue(slots.getCompoundOrEmpty(1).getStringOr("modus", "").equals("knuffel") && slots.getCompoundOrEmpty(1).getBooleanOr("leeg", false)
                && slots.getCompoundOrEmpty(2).getStringOr("modus", "").equals("eenvads") && slots.getCompoundOrEmpty(2).getBooleanOr("leeg", false),
                "middle Knuffelmodus + right Eénvads: empty, ready to make");
        helper.assertTrue(d.getBooleanOr("mag", false), "the first choice is allowed (" + d.getStringOr("waarom", "") + ")");
        CompoundTag a = new CompoundTag();
        a.putInt("slot", 1);
        a.putString("naam", "  Pluis  ");
        a.putBoolean("spelen", true);
        KarakterScherm.doe(p, "maak", a);
        helper.assertTrue(Karakter.slot(p) == 1 && Karakter.naam(p).equals("Pluis") && Karakter.modus(p) == Modus.KNUFFEL,
                "Maken en spelen: made + switched (slot " + Karakter.slot(p) + ")");
        // the welcome is remembered (account-wide) and after it the normal place rule counts again (outside the Vadsrijk: free)
        KarakterRegistry.welkom(p);
        helper.assertTrue(Karakter.lijst(p).getBooleanOr("welkom", false), "welcome shown once");
        helper.assertTrue(Karakters.waaromNiet(p) == null, "outside the Vadsrijk you may always switch");
        // the camp you arrive in counts as a town, and no boss arena covers the arrival spot
        var g = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.op(nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.AANKOMST);
        helper.assertTrue(g != null && g.stad(), "the arrival is in a town area (" + (g == null ? "-" : g.id()) + ")");
        var b = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.AANKOMST;
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas.op(b.getX() + 0.5, b.getY(), b.getZ() + 0.5) == null, "no arena at the arrival");
        helper.assertTrue(Karakters.wissel(p, 0, true), "back");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void namen(GameTestHelper helper) {
        helper.assertTrue("Sir Pluis".equals(Karakters.schoon("  Sir   Pluis ")), "spaces cleaned");
        helper.assertTrue(Karakters.schoon("<>!!") == null, "nothing usable = no name");
        helper.assertTrue(Karakters.schoon("Abcdefghijklmnopqrstuvwxyz").length() == Karakters.NAAM_MAX, "max length");
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(!Karakters.maak(p, 1, "   "), "no empty name");
        helper.assertTrue(Karakters.maak(p, 1, "Pluis") && Karakters.hernoem(p, 1, "Pluisje") && Karakter.info(p, 1).getStringOr("naam", "")
                .equals("Pluisje"), "rename");
        helper.assertTrue(Karakters.vrijSlot(p) == 2, "the next free slot");
        helper.succeed();
    }
}
