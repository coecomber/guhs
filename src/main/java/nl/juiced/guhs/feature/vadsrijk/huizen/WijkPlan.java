package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * U-HUIZEN: the layout of one ward (wijk) of Het Knuffelkwartier. Pure geometry, no level: the gametests use it in the overworld.
 * <ul>
 *   <li>Ward {@code w} has its centre at ({@code w * AFSTAND}, {@value #VLOER}, 0). The flat generator lays the grass (top at
 *   y {@value #GROND}); a ward only adds streets, the square and the houses.</li>
 *   <li>The square: |x|, |z| &lt;= {@value #PLEIN} round the centre (fountain, market stalls, notice board, the gate back).</li>
 *   <li>Four streets (east, south, west, north), 5 wide, from the square to {@value #STRAAT_EIND}; a 1-block sidewalk on each side.
 *   Each street has 6 plots: 3 on each side, {@value #KAVEL} x {@value #KAVEL}, 1 block apart. 4 x 6 = {@value #KAVELS}.</li>
 *   <li>A plot has its own frame: u across (0..19), v away from the street (0 = the street edge). The house templates are written
 *   in that frame ("canonical": street to the NORTH, u = east, v = south) and turned by {@link Kavel#rotatie()}.</li>
 * </ul>
 */
public final class WijkPlan {
    public static final int KAVELS = 24, KAVEL = 20, AFSTAND = 1024;
    /** The top of the grass, and where you stand. */
    public static final int GROND = 63, VLOER = 64;
    /** Half the square; the streets; where the first plot starts along a street; how far a plot is from the street centre. */
    public static final int PLEIN = 12, STRAAT_BEGIN = 13, STRAAT_EIND = 89, KAVEL_BEGIN = 26, ZIJ = 4, STAP = 21;
    /** Beyond this (from the ward centre, either axis) the ward ends: players are put back on the square. */
    public static final int RAND = 100;
    /** How high above the floor an owner may build (the roof of a villa is lower). */
    public static final int HOOGTE = 18;
    /** The four streets, in plot-number order. */
    public static final Direction[] ARMEN = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    /** In the plot's own frame: the gate/path/door column and the mailbox. */
    public static final int POORT_U = 9, POST_U = 11, POST_V = 1;

    private WijkPlan() {
    }

    public static BlockPos midden(int wijk) {
        return new BlockPos(wijk * AFSTAND, VLOER, 0);
    }

    /** The ward that x lies in (round the nearest centre). */
    public static int wijkVan(double x) {
        return Math.floorDiv((int) Math.floor(x) + AFSTAND / 2, AFSTAND);
    }

    /** Where you arrive on the square (in front of the gate, looking south at the fountain). */
    public static Vec3 aankomst(int wijk) {
        BlockPos m = midden(wijk);
        return new Vec3(m.getX() + 0.5, VLOER, m.getZ() - 5.5);
    }

    /** Inside the ward (the street ends + the plots, with a margin)? */
    public static boolean binnenWijk(double x, double z) {
        int w = wijkVan(x);
        double dx = x - w * AFSTAND, dz = z;
        return Math.abs(dx) <= RAND && Math.abs(dz) <= RAND;
    }

    /** The gate back (the arch on the north side of the square): x -1..1, z -10, y 64..66. */
    public static boolean inPoort(double x, double y, double z) {
        int w = wijkVan(x);
        double dx = x - w * AFSTAND;
        return dx >= -1 && dx < 2 && z >= -10.3 && z < -9 && y >= VLOER - 0.5 && y < VLOER + 3;
    }

    // ---- plots ---------------------------------------------------------------------------------------------------------------

    /**
     * One plot: ward, number 0..23, its box (inclusive) and the direction from the plot to its street.
     */
    public record Kavel(int wijk, int nr, int minX, int minZ, Direction straat) {
        public int maxX() {
            return minX + KAVEL - 1;
        }

        public int maxZ() {
            return minZ + KAVEL - 1;
        }

        /** A unique key (ward * 24 + nr). */
        public int sleutel() {
            return wijk * KAVELS + nr;
        }

        /** Turns the canonical template (street NORTH) to this plot. */
        public Rotation rotatie() {
            return switch (straat) {
                case EAST -> Rotation.CLOCKWISE_90;
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        public Direction uRichting() {
            return rotatie().rotate(Direction.EAST);
        }

        public Direction vRichting() {
            return rotatie().rotate(Direction.SOUTH);
        }

        private int x0() {
            return uRichting().getStepX() < 0 || vRichting().getStepX() < 0 ? maxX() : minX;
        }

        private int z0() {
            return uRichting().getStepZ() < 0 || vRichting().getStepZ() < 0 ? maxZ() : minZ;
        }

        /** The world block of (u, y, v) in the plot's frame. */
        public BlockPos wereld(int u, int y, int v) {
            Direction a = uRichting(), b = vRichting();
            return new BlockPos(x0() + u * a.getStepX() + v * b.getStepX(), y, z0() + u * a.getStepZ() + v * b.getStepZ());
        }

        /** The world point of (u, y, v) (block corners: u = 9.5 is the middle of column 9). */
        public Vec3 wereld(double u, double y, double v) {
            Direction a = uRichting(), b = vRichting();
            // (the frame's origin is the outer corner of block (0, 0): shift by the half block the axes point away from)
            double ox = x0() + (a.getStepX() < 0 || b.getStepX() < 0 ? 1 : 0), oz = z0() + (a.getStepZ() < 0 || b.getStepZ() < 0 ? 1 : 0);
            return new Vec3(ox + u * a.getStepX() + v * b.getStepX(), y, oz + u * a.getStepZ() + v * b.getStepZ());
        }

        /** (u, v) of a world block in this frame. */
        public int[] lokaal(BlockPos p) {
            Direction a = uRichting(), b = vRichting();
            int dx = p.getX() - x0(), dz = p.getZ() - z0();
            return new int[] {dx * a.getStepX() + dz * a.getStepZ(), dx * b.getStepX() + dz * b.getStepZ()};
        }

        public boolean bevat(double x, double z) {
            return x >= minX && x < maxX() + 1 && z >= minZ && z < maxZ() + 1;
        }

        /** Inside the fence (u, v 1..18)? */
        public boolean binnenHek(BlockPos p) {
            int[] l = lokaal(p);
            return l[0] >= 1 && l[0] <= KAVEL - 2 && l[1] >= 1 && l[1] <= KAVEL - 2;
        }

        /** The yaw that looks from the street into the plot. */
        public float yawNaarBinnen() {
            return vRichting().toYRot();
        }

        /** Where a visitor arrives: on the sidewalk in front of the gate, facing the house. */
        public Vec3 voorDeur() {
            return wereld(POORT_U + 0.5, VLOER, -0.5);
        }

        public Vec3 midden() {
            return new Vec3(minX + KAVEL / 2.0, VLOER, minZ + KAVEL / 2.0);
        }
    }

    /** Plot {@code nr} (0..23) of ward {@code wijk}. */
    public static Kavel kavel(int wijk, int nr) {
        int arm = nr / 6, i = (nr % 6) / 2;
        int kant = nr % 2 == 0 ? 1 : -1;
        Direction o = ARMEN[arm];
        Direction s = o.getClockWise();
        int a0 = KAVEL_BEGIN + i * STAP, a1 = a0 + KAVEL - 1;
        int s0 = kant > 0 ? ZIJ : -ZIJ - KAVEL + 1, s1 = s0 + KAVEL - 1;
        BlockPos m = midden(wijk);
        int x1 = m.getX() + a0 * o.getStepX() + s0 * s.getStepX(), x2 = m.getX() + a1 * o.getStepX() + s1 * s.getStepX();
        int z1 = m.getZ() + a0 * o.getStepZ() + s0 * s.getStepZ(), z2 = m.getZ() + a1 * o.getStepZ() + s1 * s.getStepZ();
        Direction straat = kant > 0 ? s.getOpposite() : s;
        return new Kavel(wijk, nr, Math.min(x1, x2), Math.min(z1, z2), straat);
    }

    public static Kavel kavel(int sleutel) {
        return kavel(Math.floorDiv(sleutel, KAVELS), Math.floorMod(sleutel, KAVELS));
    }

    public static List<Kavel> kavels(int wijk) {
        List<Kavel> l = new ArrayList<>();
        for (int i = 0; i < KAVELS; i++) {
            l.add(kavel(wijk, i));
        }
        return l;
    }

    /** The plot that contains (x, z), or null (street, square, park). */
    @Nullable
    public static Kavel kavelOp(double x, double z) {
        int w = wijkVan(x);
        for (int i = 0; i < KAVELS; i++) {
            Kavel k = kavel(w, i);
            if (k.bevat(x, z)) {
                return k;
            }
        }
        return null;
    }

    @Nullable
    public static Kavel kavelOp(BlockPos p) {
        return kavelOp(p.getX() + 0.5, p.getZ() + 0.5);
    }
}
