package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.world.GuhTime;

/**
 * The fixed timetable of the atmosphere moments (ronde 14, "Sfeer"): everything follows the day clock, so every client sees the
 * same moment at the same time without any packets, and the server knows it too (the slimes hopping in the pink shower).
 * <ul>
 *   <li><b>lichtgolf</b>: the lights of Zwamzoet switch on in a wave from {@link #GOLF_START} (sunset) and go out at dawn;</li>
 *   <li><b>sporenstorm</b>: every day at dusk, ~3 min, in the Sporenwoud;</li>
 *   <li><b>roze bui</b>: on 2 of 3 days, once during the day, 1 min of pink rain on the Klodderweide, then a rainbow for 1 min;</li>
 *   <li><b>noorderlicht</b>: every night above the Zwamtempel.</li>
 * </ul>
 * Time of day: 0 = sunrise, 6000 noon, 12000 sunset, 18000 midnight.
 */
public final class SfeerKlok {
    /** The wave starts at sunset and takes {@link #GOLF_DUUR} ticks; the lights go out between DAGERAAD and DAGERAAD + 400. */
    public static final int GOLF_START = 12500, GOLF_DUUR = 420, DAGERAAD = 23200;
    public static final int STORM_START = 11800, STORM_DUUR = 3600, STORM_RAMP = 300;
    public static final int BUI_DUUR = 1200, BOOG_DUUR = 1400;

    private SfeerKlok() {
    }

    /** Ticks since the lights wave started tonight (0..), or -1 when the lights are off (day). */
    public static long golfFase(Level l) {
        long t = GuhTime.timeOfDay(l);
        return t >= GOLF_START && t < DAGERAAD + 400 ? t - GOLF_START : -1;
    }

    /** 1 while the lights are on, fading to 0 over 400 ticks from DAGERAAD. */
    public static float dageraadFactor(Level l) {
        long t = GuhTime.timeOfDay(l);
        if (t < DAGERAAD) {
            return 1f;
        }
        return Mth.clamp((DAGERAAD + 400 - t) / 400f, 0f, 1f);
    }

    /** The spore storm, 0..1 (soft ramps). */
    public static float storm(Level l) {
        long t = GuhTime.timeOfDay(l);
        return venster(t, STORM_START, STORM_DUUR, STORM_RAMP);
    }

    /** When today's pink shower starts (time of day), or -1 on a dry day. */
    public static long buiStart(Level l) {
        long dag = GuhTime.day(l);
        long h = (dag * 2654435761L) ^ (dag << 13) ^ 0x5DEECE66DL;
        h = (h ^ (h >>> 17)) & 0x7fffffffL;
        if (h % 3 == 0) {
            return -1;
        }
        return 1500 + (h / 3) % 6000;
    }

    /** The pink rain, 0..1. */
    public static float bui(Level l) {
        long s = buiStart(l);
        return s < 0 ? 0f : venster(GuhTime.timeOfDay(l), s, BUI_DUUR, 120);
    }

    /** The rainbow after the shower, 0..1. */
    public static float regenboog(Level l) {
        long s = buiStart(l);
        return s < 0 ? 0f : venster(GuhTime.timeOfDay(l), s + BUI_DUUR - 200, BOOG_DUUR, 200);
    }

    /** Night, 0..1 (the aurora). */
    public static float nacht(Level l) {
        long t = GuhTime.timeOfDay(l);
        return venster(t, 12800, 10400, 900);
    }

    /** 0..1 inside [start, start + duur) with linear ramps of {@code ramp} ticks at both ends. */
    public static float venster(long t, long start, long duur, long ramp) {
        long in = t - start;
        if (in < 0 || in >= duur) {
            return 0f;
        }
        float a = Math.min(1f, in / (float) ramp), b = Math.min(1f, (duur - in) / (float) ramp);
        return Math.min(a, b);
    }
}
