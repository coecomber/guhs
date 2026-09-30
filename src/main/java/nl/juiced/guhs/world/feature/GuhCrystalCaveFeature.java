package nl.juiced.guhs.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * A room of the guh crystal mine (y 30-60): a lumpy cave, its walls turned to crystal stone, with glowing, singing
 * guh crystals growing on floor, walls and ceiling. Rooms from neighbouring chunks overlap into bigger caves.
 */
public class GuhCrystalCaveFeature extends Feature<NoneFeatureConfiguration> {
    public GuhCrystalCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        // 2-4 overlapping blobs make one room; stay within the chunk and its neighbours (radius <= 7 from origin)
        int blobs = 2 + random.nextInt(3);
        double[][] centres = new double[blobs][4];
        for (int i = 0; i < blobs; i++) {
            centres[i][0] = origin.getX() + random.nextInt(7) - 3;
            centres[i][1] = origin.getY() + random.nextInt(5) - 2;
            centres[i][2] = origin.getZ() + random.nextInt(7) - 3;
            centres[i][3] = 3.5 + random.nextDouble() * 2.5;
        }
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();
        BlockState stone = ModBlocks.GUH_KRISTALSTEEN.get().defaultBlockState();
        int r = 8;
        // (never into a building, like the Knabbelkelder at the same depth)
        if (nl.juiced.guhs.world.BouwRuimte.touchesBuilding(level, origin.getX() - r - 2, origin.getY() - 9, origin.getZ() - r - 2,
                origin.getX() + r + 2, origin.getY() + 9, origin.getZ() + r + 2)) {
            return false;
        }
        // carve
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -6, -r), origin.offset(r, 6, r))) {
            if (pos.getY() < 28 || pos.getY() > 62 || nearSurface(level, pos, 7)) {
                continue;
            }
            if (inAnyBlob(centres, pos, 0)) {
                level.setBlock(pos, air, 2);
            }
        }
        // line the walls with crystal stone, grow crystals on them
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -7, -r), origin.offset(r, 7, r))) {
            if (pos.getY() < 27 || pos.getY() > 63 || nearSurface(level, pos, 5) || level.getBlockState(pos).isAir() || !inAnyBlob(centres, pos, 1.3)) {
                continue;
            }
            if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
                level.setBlock(pos, random.nextInt(9) == 0 ? ModBlocks.GUH_KRISTAL_BLOK.get().defaultBlockState() : stone, 2);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (level.getBlockState(next).is(Blocks.CAVE_AIR) && random.nextInt(dir == Direction.UP ? 7 : 11) == 0) {
                    level.setBlock(next, ModBlocks.GUH_KRISTAL_CLUSTER.get().defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, dir), 2);
                }
            }
        }
        return true;
    }

    /** The mines stay well under the ground: nothing within `margin` blocks of the surface (valleys dip below y60). */
    private static boolean nearSurface(WorldGenLevel level, BlockPos pos, int margin) {
        return pos.getY() > level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - margin;
    }

    private static boolean inAnyBlob(double[][] centres, BlockPos pos, double extra) {
        for (double[] c : centres) {
            double dx = pos.getX() + 0.5 - c[0], dy = (pos.getY() + 0.5 - c[1]) * 1.4, dz = pos.getZ() + 0.5 - c[2];
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= c[3] + extra) {
                return true;
            }
        }
        return false;
    }
}
