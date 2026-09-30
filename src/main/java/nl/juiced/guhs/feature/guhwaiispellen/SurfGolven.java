package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The waves of one surf game at the surf beach of Guhwai'i (3.0): a set of swells that roll in from the sea to the
 * beach, grow, and start to break at their peak, the break "peeling" along the crest to one side. Everything is a pure
 * function of the game's seed, level and step (ticks since the start), so the server, the surfer's own game (it runs the
 * ride itself, see {@link SurfSim}), the other players' wave renderer and the tests all see exactly the same waves.
 * <p>
 * Coordinates of the surf spot: {@code u} = blocks from the beach out to sea, {@code v} = blocks along the beach. A wave
 * is a crest line parallel to the beach at {@code u = crest(k, step)}; its front face (towards the beach, u below the
 * crest) is steep, its back gentle ({@link #hoogte}).
 */
public final class SurfGolven {
    /** Where the waves come from, where they start to break, where you wait for them and where they die on the beach. */
    public static final double U_START = 56, U_BREEK = 39, U_LINE = 38, U_EIND = 4;
    /** Half the width of a wave along the beach. */
    public static final double HALF = 26;
    /** The first wave starts at this step (the countdown: "3, 2, 1, surfen!"). */
    public static final int EERSTE = 60;

    /** A level of the surf game. */
    public record Stand(Niveau niveau, int golven, int tussen, double snelheid, double amp, double pel, double vangen, double landen,
                        boolean tube, boolean vanzelf) {
        /** The steps a wave takes from the sea to the beach. */
        public int reis() {
            return (int) Math.ceil((U_START - U_EIND) / snelheid);
        }

        /** The whole game (the last wave on the beach plus a little breather). */
        public int duur() {
            return EERSTE + (golven - 1) * tussen + reis() + 60;
        }
    }

    /**
     * makkelijk: small slow waves, you catch them by yourself, lenient landings, no tube; medium: you paddle in (W) at the
     * right moment, the pocket curls into a tube; lastig: big fast waves that break quickly, strict landings.
     */
    public static Stand stand(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> new Stand(niveau, 5, 230, 0.17, 1.5, 0.16, 3.2, 75, false, true);
            case MEDIUM -> new Stand(niveau, 6, 205, 0.20, 2.1, 0.22, 3.0, 55, true, false);
            case LASTIG -> new Stand(niveau, 7, 185, 0.23, 2.8, 0.28, 2.3, 40, true, false);
        };
    }

    /** One wave: when it starts out at sea, where its peak is (along the beach) and to which side the break peels. */
    public record Golf(int index, int start, double piek, int kant) {
    }

    private final Stand stand;
    private final List<Golf> golven = new ArrayList<>();

    public SurfGolven(Niveau niveau, long seed) {
        this.stand = stand(niveau);
        long r = seed ^ 0x5DEECE66DL;
        for (int k = 0; k < stand.golven; k++) {
            r = next(r);
            double piek = ((r >>> 16) % 2001) / 1000.0 * 9 - 9;           // -9 .. 9
            r = next(r);
            int kant = ((r >>> 20) & 1) == 0 ? 1 : -1;
            golven.add(new Golf(k, EERSTE + k * stand.tussen, piek, kant));
        }
    }

    private static long next(long r) {
        return (r * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    }

    public Stand stand() {
        return stand;
    }

    public List<Golf> golven() {
        return golven;
    }

    public Golf golf(int k) {
        return golven.get(k);
    }

    /** Is wave k out on the water at this step (from its start until it dies on the beach)? */
    public boolean actief(int k, double step) {
        Golf g = golven.get(k);
        return step >= g.start && crest(k, step) > U_EIND;
    }

    /** Where the crest of wave k is (u) at this step. */
    public double crest(int k, double step) {
        return U_START - stand.snelheid * (step - golven.get(k).start);
    }

    /** The step at which wave k starts to break (its crest reaches U_BREEK). */
    public double breekStap(int k) {
        return golven.get(k).start + (U_START - U_BREEK) / stand.snelheid;
    }

    /** Has wave k started to break? */
    public boolean breekt(int k, double step) {
        return step >= breekStap(k);
    }

    /**
     * Where the break is along the beach (v): it starts at the peak and runs to the wave's kant at the peel speed. The
     * broken part lies behind it (the side it comes from).
     */
    public double pel(int k, double step) {
        Golf g = golven.get(k);
        return g.piek + g.kant * stand.pel * Math.max(0, step - breekStap(k));
    }

    /**
     * How far v is in front of the break of wave k (positive: still unbroken green water, the surfer's side; negative:
     * the whitewater). Before it breaks the whole wave is green.
     */
    public double voorDeBreek(int k, double step, double v) {
        if (!breekt(k, step)) {
            return 1000;
        }
        return golven.get(k).kant * (v - pel(k, step));
    }

    /** The height of wave k at this step (it grows when it comes in, and flattens on the beach). */
    public double amp(int k, double step) {
        double u = crest(k, step);
        double groei = clamp((U_START - u) / 12.0, 0.35, 1.0);
        double strand = clamp((u - U_EIND) / 7.0, 0.0, 1.0);
        return stand.amp * groei * strand;
    }

    /** The wave's shape: height above the water at x blocks from the crest (x below 0: the steep front face). */
    public static double vorm(double a, double x) {
        if (x < -5.5 || x > 16) {
            return 0;
        }
        double w = x < 0 ? 1.7 : 5.5;
        return a * StrictMath.exp(-(x / w) * (x / w));
    }

    /** The height of the water surface of the whole set at (u, v): the highest wave there (broken parts are lower). */
    public double hoogte(double step, double u, double v) {
        double h = 0;
        for (int k = 0; k < golven.size(); k++) {
            if (!actief(k, step)) {
                continue;
            }
            double a = amp(k, step);
            if (voorDeBreek(k, step, v) < 0) {
                a *= 0.45;                                          // (the whitewater: lower and foamy)
            }
            h = Math.max(h, vorm(a, u - crest(k, step)));
        }
        return h;
    }

    /** The last step of the game. */
    public int duur() {
        return stand.duur();
    }

    public static double clamp(double x, double lo, double hi) {
        return x < lo ? lo : x > hi ? hi : x;
    }
}
