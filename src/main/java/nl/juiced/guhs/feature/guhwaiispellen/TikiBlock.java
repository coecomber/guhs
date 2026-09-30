package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.EnumMap;
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
 * A little Tiki decoration that faces you when you put it down (the masks, the statue, the board rack, the garland, the
 * bar stool, the radio): its shape is given for facing north (pixels) and turned for the other sides. Sold for
 * schelpjesmunten at Tikiguh's stall on the surf beach of Guhwai'i.
 */
public class TikiBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TikiBlock> CODEC = simpleCodec(p -> new TikiBlock(p, new double[][]{{0, 0, 0, 16, 16, 16}}, false));

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    private final boolean wand;

    /**
     * @param boxes the shape facing north: {x0, y0, z0, x1, y1, z1} in pixels
     * @param wand  hangs on a wall: it faces away from the wall you click (the masks, the garland)
     */
    public TikiBlock(Properties properties, double[][] boxes, boolean wand) {
        super(properties);
        this.wand = wand;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : boxes) {
                double[] r = turn(b, d);
                s = Shapes.or(s, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
            }
            shapes.put(d, s);
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** A box given for north, turned to face d (around the block's middle). */
    static double[] turn(double[] b, Direction d) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (d) {
            case SOUTH -> new double[]{16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0};
            case EAST -> new double[]{16 - z1, b[1], x0, 16 - z0, b[4], x1};
            case WEST -> new double[]{z0, b[1], 16 - x1, z1, b[4], 16 - x0};
            default -> b;
        };
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
        Direction face = context.getClickedFace();
        if (wand && face.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, face);
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }
}
