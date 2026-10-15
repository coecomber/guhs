package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayDeque;
import java.util.List;

/**
 * biomes3 wereld, the Wolkenweide: THE JUMP RULE, and a search that walks it over blocks.
 * <p>
 * What a player can do without sprint-jump tricks (walking speed, an ordinary jump of 1.25 blocks; on cloud you stand two
 * pixels lower, which still clears one block):
 * <ul>
 *   <li><b>up</b>: at most ONE block higher, across a gap of at most ONE block ({@link #GAT_OMHOOG});</li>
 *   <li><b>level or down</b>: across a gap of at most TWO blocks ({@link #GAT_VLAK});</li>
 *   <li><b>landing</b>: a drop of at most three blocks onto anything solid ({@link #VAL_HARD}); any drop onto cloud, water
 *       or a lift pad (cloud has no fall damage);</li>
 *   <li><b>room</b>: three free blocks above where you take off, two above where you land, and the columns you fly over
 *       free from the higher of the two floors up to two blocks above it.</li>
 * </ul>
 * The gap is counted between the two blocks' edges: {@code sqrt(max(|dx| - 1, 0)^2 + max(|dz| - 1, 0)^2)}, so a diagonal
 * counts for its real length. Heights are in half blocks (a slab's top is a half).
 * <p>
 * Lift columns: whoever stands in or beside a wolkenlift column rides to its top and is puffed onto the standing places
 * within three blocks of it; whoever steps into a wolkenstroom column floats down to its pad.
 * <p>
 * {@link WolkTerrein} builds its ways up from this rule (stepping stones one up with a one-block gap), the game test
 * {@code BioWereldWolkGameTests} walks it over the model's blocks, and the offline tool
 * {@code guhs_workbio/reports/screens/wereld-wolk/_bron/wolkmeet.py} walks the same rule over generated region files.
 */
public final class WolkRoute {
    public static final double GAT_OMHOOG = 1.0, GAT_VLAK = 2.0;
    /** The highest step up and the deepest drop onto solid ground, in blocks. */
    public static final double RIJS = 1.0, VAL_HARD = 3.0;

    /** The gap between two blocks (dx, dz apart), edge to edge. */
    public static double gat(int dx, int dz) {
        int a = Math.max(Math.abs(dx) - 1, 0), b = Math.max(Math.abs(dz) - 1, 0);
        return Math.sqrt(a * a + b * b);
    }

    /**
     * May a player go from a floor to another floor (dx, dz) blocks away? Heights in HALF blocks; zacht: the landing is
     * cloud, water or a lift pad. (Room above and between is checked by {@link Blokken}.)
     */
    public static boolean kan(int dx, int dz, int van2, int naar2, boolean zacht) {
        if (dx == 0 && dz == 0) {
            return false;
        }
        double g = gat(dx, dz);
        double rijs = (naar2 - van2) / 2.0;
        if (rijs > RIJS) {
            return false;
        }
        if (rijs > 0) {
            return g <= GAT_OMHOOG;
        }
        return g <= GAT_VLAK && (zacht || -rijs <= VAL_HARD);
    }

    // --- blocks ---------------------------------------------------------------------------------------------------------------
    public static final byte LUCHT = 0, VAST = 1, WOLK = 2, PLAAT = 3, STROOM = 4, PAD = 5, WATER = 6;

    /** A lift column for the search. */
    public record Lift(int x, int z, int voet, int boven, boolean omlaag) {
    }

    /** A box of blocks to search in (x, y, z from an origin). */
    public static final class Blokken {
        public final int x0, y0, z0, bx, by, bz;
        public final byte[] soort;
        /** Per standing place (x, floor in half blocks, z): reached? */
        private boolean[] bereikt;

        public Blokken(int x0, int y0, int z0, int bx, int by, int bz) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            soort = new byte[bx * by * bz];
        }

        public void zet(int x, int y, int z, byte s) {
            x -= x0;
            y -= y0;
            z -= z0;
            if (x >= 0 && x < bx && y >= 0 && y < by && z >= 0 && z < bz) {
                soort[(y * bz + z) * bx + x] = s;
            }
        }

        public byte op(int x, int y, int z) {
            x -= x0;
            y -= y0;
            z -= z0;
            return x >= 0 && x < bx && y >= 0 && y < by && z >= 0 && z < bz ? soort[(y * bz + z) * bx + x] : LUCHT;
        }

        private boolean open(int x, int y, int z) {
            byte s = op(x, y, z);
            return s == LUCHT || s == STROOM || s == WATER;
        }

        /** Can a player stand with a floor at h2 half blocks (world height) in this column? */
        public boolean staat(int x, int h2, int z) {
            int y = Math.floorDiv(h2, 2);
            byte onder = (h2 & 1) == 1 ? op(x, y, z) : op(x, y - 1, z);
            if ((h2 & 1) == 1) {
                return onder == PLAAT && open(x, y + 1, z) && open(x, y + 2, z);
            }
            return (onder == VAST || onder == WOLK || onder == PAD) && open(x, y, z) && open(x, y + 1, z);
        }

        private boolean zacht(int x, int h2, int z) {
            int y = Math.floorDiv(h2, 2);
            byte onder = (h2 & 1) == 1 ? op(x, y, z) : op(x, y - 1, z);
            return onder == WOLK || onder == PLAAT || onder == PAD || op(x, y, z) == WATER;
        }

        private int index(int x, int h2, int z) {
            return ((h2 - 2 * y0) * bz + (z - z0)) * bx + (x - x0);
        }

        /** May a player move between two standing places (the rule, and the room it needs)? */
        public boolean stap(int x, int a2, int z, int nx, int b2, int nz) {
            int dx = nx - x, dz = nz - z;
            if (!kan(dx, dz, a2, b2, zacht(nx, b2, nz))) {
                return false;
            }
            int ya = Math.floorDiv(a2 + 1, 2), yb = Math.floorDiv(b2 + 1, 2), hoog = Math.max(ya, yb);
            boolean sprong = b2 > a2 || gat(dx, dz) >= 1;
            if (sprong && !open(x, ya + 2, z)) {
                return false;
            }
            int n = Math.max(Math.abs(dx), Math.abs(dz));
            for (int t = 1; t < n; t++) {
                int tx = x + Math.round(dx * t / (float) n), tz = z + Math.round(dz * t / (float) n);
                for (int y = hoog; y <= hoog + 2; y++) {
                    if (!open(tx, y, tz)) {
                        return false;
                    }
                }
            }
            // going down: the landing column is open up to where you come from
            for (int y = yb; y <= ya + 1; y++) {
                if (!open(nx, y, nz)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Walks the rule from the given standing places; afterwards {@link #bereikt} answers. start: (x, floor in half
         * blocks, z) triples.
         */
        public int loop(int[] start, List<Lift> liften) {
            bereikt = new boolean[bx * bz * by * 2];
            // every standing place per column, from the top down
            int[][] vloeren = new int[bx * bz][];
            int[] buf = new int[by * 2];
            for (int x = x0; x < x0 + bx; x++) {
                for (int z = z0; z < z0 + bz; z++) {
                    int k = 0;
                    for (int h2 = 2 * (y0 + by) - 1; h2 >= 2 * y0 + 2; h2--) {
                        if (staat(x, h2, z)) {
                            buf[k++] = h2;
                        }
                    }
                    vloeren[(z - z0) * bx + (x - x0)] = java.util.Arrays.copyOf(buf, k);
                }
            }
            ArrayDeque<int[]> rij = new ArrayDeque<>();
            int n = 0;
            for (int i = 0; i < start.length; i += 3) {
                if (staat(start[i], start[i + 1], start[i + 2]) && !bereikt[index(start[i], start[i + 1], start[i + 2])]) {
                    bereikt[index(start[i], start[i + 1], start[i + 2])] = true;
                    rij.add(new int[]{start[i], start[i + 1], start[i + 2]});
                }
            }
            boolean[] gebruikt = new boolean[liften.size()];
            while (!rij.isEmpty()) {
                int[] c = rij.poll();
                n++;
                int x = c[0], a2 = c[1], z = c[2];
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        int nx = x + dx, nz = z + dz;
                        if (dx == 0 && dz == 0 || gat(dx, dz) > GAT_VLAK || nx < x0 || nx >= x0 + bx || nz < z0 || nz >= z0 + bz) {
                            continue;
                        }
                        for (int b2 : vloeren[(nz - z0) * bx + (nx - x0)]) {
                            if (b2 <= a2 + 2 && !bereikt[index(nx, b2, nz)] && stap(x, a2, z, nx, b2, nz)) {
                                bereikt[index(nx, b2, nz)] = true;
                                rij.add(new int[]{nx, b2, nz});
                            }
                        }
                    }
                }
                int voeten = Math.floorDiv(a2 + 1, 2);
                for (int i = 0; i < liften.size(); i++) {
                    Lift l = liften.get(i);
                    if (gebruikt[i] || Math.abs(l.x - x) > 2 || Math.abs(l.z - z) > 2 || voeten < l.voet || voeten > l.boven + 1) {
                        continue;
                    }
                    gebruikt[i] = true;
                    int lo = l.omlaag ? l.voet : l.boven - 3, hi = l.omlaag ? l.voet + 2 : l.boven, reik = l.omlaag ? 2 : 3;
                    for (int ax = l.x - reik; ax <= l.x + reik; ax++) {
                        for (int az = l.z - reik; az <= l.z + reik; az++) {
                            for (int b2 = 2 * lo; b2 <= 2 * hi + 1; b2++) {
                                if (ax >= x0 && ax < x0 + bx && az >= z0 && az < z0 + bz && b2 >= 2 * y0 + 2 && b2 < 2 * (y0 + by) && !bereikt[index(ax, b2, az)]
                                        && staat(ax, b2, az)) {
                                    bereikt[index(ax, b2, az)] = true;
                                    rij.add(new int[]{ax, b2, az});
                                }
                            }
                        }
                    }
                }
            }
            return n;
        }

        public boolean bereikt(int x, int h2, int z) {
            return x >= x0 && x < x0 + bx && z >= z0 && z < z0 + bz && h2 >= 2 * y0 && h2 < 2 * (y0 + by) && bereikt[index(x, h2, z)];
        }
    }

    private WolkRoute() {
    }
}
