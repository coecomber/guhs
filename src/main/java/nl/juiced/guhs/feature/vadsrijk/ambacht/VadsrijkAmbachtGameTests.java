package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Zoek;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.economie.Waarde;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-AMBACHT: the Ambachten. Run: {@code runGameTestServer -Pgt=VadsrijkAmbacht}.
 */
public class VadsrijkAmbachtGameTests {
    private static final String BATCH = "vadsrijk_t_ambacht";

    @GuhTest(template = "empty", batch = BATCH)
    public static void xpCurve(GameTestHelper helper) {
        helper.assertTrue(AmbachtXp.CAP == 60, "cap 60 with zones 4 + 5 (U-AMBACHT2)");
        for (int l = 1; l <= AmbachtXp.CAP; l++) {
            helper.assertTrue(AmbachtXp.levelVan(AmbachtXp.totaalVoor(l)) == l, "round trip " + l);
        }
        helper.assertTrue(AmbachtXp.levelVan(Long.MAX_VALUE / 4) == AmbachtXp.CAP, "capped");
        helper.assertTrue(AmbachtXp.tierVoor(1) == 1 && AmbachtXp.tierVoor(20) == 2 && AmbachtXp.tierVoor(49) == 3, "tiers follow the zones");
        helper.assertTrue(AmbachtXp.tierVoor(50) == 4 && AmbachtXp.tierVoor(55) == 5 && AmbachtXp.tierVoor(60) == 5, "T4 50, T5 55 (T6 zone 6 volgt)");
        helper.assertTrue(AmbachtXp.verzamel(1, 10) == 30 && AmbachtXp.verzamel(1, 40) == 15, "half xp far above the tier");
        // pacing: 1 -> 20 on T1 at ~8 actions a minute takes 1-2 h, 35 -> 50 on T3 4-7 h
        double t1 = AmbachtXp.totaalVoor(20) / (double) AmbachtXp.VERZAMEL[1] / 8 / 60;
        double t3 = (AmbachtXp.totaalVoor(50) - AmbachtXp.totaalVoor(35)) / (double) AmbachtXp.VERZAMEL[3] / 8 / 60;
        helper.assertTrue(t1 > 1.0 && t1 < 2.2, "1->20 hours " + t1);
        helper.assertTrue(t3 > 4.0 && t3 < 7.0, "35->50 hours " + t3);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void dataKlopt(GameTestHelper helper) {
        for (Groep g : AmbachtPlekken.GROEPEN) {
            helper.assertTrue(g.anker() != null, "group anchor POI exists: " + g.id() + " (" + g.poi() + ")");
        }
        for (AmbachtPlekken.Werkplaats w : AmbachtPlekken.WERKPLAATSEN) {
            helper.assertTrue(w.anker() != null, "workshop hub POI exists: " + w.npc());
        }
        for (Recepten.Recept r : Recepten.ALLE.values()) {
            for (Recepten.Ingredient i : r.in()) {
                for (String o : i.opties()) {
                    helper.assertTrue(Materialen.ALLE.containsKey(o) || Materialen.KERN.containsKey(o) || o.equals("kernsplinter"), r.id() + ": unknown " + o);
                }
            }
            helper.assertTrue(!Recepten.toon(r).isEmpty(), r.id() + " shows a result");
        }
        for (Maatwerk.Soort s : Maatwerk.SOORTEN.values()) {
            for (int t = 1; t <= AmbachtXp.MAX_TIER; t++) {
                helper.assertTrue(BasisTypes.van(s.basisId(t)) != null, "base type " + s.basisId(t));
            }
        }
        // every made-ambacht has recipes at every tier, the gatherers have materials at every tier
        for (Ambacht a : Ambacht.values()) {
            if (a.verzamel) {
                for (int t = a == Ambacht.IMKEREN ? 3 : 1; t <= AmbachtXp.MAX_TIER; t++) {
                    helper.assertTrue(!Materialen.van(a, t).isEmpty(), a.id + " T" + t);
                }
            } else {
                helper.assertTrue(Recepten.van(a).size() >= 6, a.id + " recipes");
                for (int t = 1; t <= AmbachtXp.MAX_TIER; t++) {
                    int tier = t;
                    helper.assertTrue(Recepten.van(a).stream().anyMatch(r -> r.tier() == tier), a.id + " has T" + t + " recipes (grows with every zone)");
                }
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void xpEnUitgerust(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        long g = AmbachtData.geefXp(p, Ambacht.MIJNEN, 100, true);
        helper.assertTrue(g == 100 && AmbachtData.xp(p, Ambacht.MIJNEN) == 100, "plain xp " + g);
        AmbachtData.zetUitgerust(p, 70);
        g = AmbachtData.geefXp(p, Ambacht.MIJNEN, 100, true);
        helper.assertTrue(g == 200 && AmbachtData.uitgerust(p) == 63, "rested doubles and uses 7 s: " + g + " / " + AmbachtData.uitgerust(p));
        // offline: 1 s rest per 6 s away, capped at 2 h
        var oud = AmbachtData.KLOK;
        try {
            long nu = System.currentTimeMillis();
            AmbachtData.KLOK = () -> nu;
            AmbachtData.zetUitgerust(p, 0);
            AmbachtData.tag(p).putLong("laatst", nu - 6000L * 1000);
            AmbachtData.ingelogd(p);
            helper.assertTrue(AmbachtData.uitgerust(p) == 1000, "offline rest " + AmbachtData.uitgerust(p));
            AmbachtData.tag(p).putLong("laatst", nu - 1000L * 3600 * 1000);
            AmbachtData.ingelogd(p);
            helper.assertTrue(AmbachtData.uitgerust(p) == AmbachtXp.UITGERUST_MAX, "capped at 2 h");
        } finally {
            AmbachtData.KLOK = oud;
        }
        AmbachtData.zetLevel(p, Ambacht.KOKEN, 30);
        helper.assertTrue(AmbachtData.level(p, Ambacht.KOKEN) == 30, "zetLevel");
        helper.succeed();
    }

    /** A flat 11x11 test field (floor y 1). */
    private static void veld(GameTestHelper helper, net.minecraft.world.level.block.Block vloer) {
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 1, z), vloer.defaultBlockState());
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void scanTerrein(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos anker = helper.absolutePos(new BlockPos(6, 2, 6));
        veld(helper, Blocks.GRASS_BLOCK);
        List<Spot> gras = PlekWereld.scan(sl, anker, new Groep("t_gras", 1, "x", Ambacht.PLUKKEN, 1, 1, 5, Zoek.GROND, ""));
        helper.assertTrue(gras.size() == 1 && gras.get(0).posities().size() == AmbachtPlekken.POSITIES, "grass: 1 spot with 4 positions");
        for (BlockPos p : gras.get(0).posities()) {
            helper.assertTrue(sl.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) && sl.getBlockState(p).isAir(), "on the grass at " + p);
        }
        // ore wants rock
        veld(helper, Blocks.STONE);
        List<Spot> steen = PlekWereld.scan(sl, anker, new Groep("t_steen", 1, "x", Ambacht.MIJNEN, 1, 1, 5, Zoek.STEEN, ""));
        helper.assertTrue(steen.size() == 1 && sl.getBlockState(steen.get(0).posities().get(0).below()).is(Blocks.STONE), "ore on stone");
        // a path never gets a spot (strict pass); deterministic
        List<Spot> nog = PlekWereld.scan(sl, anker, new Groep("t_steen", 1, "x", Ambacht.MIJNEN, 1, 1, 5, Zoek.STEEN, ""));
        helper.assertTrue(nog.get(0).posities().equals(steen.get(0).posities()), "same terrain, same spots");
        // fish: still water with a shore
        veld(helper, Blocks.GRASS_BLOCK);
        for (int x = 3; x < 9; x++) {
            for (int z = 3; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
            }
        }
        List<Spot> water = PlekWereld.scan(sl, anker.below(), new Groep("t_water", 1, "x", Ambacht.VISSEN, 1, 1, 5, Zoek.WATER, ""));
        helper.assertTrue(!water.isEmpty(), "fish spots in the pond");
        for (BlockPos p : water.get(0).posities()) {
            helper.assertTrue(sl.getFluidState(p).isSource(), "a fish spot is water: " + p);
        }
        // hives: flowers nearby are kept for the bees
        veld(helper, Blocks.GRASS_BLOCK);
        for (int x = 1; x < 11; x += 3) {
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.POPPY.defaultBlockState());
            helper.setBlock(new BlockPos(x, 2, 10), Blocks.DANDELION.defaultBlockState());
        }
        List<Spot> korf = PlekWereld.scan(sl, anker, new Groep("t_korf", 3, "x", Ambacht.IMKEREN, 3, 1, 5, Zoek.BLOEM, "schemerhoning"));
        helper.assertTrue(!korf.isEmpty() && !korf.get(0).bloemen().isEmpty(), "a hive knows its flowers");
        helper.succeed();
    }

    private static Spot testSpot(GameTestHelper helper, Ambacht a, int tier) {
        Groep g = new Groep("t_" + a.id, 1, "x", a, tier, 1, 5, Zoek.GROND, a == Ambacht.IMKEREN ? "bloesemhoning" : "");
        return new Spot(g.id() + "_0", g, List.of(helper.absolutePos(new BlockPos(2, 2, 2)), helper.absolutePos(new BlockPos(8, 2, 8))),
                List.of(helper.absolutePos(new BlockPos(3, 2, 3))));
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void oogstPerSpeler(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        AmbachtData.reset(a);
        AmbachtData.reset(b);
        Verzamelen.vergeet(a.getUUID());
        Spot s = testSpot(helper, Ambacht.PLUKKEN, 1);
        BlockPos voor = Verzamelen.positie(a, s);
        String m = Verzamelen.doeOogst(a, sl, s, 2);
        helper.assertTrue(m.isEmpty(), "harvest: " + m);
        helper.assertTrue(!Verzamelen.beschikbaar(a, s) && Verzamelen.beschikbaar(b, s), "empty for A only");
        helper.assertTrue(!Verzamelen.positie(a, s).equals(voor), "it comes back at another position");
        int n = 0, q = -1;
        for (int i = 0; i < a.getInventory().getContainerSize(); i++) {
            ItemStack st = a.getInventory().getItem(i);
            if (Materialen.van(st) != null) {
                n += st.getCount();
                q = Materialen.kwaliteit(st);
            }
        }
        helper.assertTrue(n >= 1 && q == 2, "got " + n + " extra vadsig material (q " + q + ")");
        helper.assertTrue(AmbachtData.xp(a, Ambacht.PLUKKEN) >= 30 && AmbachtData.heeft(a, "plekken", "t_plukken"), "xp + book");
        helper.assertTrue(Verzamelen.doeOogst(a, sl, s, 0).equals("vadsrijk.ambacht.oogst.leeg"), "not twice");
        // too low for tier 3
        Verzamelen.vergeet(b.getUUID());
        helper.assertTrue(Verzamelen.doeOogst(b, sl, testSpot(helper, Ambacht.MIJNEN, 3), 0).equals("vadsrijk.ambacht.oogst.te_laag"), "tier gate");
        // a rare spot gives 3x
        Verzamelen.vergeet(b.getUUID());
        AmbachtData.zetLevel(b, Ambacht.HOUTHAKKEN, 1);
        Spot h = testSpot(helper, Ambacht.HOUTHAKKEN, 1);
        Verzamelen.maakZeldzaam(b, h);
        Verzamelen.doeOogst(b, sl, h, 0);
        int hout = MateriaalTas.tel(b, st -> Materialen.van(st) != null && Materialen.van(st).id().equals("zwamsteel"));
        helper.assertTrue(hout >= 3, "rare spot: 3x, got " + hout);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void imkeren(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        Spot k = testSpot(helper, Ambacht.IMKEREN, 3);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 1).isEmpty() && Imkeren.eigen(p, k), "first click places your hive");
        helper.assertTrue(Imkeren.raten(p, k) == 0, "empty at first");
        var oud = AmbachtData.KLOK;
        try {
            long nu = System.currentTimeMillis();
            AmbachtData.KLOK = () -> nu + 60L * 60_000;   // an hour later
            int r = Imkeren.raten(p, k);
            helper.assertTrue(r >= 2 && r <= Imkeren.MAX_RATEN, "combs after an hour: " + r);
            helper.assertTrue(Verzamelen.doeOogst(p, sl, k, 2).isEmpty(), "harvest");
            helper.assertTrue(MateriaalTas.tel(p, s -> Materialen.van(s) != null && Materialen.van(s).id().equals("bloesemhoning")) == r, "honey per comb");
            helper.assertTrue(MateriaalTas.tel(p, s -> Materialen.van(s) != null && Materialen.van(s).id().equals("bijenwas")) >= 1, "wax");
            helper.assertTrue(Imkeren.raten(p, k) == 0 && AmbachtData.heeft(p, "honing", "bloesemhoning"), "emptied + book");
        } finally {
            AmbachtData.KLOK = oud;
        }
        helper.assertTrue(Imkeren.max(1) == 1 && Imkeren.max(30) == 3, "more hives with level");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kokenEnBrouwen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        p.getInventory().clearContent();
        MateriaalTas.geef(p, Materialen.stack("klodderklaver", 2, 4));
        MateriaalTas.geef(p, Materialen.stack("kloddervisje", 2, 2));
        String m = Werkplaats.maak(p, "klodderpudding", List.of(), null, null, 2, 1);
        helper.assertTrue(m.startsWith("vadsrijk.ambacht.werk."), "made: " + m);
        ItemStack pud = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).getItem() instanceof Eten.EtenItem) {
                pud = p.getInventory().getItem(i);
            }
        }
        helper.assertTrue(pud.getCount() == 2 && Gemaakt.vadsigheid(pud) >= 30 + 30 + 24, "2 puddings, vadsigheid " + Gemaakt.vadsigheid(pud));
        helper.assertTrue(MateriaalTas.tel(p, s -> Materialen.van(s) != null) == 3, "ingredients taken (2 klaver + 1 vis)");
        helper.assertTrue(AmbachtData.xp(p, Ambacht.KOKEN) > 0, "koken xp");
        helper.assertTrue(Werkplaats.maak(p, "wortelstamppot", List.of(), null, null, 0, 1).equals("vadsrijk.ambacht.werk.te_laag"), "level gate");
        // the buff: 30 min, x Vadsigheid, one at a time
        Eten.zet(p, Eten.GERECHTEN.get("klodderpudding"), 100);
        helper.assertTrue(Math.abs(Eten.stats(p).get(Stat.VADS) - 78) < 0.01, "Vads +60 x1.3 = " + Eten.stats(p).get(Stat.VADS));
        Eten.zet(p, Eten.GERECHTEN.get("verzamelaarshapje"), 0);
        helper.assertTrue(Eten.stats(p).get(Stat.VADS) == 0 && Eten.xpFactor(p) > 1, "the new buff replaces the old");
        var oud = AmbachtData.KLOK;
        try {
            long nu = System.currentTimeMillis();
            AmbachtData.KLOK = () -> nu + Eten.DUUR_MS + 1000;
            helper.assertTrue(Eten.actief(p) == null, "gone after 30 min");
        } finally {
            AmbachtData.KLOK = oud;
        }
        // brewing: cutting crystals 3 ruw -> 1 geslepen
        AmbachtData.zetLevel(p, Ambacht.BROUWEN, 15);
        p.getInventory().add(Kristallen.stack("rozekwarts", 0, 3));
        m = Werkplaats.maak(p, "slijp_geslepen", List.of(), null, "rozekwarts", 1, 1);
        helper.assertTrue(MateriaalTas.tel(p, Werkplaats.kristalVan("rozekwarts", 1)) == 1 && MateriaalTas.tel(p, Werkplaats.kristalVan("rozekwarts", 0)) == 0,
                "cut: " + m);
        // a brewed potion: Vadsigheid makes it stronger
        ItemStack d = new ItemStack(Brouwsels.ITEM.get("vadsdrankje").get());
        Gemaakt.zet(d, 100, "x");
        helper.assertTrue(Math.abs(Gemaakt.factor(d) - 1.3) < 1e-9, "factor 1.3 at 100 %");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void maatwerkEnMeesterwerk(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        ItemStack s = Maatwerk.maak(Maatwerk.SOORTEN.get("zwaard"), 3, List.of(Stat.KRITIEK, Stat.KRACHT), 100, "test", null, p.getRandom());
        Uitrusting u = Rollen.van(s);
        helper.assertTrue(u != null && u.zeldzaamheid() == Zeldzaamheid.EPISCH && !u.gebonden() && u.level() == 45, "Episch, tradeable, lv 45");
        helper.assertTrue(u.bonussen().size() == Zeldzaamheid.EPISCH.maxBonus, "4 bonuses: " + u.bonussen());
        helper.assertTrue(!u.sokjes().isEmpty(), "+1 socket guaranteed");
        for (Bonus b : u.bonussen()) {
            if (b.stat() == Stat.KRITIEK || b.stat() == Stat.KRACHT) {
                double[] r = Bonussen.bereik(b.stat(), 45);
                helper.assertTrue(b.waarde() >= r[0] + (r[1] - r[0]) * 0.78, "100 %: chosen bonus in the top 20 %: " + b);
            }
        }
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets.setVan(u.type()) == null, "no set bonus");
        // ~90 % of the zone-3 set (schorspantser 26/76 vs maatwerk borstplaat)
        BasisType mw = BasisTypes.van("mw_t3_borstplaat"), set = BasisTypes.van("schorspantser");
        if (set != null) {
            double f = mw.statsOp(45).get(Stat.VERDEDIGING) / set.statsOp(45).get(Stat.VERDEDIGING);
            helper.assertTrue(f > 0.8 && f < 0.95, "~90 % of the set: " + f);
        }
        // Meesterwerk jewellery: Legendarisch, bound, 3 chosen + a property, above the zone-3 drop
        ItemStack r = Maatwerk.maak(Maatwerk.SOORTEN.get("ring"), 3, List.of(Stat.KRITIEK, Stat.KRITIEKSCHADE, Stat.AANVALSSNELHEID), 80, "test",
                Maatwerk.Eigenschap.BAZEN, p.getRandom());
        Uitrusting ru = Rollen.van(r);
        helper.assertTrue(ru.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH && ru.gebonden() && Maatwerk.info(r).eigenschap().equals("bazen"), "Meesterwerk");
        BasisType drop = BasisTypes.van("honingring");
        if (drop != null) {
            double mwK = ru.basisType().statsOp(45).get(Stat.KRITIEK) * ru.zeldzaamheid().factor;
            double dropK = drop.statsOp(45).get(Stat.KRITIEK) * Zeldzaamheid.LEGENDARISCH.factor;
            helper.assertTrue(mwK > dropK, "BiS: " + mwK + " > " + dropK);
        }
        // the property works: +8 % against bosses only (no boss here: the plain hit is untouched)
        double plain = Maatwerk.voorKlap(new nl.juiced.guhs.feature.vadsrijk.kern.Schade.Klap(p, p, 100, nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN,
                nl.juiced.guhs.feature.vadsrijk.kern.Schade.Soort.MELEE, false, "test"), 100);
        helper.assertTrue(plain == 100, "no boss, no bonus");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bestelbord(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        List<String> ids = Bestelbord.ids(p);
        helper.assertTrue(ids.size() == Bestelbord.PER_DAG, "3 orders for a beginner: " + ids);
        for (String id : ids) {
            helper.assertTrue(Bestelbord.POOL.get(id).level() <= 1, "beginner orders only: " + id);
        }
        // complete slot 0 whatever it is
        Bestelbord.Bestelling b = Bestelbord.POOL.get(ids.get(0));
        if (b.soort().equals("breng")) {
            String w = b.wat();
            ItemStack st = w.startsWith("eten:") ? new ItemStack(Eten.ITEM.get(w.substring(5)).get(), b.aantal())
                    : w.startsWith("drank:") ? new ItemStack(Brouwsels.ITEM.get(w.substring(6)).get(), b.aantal()) : Materialen.stack(w, 0, b.aantal());
            p.getInventory().add(st);
        } else if (b.soort().equals("verzamel")) {
            Bestelbord.verzameld(p, b.ambacht(), b.aantal());
        } else {
            for (int i = 0; i < b.aantal(); i++) {
                Bestelbord.gemaakt(p, Recepten.van(b.ambacht()).get(0), 1);
            }
        }
        helper.assertTrue(Bestelbord.klaar(p, 0), "order 0 done (" + b.id() + ")");
        long munten = nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).munten;
        helper.assertTrue(Bestelbord.claim(p, 0).equals("vadsrijk.ambacht.bestel.geclaimd") && Bestelbord.geclaimd(p, 0), "claimed");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).munten > munten && AmbachtData.xp(p, b.ambacht()) > 0, "reward");
        helper.assertTrue(!Bestelbord.claim(p, 0).equals("vadsrijk.ambacht.bestel.geclaimd"), "not twice");
        helper.assertTrue(Bestelbord.ruil(p, 1).equals("vadsrijk.ambacht.bestel.geruild") && !Bestelbord.ruil(p, 2).equals("vadsrijk.ambacht.bestel.geruild"),
                "one free swap a day");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void ranglijstEnWaarde(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        AmbachtData.reset(a);
        AmbachtData.reset(b);
        AmbachtData.zetLevel(a, Ambacht.VISSEN, 12);
        AmbachtData.zetLevel(b, Ambacht.VISSEN, 30);
        Ranglijst.bij(a);
        Ranglijst.bij(b);
        List<Ranglijst.Plek> l = Ranglijst.get(helper.getLevel().getServer()).lijst("vissen");
        int ia = -1, ib = -1;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).uuid().equals(a.getUUID())) {
                ia = i;
            }
            if (l.get(i).uuid().equals(b.getUUID())) {
                ib = i;
            }
        }
        helper.assertTrue(ib >= 0 && ia > ib, "higher level ranks higher");
        helper.assertTrue(Waarde.verkoop(Materialen.stack("amberkoper", 0, 10)) == 150, "materials sell by tier: " + Waarde.verkoop(Materialen.stack("amberkoper", 0, 10)));
        helper.assertTrue(Materialen.stack("pluisijzer", 1, 1).getMaxStackSize() == 64, "materials stack (market)");
        helper.assertTrue(!ItemStack.isSameItemSameComponents(Materialen.stack("pluisijzer", 1, 1), Materialen.stack("pluisijzer", 0, 1)), "only equal quality stacks");
        helper.succeed();
    }
}
