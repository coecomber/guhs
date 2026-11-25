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
        helper.assertTrue(AmbachtXp.CAP == 60 && Math.abs(AmbachtXp.SCHAAL - 0.33) < 1e-9, "cap 60, scale 0.33 (1.10.4: 3x faster)");
        for (int l = 1; l <= AmbachtXp.CAP; l++) {
            helper.assertTrue(AmbachtXp.levelVan(AmbachtXp.totaalVoor(l)) == l, "round trip " + l);
        }
        helper.assertTrue(AmbachtXp.levelVan(Long.MAX_VALUE / 4) == AmbachtXp.CAP, "capped");
        // 1.10.4 tiers: T1 1, T2 12, T3 22, T4 32, T5 40, T6 48 (zone 6)
        helper.assertTrue(AmbachtXp.tierVoor(1) == 1 && AmbachtXp.tierVoor(11) == 1 && AmbachtXp.tierVoor(12) == 2 && AmbachtXp.tierVoor(21) == 2
                && AmbachtXp.tierVoor(22) == 3 && AmbachtXp.tierVoor(32) == 4 && AmbachtXp.tierVoor(40) == 5 && AmbachtXp.tierVoor(47) == 5
                && AmbachtXp.tierVoor(48) == 6 && AmbachtXp.tierVoor(60) == 6, "tiers");
        helper.assertTrue(AmbachtXp.TIER_MIN[6] == 48 && AmbachtXp.tierLevel(2) == 12 && AmbachtXp.tierTop(1) == 11 && AmbachtXp.tierTop(5) == 47
                && AmbachtXp.tierTop(6) == 60, "bands");
        helper.assertTrue(AmbachtXp.verzamel(1, 10) == 30 && AmbachtXp.verzamel(1, 16) == 30 && AmbachtXp.verzamel(1, 17) == 15, "half xp 5 levels over the top");
        helper.assertTrue(AmbachtXp.verzamel(6, 60) == AmbachtXp.VERZAMEL[6] && AmbachtXp.verzamel(5, 53) == AmbachtXp.VERZAMEL[5] / 2, "never half on the highest tier (1.10.4: T6; T5 half from 53)");
        // pacing (design "Getallen"): 1 -> 10 ~40 harvests, 1 -> 20 ~250 harvests on T1 (30-40 min at ~8 a minute)
        double h10 = AmbachtXp.totaalVoor(10) / (double) AmbachtXp.VERZAMEL[1], h20 = AmbachtXp.totaalVoor(20) / (double) AmbachtXp.VERZAMEL[1];
        helper.assertTrue(h10 > 35 && h10 < 46, "1->10 harvests " + h10);
        helper.assertTrue(h20 > 220 && h20 < 280 && h20 / 8 > 25 && h20 / 8 < 40, "1->20 harvests " + h20);
        helper.assertTrue(AmbachtXp.oudNaarNieuw(20) == 12 && AmbachtXp.oudNaarNieuw(35) == 22 && AmbachtXp.oudNaarNieuw(50) == 32
                && AmbachtXp.oudNaarNieuw(55) == 40 && AmbachtXp.oudNaarNieuw(1) == 1, "old -> new level scale");
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
        // every made-ambacht has recipes at every tier, the gatherers have materials at every tier (per spot kind; hives from zone 3)
        helper.assertTrue(Ambacht.values().length == 4, "4 ambachten (1.10.4)");
        for (NodeSoort n : NodeSoort.values()) {
            for (int t = n == NodeSoort.KORF ? 3 : n == NodeSoort.DUIK ? 4 : 1; t <= (n == NodeSoort.DUIK ? 4 : AmbachtXp.MAX_TIER); t++) {
                helper.assertTrue(!Materialen.van(n, t).isEmpty(), n.id + " T" + t);
            }
        }
        for (Ambacht a : Ambacht.values()) {
            if (a.verzamel) {
                for (int t = 1; t <= AmbachtXp.MAX_TIER; t++) {
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
        long g = AmbachtData.geefXp(p, Ambacht.VERZAMELEN, 100, true);
        helper.assertTrue(g == 100 && AmbachtData.xp(p, Ambacht.VERZAMELEN) == 100, "plain xp " + g);
        AmbachtData.zetUitgerust(p, 70);
        g = AmbachtData.geefXp(p, Ambacht.VERZAMELEN, 100, true);
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
        List<Spot> gras = PlekWereld.scan(sl, anker, new Groep("t_gras", 1, "x", NodeSoort.KRUID, 1, 1, 5, Zoek.GROND, ""));
        helper.assertTrue(gras.size() == 1 && gras.get(0).posities().size() == AmbachtPlekken.POSITIES, "grass: 1 spot with 4 positions");
        for (BlockPos p : gras.get(0).posities()) {
            helper.assertTrue(sl.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) && sl.getBlockState(p).isAir(), "on the grass at " + p);
        }
        // ore wants rock
        veld(helper, Blocks.STONE);
        List<Spot> steen = PlekWereld.scan(sl, anker, new Groep("t_steen", 1, "x", NodeSoort.ERTS, 1, 1, 5, Zoek.STEEN, ""));
        helper.assertTrue(steen.size() == 1 && sl.getBlockState(steen.get(0).posities().get(0).below()).is(Blocks.STONE), "ore on stone");
        // a path never gets a spot (strict pass); deterministic
        List<Spot> nog = PlekWereld.scan(sl, anker, new Groep("t_steen", 1, "x", NodeSoort.ERTS, 1, 1, 5, Zoek.STEEN, ""));
        helper.assertTrue(nog.get(0).posities().equals(steen.get(0).posities()), "same terrain, same spots");
        // fish: still water with a shore
        veld(helper, Blocks.GRASS_BLOCK);
        for (int x = 3; x < 9; x++) {
            for (int z = 3; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
            }
        }
        List<Spot> water = PlekWereld.scan(sl, anker.below(), new Groep("t_water", 1, "x", NodeSoort.VIS, 1, 1, 5, Zoek.WATER, ""));
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
        List<Spot> korf = PlekWereld.scan(sl, anker, new Groep("t_korf", 3, "x", NodeSoort.KORF, 3, 1, 5, Zoek.BLOEM, "schemerhoning"));
        helper.assertTrue(!korf.isEmpty() && !korf.get(0).bloemen().isEmpty(), "a hive knows its flowers");
        helper.succeed();
    }

    /**
     * 1.10.14: the secret IJsgrot: a closed room with a roof, a sealed hollow under its floor and a ledge on its roof. Its spots lie on
     * the room floor only, where a player can walk (live: 0 of 12 positions were reachable).
     */
    @GuhTest(template = "empty", batch = BATCH + "_b")   // (own batch: its blocks above the floor hung over the pier test)
    public static void scanBinnen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 8; x++) {   // (small: the roof of a bigger room hung over the neighbouring pier test)
            for (int z = 0; z < 8; z++) {
                boolean rand = x == 0 || z == 0 || x == 7 || z == 7;
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), rand ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());   // the hollow
                helper.setBlock(new BlockPos(x, 2, z), rand ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 3, z), Blocks.SNOW_BLOCK.defaultBlockState());                                    // the floor
                for (int y = 4; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), rand ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                helper.setBlock(new BlockPos(x, 7, z), Blocks.STONE.defaultBlockState());                                         // the roof
                helper.setBlock(new BlockPos(x, 8, z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 9, z), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos anker = helper.absolutePos(new BlockPos(4, 4, 4));
        List<Spot> l = PlekWereld.scan(sl, anker, new Groep("z2_mijn_ijsgrot", 2, "x", NodeSoort.ERTS, 2, 3, 16, Zoek.STEEN, ""));
        helper.assertTrue(!l.isEmpty(), "spots in the room");
        for (Spot s : l) {
            for (BlockPos p : s.posities()) {
                helper.assertTrue(p.getY() == anker.getY(), "a room spot lies on the room floor: " + p + " (anchor " + anker + ")");
                BlockPos rel = p.subtract(helper.absolutePos(BlockPos.ZERO));
                helper.assertTrue(rel.getX() > 0 && rel.getX() < 7 && rel.getZ() > 0 && rel.getZ() < 7, "inside the room: " + p);
            }
        }
        // a version-3 save drops the IJsgrot (scanned again), the rest stays
        Groep ijs = AmbachtPlekken.groep("z2_mijn_ijsgrot"), kamp = AmbachtPlekken.groep("z1_pluk_kamp");
        net.minecraft.nbt.CompoundTag v3 = new net.minecraft.nbt.CompoundTag();
        v3.putInt(PlekWereld.VERSIE_SLEUTEL, 3);
        v3.put(ijs.id(), groepTag(ijs, ijs.aantal()));
        v3.put(kamp.id(), groepTag(kamp, kamp.aantal()));
        PlekWereld w = PlekWereld.load(v3);
        helper.assertTrue(w.bekend(ijs) == null && w.bekend(kamp) != null, "version 3: IJsgrot dropped, the rest kept");
        helper.succeed();
    }

    /**
     * 1.10.19: fishing in the honey ponds (honey blocks, no water) and in water that lies off-centre (the full-circle pass: the
     * Honingmoeras found 1 spot of 6 by random tries, the Mosvoet waterwheel 0).
     */
    @GuhTest(template = "empty", batch = BATCH + "_b")   // (own batch: its blocks above the floor hung over the pier test)
    public static void scanHoningEnRand(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 2; y < 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = 4; x < 8; x++) {   // a honey pond in one corner, sunk into the grass
            for (int z = 4; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.HONEY_BLOCK.defaultBlockState());
            }
        }
        BlockPos anker = helper.absolutePos(new BlockPos(1, 2, 1));
        List<Spot> hon = PlekWereld.scan(sl, anker, new Groep("z3_vis_honingvijver", 3, "x", NodeSoort.VIS, 3, 1, 8, Zoek.WATER, ""));
        helper.assertTrue(hon.size() == 1 && hon.get(0).posities().size() >= 3, "a honey-pond spot (3-4 positions, by where the test stands): " + hon);
        for (BlockPos p : hon.get(0).posities()) {
            helper.assertTrue(sl.getBlockState(p).is(Blocks.HONEY_BLOCK) && Grondstoffen.magHier(sl, NodeSoort.VIS, p), "a honey position: " + p);
        }
        for (int x = 4; x < 8; x++) {   // the same corner as water: an ordinary group finds it through the full-circle pass
            for (int z = 4; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
            }
        }
        List<Spot> rand = PlekWereld.scan(sl, anker, new Groep("t_rand", 3, "x", NodeSoort.VIS, 3, 1, 8, Zoek.WATER, ""));
        helper.assertTrue(rand.size() == 1 && rand.get(0).posities().size() >= 3, "the water in the corner is found (its outer rim has no shore): " + rand);
        helper.succeed();
    }

    /** 1.10.13: a frozen lake (the Knuffelmeer froze over on live: 0 spots): fished through holes in the ice; a node may stand there. */
    @GuhTest(template = "empty", batch = BATCH + "_b")   // (own batch: its blocks hung over the pier test)
    public static void scanIjs(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 2, z), Blocks.ICE.defaultBlockState());
                for (int y = 3; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockPos anker = helper.absolutePos(new BlockPos(6, 2, 6));
        List<Spot> water = PlekWereld.scan(sl, anker, new Groep("t_ijs", 2, "x", NodeSoort.VIS, 2, 1, 5, Zoek.WATER, ""));
        helper.assertTrue(!water.isEmpty(), "fish spots in the frozen lake");
        for (BlockPos p : water.get(0).posities()) {
            helper.assertTrue(sl.getBlockState(p).is(Blocks.ICE) && Grondstoffen.magHier(sl, NodeSoort.VIS, p), "an ice hole: " + p);
        }
        helper.assertTrue(!PlekWereld.wak(sl, helper.absolutePos(new BlockPos(6, 1, 6))), "water itself is no ice hole");
        helper.succeed();
    }

    /** 1.10.13: a node whose spot no longer exists (a rescan found fewer spots) leaves the world; its original block comes back. */
    @GuhTest(template = "empty", batch = BATCH + "_b")   // (own batch: its blocks hung over the pier test)
    public static void weesNodeOpruimen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Groep g = AmbachtPlekken.groep("z2_vis_meer_1");
        helper.assertTrue(g != null && g.anker() != null, "group with an anchor");
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt(PlekWereld.VERSIE_SLEUTEL, PlekWereld.VERSIE);
        t.put(g.id(), groepTag(g, 1));   // known scan: one spot (_0)
        PlekWereld pw = PlekWereld.get(sl.getServer());
        PlekWereld geladen = PlekWereld.load(t);
        Spot echt = geladen.bekend(g).get(0);
        Spot wees = new Spot(g.id() + "_3", g, List.of(helper.absolutePos(new BlockPos(2, 2, 2))), List.of());
        BlockPos p = wees.posities().get(0);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIRT.defaultBlockState());
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER.defaultBlockState());
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR.defaultBlockState());
        net.minecraft.nbt.CompoundTag oudPw = pw.saveVoorTest();
        try {
            pw.zetVoorTest(geladen);
            Grondstoffen gs = Grondstoffen.get(sl.getServer());
            int[] budget = {5};
            gs.bijwerk(sl, wees, budget);
            helper.assertTrue(Grondstoffen.isNode(sl.getBlockState(p), NodeSoort.VIS), "the orphan's node stands");
            gs.node(echt);
            int weg = gs.opruimen(sl);
            helper.assertTrue(weg >= 1 && gs.bekend(wees.id()) == null && gs.bekend(echt.id()) != null, "only the orphan is cleaned up");
            helper.assertTrue(sl.getBlockState(p).is(Blocks.WATER), "its water is back");
        } finally {
            pw.zetVoorTest(PlekWereld.load(oudPw));
        }
        helper.succeed();
    }

    /** 1.10.12: fishing from a pier: the deck lies 2 above the water and there is no bank at the water line (the Vissteiger, De Haven). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void scanSteiger(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
                for (int y = 2; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int z = 0; z < 12; z++) {
            helper.setBlock(new BlockPos(6, 3, z), Blocks.OAK_PLANKS.defaultBlockState());   // the pier deck
        }
        BlockPos anker = helper.absolutePos(new BlockPos(6, 1, 6));
        List<Spot> water = PlekWereld.scan(sl, anker, new Groep("t_steiger", 1, "x", NodeSoort.VIS, 1, 1, 5, Zoek.WATER, ""));
        helper.assertTrue(!water.isEmpty(), "fish spots beside the pier");
        BlockPos deck = helper.absolutePos(new BlockPos(6, 3, 0));
        for (BlockPos p : water.get(0).posities()) {
            helper.assertTrue(sl.getFluidState(p).isSource() && Math.abs(p.getX() - deck.getX()) <= 2 && p.getX() != deck.getX(),
                    "beside (never under) the pier: " + p);
        }
        // open sea without a pier: still nothing
        for (int z = 0; z < 12; z++) {
            helper.setBlock(new BlockPos(6, 3, z), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!PlekWereld.oever(sl, anker), "open water is no shore");
        helper.succeed();
    }

    /** 1.10.12: an old save drops the incomplete non-hive groups (scanned again with the new shore rule); the rest stays. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void plekkenOpnieuw(GameTestHelper helper) {
        Groep haven = AmbachtPlekken.groep("z1_vis_haven"), kamp = AmbachtPlekken.groep("z1_pluk_kamp"),
                korf = AmbachtPlekken.groep("eiland_bijenhofje");
        helper.assertTrue(haven != null && kamp != null && korf != null && haven.poi().equals("haven_z1_kamp_vertrek"), "groups (De Haven at the quay)");
        net.minecraft.nbt.CompoundTag oud = new net.minecraft.nbt.CompoundTag();
        oud.put(haven.id(), groepTag(haven, 0));
        oud.put(kamp.id(), groepTag(kamp, kamp.aantal()));
        oud.put(korf.id(), groepTag(korf, 1));
        PlekWereld w = PlekWereld.load(oud);
        helper.assertTrue(w.bekend(haven) == null, "incomplete fishing group dropped");
        helper.assertTrue(w.bekend(kamp) != null && w.bekend(kamp).size() == kamp.aantal(), "complete group kept");
        helper.assertTrue(w.bekend(korf) != null && w.bekend(korf).size() == 1, "hive meadow kept (hives are keyed on spot ids)");
        PlekWereld nieuw = PlekWereld.load(w.save());
        helper.assertTrue(nieuw.bekend(kamp) != null && nieuw.bekend(korf) != null, "the new version keeps everything");
        net.minecraft.nbt.CompoundTag v2 = new net.minecraft.nbt.CompoundTag();
        v2.putInt(PlekWereld.VERSIE_SLEUTEL, PlekWereld.VERSIE);
        v2.put(haven.id(), groepTag(haven, 0));
        helper.assertTrue(PlekWereld.load(v2).bekend(haven) != null, "a scan with the new rules is kept, even when it found little");
        // 1.10.13: a version-2 save drops the (complete) cave groups: they are scanned again on the cave floor
        Groep grot = AmbachtPlekken.groep("grot_zwamgrot_amb_2");
        helper.assertTrue(grot != null, "cave group");
        net.minecraft.nbt.CompoundTag oud2 = new net.minecraft.nbt.CompoundTag();
        oud2.putInt(PlekWereld.VERSIE_SLEUTEL, 2);
        oud2.put(grot.id(), groepTag(grot, grot.aantal()));
        oud2.put(kamp.id(), groepTag(kamp, kamp.aantal()));
        PlekWereld w2 = PlekWereld.load(oud2);
        helper.assertTrue(w2.bekend(grot) == null && w2.bekend(kamp) != null, "version 2: cave group dropped, the rest kept");
        helper.assertTrue(PlekWereld.load(w2.save()).bekend(kamp) != null, "saved as version 3");
        net.minecraft.nbt.CompoundTag v3 = new net.minecraft.nbt.CompoundTag();
        v3.putInt(PlekWereld.VERSIE_SLEUTEL, PlekWereld.VERSIE);
        v3.put(grot.id(), groepTag(grot, grot.aantal()));
        helper.assertTrue(PlekWereld.load(v3).bekend(grot) != null, "a cave scan with the new rules is kept");
        helper.succeed();
    }

    /** 1.10.12: the admin command takes back all hives of a player, nothing else. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void korvenWeg(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.geefXp(p, Ambacht.VERZAMELEN, 500, true);
        net.minecraft.nbt.CompoundTag k = new net.minecraft.nbt.CompoundTag();
        k.put("eiland_bijenhofje_2", new net.minecraft.nbt.CompoundTag());
        k.put("z3_korf_kever_2", new net.minecraft.nbt.CompoundTag());
        AmbachtData.tag(p).put("korven", k);
        AmbachtData.voegToe(p, "plekken", "z1_pluk_kamp");
        long xp = AmbachtData.xp(p, Ambacht.VERZAMELEN);
        helper.assertTrue(Imkeren.allesWeg(p) == 2, "two hives taken back");
        helper.assertTrue(AmbachtData.tag(p).getCompoundOrEmpty("korven").isEmpty(), "no hives left");
        helper.assertTrue(AmbachtData.xp(p, Ambacht.VERZAMELEN) == xp && AmbachtData.heeft(p, "plekken", "z1_pluk_kamp"), "the rest stays");
        helper.assertTrue(Imkeren.allesWeg(p) == 0, "again: none");
        // 1.10.21: one hive by its id, the others stay (and their zone count drops)
        net.minecraft.nbt.CompoundTag k2 = new net.minecraft.nbt.CompoundTag();
        k2.put("z3_korf_raatweide_0", new net.minecraft.nbt.CompoundTag());
        k2.put("z3_korf_raatweide_1", new net.minecraft.nbt.CompoundTag());
        k2.put("z3_korf_kever_2", new net.minecraft.nbt.CompoundTag());
        AmbachtData.tag(p).put("korven", k2);
        helper.assertTrue(Imkeren.inZone(p, 3) == 3, "three zone-3 hives");
        helper.assertTrue(Imkeren.eenWeg(p, "z3_korf_raatweide_1") && !Imkeren.eenWeg(p, "z3_korf_raatweide_1"), "one hive taken back, once");
        helper.assertTrue(Imkeren.ids(p).equals(java.util.List.of("z3_korf_kever_2", "z3_korf_raatweide_0")) && Imkeren.inZone(p, 3) == 2,
                "the other two stay: " + Imkeren.ids(p));
        helper.succeed();
    }

    private static net.minecraft.nbt.CompoundTag groepTag(Groep g, int spots) {
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        c.putLong("anker", g.anker().asLong());
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < spots; i++) {
            net.minecraft.nbt.CompoundTag s = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.ListTag pos = new net.minecraft.nbt.ListTag();
            pos.add(net.minecraft.nbt.LongTag.valueOf(g.anker().offset(i, 0, 0).asLong()));
            s.put("pos", pos);
            s.put("bloem", new net.minecraft.nbt.ListTag());
            l.add(s);
        }
        c.put("spots", l);
        return c;
    }

    static Spot testSpot(GameTestHelper helper, NodeSoort a, int tier) {
        return testSpot(helper, a, tier, "t_" + a.id);
    }

    /**
     * A test spot with its own group id (1.10.4: the state of a spot is shared and lives in {@link Grondstoffen}, so every test
     * uses its own id and starts clean).
     */
    static Spot testSpot(GameTestHelper helper, NodeSoort a, int tier, String id) {
        Groep g = new Groep(id, 1, "x", a, tier, 1, 5, Zoek.GROND, a == NodeSoort.KORF ? "bloesemhoning" : "");
        Spot s = new Spot(g.id() + "_0", g, List.of(helper.absolutePos(new BlockPos(2, 2, 2)), helper.absolutePos(new BlockPos(8, 2, 8))),
                List.of(helper.absolutePos(new BlockPos(3, 2, 3))));
        Grondstoffen.get(helper.getLevel().getServer()).wis(s.id());
        return s;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void oogstPerSpeler(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        AmbachtData.reset(a);
        AmbachtData.reset(b);
        Verzamelen.vergeet(a.getUUID());
        Spot s = testSpot(helper, NodeSoort.KRUID, 1, "t_kruid_oogst");
        String m = Verzamelen.doeOogst(a, sl, s, 2);
        helper.assertTrue(m.isEmpty(), "harvest: " + m);
        // 1.10.4: a real, shared block: empty for everyone (the move to another position: VadsrijkGrondstofGameTests)
        helper.assertTrue(!Verzamelen.beschikbaar(a, s) && !Verzamelen.beschikbaar(b, s), "empty for everyone");
        int n = 0, q = -1;
        for (int i = 0; i < a.getInventory().getContainerSize(); i++) {
            ItemStack st = a.getInventory().getItem(i);
            if (Materialen.van(st) != null) {
                n += st.getCount();
                q = Materialen.kwaliteit(st);
            }
        }
        helper.assertTrue(n >= 1 && q == 2, "got " + n + " extra vadsig material (q " + q + ")");
        helper.assertTrue(AmbachtData.xp(a, Ambacht.VERZAMELEN) >= 30 && AmbachtData.heeft(a, "plekken", "t_kruid_oogst"), "Verzamelen xp + book");
        helper.assertTrue(Verzamelen.doeOogst(a, sl, s, 0).equals("vadsrijk.ambacht.oogst.leeg"), "not twice");
        // too low for tier 3
        Verzamelen.vergeet(b.getUUID());
        helper.assertTrue(Verzamelen.doeOogst(b, sl, testSpot(helper, NodeSoort.ERTS, 3, "t_erts_poort"), 0).equals("vadsrijk.ambacht.oogst.te_laag"),
                "tier gate");
        // a rare spot gives 3x
        Verzamelen.vergeet(b.getUUID());
        AmbachtData.zetLevel(b, Ambacht.VERZAMELEN, 1);
        Spot h = testSpot(helper, NodeSoort.HOUT, 1, "t_hout_zeldzaam");
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
        Spot k = testSpot(helper, NodeSoort.KORF, 3);
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
        int zk = k.groep().zone();
        helper.assertTrue(Imkeren.inZone(p, zk) == 1 && Imkeren.inZone(p, zk + 1) == 0, "1.10.19: hives are counted per zone");
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
        AmbachtData.zetLevel(p, Ambacht.KOKEN, 9);   // (1.10.4: cutting is Koken's)
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
        // 1.10.4: Maatwerk = jewellery (Episch, 2 chosen, +1 socket, no set, tradeable)
        ItemStack s = Maatwerk.maak(Maatwerk.SOORTEN.get("ring"), 3, List.of(Stat.KRITIEK, Stat.KRITIEKSCHADE), 100, "test", null, p.getRandom());
        Uitrusting u = Rollen.van(s);
        helper.assertTrue(u != null && u.zeldzaamheid() == Zeldzaamheid.EPISCH && !u.gebonden() && u.level() == 45, "Episch, tradeable, lv 45");
        helper.assertTrue(u.bonussen().size() == Zeldzaamheid.EPISCH.maxBonus, "4 bonuses: " + u.bonussen());
        helper.assertTrue(u.bonussen().stream().anyMatch(b -> b.stat() == Stat.KRITIEK) && u.bonussen().stream().anyMatch(b -> b.stat() == Stat.KRITIEKSCHADE),
                "the chosen stats are on it");
        helper.assertTrue(!u.sokjes().isEmpty(), "+1 socket guaranteed");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets.setVan(u.type()) == null, "no set bonus");
        // Meesterwerk jewellery: Legendarisch, bound, 3 chosen + a property, above the zone-3 drop
        ItemStack r = Maatwerk.maak(Maatwerk.SOORTEN.get("ring"), 3, List.of(Stat.KRITIEK, Stat.KRITIEKSCHADE, Stat.AANVALSSNELHEID), 80, "test",
                Maatwerk.Eigenschap.BAZEN, p.getRandom());
        Uitrusting ru = Rollen.van(r);
        helper.assertTrue(ru.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH && ru.gebonden() && Maatwerk.info(r).eigenschap().equals("bazen"), "Meesterwerk");
        double ref = Maatwerk.buitReferentie(3, nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING, 45, Zeldzaamheid.LEGENDARISCH);
        helper.assertTrue(ref > 0 && Maatwerk.kracht(ru) > ref, "above the zone-3 drop: " + Maatwerk.kracht(ru) + " > " + ref);
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
