package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "Vrolijke reeks" (idea 22): cheer-ups in quick succession. Every opgevrolijkt mob (as a helper) counts +1; 5 s
 * ({@link #VENSTER} ticks) without one breaks it. From x10 the player gets +10 % xp until it breaks (x25: +15 %, x50: +20 %),
 * and every milestone ({@link #mijlpaal}) gives a confetti plof. Pure logic (tested), the effects are {@link Beleving}'s.
 */
public final class Reeks {
    public static final int VENSTER = 100;

    private record Stand(int n, long laatst) {
    }

    private static final Map<UUID, Stand> STANDEN = new HashMap<>();

    private Reeks() {
    }

    /** One more cheer-up at game time {@code nu}: the new count (1 after a break). */
    public static synchronized int tel(UUID speler, long nu) {
        Stand s = STANDEN.get(speler);
        int n = s == null || nu - s.laatst() > VENSTER ? 1 : s.n() + 1;
        STANDEN.put(speler, new Stand(n, nu));
        return n;
    }

    /** The running count (0 = none or broken). */
    public static synchronized int stand(UUID speler, long nu) {
        Stand s = STANDEN.get(speler);
        return s == null || nu - s.laatst() > VENSTER ? 0 : s.n();
    }

    /** Removes the broken streaks and returns them (player -> the count it reached). */
    public static synchronized Map<UUID, Integer> verlopen(long nu) {
        Map<UUID, Integer> weg = new LinkedHashMap<>();
        Iterator<Map.Entry<UUID, Stand>> it = STANDEN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Stand> e = it.next();
            if (nu - e.getValue().laatst() > VENSTER) {
                weg.put(e.getKey(), e.getValue().n());
                it.remove();
            }
        }
        return weg;
    }

    public static synchronized void wis(UUID speler) {
        STANDEN.remove(speler);
    }

    public static synchronized void wisAlles() {
        STANDEN.clear();
    }

    /** The xp factor for a running count. */
    public static double xpFactor(int n) {
        return n >= 50 ? 1.20 : n >= 25 ? 1.15 : n >= 10 ? 1.10 : 1.0;
    }

    /** A milestone: x10, x25, x50, then every 25. */
    public static boolean mijlpaal(int n) {
        return n == 10 || n == 25 || n == 50 || (n > 50 && n % 25 == 0);
    }

    /** The pitch of the plof note for count {@code n}: up a major scale per cheer-up, from 0.6 to 2.0 (then it stays on top). */
    public static float toon(int n) {
        int[] ladder = {0, 2, 4, 5, 7, 9, 11};
        int stap = Math.max(0, n - 1);
        int halve = ladder[stap % 7] + 12 * (stap / 7);
        return (float) Math.min(2.0, 0.6 * Math.pow(2, halve / 12.0));
    }
}
