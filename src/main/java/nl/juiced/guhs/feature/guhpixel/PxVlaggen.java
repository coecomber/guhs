package nl.juiced.guhs.feature.guhpixel;

/**
 * The KnusVlaggen bits (synced guh flags: GuhHooks.heeft / zet) reserved for the guhpixel slices. The Barbecuether
 * update keeps bits 8..11, 29 and 30.
 */
public final class PxVlaggen {
    /** Guhbioscoop: eats popcorn in its seat. */
    public static final int POPCORN = 1 << 24;
    /** Guhbioscoop: startled by the film. */
    public static final int SCHRIK = 1 << 25;
    /** Reisbureau: carries its little suitcase. */
    public static final int KOFFER = 1 << 26;
    /** Guh-parkour: running a route. */
    public static final int OP_ROUTE = 1 << 27;
    /** Guhkade: playing at a cabinet. */
    public static final int SPEELT_KAST = 1 << 28;

    private PxVlaggen() {
    }
}
