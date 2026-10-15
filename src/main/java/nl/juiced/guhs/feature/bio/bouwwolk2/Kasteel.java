package nl.juiced.guhs.feature.bio.bouwwolk2;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * The measures of the wolkenkasteeltje, all relative to the giant's place (his "thuis": where he lies on his bed) and for the
 * template as it is built (tools/features/bio_bouw_wolk2_bouw.py: x east, z south, the gate in the south wall, the giant's
 * head west). A castle in the world is turned a quarter, a half or three quarters; the giant's own yaw says how
 * ({@link #draai(float)}), so he needs nothing but his own position to know his hall.
 */
public final class Kasteel {
    /** Where the template has the giant (its own coordinates), and the yaw he has there (head west). */
    public static final Vec3 REUS_IN_TEMPLATE = new Vec3(24.0, 104.0, 14.0);
    public static final float REUS_YAW = 90f;
    /** The hall: the inside of the castle, from its floor blocks to above the walls (min inclusive, max exclusive). */
    public static final Vec3 HAL_MIN = new Vec3(-7, -2, -9), HAL_MAX = new Vec3(8, 9, 8);
    /** His head (the ear you must not walk past upright). */
    public static final Vec3 KOP = new Vec3(-2.5, 1.5, 0);
    /** Within this many blocks of his head (level distance) walking upright is noise too. */
    public static final double OOR = 4.6;
    /** Where he blows you to: the cloud in front of the gate. */
    public static final Vec3 LANDING = new Vec3(0.5, -1, 13.5);
    /** The hoard you take the crumb from (a block). */
    public static final BlockPos SCHAT = new BlockPos(0, -1, -6);
    /** The way his sneeze goes (to the gate). */
    public static final Vec3 NAAR_POORT = new Vec3(0, 0, 1);

    /** How the castle of a giant with this yaw is turned. */
    public static Rotation draai(float yaw) {
        return Rotation.values()[Math.floorMod(Math.round(Mth.wrapDegrees(yaw - REUS_YAW) / 90f), 4)];
    }

    /** The yaw of the giant of a castle turned like this. */
    public static float yaw(Rotation draai) {
        return Mth.wrapDegrees(REUS_YAW + 90f * draai.ordinal());
    }

    /** A template direction or offset, turned with the castle. */
    public static Vec3 draai(Vec3 v, Rotation draai) {
        return switch (draai) {
            case CLOCKWISE_90 -> new Vec3(-v.z, v.y, v.x);
            case CLOCKWISE_180 -> new Vec3(-v.x, v.y, -v.z);
            case COUNTERCLOCKWISE_90 -> new Vec3(v.z, v.y, -v.x);
            default -> v;
        };
    }

    /** A place in the world, from its offset to the giant in the template. */
    public static Vec3 wereld(Vec3 thuis, Rotation draai, Vec3 offset) {
        return thuis.add(draai(offset, draai));
    }

    /** A place in the world as an offset to the giant, in the directions of the template. */
    public static Vec3 lokaal(Vec3 thuis, Rotation draai, Vec3 plek) {
        Vec3 d = plek.subtract(thuis);
        return switch (draai) {
            case CLOCKWISE_90 -> new Vec3(d.z, d.y, -d.x);
            case CLOCKWISE_180 -> new Vec3(-d.x, d.y, -d.z);
            case COUNTERCLOCKWISE_90 -> new Vec3(-d.z, d.y, d.x);
            default -> d;
        };
    }

    /** Is this place inside the hall? */
    public static boolean inHal(Vec3 thuis, Rotation draai, Vec3 plek) {
        Vec3 l = lokaal(thuis, draai, plek);
        return l.x >= HAL_MIN.x && l.x < HAL_MAX.x && l.y >= HAL_MIN.y && l.y < HAL_MAX.y && l.z >= HAL_MIN.z && l.z < HAL_MAX.z;
    }

    /** Is this place within earshot of his head (walking upright there wakes him)? */
    public static boolean bijOor(Vec3 thuis, Rotation draai, Vec3 plek) {
        Vec3 l = lokaal(thuis, draai, plek);
        double dx = l.x - KOP.x, dz = l.z - KOP.z;
        return dx * dx + dz * dz < OOR * OOR;
    }

    private Kasteel() {
    }
}
