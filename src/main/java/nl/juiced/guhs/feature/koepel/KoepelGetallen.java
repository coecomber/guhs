package nl.juiced.guhs.feature.koepel;

/**
 * 1.8.0 (Dromen en vadskracht, de Koepel): every tunable number of the five endgame machines on the roof of the Guhtechniek-beurs,
 * in ONE place (tools/features/koepel.py reads none of them; the texts say them with %s).
 */
public final class KoepelGetallen {
    /** What the machines ask, in VK per second (while they work). */
    public static final int MEGA_OVEN = 24, WEERSTATION = 16, STERRENKOEPEL = 6, DRAAIMOLEN = 10, WOLKENVANGER = 12;

    /** Mega-oven: one batch takes this many ticks. */
    public static final int BAK_TICKS = 200;

    /** Knabbel-weerstation: the radius it gives Weer vadsers near it, how near they must be, one knabbel per this many ticks. */
    public static final int WEERSTATION_STRAAL = 150, WEERSTATION_BEREIK = 16, WEERSTATION_KNABBEL_TICKS = 600;

    /** Sterrenkoepel: how many sterrenbeelden, and what level n costs (knabbels; a knabbelblok counts 9). */
    public static final int STERRENBEELDEN = 12, KNABBELS_PER_NIVEAU = 576;
    /** The kind bonus when a new sterrenbeeld lights up: everybody online gets "uitgerust" this many ticks. */
    public static final int STERRENKOEPEL_BONUS_TICKS = 12_000;

    /** Draaimolen: a ride (one knabbel = one ticket), how far tame guhs ride along, how long they stay happy. */
    public static final int RIT_TICKS = 600, MOLEN_BEREIK = 8, MOLEN_BLIJ_TICKS = 12_000;

    /** Wolkenvanger: one pluis per this many ticks at sea level, and at its best (y 192 and higher). */
    public static final int VANGER_TRAAG = 600, VANGER_SNEL = 120, VANGER_LAAG_Y = 64, VANGER_HOOG_Y = 192;

    /** Sterrenkoepel: knabbels for level n (1-based). */
    public static long kostVoor(int niveau) {
        return (long) KNABBELS_PER_NIVEAU * niveau;
    }

    /** Wolkenvanger: ticks per pluis at this height. */
    public static int vangTicks(int y) {
        if (y <= VANGER_LAAG_Y) {
            return VANGER_TRAAG;
        }
        if (y >= VANGER_HOOG_Y) {
            return VANGER_SNEL;
        }
        double f = (y - VANGER_LAAG_Y) / (double) (VANGER_HOOG_Y - VANGER_LAAG_Y);
        return (int) Math.round(VANGER_TRAAG + (VANGER_SNEL - VANGER_TRAAG) * f);
    }

    private KoepelGetallen() {
    }
}
