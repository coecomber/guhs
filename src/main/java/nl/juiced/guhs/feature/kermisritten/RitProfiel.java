package nl.juiced.guhs.feature.kermisritten;

/**
 * How a ride turns over time: speeds up over {@code aanloop} ticks, keeps its speed, slows down over {@code aanloop}
 * ticks, and ends exactly {@code omwentelingen} whole turns further, so the rest pose after a ride is the rest pose
 * before it. A pure function of the time: server and clients get the same angle without sending it.
 */
public record RitProfiel(int duur, int aanloop, int omwentelingen) {
    private double vaart() {
        return omwentelingen * Math.PI * 2 / (duur - aanloop);
    }

    /** The angle in radians after t ticks of riding. */
    public double hoek(double t) {
        double v = vaart();
        if (t <= 0) {
            return 0;
        }
        if (t >= duur) {
            return omwentelingen * Math.PI * 2;
        }
        if (t < aanloop) {
            return v * t * t / (2 * aanloop);
        }
        if (t < duur - aanloop) {
            return v * (t - aanloop / 2.0);
        }
        double rest = duur - t;
        return omwentelingen * Math.PI * 2 - v * rest * rest / (2 * aanloop);
    }

    /** How fast it goes now: 0 standing still, 1 at full speed. */
    public double vaartNu(double t) {
        if (t <= 0 || t >= duur) {
            return 0;
        }
        return Math.min(1, Math.min(t / aanloop, (duur - t) / aanloop));
    }
}
