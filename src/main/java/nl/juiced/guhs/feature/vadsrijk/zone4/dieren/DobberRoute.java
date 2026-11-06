package nl.juiced.guhs.feature.vadsrijk.zone4.dieren;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ouwe Dobber's swim loop as a pure function of time, so server and every client compute the SAME place without sending
 * positions (smooth for passengers, nothing to sync but the dev time shift): the closed Catmull-Rom spline through
 * {@link Z4DierenPlekken#DOBBER_ROUTE}, measured by arc length. One cycle = {@link #PAUZE} ticks waiting at the halte by the
 * Schildpadeiland jetty, then one slow lap (gentle start and stop) back to the halte.
 */
public final class DobberRoute {
    /** Cruising speed (blocks per tick): 2 blocks a second, a lap of ~320 blocks takes ~2,7 minutes. */
    public static final double SNELHEID = 0.1;
    /** Ticks at the halte (30 s: time to climb on). */
    public static final int PAUZE = 600;
    /** Ticks to speed up / slow down. */
    public static final int AANLOOP = 80;
    private static final int N = 24;

    private static DobberRoute standaard;

    private final double[] xs, zs, cum;
    private final double lengte, halte;

    public static DobberRoute get() {
        if (standaard == null) {
            standaard = new DobberRoute(Z4DierenPlekken.DOBBER_ROUTE, Z4DierenPlekken.DOBBER_HALTE);
        }
        return standaard;
    }

    public DobberRoute(double[][] p, int halteIndex) {
        int n = p.length;
        xs = new double[n * N + 1];
        zs = new double[n * N + 1];
        cum = new double[n * N + 1];
        for (int i = 0; i < n; i++) {
            double[] p0 = p[Math.floorMod(i - 1, n)], p1 = p[i], p2 = p[(i + 1) % n], p3 = p[(i + 2) % n];
            for (int s = 0; s < N; s++) {
                double t = s / (double) N;
                xs[i * N + s] = cr(p0[0], p1[0], p2[0], p3[0], t);
                zs[i * N + s] = cr(p0[1], p1[1], p2[1], p3[1], t);
            }
        }
        xs[n * N] = xs[0];
        zs[n * N] = zs[0];
        for (int i = 1; i < cum.length; i++) {
            cum[i] = cum[i - 1] + Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
        }
        lengte = cum[cum.length - 1];
        halte = cum[Math.floorMod(halteIndex, n) * N];
    }

    private static double cr(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    public double lengte() {
        return lengte;
    }

    /** Ticks of swimming in one cycle. */
    public int zwemTicks() {
        return (int) Math.ceil(lengte / SNELHEID) + AANLOOP;
    }

    /** Ticks of one whole cycle (waiting + one lap). */
    public int cyclus() {
        return PAUZE + zwemTicks();
    }

    /** Ticks into the current cycle (0 = just arrived at the halte). */
    public double inCyclus(double tijd) {
        double c = cyclus();
        return ((tijd % c) + c) % c;
    }

    public boolean wacht(double tijd) {
        return inCyclus(tijd) < PAUZE;
    }

    /** Blocks swum since the halte at this time (0 while waiting, lengte at the very end of the lap). */
    public double afstand(double tijd) {
        double t = inCyclus(tijd);
        if (t < PAUZE) {
            return 0;
        }
        double s = t - PAUZE, T = zwemTicks(), v = SNELHEID, a = AANLOOP;
        if (s < a) {
            return v * s * s / (2 * a);
        }
        if (s > T - a) {
            double r = Math.max(0, T - s);
            return Math.max(0, lengte - v * r * r / (2 * a));
        }
        return v * a / 2 + v * (s - a);
    }

    /** The point (x, 0, z) this many blocks after the halte along the loop. */
    public Vec3 punt(double afstand) {
        double d = ((halte + afstand) % lengte + lengte) % lengte;
        int lo = 0, hi = cum.length - 1;
        while (hi - lo > 1) {
            int m = (lo + hi) >>> 1;
            if (cum[m] <= d) {
                lo = m;
            } else {
                hi = m;
            }
        }
        double seg = cum[hi] - cum[lo];
        double f = seg < 1e-9 ? 0 : (d - cum[lo]) / seg;
        return new Vec3(Mth.lerp(f, xs[lo], xs[hi]), 0, Mth.lerp(f, zs[lo], zs[hi]));
    }

    /** The heading (Minecraft yaw, degrees) at this distance after the halte. */
    public float yaw(double afstand) {
        Vec3 a = punt(afstand - 1.5), b = punt(afstand + 1.5);
        return (float) Math.toDegrees(Math.atan2(-(b.x - a.x), b.z - a.z));
    }

    /** The halte point (x, 0, z). */
    public Vec3 halte() {
        return punt(0);
    }
}
