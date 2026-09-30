package nl.juiced.guhs.feature.guhwaiispellen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Tiki torch: a bamboo pole with a little coconut-shell bowl and a merry flame on top (light 15). On the surf beach
 * of Guhwai'i it rings the hula podium; Tikiguh sells them.
 */
public class TikiFakkelBlock extends Block {
    public static final MapCodec<TikiFakkelBlock> CODEC = simpleCodec(TikiFakkelBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 13, 9.5), Block.box(5, 12, 5, 11, 15, 11));

    public TikiFakkelBlock(Properties properties) {
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.18, y = pos.getY() + 1.02, z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.18;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.012, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.12, z, 0, 0.02, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.35, z, 0, 0.015, 0);
        }
    }
}
