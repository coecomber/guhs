package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-3 npc slice (Z3_BRIEF: one class). Zone 3 "De Schemerwortel" (REVAMP, ronde 20) = x 1900..2700,
 * z -400..400 in guhs:vadsrijk, the trunk axis T = (2300, 0), NOT mirrored (world x = 2300 + rel x).
 * <p>Aligned with POI_Z3.md v2 (P-WERELD): a narrow tall tree in 4 layers on one axis: the roots (Wortelduister + the Hart, the
 * Wortelpoort east on the ground), the hollow trunk (the Schorsstad round the inner stair, the Pelszaal under its floor), the
 * branches (Honingtak = a ring town round the trunk at y 166 with the Honingplein in the south; 3 beekeeper villages on big
 * branches: Zoemrust = the old {@code hangnest_*} ids, Raatweide, Molenhoek) and the crown (3 stepped storeys, the nest on top).
 * The Kaaszeppelinwerf is its own island in the north-east (cable bridge from Honingtak). Every spot is
 * {@code w("<POI_Z3 id>", fallback)}; the numbers are v2 copies, only used for an id the map lacks
 * ({@code tools/vadsrijk/zone3/poicode3.py} checks that none is missing). Requests: guhs_vadsrijk/POI_VERZOEKEN_Z3.md.
 * <p>NPC/POI ids here are checked by tools/features/vadsrijk_z3_npc.py (every NPC needs a name + a "where" text there).
 */
public final class Z3NpcPlekken {
    private Z3NpcPlekken() {
    }

    // ---- z3-wereld's generated spots (K-INT: a direct reference now; the numbers below are only used for an id it lacks) -----

    /** z3-wereld's spot for this id, or null. */
    @Nullable
    public static BlockPos wereld(String id) {
        return nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek(id);
    }

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b : v2(x, y, z);
    }

    /** The v2 frame: the fallback numbers in this class are written for the trunk at (2300, 0); shifted to the map's trunk. */
    public static final int V2_X = 2300, V2_Z = 0;

    /** A v2-frame block position on the map as it is now (zone 3 may move: P-VERHUIS, the lead moves the trunk). */
    public static BlockPos v2(int x, int y, int z) {
        return new BlockPos(x - V2_X + TX, y, z - V2_Z + TZ);
    }

    /** A v2-frame point on the map as it is now. */
    public static Vec3 v2(double x, double y, double z) {
        return new Vec3(x - V2_X + TX, y, z - V2_Z + TZ);
    }

    /** A z3-wereld spot moved a little (an NPC next to a campfire, not in it). */
    static BlockPos wo(String id, int dx, int dz, int x, int y, int z) {
        return w(id, x, y, z).offset(dx, 0, dz);
    }

    /** The trunk axis (from the map: zone 3 can move). */
    public static final int TX = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X, TZ = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z;

    // ---- (P-VERHUIS) the outer riding spiral (boom2.py RIJ: y 82 -> 236, 34 per turn, meets the Honingplein at y 166, south) -----
    public static final double RIJ_Y0 = 82, RIJ_STIJG = 34, RIJ_EIND = 232, RIJ_BREED = 9, HONING_Y = 166;

    /** The outer trunk radius at level y and angle th (boom2.py r_stam: tapering, 7 lobes at the foot, bark ribs). */
    static double rStam(double y, double th) {
        double basis = 36 - 8 * Math.max(0, Math.min(1, (y - 90) / 200.0));
        double lob = Math.pow(0.55 + 0.45 * Math.cos(7 * (th - 0.3)), 2);
        double voet = 30 * Math.exp(-Math.max(0, y - 74) / 14.0) * lob;
        double rib = 1 + 0.045 * Math.sin(11 * th + y / 26.0) + 0.025 * Math.sin(23 * th - y / 15.0);
        return (basis + voet) * rib;
    }

    /**
     * The riding spiral as points (middle of the path, stand height) from the ground start (82) via the Honingplein (166, south)
     * up to kruin 1 (232), every ~3 blocks: the same formula as the map (boom2.py rij_punt_lev, frac 0.5). The map's PADEN "rijpad"
     * is a coarse x/z outline only; when it ever carries y (x, y, z) that list wins.
     */
    public static List<Vec3> rijpad() {
        List<Vec3> l = new java.util.ArrayList<>();
        int[][] kaart = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.PADEN.get("rijpad");
        if (kaart != null && kaart.length > 1 && kaart[0].length >= 3) {
            for (int[] q : kaart) {
                l.add(new Vec3(q[0] + 0.5, q[1], q[2] + 0.5));
            }
            return l;
        }
        double th0 = Math.PI / 2 - 2 * Math.PI * (((HONING_Y - RIJ_Y0) / RIJ_STIJG) % 1.0);
        double slagen = (RIJ_EIND - RIJ_Y0) / RIJ_STIJG;
        int n = (int) (slagen * 2 * Math.PI * 42 / 3);
        for (int i = 0; i <= n; i++) {
            double t = slagen * i / n, th = th0 + 2 * Math.PI * t, lev = RIJ_Y0 + RIJ_STIJG * t;
            double r = rStam(lev, th - lev / 160.0) - 2.5 + 0.5 + (RIJ_BREED - 1.5) * 0.5;
            double stand = Math.floor(2 * (lev + 1) + 1e-6) / 2;
            l.add(new Vec3(TX + r * Math.cos(th), stand, TZ + r * Math.sin(th)));
        }
        return l;
    }

    /** Is this point in zone 3 (the tracker's airship hop)? One shared lookup: the map's zone box. */
    public static boolean zone3(double x, double z) {
        return nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.inZone3(x, z);
    }

    // ---- Honingtak (the hub town on the west branch) ---------------------------------------------------------------------------
    public static final BlockPos PLEIN = w("plein", 2292, 167, 51);
    /** Where the airship sets you down (the mast at the tip of the Imkerstraat, north-west). */
    public static final BlockPos DOK = w("dok_landing", 2215, 167, -88);
    public static final BlockPos HONINGLIFT_BOVEN = w("honinglift_boven", 2405, 167, 53);
    public static final BlockPos HONINGLIFT_BENEDEN = w("honinglift_onder", 2405, 70, 59);

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
        npc("z3_lampion", w("gebouw.woning_2.npc", 2262, 167, 46));
        npc("z3_imker_1", w("gebouw.woning_1.npc", 1911, 216, 21));
        npc("z3_imker_2", w("gebouw.woning_3.npc", 1869, 228, -28));
        npc("z3_imker_3", w("gebouw.woning_6.npc", 1823, 230, -26));
        npc("z3_kuikenhoeder", w("pluimstal_oefenbaan", 1943, 236, 66));
        npc("z3_kind_1", w("npc_kraam_2z", 1889, 239, 12));
        npc("z3_kind_2", w("npc_kraam_4z", 1865, 239, 12));
        npc("z3_postguh", wo("questbord", 2, 2, 1841, 239, -5));
        // out in the zone
        npc("z3_bladwachter", wo("kampvuur_kruin_1", -2, -2, 2399, 230, 42));   // kruin 1, east: near the Mika steiger (NE)
        npc("z3_lusje", w("hangnest_lusje", 2807, 144, 27));
        npc("z3_herbergier", w("hangnest_herberg", 2801, 137, 47));
        npc("z3_knoest", w("npc_oude_knoest", 2258, 129, 37));
        npc("z3_wortelien", w("mosvoet_burgemeester", 2087, 67, 430));
        npc("z3_molenaar", wo("mosvoet_waterrad", 2, 0, 2029, 68, 446));
        npc("z3_theeguh", w("mosvoet_theehuis", 2079, 68, 394));
        npc("z3_gloei", wo("kampvuur_hartslag", 2, 2, 2269, 13, 20));
        npc("z3_nestwachter", w("hart_nestkamer", 2309, 21, 0));
        // the Honingwacht: a guard at the main campfires + two on the plaza
        npc("z3_wacht_plein_1", wo("plein", -6, -4, 1839, 239, -10));
        npc("z3_wacht_plein_2", wo("plein", 6, -4, 1839, 239, -10));
        npc("z3_wacht_kruin_1", wo("kampvuur_kruin_2", 2, 2, 2256, 261, -50));
        npc("z3_wacht_kruin_2", wo("kampvuur_kruin_nest", 2, 2, 2323, 285, 29));
        npc("z3_wacht_tak_1", wo("kampvuur_tak_west_30", 2, 2, 2568, 225, 5));
        npc("z3_wacht_tak_2", wo("kampvuur_tak_noordwest_60", 2, 2, 2704, 197, -405));
        npc("z3_wacht_spiraal", wo("kampvuur_schorsstad_ring4", 2, 2, 2276, 129, 59));
        npc("z3_wacht_wortel_1", wo("kampvuur_wortelweg_5", 2, 2, 2464, 68, -452));
        npc("z3_wacht_wortel_2", wo("kampvuur_wortelweg_4", 2, 2, 2772, 66, -84));
        npc("z3_wacht_wortel_3", wo("kampvuur_wortelweg_3", 2, 2, 2608, 67, 367));
        npc("z3_wacht_wortel_4", wo("kampvuur_wortelweg_2", 2, 2, 2135, 67, 451));
        npc("z3_wacht_gewelf", wo("kampvuur_wortelgewelf", 2, 2, 2261, 66, -163));
        // (P-VERHUIS) visible life: the two other beekeeper villages, travellers at the ground camps + the kruin-1 camp, guhs on bridges
        npc("z3_imker_raatweide", wo("raatweide_plein", 2, 1, 2248, 206, 72));
        npc("z3_imker_molenhoek", wo("molenhoek_plein", 2, 1, 2280, 187, -86));
        npc("z3_reiziger_1", wo("kamp.1", 2, 0, 2272, 68, 172));
        npc("z3_reiziger_2", wo("kamp.2", 2, 0, 2137, 62, -79));
        npc("z3_reiziger_3", wo("kamp.3", -2, 0, 2425, 66, -108));
        npc("z3_reiziger_4", wo("kampvuur_kruin_1", 2, -3, 2399, 230, 42));
        List<BlockPos> brug = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.BRUGGEN;
        int[] kies = {0, 4, 7, 9, 11, 13};
        int[][] bv = {{2345, 198, 44}, {2241, 207, 69}, {2287, 188, -89}, {2360, 167, 33}, {2252, 167, -52}, {2456, 161, -153}};
        for (int i = 0; i < kies.length; i++) {
            npc("z3_brug_" + (i + 1), kies[i] < brug.size() ? brug.get(kies[i]) : v2(bv[i][0], bv[i][1], bv[i][2]));
        }
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
        poi("z3_zweefplatform", w("zweefplatform_1", 2330, 224, 112));   // kruin 1, south edge (glide down over Honingtak)
        poi("z3_rijpad_start", w("rijpad_start", 2319, 84, -42));
        poi("z3_rijpad_eind", w("rijpad_eind", 2313, 232, 28));
        poi("z3_honingdeur", w("spiraal_poort_176", 2300, 167, 45));   // the stair door on the Honingplein
        poi("z3_schorsstad_vloer", w("trap_bordes_1", 2285, 81, 14));
        poi("z3_pelstrap", w("pelszaal_trap_boven", 2288, 81, -18));
        poi("z3_kabelbrug", w("kabelbrug_begin", 2407, 167, -100));
        poi("z3_nestkapel_balkon", w("flash_f2_kijkpunt", 2292, 152, -54));
        poi("z3_raattrossen", w("gebouw.raattros.binnen", 2354, 157, 9));
        // Kruinzee
        poi("z3_vlek_1", w("grauwe_vlek_1", 2390, 228, 51));
        poi("z3_vlek_2", w("grauwe_vlek_2", 2246, 259, 44));
        poi("z3_vlek_3", w("grauwe_vlek_3", 2261, 226, -106));
        poi("z3_zeppelinsteiger", w("mika_bevel", 2349, 228, -92));
        poi("z3_kruintop", w("nest_rand", 2296, 294, -13));
        poi("z3_nestsplinter", w("hartsplinter_2", 2293, 294, 6));
        poi("z3_kale_kruin", w("arena_kruinroofvogel", 2155, 226, 46));
        // Takkenbanen
        poi("z3_hangnest", w("hangnest_plein", 2351, 199, 70));
        poi("z3_kaaspost_1", w("kaasschutterpost_1", 2199, 212, -40));
        poi("z3_kaaspost_2", w("kaasschutterpost_2", 2395, 195, -11));
        poi("z3_kaaspost_3", w("kaasschutterpost_3", 2232, 212, 107));
        poi("z3_werf", w("werf_ingang", 2505, 161, -206));
        poi("z3_kajuit", w("roquefort_kajuit", 2591, 224, -251));
        // Schorsstad
        poi("z3_spiraalpoort", w("spiraal_poort_176", 2300, 167, 45));
        poi("z3_lantaarntoren", w("lantaarnring_1", 2303, 93, 0));
        for (int i = 1; i <= 7; i++) {
            poi("z3_ring_" + i, w("schorsstad_ring_" + i, 2300, 85 + 10 * i, 20));   // (revamp) the stair spot beside ring i (always reachable)
        }
        poi("z3_pelszaal", w("arena_piepvel", 2300, 61, 0));
        poi("z3_capespeld", w("hartsplinter_4", 2303, 61, 4));
        poi("z3_vingerafdruk", w("vingerafdruk", 2319, 130, 12));
        // Wortelvelden
        poi("z3_mosvoet", w("mosvoet_plein", 2427, 66, 92));
        poi("z3_ketel_1", w("schimmelketel_1", 2168, 68, -64));
        poi("z3_ketel_2", w("schimmelketel_2", 2141, 69, -44));
        // Wortelduister + the Hart
        poi("z3_wortelpoort", w("wortelpoort", 2446, 70, 0));
        poi("z3_hartslagzaal", w("hartslag_zaal", 2352, 23, 0));
        int[][] gl = {{2294, 13, -55}, {2294, 13, -46}, {2305, 13, -46}, {2305, 13, -55}};
        for (int i = 1; i <= 4; i++) {
            poi("z3_grotlantaarn_" + i, w("hart_grotlantaarn_" + i, gl[i - 1][0], gl[i - 1][1], gl[i - 1][2]));
        }
        poi("z3_hartpoort", w("hart_poort", 2223, 31, 0));
        poi("z3_nestkamer", w("hart_nestkamer", 2309, 21, 0));
        // lookouts (side quest "Lusje's kaartjes")
        poi("z3_uitkijk_kroon", w("uitkijk_kroon", 2365, 270, 46));
        poi("z3_uitkijk_takpunt", w("uitkijk_takpunt", 2163, 217, -55));
        poi("z3_uitkijk_spiraal", w("spiraal_balkon_1", 2256, 109, -16));
        poi("z3_uitkijk_gewelf", w("uitkijk_wortelgewelf", 2246, 81, -167));
        poi("z3_uitkijk_mast", w("dok_mast", 2209, 167, -94));
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
        GEBIED_DOEL.put("z3_kruinzee", w("kampvuur_kruin_2", 2256, 261, -50));
        GEBIED_DOEL.put("z3_takkenbanen", w("pluimpost_westtak", 2234, 209, -24));
        GEBIED_DOEL.put("z3_schorsstad", w("schorsstad_vloer", 2309, 81, -8));
        GEBIED_DOEL.put("z3_wortelvelden", w("pluimpost_3", 2199, 66, 144));
        GEBIED_DOEL.put("z3_wortelduister", w("kampvuur_wortelgang_2", 2368, 38, 6));
        GEBIED_DOEL.put("z3_zeppelinwerf", POI.get("z3_werf"));
        GEBIED_DOEL.put("z3_hart", POI.get("z3_hartpoort"));
        KAMPVUUR.put("z3_honingtak", PLEIN);
        KAMPVUUR.put("z3_kruinzee", w("kampvuur_kruin_1", 2399, 230, 42));
        KAMPVUUR.put("z3_takkenbanen", w("kampvuur_tak_west_30", 2247, 209, -19));
        KAMPVUUR.put("z3_schorsstad", w("kampvuur_schorsstad_vloer", 2290, 81, 7));
        KAMPVUUR.put("z3_wortelvelden", w("kampvuur_mosvoet", 2422, 67, 97));
        KAMPVUUR.put("z3_wortelduister", w("kampvuur_hartslag", 2360, 25, -14));
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
            new Gebied("z3_zeppelinwerf", TX + 156, TZ - 346, TX + 346, TZ - 156, 38, 40, false),
            new Gebied("z3_ring", nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MIN_X, nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MIN_Z, nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MAX_X,
                    nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.ZONE_MAX_Z, 30, 50, false));

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
    // (P-VERHUIS) all on the revamped narrow tree: trunk r ~36 (wider foot), Honingtak y 166, crown storeys 232 / 260 / 284
    // (r 130 / 95 / 60), the nest on top (294), the roots hall under the trunk (floor ~19, the egg at y 26).
    /**
     * The opening (z3_hq01; our version, used when z3-wereld's Z3Reizen hands the first arrival to us or is not in the tree): along
     * the bark wall, up through the clouds into the golden crown, the beekeepers of Honingtak waving on the ring deck, then down along
     * the trunk into the roots where two grey-blue eyes open. The player waits under the camera (OPENING_ANKER) so the chunks load.
     */
    public static final Vec3[] OPENING = {
            v2(2380.0, 110, 95), v2(2300.0, 140, 0),          // 0: low along the bark wall (south-east)
            v2(2410.0, 210, 110), v2(2300.0, 240, 0),         // 120: higher, into the clouds
            v2(2440.0, 320, 160), v2(2300.0, 285, 0),         // 200: above the clouds: the crown in gold, the nest on top
            v2(2318.0, 178, 98), v2(2300.0, 167, 52),         // 270: the Honingplein, beekeepers waving
            v2(2370.0, 95, 60), v2(2300.0, 40, 0),            // 340: down along the trunk into the dusk
            v2(2335.0, 24, 22), v2(2300.0, 24, -12)};         // 420: deep in the roots hall: two eyes open
    public static final int[] OPENING_T = {0, 120, 200, 270, 340, 420};
    /** Where the player waits during the parts of the opening: by the nest (t 0), on the plaza (t 260), in the Hartslag-zaal (t 335). */
    public static final BlockPos[] OPENING_ANKER = {w("nest_rand", 2296, 294, -13), PLEIN, w("hartslag_zaal", 2352, 23, 0)};
    /** The eyes in the dark (the Schimmelkolos), just behind the egg's root basket. */
    public static final Vec3 OGEN = v2(2300.0, 24, -12);

    /** The small BAM (z3_hq02): a Mika zeppelin over the Honingplein market (south of the trunk). */
    public static final Vec3 ZEPPELIN_VAN = v2(2420.0, 192, 130), ZEPPELIN_NAAR = v2(2200.0, 188, 30);
    public static final Vec3[] BAM = {
            v2(2312.0, 172, 84), v2(2340.0, 186, 40),
            v2(2290.0, 172, 76), v2(2262.0, 184, 32)};

    /** The old nest flashback (z3_hq05): Vahoegnir, still pink, lays a glowing egg and wraps his wings around it. */
    public static final Vec3 NEST = Vec3.atBottomCenterOf(w("nest_eierkuil", 2300, 294, 0));
    public static final Vec3[] NEST_SCENE = {
            NEST.add(26, 14, 26), NEST,
            NEST.add(10, 30, 40), NEST.add(0, 2, 0),
            NEST.add(-18, 8, 18), NEST.add(0, 3, 0)};
    public static final Vec3[] NEST_DRAAK = {NEST.add(160, 60, -90), NEST.add(40, 30, -20), NEST.add(0, 6, 0), NEST.add(0.01, 6, 0.3)};

    /** Flash F1 (z3_hq10): the long grey arm through a knot hole in the Pelszaal wall (under the Schorsstad floor) grabs the Count. */
    public static final Vec3 PELSZAAL = Vec3.atBottomCenterOf(POI.get("z3_pelszaal"));
    public static final Vec3 KWASTGAT = Vec3.atCenterOf(nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken.PIEP_KWASTGAT);
    public static final Vec3[] FLASH_F1 = {
            PELSZAAL.add(9, 4, 10), PELSZAAL.add(0, 1, 0),
            PELSZAAL.add(-6, 5, -8), KWASTGAT,
            PELSZAAL.add(6, 3, 8), PELSZAAL.add(0, 1.5, 0)};

    /** The egg in the Nestkamer (z3_hq13). */
    public static final Vec3 EI = Vec3.atBottomCenterOf(w("ei", 2300, 26, 0));

    /**
     * The escape ride (z3_hq14): out of the Hart, east through the Wortelgang to the Wortelpoort, then up the OUTER riding spiral
     * round the trunk (Honingtak, the leaf tunnel of kruin 1), the blossom-wind columns up the crown storeys, ending at the nest.
     * The dragon skims the bark. Built from {@link #rijpad()} so it follows the map.
     */
    public static final Vec3[] RIT = rit();
    public static final Vec3[] RIT_DRAAK = {v2(2520.0, 240, 160), v2(2400.0, 150, 80), v2(2200.0, 230, -110), NEST.add(0, 20, -10)};

    private static Vec3[] rit() {
        List<Vec3> pad = rijpad();
        List<Vec3> k = new java.util.ArrayList<>();
        Vec3 ei = Vec3.atBottomCenterOf(w("ei", 2300, 26, 0));
        Vec3 gang = Vec3.atBottomCenterOf(w("kampvuur_wortelgang_1", 2403, 58, 1));
        Vec3 poort = Vec3.atBottomCenterOf(w("wortelpoort", 2446, 70, 0));
        k.add(ei.add(22, 6, 14));
        k.add(gang);                                                   // 0: out of the roots hall, to the Wortelgang
        k.add(gang.add(-6, 4, 6));
        k.add(poort);                                                  // 1: up the Wortelgang to the Wortelpoort
        for (double f : new double[] {0.08, 0.35, 0.6, 0.85}) {       // 2-5: the riding spiral, from outside (14 out, 5 up)
            Vec3 q = pad.get((int) Math.min(pad.size() - 1, Math.round(f * (pad.size() - 1))));
            Vec3 uit = new Vec3(q.x - TX - 0.5, 0, q.z - TZ - 0.5).normalize();
            k.add(q.add(uit.scale(14)).add(0, 5, 0));
            k.add(q);
        }
        Vec3 eind = pad.get(pad.size() - 1);
        k.add(eind.add(10, 8, 12));                                    // 6: the end on kruin 1, the wind columns ahead
        k.add(Vec3.atBottomCenterOf(w("wind_mosvoet_kruin_onder", 2274, 233, -72)));
        k.add(Vec3.atBottomCenterOf(w("wind_lichtkever_kruin_boven", 2333, 284, -20)).add(10, 6, 8));   // 7: up the crown storeys
        k.add(NEST);
        k.add(NEST.add(20, 10, 20));                                   // 8: the nest
        k.add(NEST);
        return k.toArray(Vec3[]::new);
    }

    /** The finale (z3_hq14 end): the egg hatches; the beam to Hoogvads in the far east. */
    public static final Vec3[] FINALE = {
            NEST.add(9, 4, 9), NEST.add(0, 1, 0),
            NEST.add(-14, 8, 4), NEST.add(0, 6, 0),
            NEST.add(-8, 16, 4), NEST.add(400, 120, 40)};
    public static final Vec3 HOOGVADS = Vec3.atCenterOf(w("landmark_hoogvads_richting", 3000, 260, 0)).add(300, 60, 0);

    /**
     * W-SPEL: flash F2's first shot, "ver in het oosten": towards the Sterrendovende Toren, the grey tower at the east edge of the
     * Zijdeland (zone 6, built terrain), from the south-west and up to its top. (The throne room itself is z3-bazen's set.)
     */
    private static final Vec3 OOST_TOREN = Vec3.atCenterOf(nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalPlekken.TOREN_TOP);
    public static final Vec3[] F2_OOST = {
            OOST_TOREN.add(-120, -50, 50), OOST_TOREN.add(0, -20, 0),
            OOST_TOREN.add(-48, -5, 22), OOST_TOREN.add(0, 4, 0)};

    /** (unused since W-SPEL: nothing was ever built here, the shot showed an empty sky; F2 now uses F2_OOST + z3-bazen's set) */
    public static final Vec3 TROON = v2(2990.0, 300, -680);
    public static final Vec3[] FLASH_F2 = {
            TROON.add(0, 3, 14), TROON.add(0, 2, 0),
            TROON.add(-3, 2, 6), TROON.add(0, 1, -2),
            TROON.add(4, 4, 4), TROON.add(-6, 1, -6)};

    /**
     * The cliffhanger (z3_hq15): from the Honingplein (south, the open side of the ring deck) to the far east. (v1 used the Nestkapel
     * balcony; the Nestkapel is now a honeycomb cell under the deck without a view.)
     */
    private static final Vec3 BALKON = Vec3.atBottomCenterOf(w("knuffelbeeld", 2300, 167, 52)).add(0, 0, 12);
    public static final Vec3[] CLIFFHANGER = {BALKON.add(-4, 2, -6), BALKON.add(20, 0, 0), BALKON.add(6, 40, -10), v2(3800.0, 330, 0)};

    /** The airship to Honingtak (fallback when z3-wereld has no line): over the cloud sea from the west to the mast (north-west). */
    public static final Vec3[] VLUCHT = {
            v2(1960.0, 240, -170), v2(2215.0, 170, -88),
            v2(2110.0, 200, -130), v2(2215.0, 168, -88),
            v2(2190.0, 172, -100), v2(2300.0, 180, 0)};
}
