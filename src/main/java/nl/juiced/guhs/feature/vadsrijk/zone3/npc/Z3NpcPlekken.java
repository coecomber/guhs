package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-3 npc slice (Z3_BRIEF: one class). Zone 3 "De Schemerwortel" = x 1600..3000, z -700..700 in
 * guhs:vadsrijk, the trunk axis T = (2300, 0).
 * <p>Aligned with z3-wereld's POI_Z3.md v1 (the render design MIRRORED in x): Honingtak sits on the WEST branch (x 1634..2014,
 * pointing at zone 1/2, the mast at x ~1677), the riding branches (with Hangnest Dauwdrup and the Kaaszeppelinwerf) reach EAST,
 * Mosvoet lies in the south-west, the Mika-schimmelboerderij in the north-east. Every spot is {@code w("<POI_Z3 id>", fallback)}:
 * z3-wereld's generated {@code zone3.wereld.Z3WereldPlekken.plek(id)} wins when it is in the tree (reflection, so this tree
 * compiles without it); the numbers are copied from POI_Z3 v1. Requests: guhs_vadsrijk/POI_VERZOEKEN_Z3.md, section "npc".
 * <p>NPC/POI ids here are checked by tools/features/vadsrijk_z3_npc.py (every NPC needs a name + a "where" text there).
 */
public final class Z3NpcPlekken {
    private Z3NpcPlekken() {
    }

    // ---- z3-wereld's generated spots, when present ---------------------------------------------------------------------------
    @Nullable
    private static final Method WERELD = zoek();

    @Nullable
    private static Method zoek() {
        try {
            return Class.forName("nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken").getMethod("plek", String.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** z3-wereld's spot for this id, or null. */
    @Nullable
    public static BlockPos wereld(String id) {
        if (WERELD == null) {
            return null;
        }
        try {
            return WERELD.invoke(null, id) instanceof BlockPos b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    /** A z3-wereld spot moved a little (an NPC next to a campfire, not in it). */
    static BlockPos wo(String id, int dx, int dz, int x, int y, int z) {
        return w(id, x, y, z).offset(dx, 0, dz);
    }

    /** The trunk axis. */
    public static final int TX = 2300, TZ = 0;

    /** Is this x in zone 3 (the tracker's airship hop)? */
    public static boolean zone3(double x) {
        return x > 1500;
    }

    // ---- Honingtak (the hub town on the west branch) ---------------------------------------------------------------------------
    public static final BlockPos PLEIN = w("plein", 1839, 239, -10);
    /** Where the airship sets you down (the mast on the tip of the branch). */
    public static final BlockPos DOK = w("dok_landing", 1689, 239, 2);
    public static final BlockPos HONINGLIFT_BOVEN = w("honinglift_boven", 1907, 239, 24);
    public static final BlockPos HONINGLIFT_BENEDEN = w("honinglift_onder", 2042, 69, 400);

    // ---- the NPCs (id -> stand spot; they snap to the nearest floor within 12 blocks) -------------------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();

    static void npc(String id, BlockPos b) {
        NPC.put(id, b);
    }

    static {
        // Honingtak, travel + story
        npc("z3_kapitein", w("dok_kapitein", 1685, 239, -3));
        npc("z3_raatje", w("npc_raatje_plein", 1823, 239, -8));
        npc("z3_wasja", w("gebouw.nestkapel.npc", 1794, 182, -6));
        npc("z3_veertje", w("gebouw.pluimstal.npc", 1973, 236, 43));
        npc("z3_cafe", w("gebouw.cafe.npc", 1871, 182, -6));
        npc("z3_jager", w("gebouw.jagershut.npc", 1715, 214, -18));
        // shops and services
        npc("z3_smid", w("gebouw.smid.npc", 1887, 212, -26));
        npc("z3_juwelier", w("gebouw.juwelier.npc", 1859, 222, 19));
        npc("z3_winkel", w("gebouw.drankjes.npc", 1841, 218, -25));
        npc("z3_wapens", w("gebouw.uitrusting.npc", 1807, 214, 18));
        npc("z3_kleermaker", w("gebouw.algemeen.npc", 1779, 224, -22));
        npc("z3_markt", w("npc_kraam_3n", 1877, 239, -18));
        npc("z3_stalguh", w("gebouw.pluimstal.voer", 1983, 236, 43));
        npc("z3_liftguh", wo("honinglift_boven", 2, 2, 1907, 239, 24));
        // citizens
        npc("z3_opa_bast", wo("knuffelbeeld", 2, 3, 1829, 239, -2));
        npc("z3_zoemer", wo("plein", -3, 4, 1839, 239, -10));
        npc("z3_honingbij", w("npc_kraam_1z", 1901, 239, 13));
        npc("z3_raatwever", w("gebouw.woning_5.npc", 1733, 230, 20));
        npc("z3_bloesem", wo("bloesemboompje", 2, 2, 1751, 239, 9));
        npc("z3_druppel", w("gebouw.cafe.podium", 1870, 182, 2));
        npc("z3_lampion", w("gebouw.woning_7.npc", 1789, 230, 20));
        npc("z3_imker_1", w("gebouw.woning_1.npc", 1911, 216, 21));
        npc("z3_imker_2", w("gebouw.woning_3.npc", 1869, 228, -28));
        npc("z3_imker_3", w("gebouw.woning_6.npc", 1823, 230, -26));
        npc("z3_kuikenhoeder", w("pluimstal_oefenbaan", 1943, 236, 66));
        npc("z3_kind_1", w("npc_kraam_2z", 1889, 239, 12));
        npc("z3_kind_2", w("npc_kraam_4z", 1865, 239, 12));
        npc("z3_postguh", wo("questbord", 2, 2, 1841, 239, -5));
        // out in the zone
        npc("z3_bladwachter", wo("kampvuur_kruin_3", 2, 2, 2300, 255, -230));
        npc("z3_lusje", w("hangnest_lusje", 2807, 144, 27));
        npc("z3_herbergier", w("hangnest_herberg", 2801, 137, 47));
        npc("z3_knoest", w("npc_oude_knoest", 2258, 129, 37));
        npc("z3_wortelien", w("mosvoet_burgemeester", 2087, 67, 430));
        npc("z3_molenaar", wo("mosvoet_waterrad", 2, 0, 2029, 68, 446));
        npc("z3_theeguh", w("mosvoet_theehuis", 2079, 68, 394));
        npc("z3_gloei", wo("kampvuur_hartslag", 2, 2, 2269, 13, 20));
        npc("z3_nestwachter", w("hart_nestkamer", 2293, 50, 0));
        // the Honingwacht: a guard at the main campfires + two on the plaza
        npc("z3_wacht_plein_1", wo("plein", -6, -4, 1839, 239, -10));
        npc("z3_wacht_plein_2", wo("plein", 6, -4, 1839, 239, -10));
        npc("z3_wacht_kruin_1", wo("kampvuur_kruin_1", 2, 2, 2044, 255, 147));
        npc("z3_wacht_kruin_2", wo("kampvuur_kruin_nest", 2, 2, 2265, 282, -10));
        npc("z3_wacht_tak_1", wo("kampvuur_tak_west_30", 2, 2, 2568, 225, 5));
        npc("z3_wacht_tak_2", wo("kampvuur_tak_noordwest_60", 2, 2, 2704, 197, -405));
        npc("z3_wacht_spiraal", wo("kampvuur_schorsstad_ring4", 2, 2, 2276, 129, 59));
        npc("z3_wacht_wortel_1", wo("kampvuur_wortelweg_5", 2, 2, 2464, 68, -452));
        npc("z3_wacht_wortel_2", wo("kampvuur_wortelweg_4", 2, 2, 2772, 66, -84));
        npc("z3_wacht_wortel_3", wo("kampvuur_wortelweg_3", 2, 2, 2608, 67, 367));
        npc("z3_wacht_wortel_4", wo("kampvuur_wortelweg_2", 2, 2, 2135, 67, 451));
        npc("z3_wacht_gewelf", wo("kampvuur_wortelgewelf", 2, 2, 1956, 73, -147));
    }

    // ---- POIs: quest targets (radius POI_STRAAL, height POI_HOOGTE) --------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    public static final int POI_STRAAL = 6, POI_HOOGTE = 8, VIND_STRAAL = 3, VIND_HOOGTE = 4;

    static void poi(String id, BlockPos b) {
        POI.put(id, b);
    }

    static {
        poi("z3_plein", PLEIN);
        poi("z3_dok", DOK);
        poi("z3_pluimstal", w("gebouw.pluimstal.binnen", 1973, 237, 34));
        poi("z3_zweefplatform", w("pluimstal_zweefplatform", 1997, 236, 102));
        poi("z3_nestkapel_balkon", w("flash_f2_kijkpunt", 1794, 182, 4));
        poi("z3_raattrossen", w("gebouw.raattros.binnen", 1731, 191, -1));
        // Kruinzee
        poi("z3_vlek_1", w("grauwe_vlek_1", 2149, 264, 120));
        poi("z3_vlek_2", w("grauwe_vlek_2", 2409, 253, 205));
        poi("z3_vlek_3", w("grauwe_vlek_3", 2239, 254, -250));
        poi("z3_zeppelinsteiger", w("mika_bevel", 2495, 258, -204));
        poi("z3_kruintop", w("nest_rand", 2299, 283, -18));
        poi("z3_nestsplinter", w("hartsplinter_2", 2313, 280, -49));
        poi("z3_kale_kruin", w("arena_kruinroofvogel", 2129, 238, -170));
        // Takkenbanen
        poi("z3_hangnest", w("hangnest_plein", 2783, 140, 37));
        poi("z3_kaaspost_1", w("kaasschutterpost_1", 2640, 222, 24));
        poi("z3_kaaspost_2", w("kaasschutterpost_2", 2620, 219, -314));
        poi("z3_kaaspost_3", w("kaasschutterpost_3", 2665, 207, 337));
        poi("z3_werf", w("werf_ingang", 2861, 169, -537));
        poi("z3_kajuit", w("roquefort_kajuit", 2887, 111, -570));
        // Schorsstad
        poi("z3_spiraalpoort", w("spiraal_poort_128", 2231, 129, 77));
        poi("z3_lantaarntoren", w("lantaarnring_1", 2299, 84, 5));
        for (int i = 1; i <= 7; i++) {
            poi("z3_ring_" + i, w("lantaarnring_" + i, 2299, 68 + 16 * i, 5));
        }
        poi("z3_pelszaal", w("arena_piepvel", 2299, 184, 0));
        poi("z3_capespeld", w("hartsplinter_4", 2297, 184, 3));
        poi("z3_vingerafdruk", w("vingerafdruk", 2240, 172, 30));
        // Wortelvelden
        poi("z3_mosvoet", w("mosvoet_plein", 2049, 69, 424));
        poi("z3_ketel_1", w("schimmelketel_1", 2610, 67, -280));
        poi("z3_ketel_2", w("schimmelketel_2", 2570, 73, -330));
        // Wortelduister + the Hart
        poi("z3_wortelpoort", w("wortelpoort", 2299, 71, 282));
        poi("z3_hartslagzaal", w("hartslag_zaal", 2299, 13, 30));
        int[][] gl = {{2294, 13, -55}, {2294, 13, -46}, {2305, 13, -46}, {2305, 13, -55}};
        for (int i = 1; i <= 4; i++) {
            poi("z3_grotlantaarn_" + i, w("hart_grotlantaarn_" + i, gl[i - 1][0], gl[i - 1][1], gl[i - 1][2]));
        }
        poi("z3_hartpoort", w("hart_poort", 2299, 13, -50));
        poi("z3_nestkamer", w("hart_nestkamer", 2293, 50, 0));
        // lookouts (side quest "Lusje's kaartjes")
        poi("z3_uitkijk_kroon", w("uitkijk_kroon", 2449, 276, 150));
        poi("z3_uitkijk_takpunt", w("uitkijk_takpunt", 2851, 161, 492));
        poi("z3_uitkijk_spiraal", w("spiraal_balkon_1", 2209, 191, -35));
        poi("z3_uitkijk_gewelf", w("uitkijk_wortelgewelf", 1957, 114, -148));
        poi("z3_uitkijk_mast", w("dok_mast", 1677, 239, 0));
    }

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    /** Area id -> where the tracker points for "in area x". */
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    /** Area id -> its main campfire. */
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    /** Arena id -> its door (the tracker's target for boss objectives). */
    public static final Map<String, BlockPos> BAAS = new LinkedHashMap<>();

    static {
        GEBIED_DOEL.put("z3_honingtak", PLEIN);
        GEBIED_DOEL.put("z3_kruinzee", w("kampvuur_kruin_2", 2442, 265, 82));
        GEBIED_DOEL.put("z3_takkenbanen", w("pluimpost_westtak", 2690, 210, 36));
        GEBIED_DOEL.put("z3_schorsstad", w("schorsstad_vloer", 2279, 76, 30));
        GEBIED_DOEL.put("z3_wortelvelden", w("pluimpost_3", 2653, 69, 359));
        GEBIED_DOEL.put("z3_wortelduister", w("kampvuur_wortelgang_2", 2207, 31, 126));
        GEBIED_DOEL.put("z3_zeppelinwerf", POI.get("z3_werf"));
        GEBIED_DOEL.put("z3_hart", POI.get("z3_hartpoort"));
        KAMPVUUR.put("z3_honingtak", PLEIN);
        KAMPVUUR.put("z3_kruinzee", w("kampvuur_kruin_3", 2300, 255, -230));
        KAMPVUUR.put("z3_takkenbanen", w("kampvuur_tak_west_30", 2568, 225, 5));
        KAMPVUUR.put("z3_schorsstad", w("kampvuur_schorsstad_ring4", 2276, 129, 59));
        KAMPVUUR.put("z3_wortelvelden", w("kampvuur_mosvoet", 2055, 69, 414));
        KAMPVUUR.put("z3_wortelduister", w("kampvuur_hartslag", 2269, 13, 20));
        BAAS.put("z3_zeppelinwerf", POI.get("z3_werf"));
        BAAS.put("z3_pelszaal", POI.get("z3_pelszaal"));
        BAAS.put("z3_hart", POI.get("z3_hartpoort"));
        BAAS.put("z3_kruintop", POI.get("z3_kruintop"));
        BAAS.put("z3_kale_kruin", POI.get("z3_kale_kruin"));
    }

    /** Our fallback area boxes (flat; z3-wereld registers the y-aware ones): only the ones that do not overlap in x/z. */
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLv, int maxLv, boolean stad) {
    }

    public static final List<Gebied> GEBIEDEN = List.of(
            new Gebied("z3_honingtak", 1634, -100, 2014, 110, 30, 50, true),
            new Gebied("z3_zeppelinwerf", 2819, -660, 2979, -480, 38, 40, false),
            new Gebied("z3_ring", 1600, -700, 3000, 700, 30, 50, false));

    // ---- secrets ---------------------------------------------------------------------------------------------------------------
    /** 10 lost knuffels; 3 only by gliding or high branches. */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    /** 5 clues to the Mika-imker (all in Honingtak). */
    public static final Map<String, BlockPos> AANWIJZING = new LinkedHashMap<>();
    /** 5 treasures. */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    /** 8 boomvolk reliefs in the Schorsstad wall (het Muurverhaal). */
    public static final Map<String, BlockPos> RELIEF = new LinkedHashMap<>();
    /** 6 lost Pluimkuikens in the Kruinzee (side quest; by the honey ponds: requested as POI kuiken_1..6). */
    public static final Map<String, BlockPos> KUIKEN = new LinkedHashMap<>();
    /** 4 sleeping boomvolk lights in the Schorsstad (side quest). */
    public static final Map<String, BlockPos> LICHTJE = new LinkedHashMap<>();

    static {
        int[][] kn = {{2599, 230, 70}, {2099, 253, -120}, {2839, 137, 470}, {2909, 141, -540}, {2179, 215, 300}, {1999, 67, 360}, {2299, 68, -540},
                {2729, 66, 420}, {2337, 161, 51}, {2359, 24, 120}};
        for (int i = 0; i < kn.length; i++) {
            KNUFFEL.put("knuffel_" + (i + 1), w("geheim_knuffel_" + (i + 1), kn[i][0], kn[i][1], kn[i][2]));
        }
        int[][] sc = {{2814, 140, 465}, {2299, 286, -78}, {2342, 177, -50}, {2439, 22, 180}, {1889, 67, 270}};
        String[] scId = {"onder_de_tak", "nesttop", "schorsstad", "wortelduister", "moeras"};
        for (int i = 0; i < sc.length; i++) {
            SCHAT.put(scId[i], w("geheim_schat_" + (i + 1), sc[i][0], sc[i][1], sc[i][2]));
        }
        int[][] aw = {{1847, 239, -12}, {1699, 226, 30}, {1739, 196, 8}, {1853, 239, 12}, {1728, 191, 2}};
        String[] awId = {"rookpotjes", "imkerpak", "koningin", "grauwe_honing", "luik"};
        for (int i = 0; i < aw.length; i++) {
            AANWIJZING.put(awId[i], w("imker_aanwijzing_" + (i + 1), aw[i][0], aw[i][1], aw[i][2]));
        }
        int[][] r = {{2235, 81, 9}, {2261, 97, 52}, {2309, 113, 64}, {2352, 129, 38}, {2364, 145, -10}, {2338, 161, -53}, {2290, 177, -65},
                {2247, 81, -39}};
        for (int i = 0; i < r.length; i++) {
            RELIEF.put("relief_" + (i + 1), w("muurverhaal_" + (i + 1), r[i][0], r[i][1], r[i][2]));
        }
        int[][] ku = {{2160, 264, -87}, {2426, 265, 63}, {2222, 273, 173}, {2344, 271, -197}, {2073, 267, 93}, {2537, 232, 218}};
        for (int i = 0; i < ku.length; i++) {
            KUIKEN.put("kuiken_" + (i + 1), w("kuiken_" + (i + 1), ku[i][0], ku[i][1], ku[i][2]));
        }
        int[][] li = {{2245, 100, 36}, {2343, 132, 48}, {2342, 164, -50}, {2245, 84, -37}};
        for (int i = 0; i < li.length; i++) {
            LICHTJE.put("lichtje_" + (i + 1), w("boomvolk_lichtje_" + (i + 1), li[i][0], li[i][1], li[i][2]));
        }
    }

    // ---- scenes (camera keyframes: pos, look-at; world coordinates) ------------------------------------------------------------
    /**
     * The opening (z3_hq01; our version, used when z3-wereld's Z3Reizen hands the first arrival to us or is not in the tree): along
     * the endless bark wall, up through the clouds into the golden crown, the beekeepers of Honingtak waving, then down along the
     * trunk into the roots where two grey-blue eyes open. The player waits under the camera (OPENING_ANKER) so the chunks load.
     */
    public static final Vec3[] OPENING = {
            new Vec3(2150, 130, 140), new Vec3(2240, 170, 40),        // 0: low along the bark wall
            new Vec3(2160, 215, 150), new Vec3(2250, 240, 40),        // 120: higher, into the clouds
            new Vec3(2120, 300, 120), new Vec3(2300, 275, -30),       // 200: through the clouds: the crown in gold
            new Vec3(1880, 262, 40), new Vec3(1840, 240, -10),        // 270: Honingtak, beekeepers waving
            new Vec3(2200, 120, 110), new Vec3(2300, 40, 0),          // 340: down along the trunk into the dusk
            new Vec3(2280, 24, 40), new Vec3(2299, 20, -10)};         // 420: deep in the roots: two eyes open
    public static final int[] OPENING_T = {0, 120, 200, 270, 340, 420};
    /** Where the player waits during the parts of the opening: by the nest (t 0), on the plaza (t 260), in the Hartslag-zaal (t 335). */
    public static final BlockPos[] OPENING_ANKER = {w("nest_rand", 2299, 283, -18), PLEIN, w("hartslag_zaal", 2299, 13, 30)};
    /** The eyes in the dark (the Schimmelkolos). */
    public static final Vec3 OGEN = new Vec3(2299, 20, -10);

    /** The small BAM (z3_hq02): a Mika zeppelin over the market. */
    public static final Vec3 ZEPPELIN_VAN = new Vec3(1960, 262, -60), ZEPPELIN_NAAR = new Vec3(1780, 258, 40);
    public static final Vec3[] BAM = {
            new Vec3(1850, 246, 20), new Vec3(1890, 258, -40),
            new Vec3(1830, 246, -16), new Vec3(1800, 256, 30)};

    /** The old nest flashback (z3_hq05): Vahoegnir, still pink, lays a glowing egg and wraps his wings around it. */
    public static final Vec3 NEST = Vec3.atBottomCenterOf(w("nest_eierkuil", 2299, 280, -40));
    public static final Vec3[] NEST_SCENE = {
            NEST.add(26, 14, 26), NEST,
            NEST.add(10, 30, 40), NEST.add(0, 2, 0),
            NEST.add(-18, 8, 18), NEST.add(0, 3, 0)};
    public static final Vec3[] NEST_DRAAK = {NEST.add(160, 60, -90), NEST.add(40, 30, -20), NEST.add(0, 6, 0), NEST.add(0.01, 6, 0.3)};

    /** Flash F1 (z3_hq10): the long grey arm through a knot hole grabs the Count. */
    public static final Vec3 PELSZAAL = Vec3.atBottomCenterOf(POI.get("z3_pelszaal"));
    public static final Vec3 KWASTGAT = Vec3.atCenterOf(w("flash_f1_kwastgat", 2234, 178, 23));
    public static final Vec3[] FLASH_F1 = {
            PELSZAAL.add(10, 4, 12), PELSZAAL.add(0, 1, 0),
            PELSZAAL.add(14, 6, 10), KWASTGAT,
            PELSZAAL.add(6, 3, 8), PELSZAAL.add(0, 1.5, 0)};

    /** The egg in the Nestkamer (z3_hq13). */
    public static final Vec3 EI = Vec3.atBottomCenterOf(w("ei", 2299, 50, 0));

    /** The escape ride (z3_hq14): up the Stamspiraal outside the trunk, the dragon skims the bark; ends at the old nest. */
    public static final Vec3[] RIT = {
            new Vec3(2233, 75, 100), new Vec3(2300, 100, 140),
            Vec3.atCenterOf(w("spiraal_balkon_3", 2400, 111, 12)).add(6, 4, 6), new Vec3(2300, 130, 0),
            Vec3.atCenterOf(w("spiraal_balkon_2", 2322, 151, -93)).add(4, 4, -8), new Vec3(2220, 170, -60),
            Vec3.atCenterOf(w("spiraal_balkon_1", 2209, 191, -35)).add(-8, 4, 0), Vec3.atCenterOf(w("spiraal_top", 2299, 201, -100)),
            NEST.add(20, 10, 20), NEST};
    public static final Vec3[] RIT_DRAAK = {new Vec3(2500, 260, 200), new Vec3(2400, 180, 160), new Vec3(2250, 210, -120), NEST.add(0, 20, -10)};

    /** The finale (z3_hq14 end): the egg hatches; the beam to Hoogvads in the far east. */
    public static final Vec3[] FINALE = {
            NEST.add(9, 4, 9), NEST.add(0, 1, 0),
            NEST.add(-14, 8, 4), NEST.add(0, 6, 0),
            NEST.add(-8, 16, 4), NEST.add(400, 120, 40)};
    public static final Vec3 HOOGVADS = Vec3.atCenterOf(w("landmark_hoogvads_richting", 2999, 260, 0)).add(300, 60, 0);

    /** Flash F2 (z3_hq15): the throne room, far away (a private set high above the far north-east corner of the zone, never walked). */
    public static final Vec3 TROON = new Vec3(2990, 300, -680);
    public static final Vec3[] FLASH_F2 = {
            TROON.add(0, 3, 14), TROON.add(0, 2, 0),
            TROON.add(-3, 2, 6), TROON.add(0, 1, -2),
            TROON.add(4, 4, 4), TROON.add(-6, 1, -6)};

    /** The cliffhanger (z3_hq15): from the Nestkapel balcony to the far east. */
    private static final Vec3 BALKON = Vec3.atBottomCenterOf(w("flash_f2_kijkpunt", 1794, 182, 4));
    public static final Vec3[] CLIFFHANGER = {BALKON.add(-4, 2, -6), BALKON.add(20, 0, 0), BALKON.add(6, 40, -10), new Vec3(3800, 330, 0)};

    /** The airship to Honingtak (fallback when z3-wereld has no line): over the cloud sea from the west to the mast. */
    public static final Vec3[] VLUCHT = {
            new Vec3(1430, 280, 80), new Vec3(1890, 260, -20),
            new Vec3(1620, 250, 30), new Vec3(1840, 240, 0),
            new Vec3(1682, 242, 2), new Vec3(1840, 240, 0)};
}
