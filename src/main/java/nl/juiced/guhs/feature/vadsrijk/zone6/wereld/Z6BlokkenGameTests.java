package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-Z6WERELD (batch vadsrijk_z6blokken): the Zijdeland blocks exist with names; the date-palm fence, the sandstone wall and the
 * mashrabiya pane connect to their neighbours; the brass lantern hangs and glows; the silk awning is a thin floating cloth.
 */
public class Z6BlokkenGameTests {
    private static final String B = "vadsrijk_z6blokken";

    @GuhTest(template = "empty", batch = B)
    public static void blokken(GameTestHelper helper) {
        Language t = Language.getInstance();
        helper.assertTrue(Z6Blokken.PER_ID.size() == 19, "19 Zijdeland blocks: " + Z6Blokken.PER_ID.size());
        for (String id : Z6Blokken.PER_ID.keySet()) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("guhs", id));
            helper.assertTrue(b != Blocks.AIR && b == Z6Blokken.blok(id), "block " + id + " is registered");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("guhs", id)), "item " + id);
            helper.assertTrue(t.has("block.guhs." + id), "name of " + id);
        }
        helper.assertTrue(Z6Blokken.blok("dadelpalm_hek").defaultBlockState().is(BlockTags.WOODEN_FENCES), "the fence is a wooden fence");
        helper.assertTrue(Z6Blokken.blok("rozenzandsteen_muur").defaultBlockState().is(BlockTags.WALLS), "the wall is a wall");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void verbinden(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1);
        // fence + fence
        helper.setBlock(a, Z6Blokken.blok("dadelpalm_hek"));
        helper.setBlock(a.east(), Z6Blokken.blok("dadelpalm_hek"));
        // pane + pane
        BlockPos p = new BlockPos(1, 2, 3);
        helper.setBlock(p, Z6Blokken.blok("mashrabiya_tralies"));
        helper.setBlock(p.east(), Z6Blokken.blok("mashrabiya_tralies"));
        // wall + wall
        BlockPos w = new BlockPos(1, 2, 5);
        helper.setBlock(w, Z6Blokken.blok("rozenzandsteen_muur"));
        helper.setBlock(w.east(), Z6Blokken.blok("rozenzandsteen_muur"));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(a).getValue(CrossCollisionBlock.EAST), "the fence connects east");
            helper.assertTrue(helper.getBlockState(p).getValue(CrossCollisionBlock.EAST), "the lattice connects east");
            helper.assertTrue(helper.getBlockState(w).getValue(WallBlock.EAST) != WallSide.NONE, "the wall connects east");
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = B)
    public static void lantaarnEnLuifel(GameTestHelper helper) {
        BlockPos dak = new BlockPos(1, 4, 1);
        helper.setBlock(dak, Z6Blokken.blok("rozenzandsteen"));
        helper.setBlock(dak.below(), Z6Blokken.blok("messing_lantaarn").defaultBlockState().setValue(LanternBlock.HANGING, true));
        BlockState l = helper.getBlockState(dak.below());
        helper.assertTrue(l.is(Z6Blokken.blok("messing_lantaarn")) && l.getValue(LanternBlock.HANGING), "the lantern hangs");
        helper.assertTrue(l.getLightEmission() == 15, "the lantern glows");
        BlockPos z = new BlockPos(3, 3, 3);
        helper.setBlock(z, Z6Blokken.blok("zijden_luifel"));
        BlockState s = helper.getBlockState(z);
        helper.assertTrue(s.is(Z6Blokken.blok("zijden_luifel")), "the awning floats (no support needed)");
        helper.assertTrue(s.getCollisionShape(helper.getLevel(), helper.absolutePos(z)).isEmpty(), "the awning has no collision");
        helper.assertTrue(s.getShape(helper.getLevel(), helper.absolutePos(z)).max(Direction.Axis.Y) <= 0.1, "the awning is thin");
        helper.succeed();
    }
}
