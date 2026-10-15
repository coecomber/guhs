package nl.juiced.guhs.feature.techbuis;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A straight piece of tube with an arrow ({@link #FACING}: where the arrow points, any of the six directions): the
 * Richtingstuk ({@link RichtingBlock}) and the Filterstuk ({@link FilterBlock}). Things only go in at its back and out at its
 * front. With something that holds items behind it the piece takes items out of it and sends them on; between tubes it is
 * a one-way valve. A redstone signal locks it (like a hopper). The work is done by {@link BuisStukBlockEntity}.
 * <p>
 * Placing: the arrow points away from the block you click on (click a chest: the piece empties the chest); sneak while
 * placing and it points into it (a filter in front of a chest). Sneak + use with an empty hand turns a placed piece around.
 */
public abstract class BuisStukBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** A piece is a bit thicker than a tube (10 pixels), so you see where it sits. */
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateAll(Block.boxZ(10, 0, 16));

    protected BuisStukBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction weg = context.getClickedFace();
        return defaultBlockState().setValue(FACING, context.isSecondaryUseActive() ? weg.getOpposite() : weg);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Turns the piece around (sneak + use). */
    protected void draaiOm(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        if (level.getBlockEntity(pos) instanceof BuisStukBlockEntity stuk) {
            stuk.gedraaid();
        }
        Buizen.veranderd(level);
    }

    // --- the tubes: tell them when this piece or what it touches changes ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) || oldState.getValue(FACING) != state.getValue(FACING)) {
            Buizen.veranderd(level);   // (not for a face that changes)
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Buizen.veranderd(level);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (level instanceof Level echt && direction.getAxis() == state.getValue(FACING).getAxis() && !Buizen.isStuk(neighborState)) {
            Buizen.veranderd(echt);   // what is in front of or behind the piece changed (a piece tells it itself)
        }
        return state;
    }

    /** A neighbour changed: look again whether a redstone signal locks the piece. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BuisStukBlockEntity stuk) {
            stuk.kijkSlot();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l, pos, s, be) -> {
            if (be instanceof BuisStukBlockEntity stuk) {
                stuk.serverTick();
            }
        };
    }
}
