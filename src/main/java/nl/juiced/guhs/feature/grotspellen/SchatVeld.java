package nl.juiced.guhs.feature.grotspellen;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Schatgraven, the game itself without the world: a square field of {@link #GROOTTE} x {@link #GROOTTE} soft blocks, of
 * which the middle part is played ({@link #actief}: 7, 11 or 15 wide, per level). Under it lie treasures (6/8/10), one
 * golden cheese (a bonus) and a few piepkazen (a squeaky trap: 10 seconds off the clock). Digging a cell opens it for good;
 * Snuffie the snuffelguh smells the nearest treasure (or the golden cheese): {@link #hint}.
 * <p>
 * Cells are (x, z) from the field's low corner, 0..GROOTTE-1. Points: 10 per treasure, 25 for the golden cheese, and when
 * every treasure is found before the time is up one point per two seconds left ({@link #punten}).
 */
public final class SchatVeld {
    public static final int GROOTTE = 15;
    public static final int SCHAT_PUNTEN = 10, GOUD_PUNTEN = 25;
    /** What a piepkaas costs (ticks off the clock). */
    public static final int PIEP_STRAF = 200;
    /** A game lasts 90 seconds. */
    public static final int SPEELTICKS = 90 * 20;

    public enum Inhoud { LEEG, SCHAT, GOUD, PIEP }

    public enum Hint { KOUD, WARM, HEET }

    private final Niveau niveau;
    private final int van, maat;
    private final Inhoud[][] inhoud = new Inhoud[GROOTTE][GROOTTE];
    private final boolean[][] open = new boolean[GROOTTE][GROOTTE];
    private int gevonden, piepen;
    private boolean goud;

    public SchatVeld(Niveau niveau, RandomSource rng) {
        this.niveau = niveau;
        this.maat = actief(niveau);
        this.van = (GROOTTE - maat) / 2;
        for (Inhoud[] rij : inhoud) {
            java.util.Arrays.fill(rij, Inhoud.LEEG);
        }
        leg(rng, Inhoud.SCHAT, schatten(niveau));
        leg(rng, Inhoud.GOUD, 1);
        leg(rng, Inhoud.PIEP, piepkazen(niveau));
    }

    /** (tests) A field with exactly these things at these cells (each {x, z}); the rest is empty. */
    public SchatVeld(Niveau niveau, int[][] schatten, @Nullable int[] goudenKaas, int[][] piep) {
        this.niveau = niveau;
        this.maat = actief(niveau);
        this.van = (GROOTTE - maat) / 2;
        for (Inhoud[] rij : inhoud) {
            java.util.Arrays.fill(rij, Inhoud.LEEG);
        }
        for (int[] c : schatten) {
            inhoud[c[0]][c[1]] = Inhoud.SCHAT;
        }
        if (goudenKaas != null) {
            inhoud[goudenKaas[0]][goudenKaas[1]] = Inhoud.GOUD;
        }
        for (int[] c : piep) {
            inhoud[c[0]][c[1]] = Inhoud.PIEP;
        }
    }

    private void leg(RandomSource rng, Inhoud wat, int aantal) {
        int gelegd = 0, pogingen = 0;
        while (gelegd < aantal && pogingen++ < 10000) {
            int x = van + rng.nextInt(maat), z = van + rng.nextInt(maat);
            if (inhoud[x][z] == Inhoud.LEEG) {
                inhoud[x][z] = wat;
                gelegd++;
            }
        }
    }

    // --- per level --------------------------------------------------------------------------------------------------
    /** How wide the played part of the field is: 7, 11, 15. */
    public static int actief(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 7;
            case MEDIUM -> 11;
            case LASTIG -> GROOTTE;
        };
    }

    public static int schatten(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 6;
            case MEDIUM -> 8;
            case LASTIG -> 10;
        };
    }

    public static int piepkazen(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 1;
            case MEDIUM -> 2;
            case LASTIG -> 3;
        };
    }

    /** How often Snuffie sniffs (ticks): fewer hints on the harder levels. */
    public static int hintTicks(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 20;
            case MEDIUM -> 40;
            case LASTIG -> 60;
        };
    }

    /** Within this distance (cells) Snuffie says "warm". */
    public static double warmStraal(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 3.5;
            case MEDIUM -> 2.9;
            case LASTIG -> 2.2;
        };
    }

    // --- playing ----------------------------------------------------------------------------------------------------
    public Niveau niveau() {
        return niveau;
    }

    /** Is this cell in the played part? */
    public boolean actief(int x, int z) {
        return x >= van && x < van + maat && z >= van && z < van + maat;
    }

    public boolean open(int x, int z) {
        return x >= 0 && z >= 0 && x < GROOTTE && z < GROOTTE && open[x][z];
    }

    public Inhoud inhoud(int x, int z) {
        return inhoud[x][z];
    }

    /** Digs a cell: what was under it, or null when it can't be dug (outside the played part, or already open). */
    @Nullable
    public Inhoud graaf(int x, int z) {
        if (x < 0 || z < 0 || x >= GROOTTE || z >= GROOTTE || !actief(x, z) || open[x][z]) {
            return null;
        }
        open[x][z] = true;
        Inhoud wat = inhoud[x][z];
        switch (wat) {
            case SCHAT -> gevonden++;
            case GOUD -> goud = true;
            case PIEP -> piepen++;
            default -> {
            }
        }
        return wat;
    }

    public int gevonden() {
        return gevonden;
    }

    public int piepen() {
        return piepen;
    }

    public boolean goud() {
        return goud;
    }

    /** Treasures still under the field. */
    public int over() {
        int n = 0;
        for (int x = 0; x < GROOTTE; x++) {
            for (int z = 0; z < GROOTTE; z++) {
                if (inhoud[x][z] == Inhoud.SCHAT && !open[x][z]) {
                    n++;
                }
            }
        }
        return n;
    }

    public int totaal() {
        return gevonden + over();
    }

    /** Every treasure found (the golden cheese doesn't have to be). */
    public boolean alles() {
        return over() == 0;
    }

    /**
     * Snuffie's nose at this spot of the field (cell coordinates, may be fractional): HEET right next to (or on) a treasure
     * or the golden cheese still in the ground, WARM within {@link #warmStraal}, KOUD further away or when nothing is left.
     */
    public Hint hint(double x, double z) {
        double best = Double.MAX_VALUE;
        int cx = (int) Math.floor(x), cz = (int) Math.floor(z);
        int cheb = Integer.MAX_VALUE;
        for (int i = 0; i < GROOTTE; i++) {
            for (int j = 0; j < GROOTTE; j++) {
                if (open[i][j] || (inhoud[i][j] != Inhoud.SCHAT && inhoud[i][j] != Inhoud.GOUD)) {
                    continue;
                }
                best = Math.min(best, Math.hypot(i + 0.5 - x, j + 0.5 - z));
                cheb = Math.min(cheb, Math.max(Math.abs(i - cx), Math.abs(j - cz)));
            }
        }
        if (cheb <= 1) {
            return Hint.HEET;
        }
        return best <= warmStraal(niveau) ? Hint.WARM : Hint.KOUD;
    }

    /** The points: 10 per treasure, 25 for the golden cheese, and with everything found 1 per 2 seconds left. */
    public int punten(int ticksOver) {
        int p = gevonden * SCHAT_PUNTEN + (goud ? GOUD_PUNTEN : 0);
        if (alles()) {
            p += Math.max(0, ticksOver) / 40;
        }
        return p;
    }

    /** Schatmuntjes for these points (a good game on medium ~30-40, on lastig ~50-60); +5 for a personal record. */
    public static int munten(Niveau n, int punten, boolean record) {
        int pct = switch (n) {
            case MAKKELIJK -> 25;
            case MEDIUM -> 38;
            case LASTIG -> 55;
        };
        return GrotSpel.munten(n, punten * pct / 100, record);
    }
}
