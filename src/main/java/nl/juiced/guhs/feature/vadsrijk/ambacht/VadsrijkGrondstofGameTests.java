package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Zoek;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.4 "Grondstoffen zie je nu echt": the real, shared gathering blocks ({@link Grondstoffen}, {@link GrondstofBlok}). Run:
 * {@code runGameTestServer -Pgt=VadsrijkGrondstof}.
 */
public class VadsrijkGrondstofGameTests {
    private static final String BATCH = "vadsrijk_grondstof";

    /** A flat 12x12 field: {@code vloer} at y 1, air above. */
    private static void veld(GameTestHelper helper, Block vloer) {
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), vloer.defaultBlockState());
                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** A spot of kind {@code s} (tier {@code tier}, respawn 60-120 s) at the given relative positions; its state starts clean. */
    private static Spot spot(GameTestHelper helper, String id, NodeSoort s, int tier, BlockPos... rel) {
        Groep g = new Groep(id, 1, "x", s, tier, 1, 5, s.zoek, "");
        List<BlockPos> pos = new java.util.ArrayList<>();
        for (BlockPos r : rel) {
            pos.add(helper.absolutePos(r));
        }
        Spot spot = new Spot(id + "_0", g, List.copyOf(pos), List.of());
        Grondstoffen.get(helper.getLevel().getServer()).wis(spot.id());
        return spot;
    }

    private static Grondstoffen gs(GameTestHelper helper) {
        return Grondstoffen.get(helper.getLevel().getServer());
    }

    private static int[] budget() {
        return new int[] {Grondstoffen.PLAATS_PER_RONDE};
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void plaatsen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        // ore: the floor rock under the spot becomes the vein; the rock is remembered
        veld(helper, Blocks.STONE);
        Spot erts = spot(helper, "t_gs_erts", NodeSoort.ERTS, 2, new BlockPos(2, 2, 2));
        g.bijwerk(sl, erts, budget());
        BlockPos e = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState st = sl.getBlockState(e);
        helper.assertTrue(Grondstoffen.isNode(st, NodeSoort.ERTS) && st.getValue(GrondstofBlok.VOL) && st.getValue(GrondstofBlok.TIER) == 2,
                "ore vein in the floor, full, tier 2: " + st);
        helper.assertTrue(e.equals(g.node(erts).pos()) && g.node(erts).oud().is(Blocks.STONE), "position + original stone stored");
        // herbs: on the grass, the short grass that stood there is remembered
        veld(helper, Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.SHORT_GRASS.defaultBlockState());
        Spot kruid = spot(helper, "t_gs_kruid", NodeSoort.KRUID, 1, new BlockPos(5, 2, 5));
        g.bijwerk(sl, kruid, budget());
        helper.assertTrue(Grondstoffen.isNode(sl.getBlockState(helper.absolutePos(new BlockPos(5, 2, 5))), NodeSoort.KRUID), "a bush on the grass");
        helper.assertTrue(g.node(kruid).oud().is(Blocks.SHORT_GRASS), "original short grass stored: " + g.node(kruid).oud());
        // fishing: waterlogged in the water surface
        for (int x = 3; x < 9; x++) {
            for (int z = 3; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
            }
        }
        Spot vis = spot(helper, "t_gs_vis", NodeSoort.VIS, 1, new BlockPos(6, 1, 6));
        g.bijwerk(sl, vis, budget());
        BlockState v = sl.getBlockState(helper.absolutePos(new BlockPos(6, 1, 6)));
        helper.assertTrue(Grondstoffen.isNode(v, NodeSoort.VIS) && v.getValue(GrondstofBlok.WATERLOGGED) && sl.getFluidState(helper.absolutePos(new BlockPos(6, 1, 6)))
                .isSource(), "fishing spot waterlogged in the pond");
        helper.assertTrue(g.node(vis).oud().is(Blocks.WATER), "original water stored");
        // the budget: none left = nothing placed this round
        Spot later = spot(helper, "t_gs_later", NodeSoort.KRUID, 1, new BlockPos(1, 2, 10));
        g.bijwerk(sl, later, new int[] {0});
        helper.assertTrue(g.node(later).pos() == null && sl.getBlockState(helper.absolutePos(new BlockPos(1, 2, 10))).isAir(), "no budget: waits");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void oogstLeegEnTerug(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.STONE);
        Spot s = spot(helper, "t_gs_ronde", NodeSoort.ERTS, 1, new BlockPos(2, 2, 2), new BlockPos(8, 2, 8));
        g.bijwerk(sl, s, budget());
        BlockPos waar = g.node(s).pos();
        helper.assertTrue(waar != null, "placed");
        ServerPlayer a = GuhMockPlayer.of(helper);
        AmbachtData.reset(a);
        long nu = sl.getGameTime();
        helper.assertTrue(Verzamelen.doeOogst(a, sl, s, 1).isEmpty(), "harvest");
        // empty for the group's time (60-120 s), the block shows it
        helper.assertTrue(!sl.getBlockState(waar).getValue(GrondstofBlok.VOL) && !g.vol(sl, s), "empty block after the harvest");
        long klaar = g.node(s).klaar() - nu;
        helper.assertTrue(klaar >= 20L * AmbachtPlekken.LEEG_MIN && klaar <= 20L * AmbachtPlekken.LEEG_MAX, "empty 60-120 s: " + klaar);
        g.bijwerk(sl, s, budget());
        helper.assertTrue(waar.equals(g.node(s).pos()) && !sl.getBlockState(waar).getValue(GrondstofBlok.VOL), "still empty before its time");
        // time travel: the timer has run out -> full again, moved to the other position, the old one is rock again
        g.zetKlaar(s, nu - 1);
        g.bijwerk(sl, s, budget());
        BlockPos nieuw = g.node(s).pos();
        helper.assertTrue(nieuw != null && !nieuw.equals(waar), "moved to its other position: " + nieuw);
        helper.assertTrue(sl.getBlockState(nieuw).getValue(GrondstofBlok.VOL), "full again");
        helper.assertTrue(sl.getBlockState(waar).is(Blocks.STONE), "the old position got its stone back: " + sl.getBlockState(waar));
        helper.assertTrue(g.node(s).oud().is(Blocks.STONE), "the new original is stored");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void gedeeld(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.GRASS_BLOCK);
        Spot s = spot(helper, "t_gs_samen", NodeSoort.KRUID, 1, new BlockPos(3, 2, 3));
        g.bijwerk(sl, s, budget());
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        AmbachtData.reset(a);
        AmbachtData.reset(b);
        helper.assertTrue(Verzamelen.beschikbaar(a, s) && Verzamelen.beschikbaar(b, s), "full for both");
        helper.assertTrue(Verzamelen.positie(a, s).equals(Verzamelen.positie(b, s)), "the same block for both");
        helper.assertTrue(Verzamelen.doeOogst(a, sl, s, 0).isEmpty(), "A harvests");
        helper.assertTrue(!Verzamelen.beschikbaar(b, s), "B sees it empty");
        helper.assertTrue(Verzamelen.doeOogst(b, sl, s, 0).equals("vadsrijk.ambacht.oogst.leeg"), "B cannot harvest it");
        helper.assertTrue(!sl.getBlockState(g.node(s).pos()).getValue(GrondstofBlok.VOL), "the world block is empty for everyone");
        // a rare spot is rare for everyone
        Verzamelen.maakZeldzaam(a, s);
        helper.assertTrue(Verzamelen.zeldzaam(b, s) && Verzamelen.beschikbaar(b, s), "rare + full for B too");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void levelPoort(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.STONE);
        Spot s = spot(helper, "t_gs_poort", NodeSoort.ERTS, 3, new BlockPos(4, 2, 4));
        g.bijwerk(sl, s, budget());
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, s, 0).equals("vadsrijk.ambacht.oogst.te_laag"), "level 1 at a T3 vein: too low");
        helper.assertTrue(g.vol(sl, s) && sl.getBlockState(g.node(s).pos()).getValue(GrondstofBlok.VOL), "still full");
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, AmbachtXp.tierLevel(3));
        helper.assertTrue(Verzamelen.doeOogst(p, sl, s, 0).isEmpty(), "at the T3 level: harvested");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void herladen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.STONE);
        Spot s = spot(helper, "t_gs_herlaad", NodeSoort.ERTS, 1, new BlockPos(5, 2, 5));
        g.bijwerk(sl, s, budget());
        ServerPlayer p = GuhMockPlayer.of(helper);
        AmbachtData.reset(p);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, s, 0).isEmpty(), "harvest");
        // a restart: the data goes through save + load, the block stays in its chunk (the group must be a known one: a node of a
        // group that no longer exists is restored on load)
        Grondstoffen r;
        AmbachtPlekken.GROEPEN.add(s.groep());
        try {
            r = Grondstoffen.rondje(g);
        } finally {
            AmbachtPlekken.GROEPEN.remove(s.groep());
        }
        Grondstoffen.Node n = r.bekend(s.id()), o = g.node(s);
        helper.assertTrue(n != null && o.pos().equals(n.pos()) && n.oud().is(Blocks.STONE) && n.klaar() == o.klaar() && n.zeldzaam() == o.zeldzaam(),
                "the state survives a save/load");
        helper.assertTrue(Grondstoffen.isNode(sl.getBlockState(n.pos()), NodeSoort.ERTS) && !sl.getBlockState(n.pos()).getValue(GrondstofBlok.VOL),
                "the empty block is still there");
        r.bijwerk(sl, s, budget());
        helper.assertTrue(!sl.getBlockState(n.pos()).getValue(GrondstofBlok.VOL), "loaded again before its time: still empty");
        // loaded again after its time: full (it has only one position, so it stays)
        r.zetKlaar(s, sl.getGameTime() - 1);
        r.bijwerk(sl, s, budget());
        helper.assertTrue(sl.getBlockState(n.pos()).getValue(GrondstofBlok.VOL), "loaded again after its time: full from the saved timer");
        // without its group (removed from the game): the node becomes a "wees" and its stone comes back once its chunk is loaded
        Grondstoffen w = Grondstoffen.rondje(r);
        helper.assertTrue(w.bekend(s.id()) == null && w.wezen() == r.wezen() + 1, "unknown group: an orphan to restore");   // (r also holds other tests' nodes)
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void nooitBouwsels(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.GRASS_BLOCK);
        // position 1: a chest (block entity), position 2: on planks (a build), position 3: free grass
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST.defaultBlockState());
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.OAK_PLANKS.defaultBlockState());
        Spot s = spot(helper, "t_gs_bouw", NodeSoort.KRUID, 1, new BlockPos(2, 2, 2), new BlockPos(6, 2, 6), new BlockPos(9, 2, 9));
        g.bijwerk(sl, s, budget());
        helper.assertTrue(sl.getBlockState(helper.absolutePos(new BlockPos(2, 2, 2))).is(Blocks.CHEST), "the chest is untouched");
        helper.assertTrue(sl.getBlockState(helper.absolutePos(new BlockPos(6, 2, 6))).isAir(), "nothing on the planks");
        helper.assertTrue(helper.absolutePos(new BlockPos(9, 2, 9)).equals(g.node(s).pos()), "placed on the free grass: " + g.node(s).pos());
        // ore never replaces a block entity in the floor
        veld(helper, Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.FURNACE.defaultBlockState());
        Spot e = spot(helper, "t_gs_oven", NodeSoort.ERTS, 1, new BlockPos(3, 2, 3));
        g.bijwerk(sl, e, budget());
        helper.assertTrue(g.node(e).pos() == null && sl.getBlockState(helper.absolutePos(new BlockPos(3, 1, 3))).is(Blocks.FURNACE),
                "no vein in a furnace, waits");
        // somebody builds over a node: it is forgotten (the build stays) and comes back at a free position
        veld(helper, Blocks.GRASS_BLOCK);
        Spot b = spot(helper, "t_gs_over", NodeSoort.HOUT, 1, new BlockPos(4, 2, 4), new BlockPos(8, 2, 4));
        g.bijwerk(sl, b, budget());
        BlockPos eerst = g.node(b).pos();
        helper.assertTrue(eerst != null && Grondstoffen.isNode(sl.getBlockState(eerst), NodeSoort.HOUT), "stump placed");
        sl.setBlock(eerst, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        g.bijwerk(sl, b, budget());
        BlockPos daarna = g.node(b).pos();
        helper.assertTrue(sl.getBlockState(eerst).is(Blocks.OAK_PLANKS), "the build stays");
        helper.assertTrue(daarna != null && !daarna.equals(eerst) && Grondstoffen.isNode(sl.getBlockState(daarna), NodeSoort.HOUT), "the stump moved: " + daarna);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void verplaatsenHerstelt(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Grondstoffen g = gs(helper);
        veld(helper, Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.FERN.defaultBlockState());
        Spot s = spot(helper, "t_gs_weg", NodeSoort.KRUID, 2, new BlockPos(3, 2, 3));
        g.bijwerk(sl, s, budget());
        BlockPos oud = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.assertTrue(oud.equals(g.node(s).pos()) && g.node(s).oud().is(Blocks.FERN), "placed over the fern");
        // the group was scanned again (its anchor moved): the same spot id, other positions
        Spot opnieuw = new Spot(s.id(), s.groep(), List.of(helper.absolutePos(new BlockPos(8, 2, 8))), List.of());
        g.bijwerk(sl, opnieuw, budget());
        helper.assertTrue(sl.getBlockState(oud).is(Blocks.FERN), "the fern is back: " + sl.getBlockState(oud));
        helper.assertTrue(helper.absolutePos(new BlockPos(8, 2, 8)).equals(g.node(opnieuw).pos())
                && Grondstoffen.isNode(sl.getBlockState(helper.absolutePos(new BlockPos(8, 2, 8))), NodeSoort.KRUID), "and the bush stands at its new place");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void blokkenBestaan(GameTestHelper helper) {
        for (NodeSoort s : NodeSoort.values()) {
            GrondstofBlok b = Grondstoffen.blok(s);
            helper.assertTrue(s == NodeSoort.KORF ? b == null : b != null && b.soort == s, "a block per kind but hives: " + s);
            if (b != null) {
                helper.assertTrue(b.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO) < 0 && b.getExplosionResistance() >= 3600000f,
                        s + ": unbreakable, explosion proof");
            }
        }
        helper.assertTrue(Grondstoffen.staat(NodeSoort.DUIK, 4, true).getValue(GrondstofBlok.WATERLOGGED)
                && !Grondstoffen.staat(NodeSoort.HOUT, 1, true).getValue(GrondstofBlok.WATERLOGGED), "only water spots are waterlogged");
        helper.succeed();
    }
}
