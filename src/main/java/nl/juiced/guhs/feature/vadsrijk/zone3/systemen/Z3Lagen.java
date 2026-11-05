package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied;

/**
 * z3-systemen: the LAYERED area plan of zone 3 "De Schemerwortel" (ZONE3_PLAN §2, translated east: trunk T = (+2300, 0), box
 * x +1600..+3000, z -700..700; heights from the renders: crown ~y 210-300). Crown, branches, trunk, ground and roots overlap in
 * x/z, so every area has a y-range and most are a cylinder/ring around the trunk ({@link Gebieden.Laag}). (Revamp, ronde 20:
 * box x 1900..2700, z -400..400, the 4 layers roots 4-78 / trunk 80-160 / branches 160-216 / crown 216-312, POI v2.)
 * <p>
 * First match wins (order = this list). These are the systemen defaults until z3-wereld publishes its own list
 * ({@code Z3WereldPlekken.GEBIEDEN}): the integrator points {@link #GEBIEDEN} at that list (same ids) and nothing else changes.
 * The biome source ({@code VadsrijkBiomen}) reads this list too (pure: it never reads the runtime Gebieden list).
 */
public final class Z3Lagen {
    public static final int STAM_X = 2300, STAM_Z = 0;
    /** (P-VERHUIS) the zone box follows the map (revamp: x 1900..2700, z -400..400). */
    public static final int ZONE_MIN_X = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MIN_X,
            ZONE_MAX_X = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MAX_X,
            ZONE_MIN_Z = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MIN_Z,
            ZONE_MAX_Z = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MAX_Z;
    /**
     * K-INT: built from z3-wereld's generated {@code Z3WereldPlekken.GEBIEDEN} (same ids, same order, follows every map regen)
     * plus a catch-all {@code z3_ring} (the rest of the zone box, every height: flat 2D lookups in zone 3 land here too).
     */
    public static final List<Gebied> GEBIEDEN = bouw();

    private static List<Gebied> bouw() {
        java.util.ArrayList<Gebied> l = new java.util.ArrayList<>();
        for (var g : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.GEBIEDEN) {
            l.add(kern(g.id(), g.cilinder(), g.a(), g.b(), g.c(), g.d(), g.minY(), g.maxY(), g.minLevel(), g.maxLevel(), g.stad()));
        }
        l.add(new Gebied("z3_ring", ZONE_MIN_X, ZONE_MIN_Z, ZONE_MAX_X, ZONE_MAX_Z, 30, 50, false));
        return List.copyOf(l);
    }

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
