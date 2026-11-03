package nl.juiced.guhs.feature.erfenis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;

/**
 * De Erfenis: the geometry of the plots in the dimension {@code guhs:erfenis}.
 * <ul>
 *   <li>Every base (a player's or an NPC village's) has its own <b>cell</b> (cx, cz) on a grid of {@value #CEL}x{@value #CEL}
 *   blocks, handed out in a square spiral around (0, 0) ({@link #spiraal}).</li>
 *   <li>In the middle of a cell lies the <b>plot</b>: {@value #MAAT}x{@value #MAAT} tiles (one tile = one block), grass top at
 *   y = {@value #Y}. Around it a pink path and a grass island ({@value #EILAND} wide).</li>
 *   <li>The plot (balans.md 6): an overgrown square of 72x72 (tiles {@value #BINNEN}..{@value #BINNEN}+71) in 72 plukjes of
 *   8x8 around an open middle of 24x24 (tiles {@value #MIDDEN}..{@value #MIDDEN}+23); around it a border of 16 wide with
 *   16 kavels to buy (4 corners 16x16, 12 side pieces 24x16).</li>
 * </ul>
 */
public final class Plot {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("erfenis"));
    /** Cell size, plot size (tiles), the height of the grass top, the width of the grass island. */
    public static final int CEL = 256, MAAT = 104, Y = 64, EILAND = 136;
    /** The highest a building may reach above the grass (stamping clears this much). */
    public static final int MAX_HOOGTE = 24;
    /** Where the overgrown 72x72 starts, where the open middle starts, the plukje size, plukjes per side. */
    public static final int BINNEN = 16, MIDDEN = 40, PLUK = 8, PER_RIJ = 9;
    public static final int PLUKJES = 72, KAVELS = 16;

    private Plot() {
    }

    /** The min corner (tile 0,0 at the grass top) of the plot in cell (cx, cz). */
    public static BlockPos oorsprong(int cx, int cz) {
        int rand = (CEL - MAAT) / 2;
        return new BlockPos(cx * CEL + rand, Y, cz * CEL + rand);
    }

    public static int cel(int blok) {
        return Math.floorDiv(blok, CEL);
    }

    public static long sleutel(int cx, int cz) {
        return (long) cx << 32 | (cz & 0xFFFFFFFFL);
    }

    /**
     * Where a visitor arrives: in the open middle, on its south edge (feet), looking north. (The way in is through the
     * overgrown land: arriving on the outer path would mean a long walk through the bushes.)
     */
    public static BlockPos aankomst(int cx, int cz) {
        return oorsprong(cx, cz).offset(MIDDEN + 12, 1, MIDDEN + 23);
    }

    // =====================================================================================================================
    // the land: open middle, plukjes, kavels
    // =====================================================================================================================

    /** Is this tile in the open middle (always usable)? */
    public static boolean inMidden(int tx, int tz) {
        return tx >= MIDDEN && tx < MIDDEN + 24 && tz >= MIDDEN && tz < MIDDEN + 24;
    }

    public static boolean opPlot(int tx, int tz) {
        return tx >= 0 && tz >= 0 && tx < MAAT && tz < MAAT;
    }

    /** The plukje (0..71) on this tile, or -1 (the middle, a kavel, outside). */
    public static int plukjeOp(int tx, int tz) {
        if (tx < BINNEN || tz < BINNEN || tx >= BINNEN + 72 || tz >= BINNEN + 72) {
            return -1;
        }
        int i = (tx - BINNEN) / PLUK, j = (tz - BINNEN) / PLUK;
        return plukjeIndex(i, j);
    }

    /** Index of the plukje in grid column i, row j (0..8), or -1 for the 3x3 middle. Row by row, the middle skipped. */
    public static int plukjeIndex(int i, int j) {
        if (i < 0 || j < 0 || i >= PER_RIJ || j >= PER_RIJ || (i >= 3 && i <= 5 && j >= 3 && j <= 5)) {
            return -1;
        }
        int n = j * PER_RIJ + i;
        // (the middle cells before this one don't count)
        int voor = 0;
        for (int jj = 3; jj <= 5; jj++) {
            for (int ii = 3; ii <= 5; ii++) {
                if (jj * PER_RIJ + ii < n) {
                    voor++;
                }
            }
        }
        return n - voor;
    }

    /** {col, row} of plukje n. */
    public static int[] plukjeCel(int n) {
        int teller = 0;
        for (int j = 0; j < PER_RIJ; j++) {
            for (int i = 0; i < PER_RIJ; i++) {
                if (plukjeIndex(i, j) < 0) {
                    continue;
                }
                if (teller++ == n) {
                    return new int[] {i, j};
                }
            }
        }
        return new int[] {0, 0};
    }

    /** The min corner tile {x, z} of plukje n. */
    public static int[] plukjeHoek(int n) {
        int[] c = plukjeCel(n);
        return new int[] {BINNEN + c[0] * PLUK, BINNEN + c[1] * PLUK};
    }

    /** Ring 1 (around the middle, 16), 2 (24) or 3 (the outer, 32). */
    public static int ring(int plukje) {
        int[] c = plukjeCel(plukje);
        return Math.max(Math.abs(c[0] - 4), Math.abs(c[1] - 4)) - 1;
    }

    /** The kavel (0..15) on this tile, or -1. Order: north sides 0-2, east 3-5, south 6-8, west 9-11, corners NW NE SE SW 12-15. */
    public static int kavelOp(int tx, int tz) {
        if (!opPlot(tx, tz)) {
            return -1;
        }
        boolean w = tx < BINNEN, e = tx >= BINNEN + 72, n = tz < BINNEN, s = tz >= BINNEN + 72;
        if (n && w) {
            return 12;
        }
        if (n && e) {
            return 13;
        }
        if (s && e) {
            return 14;
        }
        if (s && w) {
            return 15;
        }
        if (n) {
            return (tx - BINNEN) / 24;
        }
        if (e) {
            return 3 + (tz - BINNEN) / 24;
        }
        if (s) {
            return 6 + (tx - BINNEN) / 24;
        }
        if (w) {
            return 9 + (tz - BINNEN) / 24;
        }
        return -1;
    }

    /** {x, z, w, h} of kavel k. */
    public static int[] kavelVak(int k) {
        int ver = BINNEN + 72;
        return switch (k) {
            case 0, 1, 2 -> new int[] {BINNEN + 24 * k, 0, 24, 16};
            case 3, 4, 5 -> new int[] {ver, BINNEN + 24 * (k - 3), 16, 24};
            case 6, 7, 8 -> new int[] {BINNEN + 24 * (k - 6), ver, 24, 16};
            case 9, 10, 11 -> new int[] {0, BINNEN + 24 * (k - 9), 16, 24};
            case 12 -> new int[] {0, 0, 16, 16};
            case 13 -> new int[] {ver, 0, 16, 16};
            case 14 -> new int[] {ver, ver, 16, 16};
            default -> new int[] {0, ver, 16, 16};
        };
    }

    /**
     * The n-th cell of a square spiral around (0, 0): 0 = (0,0), 1 = (1,0), 2 = (1,1), 3 = (0,1), 4 = (-1,1), 5 = (-1,0), ...
     */
    public static int[] spiraal(int n) {
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
