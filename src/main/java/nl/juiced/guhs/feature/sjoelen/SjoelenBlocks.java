package nl.juiced.guhs.feature.sjoelen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The blocks of the sjoelbak in the Sjoelhuisje. FACING is the way the pucks slide (from the head to the gates), DEEL
 * which of the five blocks across the bak it is (0 = the left one, seen from the head): together they draw the gate bar
 * with its four openings (2-3-4-1) and the lanes behind it, over five blocks. They only come with the Sjoelhuisje.
 */
public final class SjoelenBlocks {
    public static final IntegerProperty DEEL = IntegerProperty.create("deel", 0, 4);

    /** A block of the sjoelbak across its width: the head ({@code sjoelen_kop}), the gate bar or a lane block. */
    public static class Deel extends HorizontalDirectionalBlock {
        public static final MapCodec<Deel> CODEC = simpleCodec(Deel::new);

        public Deel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DEEL, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, DEEL);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
    }

    /** A stack of sjoelschijven (decoration: Opoe keeps them everywhere). */
    public static class Stapel extends HorizontalDirectionalBlock {
        public static final MapCodec<Stapel> CODEC = simpleCodec(Stapel::new);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

        public Stapel(Properties properties) {
            super(properties);
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

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    private SjoelenBlocks() {
    }
}
