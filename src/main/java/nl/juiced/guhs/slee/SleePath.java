package nl.juiced.guhs.slee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.SleeRailPartBlock;

/**
 * The geometry of guh sled rails (mirrored in tools/slee_track.py). A piece's "anchor" block (where the block entity
 * lives) is on its entry side. In the piece's own frame (facing NORTH = travelling towards -z, anchor block centre at
 * 0,0) the track runs:
 * <ul>
 *     <li>STRAIGHT: from (0.5, 0.5) to (0.5, -3.5)</li>
 *     <li>CURVE_RIGHT: a quarter circle (radius 2, centre (2.5, 0.5)) from (0.5, 0.5) to (2.5, -1.5), leaving towards +x</li>
 *     <li>CURVE_LEFT: the mirror image, from (0.5, 0.5) to (-1.5, -1.5), leaving towards -x</li>
 *     <li>SLOPE: straight on, rising 4 blocks</li>
 *     <li>DROP: a coaster hill, rising 8 blocks over 4, flat at both ends</li>
 *     <li>SPIRAL_RIGHT / SPIRAL_LEFT: a corkscrew, half a turn (radius 1.5) rising 4; it comes back 3 blocks over</li>
 *     <li>JUMP: a 2-block ramp, then the sled flies through the air to a rail 12 blocks further on</li>
 * </ul>
 * The same path is used to draw the rails and to move the sled, so the sled always sits on them.
 */
public final class SleePath {
    public enum Shape implements StringRepresentable {
        STRAIGHT, CURVE_LEFT, CURVE_RIGHT, SLOPE, DROP, SPIRAL_LEFT, SPIRAL_RIGHT, JUMP;

        /** Arc length at 256 steps of u. */
        private double[] table;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public double length() {
            return lengths()[256];
        }

        /** Pieces you can also lay going down (ridden backwards from the top). */
        public boolean canGoDown() {
            return this == SLOPE || this == DROP || this == SPIRAL_LEFT || this == SPIRAL_RIGHT;
        }

        private double[] lengths() {
            if (table == null) {
                double[] l = new double[257];
                Vec3 prev = raw(this, 0).pos();
                for (int i = 1; i <= 256; i++) {
                    Vec3 p = raw(this, i / 256.0).pos();
                    l[i] = l[i - 1] + p.distanceTo(prev);
                    prev = p;
                }
                table = l;
            }
            return table;
        }

        /** The raw curve's u at a share t (0..1) of the length, so the sled goes evenly fast along it. */
        double uAt(double t) {
            if (this != DROP && this != JUMP) {
                return t;
            }
            double[] l = lengths();
            double target = Mth.clamp(t, 0, 1) * l[256];
            int lo = 0, hi = 256;
            while (hi - lo > 1) {
                int mid = (lo + hi) / 2;
                if (l[mid] < target) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            double span = l[hi] - l[lo];
            return (lo + (span <= 0 ? 0 : (target - l[lo]) / span)) / 256.0;
        }
    }

    /** How high the sled sits above the rail's block. */
    public static final double RIDE_HEIGHT = 0.2;
    /** The jump: ramp length, how far the sled goes in all, and how fast the flight bends down. */
    public static final double JUMP_RAMP = 2, JUMP_LENGTH = 12, JUMP_A = 0.11;

    /** A point on a piece: position and (forward) heading. */
    public record Point(Vec3 pos, Vec3 heading) {
    }

    /** The point at t (0..1, a share of the length) in the piece's own frame (NORTH, anchor centre at the origin). */
    public static Point local(Shape shape, double t) {
        Point p = raw(shape, shape.uAt(t));
        return new Point(p.pos(), p.heading().normalize());
    }

    /** The curve at u (0..1, not always evenly fast), with an unscaled direction. */
    static Point raw(Shape shape, double u) {
        switch (shape) {
            case CURVE_RIGHT -> {
                double a = Math.toRadians(180 + 90 * u);
                return new Point(new Vec3(2.5 + 2 * Math.cos(a), 0, 0.5 + 2 * Math.sin(a)), new Vec3(-Math.sin(a), 0, Math.cos(a)));
            }
            case CURVE_LEFT -> {
                double a = Math.toRadians(-90 * u);
                return new Point(new Vec3(-1.5 + 2 * Math.cos(a), 0, 0.5 + 2 * Math.sin(a)), new Vec3(Math.sin(a), 0, -Math.cos(a)));
            }
            case SLOPE -> {
                return new Point(new Vec3(0.5, 4 * u, 0.5 - 4 * u), new Vec3(0, 1, -1));
            }
            case DROP -> {
                return new Point(new Vec3(0.5, 8 * (3 * u * u - 2 * u * u * u), 0.5 - 4 * u), new Vec3(0, 48 * u * (1 - u), -4));
            }
            case SPIRAL_RIGHT, SPIRAL_LEFT -> {
                double a = Math.PI + Math.PI * u;
                double x = 1.5 * Math.cos(a), dx = -1.5 * Math.PI * Math.sin(a);
                if (shape == Shape.SPIRAL_LEFT) {
                    x = 1 - x;
                    dx = -dx;
                }
                return new Point(new Vec3(x, 4 * u, 0.5 + 1.5 * Math.sin(a)), new Vec3(dx, 4, 1.5 * Math.PI * Math.cos(a)));
            }
            case JUMP -> {
                double d = JUMP_LENGTH * u;
                if (d <= JUMP_RAMP) {
                    return new Point(new Vec3(0.5, 0.25 * d * d, 0.5 - d), new Vec3(0, 0.5 * d, -1));
                }
                double f = d - JUMP_RAMP;
                return new Point(new Vec3(0.5, 1 + f - JUMP_A * f * f, 0.5 - d), new Vec3(0, 1 - 2 * JUMP_A * f, -1));
            }
            default -> {
                return new Point(new Vec3(0.5, 0, 0.5 - 4 * u), new Vec3(0, 0, -1));
            }
        }
    }

    /** Up to where (t) the rails are drawn: the jump's flight has none. */
    public static double railEnd(Shape shape) {
        if (shape != Shape.JUMP) {
            return 1;
        }
        double ramp = 0;
        Vec3 prev = raw(shape, 0).pos();
        for (int i = 1; i <= 64; i++) {
            Vec3 p = raw(shape, JUMP_RAMP / JUMP_LENGTH * i / 64).pos();
            ramp += p.distanceTo(prev);
            prev = p;
        }
        return ramp / shape.length();
    }

    /** Rotates a vector of the piece's frame (built facing NORTH) to face another way. */
    public static Vec3 rotate(Vec3 v, Direction facing) {
        return switch (facing) {
            case SOUTH -> new Vec3(-v.x, v.y, -v.z);
            case EAST -> new Vec3(-v.z, v.y, v.x);
            case WEST -> new Vec3(v.z, v.y, -v.x);
            default -> v;
        };
    }

    public static BlockPos rotate(int x, int y, int z, Direction facing) {
        Vec3 r = rotate(new Vec3(x, y, z), facing);
        return new BlockPos((int) Math.round(r.x), y, (int) Math.round(r.z));
    }

    /** The point at t (0..1) in the world. */
    public static Point world(BlockPos anchor, Direction facing, Shape shape, double t) {
        Point p = local(shape, t);
        Vec3 pos = Vec3.atBottomCenterOf(anchor).add(rotate(p.pos(), facing)).add(0, RIDE_HEIGHT, 0);
        return new Point(pos, rotate(p.heading(), facing));
    }

    /** The coaster pieces' blocks (from tools/slee_track.py cells(), which follows the curve). */
    private static final int[][] DROP_CELLS = {{0, 1, -1}, {0, 1, 0}, {0, 2, -1}, {0, 3, -1}, {0, 4, -2}, {0, 4, -1}, {0, 5, -2}, {0, 6, -3},
            {0, 6, -2}, {0, 7, -3}, {1, 0, 0}, {1, 1, -1}, {1, 1, 0}, {1, 2, -1}, {1, 3, -1}, {1, 4, -2}, {1, 4, -1}, {1, 5, -2}, {1, 6, -3},
            {1, 6, -2}, {1, 7, -3}};
    private static final int[][] SPIRAL_LEFT_CELLS = {{-1, 3, -1}, {-1, 3, 0}, {0, 2, -1}, {0, 2, 0}, {0, 3, -1}, {0, 3, 0}, {1, 1, -1},
            {1, 1, 0}, {1, 2, -1}, {1, 2, 0}, {2, 0, -1}, {2, 0, 0}, {2, 1, -1}, {2, 1, 0}, {3, 0, -1}, {3, 0, 0}};
    private static final int[][] SPIRAL_RIGHT_CELLS = {{-2, 0, -1}, {-2, 0, 0}, {-1, 0, -1}, {-1, 0, 0}, {-1, 1, -1}, {-1, 1, 0}, {0, 1, -1},
            {0, 1, 0}, {0, 2, -1}, {0, 2, 0}, {1, 2, -1}, {1, 2, 0}, {1, 3, -1}, {1, 3, 0}, {2, 3, -1}, {2, 3, 0}};
    private static final int[][] JUMP_CELLS = {{-1, 0, 0}, {-1, 0, -1}, {0, 0, -1}, {1, 0, 0}, {1, 0, -1}, {2, 0, 0}, {2, 0, -1}};

    /** The blocks a piece takes up, relative to its anchor, in the piece's own frame (anchor excluded). */
    public static List<int[]> cells(Shape shape) {
        switch (shape) {
            case DROP -> {
                return List.of(DROP_CELLS);
            }
            case SPIRAL_LEFT -> {
                return List.of(SPIRAL_LEFT_CELLS);
            }
            case SPIRAL_RIGHT -> {
                return List.of(SPIRAL_RIGHT_CELLS);
            }
            case JUMP -> {
                return List.of(JUMP_CELLS);
            }
            default -> {
            }
        }
        List<int[]> cells = new ArrayList<>();
        for (int x = -1; x <= 2; x++) {
            for (int z = 0; z >= -3; z--) {
                int y = shape == Shape.SLOPE ? -z : 0;
                if (x != 0 || z != 0) {
                    cells.add(new int[]{x, y, z});
                }
            }
        }
        return cells;
    }

    /** Where a piece's anchor is, given one of its blocks (the anchor itself or a part). */
    @Nullable
    public static BlockPos anchorOf(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SleeRailBlock) {
            return pos;
        }
        if (state.getBlock() instanceof SleeRailPartBlock) {
            BlockPos anchor = SleeRailPartBlock.anchor(state, pos);
            return level.getBlockState(anchor).getBlock() instanceof SleeRailBlock ? anchor : null;
        }
        return null;
    }

    /** A piece of rail in the world. */
    public record Piece(BlockPos anchor, Direction facing, Shape shape) {
        public Point at(double t) {
            return world(anchor, facing, shape, t);
        }

        @Nullable
        public static Piece of(BlockGetter level, BlockPos anyBlock) {
            BlockPos anchor = anchorOf(level, anyBlock);
            if (anchor == null) {
                return null;
            }
            BlockState state = level.getBlockState(anchor);
            return new Piece(anchor, state.getValue(SleeRailBlock.FACING), state.getValue(SleeRailBlock.SHAPE));
        }

        /** The t (0..1) of the point on this piece closest to a position. */
        public double closest(Vec3 pos) {
            double best = 0, bestDist = Double.MAX_VALUE;
            for (int i = 0; i <= 40; i++) {
                double t = i / 40.0;
                double d = at(t).pos().distanceToSqr(pos);
                if (d < bestDist) {
                    bestDist = d;
                    best = t;
                }
            }
            return best;
        }
    }

    /**
     * The piece that continues the track at a piece's end (forward = leaving at t=1, else at t=0). Returns the next
     * piece and whether the sled goes through it forwards; null at the end of the line.
     */
    @Nullable
    public static Next next(BlockGetter level, Piece piece, boolean forward) {
        Point end = piece.at(forward ? 1 : 0);
        Vec3 heading = forward ? end.heading() : end.heading().reverse();
        Vec3 flat = new Vec3(heading.x, 0, heading.z).normalize();
        Vec3 probe = end.pos().add(flat.scale(0.5));
        for (int dy : new int[]{0, -1, 1}) {
            BlockPos cell = BlockPos.containing(probe.x, end.pos().y - RIDE_HEIGHT + 0.01 + dy, probe.z);
            Piece other = Piece.of(level, cell);
            if (other == null || other.anchor().equals(piece.anchor())) {
                continue;
            }
            if (other.at(0).pos().distanceToSqr(end.pos()) < 0.2) {
                return new Next(other, true);
            }
            if (other.at(1).pos().distanceToSqr(end.pos()) < 0.2) {
                return new Next(other, false);
            }
        }
        // riding backwards off a landing: the jump's ramp is 12 blocks back, across the gap
        Direction facing = Direction.getNearest(-flat.x, 0, -flat.z);
        BlockPos ramp = blockAt(end.pos().subtract(0, RIDE_HEIGHT, 0).subtract(rotate(raw(Shape.JUMP, 1).pos(), facing)));
        Piece jump = Piece.of(level, ramp);
        if (jump != null && jump.shape() == Shape.JUMP && !jump.anchor().equals(piece.anchor()) && jump.at(1).pos().distanceToSqr(end.pos()) < 0.2) {
            return new Next(jump, false);
        }
        return null;
    }

    public record Next(Piece piece, boolean forward) {
    }

    /**
     * A new piece that continues the track from a point (on the rail, without ride height) travelling in a
     * direction. A "down" piece is turned around and ridden backwards, so that its top end meets the point.
     */
    public static Placement attach(Vec3 end, Direction travel, Shape shape, boolean down) {
        if (down && shape.canGoDown()) {
            Point top = raw(shape, 1); // leaves its top going north (slopes) or south (corkscrews), in its own frame
            Direction facing = top.heading().z < 0 ? travel.getOpposite() : travel;
            return new Placement(blockAt(end.subtract(rotate(top.pos(), facing))), facing, shape);
        }
        return new Placement(blockAt(end.subtract(rotate(raw(shape, 0).pos(), travel))), travel, shape);
    }

    /** A new piece at a free spot (not connected to anything): the anchor is the block itself (slopes go up). */
    public static Placement fresh(BlockPos pos, Direction travel, Shape shape) {
        return new Placement(pos, travel, shape);
    }

    /** The (loose) end of a piece nearest to a spot: the rail point there and the direction the track leaves in. */
    public static Placement attachTo(Piece piece, Vec3 near, Shape shape, boolean down) {
        boolean far = piece.at(1).pos().distanceToSqr(near) < piece.at(0).pos().distanceToSqr(near);
        Point end = piece.at(far ? 1 : 0);
        Vec3 h = far ? end.heading() : end.heading().reverse();
        return attach(end.pos().subtract(0, RIDE_HEIGHT, 0), Direction.getNearest(h.x, 0, h.z), shape, down);
    }

    private static BlockPos blockAt(Vec3 bottomCentre) {
        return BlockPos.containing(bottomCentre.x, bottomCentre.y + 0.01, bottomCentre.z);
    }

    public record Placement(BlockPos anchor, Direction facing, Shape shape) {
        /** Every block the piece takes up (the anchor first). */
        public List<BlockPos> blocks() {
            List<BlockPos> list = new ArrayList<>();
            list.add(anchor);
            for (int[] c : cells(shape)) {
                list.add(anchor.offset(rotate(c[0], c[1], c[2], facing)));
            }
            return list;
        }
    }

    private SleePath() {
    }
}
