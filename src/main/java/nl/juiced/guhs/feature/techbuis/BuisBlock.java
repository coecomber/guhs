package nl.juiced.guhs.feature.techbuis;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De Knabbelbuis ({@code guhs:knabbelbuis}): a see-through tube that things roll through. It grows an arm towards every
 * tube next to it, towards the front and back of a Richtingstuk or Filterstuk, and towards everything that holds items (a
 * chest, a machine, the Bank Guh, a Hapluikje: any item capability). The tube itself does nothing and knows nothing: no
 * block entity, no ticking. What moves the items are the pieces ({@link BuisStukBlock}); see {@link Buizen}.
 */
public class BuisBlock extends PipeBlock {
    public static final MapCodec<BuisBlock> CODEC = simpleCodec(BuisBlock::new);
    /** The tube is 8 pixels thick. */
    public static final float DIKTE = 8f;

    public BuisBlock(Properties properties) {
        super(DIKTE, properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** The arms this tube has here. */
    public static BlockState armen(Level level, BlockPos pos, BlockState state) {
        for (Direction kant : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(kant), Buizen.verbindt(level, pos, kant));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return armen(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!(level instanceof Level echt)) {
            return state;   // (world generation: the arms are worked out when the tube is really in the world, onPlace)
        }
        boolean arm = Buizen.verbindt(echt, pos, direction);
        if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) != arm) {
            Buizen.veranderd(echt);
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), arm);
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            // also when placed by a command or a structure: work out the arms
            BlockState goed = armen(level, pos, state);
            if (goed != state) {
                level.setBlock(pos, goed, Block.UPDATE_CLIENTS);
            }
            Buizen.veranderd(level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Buizen.veranderd(level);
    }

    // --- glass: light and sight go through ---

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1f;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
