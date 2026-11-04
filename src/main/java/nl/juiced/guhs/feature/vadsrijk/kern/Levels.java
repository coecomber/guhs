package nl.juiced.guhs.feature.vadsrijk.kern;

/**
 * Levels 1..{@link #MAX} and the xp curve. g-klassen (round 2): a player has TWO levels, both from xp: the <b>generic</b> level
 * (Vads, Verdediging, area/quest gates, armour/jewellery) and a <b>class</b> level per class (main stat, skill points, weapons,
 * job advancement). Both cap at 50 and use the same curve shape; each has its own scale ({@link #SCHAAL_GENERIEK},
 * {@link #SCHAAL_KLASSE}, set from {@code config/guhs-vadsrijk-balans.toml}).
 */
public final class Levels {
    /** z2-systemen: 20 -> 35; z3-systemen: 35 -> 50 (zone 3 "De Schemerwortel" is lv 30-50; the curve simply continues). */
    public static final int MAX = 50;
    /** The generic level cap (the same as the class cap). */
    public static final int MAX_GENERIEK = 50;
    /** Xp curve scales (1 = the zone-1 curve); tunable separately in the balans config. */
    public static volatile double SCHAAL_KLASSE = 1, SCHAAL_GENERIEK = 1;

    private Levels() {
    }

    private static int stap(int level, double schaal) {
        return (int) Math.max(1, Math.round((40 + 25 * Math.pow(level, 1.6)) * schaal));
    }

    /** The class xp needed to go from {@code level} to {@code level + 1}. */
    public static int xpVoor(int level) {
        return stap(level, SCHAAL_KLASSE);
    }

    /** The generic xp needed to go from {@code level} to {@code level + 1}. */
    public static int xpVoorGeneriek(int level) {
        return stap(level, SCHAAL_GENERIEK);
    }

    /** Total class xp needed to reach {@code level} from level 1. */
    public static long totaalVoor(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) {
            t += xpVoor(l);
        }
        return t;
    }

    /** Total generic xp needed to reach {@code level}. */
    public static long totaalVoorGeneriek(int level) {
        long t = 0;
        for (int l = 1; l < level; l++) {
            t += xpVoorGeneriek(l);
        }
        return t;
    }

    /** The class level that this much total xp gives (1..MAX). */
    public static int levelVan(long xp) {
        int l = 1;
        while (l < MAX && xp >= xpVoor(l)) {
            xp -= xpVoor(l);
            l++;
        }
        return l;
    }

    /** The generic level of this much generic xp (1..MAX_GENERIEK). */
    public static int generiekVan(long xp) {
        int l = 1;
        while (l < MAX_GENERIEK && xp >= xpVoorGeneriek(l)) {
            xp -= xpVoorGeneriek(l);
            l++;
        }
        return l;
    }

    /** The class xp into the current level (for the bar). */
    public static long restXp(long xp) {
        int l = 1;
        while (l < MAX && xp >= xpVoor(l)) {
            xp -= xpVoor(l);
            l++;
        }
        return l >= MAX ? 0 : xp;
    }

    /** The generic xp into the current level. */
    public static long restXpGeneriek(long xp) {
        int l = 1;
        while (l < MAX_GENERIEK && xp >= xpVoorGeneriek(l)) {
            xp -= xpVoorGeneriek(l);
            l++;
        }
        return l >= MAX_GENERIEK ? 0 : xp;
    }

    /** Skill points at this class level (1 per level from 2). */
    public static int skillpunten(int level) {
        return Math.max(0, level - 1);
    }
}
