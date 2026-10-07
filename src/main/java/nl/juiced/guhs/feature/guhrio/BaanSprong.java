package nl.juiced.guhs.feature.guhrio;

/**
 * The jump of a Super Guhrio level as sums (no game classes: the player's own game uses this every tick, see
 * client.BaanBesturing, and so do the game tests and tools/features/guhrio_loop.py, which reads the numbers out of THIS
 * file and walks every level of the castle with them). Together with {@link GuhrioSpel#SPRONG_ERBIJ},
 * {@link GuhrioSpel#ZWAARTE_ERBIJ} and {@link GuhrioSpel#SNEL_ERBIJ} these numbers are the whole feel of a level:
 * <ul>
 *     <li>in the air you steer much better than normal ({@link #stuur});</li>
 *     <li>the jump of the old platform games ({@link Staat#val}): while you rise with space held you are lighter, letting go
 *     cuts what is left of the rise, and you fall heavier than you rose; on Guhshi, holding space at the top of a jump
 *     flutters.</li>
 * </ul>
 * Change a number here and run the generators: they refuse a level that can no longer be finished with the new jump.
 */
public final class BaanSprong {
    /** In the air: this much speed per tick towards where you push, up to these speeds (walking / running). */
    public static final double LUCHT_STUUR = 0.04, LUCHT_LOOP = 0.26, LUCHT_REN = 0.365;
    /** While you rise with space held: this much speed back per tick. Letting go: this part of the rise is left. */
    public static final double STIJG_LICHTER = 0.035, HOP_REST = 0.4;
    /** Falling: this much heavier per tick, down to this speed. */
    public static final double VAL_ERBIJ = 0.03, VAL_MAX = -1.3;
    /** On Guhshi: holding space at the top of a jump flutters this long, rising this fast. */
    public static final int FLADDER_TICKS = 24;
    public static final double FLADDER = 0.03;
    /** Leaving the ground faster than this with space held is "a jump of your own" (a held jump rises longer). */
    public static final double EIGEN_SPRONG = 0.3;

    private BaanSprong() {
    }

    /**
     * Your speed along the lane after a tick of steering in the air. {@code teken}: +1 further along the lane, -1 back
     * (0: you do not steer, nothing changes); {@code rent}: sprinting.
     */
    public static double stuur(double langs, int teken, boolean rent) {
        if (teken == 0) {
            return langs;
        }
        double top = rent ? LUCHT_REN : LUCHT_LOOP;
        if (langs * teken < top) {
            return teken > 0 ? Math.min(top, langs + LUCHT_STUUR) : Math.max(-top, langs - LUCHT_STUUR);
        }
        return langs;
    }

    /** What a jump remembers from tick to tick. */
    public static final class Staat {
        /** Rising from a jump of your own (so letting go of space may cut it). */
        public boolean sprong;
        /** Ticks of this jump's flutter that are used up. */
        public int fladder;
        /** (set by {@link #val}) this tick was a flutter tick. */
        public boolean fladderde;

        /**
         * Your speed up or down for this tick, before the game moves you. {@code opGrond}: you stand; {@code lucht}: you
         * are in the air (not standing, not in water); {@code spatie}: the jump key is held; {@code guhshi}: you ride him.
         */
        public double val(double vy, boolean opGrond, boolean lucht, boolean spatie, boolean guhshi) {
            fladderde = false;
            if (opGrond) {
                sprong = false;
                fladder = 0;
            } else if (lucht) {
                if (vy > 0) {
                    if (sprong && spatie) {
                        return vy + STIJG_LICHTER;                 // a held jump rises longer
                    }
                    if (sprong) {
                        sprong = false;
                        return vy * HOP_REST;                      // let go: a small hop
                    }
                } else {
                    sprong = false;
                    if (guhshi && spatie && fladder < FLADDER_TICKS) {
                        fladder++;                                 // Guhshi flutters: a little higher, a lot further
                        fladderde = true;
                        return FLADDER;
                    }
                    return Math.max(VAL_MAX, vy - VAL_ERBIJ);      // falling is heavier than rising
                }
            }
            return vy;
        }

        /**
         * After the game moved you: did you just leave the ground by a jump of your own (true: yes, and it is remembered)?
         * {@code valVoor}: your speed up or down before this tick, {@code vy}: now.
         */
        public boolean na(boolean opGrond, double valVoor, double vy, boolean spatie) {
            if (!opGrond && valVoor <= 0 && vy > EIGEN_SPRONG && spatie) {
                sprong = true;
                return true;
            }
            return false;
        }
    }
}
