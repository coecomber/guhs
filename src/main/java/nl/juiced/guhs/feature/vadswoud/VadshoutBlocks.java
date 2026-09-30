package nl.juiced.guhs.feature.vadswoud;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.registry.ModSounds;

/** The special blocks of the vadshout wood set: the log (strips to its stripped log), the guh faces in the bark, the leaves and the sapling. */
public final class VadshoutBlocks {
    /** A vadshout log: an axe strips it. */
    public static class Log extends RotatedPillarBlock {
        private final Supplier<Block> stripped;

        public Log(Supplier<Block> stripped, Properties properties) {
            super(properties);
            this.stripped = stripped;
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }

    /**
     * A little guh face in the bark of a giant guh tree. It looks the way it faces, in one of four moods (0 happy,
     * 1 sleepy, 2 surprised, 3 vads); right-click it and it pulls another face. An axe strips it to a plain stripped log.
     */
    public static class Gezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<Gezicht> CODEC = simpleCodec(Gezicht::new);
        public static final IntegerProperty STEMMING = IntegerProperty.create("stemming", 0, 3);

        public Gezicht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STEMMING, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STEMMING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(STEMMING, context.getLevel().getRandom().nextInt(4));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(STEMMING), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.BLOCKS, 0.5f, 1.4f + level.getRandom().nextFloat() * 0.3f);
            }
            return InteractionResult.SUCCESS;
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return VadswoudFeature.VADSHOUT_GESTRIPT.get().defaultBlockState();
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }

    /** Vadshout leaves: now and then a little mint leaf floats down. */
    public static class Leaves extends LeavesBlock {
        // 26.1: LeavesBlock is abstract (falling-leaf particles); 1.0.0's leaves had none -> chance 0, nothing spawned
        public static final com.mojang.serialization.MapCodec<Leaves> CODEC = simpleCodec(Leaves::new);

        @Override
        public com.mojang.serialization.MapCodec<Leaves> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        }

        public Leaves(Properties properties) {
            super(0f, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            if (random.nextInt(14) == 0) {
                BlockPos below = pos.below();
                if (!isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) {
                    ParticleUtils.spawnParticleBelow(level, pos, random, VadswoudFeature.VADSBLAADJE.get());
                }
            }
        }
    }

    /** One sapling grows a vadshout tree; four in a square grow a giant guh tree. Happy on moss, wool and kaasknabbels too. */
    public static class Sapling extends SaplingBlock {
        public Sapling(Properties properties) {
            super(VadswoudFeature.GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || (state.isFaceSturdy(level, pos, Direction.UP)
                    && !state.is(net.minecraft.tags.BlockTags.LEAVES) && !state.is(net.minecraft.tags.BlockTags.LOGS));
        }
    }

    private VadshoutBlocks() {
    }
}
