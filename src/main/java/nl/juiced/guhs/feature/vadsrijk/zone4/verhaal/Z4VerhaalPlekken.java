package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-4 story slice (T_BRIEF: one class). Zone 4 "De Lampionkust" = x 1740..2540, z -716..-266 in
 * guhs:vadsrijk (WERELDINDELING W-3; tile box x 1728..2559, z -768..-257); water up to y 62 (surface 63), land ~66-70.
 * <p>Every spot is {@code w("<POI_Z4 id>", fallback)}: t-z4wereld's generated spots win once its class is in the tree (looked up by
 * reflection: {@code zone4.wereld.Z4WereldPlekken#plek(String)}, so this compiles without it); the numbers are the render
 * coordinates of renders-lampionkust/LEESMIJ.md and are only used for an id the map lacks. The requested ids are listed in
 * guhs_vadsrijk/POI_VERZOEKEN_Z4.md. NPCs snap to the nearest floor within 12 blocks ({@code Npcs.vloer}).
 * <p>NPC/POI ids here are checked by tools/features/vadsrijk_t_z4verhaal.py (every NPC needs a name + a "where" text there).
 */
public final class Z4VerhaalPlekken {
    private Z4VerhaalPlekken() {
    }

    // ---- t-z4wereld's generated spots (by reflection until both slices are in one tree) ------------------------------------------
    private static final String[] WERELD = {"nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken",
            "nl.juiced.guhs.feature.vadsrijk.zone4.Z4WereldPlekken", "nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4Plekken"};
    private static Method wereldPlek;
    private static boolean gezocht;

    /** t-z4wereld's spot for this POI_Z4 id, or null (not in this tree, or an id it does not have). */
    @Nullable
    public static synchronized BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            for (String c : WERELD) {
                try {
                    wereldPlek = Class.forName(c).getMethod("plek", String.class);
                    break;
                } catch (ReflectiveOperationException | LinkageError e) {
                    // (not this name)
                }
            }
        }
        if (wereldPlek == null) {
            return null;
        }
        try {
            Object o = wereldPlek.invoke(null, id);
            return o instanceof BlockPos b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    /** A t-z4wereld spot moved a little (an NPC next to a campfire, not in it). */
    static BlockPos wo(String id, int dx, int dz, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b.offset(dx, 0, dz) : new BlockPos(x, y, z);
    }

    // ---- the zone --------------------------------------------------------------------------------------------------------------
    public static final int ZONE_MIN_X = 1728, ZONE_MIN_Z = -768, ZONE_MAX_X = 2559, ZONE_MAX_Z = -257;

    /** Is this point in zone 4 (the tile box; Hoogvads hangs above its east end, y > 170, and is zone 5's)? */
    public static boolean zone4(double x, double z) {
        return x >= ZONE_MIN_X && x <= ZONE_MAX_X + 0.999 && z >= ZONE_MIN_Z && z <= ZONE_MAX_Z + 0.999;
    }

    public static boolean zone4(double x, double y, double z) {
        return zone4(x, z) && y < 170;
    }

    // ---- Lampionhaven (the hub) ------------------------------------------------------------------------------------------------
    public static final BlockPos PLEIN = w("havenplein", 1848, 66, -462);
    /** Where the airship sets you down (the luchthaven platform, y 70). */
    public static final BlockPos LANDING = w("luchthaven_landing", 1800, 70, -470);
    public static final BlockPos KADE = w("kade_midden", 1864, 66, -464);

    // ---- the NPCs ------------------------------------------------------------------------------------------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();

    static void npc(String id, BlockPos b) {
        NPC.put(id, b);
    }

    static {
        // Lampionhaven: story + travel
        npc("z4_havenmeester", w("npc_havenmeester", 1866, 66, -464));
        npc("z4_zeilverhuur", w("npc_zeilverhuur", 1870, 66, -478));
        npc("z4_luchthaven", w("npc_luchthaven", 1798, 70, -466));
        // shops and services
        npc("z4_lampionmaker", w("npc_lampionwerkplaats", 1828, 66, -455));
        npc("z4_bankier", w("npc_bank", 1848, 66, -468));
        npc("z4_werkplaats", w("npc_werkplaats", 1822, 66, -450));
        npc("z4_winkel", w("npc_winkel", 1846, 66, -456));
        npc("z4_smid", w("npc_smid", 1866, 66, -445));
        npc("z4_wapens", w("npc_uitrusting", 1852, 66, -456));
        npc("z4_kleermaker", w("npc_kleermaker", 1842, 66, -468));
        npc("z4_markt", w("npc_markt", 1850, 66, -463));
        npc("z4_kok", w("npc_visrestaurant", 1902, 66, -498));
        npc("z4_visser", w("npc_visser", 1878, 66, -451));
        npc("z4_duinwachter", wo("kampvuur_lampionduin", 2, 1, 1860, 90, -515));
        // citizens
        npc("z4_oma_zeeschuim", w("npc_burger_1", 1845, 66, -460));
        npc("z4_kind_lampje", w("npc_burger_2", 1852, 66, -465));
        npc("z4_kind_golfje", w("npc_burger_3", 1864, 66, -470));
        npc("z4_postguh", w("npc_burger_4", 1850, 66, -459));
        npc("z4_danseres", w("npc_burger_5", 1864, 66, -458));
        npc("z4_schipper", w("npc_burger_6", 1880, 66, -440));
        // Knuffeltij-strand: the shelter + the refugees
        npc("z4_opvang", w("npc_opvangcentrum", 1926, 66, -361));
        npc("z4_wasguh", w("npc_knuffelwas", 1942, 66, -352));
        npc("z4_soepguh", w("npc_soepkraam", 1950, 66, -358));
        npc("z4_grijsje", w("npc_vluchteling_1", 1930, 66, -358));
        npc("z4_opa_dun", w("npc_vluchteling_2", 1953, 66, -355));
        npc("z4_vaaltje", w("npc_vluchteling_3", 1948, 66, -340));
        npc("z4_mama_pluis", w("npc_vluchteling_4", 1922, 66, -358));
        // the islands
        npc("z4_limonade", w("npc_limonadebar", 2003, 67, -609));
        npc("z4_volleybal", w("npc_volleybal", 2019, 67, -603));
        npc("z4_feestguh", w("npc_strandfeest", 1991, 67, -592));
        npc("z4_vliegermaker", w("npc_vliegerwerkplaats", 2284, 80, -651));
        npc("z4_windmeester", w("npc_wedstrijdpaal", 2302, 89, -663));
        npc("z4_duikmeester", w("npc_duiksteiger", 2185, 68, -618));
        npc("z4_wachter", w("npc_vuurtorenwachter", 2406, 87, -494));
        npc("z4_imker", w("npc_bijenboerderij", 2081, 70, -415));
        npc("z4_madelief", w("npc_bloemenvelden", 2090, 70, -392));
        npc("z4_schelpie", w("npc_schildpad", 2237, 71, -447));
        npc("z4_nevel", w("npc_mysterie", 2476, 78, -326));
        npc("z4_pierwachter", w("npc_pierwachter", 2478, 72, -563));
        npc("z4_hemelkapitein", w("npc_hemelkapitein", 2512, 72, -565));
        // the Kustwacht at the campfires
        npc("z4_wacht_haven", wo("kampvuur_havenplein", 2, 1, 1844, 66, -464));
        npc("z4_wacht_knuffeltij", wo("kampvuur_knuffeltij", 2, 1, 1935, 66, -350));
        npc("z4_wacht_strandfeest", wo("kampvuur_strandfeest", 2, 1, 1985, 67, -598));
        npc("z4_wacht_bloemen", wo("kampvuur_bloemeneiland", 2, 1, 2085, 70, -430));
        npc("z4_wacht_koraal", wo("kampvuur_koraalbaai", 2, 1, 2178, 68, -620));
        npc("z4_wacht_vlieger", wo("kampvuur_vliegereiland", 2, 1, 2290, 70, -640));
        npc("z4_wacht_vuurtoren", wo("kampvuur_vuurtoreneiland", 2, 1, 2385, 70, -490));
        npc("z4_wacht_pier", wo("kampvuur_pierkop", 2, 1, 2466, 72, -560));
    }

    // ---- POIs: quest targets (radius POI_STRAAL, height POI_HOOGTE) ----------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    public static final int POI_STRAAL = 6, POI_HOOGTE = 8, VIND_STRAAL = 3, VIND_HOOGTE = 4;

    static void poi(String id, BlockPos b) {
        POI.put(id, b);
    }

    static {
        poi("z4_havenplein", PLEIN);
        poi("z4_luchthaven", LANDING);
        poi("z4_steiger", w("steiger_noord", 1872, 66, -478));
        poi("z4_lampionduin", w("lampionduin_top", 1860, 90, -517));
        poi("z4_knuffeltij_strand", w("knuffeltij_strand", 1935, 66, -340));
        poi("z4_opvangcentrum", w("opvangcentrum", 1925, 66, -365));
        poi("z4_knuffelwas", w("knuffelwas", 1945, 66, -350));
        poi("z4_droogwaslijn", w("droogwaslijn", 1950, 66, -340));
        poi("z4_feestplek", w("knuffeltij_feestplek", 1932, 66, -336));
        poi("z4_grote_knuffel_1", w("grote_knuffel_strandfeest", 2005, 66, -572));
        poi("z4_grote_knuffel_2", w("grote_knuffel_bloemeneiland", 2040, 67, -405));
        poi("z4_grote_knuffel_3", w("grote_knuffel_koraalbaai", 2200, 67, -508));
        poi("z4_grote_knuffel_4", w("grote_knuffel_schildpad", 2243, 71, -442));
        poi("z4_mysterie_poort", w("mysterie_poort", 2480, 78, -330));
        poi("z4_wrak", w("wrak", 2165, 64, -700));
        poi("z4_wrak_ruim", w("wrak_ruim", 2165, 62, -700));
        poi("z4_wedstrijdpaal", w("wedstrijdpaal", 2300, 89, -665));
        poi("z4_vliegerwerkplaats", w("vliegerwerkplaats", 2282, 80, -653));
        poi("z4_duiksteiger", w("duiksteiger", 2185, 68, -620));
        poi("z4_grotje_1", w("koraal_grotje_1", 2171, 52, -565));
        poi("z4_grotje_2", w("koraal_grotje_2", 2195, 52, -585));
        poi("z4_grotje_3", w("koraal_grotje_3", 2190, 52, -545));
        poi("z4_vuurtoren_top", w("vuurtoren_top", 2400, 125, -500));
        poi("z4_bijenboerderij", w("bijenboerderij", 2079, 70, -417));
        poi("z4_strandfeest_kampvuur", w("kampvuur_strandfeest", 1989, 67, -595));
        poi("z4_limonadebar", w("limonadebar", 2003, 67, -611));
        poi("z4_volleybalveld", w("volleybalveld", 2021, 67, -603));
        poi("z4_schildpad", w("schildpad_rug", 2239, 71, -445));
        poi("z4_pierkop", w("pierkop", 2470, 72, -565));
        poi("z4_opstijgpier", w("opstijgpier_eind", 2512, 72, -565));
        poi("z4_kringstroom", w("kringstroom_zuid", 1960, 63, -248));
    }

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    /** Area id -> where the tracker points for "in area x". */
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    /** Area id -> its main campfire. */
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    /** Boss spot id -> its door/arena (the tracker's target for boss objectives). */
    public static final Map<String, BlockPos> BAAS = new LinkedHashMap<>();

    static {
        GEBIED_DOEL.put("z4_lampionhaven", PLEIN);
        GEBIED_DOEL.put("z4_knuffeltij", POI.get("z4_knuffeltij_strand"));
        GEBIED_DOEL.put("z4_strandfeest", POI.get("z4_strandfeest_kampvuur"));
        GEBIED_DOEL.put("z4_bloemeneiland", POI.get("z4_bijenboerderij"));
        GEBIED_DOEL.put("z4_koraalbaai", POI.get("z4_duiksteiger"));
        GEBIED_DOEL.put("z4_vliegereiland", POI.get("z4_vliegerwerkplaats"));
        GEBIED_DOEL.put("z4_vuurtoreneiland", w("vuurtoren_huisje", 2404, 87, -494));
        GEBIED_DOEL.put("z4_schildpadeiland", POI.get("z4_schildpad"));
        GEBIED_DOEL.put("z4_mysterie", POI.get("z4_mysterie_poort"));
        GEBIED_DOEL.put("z4_pierkop", POI.get("z4_pierkop"));
        GEBIED_DOEL.put("z4_lampionkust", PLEIN);
        KAMPVUUR.put("z4_lampionhaven", w("kampvuur_havenplein", 1844, 66, -464));
        KAMPVUUR.put("z4_knuffeltij", w("kampvuur_knuffeltij", 1935, 66, -350));
        KAMPVUUR.put("z4_strandfeest", POI.get("z4_strandfeest_kampvuur"));
        KAMPVUUR.put("z4_bloemeneiland", w("kampvuur_bloemeneiland", 2085, 70, -430));
        KAMPVUUR.put("z4_koraalbaai", w("kampvuur_koraalbaai", 2178, 68, -620));
        KAMPVUUR.put("z4_vliegereiland", w("kampvuur_vliegereiland", 2290, 70, -640));
        KAMPVUUR.put("z4_vuurtoreneiland", w("kampvuur_vuurtoreneiland", 2385, 70, -490));
        KAMPVUUR.put("z4_pierkop", w("kampvuur_pierkop", 2466, 72, -560));
        BAAS.put("z4_koraalbaai", POI.get("z4_grotje_1"));
        BAAS.put("z4_wrak", POI.get("z4_wrak_ruim"));
    }

    /** Our fallback area boxes (flat; t-z4wereld registers the real ones): islands first, the whole coast last. */
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLv, int maxLv, boolean stad) {
    }

    public static final List<Gebied> GEBIEDEN = List.of(
            new Gebied("z4_lampionhaven", 1780, -552, 1920, -388, 50, 56, true),
            new Gebied("z4_knuffeltij", 1873, -381, 1997, -300, 50, 51, false),
            new Gebied("z4_strandfeest", 1943, -641, 2047, -569, 50, 52, false),
            new Gebied("z4_bloemeneiland", 2037, -445, 2133, -365, 51, 52, false),
            new Gebied("z4_koraalbaai", 2123, -625, 2247, -505, 52, 54, false),
            new Gebied("z4_vliegereiland", 2256, -695, 2344, -635, 51, 53, false),
            new Gebied("z4_vuurtoreneiland", 2370, -526, 2430, -474, 52, 54, false),
            new Gebied("z4_schildpadeiland", 2210, -469, 2369, -370, 52, 54, false),
            new Gebied("z4_mysterie", 2446, -358, 2514, -300, 54, 56, false),
            new Gebied("z4_pierkop", 2446, -585, 2530, -545, 54, 56, false),
            new Gebied("z4_lampionkust", ZONE_MIN_X, ZONE_MIN_Z, ZONE_MAX_X, ZONE_MAX_Z, 50, 56, false));

    // ---- the Knuffeltij knuffels (story only, a FIXED number: 6 + 4 + 2 = 12) -------------------------------------------------------
    /** The 6 grey knuffels along the waterline of the Knuffeltij-strand (z4_hq02). */
    public static final Map<String, BlockPos> TIJKNUFFEL = new LinkedHashMap<>();
    /** The 4 big ones on other islands (z4_hq03): the quest POIs z4_grote_knuffel_1..4. */
    public static final Map<String, BlockPos> GROTE_KNUFFEL = new LinkedHashMap<>();
    /** The 2 stolen ones in the hold of the wreck (z4_hq05). */
    public static final Map<String, BlockPos> WRAKKNUFFEL = new LinkedHashMap<>();
    public static final int KNUFFELTIJ_TOTAAL = 12;

    // ---- side-quest finds (only while their quest runs) --------------------------------------------------------------------------
    public static final Map<String, BlockPos> VLIEGERSPUL = new LinkedHashMap<>();
    public static final Map<String, BlockPos> PARELS = new LinkedHashMap<>();
    public static final Map<String, BlockPos> SCHELPEN = new LinkedHashMap<>();
    public static final Map<String, BlockPos> BIJTJES = new LinkedHashMap<>();
    public static final Map<String, BlockPos> BLOEMEN = new LinkedHashMap<>();
    public static final Map<String, BlockPos> LAMPIONHAKEN = new LinkedHashMap<>();
    public static final Map<String, BlockPos> MISTBRIEVEN = new LinkedHashMap<>();

    // ---- secrets (always) ---------------------------------------------------------------------------------------------------------
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    public static final Map<String, BlockPos> FLESSENPOST = new LinkedHashMap<>();
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    public static final Map<String, BlockPos> UITZICHT = new LinkedHashMap<>();

    private static void vul(Map<String, BlockPos> m, String prefix, String poiPrefix, int[][] xyz) {
        for (int i = 0; i < xyz.length; i++) {
            m.put(prefix + (i + 1), w(poiPrefix + (i + 1), xyz[i][0], xyz[i][1], xyz[i][2]));
        }
    }

    static {
        vul(TIJKNUFFEL, "strand_", "tijknuffel_", new int[][] {{1905, 66, -313}, {1917, 66, -315}, {1929, 66, -313}, {1941, 66, -315}, {1953, 66, -313},
                {1965, 66, -316}});
        for (int i = 1; i <= 4; i++) {
            GROTE_KNUFFEL.put("groot_" + i, POI.get("z4_grote_knuffel_" + i));
        }
        vul(WRAKKNUFFEL, "wrak_", "wrak_knuffel_", new int[][] {{2163, 62, -699}, {2167, 62, -701}});
        vul(VLIEGERSPUL, "vliegerspul_", "vliegerspul_", new int[][] {{2280, 78, -675}, {2315, 82, -655}, {2295, 85, -680}, {2320, 75, -670}});
        vul(PARELS, "parel_", "parel_", new int[][] {{2175, 54, -560}, {2195, 53, -575}, {2170, 55, -585}, {2200, 54, -550}});
        vul(SCHELPEN, "schelp_", "schelp_", new int[][] {{1965, 66, -572}, {1980, 66, -570}, {1995, 66, -569}, {2010, 66, -570}, {2025, 66, -574},
                {1995, 66, -640}});
        vul(BIJTJES, "bijtje_", "bijtje_", new int[][] {{2070, 71, -420}, {2095, 71, -425}, {2100, 71, -395}, {2065, 71, -395}, {2085, 71, -380}});
        vul(BLOEMEN, "bloem_", "bloem_", new int[][] {{2075, 70, -385}, {2090, 70, -385}, {2105, 70, -390}, {2060, 70, -400}, {2110, 70, -410},
                {2070, 70, -375}});
        vul(LAMPIONHAKEN, "lampionhaak_", "lampionhaak_", new int[][] {{1868, 67, -478}, {1868, 67, -451}, {1862, 67, -490}, {1840, 67, -462},
                {1856, 67, -470}});
        vul(MISTBRIEVEN, "mistbrief_", "mistbrief_", new int[][] {{2465, 78, -340}, {2495, 78, -320}, {2485, 76, -345}});
        vul(KNUFFEL, "knuffel_", "geheim_knuffel_", new int[][] {{1830, 66, -500}, {1870, 85, -525}, {2210, 52, -560}, {2310, 86, -675},
                {2100, 70, -420}, {2243, 71, -449}, {2490, 78, -335}, {2475, 72, -570}});
        vul(FLESSENPOST, "fles_", "flessenpost_", new int[][] {{1975, 66, -330}, {1960, 66, -588}, {2040, 66, -412}, {1885, 66, -430},
                {2385, 66, -515}, {2460, 66, -315}});
        String[] sc = {"duintop", "lagune", "vliegertop", "mist"};
        int[][] scp = {{1858, 90, -520}, {2180, 51, -570}, {2298, 89, -667}, {2488, 78, -328}};
        for (int i = 0; i < sc.length; i++) {
            SCHAT.put(sc[i], w("geheim_schat_" + (i + 1), scp[i][0], scp[i][1], scp[i][2]));
        }
        UITZICHT.put("duin", POI.get("z4_lampionduin"));
        UITZICHT.put("vlieger", POI.get("z4_wedstrijdpaal"));
        UITZICHT.put("vuurtoren", POI.get("z4_vuurtoren_top"));
        UITZICHT.put("pierkop", POI.get("z4_pierkop"));
    }

    // ---- scenes (camera keyframes: pos, look-at; world coordinates) ----------------------------------------------------------------
    /** The whale (zone 5, t-z4wereld/zone 5 build it): head above the Opstijgpier, axis z -520, y 170-320. */
    public static final Vec3 WALVIS_KOP = new Vec3(2460, 240, -520), WALVIS_RUG = new Vec3(2740, 290, -520);

    /** Our first flight (fallback when t-luchtschip's line C is not in the tree): through the Wolkenval, over the islands. */
    public static final Vec3[] OPENING = {
            new Vec3(1690, 150, -480), new Vec3(1880, 70, -470),        // 0: west of the coast, the Wolkenval below
            new Vec3(1830, 110, -430), new Vec3(2100, 70, -480),        // 120: over Lampionhaven, the islands ahead
            new Vec3(2050, 140, -540), WALVIS_KOP,                      // 220: high over the middle: Hoogvads on the whale
            new Vec3(1975, 95, -300), new Vec3(1935, 66, -330),         // 320: the Knuffeltij-strand: grey spots in the surf
            new Vec3(1815, 78, -478), Vec3.atCenterOf(LANDING)};        // 400: down to the luchthaven platform
    public static final int[] OPENING_T = {0, 120, 220, 320, 400};

    /** The mist flash (z4_hq04): through the grey gate, east into the mist. */
    public static final Vec3 POORT = Vec3.atBottomCenterOf(POI.get("z4_mysterie_poort"));
    public static final Vec3[] MIST = {
            POORT.add(-10, 6, 10), POORT.add(0, 2, 0),
            POORT.add(4, 5, -2), POORT.add(60, 4, -20),
            POORT.add(30, 14, -10), POORT.add(220, 10, 60)};

    /** The party on the Knuffeltij-strand (z4_hq06), then Moeder Wolkwal descends. */
    public static final Vec3 FEEST = Vec3.atBottomCenterOf(POI.get("z4_feestplek"));
    public static final Vec3[] FEEST_SCENE = {
            FEEST.add(14, 6, 14), FEEST,
            FEEST.add(-14, 9, 12), FEEST.add(0, 3, 0),
            FEEST.add(-6, 4, -16), FEEST.add(0, 2, 0)};
    public static final Vec3[] WOLKWAL = {
            FEEST.add(4, 3, 6), new Vec3(2500, 230, -520),
            FEEST.add(10, 6, 4), new Vec3(2350, 160, -490)};

    /** The cliffhanger (z4_hq07): the first airship rises along the whale's head, Hoogvads on top, then down to a cape on the pier. */
    public static final Vec3 PIER = Vec3.atBottomCenterOf(POI.get("z4_opstijgpier"));
    public static final Vec3[] CLIFF = {
            PIER.add(-4, 4, 6), PIER.add(-40, 120, 40),
            PIER.add(10, 110, 30), WALVIS_KOP,
            PIER.add(80, 230, 40), WALVIS_RUG,
            PIER.add(-14, 6, -8), PIER.add(-24, 1.5, 0)};
    /** Where the caped figure stands at the end of the cliffhanger (the land end of the pier). */
    public static final Vec3 CAPE = PIER.add(-24, 0, 0);
}
