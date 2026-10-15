package nl.juiced.guhs.feature.bio.wereld;

/**
 * biomes3 wereld, the Klaterdal: the rhythm of its weather, as plain functions of the world's clock (the client draws it,
 * {@code client/DalSfeer}; nothing here costs the server anything). A gust of wind passes every {@link #VLAAG_OM} ticks and
 * lasts {@link #VLAAG_DUUR}: it swells and dies away, and while it blows many more petals come down. Rain adds a steady
 * few on top.
 */
public final class DalWeer {
    /** A gust every this many ticks, lasting this long. */
    public static final int VLAAG_OM = 2400, VLAAG_DUUR = 360;
    /** Extra petals per tick at the height of a gust, and while it rains. */
    public static final int VLAAG_BLAADJES = 5, REGEN_BLAADJES = 2;

    /** How strong the wind blows at a moment of the world's clock: 0 (calm) to 1 (the middle of a gust). */
    public static double vlaag(long tijd) {
        long t = Math.floorMod(tijd, (long) VLAAG_OM);
        return t >= VLAAG_DUUR ? 0 : Math.sin(Math.PI * t / VLAAG_DUUR);
    }

    /** Extra petals this tick around a player in the valley. */
    public static int blaadjes(long tijd, boolean regen) {
        return (int) Math.round(VLAAG_BLAADJES * vlaag(tijd)) + (regen ? REGEN_BLAADJES : 0);
    }

    private DalWeer() {
    }
}
