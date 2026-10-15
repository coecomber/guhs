package nl.juiced.guhs.feature.guhrio;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One lane of a Super Guhrio level, in the world: a line through the middle of blocks, made of one or more axis-aligned
 * pieces (the corner points {@link #punten}; every piece runs along x or along z, the height of the points is not used).
 * A player in the level is locked on this line: only the place along it ({@code s}, in blocks from the first point) and
 * the height are free. The camera looks at the lane from one side: on the right hand of the way the lane runs
 * ({@link #cameraRechts}, the normal case: further along the lane is to the right of the screen) or on the left.
 * <p>
 * Pure sums, the same on the server and in the player's game: where a spot is on the lane ({@link #plek}), where a step
 * ends ({@link #stap}: around a corner onto the next piece, never past the two ends, never off the side), which way the
 * camera looks ({@link #cameraYaw}: it swings around a corner), which blocks belong to the lane ({@link #bevat}).
 */
public final class Baan {
    /** How far past the middle of the first and last block you can stand (up to the edge of that block). */
    public static final double RAND = 0.2;
    /** How far before and after a corner the camera swings. */
    public static final double HOEK = 4.0;

    public final String id;
    private final List<BlockPos> punten;
    /** The camera stands on the right hand of the lane's direction (further = right on screen). */
    public final boolean cameraRechts;
    /** How far the camera stands from the lane, and how many blocks it shows above and below its middle. */
    public final double afstand, hoogte;
    /** Below this height you fell out of the level; the level's ceiling (the lane's blocks are between them). */
    public final int onder, boven;
    private final Direction[] richting;
    private final double[] begin;
    private final double lengte;

    public Baan(String id, List<BlockPos> punten, boolean cameraRechts, double afstand, double hoogte, int onder, int boven) {
        if (punten.size() < 2) {
            throw new IllegalArgumentException("guhrio baan " + id + ": at least two points");
        }
        this.id = id;
        this.punten = List.copyOf(punten);
        this.cameraRechts = cameraRechts;
        this.afstand = afstand;
        this.hoogte = hoogte;
        this.onder = onder;
        this.boven = boven;
        int n = punten.size() - 1;
        this.richting = new Direction[n];
        this.begin = new double[n + 1];
        for (int i = 0; i < n; i++) {
            BlockPos a = punten.get(i), b = punten.get(i + 1);
            int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
            if ((dx == 0) == (dz == 0)) {
                throw new IllegalArgumentException("guhrio baan " + id + ": piece " + i + " must run along x or along z (" + a + " -> " + b + ")");
            }
            richting[i] = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
            begin[i + 1] = begin[i] + Math.abs(dx) + Math.abs(dz);
        }
        this.lengte = begin[n];
    }

    public List<BlockPos> punten() {
        return punten;
    }

    /** How many straight pieces. */
    public int stukken() {
        return richting.length;
    }

    /** The whole length in blocks (from the middle of the first block to the middle of the last). */
    public double lengte() {
        return lengte;
    }

    /** Which way piece {@code stuk} runs. */
    public Direction richting(int stuk) {
        return richting[Mth.clamp(stuk, 0, richting.length - 1)];
    }

    /** Where piece {@code stuk} starts, in blocks along the lane. */
    public double begin(int stuk) {
        return begin[Mth.clamp(stuk, 0, richting.length)];
    }

    public double lengte(int stuk) {
        return begin[stuk + 1] - begin[stuk];
    }

    /** The piece that {@code s} is on (a corner belongs to the piece before it). */
    public int stukBij(double s) {
        for (int i = 0; i < richting.length - 1; i++) {
            if (s <= begin[i + 1]) {
                return i;
            }
        }
        return richting.length - 1;
    }

    /** Further along the lane is this way on the screen: +1 right, -1 left. */
    public int schermRechts() {
        return cameraRechts ? 1 : -1;
    }

    /** From the lane to the camera, on piece {@code stuk} (a flat unit vector). */
    public Vec3 naarCamera(int stuk) {
        Direction d = richting(stuk);
        Vec3 rechts = new Vec3(-d.getStepZ(), 0, d.getStepX());          // (east: south)
        return cameraRechts ? rechts : rechts.scale(-1);
    }

    /** The spot on the lane at {@code s} (kept between the two ends) and height {@code y}. */
    public Vec3 punt(double s, double y) {
        s = Mth.clamp(s, -RAND, lengte + RAND);
        int i = stukBij(s);
        BlockPos a = punten.get(i);
        Direction d = richting[i];
        double langs = s - begin[i];
        return new Vec3(a.getX() + 0.5 + d.getStepX() * langs, y, a.getZ() + 0.5 + d.getStepZ() * langs);
    }

    /** A spot seen from the lane: on which piece, how far along the whole lane, and how far off the line it is. */
    public record Plek(int stuk, double s, double naast) {
    }

    /** The nearest spot on the lane to (x, z). */
    public Plek plek(double x, double z) {
        Plek beste = null;
        for (int i = 0; i < richting.length; i++) {
            BlockPos a = punten.get(i);
            Direction d = richting[i];
            double rx = x - (a.getX() + 0.5), rz = z - (a.getZ() + 0.5);
            double lang = lengte(i);
            double langs = Mth.clamp(rx * d.getStepX() + rz * d.getStepZ(), i == 0 ? -RAND : 0, i == richting.length - 1 ? lang + RAND : lang);
            double px = d.getStepX() * langs, pz = d.getStepZ() * langs;
            double naast = Math.sqrt((rx - px) * (rx - px) + (rz - pz) * (rz - pz));
            if (beste == null || naast < beste.naast - 1e-9) {
                beste = new Plek(i, begin[i] + langs, naast);
            }
        }
        return beste;
    }

    /** Where a step ends: the piece you are on now, how far along the lane, the spot itself, and whether an end stopped you. */
    public record Stap(int stuk, double s, double x, double z, boolean eind) {
    }

    /**
     * You were on piece {@code stuk} and moved to (x, z): where are you really? On the line of that piece; past its end
     * you go on around the corner on the next piece (as far as you overshot), and the two ends of the lane stop you.
     */
    public Stap stap(int stuk, double x, double z) {
        stuk = Mth.clamp(stuk, 0, richting.length - 1);
        BlockPos a = punten.get(stuk);
        Direction d = richting[stuk];
        double langs = (x - (a.getX() + 0.5)) * d.getStepX() + (z - (a.getZ() + 0.5)) * d.getStepZ();
        while (langs > lengte(stuk) && stuk < richting.length - 1) {
            langs -= lengte(stuk);
            stuk++;
        }
        while (langs < 0 && stuk > 0) {
            stuk--;
            langs += lengte(stuk);
        }
        boolean eind = false;
        double min = stuk == 0 ? -RAND : 0, max = lengte(stuk) + (stuk == richting.length - 1 ? RAND : 0);
        if (langs < min || langs > max) {
            langs = Mth.clamp(langs, min, max);
            eind = true;
        }
        Vec3 p = punt(begin[stuk] + langs, 0);
        // (punt() gives a corner to the piece before it: the same spot)
        return new Stap(stuk, begin[stuk] + langs, p.x, p.z, eind);
    }

    /**
     * Standing on a corner and pushing on ({@code teken} +1 further along the lane, -1 back): the piece you walk on next.
     */
    public int kies(int stuk, double s, int teken) {
        stuk = Mth.clamp(stuk, 0, richting.length - 1);
        if (teken > 0 && stuk < richting.length - 1 && s >= begin[stuk + 1] - 1e-4) {
            return stuk + 1;
        }
        if (teken < 0 && stuk > 0 && s <= begin[stuk] + 1e-4) {
            return stuk - 1;
        }
        return stuk;
    }

    /** The camera's yaw (degrees) looking at the lane on piece {@code stuk}. */
    public float yawOp(int stuk) {
        Vec3 c = naarCamera(stuk);
        return (float) Math.toDegrees(Math.atan2(c.x, -c.z));                // (looking the other way than naarCamera)
    }

    /** The camera's yaw at {@code s}: straight at the lane, swinging smoothly from one piece to the next around a corner. */
    public float cameraYaw(double s) {
        int i = stukBij(s);
        float yaw = yawOp(i);
        // the corner after this piece, the corner before it
        if (i < richting.length - 1 && begin[i + 1] - s < HOEK) {
            double t = 0.5 - (begin[i + 1] - s) / (2 * HOEK);                  // 0 .. 0.5
            return yaw + Mth.wrapDegrees(yawOp(i + 1) - yaw) * glad(t);
        }
        if (i > 0 && s - begin[i] < HOEK) {
            double t = 0.5 + (s - begin[i]) / (2 * HOEK);                      // 0.5 .. 1
            float vorig = yawOp(i - 1);
            return vorig + Mth.wrapDegrees(yaw - vorig) * glad(t);
        }
        return yaw;
    }

    private static float glad(double t) {
        t = Mth.clamp(t, 0, 1);
        return (float) (t * t * (3 - 2 * t));
    }

    /** Is this block one of the lane's own blocks (on the line, between the floor and the ceiling)? */
    public boolean bevat(BlockPos pos) {
        if (pos.getY() < onder || pos.getY() > boven) {
            return false;
        }
        for (int i = 0; i < richting.length; i++) {
            BlockPos a = punten.get(i), b = punten.get(i + 1);
            if (pos.getX() >= Math.min(a.getX(), b.getX()) && pos.getX() <= Math.max(a.getX(), b.getX())
                    && pos.getZ() >= Math.min(a.getZ(), b.getZ()) && pos.getZ() <= Math.max(a.getZ(), b.getZ())) {
                return true;
            }
        }
        return false;
    }

    /** Every column (x, z) of the lane, in order from the first point to the last (corners once). */
    public List<BlockPos> kolommen() {
        java.util.ArrayList<BlockPos> uit = new java.util.ArrayList<>();
        for (int i = 0; i < richting.length; i++) {
            BlockPos a = punten.get(i);
            Direction d = richting[i];
            int n = (int) Math.round(lengte(i));
            for (int k = i == 0 ? 0 : 1; k <= n; k++) {
                uit.add(new BlockPos(a.getX() + d.getStepX() * k, 0, a.getZ() + d.getStepZ() * k));
            }
        }
        return uit;
    }
}
