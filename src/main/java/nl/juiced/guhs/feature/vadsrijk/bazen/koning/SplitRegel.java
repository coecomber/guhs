package nl.juiced.guhs.feature.vadsrijk.bazen.koning;

/**
 * Koning Klodder's split rule (ronde 7): he splits into 3 Kroonklodders. The moment the first one is cheered up a window of
 * {@link #VENSTER} ticks starts; all three must be beaten inside it. If the window closes (or {@link #MAX} ticks pass without
 * a first kill) the survivors melt back and he heals. Pure logic (game time in, verdict out) so the gametest can check it.
 */
public final class SplitRegel {
    public static final int VENSTER = 240, MAX = 1200;

    public enum Uitslag {
        BEZIG, GELUKT, MISLUKT
    }

    private final long start;
    private long eerste = -1;
    private final boolean[] weg = new boolean[3];

    public SplitRegel(long start) {
        this.start = start;
    }

    public void verslagen(int i, long nu) {
        if (i < 0 || i >= 3 || weg[i]) {
            return;
        }
        if (eerste < 0) {
            eerste = nu;
        }
        if (nu - eerste <= VENSTER) {
            weg[i] = true;
        }
    }

    public int overlevers() {
        int n = 0;
        for (boolean b : weg) {
            n += b ? 0 : 1;
        }
        return n;
    }

    /** Ticks left in the window, or -1 while it hasn't started. */
    public int resterend(long nu) {
        return eerste < 0 ? -1 : (int) Math.max(0, VENSTER - (nu - eerste));
    }

    public Uitslag uitslag(long nu) {
        if (overlevers() == 0) {
            return Uitslag.GELUKT;
        }
        if ((eerste >= 0 && nu - eerste > VENSTER) || (eerste < 0 && nu - start > MAX)) {
            return Uitslag.MISLUKT;
        }
        return Uitslag.BEZIG;
    }

    public boolean weg(int i) {
        return weg[i];
    }
}
