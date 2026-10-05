package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * One game on a Guhkade cabinet: a small, exact simulation in steps of 1/30 second. It has no dice of its own: everything
 * follows from the seed and the input of every step, so
 * <ul>
 *   <li>the client plays it and sends the seed and its input, and the server plays that input again to know the real
 *   score ({@link #speelAf}); a client never reports a score;</li>
 *   <li>a guh's game is the same simulation steered by {@link #bot}: the server knows beforehand how long it lasts and how
 *   it ends ({@link #voorspel}), and every client draws the very same game on the cabinet's screen.</li>
 * </ul>
 * No Minecraft classes in this package: the games can be tried and tuned on their own.
 */
public abstract class Sim {
    /** The screen in game pixels. */
    public static final int B = 160, H = 120;
    /** Steps per second, and per game tick (20 a second). */
    public static final int HZ = 30;
    public static final float PER_TICK = HZ / 20f;
    /** No game lasts longer than this (ten minutes): then the cabinet says "TIJD". */
    public static final int MAX_STAPPEN = HZ * 60 * 10;

    public static final int GELUID_TIK = 1, GELUID_PUNT = 2, GELUID_AF = 4, GELUID_BONS = 8;

    protected final long seed;
    protected int stappen;
    protected int score;
    protected boolean af;
    /** What happened in the last step, for the beeps ({@link #GELUID_TIK}...). */
    protected int geluid;

    protected Sim(long seed) {
        this.seed = seed;
    }

    public final long seed() {
        return seed;
    }

    public final int stappen() {
        return stappen;
    }

    public final int score() {
        return score;
    }

    /** The game is over. */
    public final boolean af() {
        return af;
    }

    public final int geluid() {
        return geluid;
    }

    /** One step with this input (what an input means is the game's own business; 0 = nothing pressed). */
    public final void stap(int invoer) {
        if (af) {
            return;
        }
        geluid = 0;
        doeStap(invoer);
        stappen++;
        if (!af && stappen >= MAX_STAPPEN) {
            af = true;
            geluid |= GELUID_AF;
        }
    }

    protected abstract void doeStap(int invoer);

    /** The input a practised guh gives now: it plays well until its score is {@code doel}, and then it slips up. */
    public abstract int bot(int doel);

    /** Draws the game as it is now. */
    public abstract void teken(Doek d);

    /** Bits per step in a recording ({@link Opname}). */
    public abstract int bits();

    // --- seeded dice (the same everywhere) -----------------------------------------------------------------------------

    /** A number 0..1 that only depends on the seed and n. */
    protected final float kans(long n) {
        long x = seed * 0x9E3779B97F4A7C15L + n * 0xBF58476D1CE4E5B9L;
        x ^= x >>> 31;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 29;
        x *= 0xD6E8FEB86659FD93L;
        x ^= x >>> 32;
        return (x >>> 40) / (float) (1 << 24);
    }

    // --- whole games ---------------------------------------------------------------------------------------------------

    /** How a game ends: its score and how many steps it took. */
    public record Uitkomst(int score, int stappen) {
        /** The game ticks (20 a second) this game takes on a cabinet. */
        public int ticks() {
            return (int) Math.ceil(stappen / PER_TICK);
        }
    }

    /**
     * Plays a recorded game again (the server's check of what a client sent): the score it really gives. A recording that
     * stops before the game is over counts as far as it got (the player walked away).
     */
    public static Uitkomst speelAf(Spel spel, long seed, byte[] opname, int stappen) {
        Sim sim = spel.nieuw(seed);
        int n = Math.min(Math.max(0, stappen), Math.min(MAX_STAPPEN, Opname.stappen(opname, sim.bits())));
        for (int i = 0; i < n && !sim.af; i++) {
            sim.stap(Opname.lees(opname, sim.bits(), i));
        }
        return new Uitkomst(sim.score, sim.stappen);
    }

    /** A guh's whole game beforehand: played by {@link #bot} until it is over. */
    public static Uitkomst voorspel(Spel spel, long seed, int doel) {
        Sim sim = spel.nieuw(seed);
        while (!sim.af) {
            sim.stap(sim.bot(doel));
        }
        return new Uitkomst(sim.score, sim.stappen);
    }
}
