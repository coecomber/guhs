package nl.juiced.guhs.feature.dromen;

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
 * A place to tuck a guh in (tag {@link DromenFeature#SLAAPPLEKKEN}): the doos met knabbels (a low open cardboard box with
 * kaasknabbels in the corner) and the kattenhuisje (a little house with a round door and guh ears on the roof). The
 * tucking itself is {@link DroomSlapen}; the block is only its shape and which way it faces.
 */
public class SlaapplekBlock extends HorizontalDirectionalBlock {
    /** The open box: a floor and four low walls (a guh lies IN it). */
    public static final VoxelShape DOOS = Shapes.or(Block.box(1, 0, 1, 15, 1, 15), Block.box(1, 1, 1, 15, 8, 2), Block.box(1, 1, 14, 15, 8, 15),
            Block.box(1, 1, 2, 2, 8, 14), Block.box(14, 1, 2, 15, 8, 14));
    /** The kattenhuisje: almost a whole block (its roof ears stick out a little in the model only). */
    public static final VoxelShape KATTENHUISJE = Block.box(1, 0, 1, 15, 14, 15);

    public static final MapCodec<SlaapplekBlock> CODEC = simpleCodec(p -> new SlaapplekBlock(p, DOOS));

    private final VoxelShape vorm;

    public SlaapplekBlock(Properties properties, VoxelShape vorm) {
        super(properties);
        this.vorm = vorm;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** How high above the block's floor a guh lies in it (in the box: on its floor; on the kattenhuisje's cushion inside). */
    public double lighoogte() {
        return vorm == DOOS ? 0.07 : 0.1;
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
        return vorm;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm == DOOS ? DOOS : Block.box(1, 0, 1, 15, 2, 15);   // (a guh walks into the kattenhuisje; only its floor is solid)
    }
}
