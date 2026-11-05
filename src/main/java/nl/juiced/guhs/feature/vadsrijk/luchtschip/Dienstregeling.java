package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * T-LUCHTSCHIP: the timetable and the flight path, pure maths (the same on server and client, so both move the ship themselves
 * and the flight is smooth; nothing about the ship's position is sent).
 * <p>
 * Every line has two ships. With period P (~3 min) and flight time D (60-90 s), ship k is at {@code c = (t - fase + k·P) mod 2P}:
 * <pre>
 *   [0, D)      flying a -&gt; b          [D, P)       docked at b (boarding)
 *   [P, P+D)    flying b -&gt; a          [P+D, 2P)    docked at a (boarding)
 * </pre>
 * So both harbours of a line see a departure every P ticks (both at the same moment: the two ships pass each other halfway), and
 * the ship that will leave is docked for P - D ticks before it goes.
 */
public final class Dienstregeling {
    public enum Fase {
        LIGT_A, VLIEGT_AB, LIGT_B, VLIEGT_BA;

        public boolean vliegt() {
            return this == VLIEGT_AB || this == VLIEGT_BA;
        }
    }

    /** Where ship k of a line is at a moment: position (deck), yaw, phase, ticks into the phase, ticks left in it. */
    public record Stand(Vec3 pos, float yaw, Fase fase, double inFase, double rest) {
        /** The harbour it lies at / flies from. */
        public Lijnen.Haven van(Lijnen.Lijn l) {
            return fase == Fase.LIGT_A || fase == Fase.VLIEGT_AB ? l.a() : l.b();
        }

        /** The harbour it is going to (docked: where its next departure goes). */
        public Lijnen.Haven naar(Lijnen.Lijn l) {
            return van(l) == l.a() ? l.b() : l.a();
        }
    }

    /** One direction of a line: the spline through the control points, as an arc-length table. */
    public static final class Pad {
        final Vec3[] punten;
        final double lengte;
        final int duur;

        Pad(List<Vec3> pts, int vasteDuur) {
            int n = 900;
            punten = new Vec3[n + 1];
            double[] cum = new double[n + 1];
            Vec3[] ruw = new Vec3[n + 1];
            for (int i = 0; i <= n; i++) {
                ruw[i] = spline(pts, i / (double) n);
                if (i > 0) {
                    cum[i] = cum[i - 1] + ruw[i].distanceTo(ruw[i - 1]);
                }
            }
            lengte = cum[n];
            // resample evenly by arc length
            int j = 0;
            for (int i = 0; i <= n; i++) {
                double s = lengte * i / n;
                while (j < n - 1 && cum[j + 1] < s) {
                    j++;
                }
                double d = cum[j + 1] - cum[j];
                double f = d <= 1e-9 ? 0 : (s - cum[j]) / d;
                punten[i] = ruw[j].lerp(ruw[j + 1], Mth.clamp(f, 0, 1));
            }
            duur = vasteDuur > 0 ? vasteDuur : Mth.clamp((int) Math.round(lengte / Lijnen.VAART / (1 - RAMP)), Lijnen.MIN_DUUR, Lijnen.MAX_DUUR);
        }

        public double lengte() {
            return lengte;
        }

        public int duur() {
            return duur;
        }

        /** Position at arc fraction 0..1. */
        public Vec3 op(double u) {
            double f = Mth.clamp(u, 0, 1) * (punten.length - 1);
            int i = Math.min(punten.length - 2, (int) Math.floor(f));
            return punten[i].lerp(punten[i + 1], f - i);
        }
    }

    /** Fraction of the flight spent speeding up / slowing down (each end half of it). */
    static final double RAMP = 0.16;
    private static final Map<String, Pad> PADEN = new ConcurrentHashMap<>();

    private Dienstregeling() {
    }

    /** The path a -&gt; b ({@code heen}) or b -&gt; a, cached per line. */
    public static Pad pad(Lijnen.Lijn l, boolean heen) {
        return PADEN.computeIfAbsent(l.id() + (heen ? ">" : "<") + System.identityHashCode(l), k -> new Pad(punten(l, heen), l.vasteDuur()));
    }

    /** Forget cached paths (dev: after the POIs changed). */
    public static void vergeet() {
        PADEN.clear();
    }

    /** The control points of one direction: dock, out from the pier, the line's points, in towards the other pier, dock. */
    static List<Vec3> punten(Lijnen.Lijn l, boolean heen) {
        Lijnen.Haven van = heen ? l.a() : l.b(), naar = heen ? l.b() : l.a();
        List<Vec3> via = new ArrayList<>(l.via());
        if (!heen) {
            java.util.Collections.reverse(via);
            // the way back keeps to the other side: shift sideways (to the right of the direction of flight) in the middle
            Vec3 a = van.dok(), b = naar.dok();
            Vec3 dir = b.subtract(a);
            Vec3 rechts = new Vec3(-dir.z, 0, dir.x).normalize();
            double breed = l.test() ? 2 : 22;
            for (int i = 0; i < via.size(); i++) {
                double w = Math.sin(Math.PI * (i + 1) / (via.size() + 1));
                via.set(i, via.get(i).add(rechts.scale(breed * w)));
            }
        }
        List<Vec3> pts = new ArrayList<>();
        double uit = l.test() ? 3 : 26;
        pts.add(van.dok());
        pts.add(van.dok().add(van.uit().scale(uit)).add(0, l.test() ? 1 : 5, 0));
        pts.addAll(via);
        pts.add(naar.dok().add(naar.uit().scale(uit)).add(0, l.test() ? 1 : 4, 0));
        pts.add(naar.dok());
        return pts;
    }

    /** Flight time of the line (ticks; the longer direction counts, so both ships keep the same timetable). */
    public static int duur(Lijnen.Lijn l) {
        return Math.max(pad(l, true).duur(), pad(l, false).duur());
    }

    /** Where ship k (0/1) of the line is at time t (ticks; may be fractional on the client). */
    public static Stand stand(Lijnen.Lijn l, int k, double t) {
        int p = l.periode();
        int d = Math.min(duur(l), p - 20);
        double c = mod(t - l.fase() + (double) k * p, 2.0 * p);
        if (c < d) {
            return vlucht(l, true, c, d);
        }
        if (c < p) {
            return new Stand(l.b().dok(), yaw(l.b().uit()), Fase.LIGT_B, c - d, p - c);
        }
        if (c < p + d) {
            return vlucht(l, false, c - p, d);
        }
        return new Stand(l.a().dok(), yaw(l.a().uit()), Fase.LIGT_A, c - p - d, 2.0 * p - c);
    }

    private static Stand vlucht(Lijnen.Lijn l, boolean heen, double c, int d) {
        Pad pad = pad(l, heen);
        double x = Mth.clamp(c / d, 0, 1);
        double u = gemak(x);
        Vec3 pos = pad.op(u);
        Vec3 voor = pad.op(Math.min(1, u + 0.004)).subtract(pad.op(Math.max(0, u - 0.004)));
        Lijnen.Haven naar = heen ? l.b() : l.a(), van = heen ? l.a() : l.b();
        float yaw = voor.horizontalDistanceSqr() < 1e-6 ? yaw(van.uit()) : yaw(voor);
        // the last stretch: turn round so the bow points out again when it lies at the pier
        double draai = Mth.clamp((x - 0.86) / 0.14, 0, 1);
        if (draai > 0) {
            draai = draai * draai * (3 - 2 * draai);
            float eind = yaw(naar.uit());
            yaw = yaw + Mth.wrapDegrees(eind - yaw) * (float) draai;
        }
        return new Stand(pos, Mth.wrapDegrees(yaw), heen ? Fase.VLIEGT_AB : Fase.VLIEGT_BA, c, d - c);
    }

    /** Time -&gt; path fraction: smooth start and stop over {@link #RAMP}, steady in between. */
    static double gemak(double x) {
        double r = RAMP;
        if (x < r / 2) {
            return x * x / r / (1 - r / 2);
        }
        if (x > 1 - r / 2) {
            double y = 1 - x;
            return 1 - y * y / r / (1 - r / 2);
        }
        return (x - r / 4) / (1 - r / 2);
    }

    /** Minecraft yaw of a horizontal direction (0 = +z, 90 = -x). */
    public static float yaw(Vec3 d) {
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    /** Ticks until the next departure from this harbour of this line (0 = it leaves now). */
    public static int totVertrek(Lijnen.Lijn l, long t) {
        return (int) mod(l.fase() - t, l.periode());
    }

    /** Which ship (0/1) lies at harbour h now, or -1 (it is on its way). */
    public static int schipAan(Lijnen.Lijn l, Lijnen.Haven h, double t) {
        for (int k = 0; k < 2; k++) {
            Fase f = stand(l, k, t).fase();
            if (f == (h == l.a() ? Fase.LIGT_A : Fase.LIGT_B)) {
                return k;
            }
        }
        return -1;
    }

    private static double mod(double a, double m) {
        double r = a % m;
        return r < 0 ? r + m : r;
    }

    /** Catmull-Rom position along the control points at 0..1 (uniform in segment count). */
    static Vec3 spline(List<Vec3> pts, double u) {
        int n = pts.size() - 1;
        double f = Mth.clamp(u, 0, 1) * n;
        int i = Math.min(n - 1, (int) Math.floor(f));
        double t = f - i;
        Vec3 p0 = pts.get(Math.max(0, i - 1)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(n, i + 2));
        double t2 = t * t, t3 = t2 * t;
        return p0.scale(-0.5 * t3 + t2 - 0.5 * t).add(p1.scale(1.5 * t3 - 2.5 * t2 + 1)).add(p2.scale(-1.5 * t3 + 2 * t2 + 0.5 * t))
                .add(p3.scale(0.5 * t3 - 0.5 * t2));
    }
}
