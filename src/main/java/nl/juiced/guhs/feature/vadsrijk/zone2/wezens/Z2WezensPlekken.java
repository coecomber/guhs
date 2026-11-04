package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;

/**
 * Every coordinate of the zone-2 wezens slice (Z2_BRIEF: one class). Zone 2 "De Splinterpieken" = x -600..600, z -3000..-1800.
 * <p><b>Status:</b> z2-wereld had not published POI_Z2.md yet when this was written, so the areas follow the fixed brief
 * (Galmhellingen east, Bevroren Knuffelmeer west, Splinterkloof north, Spiegelburcht on a peak around (0, high, -2900),
 * Kandijkom (-150,-2550)..(150,-2250)) and the campfires/arenas/interior heights below are GUESSES. Integration aligns
 * {@link #VEILIG}, {@link #BURCHT} and the boss/arena spots with POI_Z2.md / Z2WereldPlekken (see NOTITIES_Z2_WEZENS.md).
 * <p>Spawners are laid out on a jittered grid inside each area box ({@link Z2WezensSpawners}); a spawner never comes within
 * its radius + {@link #VEILIG} radius of a safe spot, and a creature that still ends up in one walks back (backstop in Z2Wezen).
 */
public final class Z2WezensPlekken {
    private Z2WezensPlekken() {
    }

    /** An area box (inclusive) with its levels. */
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLevel, int maxLevel) {
        public boolean bevat(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    /** A spot where no creature may come (campfire, NPC, town, boss arena) and its radius. */
    public record Veilig(BlockPos pos, int straal) {
        public boolean bevat(double x, double z) {
            double dx = x - pos.getX(), dz = z - pos.getZ();
            return dx * dx + dz * dz <= (double) straal * straal;
        }
    }

    /** (fix round 2) The compact zone: the boxes follow z2-wereld's generated areas (Z2WereldPlekken.GEBIEDEN). */
    public static final Gebied ZONE = new Gebied("zone2", Z2WereldPlekken.ZONE_MIN_X - 40, Z2WereldPlekken.ZONE_MIN_Z - 40, Z2WereldPlekken.ZONE_MAX_X + 40, Z2WereldPlekken.ZONE_MAX_Z + 40, 20, 30);
    public static final Gebied GALMHELLINGEN = uit("galmhellingen", "z2_galmhellingen", 20, 23);
    public static final Gebied KNUFFELMEER = uit("knuffelmeer", "z2_knuffelmeer", 23, 26);
    public static final Gebied SPLINTERKLOOF = uit("splinterkloof", "z2_splinterkloof", 26, 29);
    public static final List<Gebied> GEBIEDEN = List.of(GALMHELLINGEN, KNUFFELMEER, SPLINTERKLOOF);

    /** Surface spawners search the floor from this height down (the first floor from above = the surface, bridges included). */
    public static final int BOVEN_Y = 240, ONDER_Y = 40;

    /** (integration: aligned to Z2WereldPlekken / POI_Z2) The Spiegelburcht rooms the adds stand in (floor search 24 down from
     * y+1): voorhal, lichtkamer, spiegelgang, prismazaal + the two side positions of the lichtkamer. The boss hall itself
     * (arena_spiegelzucht) is the boss's. */
    public static final BlockPos BURCHT_MIDDEN = w("burcht_spiegelgang", 0, -2909, 176);
    public static final BlockPos[] BURCHT = {w("burcht_voorhal", 0, -2868, 176), w("puzzel_lichtkamer_1", -7, -2894, 176),
            w("puzzel_lichtkamer_4", 7, -2885, 176), w("burcht_spiegelgang", 0, -2909, 176), w("puzzel_prismazaal_2", -8, -2923, 176),
            w("burcht_prismazaal", 0, -2928, 176)};

    /** The Mika fort the Mika's flee to in their smoke puff (POI mika_fort, on the north plateau of the Splinterkloof). */
    public static final BlockPos MIKA_FORT = w("mika_fort", 300, -2882, 153);

    /** No creatures here: Kandijkom + the dock, every zone-2 campfire, the boss arenas (all from Z2WereldPlekken). */
    public static final List<Veilig> VEILIG = new ArrayList<>(List.of(
            new Veilig(new BlockPos(0, 0, -2400), 92),           // Kandijkom (geode r ~80) + a margin
            new Veilig(new BlockPos(0, 0, -2262), 48),           // the dock road, the stable and the airship dock
            new Veilig(w("arena_galmwolf", 430, -2280, 121), 55),
            new Veilig(w("arena_spiegelzucht", 0, -2963, 176), 40),
            new Veilig(w("arena_boormeester", -500, -2836, 66), 40)));
    static {
        for (BlockPos k : nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.KAMPVUREN) {
            VEILIG.add(new Veilig(k, 26));
        }
    }

    private static Gebied uit(String id, String wereldId, int minLv, int maxLv) {
        for (nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied g : Z2WereldPlekken.GEBIEDEN) {
            if (g.id().equals(wereldId)) {
                return new Gebied(id, g.minX() + 4, g.minZ() + 4, g.maxX() - 4, g.maxZ() - 4, minLv, maxLv);
            }
        }
        throw new IllegalStateException("no zone-2 area " + wereldId);
    }

    /** Distance (flat) from (x, z) to the nearest zone-2 path (z2-wereld's PADEN). */
    public static double padAfstand(double x, double z) {
        double best = 1e9;
        for (int[][] pad : Z2WereldPlekken.PADEN) {
            for (int i = 0; i + 1 < pad.length; i++) {
                double ax = pad[i][0], az = pad[i][1], bx = pad[i + 1][0], bz = pad[i + 1][1];
                double dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
                double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / l2));
                best = Math.min(best, Math.hypot(x - (ax + t * dx), z - (az + t * dz)));
            }
        }
        return best;
    }

    private static BlockPos w(String id, int x, int z, int y) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    /** Is (x, z) inside a safe spot (extra = an added margin)? */
    public static boolean veilig(double x, double z, int extra) {
        for (Veilig v : VEILIG) {
            double dx = x - v.pos().getX(), dz = z - v.pos().getZ();
            double r = v.straal() + extra;
            if (dx * dx + dz * dz <= r * r) {
                return true;
            }
        }
        return false;
    }

    /**
     * (balans 2026-11) The zone-2 paths, registered in kern (Spawners.bijPad: no creature spawns within 5 of a path edge).
     * g-int: taken from the generated {@link Z2WereldPlekken#PADEN} (g-z2's new 500x575 layout) instead of a copy of the old plan.
     */
    public static final java.util.Map<String, int[][]> PADEN;

    static {
        java.util.Map<String, int[][]> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < Z2WereldPlekken.PADEN.size(); i++) {
            m.put("pad_" + i, Z2WereldPlekken.PADEN.get(i));
        }
        PADEN = java.util.Collections.unmodifiableMap(m);
    }

    public static final double PAD_BREEDTE = 2.8;

    /** The area of (x, z), or null. */
    public static Gebied gebied(double x, double z) {
        for (Gebied g : GEBIEDEN) {
            if (g.bevat(x, z)) {
                return g;
            }
        }
        return null;
    }
}
