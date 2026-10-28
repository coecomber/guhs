package nl.juiced.guhs.feature.bedden;

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
 * A little critter bed of one block (the Luciferdoosje for pieppiepmuisjes, the Schelpbedje for Schilly and Poepschilly):
 * only its shape and which way it faces (the headboard away from the player); who sleeps where is {@link Vakjes}.
 */
public class DierBedBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DierBedBlock> CODEC = simpleCodec(p -> new DierBedBlock(p, Shapes.block()));

    private final Map<Direction, VoxelShape> vorm;

    public DierBedBlock(Properties properties, VoxelShape noord) {
        super(properties);
        this.vorm = Shapes.rotateHorizontal(noord);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** A shape from boxes {x0, y0, z0, x1, y1, z1, ...} (pixels, drawn facing north). */
    public static VoxelShape box(double... b) {
        VoxelShape s = Shapes.empty();
        for (int i = 0; i + 5 < b.length; i += 6) {
            s = Shapes.or(s, Block.box(b[i], b[i + 1], b[i + 2], b[i + 3], b[i + 4], b[i + 5]));
        }
        return s;
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm.get(state.getValue(FACING));
    }
}
