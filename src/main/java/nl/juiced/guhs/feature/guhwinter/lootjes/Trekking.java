package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.Random;

/**
 * The Sint's draw: a random derangement (nobody draws themselves). Sattolo's shuffle gives one big circle (A draws B, B
 * draws C, ..., the last draws A), so with three or more there are never two players who drew each other: that keeps the
 * secret a little longer.
 */
public final class Trekking {
    private Trekking() {
    }

    /**
     * {@code uit[i]} = the receiver of giver {@code i}. For {@code n >= 2} never {@code uit[i] == i}, and every receiver
     * comes up exactly once. {@code n < 2}: an empty array (no draw).
     */
    public static int[] trek(int n, Random r) {
        if (n < 2) {
            return new int[0];
        }
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = r.nextInt(i);   // (j < i: Sattolo, a single cycle)
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return p;
    }

    /** Is this a draw where everyone gives once, receives once and never draws themselves? */
    public static boolean geldig(int[] p) {
        boolean[] gezien = new boolean[p.length];
        for (int i = 0; i < p.length; i++) {
            if (p[i] < 0 || p[i] >= p.length || p[i] == i || gezien[p[i]]) {
                return false;
            }
            gezien[p[i]] = true;
        }
        return p.length >= 2;
    }
}
