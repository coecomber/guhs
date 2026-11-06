package nl.juiced.guhs.feature.vadsrijk.ruiters;

import java.util.List;

import net.minecraft.world.phys.Vec3;

/**
 * U-RUITERS: one route of the riding guhs: a polyline of stand points (planned on the real map tiles by
 * tools/vadsrijk/ruiters/routes.py, so every straight piece between two points is walkable and y is linear in between) and how
 * the riders use it. Pure data + math, no world access (gametested).
 * <p>
 * Where a rider is is a PURE FUNCTION of the game time ({@link #stand}): there is no state to save, a rider that comes back into
 * view is exactly where it should be, and two riders of one route never bunch up (they are spread over the period). Speed per
 * piece: the flat speed, a bit slower on a slope (stairs: a hopping trot).
 * <ul>
 *   <li>{@link Modus#HEENWEER}: forward, a rest at the end, back, a rest at the start;</li>
 *   <li>{@link Modus#RONDJE}: a closed loop (last point = first point), round and round;</li>
 *   <li>{@link Modus#STAL}: every rider k stands on point k (waiting at the stable / being groomed).</li>
 * </ul>
 */
public final class RuiterRoute {
    public enum Modus {
        HEENWEER, RONDJE, STAL
    }

    /** Where a rider is at a moment: position, the way it faces (xz unit vector), whether it moves. */
    public record Stand(Vec3 pos, Vec3 richting, boolean beweegt) {
        public float yaw() {
            return (float) Math.toDegrees(Math.atan2(-richting.x, richting.z));
        }
    }

    /** Rest at each end of a back-and-forth route (ticks). */
    public static final int RUST = 100;

    public final String id;
    public final int zone;
    public final Modus modus;
    public final int ruiters;
    /** Speed on the flat (blocks per tick). */
    public final double snelheid;
    private final List<Vec3> punten;
    /** Cumulative time (ticks) to reach point i, riding forward. */
    private final double[] tijd;

    public RuiterRoute(String id, int zone, Modus modus, int ruiters, double snelheid, List<Vec3> punten) {
        if (punten.isEmpty() || (modus != Modus.STAL && punten.size() < 2)) {
            throw new IllegalArgumentException("route " + id + ": too few points");
        }
        this.id = id;
        this.zone = zone;
        this.modus = modus;
        this.ruiters = modus == Modus.STAL ? Math.min(ruiters, punten.size()) : ruiters;
        this.snelheid = snelheid;
        this.punten = List.copyOf(punten);
        this.tijd = new double[punten.size()];
        for (int i = 1; i < punten.size(); i++) {
            Vec3 a = punten.get(i - 1), b = punten.get(i);
            tijd[i] = tijd[i - 1] + a.distanceTo(b) / snelheid(a, b);
        }
    }

    /** The speed on the piece a -> b: full on the flat, 80 % on a gentle slope, 65 % on stairs. */
    double snelheid(Vec3 a, Vec3 b) {
        double hor = Math.max(0.3, Math.hypot(b.x - a.x, b.z - a.z));
        double helling = Math.abs(b.y - a.y) / hor;
        return snelheid * (helling > 0.3 ? 0.65 : helling > 0.1 ? 0.8 : 1.0);
    }

    public List<Vec3> punten() {
        return punten;
    }

    /** Ticks for one ride from the first to the last point. */
    public double rit() {
        return tijd[tijd.length - 1];
    }

    /** Length in blocks (3D). */
    public double lengte() {
        double l = 0;
        for (int i = 1; i < punten.size(); i++) {
            l += punten.get(i - 1).distanceTo(punten.get(i));
        }
        return l;
    }

    /** One full cycle in ticks (back and forth with the rests, or one loop). */
    public long periode() {
        return switch (modus) {
            case HEENWEER -> 2 * (long) Math.ceil(rit()) + 2L * RUST;
            case RONDJE -> Math.max(1, (long) Math.ceil(rit()));
            case STAL -> 1;
        };
    }

    /** A per-route offset, so the routes don't all start together. */
    private long verschuiving() {
        return Math.floorMod(id.hashCode() * 2654435761L, Math.max(1, periode()));
    }

    /** Where rider k is at game time t. */
    public Stand stand(long t, int k) {
        if (modus == Modus.STAL) {
            Vec3 p = punten.get(Math.floorMod(k, punten.size()));
            double hoek = (id.hashCode() * 31 + k * 97) % 360;   // a fixed way of looking
            return new Stand(p, new Vec3(-Math.sin(Math.toRadians(hoek)), 0, Math.cos(Math.toRadians(hoek))), false);
        }
        long per = periode();
        double f = Math.floorMod(t + verschuiving() + (long) Math.floor((double) per * k / Math.max(1, ruiters)), per);
        if (modus == Modus.RONDJE) {
            return op(f, true);
        }
        long r = (long) Math.ceil(rit());
        if (f < RUST) {
            return stil(op(0, true));
        }
        f -= RUST;
        if (f < r) {
            return op(Math.min(f, rit()), true);
        }
        f -= r;
        if (f < RUST) {
            Stand e = op(rit(), true);
            return new Stand(e.pos(), e.richting().scale(-1), false);   // rests at the end, turned round
        }
        f -= RUST;
        return op(Math.max(0, rit() - f), false);
    }

    private static Stand stil(Stand s) {
        return new Stand(s.pos(), s.richting(), false);
    }

    /** The point after time f riding forward (vooruit = facing forward, else facing back). */
    Stand op(double f, boolean vooruit) {
        int i = segment(f);
        Vec3 a = punten.get(i), b = punten.get(i + 1);
        double dt = tijd[i + 1] - tijd[i];
        double u = dt <= 0 ? 0 : Math.max(0, Math.min(1, (f - tijd[i]) / dt));
        Vec3 d = new Vec3(b.x - a.x, 0, b.z - a.z);
        d = d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
        return new Stand(a.lerp(b, u), vooruit ? d : d.scale(-1), true);
    }

    private int segment(double f) {
        int lo = 0, hi = tijd.length - 2;
        while (lo < hi) {
            int m = (lo + hi + 1) >>> 1;
            if (tijd[m] <= f) {
                lo = m;
            } else {
                hi = m - 1;
            }
        }
        return lo;
    }
}
