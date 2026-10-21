package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * guhs:beverbeek_beek (step local_modifications, once per chunk): carves the brooks of {@link Beek} into the columns of
 * this chunk that lie in the Beverbeek. The water sits one block below the banks (so it stays in its bed), one or two
 * deep on a bed of gravel, sand or clay; where the next column of the brook is lower, it flows over the edge.
 */
public class BeekFeature extends Feature<NoneFeatureConfiguration> {
    public BeekFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        long seed = level.getSeed();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        boolean iets = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int d = Beek.diepte(seed, x, z);
                if (d == 0) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                p.set(x, top, z);
                if (!level.getFluidState(p).isEmpty() || !level.getBiome(p).is(BeverbeekFeature.BIOME)) {
                    continue;
                }
                // the plants on the bank go, the top block opens
                for (int y = top + 2; y >= top; y--) {
                    p.setY(y);
                    BlockState s = level.getBlockState(p);
                    if (y == top || s.canBeReplaced() || s.is(BlockTags.FLOWERS)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int i = 1; i <= d; i++) {
                    p.setY(top - i);
                    level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                    level.scheduleTick(p, Fluids.WATER, 0);
                }
                p.setY(top - d - 1);
                if (!level.getBlockState(p).isAir()) {
                    int r = rnd.nextInt(10);
                    level.setBlock(p, (r < 5 ? Blocks.GRAVEL : r < 8 ? Blocks.SAND : Blocks.CLAY).defaultBlockState(), 2);
                }
                oevers(level, seed, x, z, top);
                iets = true;
            }
        }
        return iets;
    }

    /** A low bank up to the brook's own ground on every dry side that lies lower (so the water, also where it tumbles down a step, stays in its bed). */
    private static void oevers(WorldGenLevel level, long seed, int x, int z, int rand) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (net.minecraft.core.Direction r : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int nx = x + r.getStepX(), nz = z + r.getStepZ();
            if (Beek.diepte(seed, nx, nz) != 0) {
                continue;
            }
            int grond = level.getHeight(Heightmap.Types.OCEAN_FLOOR, nx, nz) - 1;
            for (int y = grond + 1; y <= rand; y++) {
                p.set(nx, y, nz);
                if (level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty()) {
                    level.setBlock(p, (y == rand ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), 2);
                }
            }
        }
    }
}
