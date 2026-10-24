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

    /**
     * 1.7.0: the par of the back 9 (holes 10-18, "Rondje Guhmension") per level; tools/features/golf18.py checks these
     * against the holes it builds.
     */
    public static final int[][] PARS_ACHTER = {
            {3, 3, 3, 2, 2, 3, 3, 3, 3},
            {4, 4, 4, 4, 4, 4, 4, 5, 5},
            {5, 5, 5, 5, 5, 5, 5, 6, 6}};

    // --- 1.7.0: the 18-hole template (guh_golfbaan18); golf18.py checks these numbers --------------------------------------
    /** The cup of hole 1 in the 18-hole template (the front 9 is the old course, moved): the anchor of everything below. */
    static final int[] CUP1_18 = {12, 6, 116};
    /** The medium tee of hole 10 in the 18-hole template: there is one there = this course has a back 9. */
    static final int[] TEE10_18 = {119, 7, 35};
    /** The back 9 in the 18-hole template (x0, y0, z0, x1, y1, z1): scanned for its tees, cups and moving obstacles. */
    static final int[] ACHTER_18 = {100, 0, 0, 199, 33, 159};
    /** The whole 18-hole template (for "you walked away from the course"). */
    static final int[] BAAN_18 = {0, 0, 0, 199, 33, 159};

    /** The par of a hole on a level (0-8 the front 9, 9-17 the back 9). */
    public static int par(Niveau niveau, int hole) {
        return hole < 9 ? PARS[niveau.ordinal()][hole] : PARS_ACHTER[niveau.ordinal()][hole - 9];
    }

    /**
     * A spot of the 18-hole template in the world, from where the cup of hole 1 is and the way its medium tee faces there
     * (the course is only ever turned, never mirrored).
     */
    public static BlockPos inWereld18(int[] t, BlockPos cup1, Direction tee1Facing) {
        int turns = (tee1Facing.get2DDataValue() - TEE_FACING[0].get2DDataValue()) & 3;
        int dx = t[0] - CUP1_18[0], dz = t[2] - CUP1_18[2];
        for (int i = 0; i < turns; i++) {
            int tmp = dx;
            dx = -dz;
            dz = tmp;
        }
        return cup1.offset(dx, t[1] - CUP1_18[1], dz);
    }

    /** A box of the 18-hole template in the world: {min, max}. */
    public static BlockPos[] doos18(int[] box, BlockPos cup1, Direction tee1Facing) {
        BlockPos a = inWereld18(new int[]{box[0], box[1], box[2]}, cup1, tee1Facing);
        BlockPos b = inWereld18(new int[]{box[3], box[4], box[5]}, cup1, tee1Facing);
        return new BlockPos[]{new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()))};
    }

    /** Golfballetjes for a hole of the back 9 (more than the front 9): hole-in-one 8, eagle 6, birdie 5, par 4, bogey 2, worse 1. */
    public static int balletjesAchter(int score, int par, boolean holed) {
        if (!holed) {
            return 0;
        }
        int diff = score - par;
        return score == 1 ? 8 : diff <= -2 ? 6 : diff == -1 ? 5 : diff == 0 ? 4 : diff == 1 ? 2 : 1;
    }

    /** The bonus after all 18 holes: 5, plus 8 at par or under, plus 4 for beating your own 18-hole record. */
    public static int bonus18(int diff, boolean beatRecord) {
        return 5 + (diff <= 0 ? 8 : 0) + (beatRecord ? 4 : 0);
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
