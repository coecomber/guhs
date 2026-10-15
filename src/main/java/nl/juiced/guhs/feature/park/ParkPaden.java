package nl.juiced.guhs.feature.park;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 1.4.2 (Het Knabbelpark): the park walks follow the hills block by block (terrain_matching), so on a steep bit two path
 * blocks next to each other can differ 2 or more: a step you cannot walk up. {@link #effen} (from
 * {@link ParkStructure#afterPlace}, per chunk) turns every such step into 1-block steps: the lower of two path blocks goes
 * one up (a walk next to a rigid piece goes towards the piece's own floor, up or down, because the piece itself never
 * moves), again until nothing changes. Only path blocks ({@link #isPad}) of the walks in this chunk are changed; the blocks
 * in neighbouring chunks are only read (their own chunk does the same when its turn comes).
 */
public final class ParkPaden {
    /** At most this many passes per chunk; a column moves at most {@link #MAX_VERSCHUIF} blocks in all. */
    private static final int PASSES = 8, MAX_VERSCHUIF = 4;
    /** How far up and down a neighbouring path block is looked for. */
    private static final int ZOEK = 6;

    private ParkPaden() {
    }

    /**
     * The blocks a park walk is made of (tools/features/park_bouw.py PAD, BRUG; the rigid pieces use cherry planks nowhere,
     * the generator's self-check makes sure, so a roof is never taken for a walk).
     */
    public static boolean isPad(BlockState s) {
        return s.is(Blocks.DIRT_PATH) || s.is(Blocks.CHERRY_PLANKS);
    }

    /** Makes the walks of this chunk walkable; returns how many blocks moved (for the check command). */
    public static int effen(WorldGenLevel level, BoundingBox chunk, List<BoundingBox> paden, List<BoundingBox> vast) {
        if (paden.isEmpty()) {
            return 0;
        }
        int verschoven = 0;
        java.util.Map<Long, Integer> totaal = new java.util.HashMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int pass = 0; pass < PASSES; pass++) {
            boolean anders = false;
            for (BoundingBox b : paden) {
                int x0 = Math.max(b.minX(), chunk.minX()), x1 = Math.min(b.maxX(), chunk.maxX());
                int z0 = Math.max(b.minZ(), chunk.minZ()), z1 = Math.min(b.maxZ(), chunk.maxZ());
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        if (in(vast, x, z)) {
                            continue;   // (a rigid piece's own floor never moves)
                        }
                        int y = top(level, p, x, z, b.minY(), b.maxY());
                        if (y == Integer.MIN_VALUE) {
                            continue;
                        }
                        long key = BlockPos.asLong(x, 0, z);
                        if (totaal.getOrDefault(key, 0) >= MAX_VERSCHUIF) {
                            continue;
                        }
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            int nx = x + d.getStepX(), nz = z + d.getStepZ();
                            int ny = top(level, p, nx, nz, y - ZOEK, y + ZOEK);
                            if (ny == Integer.MIN_VALUE) {
                                continue;
                            }
                            boolean buurVast = in(vast, nx, nz);
                            if (ny - y >= 2) {
                                zet(level, p, x, y, z, 1);
                            } else if (buurVast && y - ny >= 2) {
                                zet(level, p, x, y, z, -1);
                            } else {
                                continue;
                            }
                            totaal.merge(key, 1, Integer::sum);
                            verschoven++;
                            anders = true;
                            break;
                        }
                    }
                }
            }
            if (!anders) {
                break;
            }
        }
        return verschoven;
    }

    private static boolean in(List<BoundingBox> boxes, int x, int z) {
        for (BoundingBox b : boxes) {
            if (x >= b.minX() && x <= b.maxX() && z >= b.minZ() && z <= b.maxZ()) {
                return true;
            }
        }
        return false;
    }

    /** The highest path block in this column between y0 and y1 (MIN_VALUE: none). */
    private static int top(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, int y0, int y1) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return Integer.MIN_VALUE;
        }
        for (int y = y1; y >= y0; y--) {
            if (isPad(level.getBlockState(p.set(x, y, z)))) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** The path block at (x, y, z) goes one up (dy 1) or one down (dy -1), with ground under it and room above it. */
    private static void zet(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int y, int z, int dy) {
        BlockState pad = level.getBlockState(p.set(x, y, z));
        if (dy > 0) {
            level.setBlock(p.set(x, y, z), pad.is(Blocks.CHERRY_PLANKS) ? pad : Blocks.DIRT.defaultBlockState(), 2);
            level.setBlock(p.set(x, y + 1, z), pad, 2);
        } else {
            level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(p.set(x, y - 1, z), pad, 2);
            if (level.getBlockState(p.set(x, y - 2, z)).isAir()) {
                level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
            }
        }
        int boven = y + dy + 1;
        for (int k = 0; k < 2; k++) {
            if (!level.getBlockState(p.set(x, boven + k, z)).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
