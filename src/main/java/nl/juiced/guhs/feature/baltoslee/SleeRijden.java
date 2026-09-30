package nl.juiced.guhs.feature.baltoslee;

import net.minecraft.util.Mth;

/**
 * How the sled rides: one tick of the sled on its track, the same in the rider's game (which drives it, without delay) and on
 * the server (which checks it, and rides by itself when the rider's game says nothing: game tests).
 * <ul>
 *   <li>W = "Hup, hup!": the dogs run faster (up to {@link #TOP}); nothing: they trot ({@link #KRUIS}); S = brake.</li>
 *   <li>A/D steer. Off the marked track the snow is deep: slow ({@link #DIEP}).</li>
 *   <li>Downhill is faster, uphill slower. Cold dogs ({@link #warmteFactor}) run slower: rest at a vuurkorf!</li>
 *   <li>On an ice bridge the sled slides: it keeps drifting sideways and steers gently (fall off the side and you land in
 *       the soft snow below, back to the start of the bridge).</li>
 *   <li>Wind gusts ({@link #windvlaag}) push the sled sideways; how hard depends on the storm. They are part of the track
 *       (the same for the same ride on every side) and you see them coming ({@link #windKomt}).</li>
 * </ul>
 */
public final class SleeRijden {
    /** Blocks per tick: flat out, trotting, in deep snow, on an ice bridge. */
    public static final double TOP = 0.46, KRUIS = 0.28, DIEP = 0.14, IJS_MAX = 0.30;
    /** How far outside the marked track the sled can go (into the deep snow). */
    public static final double BUITEN = 1.5;
    /** On an ice bridge: this far past its edge and you fall off. */
    public static final double IJS_RAND = 0.35;
    /** The gusts: one chance per this many blocks, lasting this long. */
    public static final double VLAAG_CEL = 26, VLAAG_LENGTE = 9, VLAAG_WAARSCHUWING = 8;

    /** What the rider does: vooruit (-1 brake .. 1 faster), stuur (-1 left .. 1 right). */
    public record Invoer(double vooruit, double stuur) {
        public static final Invoer NIKS = new Invoer(0, 0);
    }

    /** Where the sled is on its leg and how it moves. */
    public static final class Stand {
        public double s, lat, v, latV;

        public Stand() {
        }

        public Stand(double s, double lat, double v, double latV) {
            this.s = s;
            this.lat = lat;
            this.v = v;
            this.latV = latV;
        }

        public Stand kopie() {
            return new Stand(s, lat, v, latV);
        }
    }

    /** Warm dogs run at full speed; cold ones slower (warmte 0..100). */
    public static double warmteFactor(float warmte) {
        return warmte < 10 ? 0.62 : warmte < 30 ? 0.8 : 1.0;
    }

    /** One tick of the sled on leg been of the route. */
    public static void stap(Stand st, Invoer in, RitRoute route, int been, float storm, float warmte, int seed) {
        SleeBaan b = route.baan(been);
        double w = b.breedte(st.s);
        boolean ijs = route.zone(been, RitRoute.Soort.IJSBRUG, st.s) != null;
        double max = TOP * warmteFactor(warmte);
        if (Math.abs(st.lat) > w + 0.05) {
            max = Math.min(max, DIEP);
        }
        if (ijs) {
            max = Math.min(max, IJS_MAX);
        }
        double vooruit = Mth.clamp(in.vooruit(), -1, 1), stuur = Mth.clamp(in.stuur(), -1, 1);
        double doel = vooruit > 0.1 ? max : Math.min(KRUIS, max);
        if (vooruit < -0.1) {
            st.v = Math.max(0, st.v - 0.03);
        } else if (st.v < doel) {
            st.v = Math.min(doel, st.v + (vooruit > 0.1 ? 0.010 : 0.006));
        } else {
            st.v = Math.max(doel, st.v - 0.014);
        }
        st.v = Mth.clamp(st.v - b.helling(st.s) * 0.018, 0, TOP * 1.2);
        st.s = Math.min(b.lengte, st.s + st.v);

        double wind = windvlaag(seed, been, st.s, b.lengte) * storm * 0.05;
        double grip = 0.25 + 0.75 * Math.min(1, st.v / KRUIS);
        if (ijs) {
            st.latV = st.latV * 0.93 + stuur * 0.016 * grip + wind * 0.25;   // (a gust on the ice: you can still hold it)
            st.lat += st.latV;
        } else {
            st.latV = 0;
            st.lat += stuur * 0.11 * grip + wind;
            st.lat = Mth.clamp(st.lat, -(w + BUITEN), w + BUITEN);
        }
    }

    /** Is the sled off the side of an ice bridge (it falls into the snow below)? */
    public static boolean vanDeBrug(RitRoute route, int been, Stand st) {
        return route.zone(been, RitRoute.Soort.IJSBRUG, st.s) != null && Math.abs(st.lat) > route.baan(been).breedte(st.s) + IJS_RAND;
    }

    // --- the gusts ----------------------------------------------------------------------------------------------------------

    private static int meng(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    /** Where the gust of cell k starts (or NaN: this cell has none), and its direction in the sign of {@link #richting}. */
    private static double vlaagStart(int seed, int been, int k) {
        int h = meng(seed * 31 + been * 1_000_003 + k * 7919);
        if ((h & 3) == 0 || k < 1) {
            return Double.NaN;
        }
        return k * VLAAG_CEL + 6 + ((h >>> 3) & 7);
    }

    private static int richting(int seed, int been, int k) {
        return (meng(seed * 31 + been * 1_000_003 + k * 7919) >>> 2 & 1) == 0 ? -1 : 1;
    }

    /** The gust at s: -1 (pushes left) .. 1 (pushes right), 0 = none. Never in the last blocks of a leg. */
    public static double windvlaag(int seed, int been, double s, double lengte) {
        if (s > lengte - 10) {
            return 0;
        }
        int k = (int) Math.floor(s / VLAAG_CEL);
        for (int c = k - 1; c <= k; c++) {
            double start = vlaagStart(seed, been, c);
            if (!Double.isNaN(start) && s >= start && s <= start + VLAAG_LENGTE) {
                return richting(seed, been, c) * Math.sin(Math.PI * (s - start) / VLAAG_LENGTE);
            }
        }
        return 0;
    }

    /** A gust coming up within the next few blocks: its direction (-1 / 1), or 0. */
    public static int windKomt(int seed, int been, double s, double lengte) {
        int k = (int) Math.floor(s / VLAAG_CEL);
        for (int c = k; c <= k + 1; c++) {
            double start = vlaagStart(seed, been, c);
            if (!Double.isNaN(start) && s < start && s >= start - VLAAG_WAARSCHUWING && start < lengte - 10) {
                return richting(seed, been, c);
            }
        }
        return 0;
    }

    private SleeRijden() {
    }
}
