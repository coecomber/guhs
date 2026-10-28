package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * Guh op een rij: four in a row on a standing board of 7 columns and 6 rows, with guh discs and Blub discs. Pure game logic
 * (no Minecraft world): {@link BordSpel} keeps one per match, the screen only draws it. Row 0 is the bottom row.
 * <p>
 * The guh-bot has three levels: makkelijk (wins when it can, otherwise random), normaal (also blocks your four, likes the
 * middle) and lastig (looks a few moves ahead).
 */
public final class OpEenRij {
    public static final int BREED = 7, HOOG = 6;
    public static final byte LEEG = 0, GUH = 1, BLUB = 2;
    public static final int MAKKELIJK = 0, NORMAAL = 1, LASTIG = 2;
    private static final int DIEPTE = 5;

    private final byte[] vak = new byte[BREED * HOOG];

    public byte get(int kolom, int rij) {
        return vak[kolom + rij * BREED];
    }

    public byte[] vakken() {
        return vak.clone();
    }

    public void zet(byte[] vakken) {
        System.arraycopy(vakken, 0, vak, 0, Math.min(vakken.length, vak.length));
    }

    public boolean kan(int kolom) {
        return kolom >= 0 && kolom < BREED && get(kolom, HOOG - 1) == LEEG;
    }

    /** Drops a disc into a column; returns the row it lands in, or -1 when the column is full (nothing changes). */
    public int laat(int kolom, byte wie) {
        if (!kan(kolom)) {
            return -1;
        }
        for (int rij = 0; rij < HOOG; rij++) {
            if (get(kolom, rij) == LEEG) {
                vak[kolom + rij * BREED] = wie;
                return rij;
            }
        }
        return -1;
    }

    private void haalWeg(int kolom) {
        for (int rij = HOOG - 1; rij >= 0; rij--) {
            if (get(kolom, rij) != LEEG) {
                vak[kolom + rij * BREED] = LEEG;
                return;
            }
        }
    }

    public boolean vol() {
        for (int k = 0; k < BREED; k++) {
            if (kan(k)) {
                return false;
            }
        }
        return true;
    }

    /** Who has four in a row (GUH or BLUB), or LEEG. */
    public byte winnaar() {
        int[][] richtingen = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int k = 0; k < BREED; k++) {
            for (int r = 0; r < HOOG; r++) {
                byte w = get(k, r);
                if (w == LEEG) {
                    continue;
                }
                for (int[] d : richtingen) {
                    int n = 1;
                    while (n < 4) {
                        int kk = k + d[0] * n, rr = r + d[1] * n;
                        if (kk < 0 || kk >= BREED || rr < 0 || rr >= HOOG || get(kk, rr) != w) {
                            break;
                        }
                        n++;
                    }
                    if (n == 4) {
                        return w;
                    }
                }
            }
        }
        return LEEG;
    }

    public static byte ander(byte wie) {
        return wie == GUH ? BLUB : GUH;
    }

    private List<Integer> vrijeKolommen() {
        List<Integer> l = new ArrayList<>();
        for (int k = 0; k < BREED; k++) {
            if (kan(k)) {
                l.add(k);
            }
        }
        return l;
    }

    /** The column that wins at once for `wie`, or -1. */
    private int winKolom(byte wie) {
        for (int k : vrijeKolommen()) {
            laat(k, wie);
            boolean wint = winnaar() == wie;
            haalWeg(k);
            if (wint) {
                return k;
            }
        }
        return -1;
    }

    /** The guh-bot's move (the board itself is not changed). -1 only when the board is full. */
    public int botZet(byte bot, int niveau, RandomSource rnd) {
        List<Integer> vrij = vrijeKolommen();
        if (vrij.isEmpty()) {
            return -1;
        }
        int win = winKolom(bot);
        if (win >= 0) {
            return win;
        }
        if (niveau == MAKKELIJK) {
            return vrij.get(rnd.nextInt(vrij.size()));
        }
        int blok = winKolom(ander(bot));
        if (blok >= 0) {
            return blok;
        }
        if (niveau == NORMAAL) {
            // likes the middle, and never hands the other one a win on top of its own disc
            List<Integer> goed = new ArrayList<>();
            for (int k : vrij) {
                laat(k, bot);
                boolean gevaar = winKolom(ander(bot)) >= 0;
                haalWeg(k);
                if (!gevaar) {
                    goed.add(k);
                }
            }
            List<Integer> kies = goed.isEmpty() ? vrij : goed;
            int beste = kies.get(0);
            for (int k : kies) {
                if (Math.abs(k - 3) < Math.abs(beste - 3) || (Math.abs(k - 3) == Math.abs(beste - 3) && rnd.nextBoolean())) {
                    beste = k;
                }
            }
            return beste;
        }
        int beste = vrij.get(0);
        int besteWaarde = Integer.MIN_VALUE;
        for (int k : volgorde(vrij)) {
            laat(k, bot);
            int w = -negamax(ander(bot), DIEPTE - 1, -1_000_000, 1_000_000);
            haalWeg(k);
            if (w > besteWaarde) {
                besteWaarde = w;
                beste = k;
            }
        }
        return beste;
    }

    private static List<Integer> volgorde(List<Integer> vrij) {
        List<Integer> l = new ArrayList<>(vrij);
        l.sort((a, b) -> Integer.compare(Math.abs(a - 3), Math.abs(b - 3)));
        return l;
    }

    private int negamax(byte aanZet, int diepte, int alfa, int beta) {
        byte w = winnaar();
        if (w != LEEG) {
            return w == aanZet ? 100_000 + diepte : -100_000 - diepte;
        }
        if (vol()) {
            return 0;
        }
        if (diepte == 0) {
            return waarde(aanZet) - waarde(ander(aanZet));
        }
        int beste = Integer.MIN_VALUE + 1;
        for (int k : volgorde(vrijeKolommen())) {
            laat(k, aanZet);
            int v = -negamax(ander(aanZet), diepte - 1, -beta, -alfa);
            haalWeg(k);
            beste = Math.max(beste, v);
            alfa = Math.max(alfa, v);
            if (alfa >= beta) {
                break;
            }
        }
        return beste;
    }

    /** A rough score: open lines of two and three, and the middle column. */
    private int waarde(byte wie) {
        int s = 0;
        for (int r = 0; r < HOOG; r++) {
            if (get(3, r) == wie) {
                s += 3;
            }
        }
        int[][] richtingen = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int k = 0; k < BREED; k++) {
            for (int r = 0; r < HOOG; r++) {
                for (int[] d : richtingen) {
                    int eigen = 0, leeg = 0;
                    boolean past = true;
                    for (int n = 0; n < 4; n++) {
                        int kk = k + d[0] * n, rr = r + d[1] * n;
                        if (kk < 0 || kk >= BREED || rr < 0 || rr >= HOOG) {
                            past = false;
                            break;
                        }
                        byte b = get(kk, rr);
                        if (b == wie) {
                            eigen++;
                        } else if (b == LEEG) {
                            leeg++;
                        }
                    }
                    if (past && eigen + leeg == 4) {
                        s += eigen == 3 ? 20 : eigen == 2 ? 5 : 0;
                    }
                }
            }
        }
        return s;
    }
}
