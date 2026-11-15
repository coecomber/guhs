package nl.juiced.guhs.feature.vadsrijk.ambacht;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * The Ambachten xp curve and pacing (VOORSTEL_PROFESSIONS §5.1, A1/A2; 1.10.4 "Getallen", akkoord 2026-11-15):
 * <ul>
 *   <li>same shape as combat ({@code 40 + 25 lv^1.6}), own scale {@link #SCHAAL} 0.33 (3x faster than 1.10.3); cap {@link #CAP} 60;</li>
 *   <li>a gathering action gives a fixed amount per resource tier ({@link #VERZAMEL}): 1 -> 10 is ~40 harvests, 1 -> 20 ~250
 *   (30-40 min). Far above the tier ({@link #TE_HOOG} levels over its top) it gives half: you rise fastest on your own tier;</li>
 *   <li>tiers by level ({@link #TIER_MIN}): T1 1, T2 12, T3 22, T4 32, T5 40 (T6 48 = zone 6 volgt);</li>
 *   <li>generic (main-level) xp: a little, ~20 % of what fighting gives in the same time ({@link #generiek});</li>
 *   <li>Uitgerust (rested): offline you build up to 2 h of double ambacht-xp (1 s per 6 s offline); every action uses 7 s.</li>
 * </ul>
 */
public final class AmbachtXp {
    /** The level cap (1.10.4: stays 60, also with zone 6's tier: T6 starts at 48). */
    public static volatile int CAP = 60;
    /** The cap per zone that is in the game (index = zone): the cap is the one of the highest zone that exists. */
    public static final int[] CAP_PER_ZONE = {0, 30, 40, 50, 55, 60, 60};
    /** Zone 6 volgt: the cap once zone 6's tier is switched on (1.10.4: the cap stays 60). */
    public static final int CAP_ZONE6 = 60;
    /** The highest tier that exists now (T6 = zone 6 volgt). */
    public static final int MAX_TIER = 5;
    /** 1.10.4: 0.33 (3x faster; was 1). */
    public static volatile double SCHAAL = 0.33;
    /** Xp of one gathering action per tier (index = tier 1..6; T6 = zone 6 volgt). */
    public static final int[] VERZAMEL = {0, 30, 52, 62, 75, 90, 105};
    /**
     * The first level of each tier (index = tier): T1 1 (zone 1/2), T2 12, T3 22, T4 32 (zone 4, Lampionkust), T5 40 (zone 5,
     * Hoogvads), T6 48 (zone 6 volgt: only used once {@link #MAX_TIER} goes to 6). Was 1 / 20 / 35 / 50 / 55 (1.10.3).
     */
    public static final int[] TIER_MIN = {0, 1, 12, 22, 32, 40, 48};
    /** 1.10.4: half xp this many levels above a tier's top (tiers are ~10 levels wide now; was 8 on 15-level tiers). */
    public static final int TE_HOOG = 5;
    /** Generic xp per action: this part of a generic level (fighting: ~1/22 of a level a minute; 20 % of that over ~8 actions). */
    public static final double GENERIEK_DEEL = 1.0 / 900;
    public static final long UITGERUST_MAX = 7200, UITGERUST_PER_ACTIE = 7;
    public static final int OFFLINE_FACTOR = 6;

    private AmbachtXp() {
    }

    public static int xpVoor(int level) {
        return (int) Math.max(1, Math.round((40 + 25 * Math.pow(Math.max(1, level), 1.6)) * SCHAAL));
    }

    public static long totaalVoor(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) {
            t += xpVoor(l);
        }
        return t;
    }

    /** The level this much total xp gives (1..CAP). */
    public static int levelVan(long xp) {
        int l = 1;
        while (l < CAP && xp >= xpVoor(l)) {
            xp -= xpVoor(l);
            l++;
        }
        return l;
    }

    /** Xp into the current level (for the bar). */
    public static long inLevel(long xp) {
        int l = levelVan(xp);
        return xp - totaalVoor(l);
    }

    /** Which tier a level works on (1..{@link #MAX_TIER}). */
    public static int tierVoor(int level) {
        for (int t = MAX_TIER; t >= 1; t--) {
            if (level >= TIER_MIN[t]) {
                return t;
            }
        }
        return 1;
    }

    /** The level a tier needs. */
    public static int tierLevel(int tier) {
        return TIER_MIN[Math.max(1, Math.min(TIER_MIN.length - 1, tier))];
    }

    /** The last level of a tier's band: the next tier's start - 1; the highest tier that exists runs to the cap. */
    public static int tierTop(int tier) {
        int t = Math.max(1, Math.min(TIER_MIN.length - 1, tier));
        return t >= MAX_TIER || t + 1 >= TIER_MIN.length ? CAP : TIER_MIN[t + 1] - 1;
    }

    /** Is this level "far above" the tier (half xp)? */
    public static boolean teHoog(int tier, int level) {
        return level > tierTop(tier) + TE_HOOG;
    }

    /** Xp of one gathering action on a tier for a player of this ambacht level. */
    public static int verzamel(int tier, int level) {
        int t = Math.max(1, Math.min(VERZAMEL.length - 1, tier));
        int xp = VERZAMEL[t];
        return teHoog(t, level) ? xp / 2 : xp;
    }

    /**
     * 1.10.4: an old (1.10.3) ambacht level on the new tier scale, piecewise along the tier starts (1/20/35/50/55/60 ->
     * 1/12/22/32/40/48). Used to rescale the level gates (recipes, hives, story steps), never for saved xp.
     */
    public static int oudNaarNieuw(int oud) {
        int[] van = {1, 20, 35, 50, 55, 60}, naar = {1, 12, 22, 32, 40, 48};
        if (oud <= van[0]) {
            return naar[0];
        }
        for (int i = 1; i < van.length; i++) {
            if (oud <= van[i]) {
                double f = (oud - van[i - 1]) / (double) (van[i] - van[i - 1]);
                return (int) Math.round(naar[i - 1] + f * (naar[i] - naar[i - 1]));
            }
        }
        return Math.min(CAP, naar[naar.length - 1] + (oud - van[van.length - 1]));
    }

    /** Generic xp of one action for this player (only the generic level, never the class). */
    public static long generiek(ServerPlayer p) {
        int lv = Math.max(1, Math.min(Levels.MAX_GENERIEK - 1, Spelers.level(p)));
        return Math.max(1, Math.round(Levels.xpVoorGeneriek(lv) * GENERIEK_DEEL));
    }
}
