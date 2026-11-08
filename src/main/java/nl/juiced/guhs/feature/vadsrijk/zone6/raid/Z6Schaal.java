package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

/**
 * U-Z6RAID: the raid's scaling math (ZONE6_PLAN §8), pure numbers so the gametests can check them.
 * <ul>
 *   <li>{@link #factor}: HP of every raid boss = base x (0.55 + 0.45 n) for n players (n = the group size at the START of the
 *       raid, 2..10; it never changes during the run, so a player who leaves does not make the boss easier or harder);</li>
 *   <li>{@link #schadeFactor}: a boss HIT does NOT scale with x f(n) (a 10-player raid would then hit 5x as hard per hit; the
 *       plan's "levenspunten en schade" is read as: the group's total incoming damage grows because there are more targets,
 *       more adds and more mechanic pieces). Hits grow only a little: x (0.9 + 0.02 n) (2 players 0.94, 10 players 1.10);</li>
 *   <li>{@link #mech}: mechanic counts (plates, mirrors, beacons, pillars, rings caught): ceil(n / 2), at least 2. With 2 players
 *       2, with 10 players 5. Every mechanic is doable with 2: more players make it busier, not different;</li>
 *   <li>{@link #adds}: add waves: 1 + floor(n / 3) (2-3 players 1, 10 players 4).</li>
 * </ul>
 */
public final class Z6Schaal {
    public static final int MIN = 2, MAX = 10;

    private Z6Schaal() {
    }

    public static int n(int spelers) {
        return Math.max(MIN, Math.min(MAX, spelers));
    }

    public static double factor(int spelers) {
        return 0.55 + 0.45 * n(spelers);
    }

    public static double schadeFactor(int spelers) {
        return 0.9 + 0.02 * n(spelers);
    }

    public static int mech(int spelers) {
        return Math.max(2, (n(spelers) + 1) / 2);
    }

    public static int adds(int spelers) {
        return 1 + n(spelers) / 3;
    }
}
