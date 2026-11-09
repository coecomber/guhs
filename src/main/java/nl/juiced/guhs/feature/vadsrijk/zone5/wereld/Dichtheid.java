package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

/**
 * W-LEVEN: a fixed, position-based choice of which decorative figures show (Z5Bewoners, Z6Bewoners): the same figures for every
 * player and after every restart, no flicker when they respawn, and independent of the order of the generated list (a regen that
 * adds or drops figures elsewhere does not reshuffle the rest).
 */
public final class Dichtheid {
    private Dichtheid() {
    }

    /** A number in [0, 1) that belongs to this spot (half-block grid). */
    public static double getal(double x, double y, double z) {
        long h = Math.round(x * 2) * 0x9E3779B97F4A7C15L ^ Math.round(y * 2) * 0xC2B2AE3D27D4EB4FL ^ Math.round(z * 2) * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Does the figure at this spot show at this density (0..1)? */
    public static boolean blijft(double x, double y, double z, double dichtheid) {
        return getal(x, y, z) < dichtheid;
    }
}
