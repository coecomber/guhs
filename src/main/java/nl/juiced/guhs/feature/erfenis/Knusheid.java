package nl.juiced.guhs.feature.erfenis;

/**
 * De Erfenis: Knusheid (cosiness). It counts towards Hoeve goals (Knusheid 250 / 1.000 / 2.000) and titles; no ranking.
 * Total = the buildings (2 per level, the Hoeve 10 per level; deco.md) + the paths (1 per 10 tiles) + the deco value, which
 * the deco catalogue (phase 2) keeps up to date with {@link #zet}. Base keeps the placed deco ({@link Base.Deco}), the
 * decoschuurtje storage and the path layer already, so phase 2 only adds the shop and the tools.
 */
public final class Knusheid {
    private Knusheid() {
    }

    /** Sets the deco part of a base's Knusheid (the deco system calls this after every change). */
    public static void zet(Base base, int decoWaarde) {
        base.knusDeco = Math.max(0, decoWaarde);
    }

    /** The whole Knusheid of a base. */
    public static int totaal(Base base) {
        return base.knusheid();
    }
}
