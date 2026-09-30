package nl.juiced.guhs.feature.baltoslee;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One leg of a sled ride as a smooth track: a Catmull-Rom line through the route's points (every few blocks), resampled by
 * arc length every {@link #STAP} blocks. {@code s} = blocks along the track from its start, {@code lat} = blocks to the right
 * (+) or left (-) of the middle, {@code breedte} = the half width of the marked track there.
 * <p>
 * The server and the rider's game build it from exactly the same numbers (the ride's route tag, floats), so they compute
 * exactly the same positions: that is what lets the rider's own game drive the sled without any delay.
 */
public final class SleeBaan {
    /** Samples every quarter of a block. */
    public static final double STAP = 0.25;

    private final double[] xs, ys, zs, ws;
    /** The arc length at each of the route's points. */
    private final double[] puntS;
    public final double lengte;

    /** @param punten x, y (the top of the snow the sled rides on), z, half width - four floats per point */
    public SleeBaan(float[] punten) {
        int n = punten.length / 4;
        if (n < 2) {
            throw new IllegalArgumentException("a track needs two points");
        }
        Vec3[] p = new Vec3[n];
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            p[i] = new Vec3(punten[i * 4], punten[i * 4 + 1], punten[i * 4 + 2]);
            b[i] = Math.max(0.6, punten[i * 4 + 3]);
        }
        // dense Catmull-Rom samples with their arc length
        final int sub = 24;
        List<double[]> dicht = new ArrayList<>();
        double[] puntS = new double[n];
        double len = 0;
        Vec3 prev = null;
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(n - 1, i + 2)];
            double yLo = Math.min(p1.y, p2.y), yHi = Math.max(p1.y, p2.y);
            for (int k = 0; k < sub; k++) {
                double t = (double) k / sub;
                Vec3 q = catmull(p0, p1, p2, p3, t);
                q = new Vec3(q.x, Mth.clamp(q.y, yLo, yHi), q.z);           // (no bumps above or below the points)
                if (prev != null) {
                    len += q.distanceTo(prev);
                }
                if (k == 0) {
                    puntS[i] = len;
                }
                dicht.add(new double[]{q.x, q.y, q.z, Mth.lerp(t, b[i], b[i + 1]), len});
                prev = q;
            }
        }
        len += p[n - 1].distanceTo(prev);
        puntS[n - 1] = len;
        dicht.add(new double[]{p[n - 1].x, p[n - 1].y, p[n - 1].z, b[n - 1], len});
        this.lengte = len;
        this.puntS = puntS;
        // resample by arc length
        int m = (int) Math.ceil(len / STAP) + 1;
        xs = new double[m];
        ys = new double[m];
        zs = new double[m];
        ws = new double[m];
        int j = 0;
        for (int i = 0; i < m; i++) {
            double s = Math.min(len, i * STAP);
            while (j < dicht.size() - 2 && dicht.get(j + 1)[4] < s) {
                j++;
            }
            double[] a = dicht.get(j), c = dicht.get(Math.min(dicht.size() - 1, j + 1));
            double f = c[4] - a[4] < 1e-9 ? 0 : Mth.clamp((s - a[4]) / (c[4] - a[4]), 0, 1);
            xs[i] = Mth.lerp(f, a[0], c[0]);
            ys[i] = Mth.lerp(f, a[1], c[1]);
            zs[i] = Mth.lerp(f, a[2], c[2]);
            ws[i] = Mth.lerp(f, a[3], c[3]);
        }
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return new Vec3(cm(p0.x, p1.x, p2.x, p3.x, t, t2, t3), cm(p0.y, p1.y, p2.y, p3.y, t, t2, t3), cm(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double cm(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }

    public int samples() {
        return xs.length;
    }

    /** The arc length at the route's point i (clamped). */
    public double sVanPunt(int i) {
        return puntS[Mth.clamp(i, 0, puntS.length - 1)];
    }

    public int punten() {
        return puntS.length;
    }

    private double lees(double[] a, double s) {
        double f = Mth.clamp(s, 0, lengte) / STAP;
        int i = Math.min(a.length - 2, (int) f);
        return Mth.lerp(Math.min(1, f - i), a[i], a[i + 1]);
    }

    /** The middle of the track at s (the top of the snow). */
    public Vec3 midden(double s) {
        return new Vec3(lees(xs, s), lees(ys, s), lees(zs, s));
    }

    /** The half width of the marked track at s. */
    public double breedte(double s) {
        return lees(ws, s);
    }

    /** The direction of the track at s (a unit vector, with its slope). */
    public Vec3 richting(double s) {
        double a = Mth.clamp(s - 0.6, 0, lengte), b = Mth.clamp(s + 0.6, 0, lengte);
        if (b - a < 0.1) {
            a = Math.max(0, b - 1.2);
        }
        Vec3 d = midden(b).subtract(midden(a));
        return d.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /** Level ground's "to the right" at s (a horizontal unit vector). */
    public Vec3 rechts(double s) {
        Vec3 t = richting(s);
        Vec3 h = new Vec3(t.x, 0, t.z);
        if (h.lengthSqr() < 1e-8) {
            return new Vec3(1, 0, 0);
        }
        h = h.normalize();
        return new Vec3(-h.z, 0, h.x);
    }

    /** Up (+) or down (-) per block along the track at s. */
    public double helling(double s) {
        double a = Mth.clamp(s - 1, 0, lengte), b = Mth.clamp(s + 1, 0, lengte);
        return b - a < 0.1 ? 0 : (lees(ys, b) - lees(ys, a)) / (b - a);
    }

    /** How much the track turns at s (radians per block; + = to the right). */
    public double bocht(double s) {
        Vec3 a = richting(Mth.clamp(s - 1.5, 0, lengte)), b = richting(Mth.clamp(s + 1.5, 0, lengte));
        // (the up component of a x b is negative for a turn to the right: facing south, right is west)
        double kruis = a.z * b.x - a.x * b.z;
        return -Math.asin(Mth.clamp(kruis, -1, 1)) / 3;
    }

    /** The spot lat blocks to the right of the middle at s. */
    public Vec3 op(double s, double lat) {
        return midden(s).add(rechts(s).scale(lat));
    }

    /** The s whose middle is nearest to p (horizontally; for placing zones and finding where something is). */
    public double dichtstbij(Vec3 p) {
        double best = 0, bestD = Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - p.x, dz = zs[i] - p.z, dy = (ys[i] - p.y) * 0.5;
            double d = dx * dx + dz * dz + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = i * STAP;
            }
        }
        return Math.min(best, lengte);
    }
}
