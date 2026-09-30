package nl.juiced.guhs.feature.ballon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The four fixed round flights of Kapitein Wolkje's guh balloon (the same on both sides, so the ride is smooth): each
 * is a closed loop of control points around the balloon's home (template frame: -z is where the guh-face flower field
 * is, +x the giant guh balloon), passing two of the eight viewpoints ({@link Uitzicht}). The balloon rises straight up
 * from its ballonsteiger, floats round and comes back down on it.
 * <p>
 * {@link Pad} is a flight's path in the world: the route turned to the festival's rotation, every control point lifted
 * above the terrain (worked out by the server at take-off and sent along), a Catmull-Rom spline through them, measured
 * so the balloon can move at a set speed in blocks per tick.
 */
public enum BallonRoute {
    PLUISJESRONDE(new double[][]{{0, 0, 0}, {0, 7, 0}, {4, 17, -9}, {0, 25, -24}, {-26, 35, -40}, {-50, 43, -12}, {-38, 37, 22}, {-12, 24, 26},
            {-3, 13, 9}, {0, 6, 0}, {0, 0, 0}}, Uitzicht.GUHGEZICHTVELD, 3, Uitzicht.WOLKENPOORT, 5),
    HOGE_VADS(new double[][]{{0, 0, 0}, {0, 7, 0}, {-7, 18, 9}, {-18, 34, 36}, {8, 54, 54}, {42, 46, 32}, {56, 36, -5}, {32, 25, -24},
            {10, 14, -11}, {0, 6, 0}, {0, 0, 0}}, Uitzicht.HOOGSTE_PUNTJE, 4, Uitzicht.REGENBOOGBOCHT, 6),
    KNABBELKRING(new double[][]{{0, 0, 0}, {0, 7, 0}, {9, 16, 6}, {24, 22, 3}, {48, 31, -20}, {36, 39, -48}, {0, 37, -54}, {-24, 29, -30},
            {-14, 16, -7}, {0, 6, 0}, {0, 0, 0}}, Uitzicht.REUZENGUH, 3, Uitzicht.HARTJESWOLKJE, 5),
    ZONNETJESROUTE(new double[][]{{0, 0, 0}, {0, 7, 0}, {-9, 16, -5}, {-30, 22, -12}, {-42, 44, 12}, {-12, 50, 44}, {22, 42, 42}, {32, 27, 14},
            {13, 14, 4}, {0, 6, 0}, {0, 0, 0}}, Uitzicht.BRANDERSPRONG, 3, Uitzicht.GUHMENSIE_UITZICHT, 5);

    /** The eight viewpoints: each flight passes two (a stamp for each on the ballonstempelkaart). */
    public enum Uitzicht {
        GUHGEZICHTVELD, WOLKENPOORT, HOOGSTE_PUNTJE, REGENBOOGBOCHT, REUZENGUH, HARTJESWOLKJE, BRANDERSPRONG, GUHMENSIE_UITZICHT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static List<String> ids() {
            return java.util.Arrays.stream(values()).map(Uitzicht::id).toList();
        }

        @Nullable
        public static Uitzicht byId(String id) {
            for (Uitzicht u : values()) {
                if (u.id().equals(id)) {
                    return u;
                }
            }
            return null;
        }
    }

    /** Cruising speed and the slow speeds (blocks per tick). */
    public static final double SNELHEID = 0.24, LANGZAAM = 0.09, START = 0.05;
    /** How far before and after a viewpoint the balloon floats slowly (blocks along the path). */
    public static final double UITZICHT_RUIMTE = 9;
    /** How far above the ground a control point stays at least. */
    public static final int VRIJ_BOVEN_GROND = 9;

    private final double[][] punten;
    public final Uitzicht eerste, tweede;
    public final int eerstePunt, tweedePunt;

    BallonRoute(double[][] punten, Uitzicht eerste, int eerstePunt, Uitzicht tweede, int tweedePunt) {
        this.punten = punten;
        this.eerste = eerste;
        this.eerstePunt = eerstePunt;
        this.tweede = tweede;
        this.tweedePunt = tweedePunt;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int punten() {
        return punten.length;
    }

    /** Control point i in the template frame (x, y up, z). */
    public Vec3 punt(int i) {
        return new Vec3(punten[i][0], punten[i][1], punten[i][2]);
    }

    public static BallonRoute byIndex(int i) {
        return values()[Math.floorMod(i, values().length)];
    }

    /** Control point i in the world: turned by the festival's yaw (degrees, like an entity's: 0 = template frame). */
    public Vec3 draai(int i, float yaw) {
        Vec3 p = punt(i);
        double r = Math.toRadians(yaw);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
    }

    /** The path of one flight in the world. */
    public Pad pad(Vec3 thuis, float yaw, float[] lift) {
        return pad(thuis, yaw, lift, 1.0);
    }

    /** The path, every offset scaled (the game tests fly small rounds). */
    public Pad pad(Vec3 thuis, float yaw, float[] lift, double schaal) {
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i < punten.length; i++) {
            pts.add(thuis.add(draai(i, yaw).scale(schaal)).add(0, i < lift.length ? lift[i] : 0, 0));
        }
        return new Pad(this, pts);
    }

    /** A measured Catmull-Rom spline through the control points: position by distance along it. */
    public static final class Pad {
        private static final int STAPPEN = 32;
        public final BallonRoute route;
        private final List<Vec3> punten;
        private final Vec3[] monster;
        private final double[] afstand;
        /** The distance along the path at each control point. */
        private final double[] knoop;

        Pad(BallonRoute route, List<Vec3> punten) {
            this.route = route;
            this.punten = punten;
            int n = punten.size() - 1;
            monster = new Vec3[n * STAPPEN + 1];
            afstand = new double[monster.length];
            knoop = new double[punten.size()];
            int k = 0;
            for (int seg = 0; seg < n; seg++) {
                for (int s = 0; s < STAPPEN; s++) {
                    monster[k++] = catmull(seg, s / (double) STAPPEN);
                }
            }
            monster[k] = punten.get(n);
            for (int i = 1; i < monster.length; i++) {
                afstand[i] = afstand[i - 1] + monster[i].distanceTo(monster[i - 1]);
            }
            for (int i = 0; i < punten.size(); i++) {
                knoop[i] = afstand[Math.min(i * STAPPEN, afstand.length - 1)];
            }
        }

        private Vec3 p(int i) {
            return punten.get(Mth.clamp(i, 0, punten.size() - 1));
        }

        private Vec3 catmull(int seg, double t) {
            Vec3 p0 = p(seg - 1), p1 = p(seg), p2 = p(seg + 1), p3 = p(seg + 2);
            double t2 = t * t, t3 = t2 * t;
            return new Vec3(cr(p0.x, p1.x, p2.x, p3.x, t, t2, t3), cr(p0.y, p1.y, p2.y, p3.y, t, t2, t3), cr(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
        }

        private static double cr(double a, double b, double c, double d, double t, double t2, double t3) {
            return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
        }

        public double lengte() {
            return afstand[afstand.length - 1];
        }

        /** Distance along the path at control point i. */
        public double bijPunt(int i) {
            return knoop[Mth.clamp(i, 0, knoop.length - 1)];
        }

        /** The position at this distance along the path (clamped to the ends). */
        public Vec3 op(double d) {
            if (d <= 0) {
                return monster[0];
            }
            if (d >= lengte()) {
                return monster[monster.length - 1];
            }
            int lo = 0, hi = afstand.length - 1;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (afstand[mid] <= d) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            double span = afstand[hi] - afstand[lo];
            double f = span <= 1e-9 ? 0 : (d - afstand[lo]) / span;
            return monster[lo].lerp(monster[hi], f);
        }

        /** Which way the path goes here (not normalised; zero at a standstill). */
        public Vec3 richting(double d) {
            return op(d + 0.6).subtract(op(d - 0.6));
        }

        /** How fast the balloon floats here: slowly off the steiger and down onto it, slowly past the viewpoints. */
        public double snelheid(double d) {
            double v = SNELHEID;
            double totEind = lengte() - d;
            if (d < 14) {
                v = Mth.lerp(Mth.clamp(d / 14, 0, 1), START, SNELHEID);
            }
            if (totEind < 16) {
                v = Math.min(v, Mth.lerp(Mth.clamp(totEind / 16, 0, 1), START, SNELHEID));
            }
            for (int punt : new int[]{route.eerstePunt, route.tweedePunt}) {
                double afstandTot = Math.abs(d - bijPunt(punt));
                if (afstandTot < UITZICHT_RUIMTE) {
                    v = Math.min(v, Mth.lerp(afstandTot / UITZICHT_RUIMTE, LANGZAAM, SNELHEID));
                }
            }
            return Math.max(v, START);
        }

        public int aantalPunten() {
            return punten.size();
        }
    }
}
