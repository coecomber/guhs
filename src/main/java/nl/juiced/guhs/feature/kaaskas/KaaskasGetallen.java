package nl.juiced.guhs.feature.kaaskas;

/**
 * Every tunable number of the Kaaskas and the Guh-sorteerband, in ONE place (tools/features/kaaskas.py reads them for the
 * texts). A full kas (about 80 kaasknabbelplantjes) gives a few thousand knabbels an hour: matige schaal (ontwerp V1).
 */
public final class KaaskasGetallen {
    /** What the Kaaskas asks, in VK per second. */
    public static final int VRAAG = 12;
    /** Its kaassaus tank, in mB (four buckets). */
    public static final int TANK = 4000;
    /** One growth push (a bonemeal-sized step for one plant) every this many ticks. */
    public static final int DUW_TICKS = 25;
    /** What one push costs, in mB of kaassaus. */
    public static final int SAUS_PER_DUW = 5;
    /** The kas: this far around the kern (x and z), and this far below and above it. */
    public static final int STRAAL = 4, ONDER = 2, BOVEN = 2;
    /** How often (ticks) the kas looks again which plants stand in it. */
    public static final int SCAN_TICKS = 100;
    /** How many ripe plants a Kassier picks on one trip. */
    public static final int KASSIER_PER_KEER = 24;
    /** How fast an item rides the sorteerband (blocks per tick). */
    public static final double BAND_SNELHEID = 0.08;

    private KaaskasGetallen() {
    }
}
