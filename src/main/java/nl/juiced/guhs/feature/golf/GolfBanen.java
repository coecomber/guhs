package nl.juiced.guhs.feature.golf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * 2.10: the three tees of every hole and what they mean: the par per level, and the extra pink slime bumpers the Golfguh
 * puts on the course for a lastig round (and takes away again after it).
 * <p>
 * The bumpers are kept in template coordinates of the course (tools/features/golf.py builds it; its self-check reads
 * these numbers from this file, so keep the one-per-line layout of {@link #LASTIG_BUMPERS} and {@link #CUPS}). In the
 * world they are found from the cup of the hole and the way its medium tee faces (the course is only ever turned, never
 * mirrored).
 */
public final class GolfBanen {
    /** Par per level and hole: makkelijk (the tee close to the cup), medium (as it always was), lastig (the far tee). */
    public static final int[][] PARS = {
            {2, 2, 2, 2, 2, 2, 2, 2, 2},
            {2, 3, 3, 3, 3, 3, 3, 3, 4},
            {3, 4, 4, 4, 4, 4, 4, 4, 5}};

    /** The cups in the template (x, y, z), hole 1..9. */
    static final int[][] CUPS = {
            {12, 1, 84},
            {22, 1, 57},
            {27, 1, 21},
            {58, 1, 12},
            {87, 1, 7},
            {85, 1, 37},
            {85, 1, 65},
            {72, 2, 78},
            {56, 1, 74}};

    /** The way the medium tee of each hole faces in the template. */
    static final Direction[] TEE_FACING = {Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST,
            Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST};

    /** The height of the lastig bumpers in the template (one block above the felt). */
    static final int BUMPER_Y = 2;

    /** The extra slime bumpers of a lastig round, per hole (template x, z). */
    static final int[][][] LASTIG_BUMPERS = {
            /* 1 */ {{27, 83}, {27, 85}, {16, 83}, {16, 85}},
            /* 2 */ {{7, 70}, {16, 57}},
            /* 3 */ {{27, 46}, {26, 40}, {28, 40}},
            /* 4 */ {{40, 10}, {40, 11}, {40, 13}, {40, 14}, {55, 12}},
            /* 5 */ {{69, 6}, {69, 8}},
            /* 6 */ {{84, 21}, {86, 21}, {85, 26}},
            /* 7 */ {{85, 53}, {83, 54}, {87, 54}},
            /* 8 */ {{84, 77}, {84, 79}, {81, 78}},
            /* 9 */ {{82, 87}, {82, 89}, {66, 88}}};

    /** The par of a hole on a level. */
    public static int par(Niveau niveau, int hole) {
        return PARS[niveau.ordinal()][hole];
    }

    /** The par of a whole round on a level (makkelijk 18, medium 27, lastig 36). */
    public static int totalPar(Niveau niveau) {
        int total = 0;
        for (int p : PARS[niveau.ordinal()]) {
            total += p;
        }
        return total;
    }

    /**
     * Where the lastig bumpers of a hole are in the world, from where its cup is and the way its medium tee faces there.
     */
    public static List<BlockPos> lastigBumpers(int hole, BlockPos cup, Direction teeFacing) {
        int turns = (teeFacing.get2DDataValue() - TEE_FACING[hole].get2DDataValue()) & 3;   // (clockwise quarter turns)
        List<BlockPos> list = new ArrayList<>();
        int[] c = CUPS[hole];
        for (int[] b : LASTIG_BUMPERS[hole]) {
            int dx = b[0] - c[0], dz = b[1] - c[2];
            for (int i = 0; i < turns; i++) {
                int t = dx;
                dx = -dz;
                dz = t;
            }
            list.add(cup.offset(dx, BUMPER_Y - c[1], dz));
        }
        return list;
    }

    private GolfBanen() {
    }
}
