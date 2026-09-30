package nl.juiced.guhs.feature.wereldleven;

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
 * The guh-xylofoon: eight coloured bars on a little pink stand with a guh face. Right-click: the xylofoon screen (play
 * the bars with the mouse or the keys 1-8, the liedjesboekje next to it). See {@link Koortje}.
 */
public class XylofoonBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<XylofoonBlock> CODEC = simpleCodec(XylofoonBlock::new);
    private static final VoxelShape X = Shapes.or(Block.box(0, 0, 3, 16, 7, 13), Block.box(1, 7, 2, 15, 9, 14));
    private static final VoxelShape Z = Shapes.or(Block.box(3, 0, 0, 13, 7, 16), Block.box(2, 7, 1, 14, 9, 15));

    public XylofoonBlock(Properties properties) {
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
        // the player stands in front of it: the bars run from left (low) to right (high) as seen by the player
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? X : Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            WereldlevenFeature.openXylofoon.accept(pos);
        }
        return InteractionResult.SUCCESS;
    }
}
