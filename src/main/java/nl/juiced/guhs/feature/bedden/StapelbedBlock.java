package nl.juiced.guhs.feature.bedden;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Stapelbedje (1.8.0): two halves, a sleeping spot in each (both in {@code #guhs:guh_slaapplekken}): at night one guh sleeps
 * below and one on top (it climbs the little ladder: SlaapplekGoal lifts it up). Placed and broken like a door.
 */
public class StapelbedBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<StapelbedBlock> CODEC = simpleCodec(StapelbedBlock::new);
    /** Each half: the bunk's mattress is what you stand on; the posts and rails are only for the eye. */
    private static final VoxelShape MATRAS = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape OMTREK = Block.box(0, 0, 0, 16, 16, 16);

    public StapelbedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** Is this the lower bunk (whose spot has the upper bunk right above it)? */
    public static boolean isOnder(BlockState s) {
        return s.getBlock() instanceof StapelbedBlock && s.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos p = context.getClickedPos();
        Level level = context.getLevel();
        if (p.getY() >= level.getMaxY() || !level.getBlockState(p.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if ((dir == Direction.UP && half == DoubleBlockHalf.LOWER) || (dir == Direction.DOWN && half == DoubleBlockHalf.UPPER)) {
            return neighbour.is(this) && neighbour.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos onder = pos.below();
            if (level.getBlockState(onder).is(this)) {
                level.setBlock(onder, Blocks.AIR.defaultBlockState(), 35);   // (the lower half carries the drop: break it quietly)
                if (!player.preventsBlockDrops()) {
                    Block.dropResources(state.setValue(HALF, DoubleBlockHalf.LOWER), level, onder, null, player, player.getMainHandItem());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OMTREK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MATRAS;
    }
}
