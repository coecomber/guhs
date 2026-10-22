package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Where the brooks of the Beverbeek run: a pure function of the world seed and x, z, so every chunk agrees with its
 * neighbours without looking at them. A brook is the zero line of a slow wavy noise (with a faster one on top for the
 * meanders): {@link #waarde} near 0 is water. The lines never end and never cross, so the brooks wind through the whole
 * forest; where the land steps down, the water tumbles down a little waterfall by itself (it is real, flowing water).
 */
public final class Beek {
    /**
     * Half the width of a brook in blocks (the distance to the zero line): 1.6.2 it swells and narrows along the brook
     * between {@link #BREED_MIN} and {@link #BREED_MAX} (2-5 blocks wide, a small river in the wide stretches); the middle
     * {@link #DIEP_DEEL} of it is two deep.
     */
    public static final double BREED_MIN = 1.1, BREED_MAX = 2.6, DIEP_DEEL = 0.45;
    private static final double SCHAAL = 120, KRONKEL = 34, KRONKEL_DEEL = 0.32, BREED_SCHAAL = 90;
    private static final long ZOUT = 20261021L;

    private static volatile Ruis ruis;

    private record Ruis(long seed, SimplexNoise traag, SimplexNoise snel, SimplexNoise breed) {
    }

    private static Ruis ruis(long seed) {
        Ruis r = ruis;
        if (r == null || r.seed() != seed) {
            RandomSource rnd = new LegacyRandomSource(seed ^ ZOUT);
            r = new Ruis(seed, new SimplexNoise(rnd), new SimplexNoise(rnd), new SimplexNoise(rnd));
            ruis = r;
        }
        return r;
    }

    /** The noise at x, z (about -1..1); the brooks are where it is near 0. */
    public static double waarde(long seed, int x, int z) {
        Ruis r = ruis(seed);
        return r.traag().getValue(x / SCHAAL, z / SCHAAL) + KRONKEL_DEEL * r.snel().getValue(x / KRONKEL, z / KRONKEL);
    }

    /**
     * About how many blocks x, z lies from the middle of a brook: the noise divided by its slope, so a brook is equally
     * wide everywhere (the plain noise would swell into ponds where it is flat near 0).
     */
    public static double afstand(long seed, int x, int z) {
        double v = waarde(seed, x, z);
        if (Math.abs(v) > 0.2) {
            return 99;                                   // (far from any brook: no slope needed)
        }
        double gx = (waarde(seed, x + 1, z) - waarde(seed, x - 1, z)) * 0.5, gz = (waarde(seed, x, z + 1) - waarde(seed, x, z - 1)) * 0.5;
        return Math.abs(v) / Math.max(Math.sqrt(gx * gx + gz * gz), 1e-4);
    }

    /** Half the brook's width at x, z (1.6.2: it varies slowly along the brook). */
    public static double breed(long seed, int x, int z) {
        double t = 0.5 + 0.5 * ruis(seed).breed().getValue(x / BREED_SCHAAL, z / BREED_SCHAAL);
        return BREED_MIN + (BREED_MAX - BREED_MIN) * Math.max(0, Math.min(1, t));
    }

    /** 0 = dry land, 1 = a shallow brook, 2 = the deep middle of a brook. */
    public static int diepte(long seed, int x, int z) {
        double d = afstand(seed, x, z);
        if (d > BREED_MAX) {
            return 0;
        }
        double b = breed(seed, x, z);
        return d < b * DIEP_DEEL ? 2 : d < b ? 1 : 0;
    }

    private Beek() {
    }
}
