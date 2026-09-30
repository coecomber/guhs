package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * One surfer's ride at the surf beach of Guhwai'i (3.0): paddle, catch a wave, ride along its face, pump for speed, do
 * tricks off the lip, hide in the tube, and never fall behind the break (the whitewater gets you: PLONS!). One call of
 * {@link #stap} = one tick with the keys the surfer held.
 * <p>
 * Deterministic on purpose (no world, no random, StrictMath): the surfer's own game runs the same simulation with its own
 * keys the moment they are pressed (a smooth ride), and sends the keys of every step to the server, which runs it again
 * in the same order and is the one that counts (score, schelpjesmunten). Lilo-guh's board is driven by
 * {@link #liloInvoer}, a pure function of her ride, so she surfs the same on every side too.
 * <p>
 * The keys, in the beach's frame (you look at the beach): A/D move you along the beach (and spin you in the air), W
 * paddles in / goes down the face (speed!), S climbs up to the lip (and grabs the board in the air), space jumps off the
 * lip.
 */
public final class SurfSim {
    /** The key bits of one step. */
    public static final int LINKS = 1, RECHTS = 2, VOORUIT = 4, ACHTERUIT = 8, SPRING = 16;

    public enum Fase { PEDDELEN, RIJDEN, LUCHT, PLONS, TERUG, KLAAR }

    /** What happened this step (the screen shows it, the server scores it, the sounds follow it). */
    public enum Soort { VANG, MIS, SCHUIM, TRUC, CUTBACK, TUBE_IN, TUBE, PLONS, GOLF_KLAAR, KLAAR }

    /** One happening: its kind, the points it gave (after the multiplier) and the lang key of its name (tricks). */
    public record Gebeurtenis(Soort soort, int punten, String naam) {
    }

    /** Where you sit on the face: 0 = the lip, down to the trough. The face relaxes to the middle when you press nothing. */
    public static final double LIP = 0, DAL = -2.4, MIDDEN = -1.2;
    public static final double MIN_VAART = 0.18, MAX_VAART = 0.75, SPRING_VAART = 0.28;
    public static final int MAX_MULT = 5;
    /** How long a PLONS lasts, and the paddle back to the line-up. */
    public static final int PLONS_TIJD = 40, TERUG_TIJD = 50;

    private final SurfGolven golven;
    private final SurfGolven.Stand stand;
    private final boolean[] klaar;
    private final List<Gebeurtenis> nu = new ArrayList<>();

    private Fase fase = Fase.PEDDELEN;
    private int step;
    private double u = SurfGolven.U_LINE, v, f = -1.8, vaart = MIN_VAART, lastDf;
    private int dir = 1, golf = -1;
    private double h, vy, spin;
    private int grab, mult = 1, score, tubeTicks, cutbackRust, timer, vorige;
    // statistics (for the advancements and the end of the game)
    private int gereden, plonsen, trucs, tubes, besteTruc, maxMult = 1, draaien;

    public SurfSim(Niveau niveau, long seed) {
        this(new SurfGolven(niveau, seed));
    }

    public SurfSim(SurfGolven golven) {
        this.golven = golven;
        this.stand = golven.stand();
        this.klaar = new boolean[stand.golven()];
    }

    // --- one step ---------------------------------------------------------------------------------------------------

    /** One tick with the keys held (bits); returns what happened. */
    public List<Gebeurtenis> stap(int in) {
        nu.clear();
        if (fase == Fase.KLAAR) {
            return nu;
        }
        int pressed = in & ~vorige;
        vorige = in;
        step++;
        switch (fase) {
            case PEDDELEN -> peddel(in);
            case RIJDEN -> rijd(in, pressed);
            case LUCHT -> lucht(in);
            case PLONS -> {
                if (--timer <= 0) {
                    fase = Fase.TERUG;
                    timer = TERUG_TIJD;
                }
                drijf();
            }
            case TERUG -> {
                u += (SurfGolven.U_LINE - u) * 0.07;
                f = -1.8;
                if (--timer <= 0) {
                    fase = Fase.PEDDELEN;
                }
            }
            default -> {
            }
        }
        voorbij();
        if (step >= golven.duur() || (fase == Fase.PEDDELEN && alleKlaar())) {
            fase = Fase.KLAAR;
            nu.add(new Gebeurtenis(Soort.KLAAR, 0, ""));
        }
        return nu;
    }

    /** Waves that rolled past the line-up without you are gone (a miss is only said while you wait for them). */
    private void voorbij() {
        for (int k = 0; k < klaar.length; k++) {
            if (!klaar[k] && k != golf && step > golven.golf(k).start()
                    && golven.crest(k, step) < SurfGolven.U_LINE - stand.vangen() - 1.5) {
                klaar[k] = true;
                if (fase == Fase.PEDDELEN) {
                    nu.add(new Gebeurtenis(Soort.MIS, 0, ""));
                }
            }
        }
    }

    private boolean alleKlaar() {
        for (boolean b : klaar) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private void peddel(int in) {
        if ((in & LINKS) != 0) {
            v += 0.09;
        }
        if ((in & RECHTS) != 0) {
            v -= 0.09;
        }
        v = SurfGolven.clamp(v, -SurfGolven.HALF + 3, SurfGolven.HALF - 3);
        u += (SurfGolven.U_LINE - u) * 0.05;
        f = -1.8;
        for (int k = 0; k < klaar.length; k++) {
            if (klaar[k] || !golven.actief(k, step)) {
                continue;
            }
            double d = golven.crest(k, step) - u;
            if (Math.abs(d) > stand.vangen()) {
                continue;
            }
            if (golven.voorDeBreek(k, step, v) < 0) {
                if (Math.abs(d) < 1.0) {                      // the whitewater rolls over you: that one's gone
                    klaar[k] = true;
                    nu.add(new Gebeurtenis(Soort.SCHUIM, 0, ""));
                }
                continue;
            }
            boolean vang = stand.vanzelf() ? Math.abs(d) < 0.9 : (in & VOORUIT) != 0;
            if (vang) {
                golf = k;
                fase = Fase.RIJDEN;
                f = -0.9;
                vaart = 0.32;
                lastDf = 0;
                tubeTicks = 0;
                int want = (in & LINKS) != 0 ? 1 : (in & RECHTS) != 0 ? -1 : 0;
                dir = want != 0 ? want : golven.golf(k).kant();
                u = golven.crest(k, step) + f;
                nu.add(new Gebeurtenis(Soort.VANG, 0, ""));
                return;
            }
        }
    }

    private void rijd(int in, int pressed) {
        double f0 = f;
        if ((in & VOORUIT) != 0) {
            f -= 0.14;
        } else if ((in & ACHTERUIT) != 0) {
            f += 0.14;
        } else {
            f += (MIDDEN - f) * 0.06;
        }
        f = SurfGolven.clamp(f, DAL, LIP);
        double df = f - f0;
        vaart += df < 0 ? -df * 0.20 : -df * 0.10;                // down the face: speed; up to the lip: it costs half
        if (lastDf > 0.01 && df < -0.01) {
            vaart += 0.025;                                       // the pump: from climbing to dropping
        }
        if (Math.abs(df) > 1e-6) {
            lastDf = df;
        }
        if (f < -1.5) {
            vaart += 0.004;
        } else if (f > -0.5) {
            vaart -= 0.003;
        }
        vaart = SurfGolven.clamp(vaart * 0.99, MIN_VAART, MAX_VAART);
        // turning round: at speed high on the face it's a cutback (a trick), otherwise just a turn
        int want = (in & LINKS) != 0 ? 1 : (in & RECHTS) != 0 ? -1 : 0;
        if (cutbackRust > 0) {
            cutbackRust--;
        }
        if (want != 0 && want != dir) {
            if (vaart >= 0.4 && f > -1.1 && cutbackRust == 0) {
                truc(Soort.CUTBACK, 120, "cutback");
            }
            dir = want;
            vaart = Math.max(MIN_VAART, vaart * 0.75);
            cutbackRust = 30;
        }
        v += dir * vaart;
        u = golven.crest(golf, step) + f;
        double d = golven.voorDeBreek(golf, step, v);
        if (d < -0.6) {
            plons("schuim");
            return;
        }
        if (Math.abs(v) > SurfGolven.HALF - 1) {
            golfKlaar(false);
            return;
        }
        if (golven.crest(golf, step) <= SurfGolven.U_EIND + 1.5) {
            golfKlaar(true);
            return;
        }
        // points: riding, speed, the pocket near the break, the tube
        int p = 1 + (int) (vaart * 3);
        boolean tube = stand.tube() && golven.breekt(golf, step) && d >= 0 && d <= 2.6 && f >= -1.3;
        if (tube) {
            tubeTicks++;
            p += 6;
            if (tubeTicks == 10) {
                nu.add(new Gebeurtenis(Soort.TUBE_IN, 0, ""));
            }
        } else {
            tubeUit();
            if (d < 7) {
                p += 2;
            }
        }
        score += p * mult;
        if ((pressed & SPRING) != 0 && f > -0.5 && vaart >= SPRING_VAART) {
            tubeUit();
            fase = Fase.LUCHT;
            h = 0;
            vy = 0.30 + 0.35 * vaart;
            spin = 0;
            grab = 0;
        }
    }

    private void tubeUit() {
        if (tubeTicks >= 20) {
            tubes++;
            truc(Soort.TUBE, 300 + tubeTicks * 5, "tube");
        }
        tubeTicks = 0;
    }

    private void lucht(int in) {
        h += vy;
        vy -= 0.045;
        v += dir * vaart * 0.9;
        f = -0.3;
        u = golven.crest(golf, step) + f;
        if ((in & LINKS) != 0) {
            spin += 20;
        }
        if ((in & RECHTS) != 0) {
            spin -= 20;
        }
        if ((in & ACHTERUIT) != 0) {
            grab++;
        }
        if (h <= 0 || Math.abs(v) > SurfGolven.HALF - 1) {
            h = 0;
            land();
        }
    }

    private void land() {
        double r = spin % 360;
        if (r > 180) {
            r -= 360;
        } else if (r < -180) {
            r += 360;
        }
        if (Math.abs(r) > stand.landen()) {
            plons("scheef");
            return;
        }
        int rondjes = (int) Math.round(Math.abs(spin) / 360.0);
        int punten = 100;
        String naam = "hop";
        if (rondjes == 1) {
            punten += 250;
            naam = "draai";
        } else if (rondjes == 2) {
            punten += 600;
            naam = "dubbel";
        } else if (rondjes >= 3) {
            punten += 1000;
            naam = "drie";
        }
        draaien += rondjes;
        if (grab >= 6) {
            punten += 150;
            naam = naam + "_grab";
        }
        truc(Soort.TRUC, punten, naam);
        fase = Fase.RIJDEN;
        f = -0.9;
        vaart = Math.max(MIN_VAART, vaart * 0.9);
        lastDf = 0;
        if (Math.abs(v) > SurfGolven.HALF - 1) {
            golfKlaar(false);
        }
    }

    /** A trick: points times the multiplier, and the multiplier goes up. */
    private void truc(Soort soort, int punten, String naam) {
        int p = punten * mult;
        score += p;
        trucs++;
        besteTruc = Math.max(besteTruc, p);
        mult = Math.min(MAX_MULT, mult + 1);
        maxMult = Math.max(maxMult, mult);
        nu.add(new Gebeurtenis(soort, p, naam));
    }

    private void plons(String waarom) {
        if (golf >= 0) {
            klaar[golf] = true;
        }
        golf = -1;
        fase = Fase.PLONS;
        timer = PLONS_TIJD;
        mult = 1;
        tubeTicks = 0;
        plonsen++;
        h = 0;
        nu.add(new Gebeurtenis(Soort.PLONS, 0, waarom));
    }

    private void golfKlaar(boolean strand) {
        tubeUit();
        int p = strand ? 150 * mult : 60 * mult;
        score += p;
        gereden++;
        klaar[golf] = true;
        golf = -1;
        fase = Fase.TERUG;
        timer = TERUG_TIJD;
        nu.add(new Gebeurtenis(Soort.GOLF_KLAAR, p, strand ? "strand" : "uit"));
    }

    private void drijf() {
        u += 0.02;
        f = -1.8;
    }

    // --- Lilo-guh ---------------------------------------------------------------------------------------------------

    /**
     * Lilo-guh's keys for this step (a pure function of her ride, so every side sees her surf the same): she waits a bit
     * beside you for the next wave, paddles in, pumps along the face and, now and then, spins a knabbeldraai off the lip.
     */
    public static int liloInvoer(SurfSim s) {
        int in = 0;
        switch (s.fase) {
            case PEDDELEN -> {
                int k = s.volgende();
                if (k >= 0) {
                    double doel = s.golven.golf(k).piek() + s.golven.golf(k).kant() * 5;
                    boolean dichtbij = Math.abs(s.golven.crest(k, s.step + 1) - s.u) < s.stand.vangen() * 1.5;
                    if (dichtbij) {
                        // (hold still: she catches it facing away from the break)
                    } else if (s.v < doel - 0.5) {
                        in |= LINKS;
                    } else if (s.v > doel + 0.5) {
                        in |= RECHTS;
                    }
                    if (Math.abs(s.golven.crest(k, s.step + 1) - s.u) < s.stand.vangen() * 0.6) {
                        in |= VOORUIT;
                    }
                }
            }
            case RIJDEN -> {
                int cyc = (s.step + 7) % 28;
                in |= cyc < 14 ? VOORUIT : ACHTERUIT;                   // pumping
                if (s.golven.voorDeBreek(s.golf, s.step, s.v) < 3) {
                    in |= s.dir > 0 ? LINKS : RECHTS;                   // (keep going away from the break)
                }
                if (s.step % 90 < 20 && s.f > -0.45 && s.vaart >= SPRING_VAART + 0.02) {
                    in |= SPRING;
                }
            }
            case LUCHT -> {
                if (Math.abs(s.spin) < 350) {
                    in |= LINKS;
                }
            }
            default -> {
            }
        }
        return in;
    }

    /** The next wave still to come (not done, not yet past the line-up), or -1. */
    public int volgende() {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int k = 0; k < klaar.length; k++) {
            if (klaar[k] || step < golven.golf(k).start() - 200) {
                continue;
            }
            double d = golven.crest(k, step) - u;
            if (d > -stand.vangen() && d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    // --- where the board is --------------------------------------------------------------------------------------------

    /** Height of the board above the calm water (on the wave's face, in the air, bobbing on the swell). */
    public double hoogte() {
        return switch (fase) {
            case RIJDEN -> SurfGolven.vorm(golven.amp(golf, step), f);
            case LUCHT -> SurfGolven.vorm(golven.amp(golf, step), f) + h;
            case PLONS -> golven.hoogte(step, u, v) - 0.35;
            default -> golven.hoogte(step, u, v);
        };
    }

    /** How steep the board lies (radians, nose down positive) along the face: for the renderer. */
    public double helling() {
        if (fase != Fase.RIJDEN) {
            return 0;
        }
        double a = golven.amp(golf, step);
        double dz = SurfGolven.vorm(a, f + 0.3) - SurfGolven.vorm(a, f - 0.3);
        return StrictMath.atan2(dz, 0.6);
    }

    // --- state ---------------------------------------------------------------------------------------------------------

    public SurfGolven golven() {
        return golven;
    }

    public Fase fase() {
        return fase;
    }

    public int step() {
        return step;
    }

    public double u() {
        return u;
    }

    public double v() {
        return v;
    }

    public double face() {
        return f;
    }

    public double vaart() {
        return vaart;
    }

    public int dir() {
        return dir;
    }

    public double spin() {
        return spin;
    }

    public int golf() {
        return golf;
    }

    public int mult() {
        return mult;
    }

    public int score() {
        return score;
    }

    public boolean inTube() {
        return tubeTicks >= 10;
    }

    public int gereden() {
        return gereden;
    }

    public int plonsen() {
        return plonsen;
    }

    public int trucs() {
        return trucs;
    }

    public int tubes() {
        return tubes;
    }

    public int besteTruc() {
        return besteTruc;
    }

    public int maxMult() {
        return maxMult;
    }

    public int draaien() {
        return draaien;
    }

    public boolean klaar() {
        return fase == Fase.KLAAR;
    }

    /** Can you catch a wave now (the "NU! (W)" hint): a wave within reach of the line-up, green where you are. */
    public boolean kanVangen() {
        if (fase != Fase.PEDDELEN) {
            return false;
        }
        for (int k = 0; k < klaar.length; k++) {
            if (!klaar[k] && golven.actief(k, step) && Math.abs(golven.crest(k, step) - u) <= stand.vangen()
                    && golven.voorDeBreek(k, step, v) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Schelpjesmunten for a surf score (before the level bonus): nothing for a splash in the water, then one per 700. */
    public static int munten(int score) {
        return score < 300 ? 0 : Math.min(14, 1 + score / 700);
    }
}
