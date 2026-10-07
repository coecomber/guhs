package nl.juiced.guhs.feature.bio.blokkenwolk;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The regenboogblok, its slab and its stairs: soft, a little see-through, and striped so that a bridge of them reads as ONE
 * rainbow.
 * <ul>
 *   <li>The sides show the colours in layers, red on top and violet below, fixed to the world: an arc of blocks, slabs and
 *       stairs looks like a rainbow from the side.</li>
 *   <li>The top and the underside show the colours as stripes that run ALONG the block's axis (the way you looked when you
 *       placed it; for stairs the way they climb), also fixed to the world, so the stripes run on over every step.</li>
 *   <li>Pieces side by side across that axis share the colours ({@link #STROOK}): one block wide is a whole rainbow, three
 *       wide is one rainbow spread over the three (wider: the middle part repeats).</li>
 * </ul>
 * Landing on it never hurts (like cloud); it does not sink. Faces between two rainbow pieces are not drawn.
 */
public final class RegenboogBlokken {
    /** Which part of the colours the top of a piece shows: all of them, or the first, middle or last third. */
    public enum Strook implements StringRepresentable {
        HEEL("heel"), A("a"), B("b"), C("c");

        private final String naam;

        Strook(String naam) {
            this.naam = naam;
        }

        @Override
        public String getSerializedName() {
            return naam;
        }

        /** The same piece seen from the other side. */
        public Strook om() {
            return this == A ? C : this == C ? A : this;
        }
    }

    public static final EnumProperty<Strook> STROOK = EnumProperty.create("strook", Strook.class);
    /** The way the stripes run (blocks and slabs; stairs: the axis of their facing). */
    public static final EnumProperty<Direction.Axis> AS = BlockStateProperties.HORIZONTAL_AXIS;

    /** A piece of rainbow: tells the way its stripes run. */
    public interface Deel {
        Direction.Axis as(BlockState state);
    }

    /** The side where the red stripe is: north for stripes along x, west for stripes along z. */
    public static Direction rodeKant(Direction.Axis as) {
        return as == Direction.Axis.X ? Direction.NORTH : Direction.WEST;
    }

    private static boolean hoortBij(BlockState buur, Direction.Axis as) {
        return buur.getBlock() instanceof Deel deel && deel.as(buur) == as;
    }

    /** The part a piece at this spot shows, from the pieces beside it (across its stripes). */
    public static Strook strook(BlockGetter level, BlockPos pos, Direction.Axis as) {
        Direction rood = rodeKant(as);
        boolean ervoor = hoortBij(level.getBlockState(pos.relative(rood)), as);
        boolean erna = hoortBij(level.getBlockState(pos.relative(rood.getOpposite())), as);
        return ervoor && erna ? Strook.B : erna ? Strook.A : ervoor ? Strook.C : Strook.HEEL;
    }

    /** (a structure template turned: the red side moves with it) */
    public static Strook gedraaid(Direction.Axis as, Strook strook, Rotation rotation) {
        boolean om = switch (rotation) {
            case NONE -> false;
            case CLOCKWISE_180 -> true;
            case CLOCKWISE_90 -> as == Direction.Axis.X;
            case COUNTERCLOCKWISE_90 -> as == Direction.Axis.Z;
        };
        return om ? strook.om() : strook;
    }

    public static Strook gespiegeld(Direction.Axis as, Strook strook, Mirror mirror) {
        boolean om = mirror == Mirror.LEFT_RIGHT && as == Direction.Axis.X || mirror == Mirror.FRONT_BACK && as == Direction.Axis.Z;
        return om ? strook.om() : strook;
    }

    private static Direction.Axis andere(Direction.Axis as) {
        return as == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** A face that lies wholly against another rainbow piece is not drawn (like glass against glass). */
    static boolean verborgen(BlockState state, BlockState buur, Direction richting) {
        if (!(buur.getBlock() instanceof Deel)) {
            return false;
        }
        VoxelShape mijn = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(richting);
        VoxelShape zijn = buur.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).getFaceShape(richting.getOpposite());
        return !Shapes.joinIsNotEmpty(mijn, zijn, BooleanOp.ONLY_FIRST);
    }

    public static class Blok extends Block implements Deel {
        public static final MapCodec<Blok> CODEC = simpleCodec(Blok::new);

        public Blok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AS, Direction.Axis.X).setValue(STROOK, Strook.HEEL));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AS, STROOK);
        }

        @Override
        public Direction.Axis as(BlockState state) {
            return state.getValue(AS);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction.Axis as = context.getHorizontalDirection().getAxis();
            return defaultBlockState().setValue(AS, as).setValue(STROOK, strook(context.getLevel(), context.getClickedPos(), as));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
            Direction.Axis as = state.getValue(AS);
            return direction.getAxis() == andere(as) ? state.setValue(STROOK, strook(level, pos, as)) : state;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            Direction.Axis as = state.getValue(AS);
            boolean kwart = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
            return state.setValue(AS, kwart ? andere(as) : as).setValue(STROOK, gedraaid(as, state.getValue(STROOK), rotation));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.setValue(STROOK, gespiegeld(state.getValue(AS), state.getValue(STROOK), mirror));
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
            return verborgen(state, neighborState, direction) || super.skipRendering(state, neighborState, direction);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            WolkenBlokken.zachtLanden(level, pos, entity, fallDistance);
        }
    }

    public static class Plaat extends SlabBlock implements Deel {
        public static final MapCodec<Plaat> CODEC = simpleCodec(Plaat::new);

        public Plaat(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(AS, Direction.Axis.X).setValue(STROOK, Strook.HEEL));
        }

        @Override
        public MapCodec<? extends SlabBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(AS, STROOK);
        }

        @Override
        public Direction.Axis as(BlockState state) {
            return state.getValue(AS);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (state == null || context.getLevel().getBlockState(context.getClickedPos()).is(this)) {
                return state;   // (a second slab on the first: it keeps its stripes)
            }
            Direction.Axis as = context.getHorizontalDirection().getAxis();
            return state.setValue(AS, as).setValue(STROOK, strook(context.getLevel(), context.getClickedPos(), as));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
            BlockState s = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
            Direction.Axis as = s.getValue(AS);
            return direction.getAxis() == andere(as) ? s.setValue(STROOK, strook(level, pos, as)) : s;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            Direction.Axis as = state.getValue(AS);
            boolean kwart = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
            return state.setValue(AS, kwart ? andere(as) : as).setValue(STROOK, gedraaid(as, state.getValue(STROOK), rotation));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.setValue(STROOK, gespiegeld(state.getValue(AS), state.getValue(STROOK), mirror));
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
            return verborgen(state, neighborState, direction) || super.skipRendering(state, neighborState, direction);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            WolkenBlokken.zachtLanden(level, pos, entity, fallDistance);
        }
    }

    public static class Trap extends StairBlock implements Deel {
        public Trap(BlockState baseState, Properties properties) {
            super(baseState, properties);
            registerDefaultState(defaultBlockState().setValue(STROOK, Strook.HEEL));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(STROOK);
        }

        @Override
        public Direction.Axis as(BlockState state) {
            return state.getValue(FACING).getAxis();
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : state.setValue(STROOK, strook(context.getLevel(), context.getClickedPos(), as(state)));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
            BlockState s = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
            Direction.Axis as = as(s);
            return direction.getAxis() == andere(as) ? s.setValue(STROOK, strook(level, pos, as)) : s;
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            Strook strook = gedraaid(as(state), state.getValue(STROOK), rotation);
            return super.rotate(state, rotation).setValue(STROOK, strook);
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            Strook strook = gespiegeld(as(state), state.getValue(STROOK), mirror);
            return super.mirror(state, mirror).setValue(STROOK, strook);
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
            return verborgen(state, neighborState, direction) || super.skipRendering(state, neighborState, direction);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            WolkenBlokken.zachtLanden(level, pos, entity, fallDistance);
        }
    }

    private RegenboogBlokken() {
    }
}
