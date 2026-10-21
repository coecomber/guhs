package nl.juiced.guhs.feature.bouwhulp.blokken;

import java.util.Map;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * F11 vormhulp (design 2.7): the shapes that make building quicker. For vanilla wood and stone and the guh blocks alike.
 * <ul>
 *   <li>{@link VerticalePlaat}: half a block standing up, against the block you click (on a floor: on the far side);</li>
 *   <li>{@link Hoekje}: a quarter column, in the corner nearest to where you click (closes the gap where two vertical slabs meet);</li>
 *   <li>{@link PaalBalk}: posts (thin, always upright) and beams (thick) that join each other and what they rest on;</li>
 *   <li>{@link DakHelling}: a sloped roof in parts (half slope: 2 parts per block of height, quarter: 4), stacked by itself
 *       when you build up the slope; right-click: the next part.</li>
 * </ul>
 */
public final class VormBlokken {

    /** Shared by the half blocks: water. */
    abstract static class Nat extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
        static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

        Nat(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, WATERLOGGED);
        }

        BlockState nat(BlockPlaceContext context, Direction facing) {
            return defaultBlockState().setValue(FACING, facing)
                    .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (state.getValue(WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
        }
    }

    public static class VerticalePlaat extends Nat {
        public static final MapCodec<VerticalePlaat> CODEC = simpleCodec(VerticalePlaat::new);
        static final Map<Direction, VoxelShape> VORM = Vorm.draai(new double[]{0, 0, 0, 16, 16, 8});

        public VerticalePlaat(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction f = context.getClickedFace();
            return nat(context, f.getAxis().isHorizontal() ? f.getOpposite() : context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM.get(state.getValue(FACING));
        }
    }

    /** A quarter column; facing north = the north-west corner, then clockwise (east = north-east...). */
    public static class Hoekje extends Nat {
        public static final MapCodec<Hoekje> CODEC = simpleCodec(Hoekje::new);
        static final Map<Direction, VoxelShape> VORM = Vorm.draai(new double[]{0, 0, 0, 8, 16, 8});

        public Hoekje(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Vec3 k = Vorm.klik(context);
            boolean oost = k.x >= 0.5, zuid = k.z >= 0.5;
            Direction f = !oost && !zuid ? Direction.NORTH : oost && !zuid ? Direction.EAST : oost ? Direction.SOUTH : Direction.WEST;
            return nat(context, f);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM.get(state.getValue(FACING));
        }
    }

    /**
     * A post (thin, {@code dik} false) or a beam (thick). Each side has an arm when something is there to join: another
     * post or beam, or for the ends (and every side of a beam) a solid face. A post is always a whole upright column; a
     * beam with nothing around it too.
     */
    public static class PaalBalk extends Block {
        static final Map<Direction, BooleanProperty> ARM = PipeBlock.PROPERTY_BY_DIRECTION;
        private final boolean dik;
        private final Function<BlockState, VoxelShape> vormen;

        public PaalBalk(Properties properties, boolean dik) {
            super(properties);
            this.dik = dik;
            BlockState s = stateDefinition.any();
            for (BooleanProperty p : ARM.values()) {
                s = s.setValue(p, false);
            }
            registerDefaultState(s.setValue(ARM.get(Direction.UP), true).setValue(ARM.get(Direction.DOWN), true));
            this.vormen = getShapeForEachState(this::vorm);
        }

        public boolean dik() {
            return dik;
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(p -> new PaalBalk(p, dik));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            ARM.values().forEach(builder::add);
        }

        private VoxelShape vorm(BlockState s) {
            double t = dik ? 10 : 6, lo = 8 - t / 2, hi = 8 + t / 2;
            VoxelShape v = Block.box(lo, lo, lo, hi, hi, hi);
            if (s.getValue(ARM.get(Direction.NORTH))) v = Shapes.or(v, Block.box(lo, lo, 0, hi, hi, lo));
            if (s.getValue(ARM.get(Direction.SOUTH))) v = Shapes.or(v, Block.box(lo, lo, hi, hi, hi, 16));
            if (s.getValue(ARM.get(Direction.WEST))) v = Shapes.or(v, Block.box(0, lo, lo, lo, hi, hi));
            if (s.getValue(ARM.get(Direction.EAST))) v = Shapes.or(v, Block.box(hi, lo, lo, 16, hi, hi));
            if (s.getValue(ARM.get(Direction.DOWN))) v = Shapes.or(v, Block.box(lo, 0, lo, hi, lo, hi));
            if (s.getValue(ARM.get(Direction.UP))) v = Shapes.or(v, Block.box(lo, hi, lo, hi, 16, hi));
            return v.optimize();
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vormen.apply(state);
        }

        /** The arms this post or beam has at pos. */
        public BlockState verbonden(LevelReader level, BlockPos pos) {
            BlockState s = defaultBlockState();
            boolean iets = false;
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                BlockState b = level.getBlockState(n);
                boolean arm = b.getBlock() instanceof PaalBalk
                        || ((dik || d.getAxis() == Direction.Axis.Y) && b.isFaceSturdy(level, n, d.getOpposite()));
                s = s.setValue(ARM.get(d), arm);
                iets |= arm;
            }
            if (!dik || !iets) {
                s = s.setValue(ARM.get(Direction.UP), true).setValue(ARM.get(Direction.DOWN), true);
            }
            return s;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return verbonden(context.getLevel(), context.getClickedPos());
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return verbonden(level, pos);
        }
    }

    /**
     * A sloped roof part (facing = the high side; you face it when you walk up). Four little steps per block; the parts
     * ({@code deel}) stack: a half slope climbs a block in 2 parts, a quarter slope in 4.
     */
    public abstract static class DakHelling extends HorizontalDirectionalBlock {
        private final Function<BlockState, VoxelShape> vormen;

        DakHelling(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(deel(), 0));
            this.vormen = getShapeForEachState(this::vorm);
        }

        abstract IntegerProperty deel();

        abstract int delen();

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, deel());
        }

        private VoxelShape vorm(BlockState s) {
            double per = 16.0 / delen();
            int d = s.getValue(deel());
            double[][] stappen = new double[4][];
            for (int i = 0; i < 4; i++) {
                double z1 = 16 - 4 * i;
                stappen[i] = new double[]{0, 0, z1 - 4, 16, d * per + per * (i + 1) / 4, z1};
            }
            return Vorm.draai(stappen).get(s.getValue(FACING));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vormen.apply(state);
        }

        /** The part that goes here: one higher than the part below the slope, else one lower than the part above it, else 0. */
        public int deelVoor(LevelReader level, BlockPos pos, Direction facing) {
            BlockState lager = level.getBlockState(pos.relative(facing.getOpposite()));
            if (lager.is(this) && lager.getValue(FACING) == facing) {
                return (lager.getValue(deel()) + 1) % delen();
            }
            BlockState hoger = level.getBlockState(pos.relative(facing));
            if (hoger.is(this) && hoger.getValue(FACING) == facing) {
                return (hoger.getValue(deel()) + delen() - 1) % delen();
            }
            BlockState hogerOp = level.getBlockState(pos.relative(facing).above());
            if (hogerOp.is(this) && hogerOp.getValue(FACING) == facing && hogerOp.getValue(deel()) == 0) {
                return delen() - 1;
            }
            return 0;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction f = context.getHorizontalDirection();
            return defaultBlockState().setValue(FACING, f).setValue(deel(), deelVoor(context.getLevel(), context.getClickedPos(), f));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(deel(), (state.getValue(deel()) + 1) % delen()), Block.UPDATE_ALL);
                level.playSound(null, pos, state.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 0.7f, 1.3f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class DakHalf extends DakHelling {
        public static final IntegerProperty DEEL = IntegerProperty.create("deel", 0, 1);
        public static final MapCodec<DakHalf> CODEC = simpleCodec(DakHalf::new);

        public DakHalf(Properties properties) {
            super(properties);
        }

        @Override
        IntegerProperty deel() {
            return DEEL;
        }

        @Override
        int delen() {
            return 2;
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static class DakKwart extends DakHelling {
        public static final IntegerProperty DEEL = IntegerProperty.create("deel", 0, 3);
        public static final MapCodec<DakKwart> CODEC = simpleCodec(DakKwart::new);

        public DakKwart(Properties properties) {
            super(properties);
        }

        @Override
        IntegerProperty deel() {
            return DEEL;
        }

        @Override
        int delen() {
            return 4;
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    private VormBlokken() {
    }
}
