package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bioscoopdoek: a thin white cloth at the back of its block; FACING is the side the picture is on. Build a rectangle of
 * them (up to 7 wide and 4 high) and a projector that looks at it finds it by itself ({@link Doek#zoek}); the blocks
 * themselves know nothing (no block entity, nothing to clean up). Placed against the side of another cloth block it turns
 * the same way, so a screen is quick to build.
 */
public class DoekBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DoekBlock> CODEC = simpleCodec(DoekBlock::new);
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(Block.box(0, 0, 14, 16, 16, 16));

    public DoekBlock(Properties properties) {
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
        BlockPos tegen = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState buur = context.getLevel().getBlockState(tegen);
        if (buur.getBlock() instanceof DoekBlock && context.getClickedFace().getAxis() != buur.getValue(FACING).getAxis()) {
            return defaultBlockState().setValue(FACING, buur.getValue(FACING));
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }
}
