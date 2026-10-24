package nl.juiced.guhs.feature.grotspellen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Lichtjespuzzel, the puzzle without the world: a 7x7 raster of mushroom lamps ("lights out", turned around: the goal is
 * every lamp ON, so Meester Knipperlicht can read his paper again). Pressing a lamp toggles it and its four neighbours.
 * <p>
 * A new puzzle ({@link #puzzel}) starts from all lamps on and presses a few different random lamps, so it can always be
 * solved; {@link #oplossing} finds the fewest presses (linear algebra over GF(2): every lamp is a bit, a press adds its
 * pattern; the 7x7 system has a unique solution, but the code also copes with free variables).
 */
public final class LichtjesRaster {
    public static final int N = 7;
    private static final int CELLEN = N * N;
    private final boolean[] aan = new boolean[CELLEN];

    /** All lamps on. */
    public LichtjesRaster() {
        java.util.Arrays.fill(aan, true);
    }

    public LichtjesRaster kopie() {
        LichtjesRaster k = new LichtjesRaster();
        System.arraycopy(aan, 0, k.aan, 0, CELLEN);
        return k;
    }

    public boolean aan(int x, int z) {
        return aan[x * N + z];
    }

    public void zet(int x, int z, boolean waarde) {
        aan[x * N + z] = waarde;
    }

    /** Presses (x, z): it and its four neighbours (inside the raster) switch. False outside the raster. */
    public boolean druk(int x, int z) {
        if (x < 0 || z < 0 || x >= N || z >= N) {
            return false;
        }
        wissel(x, z);
        wissel(x - 1, z);
        wissel(x + 1, z);
        wissel(x, z - 1);
        wissel(x, z + 1);
        return true;
    }

    private void wissel(int x, int z) {
        if (x >= 0 && z >= 0 && x < N && z < N) {
            aan[x * N + z] = !aan[x * N + z];
        }
    }

    public boolean allesAan() {
        for (boolean b : aan) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public int aantalAan() {
        int n = 0;
        for (boolean b : aan) {
            n += b ? 1 : 0;
        }
        return n;
    }

    // --- per level --------------------------------------------------------------------------------------------------
    /** How many lamps the puzzle is scrambled with: 4, 7, 10 (= the fewest presses to solve it). */
    public static int drukken(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 4;
            case MEDIUM -> 7;
            case LASTIG -> 10;
        };
    }

    /** The time (ticks): 120, 100, 80 seconds. */
    public static int speelTicks(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 120 * 20;
            case MEDIUM -> 100 * 20;
            case LASTIG -> 80 * 20;
        };
    }

    /** A new puzzle: all on, then {@code drukken} different random lamps pressed (never all on again). */
    public static LichtjesRaster puzzel(RandomSource rng, int drukken) {
        for (int poging = 0; poging < 100; poging++) {
            LichtjesRaster r = new LichtjesRaster();
            List<Integer> vrij = new ArrayList<>();
            for (int i = 0; i < CELLEN; i++) {
                vrij.add(i);
            }
            for (int k = 0; k < Math.min(drukken, CELLEN); k++) {
                int i = vrij.remove(rng.nextInt(vrij.size()));
                r.druk(i / N, i % N);
            }
            if (!r.allesAan()) {
                return r;
            }
        }
        LichtjesRaster r = new LichtjesRaster();
        r.druk(N / 2, N / 2);
        return r;
    }

    // --- solving ----------------------------------------------------------------------------------------------------
    /** The pattern of a press on cell i, as bits. */
    private static long patroon(int i) {
        int x = i / N, z = i % N;
        long p = 1L << i;
        if (x > 0) {
            p |= 1L << (i - N);
        }
        if (x < N - 1) {
            p |= 1L << (i + N);
        }
        if (z > 0) {
            p |= 1L << (i - 1);
        }
        if (z < N - 1) {
            p |= 1L << (i + 1);
        }
        return p;
    }

    /**
     * The fewest presses that make every lamp on (cell indexes x*7+z), or null when it can't be done (never for a
     * {@link #puzzel}). The press matrix is symmetric, so row i of the system is the pattern of cell i.
     */
    public int[] oplossing() {
        long[] rij = new long[CELLEN];
        boolean[] rechts = new boolean[CELLEN];
        for (int i = 0; i < CELLEN; i++) {
            rij[i] = patroon(i);
            rechts[i] = !aan[i];
        }
        int[] spil = new int[CELLEN];
        java.util.Arrays.fill(spil, -1);
        int r = 0;
        List<Integer> vrijeKolommen = new ArrayList<>();
        for (int kol = 0; kol < CELLEN; kol++) {
            int gevonden = -1;
            for (int i = r; i < CELLEN; i++) {
                if ((rij[i] >> kol & 1L) != 0) {
                    gevonden = i;
                    break;
                }
            }
            if (gevonden < 0) {
                vrijeKolommen.add(kol);
                continue;
            }
            long t = rij[r];
            rij[r] = rij[gevonden];
            rij[gevonden] = t;
            boolean tb = rechts[r];
            rechts[r] = rechts[gevonden];
            rechts[gevonden] = tb;
            for (int i = 0; i < CELLEN; i++) {
                if (i != r && (rij[i] >> kol & 1L) != 0) {
                    rij[i] ^= rij[r];
                    rechts[i] ^= rechts[r];
                }
            }
            spil[r] = kol;
            r++;
        }
        for (int i = r; i < CELLEN; i++) {
            if (rechts[i]) {
                return null;                                   // 0 = 1: no solution
            }
        }
        // a solution with every free variable 0, then try every combination of the free ones (there are none for 7x7)
        long beste = -1;
        int vrij = vrijeKolommen.size();
        long combinaties = vrij > 16 ? 1 : 1L << vrij;
        for (long c = 0; c < combinaties; c++) {
            long x = 0;
            for (int f = 0; f < vrij; f++) {
                if ((c >> f & 1L) != 0) {
                    x |= 1L << vrijeKolommen.get(f);
                }
            }
            for (int i = r - 1; i >= 0; i--) {
                int kol = spil[i];
                boolean waarde = rechts[i];
                long rest = rij[i] & ~(1L << kol) & x;
                waarde ^= (Long.bitCount(rest) & 1) == 1;
                if (waarde) {
                    x |= 1L << kol;
                }
            }
            if (beste < 0 || Long.bitCount(x) < Long.bitCount(beste)) {
                beste = x;
            }
        }
        int[] uit = new int[Long.bitCount(beste)];
        int k = 0;
        for (int i = 0; i < CELLEN; i++) {
            if ((beste >> i & 1L) != 0) {
                uit[k++] = i;
            }
        }
        return uit;
    }

    /** The fewest presses to solve it (-1: unsolvable). */
    public int minimaal() {
        int[] o = oplossing();
        return o == null ? -1 : o.length;
    }

    // --- scoring ----------------------------------------------------------------------------------------------------
    /** The points on the board: solved 100 + seconds left + up to 50 for few presses (10 off per extra press); not solved: 0. */
    public static int punten(boolean opgelost, int ticksOver, int stappen, int minimaal) {
        if (!opgelost) {
            return 0;
        }
        int extra = Math.max(0, stappen - minimaal);
        return 100 + Math.max(0, ticksOver) / 20 + Math.max(0, 50 - 10 * extra);
    }

    /**
     * Lampjesmunten: solved, a base per level (10/20/34) + a time bonus (up to 5/10/13, for the part of the time left) +
     * a press bonus (5/10/13, minus 2 per press more than needed); not solved: 1 per 10 lamps on. +5 for a record.
     */
    public static int munten(Niveau n, boolean opgelost, int ticksOver, int speelTicks, int stappen, int minimaal, int lampenAan, boolean record) {
        int basis, bonus;
        switch (n) {
            case MAKKELIJK -> {
                basis = 10;
                bonus = 5;
            }
            case MEDIUM -> {
                basis = 20;
                bonus = 10;
            }
            default -> {
                basis = 34;
                bonus = 13;
            }
        }
        int m;
        if (opgelost) {
            int tijd = (int) Math.round(bonus * Math.max(0, ticksOver) / (double) Math.max(1, speelTicks));
            int stap = Math.max(0, bonus - 2 * Math.max(0, stappen - minimaal));
            m = basis + tijd + stap;
        } else {
            m = lampenAan / 10;
        }
        return GrotSpel.munten(n, m, record);
    }
}
