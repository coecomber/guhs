package nl.juiced.guhs.feature.bouwhulp.blokken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Behang;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Kant;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Lantaarnpaal;
import nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Luik;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.DakHalf;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.DakKwart;
import nl.juiced.guhs.feature.bouwhulp.blokken.VormBlokken.PaalBalk;
import nl.juiced.guhs.gametest.GuhTest;

/** F11: every building block has a name and an item; the shapes face the right way; posts, beams, roofs, wallpaper, fences and the lamppost join up. */
public class BouwBlokkenGameTests {
    private static final double E = 1e-6;

    private static AABB vorm(GameTestHelper helper, BlockPos p) {
        return helper.getBlockState(p).getShape(helper.getLevel(), helper.absolutePos(p)).bounds();
    }

    private static boolean is(double a, double b) {
        return Math.abs(a - b) < E;
    }

    @GuhTest(template = "empty")
    public static void allesHeeftNaamEnItem(GameTestHelper helper) {
        helper.assertTrue(BouwBlokken.PER_ID.size() == BouwLijst.ALLE.length, "registered " + BouwBlokken.PER_ID.size() + " of " + BouwLijst.ALLE.length);
        for (String id : BouwLijst.ALLE) {
            Block b = BouwBlokken.blok(id);
            helper.assertTrue(Language.getInstance().has(b.getDescriptionId()), "name of " + id);
            helper.assertTrue(b.asItem() != Items.AIR, "item of " + id);
        }
        helper.assertTrue(Language.getInstance().has("itemGroup.guhs.bouwen"), "name of the Bouwen tab");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void platenEnHoekjesDraaien(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, BouwBlokken.blok("eiken_vplaat").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        AABB a = vorm(helper, p);
        helper.assertTrue(is(a.minX, 0.5) && is(a.maxX, 1) && is(a.minZ, 0) && is(a.maxZ, 1), "vertical slab east = the east half, got " + a);
        helper.setBlock(p, BouwBlokken.blok("pleister_roze_hoekje").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        a = vorm(helper, p);
        helper.assertTrue(is(a.minX, 0.5) && is(a.minZ, 0.5) && is(a.maxY, 1), "corner south = the south-east quarter, got " + a);
        helper.setBlock(p, BouwBlokken.blok("guh_luik_roze").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        a = vorm(helper, p);
        helper.assertTrue(is(a.minZ, 13 / 16.0) && is(a.maxX, 1), "shutter closed: against the wall at the south, got " + a);
        helper.setBlock(p, helper.getBlockState(p).setValue(Luik.OPEN, true).setValue(Luik.KANT, Kant.RECHTS));
        a = vorm(helper, p);
        helper.assertTrue(is(a.minX, 13 / 16.0) && is(a.minZ, 0), "shutter open on its east hinge, got " + a);
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void palenEnBalkenVerbinden(GameTestHelper helper) {
        PaalBalk balk = (PaalBalk) BouwBlokken.blok("eiken_balk");
        PaalBalk paal = (PaalBalk) BouwBlokken.blok("sparren_paal");
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
        helper.setBlock(a, balk.verbonden(helper.getLevel(), helper.absolutePos(a)));
        BlockState los = helper.getBlockState(a);
        helper.assertTrue(los.getValue(PaalBalk.ARM.get(Direction.UP)) && los.getValue(PaalBalk.ARM.get(Direction.DOWN))
                && !los.getValue(PaalBalk.ARM.get(Direction.EAST)), "a lone beam stands upright");
        helper.setBlock(b, balk.verbonden(helper.getLevel(), helper.absolutePos(b)));
        BlockState s = helper.getBlockState(a);
        helper.assertTrue(s.getValue(PaalBalk.ARM.get(Direction.EAST)), "beam joins the beam next to it");
        helper.assertTrue(helper.getBlockState(b).getValue(PaalBalk.ARM.get(Direction.WEST)), "and the other way round");
        helper.assertTrue(!s.getValue(PaalBalk.ARM.get(Direction.UP)), "two joined beams lie flat");
        BlockPos c = new BlockPos(3, 2, 3), muur = new BlockPos(4, 2, 3);
        helper.setBlock(muur, Blocks.STONE);
        helper.setBlock(c, paal.verbonden(helper.getLevel(), helper.absolutePos(c)));
        BlockState ps = helper.getBlockState(c);
        helper.assertTrue(!ps.getValue(PaalBalk.ARM.get(Direction.EAST)) && ps.getValue(PaalBalk.ARM.get(Direction.UP)),
                "a post does not reach into a wall and stays upright");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void dakdelenStapelen(GameTestHelper helper) {
        DakHalf half = (DakHalf) BouwBlokken.blok("eiken_dak_half");
        BlockPos laag = new BlockPos(1, 2, 2);
        helper.setBlock(laag, half.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        int d = half.deelVoor(helper.getLevel(), helper.absolutePos(laag.north()), Direction.NORTH);
        helper.assertTrue(d == 1, "the next part up the slope is part 1, got " + d);
        helper.assertTrue(is(vorm(helper, laag).maxY, 0.5), "part 0 of a half slope rises to half a block");
        DakKwart kwart = (DakKwart) BouwBlokken.blok("dakpan_roze_dak_kwart");
        helper.setBlock(laag, kwart.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(DakKwart.DEEL, 3));
        d = kwart.deelVoor(helper.getLevel(), helper.absolutePos(laag.north()), Direction.NORTH);
        helper.assertTrue(d == 0, "after the last quarter part comes part 0 again, got " + d);
        helper.assertTrue(is(vorm(helper, laag).maxY, 1), "the last quarter part reaches the top");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void behangKrijgtPlint(GameTestHelper helper) {
        BlockPos boven = new BlockPos(1, 3, 1), onder = new BlockPos(1, 2, 1);
        helper.setBlock(boven, BouwBlokken.blok("behang_bloemetjes_roze"));
        helper.setBlock(onder, BouwBlokken.blok("behang_guhtjes_mint"));
        helper.assertTrue(!helper.getBlockState(boven).getValue(Behang.PLINT), "wallpaper on wallpaper: no skirting");
        helper.assertTrue(helper.getBlockState(onder).getValue(Behang.PLINT), "the bottom row has a skirting");
        helper.setBlock(onder, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(boven).getValue(Behang.PLINT), "now the bottom row again: skirting");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void hekjesEnLantaarnpaal(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
        helper.setBlock(a, BouwBlokken.blok("guh_hekje_roze"));
        helper.setBlock(b, BouwBlokken.blok("guh_hekje_mint"));
        helper.assertTrue(helper.getBlockState(a).getValue(FenceBlock.EAST), "guh fences join each other");
        Lantaarnpaal lamp = (Lantaarnpaal) BouwBlokken.blok("guh_lantaarnpaal");
        BlockPos l = new BlockPos(3, 1, 3);
        lamp.zet(helper.getLevel(), helper.absolutePos(l));
        helper.assertTrue(helper.getBlockState(l.above()).is(lamp) && helper.getBlockState(l.above()).getValue(Lantaarnpaal.HALF) == DoubleBlockHalf.UPPER,
                "the lamp sits on the post");
        helper.assertTrue(helper.getBlockState(l.above()).getLightEmission() == 15 && helper.getBlockState(l).getLightEmission() == 0, "only the lamp shines");
        helper.setBlock(l, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(l.above()).isAir(), "no lamp without its post");
        helper.succeed();
    }
}
