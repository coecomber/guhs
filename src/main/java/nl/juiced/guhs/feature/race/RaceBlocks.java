package nl.juiced.guhs.feature.race;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * The blocks of the guh race track: the glowing checkpoint rings (numbered: 0 is the start/finish), the VAHOEG launch
 * pads that give a race guh a boost, and the invisible start marker (its facing is the way the race starts).
 */
public final class RaceBlocks {
    /** 2.9: which track a ring / start marker belongs to (RaceBaan.index: 0 = the old racebaan, 1-3 the Guh-Circuit's tracks). */
    public static final IntegerProperty BAAN = IntegerProperty.create("baan", 0, 3);
    /** 2.9: does this VAHOEG pad also work on lastig? (The silver ones don't: "fewer VAHOEG pads" on lastig.) */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LASTIG =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("lastig");

    /** A glowing block of a checkpoint ring. All blocks of one ring have the same number; the ring is the box around them. */
    public static class Checkpoint extends Block {
        public static final MapCodec<Checkpoint> CODEC = simpleCodec(Checkpoint::new);
        public static final int MAX = 15;
        public static final IntegerProperty NUMMER = IntegerProperty.create("nummer", 0, MAX);

        public Checkpoint(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(NUMMER, 0).setValue(BAAN, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NUMMER, BAAN);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(6) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, 0.01, 0);
            }
        }
    }

    /** A VAHOEG launch pad: a flat glowing pad with arrows. A race guh that runs over it shoots forward (see RaceGuhEntity). */
    public static class VahoegPad extends HorizontalDirectionalBlock {
        public static final MapCodec<VahoegPad> CODEC = simpleCodec(VahoegPad::new);
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
        private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 0.8f);
        private static final DustParticleOptions SILVER = new DustParticleOptions(new Vector3f(0.85f, 0.88f, 0.95f), 0.8f);

        public VahoegPad(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LASTIG, true));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LASTIG);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            Direction d = state.getValue(FACING);
            level.addParticle(state.getValue(LASTIG) ? SPARK : SILVER, pos.getX() + random.nextDouble(), pos.getY() + 0.15, pos.getZ() + random.nextDouble(),
                    d.getStepX() * 0.2, 0.05, d.getStepZ() * 0.2);
        }
    }

    /** The invisible start marker: the race guh appears here, looking the way this marker faces. */
    public static class Start extends HorizontalDirectionalBlock {
        public static final MapCodec<Start> CODEC = simpleCodec(Start::new);

        public Start(Properties properties) {
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

    private RaceBlocks() {
    }
}
