package nl.juiced.guhs.feature.vadskracht;

/**
 * Every tunable number of the vadskracht (VK per second), in ONE place: what a source gives, what a machine asks, how much a
 * Knabbelbatterij holds. Use these, no literals. (The Guhrad's per-variant table is data: data/guhs/vadskracht/guhrad.json,
 * see {@link GuhradKracht}; tools/features/vadskracht.py reads the two Guhrad numbers from this file.)
 */
public final class VadsGetallen {
    public static final int GUHRAD = 10, GUHRAD_BLIJ = 15, KNUFFEL_PER_GUH = 3, KNUFFEL_MAX_GUHS = 8, DISCO_PER_GUH = 5,
            DISCO_MAX_GUHS = 4, BLUBKACHELTJE = 6, GLOEISTERKERN = 200;
    /** What one Knabbelbatterij holds, in VK (= VK-seconds): about half an hour of one Guhrad. */
    public static final long BATTERIJ = 18_000;
    public static final int SENSOR = 1, HAPLUIKJE = 2, BUISFILTER = 2, OPZUIGER = 3, STEPSTATION = 4, GUH_OVEN = 5, MOLEN = 5,
            NEERZETTER = 5, POMP = 6, PLANTAGEBAK = 6, OOGSTER = 8, BROUWKETEL = 8, FRITUUR = 8, KNABBELAAR = 10, KNUTSELMACHINE = 10;

    /** The most blocks (Guhdraad + machine blocks) in one net; a bigger net stands still ({@link VadsNet.Status#TE_GROOT}). */
    public static final int MAX_NET = 8192;
    /** A net is evaluated once per this many ticks (and one tick after something changed). */
    public static final int TIK = 20;

    private VadsGetallen() {
    }
}
