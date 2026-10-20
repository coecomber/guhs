package nl.juiced.guhs.feature.safari.rit;

import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A path of the safari rides (S3): world points with their running length, so a ride can stand "this far along it". A
 * {@link #rond} path is a loop (the truck route): distances wrap around.
 */
public final class Pad {
    private final List<Vec3> punten;
    private final double[] lengtes;
    public final boolean rond;

    public Pad(List<Vec3> punten, boolean rond) {
        this.punten = List.copyOf(punten);
        this.rond = rond;
        int n = this.punten.size() + (rond ? 1 : 0);
        this.lengtes = new double[Math.max(1, n)];
        for (int i = 1; i < n; i++) {
            lengtes[i] = lengtes[i - 1] + punt(i).distanceTo(punt(i - 1));
        }
    }

    private Vec3 punt(int i) {
        return punten.get(i % punten.size());
    }

    public int aantal() {
        return punten.size();
    }

    public Vec3 get(int i) {
        return punten.get(Math.floorMod(i, punten.size()));
    }

    public double lengte() {
        return lengtes[lengtes.length - 1];
    }

    /** How far along the path point i lies. */
    public double afstandVan(int i) {
        return lengtes[Mth.clamp(i, 0, lengtes.length - 1)];
    }

    private double norm(double d) {
        double l = lengte();
        if (l <= 0) {
            return 0;
        }
        return rond ? ((d % l) + l) % l : Mth.clamp(d, 0, l);
    }

    /** The point this far along the path. */
    public Vec3 op(double d) {
        if (punten.isEmpty()) {
            return Vec3.ZERO;
        }
        d = norm(d);
        int lo = 0, hi = lengtes.length - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) >>> 1;
            if (lengtes[mid] <= d) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double deel = lengtes[hi] - lengtes[lo];
        return punt(lo).lerp(punt(hi), deel <= 1e-6 ? 0 : (d - lengtes[lo]) / deel);
    }

    /** The yaw of the path this far along it (looking a little ahead, so the bends are soft). */
    public float richting(double d, float anders) {
        Vec3 a = op(d - 0.75), b = op(d + 2.5);
        if (!rond) {
            a = op(Math.max(0, d - 0.75));
            b = op(Math.min(lengte(), d + 2.5));
        }
        Vec3 r = b.subtract(a);
        return r.horizontalDistanceSqr() < 1e-6 ? anders : (float) Math.toDegrees(Math.atan2(-r.x, r.z));
    }

    /** The index of the path point closest to p. */
    public int dichtst(Vec3 p) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < punten.size(); i++) {
            double d = punten.get(i).distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }
}
