package nl.juiced.guhs.feature.doolhof;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of Het Guhdoolhof: the hedge with a guh face, the lantern that lights up at night, the invisible anchor. */
public final class DoolhofBlocks {
    /** A hedge block with a trimmed guh face (topiary), facing the way you look at it from. */
    public static class HegGezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<HegGezicht> CODEC = simpleCodec(HegGezicht::new);

        public HegGezicht(Properties properties) {
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
    }

    /**
     * A little guh-ear lantern on the hedges: it lights up by itself when it gets dark (and goes out in the morning).
     * It looks every few seconds (a scheduled tick), so a whole row of them switches on one by one, like fireflies.
     */
    public static class Lantaarn extends Block {
        public static final MapCodec<Lantaarn> CODEC = simpleCodec(Lantaarn::new);
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 7, 11), Block.box(6, 7, 6, 10, 9, 10));

        public Lantaarn(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(LIT, donker(context.getLevel()));
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            if (!level.isClientSide() && !old.is(this)) {
                level.scheduleTick(pos, this, 20 + level.getRandom().nextInt(60));
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean moet = donker(level);
            if (state.getValue(LIT) != moet) {
                level.setBlock(pos, state.setValue(LIT, moet), Block.UPDATE_CLIENTS);
            }
            level.scheduleTick(pos, this, 100 + random.nextInt(100));
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            // (a lantern from a template or /setblock that never got its first scheduled tick still wakes up)
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 20);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        /** Night (or a dark, rainy evening in the Guhmension): the lanterns go on. */
        public static boolean donker(Level level) {
            long t = Math.floorMod(level.getDayTime(), 24000L);
            return t >= 12500 && t <= 23500;
        }
    }

    /** The invisible anchor under Meneer Vadskronkel (and Juf Vahoegsakee): where the building is and how it's turned. */
    public static class AnkerBlock extends HorizontalDirectionalBlock {
        public static final MapCodec<AnkerBlock> CODEC = simpleCodec(AnkerBlock::new);

        public AnkerBlock(Properties properties) {
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
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    private DoolhofBlocks() {
    }
}
