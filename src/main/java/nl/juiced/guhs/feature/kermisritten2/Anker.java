package nl.juiced.guhs.feature.kermisritten2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Local coordinates around a ride's anchor block. The kermis stamp turns with its template (kermis C stands 90 degrees
 * round), and the anchor's facing turns with it: everything a ride knows about its building is written for the facing the
 * design has at kermis A, and goes through here. (dx, dz) = east and south in that design.
 */
final class Anker {
    private Anker() {
    }

    /** The turn that takes the design's facing to this anchor's facing. */
    static Rotation draai(Direction ontwerp, Direction nu) {
        for (Rotation r : Rotation.values()) {
            if (r.rotate(ontwerp) == nu) {
                return r;
            }
        }
        return Rotation.NONE;
    }

    /** A block offset in the design, in the world. */
    static BlockPos blok(BlockPos anker, Rotation r, int dx, int dy, int dz) {
        return anker.offset(new BlockPos(dx, dy, dz).rotate(r));
    }

    /** A point in the design (0, 0 = the middle of the anchor block), in the world. */
    static Vec3 punt(BlockPos anker, Rotation r, double dx, double dy, double dz) {
        double x = dx, z = dz;
        for (int i = 0; i < stappen(r); i++) {
            double t = x;       // clockwise, seen from above
            x = -z;
            z = t;
        }
        return new Vec3(anker.getX() + 0.5 + x, anker.getY() + dy, anker.getZ() + 0.5 + z);
    }

    private static int stappen(Rotation r) {
        return switch (r) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
    }
}
