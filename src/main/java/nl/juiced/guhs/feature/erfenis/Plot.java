package nl.juiced.guhs.feature.erfenis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;

/**
 * De Erfenis (POC): the geometry of the plots in the dimension {@code guhs:erfenis}.
 * <ul>
 *   <li>Every base (a player's or an NPC village's) has its own <b>cell</b> (cx, cz) on a grid of {@value #CEL}x{@value #CEL}
 *   blocks. Cells are handed out in a square spiral around (0, 0) ({@link #spiraal}), so the world map (phase 2) grows
 *   outwards from the middle and players and NPC villages mix.</li>
 *   <li>In the middle of a cell lies the <b>plot</b>: a build area of {@value #MAAT}x{@value #MAAT} tiles (one tile = one block),
 *   the grass top at y = {@value #Y}; buildings stand from y = {@value #Y} + 1. Around it a pink path and a grass island
 *   ({@value #EILAND} wide) so the base doesn't float in nothing.</li>
 *   <li>Plot coordinates: tile (x, z) with 0 &lt;= x, z &lt; {@value #MAAT} is block {@code oorsprong(cx, cz).offset(x, 0, z)}.</li>
 * </ul>
 */
public final class Plot {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("erfenis"));
    /** Cell size, plot size (tiles), the height of the grass top, the width of the grass island. */
    public static final int CEL = 256, MAAT = 64, Y = 64, EILAND = 96;
    /** The highest a building may reach above the grass (stamping clears this much). */
    public static final int MAX_HOOGTE = 16;

    private Plot() {
    }

    /** The min corner (tile 0,0 at the grass top) of the plot in cell (cx, cz). */
    public static BlockPos oorsprong(int cx, int cz) {
        int rand = (CEL - MAAT) / 2;
        return new BlockPos(cx * CEL + rand, Y, cz * CEL + rand);
    }

    /** The cell that holds this x or z block coordinate. */
    public static int cel(int blok) {
        return Math.floorDiv(blok, CEL);
    }

    /** One long for a cell (map keys). */
    public static long sleutel(int cx, int cz) {
        return (long) cx << 32 | (cz & 0xFFFFFFFFL);
    }

    /** Where a visitor arrives: on the path at the south side of the plot, in the middle (feet), looking north. */
    public static BlockPos aankomst(int cx, int cz) {
        return oorsprong(cx, cz).offset(MAAT / 2, 1, MAAT + 1);
    }

    /**
     * The n-th cell of a square spiral around (0, 0): 0 = (0,0), 1 = (1,0), 2 = (1,1), 3 = (0,1), 4 = (-1,1), 5 = (-1,0), ...
     */
    public static int[] spiraal(int n) {
        // (walks the spiral: 1 east, 1 south, 2 west, 2 north, 3 east, ...; cheap enough for the few thousand bases a world gets)
        int x = 0, z = 0, stap = 1, i = 0;
        int[][] richting = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        int r = 0;
        while (i < n) {
            for (int herhaal = 0; herhaal < 2 && i < n; herhaal++) {
                for (int s = 0; s < stap && i < n; s++) {
                    x += richting[r][0];
                    z += richting[r][1];
                    i++;
                }
                r = (r + 1) % 4;
            }
            stap++;
        }
        return new int[] {x, z};
    }
}
