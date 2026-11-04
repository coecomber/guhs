package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * The road of the travelling caravan: Haven -> the east shore -> Zwamzoet (quay, past the plein) -> the Sporenwoud campfire, and
 * back. A polyline in world coordinates with its stops. Pure data + math (gametested).
 * <p>
 * Where the points come from: an A* walk over the real map tiles (surface blocks, steps of at most 1, 3 blocks wide, preferring
 * path blocks), from {@code haven_plein} along the map path "haven" (tools/vadsrijk/wereld/kaart.py PADEN; its last stretch ends
 * in the bay, so the cart keeps to the east shore and enters over the city quay), over the plein and the map path "woud" to
 * {@code kampvuur_sporenwoud_1}; simplified to these corners (tolerance 1.2 blocks). The y is the floor there (+1); the cart follows
 * the floor between corners itself ({@link Karavaan}).
 */
public final class KaravaanRoute {
    public record Stop(int index, String id, int ticks) {
    }

    private final List<Vec3> punten;
    private final List<Stop> stops;
    private final double[] tot;

    public KaravaanRoute(List<Vec3> punten, List<Stop> stops) {
        this.punten = List.copyOf(punten);
        this.stops = List.copyOf(stops);
        this.tot = new double[punten.size()];
        for (int i = 1; i < punten.size(); i++) {
            Vec3 a = punten.get(i - 1), b = punten.get(i);
            tot[i] = tot[i - 1] + Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
        }
    }

    private static final int[][] HOEKEN = {
            {90, 65, 466}, {93, 65, 434}, {93, 65, 412}, {95, 65, 410}, {94, 66, 394}, {97, 66, 380}, {96, 64, 332}, {99, 66, 302},
            {96, 64, 266}, {93, 64, 262}, {91, 64, 229}, {93, 64, 216}, {103, 64, 192}, {103, 64, 162}, {128, 64, 137}, {131, 64, 131},
            {126, 64, 113}, {123, 64, 109}, {123, 64, 87}, {109, 64, 84}, {36, 64, 84}, {19, 64, 67}, {8, 64, 66}, {1, 64, 70},
            {-3, 64, 70}, {-17, 64, 56}, {-17, 65, 53}, {-24, 65, 46}, {-32, 66, 46}, {-58, 65, 21}, {-141, 65, 20}, {-190, 65, 31},
            {-198, 65, 35}, {-220, 65, 14}, {-223, 65, 15}};   // (integration: fix wild moved kampvuur_sporenwoud_1 to (-228, 65, 17))

    /** The zone-1 road (stops: De Haven, Het Kustpad, Zwamzoet, Sporenwoud). */
    public static final KaravaanRoute ZONE1 = maak();

    private static KaravaanRoute maak() {
        List<Vec3> p = new ArrayList<>();
        for (int[] h : HOEKEN) {
            p.add(new Vec3(h[0] + 0.5, h[1], h[2] + 0.5));
        }
        return new KaravaanRoute(p, List.of(new Stop(0, "haven", 20 * 60), new Stop(7, "kustpad", 20 * 30), new Stop(23, "zwamzoet", 20 * 75),
                new Stop(p.size() - 1, "sporenwoud", 20 * 60)));
    }

    public double lengte() {
        return tot[tot.length - 1];
    }

    public List<Vec3> punten() {
        return punten;
    }

    public List<Stop> stops() {
        return stops;
    }

    /** Distance along the road of a corner. */
    public double afstandVan(int index) {
        return tot[index];
    }

    /** The point at distance s along the road (clamped), y interpolated between the corners. */
    public Vec3 op(double s) {
        s = Math.max(0, Math.min(lengte(), s));
        int i = segment(s);
        Vec3 a = punten.get(i), b = punten.get(i + 1);
        double len = tot[i + 1] - tot[i];
        double f = len <= 0 ? 0 : (s - tot[i]) / len;
        return a.lerp(b, f);
    }

    /** The way the road runs at s (unit xz vector in the direction of travel {@code richting} = +1 or -1). */
    public Vec3 richting(double s, int richting) {
        s = Math.max(0, Math.min(lengte(), s));
        int i = segment(s);
        Vec3 a = punten.get(i), b = punten.get(i + 1);
        Vec3 d = new Vec3(b.x - a.x, 0, b.z - a.z);
        d = d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
        return richting >= 0 ? d : d.scale(-1);
    }

    private int segment(double s) {
        for (int i = 0; i < tot.length - 2; i++) {
            if (s < tot[i + 1]) {
                return i;
            }
        }
        return tot.length - 2;
    }

    /** The next stop ahead of s in this direction (strictly ahead; null: none). */
    @Nullable
    public Stop volgendeStop(double s, int richting) {
        Stop best = null;
        for (Stop st : stops) {
            double d = (tot[st.index()] - s) * richting;
            if (d > 1e-6 && (best == null || d < (tot[best.index()] - s) * richting)) {
                best = st;
            }
        }
        return best;
    }

    /** The point of the road nearest to (x, z): its distance along the road. */
    public double dichtstbij(double x, double z) {
        double beste = 0, bd = Double.MAX_VALUE;
        for (int i = 0; i + 1 < punten.size(); i++) {
            Vec3 a = punten.get(i), b = punten.get(i + 1);
            double vx = b.x - a.x, vz = b.z - a.z, l2 = vx * vx + vz * vz;
            double f = l2 <= 0 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * vx + (z - a.z) * vz) / l2));
            double px = a.x + f * vx, pz = a.z + f * vz, d = (px - x) * (px - x) + (pz - z) * (pz - z);
            if (d < bd) {
                bd = d;
                beste = tot[i] + f * Math.sqrt(l2);
            }
        }
        return beste;
    }
}
