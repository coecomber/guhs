package nl.juiced.guhs.feature.einde2.zij;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * einde2 zij: one block of the small earth pillar of the lasting earth power ({@link ZijKrachten#AARDZUIL}). It is
 * never an item, drops nothing and crumbles by itself: every block has its own scheduled tick (saved with the chunk, so a
 * pillar never outlives a restart for long).
 */
public class AardzuilBlock extends Block {
    public static final MapCodec<AardzuilBlock> CODEC = simpleCodec(AardzuilBlock::new);

    public AardzuilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_MUD.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f + random.nextFloat() * 0.2f);
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oud, boolean moved) {
        super.onPlace(state, level, pos, oud, moved);
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, ZijKrachten.ZUIL_TICKS);   // (a block that came some other way crumbles too)
        }
    }
}
