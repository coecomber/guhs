package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.feature.safari.quests.SafariQuests;

/**
 * guhs:beverbeek_sporen (step vegetal_decoration, once per chunk, after the trees): the signs that beavers live here.
 * Gnawed stumps (a log with a stripped, chewed top), leaf heaps (a little mound of leaves with leaf litter round it) and,
 * near the brooks, trails of paw prints (the safari's {@code guhs:pootafdruk}) running down to the water.
 */
public class SporenFeature extends Feature<NoneFeatureConfiguration> {
    public SporenFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        boolean iets = false;
        for (int i = 0; i < 2; i++) {
            BlockPos g = grond(level, x0 + rnd.nextInt(16), z0 + rnd.nextInt(16));
            if (g != null && rnd.nextInt(3) > 0) {
                boolean berk = rnd.nextInt(3) == 0;
                level.setBlock(g.above(), (berk ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState(), 2);
                if (rnd.nextBoolean()) {
                    level.setBlock(g.above(2), (berk ? Blocks.STRIPPED_BIRCH_LOG : Blocks.STRIPPED_OAK_LOG).defaultBlockState(), 2);
                }
                iets = true;
            }
        }
        BlockPos g = grond(level, x0 + rnd.nextInt(16), z0 + rnd.nextInt(16));
        if (g != null && rnd.nextInt(2) == 0) {
            level.setBlock(g.above(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = grond(level, g.getX() + d.getStepX(), g.getZ() + d.getStepZ());
                if (n != null && rnd.nextInt(4) > 0) {
                    level.setBlock(n.above(), BeverpoelFeature.strooisel(rnd), 2);
                }
            }
            iets = true;
        }
        iets |= spoor(level, rnd, x0, z0);
        return iets;
    }

    /** A trail of 3-6 paw prints toward the nearest brook within 6 blocks of a random spot. */
    private static boolean spoor(WorldGenLevel level, RandomSource rnd, int x0, int z0) {
        long seed = level.getSeed();
        int x = x0 + 3 + rnd.nextInt(10), z = z0 + 3 + rnd.nextInt(10);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int stap = 1; stap <= 6; stap++) {
                if (Beek.diepte(seed, x + d.getStepX() * stap, z + d.getStepZ() * stap) == 0) {
                    continue;
                }
                int n = 0;
                for (int i = 0; i < stap && i < 6; i++) {
                    BlockPos g = grond(level, x + d.getStepX() * i, z + d.getStepZ() * i);
                    if (g != null && level.getBlockState(g.above()).isAir()) {
                        level.setBlock(g.above(), SafariQuests.POOTAFDRUK.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, d), 2);
                        n++;
                    }
                }
                return n > 0;
            }
        }
        return false;
    }

    /** The grass or dirt on top at x, z, with air above, in the Beverbeek (null otherwise). */
    private static BlockPos grond(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        BlockState s = level.getBlockState(top);
        if (!(s.is(BlockTags.DIRT) || s.is(BlockTags.GRASS_BLOCKS)) || !level.getBlockState(top.above()).canBeReplaced() || !level.getBiome(top).is(BeverbeekFeature.BIOME)) {
            return null;
        }
        return top;
    }
}
