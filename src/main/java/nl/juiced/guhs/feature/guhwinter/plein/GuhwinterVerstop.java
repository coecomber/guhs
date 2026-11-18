package nl.juiced.guhs.feature.guhwinter.plein;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Ozovads' hiding spot down the valley south of the arrival (anchor {@code paard_verstop}): a nook behind a few rocks and two
 * little spruces, with a hay bale. Built on the ground as it is (outside the square: only cheap, breakable blocks).
 */
final class GuhwinterVerstop {
    private GuhwinterVerstop() {
    }

    private static BlockPos grond(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    static String bouw(ServerLevel level, BlockPos midden) {
        int n = 0;
        // rocks between the trail (north) and the spot
        int[][] rotsen = {{-1, -2, 2}, {0, -2, 2}, {1, -2, 1}, {-2, -1, 1}, {2, -2, 1}, {-1, -3, 1}};
        for (int[] r : rotsen) {
            BlockPos p = grond(level, midden.getX() + r[0], midden.getZ() + r[1]);
            for (int dy = 0; dy < r[2]; dy++) {
                BlockState s = (r[0] + dy) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
                level.setBlock(p.above(dy), s, 3);
                n++;
            }
        }
        // two little spruces at the sides
        for (int[] b : new int[][]{{-3, 0}, {3, -1}}) {
            BlockPos p = grond(level, midden.getX() + b[0], midden.getZ() + b[1]);
            BlockState stam = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState blad = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            for (int dy = 0; dy < 4; dy++) {
                level.setBlock(p.above(dy), stam, 3);
                n++;
            }
            for (int dy = 1; dy <= 5; dy++) {
                int r = dy <= 2 ? 2 : (dy <= 4 ? 1 : 0);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > r + (dy % 2) || (dx == 0 && dz == 0 && dy < 4)) {
                            continue;
                        }
                        BlockPos q = p.offset(dx, dy, dz);
                        if (level.getBlockState(q).isAir()) {
                            level.setBlock(q, blad, 3);
                            n++;
                        }
                    }
                }
            }
        }
        // a hay bale in the nook (where he was nibbling)
        BlockPos hooi = grond(level, midden.getX() + 1, midden.getZ() + 1);
        level.setBlock(hooi, Blocks.HAY_BLOCK.defaultBlockState(), 3);
        n++;
        return "Ozovads' hiding spot at " + grond(level, midden.getX(), midden.getZ()).toShortString() + ": " + n + " blocks (rocks, two spruces, hay)";
    }
}
