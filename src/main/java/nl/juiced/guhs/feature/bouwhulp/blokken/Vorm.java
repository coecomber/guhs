package nl.juiced.guhs.feature.bouwhulp.blokken;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shapes drawn facing north (in pixels, like the models), turned to the four sides; and where a click landed. */
final class Vorm {
    /** {x0, y0, z0, x1, y1, z1}... facing north -> the shape for every horizontal facing (as the blockstates turn the models). */
    static Map<Direction, VoxelShape> draai(double[]... boxes) {
        Map<Direction, VoxelShape> uit = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (double[] b : boxes) {
                s = Shapes.or(s, box(b, dir));
            }
            uit.put(dir, s.optimize());
        }
        return uit;
    }

    static VoxelShape box(double[] b, Direction dir) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (dir) {
            case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
            case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
            case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
            default -> Block.box(x0, b[1], z0, x1, b[4], z1);
        };
    }

    /** Where in the new block the click landed (0..1 on each axis). */
    static Vec3 klik(BlockPlaceContext context) {
        BlockPos p = context.getClickedPos();
        return context.getClickLocation().subtract(p.getX(), p.getY(), p.getZ());
    }

    /** Against a wall: the clicked side when it is a wall, else facing the player. */
    static Direction tegenMuur(BlockPlaceContext context) {
        Direction f = context.getClickedFace();
        return f.getAxis().isHorizontal() ? f : context.getHorizontalDirection().getOpposite();
    }

    private Vorm() {
    }
}
