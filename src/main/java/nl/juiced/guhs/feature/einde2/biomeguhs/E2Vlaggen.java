package nl.juiced.guhs.feature.einde2.biomeguhs;

/**
 * einde2, slice biomeguhs: the one KnusVlaggen bit of einde2 (synced to the client, saved; set and read with
 * {@code nl.juiced.guhs.feature.knus.GuhHooks.zet/heeft}). Every other bit of that int is taken or kept for another update
 * (build contract 3.3), so there is exactly one, and what it means follows from the guh's variant.
 */
public final class E2Vlaggen {
    /**
     * "This guh is doing its trick now": a KORSTGUH is rolled up, a DUINGUH has dug in, a SCHIMMELGUH is a lamp, a KOORGUH
     * sings. (The Kruimelguh's floating uses the older VerhaalVlaggen.ZWEEFT; the Kristalguh needs no flag.)
     */
    public static final int TRUC = 1 << 31;

    private E2Vlaggen() {
    }
}
