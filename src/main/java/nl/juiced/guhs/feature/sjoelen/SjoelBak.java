package nl.juiced.guhs.feature.sjoelen;

import java.util.ArrayList;
import java.util.List;

/**
 * The sjoelbak as a flat little world of its own: real sliding-puck physics in the bak's frame, independent of blocks.
 * u runs along the bak (0 = the head where you stand, the gate bar at {@link #BAR}, the back wall at {@link #BACK}), v runs
 * across it (0 = the left rim, {@link #WIDTH} = the right rim, as seen from the head). A puck (a circle of radius
 * {@link #R}) slides with friction, bounces off the rims, the back wall and the dividers of the four gates (hitting the
 * corner of a divider sends it sideways, like a real sjoelbak) and pushes other pucks (equal masses). A puck counts for a
 * gate when it lies still completely behind the gate bar in that gate's lane.
 * <p>
 * The gates are 2-3-4-1 from left to right; every complete set (one puck in each gate) is 20 points, the pucks left
 * over count their gate's value ({@link #score}).
 */
public final class SjoelBak {
    public static final double WIDTH = 5, BAR = 20, BACK = 23, R = 0.22;
    /** Where a new puck lies (u), and the head rim behind it. */
    public static final double START_U = 0.5;
    /** Slowing down per tick (blocks/tick²): the bak is waxed nice and smooth. */
    public static final double FRICTION = 0.012;
    public static final double RIM_BOUNCE = 0.5, BACK_BOUNCE = 0.45, DIVIDER_BOUNCE = 0.4, PUCK_BOUNCE = 0.85;
    /** Below this speed a puck lies still. */
    public static final double STOP = 0.003;
    public static final int SUBSTEPS = 4;
    /** The four gate openings (v from, to), left to right, and what each gate is worth. */
    public static final double[][] OPENINGS = {{0.25, 1.0}, {1.5, 2.25}, {2.75, 3.5}, {4.0, 4.75}};
    public static final int[] VALUES = {2, 3, 4, 1};
    /** The dividers (v from, to): from the gate bar all the way to the back wall. */
    public static final double[][] DIVIDERS = {{0, 0.25}, {1.0, 1.5}, {2.25, 2.75}, {3.5, 4.0}, {4.75, 5.0}};
    /** The slowest and the fastest slide, and how much of your aim (sideways) reaches the puck. */
    public static final double MIN_SPEED = 0.30, MAX_SPEED = 0.95, MAX_ANGLE = Math.toRadians(35), AIM = 0.35;

    /** One puck: position, velocity, and a little spin for the looks. */
    public static final class Puck {
        public final int id;
        public double u, v, du, dv;
        public boolean moving;
        public float spin;

        Puck(int id, double u, double v, double du, double dv) {
            this.id = id;
            this.u = u;
            this.v = v;
            this.du = du;
            this.dv = dv;
            this.moving = du * du + dv * dv > 0;
        }

        public double speed() {
            return Math.hypot(du, dv);
        }
    }

    private final List<Puck> pucks = new ArrayList<>();
    private int nextId;
    /** Something hit something hard this step (for a "klak" sound). */
    private int knocks;

    public List<Puck> pucks() {
        return pucks;
    }

    /** The speed of a slide with this much power (0..1). */
    public static double speed(double power) {
        return MIN_SPEED + (MAX_SPEED - MIN_SPEED) * Math.max(0, Math.min(1, power));
    }

    /** The direction of the puck (radians, 0 = straight down the bak, + = to the right) for an aim this far off the bak's axis. */
    public static double angle(double aim) {
        return Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, aim)) * AIM;
    }

    /** Slides a new puck from the head at v (kept inside the rims) with this speed and direction. */
    public Puck slide(double v, double speed, double angle) {
        double start = Math.max(R + 0.02, Math.min(WIDTH - R - 0.02, v));
        Puck p = new Puck(nextId++, START_U, start, speed * Math.cos(angle), speed * Math.sin(angle));
        pucks.add(p);
        return p;
    }

    /** Puts a puck somewhere, lying still (tests). */
    public Puck place(double u, double v) {
        Puck p = new Puck(nextId++, u, v, 0, 0);
        pucks.add(p);
        return p;
    }

    public void clear() {
        pucks.clear();
    }

    public boolean allStill() {
        return pucks.stream().noneMatch(p -> p.moving);
    }

    /** One game tick; returns how many hard knocks there were (for sounds). */
    public int tick() {
        knocks = 0;
        double dt = 1.0 / SUBSTEPS;
        for (int s = 0; s < SUBSTEPS; s++) {
            for (Puck p : pucks) {
                if (!p.moving) {
                    continue;
                }
                double speed = p.speed();
                double slower = Math.max(0, speed - FRICTION * dt);
                if (slower < STOP) {
                    p.du = p.dv = 0;
                    p.moving = false;
                    continue;
                }
                p.du *= slower / speed;
                p.dv *= slower / speed;
                p.u += p.du * dt;
                p.v += p.dv * dt;
                p.spin += (float) (speed * dt * 2.5);
                walls(p);
                for (double[] d : DIVIDERS) {
                    divider(p, d[0], d[1]);
                }
            }
            collide();
        }
        return knocks;
    }

    private void walls(Puck p) {
        if (p.v < R) {
            p.v = R;
            if (p.dv < 0) {
                bump(-p.dv);
                p.dv = -p.dv * RIM_BOUNCE;
            }
        } else if (p.v > WIDTH - R) {
            p.v = WIDTH - R;
            if (p.dv > 0) {
                bump(p.dv);
                p.dv = -p.dv * RIM_BOUNCE;
            }
        }
        if (p.u > BACK - R) {
            p.u = BACK - R;
            if (p.du > 0) {
                bump(p.du);
                p.du = -p.du * BACK_BOUNCE;
            }
        } else if (p.u < R) {
            p.u = R;
            if (p.du < 0) {
                bump(-p.du);
                p.du = -p.du * RIM_BOUNCE;
            }
        }
    }

    /** A divider: the rectangle u in [BAR, BACK], v in [a, b]. Its front corners send a puck sideways. */
    private void divider(Puck p, double a, double b) {
        double cu = Math.max(BAR, Math.min(BACK, p.u)), cv = Math.max(a, Math.min(b, p.v));
        double du = p.u - cu, dv = p.v - cv;
        double d = Math.hypot(du, dv);
        if (d >= R) {
            return;
        }
        double nu, nv;
        if (d < 1e-9) {                                    // (the centre got inside: out the nearest side)
            double front = p.u - BAR, left = p.v - a, right = b - p.v;
            if (front <= left && front <= right) {
                nu = -1;
                nv = 0;
                d = -front;
            } else if (left <= right) {
                nu = 0;
                nv = -1;
                d = -left;
            } else {
                nu = 0;
                nv = 1;
                d = -right;
            }
        } else {
            nu = du / d;
            nv = dv / d;
        }
        double push = R - d;
        p.u += nu * push;
        p.v += nv * push;
        double vn = p.du * nu + p.dv * nv;
        if (vn < 0) {
            bump(-vn);
            p.du -= (1 + DIVIDER_BOUNCE) * vn * nu;
            p.dv -= (1 + DIVIDER_BOUNCE) * vn * nv;
            p.du *= 0.97;                                  // (a little rub along it)
            p.dv *= 0.97;
        }
    }

    /** Pucks bump into each other: equal masses, nearly elastic. A puck lying still gets pushed along. */
    private void collide() {
        for (int i = 0; i < pucks.size(); i++) {
            Puck a = pucks.get(i);
            for (int j = i + 1; j < pucks.size(); j++) {
                Puck b = pucks.get(j);
                double du = b.u - a.u, dv = b.v - a.v;
                double d = Math.hypot(du, dv);
                if (d >= 2 * R || (!a.moving && !b.moving)) {
                    continue;
                }
                double nu, nv;
                if (d < 1e-9) {
                    nu = 1;
                    nv = 0;
                } else {
                    nu = du / d;
                    nv = dv / d;
                }
                double overlap = 2 * R - d;
                a.u -= nu * overlap / 2;
                a.v -= nv * overlap / 2;
                b.u += nu * overlap / 2;
                b.v += nv * overlap / 2;
                double rel = (b.du - a.du) * nu + (b.dv - a.dv) * nv;
                if (rel < 0) {
                    double j2 = -(1 + PUCK_BOUNCE) * rel / 2;
                    a.du -= j2 * nu;
                    a.dv -= j2 * nv;
                    b.du += j2 * nu;
                    b.dv += j2 * nv;
                    a.moving = b.moving = true;
                    bump(-rel);
                }
                walls(a);
                walls(b);
            }
        }
    }

    private void bump(double speed) {
        if (speed > 0.08) {
            knocks++;
        }
    }

    /** The gate (0..3, left to right) this puck lies in, or -1: it must be completely behind the gate bar. */
    public static int gate(Puck p) {
        if (p.u < BAR + R || p.u > BACK) {
            return -1;
        }
        for (int k = 0; k < OPENINGS.length; k++) {
            if (p.v >= OPENINGS[k][0] && p.v <= OPENINGS[k][1]) {
                return k;
            }
        }
        return -1;
    }

    /** How many (still or moving) pucks are in each gate now. */
    public int[] counts() {
        int[] counts = new int[OPENINGS.length];
        for (Puck p : pucks) {
            int g = gate(p);
            if (g >= 0) {
                counts[g]++;
            }
        }
        return counts;
    }

    /** Complete sets (one in each gate). */
    public static int sets(int[] counts) {
        int sets = Integer.MAX_VALUE;
        for (int c : counts) {
            sets = Math.min(sets, c);
        }
        return sets == Integer.MAX_VALUE ? 0 : sets;
    }

    /** The points: 20 for every complete set, the rest counts per gate (2-3-4-1). 20 pucks, 5 in each gate: 100. */
    public static int score(int[] counts) {
        int sets = sets(counts);
        int score = sets * 20;
        for (int k = 0; k < counts.length; k++) {
            score += (counts[k] - sets) * VALUES[k];
        }
        return score;
    }
}
