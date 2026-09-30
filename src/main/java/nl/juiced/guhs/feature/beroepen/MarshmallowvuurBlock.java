package nl.juiced.guhs.feature.beroepen;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A marshmallow kampvuurkuil (the Brandweer's oefenterrein): a ring of guh-stones with logs and two marshmallows on
 * sticks. {@link #VUUR} 0 = out (cosy), 1..3 = the fire has got a bit out of hand (bigger flames, more light, crackling).
 * The flames never hurt anyone and never spread - they only go out when you spray them ({@link #blus}).
 */
public class MarshmallowvuurBlock extends Block {
    public static final MapCodec<MarshmallowvuurBlock> CODEC = simpleCodec(MarshmallowvuurBlock::new);
    public static final int MAX = 3;
    public static final IntegerProperty VUUR = IntegerProperty.create("vuur", 0, MAX);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public MarshmallowvuurBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VUUR, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VUUR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int v = state.getValue(VUUR);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.35, z = pos.getZ() + 0.5;
        if (v == 0) {
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
            }
            return;
        }
        for (int i = 0; i < v * 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.7, y + random.nextDouble() * 0.4 * v,
                    z + (random.nextDouble() - 0.5) * 0.7, 0, 0.02 + 0.01 * v, 0);
        }
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.6 * v, z + (random.nextDouble() - 0.5) * 0.4,
                0, 0.06, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y + 0.3, z, 0, 0, 0);
        }
        if (random.nextInt(6) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f + 0.2f * v, 0.8f + random.nextFloat() * 0.3f, false);
        }
    }

    /** A spray of water hits this fire: one step smaller (true); out: a big hiss and a cloud of steam. */
    public static boolean blus(ServerLevel level, BlockPos pos, @Nullable ServerPlayer by) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MarshmallowvuurBlock) || state.getValue(VUUR) == 0) {
            return false;
        }
        int v = state.getValue(VUUR) - 1;
        level.setBlock(pos, state.setValue(VUUR, v), 3);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        if (v == 0) {
            level.playSound(null, pos, BeroepenFeature.SISSEN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.CLOUD, x, y + 0.4, z, 20, 0.35, 0.4, 0.35, 0.04);
            level.sendParticles(ParticleTypes.WHITE_SMOKE, x, y, z, 10, 0.3, 0.2, 0.3, 0.02);
        } else {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
            level.sendParticles(ParticleTypes.WHITE_SMOKE, x, y, z, 6, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }
}
