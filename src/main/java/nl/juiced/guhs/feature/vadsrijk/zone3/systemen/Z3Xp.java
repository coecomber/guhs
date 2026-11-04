package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import nl.juiced.guhs.feature.vadsrijk.kern.Levels;

/**
 * Zone-3 xp pacing (ronde 18: lv 30 -> 50 in ~6-8 h with the main line, a few side quests and light repeatables). The kern curve
 * continues unchanged ({@code 40 + 25 lv^1.6}, ~185k xp from 30 to 50); the zone-3 sources are sized against it, as a share of
 * the level they are earned at:
 * <ul>
 *   <li>a normal mob: 1/55 of a level ({@link #mob}); an elite = 3x (kern multiplies); so ~2 kills a minute alone is ~40 % of
 *   the pace;</li>
 *   <li>a world/dungeon boss: 35 % of a level ({@link #baas}); a dungeon boss in Vahoeg form the same;</li>
 *   <li>a main-quest step: 45 % of a level, a side quest 18 %, a bounty 5 % ({@link #quest}, {@link #premie}).</li>
 * </ul>
 * 15 main steps + ~6 side quests + kills give roughly 20 levels in 6-8 h (the main line ~7 levels, side ~1, kills ~10, bounties
 * and contracts the rest). z3-wezens / z3-bazen / z3-npc: use these instead of fixed numbers.
 */
public final class Z3Xp {
    public static final double MOB = 1.0 / 55, BAAS = 0.35, HOOFDQUEST = 0.45, ZIJQUEST = 0.18, PREMIE = 0.05, CONTRACT = 0.10;

    private Z3Xp() {
    }

    private static int deel(int level, double deel) {
        int l = Math.max(1, Math.min(Levels.MAX - 1, level));
        return (int) Math.max(1, Math.round(Levels.xpVoor(l) * deel));
    }

    /** Xp of a normal zone-3 mob of this level (elites: kern gives 3x). */
    public static int mob(int level) {
        return deel(level, MOB);
    }

    /** Xp of a zone-3 boss of this level. */
    public static int baas(int level) {
        return deel(level, BAAS);
    }

    /** Xp of a quest at this level (main = true: a main-line step, else a side quest). */
    public static int quest(int level, boolean hoofd) {
        return deel(level, hoofd ? HOOFDQUEST : ZIJQUEST);
    }

    /** Xp of a bounty for a player of this level. */
    public static int premie(int level) {
        return deel(level, PREMIE);
    }

    /** Xp of a hunting contract. */
    public static int contract(int level) {
        return deel(level, CONTRACT);
    }
}
