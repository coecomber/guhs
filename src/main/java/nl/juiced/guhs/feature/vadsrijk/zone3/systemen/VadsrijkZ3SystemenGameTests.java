package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Juwelier;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 3 systemen: layered areas (kern Gebieden with a y-range / cylinder), cap 50 + the xp pacing, the 4th advancement, tree
 * rows 6/7, the bounty reset/swap/claim, zone-3 crystals + 3 sockets, Gouden Veren.
 * Run: {@code runGameTestServer -Pgt=VadsrijkZ3Systemen}.
 */
public class VadsrijkZ3SystemenGameTests {
    private static final String BATCH = "vadsrijk_z3_systemen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void gelaagdeGebieden(GameTestHelper helper) {
        Z3Lagen.registreer();
        // same x/z, different heights
        int X0 = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X, Z0 = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z;   // (P-VERHUIS) zone 3 can move: everything relative to the trunk
        // (P-VERHUIS) the revamped tree (POI v2): crown 212+, Honingtak ring 145-200, hollow trunk r < 26, roots hall r < 160 with the
        // Hart's ring of rooms r 76-150 round it, the riding spiral r 26-64 outside the trunk
        verwacht(helper, X0 + 0, 250, Z0 + 0, "z3_kruinzee");
        verwacht(helper, X0 + 0, 150, Z0 + 0, "z3_schorsstad");
        verwacht(helper, X0 + 110, 29, Z0 + 10, "z3_hart");
        verwacht(helper, X0 + 0, 40, Z0 + 0, "z3_wortelduister");
        verwacht(helper, X0 + -50, 30, Z0 + 0, "z3_wortelduister");
        verwacht(helper, X0 + 200, 80, Z0 + 0, "z3_wortelvelden");
        verwacht(helper, X0 + 0, 170, Z0 + 100, "z3_honingtak");
        verwacht(helper, X0 + 150, 80, Z0 + 150, "z3_wortelvelden");
        verwacht(helper, X0 + 0, 205, Z0 + 190, "z3_takkenbanen");
        verwacht(helper, X0 + 251, 192, Z0 + -291, "z3_zeppelinwerf");
        verwacht(helper, X0 + 0, -10, Z0 + 0, "z3_ring");
        verwacht(helper, X0 + 45, 120, Z0 + 0, "z3_stamspiraal");
        // a flat lookup cannot know the layer: it skips the layered areas
        Gebieden.Gebied plat = Gebieden.op(X0, Z0);
        helper.assertTrue(plat != null && plat.id().equals("z3_ring"), "flat lookup in zone 3: " + (plat == null ? null : plat.id()));
        // zone 1/2 are unchanged (no layer, every height)
        helper.assertTrue(Gebieden.op(0, 0).id().equals("zwamzoet") && Gebieden.op(0, 300, 0).id().equals("zwamzoet")
                && Gebieden.op(0, -40, 0).id().equals("zwamzoet"), "zone 1 ignores y");
        helper.assertTrue(!Gebieden.van("zwamzoet").gelaagd(), "old areas have no layer");
        // a cylinder: a box corner is outside the circle
        Gebieden.Gebied c = Gebieden.Gebied.cilinder("t", 0, 0, 0, 10, 0, 10, 1, 1, false);
        helper.assertTrue(c.bevat(5, 5, 5) && !c.bevat(9.5, 5, 9.5) && !c.bevat(5, 11, 5) && c.bevat(new BlockPos(0, 10, 9)), "cylinder");
        Gebieden.Gebied ring = c.metRing(0, 0, 4, 10);
        helper.assertTrue(!ring.bevat(1, 5, 1) && ring.bevat(6, 5, 0), "ring");
        // Z3Lagen agrees with the runtime list
        helper.assertTrue(Z3Lagen.op(X0, 250, Z0).id().equals("z3_kruinzee") && Z3Lagen.op(0, 64, 0) == null, "Z3Lagen.op");
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, double x, double y, double z, String id) {
        Gebieden.Gebied g = Gebieden.op(x, y, z);
        helper.assertTrue(g != null && g.id().equals(id), "at " + x + ", " + y + ", " + z + ": " + (g == null ? null : g.id()) + " instead of " + id);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void levelCap50(GameTestHelper helper) {
        helper.assertTrue(Levels.MAX >= 50 && Levels.MAX_GENERIEK >= 50, "cap at least 50 (T-INT: 60), is " + Levels.MAX);
        for (int l = 1; l <= Levels.MAX; l++) {
            helper.assertTrue(Levels.levelVan(Levels.totaalVoor(l)) == l, "level " + l + " round trip");
            helper.assertTrue(Levels.generiekVan(Levels.totaalVoorGeneriek(l)) == l, "generic level " + l + " round trip");
            helper.assertTrue(l == Levels.MAX || Levels.xpVoor(l + 1) > Levels.xpVoor(l), "the curve rises at " + l);
        }
        helper.assertTrue(Levels.levelVan(Long.MAX_VALUE / 4) == Levels.MAX, "capped at " + Levels.MAX);   // (T-INT: 60 now)
        helper.assertTrue(Levels.skillpunten(50) - Levels.skillpunten(35) == 15, "lv 36-50 gives 15 points");
        // pacing: 30 -> 50 needs the plan's ~185k xp; a mob is ~1/55 of a level, a bounty 5 %
        long nodig = Levels.totaalVoor(50) - Levels.totaalVoor(30);
        helper.assertTrue(nodig > 150_000 && nodig < 230_000, "xp 30->50 " + nodig);
        helper.assertTrue(Z3Xp.mob(40) * 55 >= Levels.xpVoor(40) * 0.95 && Z3Xp.mob(40) * 55 <= Levels.xpVoor(40) * 1.05, "mob xp " + Z3Xp.mob(40));
        helper.assertTrue(Z3Xp.quest(45, true) > Z3Xp.quest(45, false) && Z3Xp.quest(45, false) > Z3Xp.premie(45), "quest > side > bounty");
        // ~20 levels from 15 main steps (45 %) + 6 side (18 %) + ~1100 kills (1/55) + 20 bounties: in range
        double lv = 15 * Z3Xp.HOOFDQUEST + 6 * Z3Xp.ZIJQUEST + 1100 * Z3Xp.MOB + 20 * Z3Xp.PREMIE;
        helper.assertTrue(lv >= 18 && lv <= 36, "rough levels from a playthrough " + lv);
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        Spelers.geefXp(p, Levels.totaalVoor(48), "test");
        helper.assertTrue(Spelers.level(p) == 48, "xp up to lv 48, got " + Spelers.level(p));
        for (Klasse k : Klassen.alle()) {
            helper.assertTrue(k.basis(50).get(Stat.VADS) > k.basis(35).get(Stat.VADS), k.id() + ": stats keep growing after 35");
        }
        helper.succeed();
    }

    /** U-SPECS: the old lv-50 step is gone; Schemerwijze (this slice's ult upgrade) is now the Sterrenwever's step 3 (lv 58). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void advancementStap4(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.MAGIER;
        Boom.knopen(d, Klassen.MAGIER).add(nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec.STERRENWEVER.knoop());
        for (int s = 1; s <= 2; s++) {
            Boom.knopen(d, Klassen.MAGIER).add("m_adv" + s);
        }
        Advancement.POORTEN = false;
        try {
            d.xp.put(Klassen.MAGIER, Levels.totaalVoor(57));
            helper.assertTrue(Advancement.stap(p, Klassen.MAGIER) == 2 && !Advancement.kanDoen(p, Klassen.MAGIER), "lv 57: not yet");
            helper.assertTrue(Advancement.nodigLevel(2) == 58 && Advancement.MAX_STAP == 3, "step 3 at 58");
            d.xp.put(Klassen.MAGIER, Levels.totaalVoor(58));
            helper.assertTrue(Advancement.kanDoen(p, Klassen.MAGIER), "lv 58: step 3 is open");
            double voor = Spelers.stats(p).get(Stat.WIJSHEID);
            helper.assertTrue(Advancement.doe(p, Klassen.MAGIER), "the ceremony");
            helper.assertTrue(Advancement.stap(p, Klassen.MAGIER) == 3 && Advancement4.heeft(p, Klassen.MAGIER), "step 3 done");
            helper.assertTrue(Advancement.titelKey(p).equals("vadsrijk.klassen.titel.sterrenwever.3"), "title Schemerwijze: " + Advancement.titelKey(p));
            helper.assertTrue(Spelers.stats(p).get(Stat.WIJSHEID) > voor, "the adv3 node gives its stats");
            helper.assertTrue(!Advancement.kanDoen(p, Klassen.MAGIER), "no 4th step");
            Boom.knopen(d, Klassen.MAGIER).add("m_wijs");
            helper.assertTrue(Boom.respec(d, Klassen.MAGIER, System.currentTimeMillis()) == null && Advancement.stap(p, Klassen.MAGIER) == 3,
                    "a respec keeps the advancement");
        } finally {
            Advancement.POORTEN = true;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void boomRij6(GameTestHelper helper) {
        for (Klasse k : Klassen.alle()) {
            int n = 0;
            for (var b : k.boom()) {
                if (b.rij() >= 6) {
                    n++;
                    helper.assertTrue(Boom.MIN_LEVEL.getOrDefault(b.id(), 0) >= 40, k.id() + ": zone-3 node without gate " + b.id());
                }
            }
            helper.assertTrue(n >= 4, k.id() + " has " + n + " zone-3 nodes");   // U-SPECS: + the spec keystones in row 7
        }
        // U-SPECS: Schorshuid is in the Tank branch (under IJzeren Knuffel), Drakenkussen ends the trunk (under Kristalhart)
        Klasse r = Klassen.van(Klassen.RIDDER);
        Set<String> heeft = new LinkedHashSet<>(List.of("r_pluisvacht", "r_doornknuffel", "r_beschermknuffel", "r_bolwerk", "r_onverzettelijk",
                "r_ijzeren_knuffel", "r_knuffelspier", "r_taai", "r_schokgolf", "r_kussenstorm", "r_ijspantser"));
        helper.assertTrue("level".equals(Boom.waaromNiet(r, heeft, 39, "r_schorshuid")), "row 6 is closed at lv 39");
        helper.assertTrue(Boom.waaromNiet(r, heeft, 40, "r_schorshuid") == null, "row 6 opens at lv 40 (after a row-5 node): "
                + Boom.waaromNiet(r, heeft, 40, "r_schorshuid"));
        helper.assertTrue("vereist".equals(Boom.waaromNiet(r, heeft, 50, "r_drakenkussen")), "the keystone needs the trunk node above");
        heeft.add("r_kristalhart");
        helper.assertTrue("level".equals(Boom.waaromNiet(r, heeft, 47, "r_drakenkussen")), "keystone closed at 47");
        helper.assertTrue(Boom.waaromNiet(r, heeft, 48, "r_drakenkussen") == null, "keystone at 48");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void premiesResetEnRuil(GameTestHelper helper) {
        var oud = Z3Data.KLOK;
        try {
            // the day turns at 06:00 Amsterdam
            ZonedDateTime voor = ZonedDateTime.of(2026, 11, 4, 5, 59, 0, 0, Z3Data.TIJDZONE);
            helper.assertTrue(Z3Data.resetDag(voor).toString().equals("2026-11-03") && Z3Data.resetDag(voor.plusMinutes(1)).toString().equals("2026-11-04"),
                    "reset at 06:00");
            Z3Data.KLOK = () -> ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, Z3Data.TIJDZONE);
            ServerPlayer p = GuhMockPlayer.of(helper);
            Spelers.zetLevel(p, 45);
            List<String> ids = Premies.ids(p);
            helper.assertTrue(ids.size() == 3 && new LinkedHashSet<>(ids).size() == 3, "3 different bounties: " + ids);
            helper.assertTrue(ids.stream().noneMatch(i -> Premies.POOL.get(i).event()), "no event bounties while no slice switched them on");
            helper.assertTrue(Premies.ids(p).equals(ids), "the same all day");
            // not finished: no claim
            helper.assertTrue(Premies.claim(p, 0).endsWith("nog_niet"), "not finished yet");
            // finish slot 0 (through the real goal path when it is a kill/ride/wind goal)
            Premies.Premie x = Premies.POOL.get(ids.get(0));
            Premies.voortgang(p, d -> d.equals(x.doel()), x.aantal());
            helper.assertTrue(Premies.klaar(p, 0), "slot 0 done");
            int pen = Z3Data.penningen(p);
            long munten = Spelers.data(p).munten;
            helper.assertTrue(Premies.claim(p, 0).endsWith("geclaimd"), "claimed");
            helper.assertTrue(Z3Data.penningen(p) == pen + (x.baas() ? Premies.PENNINGEN_BAAS : Premies.PENNINGEN), "Jachtpenningen paid");
            helper.assertTrue(Spelers.data(p).munten > munten, "coins paid");
            helper.assertTrue(Premies.claim(p, 0).endsWith(".al"), "no double claim");
            // the free swap: once a day, not on a claimed one
            helper.assertTrue(Premies.ruil(p, 0).endsWith("niet_ruilen"), "a claimed bounty can't be swapped");
            String twee = Premies.ids(p).get(1);
            helper.assertTrue(Premies.ruil(p, 1).endsWith("geruild") && !Premies.ids(p).get(1).equals(twee), "swapped");
            helper.assertTrue(Premies.ruil(p, 2).endsWith("al_geruild"), "only one swap a day");
            // the area goal: dood:z3_... matches by area id
            helper.assertTrue(Premies.gebiedPast("z3_schorsstad|z3_stamspiraal", "z3_stamspiraal") && !Premies.gebiedPast("z3_kruinzee", "z3_hart")
                    && Premies.gebiedPast("*", "z3_ring") && !Premies.gebiedPast("*", "zwamzoet"), "area matching");
            // the next day: fresh board, swap again
            Z3Data.KLOK = () -> ZonedDateTime.of(2026, 11, 5, 7, 0, 0, 0, Z3Data.TIJDZONE);
            CompoundTag t = Premies.vandaag(p);
            helper.assertTrue(!t.getBooleanOr("geruild", true) && !Premies.geclaimd(p, 0) && Premies.voortgang(p, 0) == 0, "new day, new board");
            helper.assertTrue(t.getIntOr("weekN", 0) == 1, "the week counter keeps going within the week");
            // under lv 40 nothing counts
            ServerPlayer laag = GuhMockPlayer.of(helper);
            Spelers.zetLevel(laag, 35);
            helper.assertTrue(Premies.claim(laag, 0).endsWith("te_laag"), "the board starts at 40");
            // contracts: 2 a day, the same for everyone
            helper.assertTrue(Contracten.vandaag().size() == 2 && Contracten.vandaag().equals(Contracten.vandaag()), "2 contracts");
        } finally {
            Z3Data.KLOK = oud;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kristallenZone3(GameTestHelper helper) {
        for (String s : Z3Kristallen.SOORTEN) {
            helper.assertTrue(!Kristallen.stack(s, 2, 1).isEmpty() && (s + ":2").equals(Kristallen.gem(Kristallen.stack(s, 2, 1))), "crystal " + s);
        }
        var bt = BasisTypes.van(Z3Wapens.TAKKENKNOTS);
        helper.assertTrue(bt != null, "zone-3 weapon registered");
        RandomSource rnd = RandomSource.create(3);
        boolean drie = false;
        for (int i = 0; i < 600; i++) {
            Zeldzaamheid z = Zeldzaamheid.values()[i % Zeldzaamheid.values().length];
            int a = Rollen.rol(bt, 35, z, rnd).sokjes().size(), b = Rollen.rol(bt, 47, z, rnd).sokjes().size();
            helper.assertTrue(a <= 2, "lv 35 at most 2 sockets: " + a);
            helper.assertTrue(b <= 3 && (z != Zeldzaamheid.GUHDDELIJK || b == 3), "lv 47 " + z + ": " + b);
            drie |= b == 3;
        }
        helper.assertTrue(drie, "a 3rd socket happens at lv 45+");
        // insert an Amberroze: Levensroof counts; the juwelier still never loses it
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.zetLevel(p, 47);
        Uitrusting u = Rollen.rol(bt, 40, Zeldzaamheid.ZELDZAAM, RandomSource.create(1)).metSokjes(List.of("", "", ""));
        p.getInventory().setItem(0, Rollen.stack(u));
        p.getInventory().setItem(1, Kristallen.stack("amberroze", 2, 1));
        double voor = Rollen.stats(u).get(Stat.LEVENSROOF);
        Juwelier.zet(p, 0, 2, 1);
        Uitrusting na = Rollen.van(p.getInventory().getItem(0));
        helper.assertTrue(na != null && "amberroze:2".equals(na.sokjes().get(2)), "gem in socket 3");
        helper.assertTrue(Math.abs(Rollen.stats(na).get(Stat.LEVENSROOF) - voor - 2.6) < 0.01, "Levensroof +2.6 %");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void goudenVeren(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.zetLevel(p, 50);
        GoudenVeren.geef(p, 24);
        helper.assertTrue(!GoudenVeren.kanRuilen(p) && !GoudenVeren.ruil(p) && GoudenVeren.aantal(p) == 24, "24 is not enough");
        GoudenVeren.geef(p, 1);
        helper.assertTrue(GoudenVeren.ruil(p), "25 feathers");
        helper.assertTrue(Rijdieren.heeftKleur(p, RijdierSoort.PluimKleur.GOUD) && GoudenVeren.aantal(p) == 0, "the gold Pluimguh, feathers spent");
        GoudenVeren.geef(p, 30);
        helper.assertTrue(!GoudenVeren.ruil(p) && GoudenVeren.aantal(p) == 30, "only once");
        // Vahoeg gate: lv 50 + a seal
        helper.assertTrue(Vahoeg.magVahoeg(p) && !Vahoeg.neemZegel(p), "no seal");
        p.getInventory().add(new net.minecraft.world.item.ItemStack(Z3Spul.VAHOEGZEGEL.get()));
        helper.assertTrue(Vahoeg.heeftZegel(p) && Vahoeg.neemZegel(p) && !Vahoeg.heeftZegel(p), "the seal is used");
        // the penningwinkel
        Z3Data.geefPenningen(p, Vahoeg.ZEGEL_PRIJS);
        helper.assertTrue(Premies.koop(p, "vahoegzegel").endsWith("gekocht") && Vahoeg.heeftZegel(p) && Z3Data.penningen(p) == 0, "bought a seal");
        helper.assertTrue(Premies.koop(p, "vahoegzegel").endsWith("te_weinig"), "no coins left");
        helper.succeed();
    }
}
