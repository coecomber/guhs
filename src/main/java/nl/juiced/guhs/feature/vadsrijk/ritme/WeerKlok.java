package nl.juiced.guhs.feature.vadsrijk.ritme;

import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerKlok;

/**
 * U-LEVEN-RITME: "weer met charme", the fixed timetable per zone (deterministic from the day clock, so every client sees the same
 * shower at the same time without packets, and the server knows it for the newspaper's weather forecast). Zone 1 keeps the pink
 * shower + rainbow of fix-sfeer ({@link SfeerKlok#bui}); this adds the other zones and the morning mist everywhere.
 * <ul>
 *   <li><b>bui</b> (zone 2 snow shower, zone 3 blossom rain, zone 4 sweet rain shower, zone 5 a parade of cloud wisps, zone 6 warm sand
 *   swirls): on 2 of 3 days (zone 6: 3 of 4), once by day, ~70 s;</li>
 *   <li><b>regenboog</b> after the shower in zone 2, 3 and 4 (~70 s);</li>
 *   <li><b>gloed</b>: zone 4, every evening, the soft lampion-glow haze;</li>
 *   <li><b>mist</b>: morning mist around sunrise on every other day, all zones.</li>
 * </ul>
 * Time of day: 0 = sunrise, 6000 noon, 12000 sunset, 18000 midnight.
 */
public final class WeerKlok {
    public static final int BUI_DUUR = 1400, BOOG_DUUR = 1400, BUI_RAMP = 160;
    public static final int GLOED_START = 11200, GLOED_DUUR = 3800, GLOED_RAMP = 700;
    public static final int MIST_START = 22600, MIST_DUUR = 3400, MIST_RAMP = 700;

    private WeerKlok() {
    }

    private static long hash(long dag, int zone) {
        long h = (dag * 2654435761L) ^ ((long) (zone + 7) * 0x9E3779B97F4A7C15L) ^ 0x5DEECE66DL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return h & 0x7fffffffffffL;
    }

    /** When the shower of this zone starts today (time of day), or -1 on a dry day / a zone without showers. */
    public static long buiStart(long dag, int zone) {
        if (zone < 2 || zone > 6) {
            return -1;
        }
        long h = hash(dag, zone);
        if (zone == 6 ? h % 4 == 0 : h % 3 == 0) {
            return -1;
        }
        long van = zone == 6 ? 5000 : 1200, breed = zone == 6 ? 4500 : 7800;
        return van + (h / 4) % breed;
    }

    public static float bui(long dag, long tijd, int zone) {
        long s = buiStart(dag, zone);
        return s < 0 ? 0f : SfeerKlok.venster(tijd, s, BUI_DUUR, BUI_RAMP);
    }

    public static float bui(Level l, int zone) {
        return bui(Ritme.dag(l), Ritme.tijd(l), zone);
    }

    /** The rainbow after a shower (zone 2, 3, 4). */
    public static float regenboog(long dag, long tijd, int zone) {
        if (zone < 2 || zone > 4) {
            return 0f;
        }
        long s = buiStart(dag, zone);
        return s < 0 ? 0f : SfeerKlok.venster(tijd, s + BUI_DUUR - 200, BOOG_DUUR, 200);
    }

    public static float regenboog(Level l, int zone) {
        return regenboog(Ritme.dag(l), Ritme.tijd(l), zone);
    }

    /** Zone 4's evening lampion haze, every evening. */
    public static float gloed(long tijd, int zone) {
        return zone == 4 ? SfeerKlok.venster(tijd, GLOED_START, GLOED_DUUR, GLOED_RAMP) : 0f;
    }

    /** Is there morning mist around the sunrise that starts day {@code dag}? (every other day, hashed) */
    public static boolean mistDag(long dag) {
        return hash(dag, 99) % 2 == 0;
    }

    /** The morning mist, 0..1 (around sunrise; the window crosses the day boundary at time 0). */
    public static float mist(long dag, long tijd) {
        long t = tijd < 6000 ? tijd + 24000 : tijd;
        long zonsopgangDag = tijd < 6000 ? dag : dag + 1;
        return mistDag(zonsopgangDag) ? SfeerKlok.venster(t, MIST_START, MIST_DUUR, MIST_RAMP) : 0f;
    }

    public static float mist(Level l) {
        return mist(Ritme.dag(l), Ritme.tijd(l));
    }

    /** The newspaper's forecast: lang key of the kind of shower in this zone ("" = none in this zone). */
    public static String soort(int zone) {
        return switch (zone) {
            case 1 -> "roze";
            case 2 -> "sneeuw";
            case 3 -> "bloesem";
            case 4 -> "regen";
            case 5 -> "wolken";
            case 6 -> "zand";
            default -> "";
        };
    }

    /** Zone 1's shower start today (fix-sfeer's pink shower), for the newspaper. */
    public static long buiStartZone1(Level l) {
        return SfeerKlok.buiStart(l);
    }
}
