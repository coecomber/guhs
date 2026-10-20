package nl.juiced.guhs.feature.safari.spullen;

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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin decorative block that faces the player who places it: the bamboe-omheining (a bamboo screen) and the
 * dierentuinbordje (a zoo sign on a post). Purely decorative, no menu.
 */
public class DunBlok extends HorizontalDirectionalBlock {
    public static final MapCodec<DunBlok> CODEC = simpleCodec(p -> new DunBlok(p, 6, 10));

    private final VoxelShape noordZuid;
    private final VoxelShape oostWest;

    /** z0..z1: the thickness of the block (in pixels) when it faces north or south. */
    public DunBlok(Properties properties, double z0, double z1) {
        super(properties);
        this.noordZuid = Block.box(0, 0, z0, 16, 16, z1);
        this.oostWest = Block.box(z0, 0, 0, z1, 16, 16);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? noordZuid : oostWest;
    }
}
