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

    /** The Spiegelburcht on its peak (guess): the rooms the adds stand in (floor search 24 down from y+1). */
    public static final BlockPos BURCHT_MIDDEN = new BlockPos(0, 180, -2900);
    public static final BlockPos[] BURCHT = {new BlockPos(0, 181, -2860), new BlockPos(-22, 181, -2885), new BlockPos(22, 181, -2885),
            new BlockPos(-18, 189, -2915), new BlockPos(18, 189, -2915), new BlockPos(0, 189, -2930)};

    /** The Mika fort the Mika's flee to in their smoke puff (direction of the puff trail; guess: the drilling rigs, north-west). */
    public static final BlockPos MIKA_FORT = new BlockPos(-420, 120, -2900);

    /** No creatures here: Kandijkom + the dock, the campfires (guess, 3 per area), the boss arenas (guess). */
    public static final List<Veilig> VEILIG = new ArrayList<>(List.of(
            new Veilig(new BlockPos(0, 0, -2400), 200),          // Kandijkom (box 300x300) + a margin
            new Veilig(new BlockPos(0, 0, -2230), 60),           // the airship dock on the south side
            new Veilig(new BlockPos(260, 0, -2060), 26), new Veilig(new BlockPos(430, 0, -2330), 26), new Veilig(new BlockPos(330, 0, -2600), 26),
            new Veilig(new BlockPos(-260, 0, -2060), 26), new Veilig(new BlockPos(-430, 0, -2330), 26), new Veilig(new BlockPos(-330, 0, -2600), 26),
            new Veilig(new BlockPos(-300, 0, -2800), 26), new Veilig(new BlockPos(300, 0, -2800), 26), new Veilig(new BlockPos(0, 0, -2740), 26),
            new Veilig(new BlockPos(470, 0, -2150), 55),         // Galmwolf arena (world boss, guess)
            new Veilig(new BlockPos(0, 0, -2900), 95),           // the Spiegelburcht itself (its adds are the burcht spawners)
            new Veilig(new BlockPos(-440, 0, -2930), 40)));      // Mika-boormeester (secret, guess)

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
