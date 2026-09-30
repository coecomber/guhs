package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * The route of a Vadsparade: a winding line over the ground, one point per block, where every step goes at most one
 * block up or down (so the guhs can walk it) and never crosses itself. {@link #plan} lays it out over any {@link Terrain}
 * (in the game: the Guhmension outdoors, away from buildings, water and cliffs).
 */
public final class ParadeRoute {
    /** Where a guh can walk: the feet height at a block column, or null (water, a building, not loaded, too steep...). */
    @FunctionalInterface
    public interface Terrain {
        @Nullable
        Integer groundY(int x, int z);
    }

    /** Every so many steps the route bends a little. */
    public static final int BEND_EVERY = 6;
    private static final double[] TRIES = {0, 0.35, -0.35, 0.7, -0.7, 1.05, -1.05, 1.4, -1.4};

    private final List<Vec3> points;

    public ParadeRoute(List<Vec3> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("a route needs at least two points");
        }
        this.points = List.copyOf(points);
    }

    public List<Vec3> points() {
        return points;
    }

    /** Length in blocks (the points are one block apart). */
    public double length() {
        return points.size() - 1;
    }

    /** The spot this far along the route (clamped to its ends). */
    public Vec3 at(double s) {
        double c = Mth.clamp(s, 0, length());
        int i = Math.min((int) Math.floor(c), points.size() - 2);
        return points.get(i).lerp(points.get(i + 1), c - i);
    }

    /** The way a guh faces this far along the route (Minecraft yaw), looking a little ahead so it turns smoothly. */
    public float yawAt(double s) {
        Vec3 from = at(s - 0.75), to = at(s + 0.75);
        if (from.distanceToSqr(to) < 1.0e-6) {
            from = at(length() - 1);
            to = at(length());
        }
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    /**
     * Lays out a route from (x, z) heading that way (radians: 0 = east, pi/2 = south), up to {@code wanted} blocks
     * long; null when it gets stuck before {@code min} blocks.
     */
    @Nullable
    public static ParadeRoute plan(Terrain terrain, RandomSource random, double x, double z, double heading, int wanted, int min) {
        Integer y0 = terrain.groundY(Mth.floor(x), Mth.floor(z));
        if (y0 == null) {
            return null;
        }
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(Mth.floor(x) + 0.5, y0, Mth.floor(z) + 0.5));
        double cx = Mth.floor(x) + 0.5, cz = Mth.floor(z) + 0.5, h = heading;
        int cy = y0;
        for (int i = 1; i <= wanted; i++) {
            if (i % BEND_EVERY == 0) {
                h += (random.nextDouble() - 0.5) * 0.8;
            }
            boolean moved = false;
            for (double t : TRIES) {
                double nx = cx + Math.cos(h + t), nz = cz + Math.sin(h + t);
                Integer ny = terrain.groundY(Mth.floor(nx), Mth.floor(nz));
                if (ny == null || Math.abs(ny - cy) > 1 || crosses(points, nx, nz)) {
                    continue;
                }
                cx = nx;
                cz = nz;
                cy = ny;
                h += t;
                points.add(new Vec3(cx, cy, cz));
                moved = true;
                break;
            }
            if (!moved) {
                break;
            }
        }
        return points.size() - 1 >= min ? new ParadeRoute(points) : null;
    }

    /** Would this step come back too close to an earlier part of the route? */
    private static boolean crosses(List<Vec3> points, double x, double z) {
        for (int i = 0; i < points.size() - 6; i++) {
            Vec3 p = points.get(i);
            if ((p.x - x) * (p.x - x) + (p.z - z) * (p.z - z) < 9) {
                return true;
            }
        }
        return false;
    }
}
