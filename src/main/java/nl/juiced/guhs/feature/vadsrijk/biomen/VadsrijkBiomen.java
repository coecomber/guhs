package nl.juiced.guhs.feature.vadsrijk.biomen;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Lagen;

/**
 * Fix round 2 "biomen": which Vadsrijk biome ({@code guhs:vadsrijk_<id>}, "Het Vadsrijk - &lt;plek&gt;" in F3 and JourneyMap) lies
 * at a block x/z. Pure and deterministic (only the fixed area boxes, never the runtime list that dungeons/arenas are pushed into):
 * <ol>
 *   <li>z3-systemen: the zone-3 LAYERS ({@link Z3Lagen#GEBIEDEN}): these need the height, so the biome source is 3D there (crown,
 *   branches, trunk, ground and roots of the world tree each have their own biome; the zone-3 catch-all = De Schemerwortel);</li>
 *   <li>the zone-2 areas ({@link Z2WereldPlekken#GEBIEDEN}, first match wins; their ring = De Splinterpieken);</li>
 *   <li>the Kloof ravine (the same polyline as the fog of wereld/sfeer GebiedMist: it wins over the box it cuts through);</li>
 *   <li>the zone-1 areas of {@link Gebieden} in their order (their catch-all ring and the proeftuin = Tussenland);</li>
 *   <li>everything else: De Zee.</li>
 * </ol>
 */
public final class VadsrijkBiomen {
    /** Biome ids (without "vadsrijk_"); the index is what {@link #index} returns. Keep in sync with tools/features/vadsrijk_biomen.py. */
    private static final List<String> BASIS = List.of("knuffelkamp", "zwamzoet", "haven", "klodderweide", "sporenwoud", "druipgrotten", "kloof",
            "zwamtempel", "tussenland", "zee", "kandijkom", "galmhellingen", "knuffelmeer", "splinterkloof", "spiegelburcht", "splinterpieken",
            // z3-systemen: zone 3 "De Schemerwortel", one biome per layer
            "honingtak", "kruinzee", "takkenbanen", "schorsstad", "wortelvelden", "wortelduister", "zeppelinwerf", "hart", "schemerwortel",
            // T-Z4WERELD: zone 4 "De Lampionkust", one biome per island (Z4WereldRegistry.biome)
            "lampionhaven", "knuffeltij", "strandfeest", "bloemeneiland", "koraalbaai", "vliegereiland", "vuurtoreneiland", "pierkop",
            "mysterie", "schildpadeiland", "wrak", "lampionkust",
            // T-Z5WERELD: zone 5 "Hoogvads" (above zone 4, by height: Z5WereldRegistry.biome)
            "hoogvads_paleis", "hoogvads_universiteit", "hoogvads_academie", "hoogvads_markt", "hoogvads_kathedraal", "hoogvads_luchthaven",
            "hoogvads_staartpark", "hoogvads_ooggaarde", "hoogvads", "moeder_wolkwal",
            // U-GROTTEN: the five atmosphere caves (grotten.Grotten.biome, by height: checked first)
            "grot_zwamgrot", "grot_kristalgrot", "grot_wortelgrot", "grot_koraalgrot", "grot_wolkengrot");
    /** U-OVERGANG: every id incl. the sea-band biomes (tools/features/vadsrijk_u_overgang.py band_ids(), same order). */
    public static final List<String> IDS = metZeeband(BASIS);
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

    /** A zone-3 area id (z3_...) to its biome id; unknown ids fall back to De Schemerwortel. */
    static String zone3Biome(String gebied) {
        String id = gebied.startsWith("z3_") ? gebied.substring(3) : gebied;
        if (id.startsWith("honingtak")) {
            return "honingtak";
        }
        if (id.equals("stamspiraal")) {
            return "schorsstad";
        }
        return IDS.contains(id) && IDS.indexOf(id) >= IDS.indexOf("honingtak") ? id : "schemerwortel";
    }

    /** z3-systemen: the biome index at block x/y/z (zone 3 is layered; elsewhere the height does not matter). */
    public static int index(int x, int y, int z) {
        String grot = nl.juiced.guhs.feature.vadsrijk.grotten.Grotten.biome(x, y, z);   // U-GROTTEN: inside a cave = its own biome
        if (grot != null) {
            return IDS.indexOf(grot);
        }
        String z5 = nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldRegistry.biome(x, y, z);   // T-Z5WERELD: Hoogvads, above zone 4
        if (z5 != null) {   // (T-INT order: zone 5 by height first, then zone 4, then zone 3)
            return IDS.indexOf(z5);
        }
        int zee = zeeband(x, y, z);   // U-OVERGANG: the open sea between the zones = a colour band, not area boxes
        if (zee != GEEN_BAND) {
            return zee;
        }
        if (Z3Lagen.inZone3(x, z) && !nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.inZone4(x, z)) {   // T-Z4WERELD: zone 4 wins
            for (Gebieden.Gebied g : Z3Lagen.GEBIEDEN) {
                if (g.bevat(x + 0.5, y, z + 0.5)) {
                    return IDS.indexOf(zone3Biome(g.id()));
                }
            }
            return IDS.indexOf("schemerwortel");
        }
        return indexVak(x, z);
    }

    /** The biome id at block x/y/z. */
    public static String id(int x, int y, int z) {
        return IDS.get(index(x, y, z));
    }

    /** The biome index ({@link #IDS}) at block x/z (flat: zone 3 gives its ground layer). */
    public static int index(int x, int z) {
        int zee = zeeband(x, 64, z);   // U-OVERGANG
        if (zee != GEEN_BAND) {
            return zee;
        }
        return indexVak(x, z);
    }

    /** The old flat lookup by area boxes (zone 4, zone 3 ground, zone 2, the Kloof, zone 1, else De Zee). */
    private static int indexVak(int x, int z) {
        String z4 = nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldRegistry.biome(x, z);   // T-Z4WERELD (before zone 3: its margin reaches x 1991)
        if (z4 != null) {
            return IDS.indexOf(z4);
        }
        if (Z3Lagen.inZone3(x, z)) {
            return index(x, 64, z);
        }
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
        return ankerBinnen(x, z);   // U-OVERGANG (was: De Zee)
    }

    /** U-OVERGANG: a column inside a zone's coast shape that no area claims gets that zone's anchor (else De Zee). */
    private static int ankerBinnen(int x, int z) {
        if (nl.juiced.guhs.feature.vadsrijk.indeling.ZeeVulling.inZee(x, z)) {
            for (Kust k : KUSTEN) {
                if (k.buiten(x + 0.5, 64, z + 0.5) <= 0) {
                    return IDS.indexOf(ANKER[k.zone()]);
                }
            }
        }
        return ZEE;
    }

    /** The biome id ("zwamzoet", "zee", ...) at block x/z. */
    public static String id(int x, int z) {
        return IDS.get(index(x, z));
    }

    // ================================================================================================ U-OVERGANG: the sea band
    /**
     * The coast shapes of the zones (superellipse p 4, or a circle): inside one, the zone's own area biomes apply; outside all
     * of them (the open sea, also inside a zone's tile box) a sea-band biome between the two nearest zones. The zone's catch-all
     * biome ({@link #ANKER}) is the colour at its own coast, so nothing jumps where the band starts.
     */
    private record Kust(int zone, double cx, double cz, double rx, double rz, boolean cirkel, int vanafY) {
        /** Blocks outside the shape (<= 0 inside). */
        double buiten(double x, double y, double z) {
            if (y < vanafY) {
                return Double.MAX_VALUE;
            }
            if (cirkel) {
                return Math.hypot(x - cx, z - cz) - rx;
            }
            double a = Math.abs(x - cx) / rx, b = Math.abs(z - cz) / rz;
            return (Math.pow(a * a * a * a + b * b * b * b, 0.25) - 1) * Math.min(rx, rz);
        }
    }

    /** Zone order of the band (z1, z2, z3, z4) = vadsrijk_u_overgang.ZONES. */
    private static final String[] ANKER = {"tussenland", "splinterpieken", "schemerwortel", "lampionkust"};
    public static final int ZEEBAND_STAPPEN = 8;
    static final int GEEN_BAND = -1;
    private static final Kust[] KUSTEN = {
            new Kust(0, 0, -20, 715, 660, false, -64),          // zone 1 (the organic coast stays inside ~640)
            new Kust(0, -560, 600, 120, 110, false, -64),       // its south-west peninsula
            new Kust(0, 520, 720, 90, 90, false, -64),          // the turtle island
            new Kust(0, -373, 996, 205, 200, false, -64),       // Knuffelkamp (same colours as zone 1)
            new Kust(1, 704, -1121, 305, 340, false, -64),      // zone 2 (inside its tile box: x <= 1009, z >= -1461)
            new Kust(2, 1404, -576, 205, 205, true, -64),       // zone 3: the island (its sea starts at 205 from the trunk)
            new Kust(2, 1404, -576, 275, 275, true, 100),       // ... and the crown / branch tips higher up
            new Kust(2, 1655, -827, 70, 70, true, -64),         // the Kaaszeppelinwerf
            new Kust(3, 2184, -491, 426, 270, false, -64)};     // zone 4 (the islands; same as the barrier's free area)

    static List<String> metZeeband(List<String> basis) {
        List<String> l = new ArrayList<>(basis);
        for (int a = 0; a < 4; a++) {          // (ANKER is not initialised yet when IDS is built)
            for (int b = a + 1; b < 4; b++) {
                for (int k = 1; k < ZEEBAND_STAPPEN; k++) {
                    l.add("zeeband_z" + (a + 1) + "_z" + (b + 1) + "_" + k);
                }
            }
        }
        return List.copyOf(l);
    }

    /** The biome index of a sea-band biome between zone a and b (0..3) with weight k/8 of zone b (k 0 = a's anchor, 8 = b's). */
    static int band(int a, int b, int k) {
        if (a > b) {
            return band(b, a, ZEEBAND_STAPPEN - k);
        }
        if (k <= 0) {
            return IDS.indexOf(ANKER[a]);
        }
        if (k >= ZEEBAND_STAPPEN) {
            return IDS.indexOf(ANKER[b]);
        }
        return IDS.indexOf("zeeband_z" + (a + 1) + "_z" + (b + 1) + "_" + k);
    }

    /**
     * The sea-band biome at x/y/z, or {@link #GEEN_BAND} when x/z lies inside a zone's coast shape (then the area boxes decide)
     * or outside the island sea. Between the two nearest zones A and B the weight of B is dA / (dA + dB), in eighths.
     */
    static int zeeband(int x, int y, int z) {
        if (!nl.juiced.guhs.feature.vadsrijk.indeling.ZeeVulling.inZee(x, z)) {
            return GEEN_BAND;
        }
        double[] d = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        for (Kust k : KUSTEN) {
            double b = k.buiten(x + 0.5, y, z + 0.5);
            if (b <= 0) {
                return GEEN_BAND;
            }
            d[k.zone()] = Math.min(d[k.zone()], b);
        }
        int a = 0;
        for (int i = 1; i < 4; i++) {
            if (d[i] < d[a]) {
                a = i;
            }
        }
        int b = a == 0 ? 1 : 0;
        for (int i = 0; i < 4; i++) {
            if (i != a && d[i] < d[b]) {
                b = i;
            }
        }
        double t = d[a] / (d[a] + d[b]);                     // 0 at A's coast .. 0.5 halfway
        int k = (int) Math.floor(t * ZEEBAND_STAPPEN + 0.3);  // (a little pure anchor colour along every coast)
        return band(a, b, k);
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
