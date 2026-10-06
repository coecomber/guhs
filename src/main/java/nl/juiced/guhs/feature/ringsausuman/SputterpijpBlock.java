package nl.juiced.guhs.feature.ringsausuman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sputterpijp: an exhaust pipe of Sausuman's machines. It puffs, pops and sputters all by itself (client only: smoke and a
 * sound now and then) as long as there is air above it; {@code groot}: a tall plume like a signal fire, for the chimneys on
 * the outside of the tower. Decoration: it burns nothing and hurts nobody.
 */
public class SputterpijpBlock extends Block {
    public static final MapCodec<SputterpijpBlock> CODEC = simpleCodec(SputterpijpBlock::new);
    public static final BooleanProperty GROOT = BooleanProperty.create("groot");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 12, 11), Block.box(4, 12, 4, 12, 16, 12));

    public SputterpijpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GROOT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROOT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.02, z = pos.getZ() + 0.5;
        boolean groot = state.getValue(GROOT);
        if (random.nextInt(groot ? 3 : 5) == 0) {
            level.addAlwaysVisibleParticle(groot ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    x + (random.nextDouble() - 0.5) * 0.2, y, z + (random.nextDouble() - 0.5) * 0.2, 0, 0.06 + random.nextDouble() * 0.02, 0);
        }
        // a sputter: a pop, a puff of dark smoke and a spark
        if (random.nextInt(groot ? 40 : 60) == 0) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, (random.nextDouble() - 0.5) * 0.06, 0.05 + random.nextDouble() * 0.05,
                        (random.nextDouble() - 0.5) * 0.06);
            }
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
            level.playLocalSound(x, y, z, RingSausumanFeature.SPUTTER.get(), SoundSource.BLOCKS, groot ? 0.5f : 0.35f, 0.7f + random.nextFloat() * 0.6f, false);
        }
    }
}
