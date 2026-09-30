package nl.juiced.guhs.feature.meppen;

import java.util.Locale;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The blocks of the Mika-mephal: the holes of the whack-a-Mika board (mika_mep_gat), the heads that pop up out of them
 * (mika_mep_kop: a Mika, a golden Mika or a guh, squashed flat for a moment after a whack) and the Mika trophy (deco).
 */
public final class MepBlocks {

    /** What pops up out of a hole. */
    public enum Kop implements StringRepresentable {
        MIKA, GOUD, GUH;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A head on the board, standing on a hole. It always looks at the player who is playing (the game sets FACING when it
     * pops up); after a whack it is squashed flat (BONK) for a few ticks and then goes back into its hole.
     */
    public static class MepKop extends HorizontalDirectionalBlock {
        public static final MapCodec<MepKop> CODEC = simpleCodec(MepKop::new);
        public static final EnumProperty<Kop> KOP = EnumProperty.create("kop", Kop.class);
        public static final BooleanProperty BONK = BooleanProperty.create("bonk");
        private static final VoxelShape HEAD = Block.box(2, 0, 2, 14, 13, 14), FLAT = Block.box(1, 0, 1, 15, 5, 15);

        public MepKop(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KOP, Kop.MIKA).setValue(BONK, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, KOP, BONK);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(BONK) ? FLAT : HEAD;
        }
    }

    /** A Mika head on a golden plinth: a trophy for real Mika-meppers (deco, sold by the Mepguh). */
    public static class MikaTrofee extends HorizontalDirectionalBlock {
        public static final MapCodec<MikaTrofee> CODEC = simpleCodec(MikaTrofee::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

        public MikaTrofee(Properties properties) {
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

    private MepBlocks() {
    }
}
