package nl.juiced.guhs.block;

import java.util.EnumMap;
import java.util.Map;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhSeatEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Guh furniture (chair, table, sofa, bean bags, cushions): turns to face you when placed; the seats can be sat on
 * (right-click with an empty hand).
 */
public class GuhFurnitureBlock extends HorizontalDirectionalBlock {
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    /** Where you sit (blocks above the bottom), or below 0 if it's not a seat. */
    private final double seatHeight;

    /** @param boxes the shape facing north, in pixels: {x0, y0, z0, x1, y1, z1}... */
    public GuhFurnitureBlock(Properties properties, double seatHeight, double[]... boxes) {
        super(properties);
        this.seatHeight = seatHeight;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                shape = Shapes.or(shape, rotated(b, dir));
            }
            shapes.put(dir, shape.optimize());
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape rotated(double[] b, Direction dir) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (dir) {
            case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
            case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
            case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
            default -> Block.box(x0, b[1], z0, x1, b[4], z1);
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new GuhFurnitureBlock(p, seatHeight));
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
        return shapes.get(state.getValue(FACING));
    }

    public boolean isSeat() {
        return seatHeight >= 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isSeat() || player.isSecondaryUseActive() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!level.getEntitiesOfClass(GuhSeatEntity.class, new AABB(pos)).isEmpty()) {
                return InteractionResult.CONSUME; // someone is already sitting here
            }
            GuhSeatEntity seat = ModEntities.GUH_SEAT.get().create(level);
            if (seat != null) {
                seat.moveTo(pos.getX() + 0.5, pos.getY() + seatHeight, pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0);
                level.addFreshEntity(seat);
                player.startRiding(seat);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
