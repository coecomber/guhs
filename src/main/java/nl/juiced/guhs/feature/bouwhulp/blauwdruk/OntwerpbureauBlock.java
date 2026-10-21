package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
 * The Ontwerpbureau (design 2.2/2.6; not "Tekentafel", that name is taken): a drawing desk with a blue sheet on it.
 * Right-click: the blueprint browser (own, catalogue, the server's library), where you print a blueprint to carry around
 * (a sheet of paper), give thumbs up and tidy your own. Publishing happens in a Bouwkeet (from a Sauskeet).
 */
public class OntwerpbureauBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<OntwerpbureauBlock> CODEC = simpleCodec(OntwerpbureauBlock::new);
    private static final VoxelShape VORM = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(1, 0, 1, 3, 12, 3), Block.box(13, 0, 1, 15, 12, 3),
            Block.box(1, 0, 13, 3, 12, 15), Block.box(13, 0, 13, 15, 12, 15));

    public OntwerpbureauBlock(Properties properties) {
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
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            Blauwdrukken.openBureau(p, true);
        }
        return InteractionResult.SUCCESS;
    }
}
