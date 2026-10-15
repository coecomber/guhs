package nl.juiced.guhs.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.redstone.Orientation;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
/**
 * Guhdraad: pink "redstone dust" that carries vadskracht (bbq2; feature/vadskracht). Everything joined by Guhdraad is one
 * net: sources (a Guhrad...), machines (the Guhoven...) and batteries. It looks and connects like redstone dust (dot / line /
 * corner / up a block) and also bends towards every vadskracht block; dark rose while its net stands still, bright glowing
 * pink with sparkles while its net runs ({@link #POWERED}, set by the net: {@code VadsNet}).
 * <p>
 * Redstone INTO the wire does nothing any more (a lever is not a guh). The wire still GIVES a full redstone signal (15) while
 * its net runs, however long it is, so old builds that used a Guhrad as a redstone source keep working.
 */
public class GuhWireBlock extends Block {
    public static final MapCodec<GuhWireBlock> CODEC = simpleCodec(GuhWireBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    /** Tint colours (used by the colour handler in GuhsClient, on top of the vanilla dust textures). */
    public static final int COLOR_OFF = 0x7A2E4A;
    public static final int COLOR_ON = 0xFF5CB8;
    private static final DustParticleOptions SPARKLE = new DustParticleOptions(0xFF73BF /* 1.0, 0.45, 0.75 */, 0.8f);
    /** Safety limit for one connected network. */
    public static final int MAX_NETWORK_SIZE = nl.juiced.guhs.feature.vadskracht.VadsGetallen.MAX_NET;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public GuhWireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE).setValue(SOUTH, RedstoneSide.NONE).setValue(WEST, RedstoneSide.NONE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- placement: like redstone dust it needs something solid underneath ---

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return connectionState(level, pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    /** Wires one step up or down (diagonally) don't get a normal neighbour update, so tell them too. */
    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            for (BlockPos p : new BlockPos[]{side.above(), side.below()}) {
                BlockState other = level.getBlockState(p);
                if (other.is(this)) {
                    BlockState updated = connectionState(level, p, other);
                    if (updated != other) {
                        level.setBlock(p, updated, flags & ~Block.UPDATE_NEIGHBORS, recursionLeft);
                    }
                }
            }
        }
    }

    // --- looks: connect like redstone dust ---

    /** Works out which sides connect (keeping POWERED), exactly like redstone dust does. */
    private BlockState connectionState(BlockGetter level, BlockPos pos, BlockState state) {
        boolean canGoUp = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above());
        RedstoneSide n = side(level, pos, Direction.NORTH, canGoUp);
        RedstoneSide e = side(level, pos, Direction.EAST, canGoUp);
        RedstoneSide s = side(level, pos, Direction.SOUTH, canGoUp);
        RedstoneSide w = side(level, pos, Direction.WEST, canGoUp);
        boolean north = n.isConnected(), east = e.isConnected(), south = s.isConnected(), west = w.isConnected();
        if (!north && !east && !south && !west) {
            // on its own: a little cross, like a single piece of redstone
            n = e = s = w = RedstoneSide.SIDE;
        } else if (!east && !west) {
            // only north/south: make it a straight line
            if (!north) n = RedstoneSide.SIDE;
            if (!south) s = RedstoneSide.SIDE;
        } else if (!north && !south) {
            if (!east) e = RedstoneSide.SIDE;
            if (!west) w = RedstoneSide.SIDE;
        }
        return state.setValue(NORTH, n).setValue(EAST, e).setValue(SOUTH, s).setValue(WEST, w);
    }

    private RedstoneSide side(BlockGetter level, BlockPos pos, Direction dir, boolean canGoUp) {
        BlockPos next = pos.relative(dir);
        BlockState nextState = level.getBlockState(next);
        if (canGoUp && nextState.isFaceSturdy(level, next, Direction.UP) && isWire(level.getBlockState(next.above()))) {
            return nextState.isFaceSturdy(level, next, dir.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
        }
        if (isWire(nextState) || nextState.canRedstoneConnectTo(level, next, dir) || nextState.is(VadsKracht.TOON)) {
            return RedstoneSide.SIDE;
        }
        if (!nextState.isRedstoneConductor(level, next) && isWire(level.getBlockState(next.below()))) {
            return RedstoneSide.SIDE;
        }
        return RedstoneSide.NONE;
    }

    private boolean isWire(BlockState state) {
        return state.is(this);
    }

    /** Pink sparkles while powered (like redstone dust's red dust particles). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED) && random.nextInt(3) == 0) {
            level.addParticle(SPARKLE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.08,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0, 0);
        }
    }

    // --- redstone output ---

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** Strongly powers the block it lies on (just like redstone dust), so power also goes "through" that block. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP ? getSignal(state, level, pos, direction) : 0;
    }

    // --- the vadskracht net (feature/vadskracht keeps it; the wire only says when something changed) ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            // also when placed by commands/structures: work out the dust shape
            BlockState shaped = connectionState(level, pos, state);
            if (shaped != state) {
                level.setBlock(pos, shaped, Block.UPDATE_CLIENTS);
            }
            VadsKracht.veranderd(level, pos);
        }
    }

    /** A neighbour changed: a machine may have appeared or disappeared next to the wire. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            VadsKracht.buurVeranderd(server, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // 26.1: was onRemove (only called when the block really changed, on the server)
        // the wire that was here may have been powering the block below; the rest of its net is rebuilt
        level.updateNeighborsAt(pos.below(), this);
        VadsKracht.veranderd(level, pos);
    }

    /** Wires connect to wires next to, above, below, and diagonally up/down a step (like dust going up stairs). The first 6 are the faces, in Direction order. */
    public static List<BlockPos> connections(BlockPos pos) {
        List<BlockPos> list = new ArrayList<>(14);
        for (Direction dir : Direction.values()) {
            list.add(pos.relative(dir));
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            list.add(pos.relative(dir).above());
            list.add(pos.relative(dir).below());
        }
        return list;
    }
}
