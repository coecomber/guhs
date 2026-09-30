package nl.juiced.guhs.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * A pink water pool of the guh sea: a roundish hole 1-3 deep (deepest in the middle), a rim of pink concrete powder,
 * and guh lily pads floating on it. Only on (roughly) flat ground, so the water can't run out.
 */
public class GuhPoolFeature extends Feature<NoneFeatureConfiguration> {
    public GuhPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        double rx = 3 + random.nextInt(4), rz = 3 + random.nextInt(4);
        double wobble = random.nextDouble() * Math.PI;
        int r = (int) Math.ceil(Math.max(rx, rz)) + 2;
        // the water sits at the lowest ground around the pool (so it can't run out); too hilly = no pool
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (inside(dx, dz, rx + 1.5, rz + 1.5, wobble)) {
                    int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz);
                    low = Math.min(low, h);
                    high = Math.max(high, h);
                }
            }
        }
        if (high - low > 3) {
            return false;
        }
        int surface = low;
        // (never into a building: the buildings of the chunks around are already there when this runs)
        if (nl.juiced.guhs.world.BouwRuimte.touchesBuilding(level, origin.getX() - r - 1, surface - 6, origin.getZ() - r - 1,
                origin.getX() + r + 1, high + 6, origin.getZ() + r + 1)) {
            return false;
        }
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState rim = Blocks.PINK_CONCRETE_POWDER.defaultBlockState();
        int top = surface - 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = dist(dx, dz, rx, rz, wobble);
                BlockPos column = new BlockPos(origin.getX() + dx, top, origin.getZ() + dz);
                if (d <= 1) {
                    int depth = d < 0.35 ? 3 : d < 0.7 ? 2 : 1;
                    for (int y = 0; y < depth; y++) {
                        level.setBlock(column.below(y), water, 2);
                    }
                    level.setBlock(column.below(depth), rim, 2);
                    for (int y = 1; y <= 4; y++) {
                        level.setBlock(column.above(y), Blocks.AIR.defaultBlockState(), 2);
                    }
                    if (random.nextInt(7) == 0 && d > 0.2) {
                        level.setBlock(column.above(), ModBlocks.GUH_WATERLELIE.get().defaultBlockState(), 2);
                    }
                } else if (d <= 1 + 1.5 / Math.min(rx, rz)) {
                    level.setBlock(column, rim, 2);
                }
            }
        }
        return true;
    }

    private static double dist(int dx, int dz, double rx, double rz, double wobble) {
        double a = Math.atan2(dz, dx);
        double w = 1 + 0.18 * Math.sin(a * 3 + wobble);
        return Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz)) / w;
    }

    private static boolean inside(int dx, int dz, double rx, double rz, double wobble) {
        return dist(dx, dz, rx, rz, wobble) <= 1;
    }
}
