package nl.juiced.guhs.feature.vadsrijk.biomen;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;

/**
 * Fix round 2 "biomen": which Vadsrijk biome ({@code guhs:vadsrijk_<id>}, "Het Vadsrijk - &lt;plek&gt;" in F3 and JourneyMap) lies
 * at a block x/z. Pure and deterministic (only the fixed area boxes, never the runtime list that dungeons/arenas are pushed into):
 * <ol>
 *   <li>the zone-2 areas ({@link Z2WereldPlekken#GEBIEDEN}, first match wins; their ring = De Splinterpieken);</li>
 *   <li>the Kloof ravine (the same polyline as the fog of wereld/sfeer GebiedMist: it wins over the box it cuts through);</li>
 *   <li>the zone-1 areas of {@link Gebieden} in their order (their catch-all ring and the proeftuin = Tussenland);</li>
 *   <li>everything else: De Zee.</li>
 * </ol>
 */
public final class VadsrijkBiomen {
    /** Biome ids (without "vadsrijk_"); the index is what {@link #index} returns. Keep in sync with tools/features/vadsrijk_biomen.py. */
    public static final List<String> IDS = List.of("knuffelkamp", "zwamzoet", "haven", "klodderweide", "sporenwoud", "druipgrotten", "kloof",
            "zwamtempel", "tussenland", "zee", "kandijkom", "galmhellingen", "knuffelmeer", "splinterkloof", "spiegelburcht", "splinterpieken");
    public static final int ZEE = IDS.indexOf("zee"), KLOOF = IDS.indexOf("kloof"), TUSSENLAND = IDS.indexOf("tussenland");

    /** The Kloof ravine (tools/vadsrijk/wereld/kaart.py KLOOF and GebiedMist.KLOOF_LIJN, world coordinates). */
    private static final int[][] KLOOF_LIJN = {{40, -130}, {100, -170}, {170, -230}, {240, -300}, {280, -350}, {360, -260}, {450, -220}};
    /** Within this many blocks of the ravine line it is the Kloof (the fog is full at 18, gone at 38). */
    private static final int KLOOF_STRAAL = 24;
    private static final int KLOOF_MIN_X, KLOOF_MAX_X, KLOOF_MIN_Z, KLOOF_MAX_Z;

    private record Doos(int minX, int minZ, int maxX, int maxZ, int index) {
        boolean bevat(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private static final Doos[] ZONE2, ZONE1;

    static {
        int a = Integer.MAX_VALUE, b = Integer.MIN_VALUE, c = Integer.MAX_VALUE, d = Integer.MIN_VALUE;
        for (int[] p : KLOOF_LIJN) {
            a = Math.min(a, p[0]);
            b = Math.max(b, p[0]);
            c = Math.min(c, p[1]);
            d = Math.max(d, p[1]);
        }
        KLOOF_MIN_X = a - KLOOF_STRAAL;
        KLOOF_MAX_X = b + KLOOF_STRAAL;
        KLOOF_MIN_Z = c - KLOOF_STRAAL;
        KLOOF_MAX_Z = d + KLOOF_STRAAL;

        List<Doos> z2 = new ArrayList<>();
        for (Gebieden.Gebied g : Z2WereldPlekken.GEBIEDEN) {
            z2.add(new Doos(g.minX(), g.minZ(), g.maxX(), g.maxZ(), IDS.indexOf(zone2Biome(g.id()))));
        }
        ZONE2 = z2.toArray(Doos[]::new);
        List<Doos> z1 = new ArrayList<>();
        for (String id : List.of("zwamzoet", "haven", "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel", "knuffelkamp", "proeftuin", "ring")) {
            Gebieden.Gebied g = Gebieden.van(id);
            if (g != null) {
                int i = IDS.indexOf(id);
                z1.add(new Doos(g.minX(), g.minZ(), g.maxX(), g.maxZ(), i < 0 ? TUSSENLAND : i));
            }
        }
        ZONE1 = z1.toArray(Doos[]::new);
    }

    private VadsrijkBiomen() {
    }

    /** A zone-2 area id (z2_...) to its biome id; unknown (newly generated) areas fall back to De Splinterpieken. */
    static String zone2Biome(String gebied) {
        String id = gebied.startsWith("z2_") ? gebied.substring(3) : gebied;
        if (id.startsWith("kandijkom")) {
            return "kandijkom";
        }
        return IDS.contains(id) ? id : "splinterpieken";
    }

    /** The biome index ({@link #IDS}) at block x/z. */
    public static int index(int x, int z) {
        for (Doos d : ZONE2) {
            if (d.bevat(x, z)) {
                return d.index;
            }
        }
        if (inKloof(x, z)) {
            return KLOOF;
        }
        for (Doos d : ZONE1) {
            if (d.bevat(x, z)) {
                return d.index;
            }
        }
        return ZEE;
    }

    /** The biome id ("zwamzoet", "zee", ...) at block x/z. */
    public static String id(int x, int z) {
        return IDS.get(index(x, z));
    }

    private static boolean inKloof(int x, int z) {
        if (x < KLOOF_MIN_X || x > KLOOF_MAX_X || z < KLOOF_MIN_Z || z > KLOOF_MAX_Z) {
            return false;
        }
        long r2 = (long) KLOOF_STRAAL * KLOOF_STRAAL;
        for (int i = 0; i + 1 < KLOOF_LIJN.length; i++) {
            double ax = KLOOF_LIJN[i][0], az = KLOOF_LIJN[i][1], vx = KLOOF_LIJN[i + 1][0] - ax, vz = KLOOF_LIJN[i + 1][1] - az;
            double t = Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / (vx * vx + vz * vz)));
            double dx = x - (ax + t * vx), dz = z - (az + t * vz);
            if (dx * dx + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }
}
