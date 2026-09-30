package nl.juiced.guhs.feature.sterrenwacht;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sterrenlantaarn: a little lantern with a glowing star inside and two guh ears on its roof. Deco (light 15), and
 * the Knusfeest's feest-item of the task "sterrenlantaarns" (item tag guhs:knus/sterrenlantaarns): Professor
 * Sterretje gives three for every constellation you connect while that task is open.
 */
public class SterrenlantaarnBlock extends Block {
    public static final MapCodec<SterrenlantaarnBlock> CODEC = simpleCodec(SterrenlantaarnBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 9, 12), Block.box(5, 9, 5, 11, 12, 11));

    public SterrenlantaarnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(SterrenwachtFeature.WENSSTER_DEELTJE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + 0.5 + random.nextDouble() * 0.5, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0, 0.015, 0);
        }
    }
}
