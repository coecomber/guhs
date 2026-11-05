package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Every coordinate of the zone-3 wezens slice (Z3_BRIEF: one class). Zone 3 "De Schemerwortel" (REVAMP, ronde 20) = x 1900..2700,
 * z -400..400, the trunk axis T at (2300, 0): a narrow tall tree in 4 layers (roots 4-78, trunk 80-160, branches 160-216, crown
 * 216-312), the ground of the floating island round it (y ~60-82), the werf on its own island in the north-east (POI_Z3.md v2).
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
    public static final int MIN_X = 1900, MAX_X = 2700, MIN_Z = -400, MAX_Z = 400;

    /** The five layers (areas) with their levels (ZONE3_PLAN §2) and heights (POI_Z3 v2 mob zones: the crown storeys from 222, the
     *  branches 170-222, the dark stair section of the trunk 172-228, the ground 50-110, the roots hall and passage 14-72). */
    public record Laag(String id, int minY, int maxY, int minLevel, int maxLevel) {
    }

    public static final Laag KRUINZEE = new Laag("kruinzee", 212, 320, 30, 34);
    public static final Laag TAKKENBANEN = new Laag("takkenbanen", 160, 226, 34, 38);
    public static final Laag SCHORSSTAD = new Laag("schorsstad", 55, 232, 40, 43);
    public static final Laag WORTELVELDEN = new Laag("wortelvelden", 45, 120, 43, 46);
    public static final Laag WORTELDUISTER = new Laag("wortelduister", 0, 72, 46, 49);
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
        return y < 64 && r(x, z) <= 160;
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

    /** POI_Z3 v2 "Mob zones" (copy; the live list replaces it). */
    private static final List<Zone> MOBZONES_V1 = List.of(
            new Zone("z3_kruinzee_zeppelin", true, 2355, -95, 0, 34, 220, 250, 31, 34),
            new Zone("z3_kruinzee_noord", false, 2170, -130, 2430, -10, 222, 300, 30, 33),
            new Zone("z3_kruinzee_zuid", false, 2170, 10, 2430, 130, 222, 300, 30, 34),
            new Zone("z3_takkenbanen_west", false, 2140, -50, 2250, 50, 170, 222, 34, 37),
            new Zone("z3_takkenbanen_noordwest", false, 2240, -160, 2450, -50, 170, 222, 35, 38),
            new Zone("z3_takkenbanen_zuidwest", false, 2160, 50, 2340, 160, 170, 222, 34, 37),
            new Zone("z3_schorsstad_ringen", true, 2300, 0, 0, 24, 172, 228, 40, 43),
            new Zone("z3_wortelvelden_lichtkeverbos", false, 2220, -215, 2380, -110, 50, 110, 43, 45),
            new Zone("z3_wortelvelden_schimmelboerderij", false, 2085, -80, 2190, 80, 50, 110, 44, 46),
            new Zone("z3_wortelvelden_paddenbloesemdal", false, 2220, 110, 2380, 215, 50, 110, 43, 45),
            new Zone("z3_wortelvelden_honingmoeras", false, 2410, -80, 2515, 40, 50, 110, 44, 46),
            new Zone("z3_wortelduister_gang", false, 2340, -12, 2450, 16, 18, 72, 46, 47),
            new Zone("z3_wortelduister_grotten", true, 2300, 0, 24, 86, 14, 58, 47, 49));

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
    public static final BlockPos ZEPPELIN_KRUIN = w("mika_steiger", 2355, 227, -96);
    public static final BlockPos ZEPPELIN_WERF = w("werf_ingang", 2505, 161, -206);
    public static final BlockPos ZEPPELIN_BOERDERIJ = w("schimmelboerderij", 2149, 68, -55);

    /** (revamp) the crown from 212, the branches + Honingtak + the werf 120-212, below that the ground (the mould farm). */
    public static BlockPos zeppelin(double x, double y, double z) {
        if (y >= 212) {
            return ZEPPELIN_KRUIN;
        }
        return y >= 120 ? ZEPPELIN_WERF : ZEPPELIN_BOERDERIJ;
    }

    /** A spot: its own id when the map has it, else next to an anchor POI of the same place (dx, dy, dz), else the v2 copy. */
    private static BlockPos wa(String id, String anker, int dx, int dy, int dz, int x, int y, int z) {
        BlockPos b = wereld(id);
        if (b != null) {
            return b;
        }
        BlockPos a = wereld(anker);
        return a != null ? a.offset(dx, dy, dz) : new BlockPos(x, y, z);
    }

    /** The named area leaders' landmarks (ZONE3_PLAN §6: "elites staan bij de landmarks"); floor search 24 down / 10 up. */
    public static final Map<String, BlockPos> LEIDERS = new LinkedHashMap<>();

    static {
        // (P-VERHUIS) on the revamped map: each leader by a landmark of its own layer (its own POI when wereld publishes one)
        LEIDERS.put("rupsenmoeder", wa("leider_rupsenmoeder", "bloesemwaterval_top", 0, 0, -6, 2261, 222, 105));      // kruin 1 south, by the blossom fall
        LEIDERS.put("kapitein_bladrover", wa("leider_kapitein_bladrover", "mika_steiger", -6, 0, 6, 2349, 227, -90));   // the Mika zeppelin dock (elite camp)
        LEIDERS.put("harskolos", wa("leider_harskolos", "boomvolk_lichtje_3", -4, 0, 0, 2312, 199, 8));                  // the dark stair above Honingtak
        LEIDERS.put("wilde_rotkeiler", wa("leider_wilde_rotkeiler", "lichtkeverbos", 0, 0, 0, 2299, 69, -150));         // the Lichtkeverbos (ground north)
        LEIDERS.put("ziener_oudste", wa("leider_ziener_oudste", "geheim_schat_4", 6, 0, -6, 2246, 30, 4));               // the roots hall
    }

    /**
     * The lairs of the 8 contract elites (ZONE3_PLAN §8.4: "max ~3 min from a Pluimpost"). Ids = {@link Z3Contracten} ids.
     * Each one near a Pluimpost or a branch Pluimpost, in its creature's own area.
     */
    public static final Map<String, BlockPos> HOLEN = new LinkedHashMap<>();

    static {
        HOLEN.put("ouwe_rotkeiler", wa("hol_ouwe_rotkeiler", "schimmelketel_3", 0, 0, -12, 2163, 67, -111));            // the mould farm (pluimpost_2)
        HOLEN.put("moeder_wesp", wa("hol_moeder_wesp", "kaasschutterpost_3", -10, 0, 8, 2222, 212, 115));              // on the Zuidwesttak
        HOLEN.put("de_harsreus", wa("hol_de_harsreus", "boomvolk_lichtje_4", 0, 6, 0, 2305, 218, -19));                // the dark stair, high
        HOLEN.put("kapitein_kaaskop", w("kaasschutterpost_2", 2395, 195, -11));                                        // a Kaasschutter post on the Noordtak
        HOLEN.put("grootvader_rups", wa("hol_grootvader_rups", "kuiken_3", 0, 0, 10, 2208, 227, -21));                 // kruin 1 west
        HOLEN.put("de_stille_ziener", wa("hol_de_stille_ziener", "geheim_knuffel_10", 0, 0, 0, 2260, 30, 30));         // the roots hall
        HOLEN.put("slangenkoningin_sapsie", wa("hol_slangenkoningin_sapsie", "honingmoeras", 10, 0, 6, 2465, 68, -51)); // the Honingmoeras (pluimpost_1)
        HOLEN.put("bramzwijn_de_brede", wa("hol_bramzwijn_de_brede", "paddenbloesemdal", 12, 0, 0, 2298, 68, 159));    // the Paddenbloesemdal (pluimpost_3)
    }

    /** A place no creature may come: a flat circle and a y-range (layers!). */
    public record Veilig(BlockPos pos, int straal, int minY, int maxY) {
        public boolean bevat(double x, double y, double z, double extra) {
            double dx = x - pos.getX(), dz = z - pos.getZ(), r = straal + extra;
            return dx * dx + dz * dz <= r * r && y >= minY && y <= maxY;
        }
    }

    private static final List<BlockPos> KAMPVUREN_V1 = List.of(new BlockPos(2342, 197, 59), new BlockPos(2360, 25, -14), new BlockPos(2399, 230, 42),
            new BlockPos(2256, 261, -50), new BlockPos(2281, 284, 40), new BlockPos(2323, 285, 29), new BlockPos(2422, 67, 97), new BlockPos(2298, 132, 26),
            new BlockPos(2290, 81, 7), new BlockPos(2287, 185, -77), new BlockPos(2247, 209, -19), new BlockPos(2207, 210, -31), new BlockPos(2250, 204, 59),
            new BlockPos(2403, 58, 1), new BlockPos(2368, 38, 6), new BlockPos(2261, 66, -163), new BlockPos(2453, 64, 88), new BlockPos(2300, 64, 177),
            new BlockPos(2146, 66, 88), new BlockPos(2146, 65, -89), new BlockPos(2299, 65, -177), new BlockPos(2453, 66, -89));

    public static final List<Veilig> VEILIG = new ArrayList<>();

    private static void veilig(BlockPos p, int straal, int onder, int boven) {
        VEILIG.add(new Veilig(p, straal, p.getY() - onder, p.getY() + boven));
    }

    static {
        // (P-VERHUIS) Honingtak: the ring town round the trunk (deck 166, houses to ~182, the boardwalk branches to r ~140)
        VEILIG.add(new Veilig(new BlockPos(TX, 166, TZ), 145, 145, 186));
        veilig(w("dok_mast", 2209, 167, -94), 30, 10, 24);                           // the airship dock at the tip of the Imkerstraat
        // the beekeeper villages on the big branches
        for (String id : List.of("hangnest_plein", "raatweide_plein", "molenhoek_plein")) {
            BlockPos b = wereld(id);
            if (b != null) {
                veilig(b, 26, 16, 22);
            }
        }
        veilig(w("mosvoet_plein", 2427, 66, 92), 34, 16, 30);                         // Mosvoet (ground, foot of the Honinglift)
        veilig(w("gebouw.pluimstal.binnen", 2272, 75, -101), 34, 12, 24);             // the Pluimstal + its yard and training track
        veilig(w("pluimstal_oefenbaan", 2282, 69, -141), 22, 10, 20);
        veilig(w("arena_vahoegnir", 2300, 294, 0), 30, 14, 40);                       // the old nest: Vahoegnir
        veilig(w("arena_kruinroofvogel", 2155, 226, 46), 34, 20, 40);                 // the world boss on the Kale Kruin
        veilig(w("kruinroofvogel_zit_1", 2367, 210, 124), 20, 20, 24);
        veilig(w("kruinroofvogel_zit_2", 2426, 202, -11), 20, 20, 24);
        // the Kaaszeppelinwerf (dungeon 1): its whole island
        for (var g : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.GEBIEDEN) {
            if (g.id().equals("z3_zeppelinwerf") && g.cilinder()) {
                VEILIG.add(new Veilig(new BlockPos(g.a(), (g.minY() + g.maxY()) / 2, g.b()), g.d(), g.minY(), g.maxY()));
            }
        }
        veilig(w("kabelbrug_begin", 2407, 167, -100), 12, 8, 16);
        // the Hart van de Boom (dungeon 2): a ring of rooms round the roots hall: each room, not the hall itself
        for (String id : List.of("hart_kamer_aderloop", "hart_kamer_schimmelkwekerij", "hart_kamer_ringenzaal", "arena_wortelwurger",
                "arena_nestwachter", "arena_baron_bleu", "hart_poort")) {
            BlockPos b = wereld(id);
            if (b != null) {
                veilig(b, 26, 8, 18);
            }
        }
        veilig(w("hart_nestkamer", 2309, 21, 0), 14, 6, 12);                          // the egg's root basket in the middle of the hall
        veilig(w("hartslag_zaal", 2352, 23, 0), 20, 8, 16);                           // the Hartslag-zaal (Gloei's campfire)
        veilig(w("arena_piepvel", 2300, 61, 0), 18, 4, 14);                           // the Pelszaal under the Schorsstad floor
        VEILIG.add(new Veilig(new BlockPos(TX, 81, TZ), 26, 75, 168));                // the Schorsstad: the town in the hollow trunk (floor -> binnenhof)
        veilig(w("wortelpoort", 2446, 70, 0), 18, 10, 18);                            // the Wortelpoort
        for (String id : List.of("pluimpost_1", "pluimpost_2", "pluimpost_3", "pluimpost_4", "pluimpost_westtak")) {
            BlockPos p = wereld(id);
            if (p != null) {
                veilig(p, 18, 12, 20);
            }
        }
        for (int i = 1; i <= 4; i++) {                                           // the mould kettles (quest objects)
            BlockPos k = wereld("schimmelketel_" + i);
            if (k != null) {
                veilig(k, 7, 6, 16);
            }
        }
        for (int i = 1; i <= 6; i++) {                                           // the stair landings (doors): never a mob in a doorway
            BlockPos b = wereld("trap_bordes_" + i);
            if (b != null) {
                veilig(b, 8, 4, 8);
            }
        }
        List<BlockPos> vuren = new ArrayList<>(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.KAMPVUREN);
        if (vuren.isEmpty()) {
            vuren.addAll(KAMPVUREN_V1);
        }
        for (BlockPos b : vuren) {
            veilig(b, 20, 12, 18);
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

    /** POI_Z3 v2 paths (a short copy: only used when the generated list is empty). */
    private static final Map<String, int[][]> PADEN_V1 = Map.of(
            "kabelbrug", new int[][] {{2407, -99}, {2505, -205}},
            "wortelgang", new int[][] {{2440, 2}, {2430, 0}, {2417, -3}, {2406, -2}, {2396, 2}, {2386, 6}, {2376, 5}, {2366, 2}, {2357, 1}, {2348, 0},
                    {2342, 0}});

    /** The heights a named path runs at (z3-wereld's paths are flat x/z lists). */
    static int[] hoogten(String naam) {
        // (P-VERHUIS) per height on the revamped tree: a path on one layer never blocks the layers above/below it
        if (naam.equals("rijpad") || naam.contains("spiraal")) {
            return new int[] {76, 240};          // the outer riding spiral: ground (82) -> kruin 1 (232)
        }
        if (naam.startsWith("kruin")) {
            return new int[] {220, 320};
        }
        if (naam.startsWith("takbaan") || naam.startsWith("tak") || naam.contains("lianen") || naam.contains("valbrug")) {
            return new int[] {165, 232};         // the big branches (their ridge is the path)
        }
        if (naam.startsWith("kabelbrug")) {
            return new int[] {150, 180};
        }
        if (naam.startsWith("wortelweg")) {
            return new int[] {50, 110};
        }
        if (naam.startsWith("wortelgang") || naam.contains("duister")) {
            return new int[] {14, 80};
        }
        return new int[] {-64, 320};
    }

    /** Branch ridge paths are narrow: their no-spawn margin is smaller (mobs live on the flanks of the big branches). */
    public static final double TAK_VRIJ = 1.5;

    public static double vrij(Pad pad) {
        return pad.naam().startsWith("takbaan") ? TAK_VRIJ : PAD_VRIJ;
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
                best = Math.min(best, afstandTot(pad, x, z) + PAD_VRIJ - vrij(pad));
            }
        }
        return best;
    }

    /** The distance to the nearest path edge for ANY height inside this y-range (planning a layer spawner). */
    public static double padAfstand(double x, double z, int minY, int maxY) {
        double best = Double.MAX_VALUE;
        for (Pad pad : PADEN) {
            if (pad.maxY() >= minY && pad.minY() <= maxY) {
                best = Math.min(best, afstandTot(pad, x, z) + PAD_VRIJ - vrij(pad));
            }
        }
        return best;
    }

    /** Like {@link #padAfstand(double, double, int, int)} but ignoring the branch ridge paths (planning branch-flank spawners). */
    public static double padAfstandBuitenTakken(double x, double z, int minY, int maxY) {
        double best = Double.MAX_VALUE;
        for (Pad pad : PADEN) {
            if (!pad.naam().startsWith("takbaan") && pad.maxY() >= minY && pad.minY() <= maxY) {
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
     * A big branch (boom2.py TAKLIJST, after the riding-spiral alignment): its ride path (the ridge, PADEN takbaan_*) and its radius
     * at the trunk and at the tip. The Takkenbanen spawners sit on the flanks beside the ridge; the floor search covers the branch
     * layer (170-224), so the exact ridge height is found in the world.
     */
    public record Tak(String pad, double rStam, double rPunt) {
    }

    public static final List<Tak> TAKKEN = List.of(
            new Tak("takbaan_bijentak", 11, 5.5), new Tak("takbaan_zuidwesttak", 11, 5.5), new Tak("takbaan_westtak", 12, 5.5),
            new Tak("takbaan_molentak", 10, 5.5), new Tak("takbaan_noordtak", 9, 5.5));
    public static final int TAK_MIN_Y = 170, TAK_MAX_Y = 224;

    /** The dark stair section of the trunk (Schorsstad mob zone, above Honingtak): spawners on the stair at radius 19, every 12 up. */
    public static final int[] RINGEN = {176, 188, 200, 212, 222};
    public static final int RING_STRAAL = 19;
}
