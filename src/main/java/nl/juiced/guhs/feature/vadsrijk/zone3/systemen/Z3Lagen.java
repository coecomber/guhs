package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied;

/**
 * z3-systemen: the LAYERED area plan of zone 3 "De Schemerwortel" (ZONE3_PLAN §2, translated east: trunk T = (+2300, 0), box
 * x +1600..+3000, z -700..700; heights from the renders: crown ~y 210-300). Crown, branches, trunk, ground and roots overlap in
 * x/z, so every area has a y-range and most are a cylinder/ring around the trunk ({@link Gebieden.Laag}).
 * <p>
 * First match wins (order = this list). These are the systemen defaults until z3-wereld publishes its own list
 * ({@code Z3WereldPlekken.GEBIEDEN}): the integrator points {@link #GEBIEDEN} at that list (same ids) and nothing else changes.
 * The biome source ({@code VadsrijkBiomen}) reads this list too (pure: it never reads the runtime Gebieden list).
 */
public final class Z3Lagen {
    public static final int STAM_X = 2300, STAM_Z = 0;
    public static final int ZONE_MIN_X = 1600, ZONE_MAX_X = 3000, ZONE_MIN_Z = -700, ZONE_MAX_Z = 700;
    /**
     * Copied from z3-wereld's POI_Z3.md v0 ({@code Z3WereldPlekken.GEBIEDEN}, same ids, same order) plus a catch-all
     * {@code z3_ring}. The integrator may replace this with a conversion of that list ({@link #kern}).
     */
    public static final List<Gebied> GEBIEDEN = List.of(
            kern("z3_honingtak", false, 1634, -100, 2014, 110, 150, 290, 30, 50, true),
            kern("z3_zeppelinwerf", false, 2819, -660, 2979, -480, 95, 185, 38, 40, false),
            kern("z3_hart", true, 2300, 0, 0, 120, 40, 74, 49, 50, false),
            kern("z3_schorsstad", true, 2300, 0, 0, 76, 75, 199, 40, 43, false),
            kern("z3_stamspiraal", true, 2300, 0, 76, 132, 66, 205, 40, 43, false),
            kern("z3_kruinzee", true, 2300, 0, 0, 400, 200, 320, 30, 34, false),
            kern("z3_takkenbanen", true, 2300, 0, 0, 820, 120, 320, 34, 38, false),
            kern("z3_wortelduister", true, 2300, 0, 0, 470, 0, 61, 46, 49, false),
            kern("z3_wortelvelden", true, 2300, 0, 0, 900, 0, 320, 43, 46, false),
            // the rest of the zone box (every height): flat 2D lookups in zone 3 land here too
            new Gebied("z3_ring", ZONE_MIN_X, ZONE_MIN_Z, ZONE_MAX_X, ZONE_MAX_Z, 30, 50, false));

    /**
     * A kern area from z3-wereld's {@code Gebied3} fields: a box (a,b)..(c,d) or a cylinder/ring around (a, b) with radius c..d,
     * between minY and maxY (inclusive). Cylinders are clipped to the zone box.
     */
    public static Gebied kern(String id, boolean cilinder, int a, int b, int c, int d, int minY, int maxY, int lo, int hi, boolean stad) {
        if (!cilinder) {
            return new Gebied(id, a, b, c, d, lo, hi, stad).metY(minY, maxY);
        }
        return new Gebied(id, Math.max(ZONE_MIN_X, a - d), Math.max(ZONE_MIN_Z, b - d), Math.min(ZONE_MAX_X, a + d), Math.min(ZONE_MAX_Z, b + d),
                lo, hi, stad).metY(minY, maxY).metRing(a, b, c, d);
    }

    private Z3Lagen() {
    }

    public static boolean inZone3(double x, double z) {
        return x >= ZONE_MIN_X && x <= ZONE_MAX_X + 0.999 && z >= ZONE_MIN_Z && z <= ZONE_MAX_Z + 0.999;
    }

    /** The zone-3 area at x/y/z from this fixed plan (null outside the zone box). */
    @Nullable
    public static Gebied op(double x, double y, double z) {
        if (!inZone3(x, z)) {
            return null;
        }
        for (Gebied g : GEBIEDEN) {
            if (g.bevat(x, y, z)) {
                return g;
            }
        }
        return null;
    }

    /** Push the plan into kern Gebieden, in front of zone 1/2 (voegVooraan puts each at index 0, so add them reversed). */
    public static void registreer() {
        for (int i = GEBIEDEN.size() - 1; i >= 0; i--) {
            Gebieden.voegVooraan(GEBIEDEN.get(i));
        }
    }
}
