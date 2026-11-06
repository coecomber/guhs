package nl.juiced.guhs.feature.vadsrijk.zone5.binnen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-Z5BINNEN (batch vadsrijk_z5licht): the Hoogvads light blocks exist with names and give light; the candle rail joins its
 * neighbours (and a chandelier) when placed, and lets go again when a neighbour is broken; hanging pieces have no collision.
 */
public class Z5LichtGameTests {
    private static final String B = "vadsrijk_z5licht";

    @GuhTest(template = "empty", batch = B)
    public static void blokken(GameTestHelper helper) {
        Language t = Language.getInstance();
        helper.assertTrue(Z5Licht.PER_ID.size() == 7, "7 light blocks: " + Z5Licht.PER_ID.size());
        for (String id : Z5Licht.PER_ID.keySet()) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("guhs", id));
            helper.assertTrue(b != Blocks.AIR && b == Z5Licht.blok(id), "block " + id + " is registered");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("guhs", id)), "item " + id);
            helper.assertTrue(t.has("block.guhs." + id), "name of " + id);
            if (!id.equals("gouden_kroonlijst_muur")) {
                helper.assertTrue(b.defaultBlockState().getLightEmission() > 0, id + " gives light");
            }
        }
        BlockPos p = new BlockPos(1, 3, 1);
        helper.setBlock(p, Z5Licht.blok("kroonluchter"));
        BlockState s = helper.getBlockState(p);
        helper.assertTrue(s.getCollisionShape(helper.getLevel(), helper.absolutePos(p)).isEmpty(), "a chandelier has no collision");
        helper.assertTrue(s.getLightEmission() == 15, "a chandelier gives light 15");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void kaarsenrandVerbindt(GameTestHelper helper) {
        Block rand = Z5Licht.blok("gouden_kaarsenrand");
        BlockPos a = new BlockPos(1, 3, 1), b = a.east(), k = a.west();
        helper.setBlock(a, rand);
        helper.setBlock(k, Z5Licht.blok("kroonluchter"));
        helper.setBlock(b, rand);
        // (helper.setBlock places with flag 3: the neighbours get a shape update, like in survival)
        BlockState sa = helper.getBlockState(a);
        helper.assertTrue(sa.getValue(Z5Licht.Kaarsenrand.EAST), "the rail joins the rail to the east");
        helper.assertTrue(sa.getValue(Z5Licht.Kaarsenrand.WEST), "and the chandelier to the west");
        helper.setBlock(b, Blocks.AIR);
        helper.assertTrue(!helper.getBlockState(a).getValue(Z5Licht.Kaarsenrand.EAST), "and lets go when it is broken");
        helper.assertTrue(helper.getBlockState(a).getLightEmission() == 13, "a rail with a candle gives light 13");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void wandluchter(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 2, 1);
        helper.setBlock(p, Z5Licht.blok("gouden_wandluchter").defaultBlockState().setValue(Z5Licht.Wandluchter.FACING, Direction.EAST));
        BlockState s = helper.getBlockState(p);
        helper.assertTrue(s.getCollisionShape(helper.getLevel(), helper.absolutePos(p)).isEmpty(), "a sconce has no collision");
        helper.assertTrue(s.getLightEmission() == 14, "a sconce gives light 14");
        helper.succeed();
    }
}
