package nl.juiced.guhs.feature.guhpixel.blok;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A decoration that hangs on a wall (framed pictures, signs, stickers): FACING is the side it looks at, the wall is
 * behind it; it pops off when the wall goes. The shape is given for facing north (so it lies against the south side of
 * its block: z 14..16). Souvenir "paintings" are these blocks, NOT painting variants (a variant in the placeable tag is
 * also handed out by plain vanilla paintings).
 */
public class MuurDecoBlock extends DecoBlock {
    public MuurDecoBlock(Properties properties, VoxelShape noord) {
        super(properties, noord);
    }

    /** The usual properties of a wall decoration: no collision, breaks at once. */
    public static Properties props() {
        return DecoBlock.props().noCollision();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos muur = pos.relative(facing.getOpposite());
        return level.getBlockState(muur).isFaceSturdy(level, muur, facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction kijk : context.getNearestLookingDirections()) {
            if (kijk.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, kijk.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction richting,
                                     BlockPos buurPos, BlockState buur, RandomSource random) {
        return richting == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, richting, buurPos, buur, random);
    }
}
