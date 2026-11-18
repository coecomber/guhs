package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The plaza blocks of the Guhwinter games (the highscore board has its own class, {@link HighscorebordBlock}):
 * <ul>
 *   <li>{@link Arcadekast}: two blocks tall; right-click = the Pepernotenrace;</li>
 *   <li>{@link Kraam}: the pepernoten shop's stall and the oliebollen stall (bakkraam);</li>
 *   <li>{@link Schaatsbord}: the little start sign of the schaatsrondje on the pond;</li>
 *   <li>{@link Zakje}: a hidden bag of pepernoten (one handful per player);</li>
 *   <li>{@link Lampjes}: kerstlampjes, a string of lights for on a wall (from the shop; melt on 3 January).</li>
 * </ul>
 */
public final class SpelBlokken {
    private SpelBlokken() {
    }

    /** A block that looks a way (the side you placed it from). */
    public abstract static class Gericht extends HorizontalDirectionalBlock {
        protected Gericht(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    // --- the arcade cabinet ------------------------------------------------------------------------------------------------

    public static class Arcadekast extends HorizontalDirectionalBlock {
        public static final MapCodec<Arcadekast> CODEC = simpleCodec(Arcadekast::new);
        public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 16, 15);

        public Arcadekast(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, HALF);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockPos pos = context.getClickedPos();
            if (pos.getY() < context.getLevel().getMaxY() && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
                return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
            }
            return null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }

        /** Puts a whole cabinet down (the placing command). */
        public static void bouw(Level level, BlockPos pos, Block kast, Direction facing) {
            BlockState onder = kast.defaultBlockState().setValue(FACING, facing);
            level.setBlock(pos, onder, 3);
            level.setBlock(pos.above(), onder.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos buurPos,
                                         BlockState buur, RandomSource random) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
                return buur.is(this) && buur.getValue(HALF) != half ? state.setValue(FACING, buur.getValue(FACING)) : Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, level, ticks, pos, dir, buurPos, buur, random);
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                BlockState below = level.getBlockState(pos.below());
                return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
            }
            return super.canSurvive(state, level, pos);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        public static BlockPos onder(BlockState state, BlockPos pos) {
            return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Pepernotenrace.open(sp, onder(state, pos));
            }
            return InteractionResult.SUCCESS;
        }
    }

    // --- the stalls --------------------------------------------------------------------------------------------------------

    /** WINKEL = the pepernoten shop, BAKKRAAM = oliebollen bakken (Oud &amp; Nieuw). */
    public enum KraamSoort { WINKEL, BAKKRAAM }

    public static class Kraam extends Gericht {
        private final KraamSoort soort;
        private final MapCodec<Kraam> codec;

        public Kraam(Properties p, KraamSoort soort) {
            super(p);
            this.soort = soort;
            this.codec = simpleCodec(q -> new Kraam(q, soort));
        }

        public KraamSoort soort() {
            return soort;
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return codec;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                if (soort == KraamSoort.WINKEL) {
                    GuhwinterWinkel.open(sp);
                } else {
                    Oliebollen.bijKraam(sp);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    // --- the start sign of the skating lap ---------------------------------------------------------------------------------

    public static class Schaatsbord extends Gericht {
        public static final MapCodec<Schaatsbord> CODEC = simpleCodec(Schaatsbord::new);
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 15, 12);

        public Schaatsbord(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Schaatsrondje.start(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // --- a hidden bag of pepernoten ----------------------------------------------------------------------------------------

    public static class Zakje extends Gericht {
        public static final MapCodec<Zakje> CODEC = simpleCodec(Zakje::new);
        private static final VoxelShape VORM = Block.box(5, 0, 5, 11, 7, 11);

        public Zakje(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                VerstopteNoten.pak(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // --- kerstlampjes ------------------------------------------------------------------------------------------------------

    /** A string of Christmas lights against a wall (FACING = away from the wall), at the top of the block. */
    public static class Lampjes extends HorizontalDirectionalBlock {
        public static final MapCodec<Lampjes> CODEC = simpleCodec(Lampjes::new);
        private static final Map<Direction, VoxelShape> VORMEN = Map.of(
                Direction.NORTH, Block.box(0, 9, 13, 16, 16, 16),
                Direction.SOUTH, Block.box(0, 9, 0, 16, 16, 3),
                Direction.EAST, Block.box(0, 9, 0, 3, 16, 16),
                Direction.WEST, Block.box(13, 9, 0, 16, 16, 16));

        public Lampjes(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction kant = context.getClickedFace();
            Direction f = kant.getAxis().isHorizontal() ? kant : context.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, f);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORMEN.get(state.getValue(FACING));
        }
    }
}
