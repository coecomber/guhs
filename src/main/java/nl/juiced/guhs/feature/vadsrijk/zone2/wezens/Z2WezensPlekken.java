package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

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

    public static final Gebied ZONE = new Gebied("zone2", -600, -3000, 600, -1800, 20, 35);
    public static final Gebied GALMHELLINGEN = new Gebied("galmhellingen", 160, -2700, 590, -1830, 20, 25);
    public static final Gebied KNUFFELMEER = new Gebied("knuffelmeer", -590, -2700, -160, -1830, 25, 30);
    public static final Gebied SPLINTERKLOOF = new Gebied("splinterkloof", -570, -2990, 570, -2715, 30, 34);
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
            new Veilig(new BlockPos(0, 0, -2400), 200),          // Kandijkom (box 300x300) + a margin
            new Veilig(new BlockPos(0, 0, -2230), 60),           // the airship dock on the south side
            new Veilig(w("arena_galmwolf", 430, -2280, 121), 55),
            new Veilig(w("arena_spiegelzucht", 0, -2963, 176), 40),
            new Veilig(w("arena_boormeester", -500, -2836, 66), 40)));
    static {
        for (BlockPos k : nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.KAMPVUREN) {
            VEILIG.add(new Veilig(k, 26));
        }
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
     * (balans 2026-11) The zone-2 paths (tools/vadsrijk/zone2/plan.py PADEN, world x/z; road half width 2.6 in kaart2.weg):
     * no creature spawns within 5 of them (kern Spawners.bijPad) and the spawners stand beside them.
     */
    public static final java.util.Map<String, int[][]> PADEN = java.util.Map.ofEntries(
            java.util.Map.entry("oost", new int[][] {{118, -2400}, {180, -2380}, {232, -2330}, {330, -2290}, {398, -2282}}),
            java.util.Map.entry("oost_zuid", new int[][] {{232, -2330}, {300, -2200}, {330, -2120}, {400, -1990}}),
            java.util.Map.entry("oost_noord", new int[][] {{232, -2330}, {250, -2450}, {250, -2560}, {306, -2618}}),
            java.util.Map.entry("oost_noord2", new int[][] {{330, -2290}, {480, -2440}, {520, -2560}}),
            java.util.Map.entry("west", new int[][] {{-118, -2400}, {-170, -2390}, {-220, -2300}, {-200, -2120}, {-260, -2060}}),
            java.util.Map.entry("west_noord", new int[][] {{-170, -2390}, {-250, -2440}, {-330, -2470}, {-470, -2500}, {-540, -2380}, {-560, -2240},
                    {-540, -2080}}),
            java.util.Map.entry("zadel", new int[][] {{-250, -2440}, {-290, -2540}, {-330, -2610}}),
            java.util.Map.entry("noord", new int[][] {{0, -2522}, {10, -2600}, {30, -2680}, {24, -2730}, {0, -2752}}),
            java.util.Map.entry("kloof_west", new int[][] {{24, -2730}, {-120, -2715}, {-260, -2700}, {-430, -2722}}),
            java.util.Map.entry("kloof_oost", new int[][] {{24, -2730}, {160, -2716}, {300, -2702}, {460, -2726}}),
            java.util.Map.entry("zadel_kloof", new int[][] {{-330, -2610}, {-300, -2660}, {-260, -2700}}));
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
