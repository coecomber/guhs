package nl.juiced.guhs.feature.doolhof;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Where a building of the doolhofspelen slice stands in the world: its invisible anchor block (a marker with a facing,
 * under the floor below the NPC) and how the structure was turned. Everything else of the building is at fixed spots of
 * the template (the constants in DoolhofVeld / Speelvelden, mirrored in tools/features/doolhof*.py and knabbelspelen*.py),
 * turned around the anchor the same way the jigsaw placement turned the whole template. In the template the anchor faces
 * north; {@code lx/ly/lz} are template coordinates, {@code (ax, ay, az)} is the anchor's own template position.
 */
public record Anker(BlockPos pos, Rotation rot, int ax, int ay, int az) {
    /** The rotation that turns the template's north into this facing. */
    public static Rotation rotatie(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static Anker van(BlockPos pos, Direction facing, int ax, int ay, int az) {
        return new Anker(pos.immutable(), rotatie(facing), ax, ay, az);
    }

    /** The world block of this template block. */
    public BlockPos blok(int lx, int ly, int lz) {
        return pos.offset(new BlockPos(lx - ax, ly - ay, lz - az).rotate(rot));
    }

    /** A spot (template coordinates, may be fractional) in the world. */
    public Vec3 punt(double lx, double ly, double lz) {
        double dx = lx - ax - 0.5, dz = lz - az - 0.5;
        double[] r = draai(dx, dz, rot);
        return new Vec3(pos.getX() + 0.5 + r[0], pos.getY() + (ly - ay), pos.getZ() + 0.5 + r[1]);
    }

    /** A world spot back in template coordinates. */
    public Vec3 lokaal(Vec3 world) {
        double dx = world.x - pos.getX() - 0.5, dz = world.z - pos.getZ() - 0.5;
        double[] r = draai(dx, dz, terug(rot));
        return new Vec3(ax + 0.5 + r[0], ay + (world.y - pos.getY()), az + 0.5 + r[1]);
    }

    /** A yaw in the template (0 = towards +z) as a world yaw. */
    public float yaw(float lokaalYaw) {
        return lokaalYaw + switch (rot) {
            case CLOCKWISE_90 -> 90f;
            case CLOCKWISE_180 -> 180f;
            case COUNTERCLOCKWISE_90 -> 270f;
            default -> 0f;
        };
    }

    /** A direction of the template in the world. */
    public Direction richting(Direction lokaal) {
        return rot.rotate(lokaal);
    }

    /** A horizontal vector (template) in the world. */
    public Vec3 vector(double lx, double lz) {
        double[] r = draai(lx, lz, rot);
        return new Vec3(r[0], 0, r[1]);
    }

    static Rotation terug(Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> r;
        };
    }

    /** (x, z) turned like BlockPos.rotate (clockwise 90: (x, z) -> (-z, x)). */
    static double[] draai(double x, double z, Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> new double[] {-z, x};
            case CLOCKWISE_180 -> new double[] {-x, -z};
            case COUNTERCLOCKWISE_90 -> new double[] {z, -x};
            default -> new double[] {x, z};
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        tag.putInt("Rot", rot.ordinal());
        return tag;
    }

    public static Anker load(CompoundTag tag, int ax, int ay, int az) {
        return new Anker(BlockPos.of(tag.getLong("Pos")), Rotation.values()[Math.floorMod(tag.getInt("Rot"), Rotation.values().length)], ax, ay, az);
    }
}
