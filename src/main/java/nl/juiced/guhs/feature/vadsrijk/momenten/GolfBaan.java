package nl.juiced.guhs.feature.vadsrijk.momenten;

/**
 * The timing of the pink recolour wave (shared by server and client, pure math): the front is a circle around the Zwamtempel
 * whose radius grows in three phases so the part near the player is on screen long enough.
 * <ul>
 *   <li>0 .. {@link #A}: fast from the temple to 120 blocks before the player (far away, in the fog);</li>
 *   <li>{@link #A} .. {@link #B}: slow, 240 blocks in 7 s: it rolls towards the camera, over the player and past it;</li>
 *   <li>{@link #B} .. {@link #DUUR}: on to the edge of the zone ({@code rMax}), the camera follows it.</li>
 * </ul>
 * {@code afstand} = the player's flat distance to the temple when the wave starts.
 */
public record GolfBaan(double tx, double tz, double afstand, double rMax) {
    public static final int A = 60, B = 200, DUUR = 400;
    /** Scene length: the wave + the title card. */
    public static final int SCENE = 470;
    public static final double VOOR = 120;

    public double straal(double t) {
        double dichtbij = Math.max(0, afstand - VOOR), voorbij = Math.max(dichtbij + 1, afstand + VOOR);
        double eind = Math.max(rMax, voorbij + 200);
        if (t <= 0) {
            return 0;
        }
        if (t < A) {
            return dichtbij * glad(t / A);
        }
        if (t < B) {
            return dichtbij + (voorbij - dichtbij) * (t - A) / (B - A);
        }
        if (t < DUUR) {
            // keeps the speed of the slow phase at first, then speeds up to reach the edge at DUUR
            double v0 = (voorbij - dichtbij) / (B - A), dt = t - B, n = DUUR - B;
            double a = (eind - voorbij - v0 * n) / (n * n);
            return voorbij + v0 * dt + a * dt * dt;
        }
        return eind;
    }

    /** Is a block at (x, z) already healed at time t? */
    public boolean heel(double x, double z, double t) {
        double r = straal(t), dx = x - tx, dz = z - tz;
        return dx * dx + dz * dz < r * r;
    }

    private static double glad(double f) {
        return f * f * (3 - 2 * f);
    }
}
