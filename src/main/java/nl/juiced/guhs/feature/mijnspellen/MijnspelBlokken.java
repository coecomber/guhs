package nl.juiced.guhs.feature.mijnspellen;

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
 * The game blocks of the cave minigames (they only come with the buildings: unbreakable, no item):
 * <ul>
 *   <li>{@link Begin}: where a baan starts (the tunnel mouth of a rail network, the foul line of a bowling lane): FACING is
 *       the way the carts roll / the ball goes, BAAN which of the three it is (0 makkelijk .. 2 lastig). The game guh finds
 *       its banen by them;</li>
 *   <li>{@link Soort}: a block with a kind (the ore of a bin, ...) and a facing, optionally a lower shape.</li>
 * </ul>
 */
public final class MijnspelBlokken {
    public static final IntegerProperty BAAN = IntegerProperty.create("baan", 0, 2);
    public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 5);

    public static class Begin extends HorizontalDirectionalBlock {
        public static final MapCodec<Begin> CODEC = simpleCodec(Begin::new);

        public Begin(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BAAN, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, BAAN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
    }

    public static class Soort extends HorizontalDirectionalBlock {
        public static final MapCodec<Soort> CODEC = simpleCodec(p -> new Soort(p, 16));
        private final VoxelShape shape;

        public Soort(Properties properties, double hoogte) {
            super(properties);
            this.shape = Block.box(0, 0, 0, 16, hoogte, 16);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOORT, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SOORT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    /** A block with a fixed (smaller) shape and no properties: a gutter, a bumper, a post. */
    public static class Laag extends Block {
        private final VoxelShape shape;

        public Laag(Properties properties, double x0, double hoogte, double x1) {
            super(properties);
            this.shape = Block.box(x0, 0, x0, x1, hoogte, x1);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    private MijnspelBlokken() {
    }
}
