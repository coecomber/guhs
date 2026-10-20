package nl.juiced.guhs.feature.safari.momenten;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The schemabord of the village square (S3, block {@code guhs:safari_schemabord}): a notice board on two posts. A click
 * shows today's plan in the chat: the feeding times, the ranger talks, a night safari, and whose birthday it is
 * ({@link Momenten#schema}).
 */
public class SchemabordBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SchemabordBlock> CODEC = simpleCodec(SchemabordBlock::new);
    private static final VoxelShape NS = Shapes.or(Block.box(0, 0, 6, 16, 24, 10));
    private static final VoxelShape EW = Shapes.or(Block.box(6, 0, 0, 10, 24, 16));

    public SchemabordBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Momenten.schema(level));
        }
        return InteractionResult.SUCCESS;
    }
}
