package nl.juiced.guhs.feature.baltoslee;

import net.minecraft.util.Mth;

/**
 * How the sled rides: one tick of the sled on its track, the same in the rider's game (which drives it, without delay) and on
 * the server (which checks it, and rides by itself when the rider's game says nothing: game tests).
 * <ul>
 *   <li>W = "Hup, hup!": the dogs run faster (up to {@link #TOP}); nothing: they trot ({@link #KRUIS}); S = brake.</li>
 *   <li>A/D steer. Off the marked track the snow is deep: slow ({@link #DIEP}).</li>
 *   <li>Downhill is faster, uphill slower. Cold dogs ({@link #warmteFactor}) run slower: rest at a vuurkorf! (1.3.1: they
 *       really get cold now, once per trek: see {@link #KOU_PER_TICK}.)</li>
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
    // --- 1.3.1: how hard the trek is (the medicine ride and the sledesprint): every number in one place -------------------------
    /**
     * The gusts: one chance (3 in 4) per VLAAG_CEL blocks, VLAAG_LENGTE blocks long, announced VLAAG_WAARSCHUWING blocks ahead
     * (HUD arrow + whoosh from that side). 1.3.1: was 26 / 9 / 8.
     */
    public static final double VLAAG_CEL = 22, VLAAG_LENGTE = 12, VLAAG_WAARSCHUWING = 12;
    /**
     * A gust's push at its peak, blocks per tick sideways: WIND_KRACHT * (WIND_BASIS + (1 - WIND_BASIS) * storm). Full steering
     * is 0.11, so in a heavy storm the peak of a gust is stronger than you can steer: steer against it from the warning on.
     * 1.3.1: was 0.05 * storm (a gust you could ignore). On an ice bridge only WIND_IJS of it counts (as before: holdable).
     */
    public static final double WIND_KRACHT = 0.16, WIND_BASIS = 0.35, WIND_IJS = 0.08;
    /**
     * The cold: every riding tick the dogs lose KOU_PER_TICK + storm * KOU_STORM warmth (of 100). 1.3.1: was storm * 0.04,
     * which never got them cold. Now: "getting cold" (the warning) below WARMTE_WAARSCHUWING after about 560 ticks of
     * riding, slow below WARMTE_KOUD (speed x TRAAG_KOUD) after about 800, very slow below WARMTE_IJSKOUD (x TRAAG_IJSKOUD)
     * after about 1020; the whole trek is about 1400 ticks flat out. So: one stop at a vuurkorf somewhere in the middle (it
     * warms them up completely in {@link SleeRit#RUST_TICKS}) and they stay warm to the finish; none and the last third crawls.
     */
    public static final float KOU_PER_TICK = 0.08f, KOU_STORM = 0.01f;
    public static final float WARMTE_WAARSCHUWING = 50, WARMTE_KOUD = 30, WARMTE_IJSKOUD = 10;
    public static final double TRAAG_KOUD = 0.7, TRAAG_IJSKOUD = 0.45;

    /** How much warmth the dogs lose in one riding tick in this storm. */
    public static float kou(float storm) {
        return KOU_PER_TICK + storm * KOU_STORM;
    }

    /** A gust's push (blocks per tick sideways, signed) for windvlaag g (-1..1) in this storm. */
    public static double windKracht(double g, float storm) {
        return g * WIND_KRACHT * (WIND_BASIS + (1 - WIND_BASIS) * storm);
    }

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
        return warmte < WARMTE_IJSKOUD ? TRAAG_IJSKOUD : warmte < WARMTE_KOUD ? TRAAG_KOUD : 1.0;
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

        double wind = windKracht(windvlaag(seed, been, st.s, b.lengte), storm);
        double grip = 0.25 + 0.75 * Math.min(1, st.v / KRUIS);
        if (ijs) {
            st.latV = st.latV * 0.93 + stuur * 0.016 * grip + wind * WIND_IJS;   // (a gust on the ice: you can still hold it)
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
