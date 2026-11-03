package nl.juiced.guhs.feature.erfenis;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * De Erfenis ("Een opknapper!"): the economy maths (offline production, baskets, caps, the staff penalty, workshops using
 * their inputs, vadskracht, residents, builders, upgrade gating and Hoeve goals, clearing, kavels, the Leverkist caps),
 * saving, the plot geometry, and stamping one building.
 */
public class ErfenisGameTests {
    private static final String BATCH = "erfenis";
    private static final long T0 = 1_000_000_000L, UUR = 3_600_000L, MIN = 60_000L;

    private static Base start() {
        return Base.nieuw(UUID.randomUUID(), "Testguh", 0, 0, T0);
    }

    /** A base with all land open and lots of money and room (for the maths tests). */
    private static Base ruim() {
        Base b = start();
        b.allesOpen();
        b.hoeve().level = 10;
        b.guhlden = 1_000_000;
        b.goederen.clear();
        return b;
    }

    private static boolean ongeveer(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
    }

    /** Our 10 levels on balans' 5: L1 = 70%, even = balans, odd = halfway; costs L1 = half. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tienLevels(GameTestHelper helper) {
        Recept ei = Productie.recepten(GebouwType.KIPPENHOK).get(0);
        helper.assertTrue(ongeveer(Productie.perUur(GebouwType.KIPPENHOK, ei, 1), 14), "kippenhok L1 = 70% of 20");
        helper.assertTrue(ongeveer(Productie.perUur(GebouwType.KIPPENHOK, ei, 2), 20), "kippenhok L2 = balans L1");
        helper.assertTrue(ongeveer(Productie.perUur(GebouwType.KIPPENHOK, ei, 3), 25), "kippenhok L3 = halfway 20..30");
        helper.assertTrue(ongeveer(Productie.perUur(GebouwType.KIPPENHOK, ei, 10), 90), "kippenhok L10 = balans L5");
        Recept wol = Productie.recepten(GebouwType.MAMMOETWEI).get(0);
        helper.assertTrue(ongeveer(Productie.perUur(GebouwType.MAMMOETWEI, wol, 3), 12), "mammoetwei L3 = 12 (3 levels)");
        helper.assertTrue(Prijzen.gebouw(GebouwType.KIPPENHOK, 1).guhlden() == 15 && Prijzen.gebouw(GebouwType.KIPPENHOK, 2).guhlden() == 30,
                "class A: L1 half of 30, L2 30");
        helper.assertTrue(Prijzen.gebouw(GebouwType.KIPPENHOK, 10).guhlden() == 4000, "class A L10 = 4000");
        helper.assertTrue(Prijzen.gebouw(GebouwType.TIMMERSCHUUR, 7).hoofdwereld().containsKey("guhs:vahoege_vads_ingot"), "3rd builder asks vads ingots");
        helper.assertTrue(Prijzen.gebouw(GebouwType.KOEIENSTAL, 2).goederen().get(Goed.PLANK) == 30, "large building: x1.5 goods");
        helper.assertTrue(Prijzen.gebouw(GebouwType.LOKET, 1).tijdMs() == 20 * MIN, "loket L1 20 min");
        for (GebouwType t : GebouwType.values()) {
            for (int l = t == GebouwType.HOEVE ? 2 : 1; l <= t.topLevel(); l++) {
                long tijd = Prijzen.gebouw(t, l).tijdMs();
                helper.assertTrue(tijd > 0 && tijd <= 4 * UUR, t.id + " L" + l + ": timer between 1 min and 4 hours");
            }
        }
        helper.assertTrue(Productie.vadsmastCap(1) == 40 && Productie.vadsmastCap(10) == 140, "vadsmast 40 -> 140");
        helper.assertTrue(Productie.bonusProcent(4) == 5 && Productie.bonusProcent(5) == 10 && Productie.bonusProcent(9) == 15, "café 5/10/15");
        helper.assertTrue(Productie.bouwers(0) == 1 && Productie.bouwers(1) == 2 && Productie.bouwers(6) == 2 && Productie.bouwers(7) == 3,
                "builders 1 / 2 (L1) / 3 (L7)");
        helper.succeed();
    }

    /** Offline production: real time, buffer of 6 hours (8 from L7), byproducts, and the staff penalty. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void offlineProductie(GameTestHelper helper) {
        Base b = ruim();
        Gebouw hok = b.nieuwGebouw(GebouwType.KIPPENHOK, 2, 53, 41, 0, T0);
        b.bewoners = 1;
        b.bijgewerkt = T0;
        b.bijwerken(T0 + UUR);
        helper.assertTrue(ongeveer(hok.inBuffer(Goed.EI), 20) && ongeveer(hok.inBuffer(Goed.VEER), 10), "1 hour: 20 eggs + 10 feathers, got "
                + hok.inBuffer(Goed.EI));
        b.bijwerken(T0 + 10 * UUR);
        helper.assertTrue(ongeveer(hok.inBuffer(Goed.EI), 6 * 20), "full after 6 hours (L2): 120 eggs, got " + hok.inBuffer(Goed.EI));
        helper.assertTrue(hok.vol(), "the basket is full");
        hok.level = 7;
        hok.buffer.clear();
        b.bijwerken(T0 + 30 * UUR);
        helper.assertTrue(ongeveer(hok.inBuffer(Goed.EI), 8 * Productie.perUur(GebouwType.KIPPENHOK, Productie.recept(hok), 7)), "L7: 8 hours");
        // nobody working: one empty workplace = -25%
        Base c = ruim();
        Gebouw h2 = c.nieuwGebouw(GebouwType.KIPPENHOK, 2, 53, 41, 0, T0);
        c.bewoners = 0;
        c.bijgewerkt = T0;
        c.bijwerken(T0 + UUR);
        helper.assertTrue(ongeveer(h2.inBuffer(Goed.EI), 15), "no staff: 75%, got " + h2.inBuffer(Goed.EI));
        helper.assertTrue(ongeveer(Productie.werkFactor(3, 0), 0.25) && ongeveer(Productie.werkFactor(3, 1), 0.5)
                && ongeveer(Productie.werkFactor(5, 0), 0.25), "-25% per empty workplace, never under 25%");
        helper.succeed();
    }

    /** A workshop takes its inputs from the storage while it works, and waits when they run out. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void verwerkingGebruiktOpslag(GameTestHelper helper) {
        Base b = ruim();
        Gebouw molen = b.nieuwGebouw(GebouwType.MOLEN, 2, 53, 41, 0, T0);
        b.bewoners = 1;
        b.geefGoed(Goed.GRAAN, 100);
        b.bijgewerkt = T0;
        b.bijwerken(T0 + UUR);
        helper.assertTrue(ongeveer(molen.inBuffer(Goed.MEEL), 20) && ongeveer(b.goed(Goed.GRAAN), 60), "1 hour at L2 (= balans L1): 20 meel from 40 graan");
        b.bijwerken(T0 + 3 * UUR);
        helper.assertTrue(ongeveer(molen.inBuffer(Goed.MEEL), 50) && b.goed(Goed.GRAAN) < 1e-6, "graan gone: 50 meel, then it waits");
        // a choice the level doesn't allow yet falls back to the first recipe
        Gebouw kaas = b.nieuwGebouw(GebouwType.KAASMAKERIJ, 2, 53, 51, 0, T0);
        helper.assertTrue(b.kies(kaas.id, 1) != null, "kaasknabbels only from L3");
        kaas.level = 3;
        helper.assertTrue(b.kies(kaas.id, 1) == null && Productie.recept(kaas).uit() == Goed.KAASKNABBEL, "L3: kaasknabbels");
        helper.succeed();
    }

    /** Collecting stops at the storage room (the rest stays inside); Guhlden stop at the Hoeve's cap. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslagEnCaps(GameTestHelper helper) {
        Base b = start();
        b.goederen.clear();
        Gebouw hok = b.nieuwGebouw(GebouwType.KIPPENHOK, 1, 53, 41, 0, T0);
        hok.buffer.put(Goed.EI, 500.0);
        helper.assertTrue(b.opslag() == Productie.HOEVE_OPSLAG, "start: only the Hoeve's room");
        Object r = b.ophalen(hok.id, new Random(1));
        helper.assertTrue(r instanceof Map<?, ?> m && ((Integer) m.get(Goed.EI)) == Productie.HOEVE_OPSLAG, "only what fits");
        helper.assertTrue(ongeveer(hok.inBuffer(Goed.EI), 500 - Productie.HOEVE_OPSLAG), "the rest stays in the basket");
        helper.assertTrue("erfenis.fout.vol".equals(b.kanOphalen(hok.id).key()), "full storage");
        b.nieuwGebouw(GebouwType.OPSLAGSCHUUR, 2, 41, 53, 0, T0);
        helper.assertTrue(b.opslag() == Productie.HOEVE_OPSLAG + Productie.opslagschuur(2), "an Opslagschuur adds room");
        b.guhlden = 0;
        helper.assertTrue(b.geefGuhlden(10_000) == 500 && b.guhlden == 500, "H1 holds 500 Guhlden");
        helper.succeed();
    }

    /** Max level = Hoeve level; counts and locks per Hoeve level; one builder; the Hoeve's goals. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upgradeRegels(GameTestHelper helper) {
        Base b = start();
        b.guhlden = 500;
        helper.assertTrue("erfenis.fout.slot".equals(b.kanKopen(GebouwType.KONIJNENREN).key()), "konijnenren waits for Hoeve 3");
        helper.assertTrue(b.kanKopen(GebouwType.KIPPENHOK) == null && b.prijsNieuw(GebouwType.KIPPENHOK).guhlden() == 0, "first kippenhok free");
        helper.assertTrue(b.plaats(GebouwType.KIPPENHOK, 53, 41, 0, T0, 0) == null, "placed in the open middle");
        helper.assertTrue("erfenis.fout.slot".equals(b.kanKopen(GebouwType.KIPPENHOK).key()), "a 2nd kippenhok waits for Hoeve 4");
        b.bijwerken(T0 + 1);
        helper.assertTrue("erfenis.fout.overwoekerd".equals(b.kanPlaatsen(GebouwType.GUHHUTJE, 5, 5, 0).key()), "not on overgrown land");
        Gebouw hok = b.op(53, 41);
        helper.assertTrue(hok != null && hok.level == 1, "built");
        helper.assertTrue("erfenis.fout.hoeve_level".equals(b.kanVerbeteren(hok.id).key()), "L2 needs Hoeve 2");
        // the Hoeve's goals
        Gebouw h = b.hoeve();
        b.goederen.put(Goed.PLANK, 100.0);
        b.goederen.put(Goed.EI, 50.0);
        helper.assertTrue("erfenis.fout.doel".equals(b.kanVerbeteren(h.id).key()), "goals first (1 resident, 1 order, 1 plukje)");
        b.bewoners = 1;
        b.bestellingen = 1;
        b.opgeruimd[0] = true;
        helper.assertTrue(b.kanVerbeteren(h.id) == null, "goals reached: the Hoeve may go up");
        helper.assertTrue(b.verbeter(h.id, T0 + 10, -1) == null, "Hoeve 2 starts");
        helper.assertTrue("erfenis.fout.bouwer".equals(b.kanKopen(GebouwType.AKKER).key()), "one builder: busy");
        b.bijwerken(T0 + 10 + Prijzen.hoeve(2).tijdMs());
        helper.assertTrue(b.hoeveLevel() == 2 && b.kanVerbeteren(hok.id) == null, "Hoeve 2: kippenhok L2 allowed");
        // Hoeve 7 asks for the Loket
        helper.assertTrue(Prijzen.doelen(7).stream().anyMatch(d -> d.soort().equals("loket")), "Hoeve 7 needs a Loket");
        helper.succeed();
    }

    /** Builders: a 2nd from Timmerschuur L1 (two jobs at once). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bouwers(GameTestHelper helper) {
        Base b = ruim();
        b.goederen.put(Goed.PLANK, 5000.0);
        helper.assertTrue(b.bouwers() == 1, "one builder");
        helper.assertTrue(b.plaats(GebouwType.AKKER, 53, 41, 0, T0, -1) == null, "job 1");
        helper.assertTrue("erfenis.fout.bouwer".equals(b.kanKopen(GebouwType.BOSJE).key()), "no second job");
        b.nieuwGebouw(GebouwType.TIMMERSCHUUR, 1, 41, 56, 0, T0);
        helper.assertTrue(b.bouwers() == 2 && b.vrijeBouwers() == 1, "Timmerschuur: 2 builders");
        helper.assertTrue(b.plaats(GebouwType.BOSJE, 70, 20, 0, T0, -1) == null, "job 2 with the second builder");
        helper.assertTrue("erfenis.fout.bouwer".equals(b.kanKopen(GebouwType.GUHHUTJE).key()), "no third job");
        helper.succeed();
    }

    /** Residents move in every 15 minutes while there is room (7.5 with a Speeltuin). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bewonersTrekkenIn(GameTestHelper helper) {
        Base b = ruim();
        b.nieuwGebouw(GebouwType.GUHHUTJE, 1, 53, 41, 0, T0);
        b.bijgewerkt = T0;
        b.bijwerken(T0);
        helper.assertTrue(b.maxBewoners() == 2 && b.bewoners == 0 && b.volgendeIntrek == T0 + 15 * MIN, "room for 2, first in 15 min");
        b.bijwerken(T0 + 16 * MIN);
        helper.assertTrue(b.bewoners == 1, "one after 15 min");
        b.bijwerken(T0 + 5 * UUR);
        helper.assertTrue(b.bewoners == 2 && b.volgendeIntrek == 0, "full: 2");
        helper.assertTrue(b.momenten.contains("bewoners:2"), "moment bewoners:2");
        b.nieuwGebouw(GebouwType.SPEELTUIN, 1, 60, 41, 0, T0);
        b.bijwerken(T0 + 5 * UUR + 1);
        helper.assertTrue(b.volgendeIntrek == T0 + 5 * UUR + 7 * MIN + 30_000, "a Speeltuin: 7.5 min, got " + (b.volgendeIntrek - T0));
        helper.succeed();
    }

    /** Vadskracht: workshops at L6 speed without it, the Mammoetwei sleeps; with enough VK at the Vadsmast: full speed. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vadskracht(GameTestHelper helper) {
        Base b = ruim();
        Gebouw glas = b.nieuwGebouw(GebouwType.GLASBLAZERIJ, 8, 53, 41, 0, T0);
        Gebouw mam = b.nieuwGebouw(GebouwType.MAMMOETWEI, 2, 20, 20, 0, T0);
        b.bewoners = 100;
        b.geefGoed(Goed.ZAND, 140);
        Recept r = Productie.recept(glas);
        helper.assertTrue(ongeveer(Productie.nuPerUur(b, glas), Productie.perUur(GebouwType.GLASBLAZERIJ, r, 6)), "no VK: L6 speed");
        helper.assertTrue(Productie.nuPerUur(b, mam) == 0, "no VK: the mammoths sleep");
        b.vk = 500;
        helper.assertTrue(b.vkInDorp() == 0, "no Vadsmast: nothing arrives");
        b.nieuwGebouw(GebouwType.VADSMAST, 1, 70, 41, 0, T0);
        helper.assertTrue(b.vkInDorp() == 40, "Vadsmast L1 takes 40");
        helper.assertTrue(ongeveer(Productie.nuPerUur(b, glas), Productie.perUur(GebouwType.GLASBLAZERIJ, r, 8)), "8 VK: full speed");
        helper.assertTrue(Productie.nuPerUur(b, mam) > 0, "glas 8 + mammoet 20 = 28 <= 40: the mammoths work");
        helper.succeed();
    }

    /** Clearing: ring gating, a builder, rewards and the moments; kavels only after all 72, priced n x 600. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opruimenEnKavels(GameTestHelper helper) {
        Base b = start();
        b.guhlden = 500;
        int ring1 = -1, ring2 = -1;
        for (int i = 0; i < Plot.PLUKJES; i++) {
            if (Plot.ring(i) == 1 && ring1 < 0) {
                ring1 = i;
            }
            if (Plot.ring(i) == 2 && ring2 < 0) {
                ring2 = i;
            }
        }
        helper.assertTrue("erfenis.fout.slot".equals(b.kanOpruimen(ring2).key()), "ring 2 waits for Hoeve 3");
        helper.assertTrue(b.opruimen(ring1, T0, -1) == null && b.guhlden == 475, "ring 1 costs 25");
        int voor = b.opslagGebruikt();
        b.bijwerken(T0 + 2 * MIN);
        helper.assertTrue(b.opgeruimd[ring1] && b.opslagGebruikt() >= voor + 10, "cleared: 10-20 wood or stone");
        helper.assertTrue(b.momenten.contains("plukje_opgeruimd:1"), "moment plukje_opgeruimd:1");
        int[] hoek = Plot.plukjeHoek(ring1);
        helper.assertTrue(b.open(hoek[0], hoek[1]), "the land is open now");
        helper.assertTrue("erfenis.fout.eerst_opruimen".equals(b.kanKavel(0).key()), "kavels after all plukjes");
        b.allesOpen();
        b.kavels = 0;
        b.hoeve().level = 6;
        b.guhlden = 5000;
        b.goederen.put(Goed.PLANK, 100.0);
        helper.assertTrue(b.kavelKopen(3) == null && b.guhlden == 4400, "kavel 1: 600");
        helper.assertTrue(b.kavelKopen(5) == null && b.guhlden == 3200, "kavel 2: 1200");
        helper.assertTrue("erfenis.fout.slot".equals(b.kanKavel(7).key()), "H6: 2 kavels");
        // the geometry: 72 plukjes (16/24/32), 16 kavels, every tile of the plot belongs to exactly one part
        int[] ringen = new int[4];
        for (int i = 0; i < Plot.PLUKJES; i++) {
            ringen[Plot.ring(i)]++;
            int[] c = Plot.plukjeHoek(i);
            helper.assertTrue(Plot.plukjeOp(c[0] + 3, c[1] + 5) == i, "plukje " + i + " round trip");
        }
        helper.assertTrue(ringen[1] == 16 && ringen[2] == 24 && ringen[3] == 32, "rings 16/24/32");
        for (int x = 0; x < Plot.MAAT; x++) {
            for (int z = 0; z < Plot.MAAT; z++) {
                int delen = (Plot.inMidden(x, z) ? 1 : 0) + (Plot.plukjeOp(x, z) >= 0 ? 1 : 0) + (Plot.kavelOp(x, z) >= 0 ? 1 : 0);
                helper.assertTrue(delen == 1, "tile " + x + "," + z + " in " + delen + " parts");
            }
        }
        for (int k = 0; k < Plot.KAVELS; k++) {
            int[] v = Plot.kavelVak(k);
            helper.assertTrue(Plot.kavelOp(v[0] + v[2] - 1, v[1] + v[3] - 1) == k, "kavel " + k + " round trip");
        }
        helper.succeed();
    }

    /** The Leverkist: per material a cap per hour that saves up to 8 hours; wool exempt; village-only goods stay. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void leverkistCaps(GameTestHelper helper) {
        Base b = ruim();
        b.nieuwGebouw(GebouwType.OPSLAGSCHUUR, 10, 41, 56, 0, T0);
        b.nieuwGebouw(GebouwType.OPSLAGSCHUUR, 10, 53, 41, 0, T0);
        b.geefGoed(Goed.STAM, 3000);
        b.geefGoed(Goed.WOL, 3000);
        b.geefGoed(Goed.PLANK, 100);
        helper.assertTrue(b.magNaarHuis(Goed.STAM, T0) == 8 * 150, "8 hours of 150 logs saved up");
        helper.assertTrue(b.naarHuis(Goed.STAM, 1200, T0) instanceof Map<?, ?>, "1200 logs go home");
        helper.assertTrue(b.kanNaarHuis(Goed.STAM, 1, T0) != null, "then the cap is used up");
        helper.assertTrue(b.magNaarHuis(Goed.STAM, T0 + UUR) == 150, "an hour later: 150 more");
        helper.assertTrue(b.magNaarHuis(Goed.WOL, T0) == b.heeft(Goed.WOL), "wool: no cap");
        helper.assertTrue(!Goed.PLANK.naarHuis() && "erfenis.fout.niet_naar_huis".equals(b.kanNaarHuis(Goed.PLANK, 1, T0).key()),
                "planks stay in the village (half-products)");
        Object r = b.naarHuis(Goed.WOL, 70, T0);
        helper.assertTrue(r instanceof Map<?, ?> m && Integer.valueOf(70).equals(m.get("minecraft:white_wool")), "white wool goes home finished");
        helper.succeed();
    }

    /** Saving and loading keep everything (also buffers, choices, land, paths, the Loket, logés). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslaanEnLaden(GameTestHelper helper) {
        Base b = ruim();
        Gebouw a = b.nieuwGebouw(GebouwType.AKKER, 3, 53, 41, 1, T0);
        a.keuze = 2;
        a.buffer.put(Goed.WORTEL, 12.5);
        a.palet = 3;
        a.paletGekozen = true;
        Gebouw w = b.nieuwGebouw(GebouwType.BOUWBLOKKENWERKPLAATS, 2, 41, 56, 0, T0);
        w.selectie.add("guhs:pastelbaksteen_aqua");
        b.paden[5 * Plot.MAAT + 7] = 1;
        b.padSoorten.add("zandpaadje");
        b.loket.put("guhs:guh_kristal", 5);
        b.vondsten.add("roestig_sleuteltje");
        b.opgeruimd[10] = false;
        b.opruimen(10, T0, -1);
        b.kavels = 0b101;
        b.bestellingen = 7;
        Base t = Base.load(b.save());
        Gebouw ta = t.gebouw(a.id);
        helper.assertTrue(ta != null && ta.keuze == 2 && ongeveer(ta.inBuffer(Goed.WORTEL), 12.5) && ta.palet == 3 && ta.paletGekozen && ta.rot == 1,
                "building kept");
        helper.assertTrue(t.gebouw(w.id).selectie.contains("guhs:pastelbaksteen_aqua"), "Bouwkeet choice kept");
        helper.assertTrue(t.paden[5 * Plot.MAAT + 7] == 1 && t.padSoorten.equals(b.padSoorten), "paths kept");
        helper.assertTrue(t.loket.equals(b.loket) && t.vondsten.equals(b.vondsten) && t.kavels == b.kavels && t.bestellingen == 7, "rest kept");
        helper.assertTrue(t.bouwen.size() == 1 && t.bouwen.get(0).soort() == Base.Bouw.PLUKJE, "the clearing job kept");
        helper.assertTrue(java.util.Arrays.equals(t.opgeruimd, b.opgeruimd) && t.guhlden == b.guhlden, "land and money kept");
        Set<Long> gezien = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            int[] c = Plot.spiraal(i);
            helper.assertTrue(gezien.add(Plot.sleutel(c[0], c[1])), "spiral cell " + i + " given twice");
        }
        helper.succeed();
    }

    /** The Hoeve's moments; Verven within the unlocked palettes; NPC villages keep to their limits. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void momentenEnVerven(GameTestHelper helper) {
        Base b = ruim();
        b.hoeve().level = 3;
        b.bewoners = 99;
        b.bestellingen = 99;
        b.goederen.put(Goed.PLANK, 200.0);
        b.goederen.put(Goed.BAKSTEEN, 100.0);
        b.goederen.put(Goed.MEEL, 50.0);
        b.goederen.put(Goed.STOF, 50.0);
        b.nieuwGebouw(GebouwType.KIPPENHOK, 1, 53, 41, 0, T0);
        b.nieuwGebouw(GebouwType.SCHAAPSKOOI, 1, 20, 20, 0, T0);
        b.nieuwGebouw(GebouwType.KOEIENSTAL, 1, 70, 20, 0, T0);
        helper.assertTrue(b.verbeter(b.hoeve().id, T0, 0) == null, "Hoeve 4 starts");
        b.bijwerken(T0 + 1);
        helper.assertTrue(b.momenten.contains("hoeve_level:4") && b.momenten.contains("gebouw_level:hoeve:4"), "moments fired");
        helper.assertTrue(b.maxPalet() == 2, "Hoeve 4: palettes T1-T2");
        helper.assertTrue(b.verf(-1, 2) == null && b.gebouwen.stream().allMatch(g -> g.palet == 2), "the whole village in T2");
        helper.assertTrue("erfenis.fout.palet".equals(b.kanVerven(b.hoeve().id, 3).key()), "T3 not yet");
        for (int h = 1; h <= GebouwType.MAX_HOEVE; h++) {
            Base npc = Base.npc(UUID.randomUUID(), "Knabbeldorp", 3, 4, h, 42L + h, T0);
            helper.assertTrue(npc.hoeveLevel() == h, "npc hoeve level");
            for (GebouwType t : GebouwType.values()) {
                helper.assertTrue(npc.aantal(t) <= Math.max(1, t.maxAantal(h)), "npc " + t.id + " over its limit at " + h);
            }
            for (Gebouw g : npc.gebouwen) {
                helper.assertTrue(npc.vrij(g.x, g.z, g.b(), g.d(), g.id), "npc building overlaps or sticks out");
            }
        }
        helper.succeed();
    }

    /** The Bouwkeet sets (from the registry) and a workshop's selection. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bouwkeetSets(GameTestHelper helper) {
        List<List<String>> sets = Bouwkeet.sets();
        helper.assertTrue(sets.size() == 5 && sets.get(0).size() >= 16 && sets.get(0).contains(Bouwkeet.STANDAARD), "set 1: pastel bricks + wood, got "
                + Bouwkeet.grootte());
        for (List<String> s : sets) {
            helper.assertTrue(!s.isEmpty(), "every set has blocks: " + Bouwkeet.grootte());
        }
        Base b = ruim();
        Gebouw w = b.nieuwGebouw(GebouwType.BOUWBLOKKENWERKPLAATS, 1, 53, 41, 0, T0);
        helper.assertTrue(b.selectie(w.id, List.of(sets.get(1).get(0))) != null, "L1: no plaster yet");
        helper.assertTrue(b.selectie(w.id, List.of(sets.get(0).get(0), sets.get(0).get(1))) == null && w.selectie.size() == 2, "a mix of two");
        w.buffer.put(Goed.BOUWBLOK, 10.0);
        b.ophalen(w.id, new Random(3));
        int som = b.bouwblokSoorten.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(som == 10 && b.bouwblokSoorten.keySet().stream().allMatch(w.selectie::contains), "10 blocks of the chosen kinds");
        helper.succeed();
    }

    /** Stamping one building puts blocks in its footprint (the stand-in shapes until the vormen merge). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void stempelGebouw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Base b = ruim();
        Gebouw m = b.nieuwGebouw(GebouwType.VADSMAST, 1, 1, 1, 0, T0);
        BlockPos o = helper.absolutePos(new BlockPos(0, 0, 0)).offset(-1, 0, -1);
        Stempel.gebouw(level, o, b, m);
        boolean iets = false;
        for (int x = 0; x < m.b(); x++) {
            for (int z = 0; z < m.d(); z++) {
                iets |= !level.getBlockState(o.offset(m.x + x, 1, m.z + z)).isAir();
            }
        }
        helper.assertTrue(iets, "the vadsmast's footprint has blocks");
        helper.succeed();
    }

    /**
     * Integration 1.9: every deco id another slice hands out (orders, the star, Burenhulp, Sherlock, finds) is a vormen deco
     * shape; a piece from the decoschuurtje can be placed (Knusheid goes up), not twice on one tile, and a building placed over
     * it sends it back into the schuurtje.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void integratieDeco(GameTestHelper helper) {
        Set<String> vormen = new HashSet<>(nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen.decoIds());
        List<String> ids = new java.util.ArrayList<>(List.of("mika_schrik", "knabbelbessenstruik", "de_eerste_knabbel", "kaassausfonteintje",
                "mammoetje_van_heg", "weerhaan", "strooguh", "melkkar", "knabbeltrommel", "trekguh", "kaartjesbord"));
        ids.addAll(List.of(nl.juiced.guhs.feature.erfenis.kaart.Handje.BURENSTUKKEN));
        ids.addAll(nl.juiced.guhs.feature.erfenis.sherlock.Zaken.DECO);
        for (String id : ids) {
            helper.assertTrue(vormen.contains(ErfenisDeco.vorm(id)), "deco " + id + " has a vormen shape");
        }
        Base b = ruim();
        b.decoOpslag.put("hartjesnest", 1);
        int knus = b.knusheid();
        helper.assertTrue(b.plaatsDeco("hartjesnest", 30, 30, 0) == null && b.decoOpslag.isEmpty() && b.knusheid() == knus + 10,
                "placed: out of the schuurtje, +10 Knusheid");
        b.decoOpslag.put("uilenhuisje", 1);
        helper.assertTrue(b.plaatsDeco("uilenhuisje", 31, 31, 0) != null, "not on top of another piece");
        helper.assertTrue(b.plaats(GebouwType.KIPPENHOK, 28, 28, 0, T0, 0) == null && b.deco.isEmpty()
                && b.decoOpslag.getOrDefault("hartjesnest", 0) == 1, "a building over it: the piece goes back into the schuurtje");
        helper.succeed();
    }
}
