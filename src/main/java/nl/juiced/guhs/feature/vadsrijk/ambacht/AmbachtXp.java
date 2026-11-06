package nl.juiced.guhs.feature.vadsrijk.ambacht;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * The Ambachten xp curve and pacing (VOORSTEL_PROFESSIONS §5.1, A1/A2):
 * <ul>
 *   <li>same shape as combat ({@code 40 + 25 lv^1.6}), own scale {@link #SCHAAL}; cap {@link #CAP} (60 with zones 4 + 5, +5 per zone);</li>
 *   <li>a gathering action gives a fixed amount per resource tier ({@link #VERZAMEL}), sized for ~8 actions a minute:
 *   1 -> 20 in ~1.5 h on T1, 20 -> 35 in ~3 h on T2, 35 -> 50 in ~5 h on T3 (crafting and orders on top make it a bit quicker).
 *   Far above the tier ({@link #TE_HOOG} levels over its top) it gives half: you rise fastest on your own tier;</li>
 *   <li>generic (main-level) xp: a little, ~20 % of what fighting gives in the same time ({@link #generiek});</li>
 *   <li>Uitgerust (rested): offline you build up to 2 h of double ambacht-xp (1 s per 6 s offline); every action uses 7 s.</li>
 * </ul>
 */
public final class AmbachtXp {
    /**
     * The level cap (A1 + the user rule "de ambachten groeien ALTIJD mee met elke zone"): +5 per zone. U-AMBACHT2: zone 4 (55) and
     * zone 5 (60) are in, so 60. Zone 6 (Zijdeland, lv 60-70) raises it to {@link #CAP_ZONE6} once its tier is switched on
     * ({@link Zone6Haken}, "zone 6 volgt").
     */
    public static volatile int CAP = 60;
    /** The cap per zone that is in the game (index = zone): the cap is the one of the highest zone that exists. */
    public static final int[] CAP_PER_ZONE = {0, 30, 40, 50, 55, 60, 70};
    /** Zone 6 volgt: the cap once zone 6's tier is switched on. */
    public static final int CAP_ZONE6 = 70;
    /** The highest tier that exists now (T6 = zone 6 volgt). */
    public static final int MAX_TIER = 5;
    public static volatile double SCHAAL = 1;
    /** Xp of one gathering action per tier (index = tier 1..6; T6 = zone 6 volgt). */
    public static final int[] VERZAMEL = {0, 30, 52, 62, 75, 90, 105};
    /**
     * The level band of each tier (index = tier): T1 1-20, T2 20-35, T3 35-50, T4 50-55 (zone 4, Lampionkust), T5 55-60 (zone 5,
     * Hoogvads), T6 60-70 (zone 6 volgt: only used once {@link #MAX_TIER} goes to 6).
     */
    public static final int[] TIER_MIN = {0, 1, 20, 35, 50, 55, 60}, TIER_MAX = {0, 20, 35, 50, 55, 60, 70};
    public static final int TE_HOOG = 8;
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

    /** Xp of one gathering action on a tier for a player of this ambacht level. */
    public static int verzamel(int tier, int level) {
        int t = Math.max(1, Math.min(VERZAMEL.length - 1, tier));
        int xp = VERZAMEL[t];
        return level > TIER_MAX[t] + TE_HOOG ? xp / 2 : xp;
    }

    /** Generic xp of one action for this player (only the generic level, never the class). */
    public static long generiek(ServerPlayer p) {
        int lv = Math.max(1, Math.min(Levels.MAX_GENERIEK - 1, Spelers.level(p)));
        return Math.max(1, Math.round(Levels.xpVoorGeneriek(lv) * GENERIEK_DEEL));
    }
}
