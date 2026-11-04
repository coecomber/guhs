package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-2 npc slice (Z2_BRIEF: one class). Zone 2 = x -600..600, z -3000..-1800 in guhs:vadsrijk.
 * <p>Aligned with z2-wereld's POI_Z2.md v1 (2026-11-04, generated from the real map; ids in the comments "POI:&lt;id&gt;"). This tree
 * does not hold z2-wereld's {@code Z2WereldPlekken}, so the numbers are copied; integration: replace them by
 * {@code Z2WereldPlekken.plek("<id>")} (as zone 1's NpcPlekken uses WereldPlekken). Spots POI_Z2 does not list (most citizens, a
 * few quest places) are chosen next to listed ones; NPCs snap to the nearest floor within 12 blocks. Requests:
 * guhs_vadsrijk/POI_VERZOEKEN_Z2.md, section "npc".
 * <p>NPC/POI/area ids here are checked by tools/features/vadsrijk_z2_npc.py (every NPC needs a name + a "where" text there).
 */
public final class Z2NpcPlekken {
    private Z2NpcPlekken() {
    }

    static BlockPos p(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    /** (integration) The spot from z2-wereld's generated Z2WereldPlekken; the numbers are only the fallback. */
    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    /** Stand y of the Kandijkom geode floor (POI_Z2: 91). */
    static final int KK = 91;

    // ---- Kandijkom -----------------------------------------------------------------------------------------------------------
    public static final BlockPos PLEIN = w("plein", 0, KK, -2412);                  // POI:plein
    /** Where the airship sets you down in Kandijkom. */
    public static final BlockPos DOK = w("dok_landing", 0, KK, -2244);                    // POI:dok_landing
    public static final BlockPos DOK_AANKOMST = DOK;
    /** (fix round 2) The geode: its middle and inner radius, and the inner mouths of its gates (plan.py POORTEN; the tracker
     *  points there first when you are in town and the target is outside). NOTE (2026-11-04): the east and west gates are
     *  blocked by a 7-high wall at x +-128 and the north gate by a 4-high step at z -2528 (NOTITIES_G_UX.md). */
    public static final BlockPos GEODE_MIDDEN = new BlockPos(0, KK, -2400);
    public static final int GEODE_BINNEN = 124;
    public static final BlockPos POORT_OOST = p(120, KK, -2400), POORT_WEST = p(-120, KK, -2400), POORT_NOORD = p(0, KK, -2520);
    /** The gondola stations (POI:gondel_dal / gondel_berg). */
    public static final BlockPos GONDEL_BENEDEN = p(78, KK, -2466);
    public static final BlockPos GONDEL_BOVEN = p(306, 151, -2616);

    // ---- NPC spots (id -> spot; look + clothes in Z2Npcs) ----------------------------------------------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();
    static {
        // Kandijkom: travel, the order, the guard
        NPC.put("z2_kapitein", w("dok_kapitein", -4, KK, -2240));                 // POI:dok_kapitein
        NPC.put("z2_cafe", w("npc_cafe", 59, KK, -2344));                     // POI:npc_cafe (behind the counter of the cafe)
        NPC.put("z2_burgemeester", w("npc_burgemeester", 0, KK, -2418));              // POI:npc_burgemeester
        NPC.put("z2_kwarts", p(17, KK, -2425));                   // next to POI:questbord
        NPC.put("z2_wacht_plein_west", p(-24, KK, -2412));
        NPC.put("z2_wacht_plein_oost", p(24, KK, -2412));
        NPC.put("z2_ordemeester", w("npc_kristalkapel", 0, KK, -2474));               // POI:npc_kristalkapel (the chapel = the order's branch here)
        // Kandijkom: shops and services
        NPC.put("z2_juwelier", w("gebouw.juwelier.npc", -55, KK, -2440));                // POI:gebouw.juwelier.npc
        NPC.put("z2_smid", w("gebouw.smid.npc", 55, KK, -2440));                     // POI:gebouw.smid.npc
        NPC.put("z2_wapenhandelaar", p(48, KK, -2437));           // in the smithy (POI:gebouw.smid.binnen)
        NPC.put("z2_winkelier", w("gebouw.winkel_kandij.npc", 68, KK, -2392));                // POI:gebouw.winkel_kandij.npc
        NPC.put("z2_breister", w("gebouw.winkel_wantjes.npc", -68, KK, -2392));                // POI:gebouw.winkel_wantjes.npc
        NPC.put("z2_veilingmeester", w("gebouw.bank.npc", -55, KK, -2344));          // POI:gebouw.bank.npc
        NPC.put("z2_schaatsjuf", w("npc_schaatsguh", 13, KK, -2400));               // POI:npc_schaatsguh
        NPC.put("z2_gondelier", w("npc_gondel", 76, KK, -2465));                // POI:npc_gondel
        // Kandijkom: citizens
        NPC.put("z2_opa_sneeuwbal", p(36, KK, -2346));            // the cafe terrace (POI:cafe_terras)
        NPC.put("z2_bakker", p(93, KK, -2392));                   // outside POI:gebouw.huis_4.deur (g-ux: x 95 was inside its west wall)
        NPC.put("z2_vlokje", p(-6, KK, -2404));                   // at the skating pond
        NPC.put("z2_uitvinder", p(-52, KK, -2487));               // outside POI:gebouw.huis_8.deur (his workshop)
        NPC.put("z2_galmpje", p(-14, KK, -2424));
        NPC.put("z2_postguh", p(-21, KK, -2302));                 // outside POI:gebouw.huis_5.deur
        NPC.put("z2_sneeuwpop", p(30, KK, -2290));                // south of POI:gebouw.huis_6
        NPC.put("z2_kasguh", p(-89, KK, -2392));                  // outside POI:gebouw.huis_3.deur
        NPC.put("z2_winterslaap", p(83, KK, -2437));              // outside POI:gebouw.huis_2.deur
        NPC.put("z2_poollicht", p(-83, KK, -2441));               // outside POI:gebouw.huis_1.deur
        // out in the zone
        NPC.put("z2_gondel_boven", p(303, 151, -2612));           // POI:gondel_berg
        NPC.put("z2_berggids", p(236, 99, -2334));                // POI:kampvuur_galmhellingen_1
        NPC.put("z2_visser", w("npc_ijsvisser", -418, 88, -2261));                 // POI:npc_ijsvisser (the island in the lake)
        NPC.put("z2_beeldhouwer", p(-254, 106, -2444));           // POI:kampvuur_knuffelmeer_3 (north shore)
        NPC.put("z2_touwtje", p(3, 151, -2762));                  // POI:brug_3_zuid (the middle bridge)
        NPC.put("z2_prisma", p(4, 176, -2846));                   // POI:deur_spiegelburcht_voor / burcht_voorplein
        // the Kristalwacht: one at every campfire (POI:ridder_*), the knights of zone 2
        NPC.put("z2_wacht_dok", p(-7, KK, -2260));
        NPC.put("z2_wacht_galm_1", p(234, 99, -2328));
        NPC.put("z2_wacht_galm_2", p(332, 127, -2118));
        NPC.put("z2_wacht_galm_3", p(482, 150, -2438));
        NPC.put("z2_wacht_galm_4", p(252, 115, -2558));
        NPC.put("z2_wacht_meer_1", p(-198, 89, -2118));
        NPC.put("z2_wacht_meer_2", p(-558, 96, -2238));
        NPC.put("z2_wacht_meer_3", p(-248, 107, -2438));
        NPC.put("z2_wacht_meer_4", p(-468, 114, -2498));
        NPC.put("z2_wacht_kloof_1", p(-258, 139, -2698));
        NPC.put("z2_wacht_kloof_2", p(302, 140, -2700));
        NPC.put("z2_wacht_kloof_3", p(26, 132, -2728));
        NPC.put("z2_wacht_kloof_4", p(-98, 65, -2772));
        NPC.put("z2_wacht_kloof_5", p(-358, 147, -2878));
        NPC.put("z2_wacht_burcht", p(14, 163, -2824));
        // the petrified guhs on the frozen lake (main quest z2_hq06)
        NPC.put("z2_versteend_zwier", p(-380, 85, -2226));        // POI:meer_midden
        NPC.put("z2_versteend_snoek", p(-334, 85, -2082));        // POI:meer_schotsen (the floes in the south)
        NPC.put("z2_versteend_pluk", p(-520, 87, -2270));         // the west side of the ice
        NPC.put("z2_versteend_ijsje", p(-412, 88, -2266));        // on the island, next to Wak
    }

    // ---- points of interest (quest objectives "poi:<id>", tracker targets "poi:<id>") ------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    static {
        POI.put("z2_dok", DOK);
        POI.put("z2_plein", PLEIN);
        POI.put("z2_schaatsvijver", w("schaatsvijver", 0, KK, -2400));             // POI:schaatsvijver
        POI.put("z2_galmnaald", w("galmwolf_naald", 430, 155, -2320));              // POI:galmwolf_naald
        POI.put("z2_naald_do", w("galm_naalden_3", 260, 95, -1980));                // POI:galm_naalden_3
        POI.put("z2_naald_re", w("galm_naalden_1", 330, 130, -2200));               // POI:galm_naalden_1
        POI.put("z2_naald_mi", w("galm_naalden_4", 420, 165, -2480));               // POI:galm_naalden_4
        POI.put("z2_lawinepad", p(360, 149, -2430));              // POI:galm_lawine_1_voet
        POI.put("z2_sneeuwpophelling", w("galm_lawine_3_voet", 400, 137, -1880));       // POI:galm_lawine_3_voet
        POI.put("z2_warmwaterbron", p(520, 146, -1960));          // (requested: a small hot spring near POI:galm_naalden_2)
        POI.put("z2_gondel_top", GONDEL_BOVEN);
        POI.put("z2_meer_scheur", p(-390, 85, -2392));            // (requested: a crack in the ice, north part of the lake)
        POI.put("z2_brug_1", p(0, 150, -2765));                   // POI:brug_3_zuid (the middle bridge, Touwtje)
        POI.put("z2_brug_2", p(-180, 140, -2750));                // POI:brug_2_zuid
        POI.put("z2_brug_3", p(-430, 138, -2734));                // POI:brug_1_zuid (the far west bridge)
        POI.put("z2_boortoren", w("boortoren_3", 400, 136, -2744));              // POI:boortoren_3 (east)
        POI.put("z2_boortoren_2", w("boortoren_1", -330, 140, -2738));           // POI:boortoren_1 (west)
        POI.put("z2_drakenravijn", w("draak_zadel_kijk", -330, 126, -2582));          // POI:draak_zadel_kijk (the dragon rests at POI:draak_zadel)
        POI.put("z2_boorhal", w("arena_boormeester_deur", -500, 66, -2779));                // POI:arena_boormeester_deur
        POI.put("z2_spiegelpoort", w("deur_spiegelburcht_voor", 0, 176, -2851));             // POI:deur_spiegelburcht_voor
        POI.put("z2_uitkijk_poollicht", p(-210, 208, -2930));     // POI:uitkijk_kloofkijk
        POI.put("z2_kampvuur_galm", w("kampvuur_galmhellingen_1", 232, 99, -2330));           // POI:kampvuur_galmhellingen_1
        POI.put("z2_kampvuur_meer", w("kampvuur_knuffelmeer_1", -200, 89, -2120));          // POI:kampvuur_knuffelmeer_1
        POI.put("z2_kampvuur_kloof", w("kampvuur_splinterkloof_3", 24, 132, -2730));          // POI:kampvuur_splinterkloof_3
    }
    /** Horizontal radius / vertical tolerance for "you are there" (as zone 1); the high needles/lookouts get more height. */
    public static final double POI_STRAAL = 6, POI_HOOGTE = 10, NAALD_HOOGTE = 30;

    // ---- areas (z2-wereld's ids, POI_Z2 "Areas"): tracker target per area, campfires, boss arenas -------------------------------
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    public static final Map<String, BlockPos> BAAS = new LinkedHashMap<>();
    static {
        GEBIED_DOEL.put("z2_kandijkom", PLEIN);
        GEBIED_DOEL.put("z2_kandijkom_dok", DOK);
        GEBIED_DOEL.put("z2_galmhellingen", POI.get("z2_kampvuur_galm"));
        GEBIED_DOEL.put("z2_knuffelmeer", POI.get("z2_kampvuur_meer"));
        GEBIED_DOEL.put("z2_splinterkloof", POI.get("z2_kampvuur_kloof"));
        GEBIED_DOEL.put("z2_spiegelburcht", POI.get("z2_spiegelpoort"));
        KAMPVUUR.put("z2_kandijkom", w("kampvuur_kandijkom_dok_1", -9, KK, -2262));          // POI:kampvuur_kandijkom_dok_1
        KAMPVUUR.put("z2_kandijkom_dok", p(-9, KK, -2262));
        KAMPVUUR.put("z2_galmhellingen", POI.get("z2_kampvuur_galm"));
        KAMPVUUR.put("z2_knuffelmeer", POI.get("z2_kampvuur_meer"));
        KAMPVUUR.put("z2_splinterkloof", POI.get("z2_kampvuur_kloof"));
        KAMPVUUR.put("z2_spiegelburcht", p(12, 164, -2826));     // POI:kampvuur_spiegelburcht_1
        BAAS.put("z2_galmhellingen", p(392, 121, -2280));        // POI:arena_galmwolf_deur
        BAAS.put("z2_spiegelburcht", w("arena_spiegelzucht_deur", 0, 176, -2937));          // POI:arena_spiegelzucht_deur
        BAAS.put("z2_splinterkloof", POI.get("z2_boorhal"));      // POI:arena_boormeester_deur (secret)
    }

    /**
     * Fallback areas (kern Gebieden) = POI_Z2 v1's boxes, only registered when z2-wereld did not register an area with that id
     * (Z2NpcRegistry on server start). Order: catch-all first, smaller ones later (voegVooraan puts each in front).
     */
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minLv, int maxLv, boolean stad) {
    }

    public static final Gebied[] GEBIEDEN = {
            new Gebied("z2_ring", -600, -3000, 600, -1800, 20, 35, false),
            new Gebied("z2_splinterkloof", -600, -3000, 600, -2701, 30, 34, false),
            new Gebied("z2_knuffelmeer", -600, -2700, -151, -1800, 25, 30, false),
            new Gebied("z2_galmhellingen", 151, -2700, 600, -1800, 20, 25, false),
            new Gebied("z2_kandijkom", -150, -2550, 150, -2250, 20, 35, true),
            new Gebied("z2_kandijkom_dok", -40, -2249, 40, -2200, 20, 35, true),
            new Gebied("z2_spiegelburcht", -70, -2995, 70, -2850, 33, 35, false)};

    // ---- secrets ----------------------------------------------------------------------------------------------------------------
    /** The 10 knuffels frozen under the lake ice (POI:knuffel_ijs_1..10 = stand on the ice above one; Kleine Vlokje's quest). */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    static {
        int[][] ijs = {{-300, -2160}, {-350, -2300}, {-430, -2180}, {-470, -2320}, {-380, -2380}, {-300, -2340}, {-440, -2090}, {-360, -2110},
                {-500, -2220}, {-270, -2250}};
        for (int i = 0; i < ijs.length; i++) {
            KNUFFEL.put("ijs_" + (i + 1), p(ijs[i][0], 85, ijs[i][1]));
        }
    }
    /** The 5 clues to the Mika-boormeester (start the secret quest; all 5 unlock the boss: bazen Boormeester.ontgrendel). */
    public static final Map<String, BlockPos> AANWIJZING = new LinkedHashMap<>();
    static {
        AANWIJZING.put("olievlek", p(404, 136, -2740));          // at POI:boortoren_3
        AANWIJZING.put("bouwtekening", p(-326, 140, -2734));     // at POI:boortoren_1
        AANWIJZING.put("boorgaten", p(364, 149, -2426));         // at POI:galm_lawine_1_voet
        AANWIJZING.put("boorschema", p(-56, KK, -2486));         // behind the inventor's workshop (POI:gebouw.huis_8)
        AANWIJZING.put("warme_spleet", p(-496, 66, -2776));      // next to POI:geheim_boorplaats / the boorhal door
    }
    /** Hidden treasures (one-time loot; POI_Z2 "Secrets"). */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    static {
        SCHAT.put("ijsgrot", w("geheim_ijsgrot", -575, 122, -2400));               // POI:geheim_ijsgrot
        SCHAT.put("galmgrot", w("geheim_galmgrot", 566, 140, -1990));               // POI:geheim_galmgrot
        SCHAT.put("chocokelder", w("geheim_chocokelder", 52, 85, -2344));              // POI:geheim_chocokelder
        SCHAT.put("burchtdak", w("geheim_burchtdak", 10, 195, -2903));               // POI:geheim_burchtdak
        SCHAT.put("onder_de_brug", p(-180, 128, -2774));         // (requested: a chest under the west bridge, POI:brug_2)
    }
    public static final double VIND_STRAAL = 3, VIND_HOOGTE = 8;

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    public static double hoogteVan(String poi) {
        return poi.startsWith("z2_naald") || poi.equals("z2_galmnaald") || poi.equals("z2_uitkijk_poollicht") || poi.equals("z2_drakenravijn")
                ? NAALD_HOOGTE : POI_HOOGTE;
    }

    // ---- scenes (camera keyframes: pos, look-at; world coordinates) ------------------------------------------------------------
    /** The crystal needle on the geode rim where the Galmwolf howls in the opening (it bursts; requested: POI "opening_naald"). */
    public static final Vec3 OPENING_NAALD = new Vec3(70, 148, -2205);
    /**
     * The opening: the airship comes from the south over the cloud sea into a snowstorm (ice floes, lights out), the Galmwolf howls
     * on a needle that bursts, the ship slips into the warm geode of Kandijkom. The player stands at the dock from tick 0 (so the
     * chunks around the camera are loaded); the camera stays within ~140 blocks of it.
     */
    public static final Vec3[] OPENING = {
            new Vec3(-8, 138, -2110), new Vec3(10, 132, -2300),       // 0: from the deck, in the storm, Kandijkom ahead (unseen)
            new Vec3(4, 134, -2140), new Vec3(30, 128, -2240),        // 100: the floes hit, lights out
            new Vec3(20, 140, -2165), OPENING_NAALD,                   // 180: turn to the needle: the wolf
            new Vec3(26, 142, -2180), OPENING_NAALD.add(0, 2, 0),      // 250: the howl, the needle bursts
            new Vec3(0, 120, -2205), new Vec3(0, 98, -2400),           // 310: diving into the geode, the pink glow
            new Vec3(0, 96, -2236), new Vec3(59, 92, -2344)};          // 400: at the dock, looking at the cafe
    /** The ship's path in the opening (the effects follow it). */
    public static final Vec3 SCHIP_VAN = new Vec3(0, 132, -2150), SCHIP_NAAR = new Vec3(0, 93, -2242);

    /** The wounded dragon (z2_hq09) on the col north of the lake (POI:draak_zadel), seen from POI:draak_zadel_kijk. */
    public static final Vec3 DRAAK_RUST = new Vec3(-330, 128, -2610);
    public static final Vec3[] DRAAK_1 = {
            new Vec3(-344, 150, -2570), DRAAK_RUST,                    // from above the col, looking down
            new Vec3(-334, 131, -2592), DRAAK_RUST.add(0, 2, 0)};      // down to his head
    public static final Vec3[] DRAAK_2 = {
            new Vec3(-332, 130, -2596), DRAAK_RUST.add(0, 3, 0),       // close: he looks at you
            new Vec3(-340, 138, -2580), DRAAK_RUST.add(0, 40, -40),    // he rises
            new Vec3(-346, 150, -2570), new Vec3(-420, 230, -2900)};   // and flies off to the north
    /** The dragon's path off the col. */
    public static final Vec3[] DRAAK_WEG = {DRAAK_RUST, DRAAK_RUST.add(4, 30, -20), DRAAK_RUST.add(-30, 80, -100), new Vec3(-450, 240, -3000)};

    /**
     * The flashback (z2_hq11): the shard in the burcht (POI:burcht_splinter), then long ago: the pink dragon over the realm (high
     * above the burcht looking south over the peaks, within the chunks loaded around the player at the gate).
     */
    public static final Vec3[] FLASHBACK = {
            new Vec3(3, 179, -2971), new Vec3(0, 177.5, -2976),        // the shard close up
            new Vec3(0, 245, -2840), new Vec3(0, 160, -2600),          // memory: high over the peaks, the realm below
            new Vec3(-60, 235, -2800), new Vec3(60, 180, -2650)};      // the pink dragon flies over, protective
    public static final Vec3[] FLASHBACK_DRAAK = {new Vec3(-200, 210, -2720), new Vec3(-60, 224, -2790), new Vec3(70, 220, -2780),
            new Vec3(230, 232, -2680)};

    /** The cliffhanger (z2_hq12): from the Kristalkapel to the far west (azimuth ~255, the giant tree landmark of WereldLucht). */
    public static final Vec3[] CLIFFHANGER = {
            new Vec3(4, 93, -2468), new Vec3(0, 92, -2476),            // in the Kristalkapel
            new Vec3(-10, 130, -2470), new Vec3(-900, 190, -2240),
            new Vec3(-30, 150, -2450), new Vec3(-1000, 220, -2210)};

    /** The gondola ride (fallback when z2-wereld's own gondola is not in this tree). */
    public static final Vec3[] GONDEL = {
            new Vec3(84, 100, -2470), new Vec3(306, 159, -2618),       // the valley station, the cable up
            new Vec3(190, 135, -2540), new Vec3(306, 159, -2618),
            new Vec3(296, 156, -2606), new Vec3(420, 165, -2480)};     // the top: over the Galmhellingen
    /** The airship between De Haven and Kandijkom (fallback when z2-wereld has no line of its own). */
    public static final Vec3[] VLUCHT_KANDIJKOM = {
            new Vec3(0, 140, -2120), new Vec3(0, 120, -2300),
            new Vec3(0, 116, -2200), new Vec3(0, 98, -2400),
            new Vec3(0, 97, -2236), new Vec3(59, 92, -2344)};
}
