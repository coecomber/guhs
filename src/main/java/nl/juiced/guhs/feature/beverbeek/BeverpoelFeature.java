package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * guhs:beverbeek_poel (after the brooks, rare): a wild beaver pool on a stretch of brook (a deep one when there is). The pool is widened round
 * the brook at the brook's own water level; wherever the land around it is lower (that is: downstream) a dam of gnawed
 * logs, mud and twigs holds the water, and in the pool stands a lodge (a mound of logs and mud with a dry room inside,
 * at the water line, and leaf litter on top). No beavers: they only live at a ranger post.
 * Everything stays within the chunk and the ring of blocks around it (radius at most {@link #STRAAL} + 1 from a point
 * at least 4 blocks inside the chunk).
 */
public class BeverpoelFeature extends Feature<NoneFeatureConfiguration> {
    static final int STRAAL = 5;

    public BeverpoelFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        long seed = level.getSeed();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        // a deep bit of brook near the middle of the chunk
        BlockPos midden = null;
        int beste = 0;
        for (int x = x0 + 4; x <= x0 + 11; x++) {
            for (int z = z0 + 4; z <= z0 + 11; z++) {
                int d = Beek.diepte(seed, x, z);
                if (d > beste) {
                    BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                    if (level.getFluidState(top).is(Fluids.WATER) && level.getBiome(top).is(BeverbeekFeature.BIOME)) {
                        midden = top;
                        beste = d;
                    }
                }
            }
        }
        if (midden == null) {
            return false;
        }
        int wy = midden.getY();
        double rx = STRAAL - rnd.nextInt(2), rz = STRAAL - rnd.nextInt(2);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // 1. the pool: cells inside the ellipse whose ground is about at the water level
        for (int dx = -STRAAL - 1; dx <= STRAAL + 1; dx++) {
            for (int dz = -STRAAL - 1; dz <= STRAAL + 1; dz++) {
                double e = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
                int x = midden.getX() + dx, z = midden.getZ() + dz;
                int grond = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                if (e <= 1.0) {
                    if (grond > wy + 3) {
                        continue;                                // a bank too high to dig away: it stays an island
                    }
                    int bodem = e < 0.45 ? wy - 2 : wy - 1;
                    for (int y = Math.max(grond, wy) + 2; y > wy; y--) {
                        level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                    for (int y = wy; y >= bodem; y--) {
                        level.setBlock(p.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                        level.scheduleTick(p, Fluids.WATER, 0);
                    }
                    level.setBlock(p.set(x, bodem - 1, z), (rnd.nextInt(3) == 0 ? Blocks.CLAY : Blocks.MUD).defaultBlockState(), 2);
                } else if (e <= 1.7 && grond <= wy) {
                    // 2. the dam: the ring where the land is lower than the water
                    for (int y = grond + 1; y <= wy + 1; y++) {
                        level.setBlock(p.set(x, y, z), damBlok(rnd, y == wy + 1), 2);
                    }
                    if (rnd.nextInt(3) > 0) {
                        level.setBlock(p.set(x, wy + 2, z), strooisel(rnd), 2);
                    }
                }
            }
        }
        // 3. the lodge, a little off the middle
        int lx = midden.getX() + (rnd.nextBoolean() ? 2 : -2), lz = midden.getZ() + rnd.nextInt(3) - 1;
        burcht(level, rnd, new BlockPos(lx, wy, lz));
        return true;
    }

    private static BlockState damBlok(RandomSource rnd, boolean boven) {
        int r = rnd.nextInt(10);
        if (r < 4) {
            BlockState log = (rnd.nextInt(3) == 0 ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState();
            return log.setValue(RotatedPillarBlock.AXIS, rnd.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        if (r < 6) {
            return Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, rnd.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        return (boven ? Blocks.MUDDY_MANGROVE_ROOTS : r < 8 ? Blocks.MUD : Blocks.PACKED_MUD).defaultBlockState();
    }

    static BlockState strooisel(RandomSource rnd) {
        if (rnd.nextInt(4) == 0) {
            return Blocks.DEAD_BUSH.defaultBlockState();
        }
        return Blocks.LEAF_LITTER.defaultBlockState().setValue(SegmentableBlock.AMOUNT, 1 + rnd.nextInt(4))
                .setValue(LeafLitterBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd));
    }

    /** A beaver lodge: a mound of logs and mud, radius 2.5, rising 3 above the water, with a dry room at the water line. */
    static void burcht(WorldGenLevel level, RandomSource rnd, BlockPos voet) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 2.7) {
                    continue;
                }
                int hoog = (int) Math.round(3.2 - d * 1.05);
                for (int dy = -2; dy <= hoog; dy++) {
                    p.set(voet.getX() + dx, voet.getY() + dy, voet.getZ() + dz);
                    boolean kamer = d < 1.3 && dy >= 1 && dy <= 1;
                    level.setBlock(p, kamer ? Blocks.AIR.defaultBlockState() : damBlok(rnd, false), 2);
                }
                if (hoog >= 0 && rnd.nextInt(3) > 0) {
                    level.setBlock(p.set(voet.getX() + dx, voet.getY() + hoog + 1, voet.getZ() + dz), strooisel(rnd), 2);
                }
            }
        }
        // the underwater way in (beavers dive into their lodge)
        level.setBlock(p.set(voet.getX() + 2, voet.getY() - 1, voet.getZ()), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(p.set(voet.getX() + 1, voet.getY() - 1, voet.getZ()), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(p.set(voet.getX(), voet.getY(), voet.getZ()), Blocks.WATER.defaultBlockState(), 2);
    }
}
