package nl.juiced.guhs.feature.vadsrijk.kern;

/** Levels 1..{@link #MAX} (zone 1 cap 20, zone 2 cap 35), the xp curve, and the automatic stat growth per level. */
public final class Levels {
    /** z2-systemen: 20 -> 35 (zone 2 "De Splinterpieken" is lv 20-35; the curve below simply continues, 1 skill point per level). */
    public static final int MAX = 35;

    private Levels() {
    }

    /** The xp needed to go from {@code level} to {@code level + 1}. */
    public static int xpVoor(int level) {
        return (int) Math.round(40 + 25 * Math.pow(level, 1.6));
    }

    /** Total xp needed to reach {@code level} from level 1. */
    public static long totaalVoor(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) {
            t += xpVoor(l);
        }
        return t;
    }

    /** The level that this much total xp gives (1..MAX). */
    public static int levelVan(long xp) {
        int l = 1;
        while (l < MAX && xp >= xpVoor(l)) {
            xp -= xpVoor(l);
            l++;
        }
        return l;
    }

    /** The xp into the current level (for the bar). */
    public static long restXp(long xp) {
        int l = 1;
        while (l < MAX && xp >= xpVoor(l)) {
            xp -= xpVoor(l);
            l++;
        }
        return l >= MAX ? 0 : xp;
    }

    /** Skill points at this level (1 per level from 2). */
    public static int skillpunten(int level) {
        return Math.max(0, level - 1);
    }
}
