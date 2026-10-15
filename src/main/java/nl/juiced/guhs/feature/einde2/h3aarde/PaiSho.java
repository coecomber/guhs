package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Guh Pai Sho, the simple version (CONTRACT_E2_B 12a.4: "take turns placing tiles, first to form a lotus wins"): a board of
 * {@link #N} x {@link #N}; two players put down a tile in turn on any empty square; whoever first has a <b>lotus</b> wins: a
 * tile of theirs with all four petals around it (the squares north, east, south and west of it) theirs as well. A full
 * board without a lotus is a draw ("een gelijkspel is ook een spel, njeg"). Pure rules, no Minecraft: the story game with
 * Miroh ({@link PaiShoSpel}) and the table ({@link PaiShoTafelBlockEntity}) both play with this.
 * <p>
 * The board is a string of {@link #N}*{@link #N} characters: '.' empty, 'a' a tile of the first player, 'b' of the second.
 */
public final class PaiSho {
    public static final int N = 5;
    public static final char LEEG = '.', A = 'a', B = 'b';
    public static final String NIEUW = ".".repeat(N * N);

    /** Every lotus on the board: the middle and its four petals (the middle may not be on the edge). */
    public static final List<int[]> LOTUSSEN = lotussen();

    private PaiSho() {
    }

    private static List<int[]> lotussen() {
        List<int[]> uit = new ArrayList<>();
        for (int y = 1; y < N - 1; y++) {
            for (int x = 1; x < N - 1; x++) {
                uit.add(new int[] {cel(x, y), cel(x, y - 1), cel(x + 1, y), cel(x, y + 1), cel(x - 1, y)});
            }
        }
        return List.copyOf(uit);
    }

    public static int cel(int x, int y) {
        return y * N + x;
    }

    /** Who has a lotus ('a', 'b'), or LEEG. */
    public static char winnaar(String bord) {
        for (int[] l : LOTUSSEN) {
            char c = bord.charAt(l[0]);
            if (c == LEEG) {
                continue;
            }
            boolean heel = true;
            for (int i = 1; i < l.length && heel; i++) {
                heel = bord.charAt(l[i]) == c;
            }
            if (heel) {
                return c;
            }
        }
        return LEEG;
    }

    /** The cells of the lotus that won (empty: none). */
    public static int[] winnendeLotus(String bord) {
        for (int[] l : LOTUSSEN) {
            char c = bord.charAt(l[0]);
            if (c == LEEG) {
                continue;
            }
            boolean heel = true;
            for (int i = 1; i < l.length && heel; i++) {
                heel = bord.charAt(l[i]) == c;
            }
            if (heel) {
                return l.clone();
            }
        }
        return new int[0];
    }

    public static boolean vol(String bord) {
        return bord.indexOf(LEEG) < 0;
    }

    /** Over: a lotus, or a full board. */
    public static boolean klaar(String bord) {
        return winnaar(bord) != LEEG || vol(bord);
    }

    /** Whose turn: 'a' begins; after every tile the other. */
    public static char beurt(String bord) {
        int a = 0, b = 0;
        for (int i = 0; i < bord.length(); i++) {
            a += bord.charAt(i) == A ? 1 : 0;
            b += bord.charAt(i) == B ? 1 : 0;
        }
        return a <= b ? A : B;
    }

    /** The board with a tile of {@code wie} on cel (null: not allowed: taken, out of the board, over, or not their turn). */
    public static String zet(String bord, int cel, char wie) {
        if (cel < 0 || cel >= N * N || bord.charAt(cel) != LEEG || klaar(bord) || beurt(bord) != wie) {
            return null;
        }
        return bord.substring(0, cel) + wie + bord.substring(cel + 1);
    }

    public static char ander(char wie) {
        return wie == A ? B : A;
    }

    /**
     * The move of a guh for {@code wie}: a lotus it can finish now; else the one square that stops the other's lotus; else
     * the square in the most open lotuses (its own count weighed up, the other's half). {@code kans} = how often (0..1) the
     * guh simply plays for the fun of it (a random free square): Miroh plays "for the gezelligheid".
     */
    public static int guhZet(String bord, char wie, Random r, double kans) {
        List<Integer> vrij = new ArrayList<>();
        for (int i = 0; i < bord.length(); i++) {
            if (bord.charAt(i) == LEEG) {
                vrij.add(i);
            }
        }
        if (vrij.isEmpty()) {
            return -1;
        }
        for (int c : vrij) {
            if (winnaar(bord.substring(0, c) + wie + bord.substring(c + 1)) == wie) {
                return c;
            }
        }
        char ander = ander(wie);
        for (int c : vrij) {
            if (winnaar(bord.substring(0, c) + ander + bord.substring(c + 1)) == ander) {
                return c;
            }
        }
        if (r.nextDouble() < kans) {
            return vrij.get(r.nextInt(vrij.size()));
        }
        int beste = vrij.get(0);
        double score = -1;
        for (int c : vrij) {
            double s = r.nextDouble() * 0.1 + (c == cel(N / 2, N / 2) ? 0.5 : 0);
            for (int[] l : LOTUSSEN) {
                boolean in = false;
                int eigen = 0, zijn = 0;
                for (int q : l) {
                    in |= q == c;
                    eigen += bord.charAt(q) == wie ? 1 : 0;
                    zijn += bord.charAt(q) == ander ? 1 : 0;
                }
                if (!in) {
                    continue;
                }
                if (zijn == 0) {
                    s += 1 + eigen * eigen;
                }
                if (eigen == 0) {
                    s += 0.5 * (1 + zijn * zijn);
                }
            }
            if (s > score) {
                score = s;
                beste = c;
            }
        }
        return beste;
    }
}
