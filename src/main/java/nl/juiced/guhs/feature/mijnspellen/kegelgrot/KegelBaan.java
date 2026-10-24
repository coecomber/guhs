package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The physics of one lane of the Kegelgrot, in the lane's own frame (pure: no world, so the GameTests can roll balls
 * without a level): {@code u} along the lane (0 = the foul line, the pins near the end), {@code v} across it (+ = right,
 * seen from the approach). One pink wool ball and ten plush guh pins, all circles on the floor.
 * <ul>
 *   <li>the ball rolls straight on, with a little hook ({@code spin}, bending it sideways) and a little friction;</li>
 *   <li>a pin it touches falls over ("piep!") and slides away, knocking over the pins it touches in turn: the ball is
 *       five times as heavy as a pin, so it keeps going (and deflects a little), like real bowling;</li>
 *   <li>three lanes ({@link Soort}): straight with gutters, a bending one with bumpers along it, and a straight one with
 *       bumpers and obstacles (two plush posts and a sliding Mika-blokje).</li>
 * </ul>
 * The {@link KegelGame} moves the entities to these positions every tick.
 */
public final class KegelBaan {
    /** The three lanes of the Kegelgrot (index 0, 1, 2 = makkelijk, medium, lastig). */
    public enum Soort {
        RECHT, BOCHTIG, HINDERNIS
    }

    /** The lane: half its width (bumpers or the gutters start there), the gutters' outer edge (RECHT), the pit, the end. */
    public static final double HALF = 1.5, GOOT = 2.5, PUT = 15.5, EIND = 16.5;
    /** The head pin, the distance between pins in a row and between rows. */
    public static final double KOP = 12.5, AFSTAND = 0.5, RIJ = 0.433;
    public static final double BAL_R = 0.2, KEGEL_R = 0.12, LIGGEND_R = 0.15;
    /** The slowest and the fastest ball (blocks per tick), the largest aim off the lane's axis, the hook at full spin. */
    public static final double MIN_SNELHEID = 0.16, MAX_SNELHEID = 0.5, MAX_HOEK = Math.toRadians(9), HOOK = 0.0011;
    /** The bend of BOCHTIG: how far the lane swings out (to the right) between these u. */
    public static final double BOCHT = 1.0, BOCHT_VAN = 2.5, BOCHT_TOT = 11.0;
    /** HINDERNIS: the two posts, and the sliding Mika-blokje (its row, how far it slides, ticks for there and back). */
    public static final double PAAL_U = 7.0, PAAL_V = 1.0, PAAL_R = 0.3, SCHUIF_U = 10.0, SCHUIF_V = 1.1, SCHUIF_R = 0.35;
    public static final int SCHUIF_TIJD = 70;
    /** Sub-steps per tick, and how long a roll may take at most. */
    private static final int STAPPEN = 4, MAX_TIKKEN = 20 * 15;

    public static final class Kegel {
        public final int nr;
        public double u, v, du, dv;
        /** Fallen over (and the way it lies); in the pit (gone). */
        public boolean om, weg;
        public float ligHoek;

        Kegel(int nr, double u, double v) {
            this.nr = nr;
            this.u = u;
            this.v = v;
        }

        double straal() {
            return om ? LIGGEND_R : KEGEL_R;
        }

        boolean beweegt() {
            return du * du + dv * dv > 1e-6;
        }
    }

    public static final class Bal {
        public double u, v, du, dv, spin, rol;
        /** In the gutter (RECHT: it can't touch a pin any more); done (in the pit, or stopped). */
        public boolean goot, weg;
    }

    public final Soort soort;
    private final List<Kegel> kegels = new ArrayList<>();
    private final Bal bal = new Bal();
    private boolean rolt;
    private int tikken;
    private long klok;
    private Random rng = new Random(1);
    /** The pins that stood when the ball was thrown. */
    private final boolean[] stondBijWorp = new boolean[10];
    /** Something hit something this tick: 1 a pin fell, 2 the ball hit a bumper / post, 4 the ball went into the gutter. */
    private int gebeurd;

    public KegelBaan(Soort soort) {
        this.soort = soort;
        for (int i = 0; i < 10; i++) {
            double[] p = opstelling(i);
            kegels.add(new Kegel(i, p[0], p[1]));
        }
        bal.weg = true;
    }

    /** Where pin i stands: row 0 the head pin, row 3 the back row (u, v). */
    public static double[] opstelling(int i) {
        int rij = i == 0 ? 0 : i < 3 ? 1 : i < 6 ? 2 : 3;
        int eerste = rij * (rij + 1) / 2, k = i - eerste;
        return new double[] {KOP + rij * RIJ, (k - rij / 2.0) * AFSTAND};
    }

    public List<Kegel> kegels() {
        return kegels;
    }

    public Bal bal() {
        return bal;
    }

    public boolean rolt() {
        return rolt;
    }

    /** What happened in the last tick (bits: 1 a pin fell, 2 the ball bounced, 4 gutter). */
    public int gebeurd() {
        return gebeurd;
    }

    /** Puts these pins up (true = standing, at its spot), the others away. */
    public void opzetten(boolean[] staand) {
        for (Kegel k : kegels) {
            double[] p = opstelling(k.nr);
            k.u = p[0];
            k.v = p[1];
            k.du = k.dv = 0;
            k.om = !staand[k.nr];
            k.weg = !staand[k.nr];
        }
    }

    /** Takes the fallen pins away (the sweeper); the standing ones stay where they are. */
    public void vegen() {
        for (Kegel k : kegels) {
            if (k.om) {
                k.weg = true;
            }
            k.du = k.dv = 0;
        }
    }

    public boolean[] staand() {
        boolean[] out = new boolean[10];
        for (Kegel k : kegels) {
            out[k.nr] = !k.om && !k.weg;
        }
        return out;
    }

    /** The middle of the lane at u (BOCHTIG swings out; the others are straight). */
    public static double midden(Soort soort, double u) {
        if (soort != Soort.BOCHTIG || u <= BOCHT_VAN || u >= BOCHT_TOT) {
            return 0;
        }
        return BOCHT * Math.sin(Math.PI * (u - BOCHT_VAN) / (BOCHT_TOT - BOCHT_VAN));
    }

    private static double helling(Soort soort, double u) {
        return (midden(soort, u + 0.01) - midden(soort, u - 0.01)) / 0.02;
    }

    /** The sliding Mika-blokje's v at this time (HINDERNIS). */
    public static double schuifV(long klok) {
        return SCHUIF_V * Math.sin(2 * Math.PI * (klok % SCHUIF_TIJD) / SCHUIF_TIJD);
    }

    /** The obstacles at this time: {u, v, r} (HINDERNIS only). */
    public static List<double[]> obstakels(Soort soort, long klok) {
        if (soort != Soort.HINDERNIS) {
            return List.of();
        }
        return List.of(new double[] {PAAL_U, -PAAL_V, PAAL_R}, new double[] {PAAL_U, PAAL_V, PAAL_R},
                new double[] {SCHUIF_U, schuifV(klok), SCHUIF_R});
    }

    /** The speed of a ball thrown with this power (0..1). */
    public static double snelheid(float kracht) {
        return MIN_SNELHEID + (MAX_SNELHEID - MIN_SNELHEID) * Math.max(0, Math.min(1, kracht));
    }

    /**
     * Throws the ball from (0, v0): speed in blocks per tick, angle off the lane's axis (radians, + = right, limited),
     * spin -1..1 (+ = it bends to the right). The seed makes the pins' scatter repeatable.
     */
    public void gooi(double v0, double snelheid, double hoek, double spin, long seed, long klok) {
        double h = Math.max(-MAX_HOEK, Math.min(MAX_HOEK, hoek));
        bal.u = 0;
        bal.v = Math.max(-HALF + BAL_R, Math.min(HALF - BAL_R, v0));
        bal.du = Math.cos(h) * snelheid;
        bal.dv = Math.sin(h) * snelheid;
        bal.spin = Math.max(-1, Math.min(1, spin));
        bal.goot = bal.weg = false;
        bal.rol = 0;
        rng = new Random(seed);
        for (Kegel k : kegels) {
            stondBijWorp[k.nr] = !k.om && !k.weg;
        }
        rolt = true;
        tikken = 0;
        this.klok = klok;
    }

    /** How many of the pins that stood at the throw are down now. */
    public int omgevallen() {
        int n = 0;
        for (Kegel k : kegels) {
            if (stondBijWorp[k.nr] && (k.om || k.weg)) {
                n++;
            }
        }
        return n;
    }

    /** One tick; false once the ball is done and every pin lies still (or it took too long). */
    public boolean tik() {
        gebeurd = 0;
        if (!rolt) {
            return false;
        }
        double dt = 1.0 / STAPPEN;
        for (int s = 0; s < STAPPEN; s++) {
            stapBal(dt);
            stapKegels(dt);
        }
        klok++;
        tikken++;
        boolean beweegt = !bal.weg;
        for (Kegel k : kegels) {
            beweegt |= !k.weg && k.beweegt();
        }
        if (!beweegt || tikken > MAX_TIKKEN) {
            rolt = false;
            for (Kegel k : kegels) {
                k.du = k.dv = 0;
            }
            bal.weg = true;
        }
        return rolt;
    }

    private void stapBal(double dt) {
        if (bal.weg) {
            return;
        }
        Bal b = bal;
        if (!b.goot) {
            b.dv += b.spin * HOOK * dt;
        }
        double f = 1 - 0.0015 * dt;
        b.du *= f;
        b.dv *= f;
        b.u += b.du * dt;
        b.v += b.dv * dt;
        b.rol += Math.hypot(b.du, b.dv) * dt / BAL_R;
        if (b.goot) {
            b.v = Math.signum(b.v) * (HALF + BAL_R + 0.3);
            b.dv = 0;
        } else if (soort == Soort.RECHT) {
            if (Math.abs(b.v) > HALF && b.u < PUT) {
                b.goot = true;
                gebeurd |= 4;
            }
        } else if (muur(b.u, b.v, b.du, BAL_R, b)) {
            gebeurd |= 2;
        }
        if (!b.goot) {
            for (double[] o : obstakels(soort, klok)) {
                if (botsVast(b, o[0], o[1], o[2] + BAL_R)) {
                    gebeurd |= 2;
                }
            }
            for (Kegel k : kegels) {
                if (!k.weg && raakt(b.u, b.v, BAL_R, k)) {
                    stoot(b, k);
                }
            }
        }
        if (b.u >= PUT || b.du * b.du + b.dv * b.dv < 0.0004) {
            b.weg = true;
        }
    }

    /** Keeps something between the bumpers (BOCHTIG, HINDERNIS); true when it bounced. */
    private boolean muur(double u, double v, double du, double r, Object wie) {
        double c = midden(soort, u), grens = HALF - r;
        double rel = v - c;
        if (Math.abs(rel) <= grens) {
            return false;
        }
        double kant = Math.signum(rel);
        double nieuweV = c + kant * grens;
        double slope = helling(soort, u);
        if (wie instanceof Bal b) {
            b.v = nieuweV;
            double dvRel = b.dv - b.du * slope;
            if (dvRel * kant > 0) {
                b.dv = b.du * slope - dvRel * 0.6;
            }
        } else if (wie instanceof Kegel k) {
            k.v = nieuweV;
            double dvRel = k.dv - k.du * slope;
            if (dvRel * kant > 0) {
                k.dv = k.du * slope - dvRel * 0.4;
            }
        }
        return true;
    }

    private static boolean raakt(double u, double v, double r, Kegel k) {
        double du = k.u - u, dv = k.v - v, rr = r + k.straal();
        return du * du + dv * dv < rr * rr;
    }

    /** The ball bounces off a fixed (or sliding) obstacle. */
    private static boolean botsVast(Bal b, double ou, double ov, double rr) {
        double du = b.u - ou, dv = b.v - ov, d2 = du * du + dv * dv;
        if (d2 >= rr * rr || d2 < 1e-9) {
            return false;
        }
        double d = Math.sqrt(d2), nu = du / d, nv = dv / d;
        b.u = ou + nu * rr;
        b.v = ov + nv * rr;
        double vn = b.du * nu + b.dv * nv;
        if (vn < 0) {
            b.du -= 1.6 * vn * nu;
            b.dv -= 1.6 * vn * nv;
        }
        return true;
    }

    /** The ball (five times as heavy) hits a pin: the pin flies off and falls, the ball goes on, a little slower. */
    private void stoot(Bal b, Kegel k) {
        double du = k.u - b.u, dv = k.v - b.v, d = Math.max(1e-6, Math.hypot(du, dv));
        double nu = du / d, nv = dv / d;
        double rel = (b.du - k.du) * nu + (b.dv - k.dv) * nv;
        double overlap = BAL_R + k.straal() - d;
        k.u += nu * overlap;
        k.v += nv * overlap;
        if (rel <= 0) {
            return;
        }
        double mb = 5, mk = 1, e = 0.75;
        double j = (1 + e) * rel / (1 / mb + 1 / mk);
        b.du -= j / mb * nu;
        b.dv -= j / mb * nv;
        k.du += j / mk * nu;
        k.dv += j / mk * nv;
        valOm(k);
    }

    private void valOm(Kegel k) {
        if (k.om) {
            return;
        }
        k.om = true;
        gebeurd |= 1;
        double a = (rng.nextDouble() - 0.5) * Math.toRadians(40);
        double cu = k.du * Math.cos(a) - k.dv * Math.sin(a), cv = k.du * Math.sin(a) + k.dv * Math.cos(a);
        k.du = cu;
        k.dv = cv;
        k.ligHoek = (float) Math.atan2(k.dv, k.du);
    }

    private void stapKegels(double dt) {
        for (Kegel k : kegels) {
            if (k.weg || !k.beweegt()) {
                continue;
            }
            k.u += k.du * dt;
            k.v += k.dv * dt;
            double f = Math.pow(0.9, dt);
            k.du *= f;
            k.dv *= f;
            if (k.du * k.du + k.dv * k.dv < 1e-6) {
                k.du = k.dv = 0;
            }
            if (k.u >= PUT) {
                k.weg = true;
                k.om = true;
                continue;
            }
            if (soort == Soort.RECHT) {
                if (Math.abs(k.v) > HALF + 0.2) {               // in the gutter: it lies there
                    k.v = Math.signum(k.v) * Math.min(GOOT - LIGGEND_R, Math.abs(k.v));
                    k.dv = 0;
                    k.du *= 0.5;
                }
            } else {
                muur(k.u, k.v, k.du, k.straal(), k);
            }
            for (Kegel o : kegels) {
                if (o == k || o.weg || !raakt(k.u, k.v, k.straal(), o)) {
                    continue;
                }
                double du = o.u - k.u, dv = o.v - k.v, d = Math.max(1e-6, Math.hypot(du, dv));
                double nu = du / d, nv = dv / d;
                double rel = (k.du - o.du) * nu + (k.dv - o.dv) * nv;
                double overlap = k.straal() + o.straal() - d;
                double j = rel > 0 ? (1 + 0.7) * rel / 2 : 0;
                if (!o.om && j <= 0.012) {
                    // too soft to knock it over: the standing pin stays put, the sliding one bounces off it
                    k.u -= nu * overlap;
                    k.v -= nv * overlap;
                    k.du -= 2 * j * nu;
                    k.dv -= 2 * j * nv;
                    continue;
                }
                o.u += nu * overlap / 2;
                o.v += nv * overlap / 2;
                k.u -= nu * overlap / 2;
                k.v -= nv * overlap / 2;
                if (rel <= 0) {
                    continue;
                }
                k.du -= j * nu;
                k.dv -= j * nv;
                o.du += j * nu;
                o.dv += j * nv;
                if (!o.om) {
                    valOm(o);
                }
            }
        }
    }

    /** (tests) the time on the lane's clock. */
    public long klok() {
        return klok;
    }
}
