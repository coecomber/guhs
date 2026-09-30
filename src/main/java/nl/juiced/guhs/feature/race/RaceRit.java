package nl.juiced.guhs.feature.race;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A scripted ride of a race guh (2.9, the Vadsbaan's looping): from the entrance the guh goes once around a loop that
 * moves {@link #length} forward and {@link #side} to the right while it turns (a corkscrew, so the way out lies next to
 * the way in), in {@link #ticks} ticks. Both sides work out the same path from these numbers (the rider's own game moves
 * the guh, the server follows along), so it needs no packets but the start.
 * <p>
 * The guh's feet follow a circle of {@link #radius} around the loop's middle that shrinks towards the top (by
 * {@link #DROP}): the loop's road is a ring of radius + 1 around the same middle, and at the top the guh and its rider
 * hang under it instead of poking through (tools/features/circuit_banen.py builds the ring from the same formula).
 */
public record RaceRit(Vec3 entrance, Direction forward, double radius, double length, double side, int ticks) {
    /** How much closer to the middle the path is at the top of the loop (room for the guh and its rider under the road). */
    public static final double DROP = 3.6;

    /** Where the guh's feet are at t (0 = the entrance, 1 = the way out). */
    public Vec3 pos(double t) {
        double a = Mth.TWO_PI * t;
        Direction right = forward.getClockWise();
        double rho = radius - DROP * (1 - Math.cos(a)) / 2;
        double f = length * t + rho * Math.sin(a);
        double up = radius - rho * Math.cos(a);
        double s = side * t;
        return entrance.add(forward.getStepX() * f + right.getStepX() * s, up, forward.getStepZ() * f + right.getStepZ() * s);
    }

    /** The way out (t = 1). */
    public Vec3 exit() {
        return pos(1);
    }

    /** Where the guh is after this many ticks of the ride. */
    public Vec3 at(int tick) {
        return pos(Math.min(1.0, Math.max(0, tick) / (double) ticks));
    }

    public float yaw() {
        return forward.toYRot();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", entrance.x);
        tag.putDouble("Y", entrance.y);
        tag.putDouble("Z", entrance.z);
        tag.putInt("Forward", forward.get2DDataValue());
        tag.putDouble("Radius", radius);
        tag.putDouble("Length", length);
        tag.putDouble("Side", side);
        tag.putInt("Ticks", ticks);
        return tag;
    }

    /** A ride from its saved tag, or null for an empty tag (no ride). */
    @javax.annotation.Nullable
    public static RaceRit load(CompoundTag tag) {
        if (!tag.contains("Ticks")) {
            return null;
        }
        return new RaceRit(new Vec3(tag.getDoubleOr("X", 0.0), tag.getDoubleOr("Y", 0.0), tag.getDoubleOr("Z", 0.0)), Direction.from2DDataValue(tag.getIntOr("Forward", 0)),
                tag.getDoubleOr("Radius", 0.0), tag.getDoubleOr("Length", 0.0), tag.getDoubleOr("Side", 0.0), Math.max(1, tag.getIntOr("Ticks", 0)));
    }
}
