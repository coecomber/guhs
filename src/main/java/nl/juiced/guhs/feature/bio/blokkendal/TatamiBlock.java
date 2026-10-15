package nl.juiced.guhs.feature.bio.blokkendal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Tatami: a floor block of woven rush with a cloth border along its two long sides. One block is a half mat (the weave runs
 * the way you looked, a seam at both ends). Put a second one next to a half mat and the two become ONE mat of 1 x 2: the seam between them
 * goes and the weave and the borders run along the whole mat. That is all a classic room needs: lay the mats two by two, each pair the way you
 * face, and finish with a half mat (sneak to keep a block a half mat next to another one).
 * <p>FACING: for a paired block the direction of its other half, for a half mat the direction of the weave.
 */
public class TatamiBlock extends Block {
    public static final MapCodec<TatamiBlock> CODEC = simpleCodec(TatamiBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty GEKOPPELD = BooleanProperty.create("gekoppeld");

    public TatamiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GEKOPPELD, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GEKOPPELD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction kijk = context.getHorizontalDirection();
        if (!context.isSecondaryUseActive()) {
            // a half mat beside me becomes my other half: first the one I look along, then the others
            for (Direction d : new Direction[] {kijk, kijk.getOpposite(), kijk.getClockWise(), kijk.getCounterClockWise()}) {
                BlockState other = level.getBlockState(pos.relative(d));
                if (other.is(this) && !other.getValue(GEKOPPELD)) {
                    return defaultBlockState().setValue(FACING, d).setValue(GEKOPPELD, true);
                }
            }
        }
        return defaultBlockState().setValue(FACING, kijk);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighbor, RandomSource random) {
        if (direction.getAxis().isHorizontal()) {
            boolean wijstNaarMij = neighbor.is(this) && neighbor.getValue(GEKOPPELD) && neighbor.getValue(FACING) == direction.getOpposite();
            if (state.getValue(GEKOPPELD)) {
                if (direction == state.getValue(FACING) && !wijstNaarMij) {
                    return state.setValue(GEKOPPELD, false);        // my other half is gone: a half mat again
                }
            } else if (wijstNaarMij) {
                return state.setValue(FACING, direction).setValue(GEKOPPELD, true);
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
