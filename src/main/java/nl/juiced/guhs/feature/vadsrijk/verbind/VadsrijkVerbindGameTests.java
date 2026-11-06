package nl.juiced.guhs.feature.vadsrijk.verbind;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-HEKKEN: blocks placed by code connect (Verbind). A fence line placed with flag 16 (no shape updates at all, like
 * bazen.Herstel) and a wall line placed with flag 2 are fully connected, the ends included. Run: -Pgt=VadsrijkVerbind.
 */
public class VadsrijkVerbindGameTests {
    private static final String BATCH = "vadsrijk_verbind";

    @GuhTest(template = "empty", batch = BATCH)
    public static void hekLijnVerbonden(GameTestHelper helper) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int x = 1; x <= 4; x++) {
            Verbind.zet(helper.getLevel(), helper.absolutePos(new BlockPos(x, 1, 2)), Blocks.SPRUCE_FENCE.defaultBlockState(), flags);
        }
        for (int x = 1; x <= 4; x++) {
            BlockState s = helper.getBlockState(new BlockPos(x, 1, 2));
            helper.assertTrue(s.getValue(CrossCollisionBlock.EAST) == (x < 4), "fence " + x + " east: " + s);
            helper.assertTrue(s.getValue(CrossCollisionBlock.WEST) == (x > 1), "fence " + x + " west: " + s);
        }
        // a pane line placed with flag 2 (vanilla only connects the neighbours, not the placed block itself)
        for (int z = 4; z <= 6; z++) {
            BlockPos p = helper.absolutePos(new BlockPos(1, 1, z));
            helper.getLevel().setBlock(p, Verbind.vorm(helper.getLevel(), p, Blocks.IRON_BARS.defaultBlockState()), Block.UPDATE_CLIENTS);
        }
        BlockState midden = helper.getBlockState(new BlockPos(1, 1, 5));
        helper.assertTrue(midden.getValue(CrossCollisionBlock.NORTH) && midden.getValue(CrossCollisionBlock.SOUTH), "bars: " + midden);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void muurEnGebied(GameTestHelper helper) {
        // raw default states (as an old placement would leave them), then one gebied() pass
        for (int x = 1; x <= 3; x++) {
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 1, 1)), Blocks.COBBLESTONE_WALL.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        int n = Verbind.gebied(helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(4, 2, 2)));
        helper.assertTrue(n >= 3, "gebied changed " + n);
        BlockState m = helper.getBlockState(new BlockPos(2, 1, 1));
        helper.assertTrue(m.getValue(WallBlock.EAST) == WallSide.LOW && m.getValue(WallBlock.WEST) == WallSide.LOW
                && !m.getValue(WallBlock.UP), "middle wall: " + m);
        helper.succeed();
    }
}
