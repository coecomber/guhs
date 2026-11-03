package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The smid's stars (design 9: "sterren met risico", Star Force spanning, but sweet). Pure maths, the same on both sides (the
 * screen shows the chances from here) and tested in {@code VadsrijkEconomieGameTests}.
 * <ul>
 *   <li>+0..+10. Going from s to s+1 succeeds with {@link #KANS}[s].</li>
 *   <li>A failure from +0..+2 costs only the steentje. From +3 on a failure can cost one star ({@link #STER_KWIJT}).</li>
 *   <li>From +6 on a failure can make the piece <b>verpluisd</b> ({@link #PLUIS}[s]): it keeps its stars and rolls, but gives
 *       nothing until the smid repairs it. NEVER permanent loss (user rule): no outcome removes or destroys the item, and a
 *       verpluisd piece always comes back whole.</li>
 * </ul>
 */
public final class SterRegels {
    public static final int MAX = 10;
    /** Success chance going from s to s+1. */
    public static final double[] KANS = {0.95, 0.90, 0.85, 0.75, 0.65, 0.55, 0.45, 0.40, 0.35, 0.30};
    /** From this many stars on a failure can cost a star; chance of that within a failure. */
    public static final int KWIJT_VANAF = 3;
    public static final double STER_KWIJT = 0.40;
    /** From this many stars on a failure can make the piece verpluisd; chance of that within a failure, by current stars. */
    public static final int PLUIS_VANAF = 6;
    public static final double[] PLUIS = {0, 0, 0, 0, 0, 0, 0.06, 0.08, 0.10, 0.12};

    public enum Uitkomst {
        /** +1 star. */
        GELUKT,
        /** Nothing changes (only the steentje and the munten are used). */
        MISLUKT,
        /** One star less (never below 0). */
        STER_KWIJT,
        /** The piece keeps everything but is verpluisd until repaired. */
        VERPLUISD,
        /** Not possible (max, verpluisd): nothing happened, nothing paid. */
        KAN_NIET
    }

    private SterRegels() {
    }

    public static boolean kan(Uitrusting u) {
        return u.sterren() < MAX && !u.verpluisd();
    }

    /** The outcome from three dice in [0,1): success, star loss, verpluisd (deterministic: the tests feed their own). */
    public static Uitkomst uitkomst(int sterren, boolean verpluisd, double dSucces, double dKwijt, double dPluis) {
        if (sterren >= MAX || sterren < 0 || verpluisd) {
            return Uitkomst.KAN_NIET;
        }
        if (dSucces < KANS[sterren]) {
            return Uitkomst.GELUKT;
        }
        if (sterren >= PLUIS_VANAF && dPluis < PLUIS[sterren]) {
            return Uitkomst.VERPLUISD;
        }
        if (sterren >= KWIJT_VANAF && dKwijt < STER_KWIJT) {
            return Uitkomst.STER_KWIJT;
        }
        return Uitkomst.MISLUKT;
    }

    public static Uitkomst rol(Uitrusting u, RandomSource r) {
        return uitkomst(u.sterren(), u.verpluisd(), r.nextDouble(), r.nextDouble(), r.nextDouble());
    }

    /** The piece after this outcome (the same piece, never null, never "gone"). */
    public static Uitrusting pasToe(Uitrusting u, Uitkomst k) {
        return switch (k) {
            case GELUKT -> u.metSterren(Math.min(MAX, u.sterren() + 1));
            case STER_KWIJT -> u.metSterren(Math.max(0, u.sterren() - 1));
            case VERPLUISD -> u.metVerpluisd(true);
            default -> u;
        };
    }

    /** Repairing at the smid: back to normal, stars and rolls kept. */
    public static Uitrusting repareer(Uitrusting u) {
        return u.metVerpluisd(false);
    }

    /** Chance (0..1) of each bad outcome for the screen: {star loss, verpluisd}. */
    public static double[] risico(int sterren) {
        if (sterren >= MAX) {
            return new double[] {0, 0};
        }
        double faal = 1 - KANS[sterren];
        double pluis = sterren >= PLUIS_VANAF ? faal * PLUIS[sterren] : 0;
        double kwijt = sterren >= KWIJT_VANAF ? (faal - pluis) * STER_KWIJT : 0;
        return new double[] {kwijt, pluis};
    }

    // ---- prices -----------------------------------------------------------------------------------------------------------

    /** Munten (brons) for one star attempt on a piece of this level going from s. */
    public static long prijs(int level, int sterren) {
        return (20L + 6L * level) * (sterren + 1) * (sterren >= 5 ? 2 : 1);
    }

    /** Verbetersteentjes for one attempt. */
    public static int steentjes(int sterren) {
        return sterren >= 7 ? 3 : sterren >= 4 ? 2 : 1;
    }

    /** Repair: munten (brons) and pluisdraad. */
    public static long reparatiePrijs(int level, int sterren) {
        return (60L + 12L * level) * (2 + sterren) / 2;
    }

    public static int reparatieDraad(int sterren) {
        return 1 + sterren / 3;
    }

    /** Reroll: munten (brons) and Herrolknabbels (one extra knabbel and double munten when a bonus is locked). */
    public static long herrolPrijs(int level, boolean slot) {
        return (30L + 10L * level) * (slot ? 2 : 1);
    }

    public static int herrolKnabbels(boolean slot) {
        return slot ? 2 : 1;
    }

    // ---- what stars give (Rollen.STER_BONUS / STER_SCHADE) -------------------------------------------------------------------

    /** The stat bonus of the stars on a piece: per star a little main stat (more from +6), armour also Vads and verdediging. */
    public static Stats bonus(Uitrusting u) {
        Stats s = new Stats();
        int n = u.sterren();
        if (n <= 0) {
            return s;
        }
        BasisType t = u.basisType();
        double perSter = 1 + u.level() / 8.0;
        double hoofd = 0;
        for (int i = 1; i <= n; i++) {
            hoofd += perSter + (i > 5 ? 1 : 0);
        }
        hoofd = Math.round(hoofd * 10) / 10.0;
        if (t == null || t.slot() != Slot.WAPEN) {
            s.plus(Stat.VERDEDIGING, Math.round(hoofd * 0.8 * 10) / 10.0);
            s.plus(Stat.VADS, Math.round((4 + u.level() / 2.0) * n));
        } else {
            s.plus(hoofdStat(t.klasse()), hoofd);
        }
        if (n >= MAX) {
            s.plus(Stat.KRITIEK, 3);   // a +10 shines: a little crit on top
        }
        return s;
    }

    /** Weapon damage factor of the stars: +4 % per star, +5 % extra at +5, +10 % extra at +10. */
    public static double schade(Uitrusting u) {
        int n = u.sterren();
        return 1 + 0.04 * n + (n >= 5 ? 0.05 : 0) + (n >= MAX ? 0.10 : 0);
    }

    public static Stat hoofdStat(String klasse) {
        if (Klassen.JAGER.equals(klasse)) {
            return Stat.BEHENDIGHEID;
        }
        if (Klassen.MAGIER.equals(klasse)) {
            return Stat.WIJSHEID;
        }
        return Stat.KRACHT;
    }
}
