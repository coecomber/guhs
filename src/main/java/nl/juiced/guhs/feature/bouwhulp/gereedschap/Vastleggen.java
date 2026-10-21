package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * Capturing a blueprint with the vormstaf (design 2.4/2.6, ronde B11): two corners, or one click and it finds the edge of
 * the building itself (it stops at natural ground). The frame can be adjusted afterwards, then it is saved.
 * <p>
 * What is captured: every block in the frame; the empty spots only where they are inside the building (between a block
 * below and a block above in the same column), so a house keeps its empty rooms but placing it does not dig a hole in the
 * hill around it.
 */
public final class Vastleggen {
    public record Doos(BlockPos min, BlockPos max) {
        public static Doos van(BlockPos a, BlockPos b) {
            return new Doos(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                    new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
        }

        public int maat() {
            return Math.max(max.getX() - min.getX(), Math.max(max.getY() - min.getY(), max.getZ() - min.getZ())) + 1;
        }

        /** One side moved: kant 0..5 = west, east, down, up, north, south; positive grows. */
        public Doos schuif(int kant, int n) {
            int x0 = min.getX(), y0 = min.getY(), z0 = min.getZ(), x1 = max.getX(), y1 = max.getY(), z1 = max.getZ();
            switch (kant) {
                case 0 -> x0 = Math.min(x1, x0 - n);
                case 1 -> x1 = Math.max(x0, x1 + n);
                case 2 -> y0 = Math.min(y1, y0 - n);
                case 3 -> y1 = Math.max(y0, y1 + n);
                case 4 -> z0 = Math.min(z1, z0 - n);
                default -> z1 = Math.max(z0, z1 + n);
            }
            return new Doos(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1));
        }
    }

    /** Natural ground and plants (where the automatic frame stops). */
    public static boolean natuur(BlockState s) {
        return s.isAir() || s.getBlock() instanceof LiquidBlock || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.BASE_STONE_NETHER) || s.is(BlockTags.LEAVES) || s.is(BlockTags.SNOW) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.ICE) || s.is(Blocks.SNOW);
    }

    /** The automatic frame: everything not natural that touches the clicked block (also diagonally), at most {@code max} wide. */
    public static Doos auto(ServerLevel level, BlockPos start, int max) {
        BlockPos min = start, mx = start;
        Set<BlockPos> gezien = new HashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start);
        gezien.add(start);
        int stappen = 0;
        while (!rij.isEmpty() && stappen++ < 200_000) {
            BlockPos p = rij.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (gezien.contains(n) || Math.abs(n.getX() - start.getX()) >= max || Math.abs(n.getY() - start.getY()) >= max
                                || Math.abs(n.getZ() - start.getZ()) >= max) {
                            continue;
                        }
                        gezien.add(n);
                        if (natuur(level.getBlockState(n))) {
                            continue;
                        }
                        Doos d = Doos.van(new BlockPos(Math.min(min.getX(), n.getX()), Math.min(min.getY(), n.getY()), Math.min(min.getZ(), n.getZ())),
                                new BlockPos(Math.max(mx.getX(), n.getX()), Math.max(mx.getY(), n.getY()), Math.max(mx.getZ(), n.getZ())));
                        if (d.maat() > max) {
                            continue;
                        }
                        min = d.min();
                        mx = d.max();
                        rij.add(n);
                    }
                }
            }
        }
        return new Doos(min, mx);
    }

    /** The ontwerp of what stands in the frame now. */
    public static Ontwerp vang(ServerLevel level, Doos d) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockPos a = d.min(), b = d.max();
        for (int x = a.getX(); x <= b.getX(); x++) {
            for (int z = a.getZ(); z <= b.getZ(); z++) {
                int laag = Integer.MAX_VALUE, hoog = Integer.MIN_VALUE;
                for (int y = a.getY(); y <= b.getY(); y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        laag = Math.min(laag, y);
                        hoog = Math.max(hoog, y);
                    }
                }
                for (int y = a.getY(); y <= b.getY(); y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir()) {
                        m.put(p.subtract(a), s);
                    } else if (y > laag && y < hoog) {
                        m.put(p.subtract(a), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        return Ontwerp.van(m);
    }

    private Vastleggen() {
    }
}
