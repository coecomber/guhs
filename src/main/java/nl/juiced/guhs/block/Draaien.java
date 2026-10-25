package nl.juiced.guhs.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 1.7.0: helpers for {@code rotate(BlockState, Rotation)} / {@code mirror(BlockState, Mirror)} of our own block classes, so a
 * structure template placed turned or mirrored turns its blocks along ({@code Block.rotate} / {@code Block.mirror} keep the
 * state as it is). Same semantics as vanilla: facings turn with the rotation and flip with the mirror, a horizontal axis
 * swaps on a quarter turn, side properties (north/east/south/west) move round with the sides.
 */
public final class Draaien {
    private Draaien() {
    }

    public static BlockState facing(BlockState state, EnumProperty<Direction> facing, Rotation rotation) {
        return state.setValue(facing, rotation.rotate(state.getValue(facing)));
    }

    public static BlockState facing(BlockState state, EnumProperty<Direction> facing, Mirror mirror) {
        return state.setValue(facing, mirror.mirror(state.getValue(facing)));
    }

    /** X and Z swap on a quarter turn (Y stays); a mirror leaves an axis as it is. */
    public static BlockState as(BlockState state, EnumProperty<Direction.Axis> as, Rotation rotation) {
        if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90) {
            return state;
        }
        Direction.Axis a = state.getValue(as);
        return a == Direction.Axis.Y ? state : state.setValue(as, a == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    /** The values of the horizontal sides move to the turned sides (up/down, if in the map, stay). */
    public static <T extends Comparable<T>> BlockState kanten(BlockState state, Map<Direction, ? extends Property<T>> kanten, Rotation rotation) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            out = out.setValue(kanten.get(rotation.rotate(d)), state.getValue(kanten.get(d)));
        }
        return out;
    }

    /** The values of the horizontal sides move to the mirrored sides. */
    public static <T extends Comparable<T>> BlockState kanten(BlockState state, Map<Direction, ? extends Property<T>> kanten, Mirror mirror) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            out = out.setValue(kanten.get(mirror.mirror(d)), state.getValue(kanten.get(d)));
        }
        return out;
    }

    /**
     * A part of a multi-block that stores its offset to the controller as dx/dz with {@code midden} meaning 0 (like
     * MachineDeelBlock): the offset turns along, so the part still points at its controller.
     */
    public static BlockState offset(BlockState state, IntegerProperty dx, IntegerProperty dz, int midden, Rotation rotation) {
        BlockPos d = new BlockPos(state.getValue(dx) - midden, 0, state.getValue(dz) - midden).rotate(rotation);
        return state.setValue(dx, d.getX() + midden).setValue(dz, d.getZ() + midden);
    }

    public static BlockState offset(BlockState state, IntegerProperty dx, IntegerProperty dz, int midden, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(dz, 2 * midden - state.getValue(dz));
            case FRONT_BACK -> state.setValue(dx, 2 * midden - state.getValue(dx));
            default -> state;
        };
    }

    /**
     * A row of {@code lengte} parts along a horizontal axis, numbered 0.. towards +X (axis X) or +Z (axis Z) from its first
     * part: after the turn/mirror, the row may run the other way, then the numbers go the other way too (so part 0 is again
     * the first part at the low end and the parts still find each other).
     */
    public static BlockState rij(BlockState state, EnumProperty<Direction.Axis> as, IntegerProperty nr, int lengte, Direction langs) {
        BlockState out = state.setValue(as, langs.getAxis());
        return langs.getAxisDirection() == Direction.AxisDirection.POSITIVE ? out : out.setValue(nr, lengte - 1 - state.getValue(nr));
    }

    /** The positive direction along a horizontal axis. */
    public static Direction langs(Direction.Axis as) {
        return as == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }
}
