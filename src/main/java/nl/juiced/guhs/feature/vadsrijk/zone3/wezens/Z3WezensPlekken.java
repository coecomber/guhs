package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Every coordinate of the zone-3 wezens slice (Z3_BRIEF: one class). Zone 3 "De Schemerwortel" = x +1600..+3000, z -700..700,
 * the trunk axis T at (+2300, 0). The world is the render design MIRRORED in x (POI_Z3.md v1): Honingtak on the WEST branch, the
 * W/NW/ZW riding branches reach EAST.
 * <p><b>Source:</b> z3-wereld's POI_Z3.md v1 / {@code zone3.wereld.Z3WereldPlekken} (mob zones, paths, campfires, arenas, towns).
 * The values below are a COPY of that v1 so this slice works on its own; when Z3WereldPlekken is on the classpath (after
 * integration) its live MOBZONES / PADEN / KAMPVUREN / plek(id) are read by reflection instead (no compile dependency), so a
 * later map change flows through without touching this class.
 * <p><b>Layers:</b> the areas are stacked on the same x/z, so every mob zone and every safe spot has a y-range, and every path
 * has the heights it runs at (a crown path never blocks the ground below it).
 */
public final class Z3WezensPlekken {
    private Z3WezensPlekken() {
    }

    public static final int TX = 2300, TZ = 0;
    public static final int MIN_X = 1600, MAX_X = 3000, MIN_Z = -700, MAX_Z = 700;

    /** The five layers (areas) with their levels (ZONE3_PLAN §2) and heights (POI_Z3 v1). */
    public record Laag(String id, int minY, int maxY, int minLevel, int maxLevel) {
    }

    public static final Laag KRUINZEE = new Laag("kruinzee", 200, 320, 30, 34);
    public static final Laag TAKKENBANEN = new Laag("takkenbanen", 120, 235, 34, 38);
    public static final Laag SCHORSSTAD = new Laag("schorsstad", 75, 199, 40, 43);
    public static final Laag WORTELVELDEN = new Laag("wortelvelden", 60, 140, 43, 46);
    public static final Laag WORTELDUISTER = new Laag("wortelduister", 6, 62, 46, 49);
    public static final List<Laag> LAGEN = List.of(KRUINZEE, TAKKENBANEN, SCHORSSTAD, WORTELVELDEN, WORTELDUISTER);

    public static double r(double x, double z) {
        return Math.hypot(x - TX, z - TZ);
    }

    public static boolean inZone(double x, double z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    /** Is this in the Wortelduister or the Hart below the trunk: the soft lantern rule applies. */
    public static boolean duister(double x, double y, double z) {
        if (!inZone(x, z)) {
            return false;
        }
        // z3-systemen's layered kern areas (Gebieden.op(x, y, z)); the plain geometry as a fallback outside them
        nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.Gebied g = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.op(x, y, z);
        if (g != null && g.id().startsWith("z3_")) {
            return g.id().equals("z3_wortelduister") || g.id().equals("z3_hart");
        }
        return y < 62 && r(x, z) <= 470;
    }

    // ---- mob zones ------------------------------------------------------------------------------------------------------

    /** A mob zone (z3-wereld's Gebied3 shape): a box (a,b)..(c,d) or a ring around (a,b) with radius c..d; heights; levels. */
    public record Zone(String id, boolean cilinder, int a, int b, int c, int d, int minY, int maxY, int minLevel, int maxLevel) {
        public boolean bevatPlat(double x, double z) {
            if (!cilinder) {
                return x >= a && x <= c + 0.999 && z >= b && z <= d + 0.999;
            }
            double dx = x - (a + 0.5), dz = z - (b + 0.5), r2 = dx * dx + dz * dz;
            return r2 >= (double) c * c && r2 <= (double) d * d;
        }

        public boolean bevat(double x, double y, double z) {
            return y >= minY && y <= maxY + 0.999 && bevatPlat(x, z);
        }

        /** The flat bounding box: minX, minZ, maxX, maxZ. */
        public int[] doos() {
            return cilinder ? new int[] {a - d, b - d, a + d, b + d} : new int[] {a, b, c, d};
        }

        /** Its layer, from the id (z3_&lt;laag&gt;_...). */
        @Nullable
        public Laag laag() {
            for (Laag l : LAGEN) {
                if (id.startsWith("z3_" + l.id())) {
                    return l;
                }
            }
            return null;
        }
    }

    /** POI_Z3 v1 "Mob zones" (copy; the live list replaces it when z3-wereld is there). */
    private static final List<Zone> MOBZONES_V1 = List.of(
            new Zone("z3_kruinzee_zeppelin", true, 2489, -200, 0, 60, 230, 300, 31, 34),
            new Zone("z3_kruinzee_noord", true, 2300, -150, 0, 160, 205, 300, 30, 33),
            new Zone("z3_kruinzee_zuid", true, 2300, 140, 0, 170, 205, 300, 30, 34),
            new Zone("z3_takkenbanen_west", false, 2719, -40, 2999, 110, 120, 230, 34, 37),
            new Zone("z3_takkenbanen_noordwest", false, 2559, -560, 2859, -250, 120, 230, 35, 38),
            new Zone("z3_takkenbanen_zuidwest", false, 2559, 240, 2859, 520, 120, 230, 34, 37),
            new Zone("z3_schorsstad_ringen", true, 2300, 0, 8, 66, 76, 182, 40, 43),
            new Zone("z3_wortelvelden_lichtkeverbos", false, 2139, -640, 2459, -330, 60, 140, 43, 45),
            new Zone("z3_wortelvelden_schimmelboerderij", false, 2479, -420, 2719, -180, 60, 140, 44, 46),
            new Zone("z3_wortelvelden_paddenbloesemdal", false, 2479, 200, 2819, 520, 60, 140, 43, 45),
            new Zone("z3_wortelvelden_honingmoeras", false, 1779, 150, 2139, 520, 60, 140, 44, 46),
            new Zone("z3_wortelduister_gang", true, 2300, 160, 0, 140, 10, 62, 46, 47),
            new Zone("z3_wortelduister_grotten", true, 2300, 0, 70, 450, 6, 60, 47, 49));

    public static final List<Zone> MOBZONES = new ArrayList<>();

    /** The mob zone at this point, or null (spawns happen only inside one). */
    @Nullable
    public static Zone mobzone(double x, double y, double z) {
        for (Zone g : MOBZONES) {
            if (g.bevat(x, y, z)) {
                return g;
            }
        }
        return null;
    }

    public static List<Zone> zonesVan(Laag l) {
        List<Zone> u = new ArrayList<>();
        for (Zone z : MOBZONES) {
            if (z.laag() == l) {
                u.add(z);
            }
        }
        return u;
    }

    // ---- z3-wereld (direct since K-INT) -------------------------------------------------------------------------------------

    // K-INT: z3-wereld is in the tree now: direct references instead of reflection (the V1 copies stay as the fallback).

    /** Is z3-wereld's generated plekken class there? (always, after integration) */
    public static boolean wereldBekend() {
        return true;
    }

    /** Z3WereldPlekken.plek(id), or null (id missing). */
    @Nullable
    public static BlockPos wereld(String id) {
        return nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek(id);
    }

    /** A spot: z3-wereld's id when it exists, else this POI_Z3 v1 copy. */
    private static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    static {
        for (var g : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.MOBZONES) {
            MOBZONES.add(new Zone(g.id(), g.cilinder(), g.a(), g.b(), g.c(), g.d(), g.minY(), g.maxY(), g.minLevel(), g.maxLevel()));
        }
        if (MOBZONES.isEmpty()) {
            MOBZONES.addAll(MOBZONES_V1);
        }
    }

    // ---- spots ----------------------------------------------------------------------------------------------------------

    /** Where fleeing Mika's head (their zeppelins): the crown dock, the werf, the mould farm. */
    public static final BlockPos ZEPPELIN_KRUIN = w("mika_steiger", 2483, 258, -196);
    public static final BlockPos ZEPPELIN_WERF = w("werf_ingang", 2861, 169, -537);
    public static final BlockPos ZEPPELIN_BOERDERIJ = w("schimmelboerderij", 2599, 70, -300);

    public static BlockPos zeppelin(double x, double y, double z) {
        if (y >= 200) {
            return ZEPPELIN_KRUIN;
        }
        return y >= 120 ? ZEPPELIN_WERF : ZEPPELIN_BOERDERIJ;
    }

    /** The named area leaders' landmarks (ZONE3_PLAN §6: "elites staan bij de landmarks"); floor search 24 down / 10 up. */
    public static final Map<String, BlockPos> LEIDERS = new LinkedHashMap<>();

    static {
        LEIDERS.put("rupsenmoeder", w("leider_rupsenmoeder", 2230, 275, 230));            // the Kruinzee south, near the blossom fall
        LEIDERS.put("kapitein_bladrover", w("leider_kapitein_bladrover", 2489, 262, -216)); // the zeppelin dock (elite camp)
        LEIDERS.put("harskolos", w("leider_harskolos", 2325, 146, -59));                   // the 5th ring of the Schorsstad
        LEIDERS.put("wilde_rotkeiler", w("leider_wilde_rotkeiler", 2380, 80, -420));       // the Lichtkeverbos
        LEIDERS.put("ziener_oudste", w("leider_ziener_oudste", 2420, 36, -150));           // the caves of the Wortelduister
    }

    /**
     * The lairs of the 8 contract elites (ZONE3_PLAN §8.4: "max ~3 min from a Pluimpost"). Ids = {@link Z3Contracten} ids.
     * Each one near a Pluimpost or a branch Pluimpost, in its creature's own area.
     */
    public static final Map<String, BlockPos> HOLEN = new LinkedHashMap<>();

    static {
        HOLEN.put("ouwe_rotkeiler", w("hol_ouwe_rotkeiler", 2640, 70, -230));           // the mould farm (pluimpost_2)
        HOLEN.put("moeder_wesp", w("hol_moeder_wesp", 2700, 205, 380));                  // on the ZW-tak
        HOLEN.put("de_harsreus", w("hol_de_harsreus", 2325, 76, -30));                   // the Schorsstad floor
        HOLEN.put("kapitein_kaaskop", w("kaasschutterpost_2", 2620, 219, -314));        // a Kaasschutter post on the NW-tak
        HOLEN.put("grootvader_rups", w("hol_grootvader_rups", 2250, 265, 180));          // the Kruinzee south
        HOLEN.put("de_stille_ziener", w("hol_de_stille_ziener", 2420, 30, -230));         // the Wortelduister caves
        HOLEN.put("slangenkoningin_sapsie", w("hol_slangenkoningin_sapsie", 1960, 67, 250)); // the Honingmoeras (pluimpost_4)
        HOLEN.put("bramzwijn_de_brede", w("hol_bramzwijn_de_brede", 2720, 70, 440));     // the Paddenbloesemdal (pluimpost_3)
    }

    /** A place no creature may come: a flat circle and a y-range (layers!). */
    public record Veilig(BlockPos pos, int straal, int minY, int maxY) {
        public boolean bevat(double x, double y, double z, double extra) {
            double dx = x - pos.getX(), dz = z - pos.getZ(), r = straal + extra;
            return dx * dx + dz * dz <= r * r && y >= minY && y <= maxY;
        }
    }

    private static final List<BlockPos> KAMPVUREN_V1 = List.of(new BlockPos(2787, 140, 33), new BlockPos(2269, 13, 20), new BlockPos(2044, 255, 147),
            new BlockPos(2442, 265, 82), new BlockPos(2300, 255, -230), new BlockPos(2265, 282, -10), new BlockPos(2055, 69, 414),
            new BlockPos(2276, 129, 59), new BlockPos(2273, 76, 34), new BlockPos(2704, 197, -405), new BlockPos(2568, 225, 5),
            new BlockPos(2760, 201, 20), new BlockPos(2616, 214, 279), new BlockPos(2244, 47, 213), new BlockPos(2207, 31, 126),
            new BlockPos(1956, 73, -147), new BlockPos(1827, 67, 83), new BlockPos(2135, 67, 451), new BlockPos(2608, 67, 367),
            new BlockPos(2772, 66, -84), new BlockPos(2464, 68, -452), new BlockPos(1991, 68, -368));

    public static final List<Veilig> VEILIG = new ArrayList<>();

    private static void veilig(BlockPos p, int straal, int onder, int boven) {
        VEILIG.add(new Veilig(p, straal, p.getY() - onder, p.getY() + boven));
    }

    static {
        // Honingtak: box (1634,-100)..(2014,110), y 150-290 (the Pluimstal included), covered by circles
        for (int x = 1660; x <= 2010; x += 70) {
            VEILIG.add(new Veilig(new BlockPos(x, 220, 5), 112, 145, 295));
        }
        veilig(w("hangnest_plein", 2783, 140, 37), 42, 30, 60);                // Hangnest Dauwdrup under the W-tak
        veilig(w("mosvoet_plein", 2049, 69, 424), 60, 20, 45);                 // Mosvoet
        veilig(w("arena_vahoegnir", 2299, 280, -40), 48, 40, 40);              // the old nest: Vahoegnir
        veilig(w("arena_kruinroofvogel", 2129, 238, -170), 48, 40, 50);        // the world boss on the Kale Kruin
        veilig(w("kruinroofvogel_zit_1", 2037, 258, 42), 26, 30, 30);
        veilig(w("kruinroofvogel_zit_2", 2339, 248, -292), 26, 30, 30);
        VEILIG.add(new Veilig(new BlockPos(2899, 140, -570), 118, 90, 190));    // the Kaaszeppelinwerf (dungeon 1)
        VEILIG.add(new Veilig(new BlockPos(TX, 57, TZ), 122, 38, 76));          // the Hart van de Boom (dungeon 2)
        veilig(w("hartslag_zaal", 2299, 13, 30), 66, 13, 32);                   // the Hartslag-zaal + the Hart's gate
        veilig(w("arena_piepvel", 2299, 184, 0), 16, 110, 20);                 // the Lantaarntoren (Pelszaal on top)
        veilig(w("wortelpoort", 2299, 71, 282), 22, 10, 20);                   // the Grote Wortelpoort
        for (String id : List.of("pluimpost_1", "pluimpost_2", "pluimpost_3", "pluimpost_4")) {
            BlockPos p = wereld(id);
            if (p != null) {
                veilig(p, 22, 14, 24);
            }
        }
        if (wereld("pluimpost_1") == null) {
            veilig(new BlockPos(2300, 66, -514), 22, 14, 24);
            veilig(new BlockPos(2667, 68, -362), 22, 14, 24);
            veilig(new BlockPos(2653, 69, 359), 22, 14, 24);
            veilig(new BlockPos(1918, 72, 387), 22, 14, 24);
        }
        veilig(w("pluimpost_westtak", 2690, 210, 36), 20, 14, 24);
        for (int i = 1; i <= 4; i++) {                                           // the mould kettles (quest objects)
            BlockPos k = wereld("schimmelketel_" + i);
            if (k != null) {
                veilig(k, 9, 6, 16);
            }
        }
        List<BlockPos> vuren = new ArrayList<>(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.KAMPVUREN);
        if (vuren.isEmpty()) {
            vuren.addAll(KAMPVUREN_V1);
        }
        for (BlockPos b : vuren) {
            veilig(b, 24, 14, 20);
        }
    }

    /** Is this spot safe (extra = an added flat margin)? */
    public static boolean veilig(double x, double y, double z, double extra) {
        for (Veilig v : VEILIG) {
            if (v.bevat(x, y, z, extra)) {
                return true;
            }
        }
        return false;
    }

    // ---- paths (riding routes): no spawns within ~5 blocks, PER LAYER --------------------------------------------------------

    /** A path: its points (x, z) and the heights it runs between (a path on the crown never blocks the ground below it). */
    public record Pad(String naam, int[][] punten, int minY, int maxY) {
    }

    public static final List<Pad> PADEN = new ArrayList<>();
    public static final double PAD_BREEDTE = 3.5, PAD_VRIJ = 5;

    /** POI_Z3 v1 paths (copy). */
    private static final Map<String, int[][]> PADEN_V1 = Map.of(
            "kruinring", new int[][] {{1994, 0}, {2007, 78}, {2044, 147}, {2099, 200}, {2166, 231}, {2235, 240}, {2299, 230}, {2354, 203}, {2396, 166},
                    {2425, 125}, {2442, 82}, {2452, 40}, {2454, 0}, {2452, -40}, {2442, -82}, {2425, -125}, {2396, -166}, {2354, -203}, {2300, -230},
                    {2235, -240}, {2166, -231}, {2099, -200}, {2044, -147}, {2007, -78}, {1994, 0}},
            "takbaan_west", new int[][] {{2339, 8}, {2411, 12}, {2499, 18}, {2567, 21}, {2629, 24}, {2690, 27}, {2749, 30}, {2808, 32}, {2864, 34},
                    {2927, 35}, {2974, 36}},
            "takbaan_noordwest", new int[][] {{2327, -28}, {2391, -89}, {2469, -165}, {2531, -226}, {2589, -285}, {2650, -342}, {2709, -398},
                    {2768, -452}, {2819, -500}, {2861, -536}, {2889, -560}},
            "takbaan_zuidwest", new int[][] {{2327, 28}, {2386, 82}, {2459, 150}, {2521, 207}, {2579, 262}, {2637, 313}, {2691, 360}, {2744, 402},
                    {2789, 440}, {2827, 471}, {2851, 492}},
            "wortelweg", new int[][] {{1829, 0}, {1837, 81}, {1858, 160}, {1892, 234}, {1939, 302}, {1997, 360}, {2064, 407}, {2139, 441}, {2218, 462},
                    {2299, 470}, {2381, 462}, {2460, 441}, {2534, 407}, {2602, 360}, {2660, 302}, {2707, 234}, {2741, 160}, {2762, 81}, {2769, 0},
                    {2762, -81}, {2741, -160}, {2707, -235}, {2660, -302}, {2602, -360}, {2535, -407}, {2460, -441}, {2381, -462}, {2300, -470},
                    {2218, -462}, {2139, -441}, {2064, -407}, {1997, -360}, {1939, -302}, {1892, -235}, {1858, -160}, {1837, -81}, {1829, 0}},
            "wortelgang", new int[][] {{2299, 268}, {2287, 256}, {2269, 239}, {2249, 219}, {2226, 195}, {2209, 170}, {2205, 145}, {2208, 120},
                    {2217, 99}, {2235, 79}, {2249, 66}});

    /** The heights a named path runs at (z3-wereld's paths are flat x/z lists). */
    static int[] hoogten(String naam) {
        if (naam.startsWith("kruin")) {
            return new int[] {196, 320};
        }
        if (naam.startsWith("takbaan") || naam.startsWith("tak") || naam.contains("lianen") || naam.contains("valbrug")) {
            return new int[] {120, 250};
        }
        if (naam.startsWith("wortelweg")) {
            return new int[] {50, 145};
        }
        if (naam.startsWith("wortelgang") || naam.contains("duister")) {
            return new int[] {0, 80};
        }
        if (naam.contains("spiraal")) {
            return new int[] {60, 210};
        }
        return new int[] {-64, 320};
    }

    static {
        Map<String, int[][]> bron = new LinkedHashMap<>();
        bron.putAll(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.PADEN);
        if (bron.isEmpty()) {
            bron.putAll(PADEN_V1);
        }
        bron.forEach((n, p) -> {
            if (p.length > 1) {
                int[] h = hoogten(n);
                PADEN.add(new Pad(n, p, h[0], h[1]));
            }
        });
    }

    private static double afstandTot(Pad pad, double x, double z) {
        double best = Double.MAX_VALUE;
        int[][] p = pad.punten();
        for (int i = 0; i + 1 < p.length; i++) {
            double ax = p[i][0], az = p[i][1], bx = p[i + 1][0], bz = p[i + 1][1];
            double dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
            double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / l2));
            best = Math.min(best, Math.hypot(x - (ax + t * dx), z - (az + t * dz)) - PAD_BREEDTE / 2);
        }
        return best;
    }

    /** The flat distance from (x, z) to the nearest path EDGE of a path that runs at this height (MAX_VALUE: none). */
    public static double padAfstand(double x, double y, double z) {
        double best = Double.MAX_VALUE;
        for (Pad pad : PADEN) {
            if (y >= pad.minY() && y <= pad.maxY()) {
                best = Math.min(best, afstandTot(pad, x, z));
            }
        }
        return best;
    }

    /** The distance to the nearest path edge for ANY height inside this y-range (planning a layer spawner). */
    public static double padAfstand(double x, double z, int minY, int maxY) {
        double best = Double.MAX_VALUE;
        for (Pad pad : PADEN) {
            if (pad.maxY() >= minY && pad.minY() <= maxY) {
                best = Math.min(best, afstandTot(pad, x, z));
            }
        }
        return best;
    }

    /** The path by name (or null). */
    @Nullable
    public static Pad pad(String naam) {
        for (Pad p : PADEN) {
            if (p.naam().equals(naam)) {
                return p;
            }
        }
        return null;
    }

    // ---- the branches (Takkenbanen spawners follow them) ---------------------------------------------------------------

    /**
     * A branch: its ride path (the ridge) and, per point, the branch top height and radius (render TAKKEN mirrored to the east:
     * the top = centre y + radius). The Takkenbanen spawners sit beside the ridge, in the branch mob zones.
     */
    public record Tak(String pad, int[] top, int[] stralen) {
    }

    public static final List<Tak> TAKKEN = List.of(
            new Tak("takbaan_west", new int[] {242, 235, 228, 220, 212, 204, 196, 188, 180, 172, 167}, new int[] {40, 34, 30, 27, 24, 22, 20, 18, 16, 13, 11}),
            new Tak("takbaan_noordwest", new int[] {240, 236, 232, 226, 218, 210, 201, 192, 184, 172, 163},
                    new int[] {38, 32, 28, 25, 22, 20, 18, 16, 14, 12, 10}),
            new Tak("takbaan_zuidwest", new int[] {240, 235, 230, 225, 217, 209, 200, 192, 184, 172, 163},
                    new int[] {38, 32, 28, 25, 22, 20, 18, 16, 14, 12, 10}));

    /** The Schorsstad ring terraces (stand y = 81 + 16 i, POI schorsstad_ring_N) at radius ~58 from the axis. */
    public static final int[] RINGEN = {80, 96, 112, 128, 144, 160, 176};
    public static final int RING_STRAAL = 58;
}
