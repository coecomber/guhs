package nl.juiced.guhs.feature.vadsrijk.verbind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * U-HEKKEN: blocks placed by code (puzzles, halls, camp and boss pieces) with their connection states, so fences, walls,
 * panes and bars are lines and never loose posts. {@code setBlock} with a {@code defaultBlockState()} only connects the
 * NEIGHBOURS (and not with flag 16); {@link #vorm} gives the placed block its own connections.
 * The map tiles get the same treatment offline (tools/vadsrijk/verbind.py, rules in tools/vadsrijk/LEESMIJ_BOUWREGELS.md).
 */
public final class Verbind {
    private Verbind() {
    }

    /** Fences, panes, iron bars (CrossCollisionBlock), walls and fence gates: their look depends on the neighbours. */
    public static boolean verbindt(BlockState s) {
        Block b = s.getBlock();
        return b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof FenceGateBlock;
    }

    /** The state with its connections to the current neighbours (other blocks unchanged). */
    public static BlockState vorm(LevelAccessor l, BlockPos p, BlockState s) {
        return verbindt(s) ? Block.updateFromNeighbourShapes(s, l, p) : s;
    }

    /** Re-shapes the connecting blocks around p (after a change with flag 16, which skips the neighbour shapes). */
    public static void buren(Level l, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockPos q = p.relative(d);
            BlockState n = l.getBlockState(q);
            if (verbindt(n)) {
                BlockState m = Block.updateFromNeighbourShapes(n, l, q);
                if (m != n) {
                    l.setBlock(q, m, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /** setBlock with connections: the block itself and its connecting neighbours. */
    public static void zet(Level l, BlockPos p, BlockState s, int flags) {
        l.setBlock(p, vorm(l, p, s), flags);
        if ((flags & Block.UPDATE_KNOWN_SHAPE) != 0) {
            buren(l, p);
        }
    }

    /** Re-shapes every connecting block in the box (inclusive). Returns how many changed. */
    public static int gebied(Level l, BlockPos a, BlockPos b) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            BlockState s = l.getBlockState(p);
            if (verbindt(s)) {
                BlockState m = Block.updateFromNeighbourShapes(s, l, p);
                if (m != s) {
                    l.setBlock(p.immutable(), m, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    n++;
                }
            }
        }
        return n;
    }
}
