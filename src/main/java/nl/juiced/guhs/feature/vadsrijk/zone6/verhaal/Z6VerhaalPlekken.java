package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-6 story slice (U-Z6VERHAAL): Het Zijdeland, the west coast of the Grote Vasteland (ZONE6_PLAN 1.1:
 * tile box x 3280..4180, z -1060..-460, lv 60-70).
 * <p><b>PLACEHOLDERS.</b> The land itself is u-z6wereld's. Until it exists every spot is {@code w("<poi id>", x, y, z)}: the
 * generated {@code zone6.wereld.Z6WereldPlekken.plek(id)} wins when that class exists (looked up by reflection, so this compiles
 * without it), else the number here. The numbers come from the concept renders (renders-zijdeland/_bron/zijwereld.py: PLEK, KLOOF,
 * SIQ, RIVIER, WEG, SAFFRAAN, ZOUT, OASE, TOREN; rozenkloof.py / saffraanpoort.py), which already work in game coordinates.
 * Main anchors: Rozenkloof plein (3400, 69, -714), Schatkamer in the east end wall (x ~3470), the Siq top (3566, 121, -794),
 * the Luchthaven on the north rim (3398, 124, -772); Saffraanpoort palace square (3762, 70, -846), palace (3762, -872), bazaar
 * (3742, -812), Westpoort (3640, -846); beacons palm (3478, 121, -906), duin (3655, 96, -560), zout (3930, 69, -612), ruine
 * (4045, 108, -1000), oase (4062, 70, -842); dungeon (3985, 72, -962); cave (3874, 69, -505); oasis lake (4096, -768); tower
 * (4140, -688). NPCs hover when no floor is built yet (Npcs.zet).
 * <p>Ids are checked by tools/features/vadsrijk_u_z6verhaal.py (every NPC/POI needs a name/"where" text there).
 */
public final class Z6VerhaalPlekken {
    private Z6VerhaalPlekken() {
    }

    // ---- u-z6wereld's generated spots (reflection: the class does not exist yet) -------------------------------------------------
    private static Method wereldPlek;
    private static boolean gezocht;

    /** u-z6wereld's spot for this id, or null (no Z6WereldPlekken in this tree, or the id is not on its map). */
    @Nullable
    public static synchronized BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                wereldPlek = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken").getMethod("plek", String.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                wereldPlek = null;
            }
        }
        if (wereldPlek == null) {
            return null;
        }
        try {
            return wereldPlek.invoke(null, id) instanceof BlockPos b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    // ---- the zone box (ZONE6_PLAN 1.1; u-z6wereld may add VadsrijkZones.Zone.Z6 with the same box) -------------------------------
    public static final int MIN_X = 3280, MAX_X = 4180, MIN_Z = -1060, MAX_Z = -460;

    /** Is this point in the Zijdeland (zone 6)? Any height (the land is on the ground; Hoogvads is west of x 3200). */
    public static boolean zone6(double x, double y, double z) {
        return x >= MIN_X && x < MAX_X + 1 && z >= MIN_Z && z < MAX_Z + 1;
    }

    // ---- key places ------------------------------------------------------------------------------------------------------------
    /** Line E's harbour on Hoogvads: the Staarthaven at the end of the whale's tail (east; t-z5wereld's POI haven_z5_z6_vertrek wins). */
    public static final BlockPos STAARTHAVEN = staarthaven();

    /** W-SCHEPEN: zone 5 builds the Staarthaven now (the open east end of the tail path, POI haven_z5_z6_vertrek): use it. */
    private static BlockPos staarthaven() {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.ALLE.get("haven_z5_z6_vertrek");
        return b != null ? b : w("staarthaven", 2966, 210, -520);
    }
    /** Line E's harbour in Rozenkloof: the Luchthaven on the north rim of the gorge (POI haven_z6_z5_vertrek). */
    public static final BlockPos LUCHTHAVEN = w("haven_z6_z5_vertrek", 3398, 120, -768);
    /** Where the first flight (and every short flight) sets you down: on the deck of the Luchthaven. */
    public static final BlockPos AANKOMST = w("rozenkloof_aankomst", 3398, 120, -760);
    public static final BlockPos PLEIN = w("rozenkloof_plein", 3400, 70, -716);
    public static final BlockPos SCHATKAMER = w("rozenkloof_schatkamer", 3470, 74, -699);
    public static final BlockPos SIQ = w("rozenkloof_siq_boven", 3564, 118, -794);
    public static final BlockPos ZEEMOND = w("rozenkloof_zeemond", 3262, 64, -745);
    public static final BlockPos SAFFRAAN = w("saffraanpoort_paleisplein", 3762, 71, -846);
    public static final BlockPos PALEIS = w("paleis_poort", 3762, 71, -884);
    public static final BlockPos SPIEGELZAAL = w("paleis_spiegelzaal", 3760, 71, -905);
    public static final BlockPos BAZAAR = w("bazaar_kruising", 3762, 71, -812);
    public static final BlockPos WESTPOORT = w("saffraanpoort_westpoort", 3644, 71, -806);
    public static final BlockPos SERAI = w("karavanserai_hof", 3679, 71, -797);
    public static final BlockPos STERRENWACHT = w("sterrenwacht", 3846, 71, -898);
    public static final BlockPos KIJKER = w("sterrenwacht_top", 3846, 71, -906);
    public static final BlockPos TUIN = w("paleis_tuin", 3762, 71, -872);
    public static final BlockPos BRUG = w("saffraanbrug_paviljoen", 3810, 70, -846);
    public static final BlockPos OOSTPOORT = w("saffraanpoort_poort_oost", 3880, 82, -846);
    public static final BlockPos OASE = w("moeder_bron", 4072, 80, -806);
    public static final BlockPos TORENVOET = w("toren_voet", 4112, 68, -688);
    public static final BlockPos TOREN_TOP = w("toren_top", 4140, 236, -688);
    public static final BlockPos KERKER = w("kerker_ingang", 3985, 70, -950);

    /** The five star beacons: where you stand to read the stars (u-z6wereld's {@code <baken>_puzzel}). */
    public static final Map<String, BlockPos> BAKEN = new LinkedHashMap<>();
    /** The star of each beacon (u-z6wereld's {@code <baken>_ster}: the light blocks; the beam starts there). */
    public static final Map<String, BlockPos> BAKEN_STER = new LinkedHashMap<>();

    static {
        BAKEN.put("palm", w("palmbaken_puzzel", 3496, 106, -906));
        BAKEN.put("zout", w("zoutbaken_puzzel", 3948, 69, -612));
        BAKEN.put("ruine", w("ruinebaken_puzzel", 4063, 69, -1000));
        BAKEN.put("duin", w("duinbaken_puzzel", 3673, 84, -560));
        BAKEN.put("oase", w("oasebaken_puzzel", 4080, 81, -842));
        BAKEN_STER.put("palm", w("palmbaken_ster", 3478, 141, -906));
        BAKEN_STER.put("zout", w("zoutbaken_ster", 3930, 104, -612));
        BAKEN_STER.put("ruine", w("ruinebaken_ster", 4045, 105, -1000));
        BAKEN_STER.put("duin", w("duinbaken_ster", 3655, 105, -560));
        BAKEN_STER.put("oase", w("oasebaken_ster", 4062, 103, -842));
    }

    // ---- the NPCs (id -> stand spot; they snap to the nearest floor within 12 blocks, or hover) ----------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();

    static void npc(String id, BlockPos b) {
        NPC.put(id, b);
    }

    static BlockPos naast(BlockPos b, int dx, int dz) {
        return b.offset(dx, 0, dz);
    }

    static {
        // Rozenkloof
        npc("z6_zandloper", w("rozenkloof_havenmeester", 3392, 107, -778));
        npc("z6_roosje", w("rozenkloof_kloofmoeder", 3488, 75, -699));
        npc("z6_dadel", w("rozenkloof_oma_dadel", 3412, 70, -720));
        npc("z6_hobbel", w("rozenkloof_stalmeester", 3433, 71, -716));
        npc("z6_franje", w("rozenkloof_tapijtwinkel", 3384, 69, -708));
        npc("z6_roosmarijn", w("rozenkloof_tuinguh", 3372, 68, -712));
        npc("z6_stempel", w("rozenkloof_postguh", 3390, 69, -722));
        npc("z6_bankier_kloof", w("rozenkloof_bank", 3420, 71, -706));
        npc("kk_poort_rozenkloof", naast(PLEIN, -6, -6));
        npc("bond_bord_rozenkloof", w("rozenkloof_recruitbord", 3396, 69, -722));
        // Saffraanpoort: the palace
        npc("z6_rozijntje", w("paleis_troon", 3782, 71, -905));
        npc("z6_kaneelstok", w("paleis_grootvizier", 3778, 71, -900));
        npc("z6_glans", w("paleis_glans", 3748, 71, -900));
        npc("z6_twinkel", w("sterrenwacht_twinkel", 3846, 71, -902));
        npc("z6_dadel_serai", naast(SERAI, 2, 2));
        // Saffraanpoort: the city
        npc("z6_knoopje", w("bazaar_knoopje", 3740, 71, -809));
        npc("z6_kardemom", w("bazaar_koopman_1", 3720, 71, -815));
        npc("z6_pistache", w("bazaar_koopman_2", 3780, 71, -809));
        npc("z6_lokum", w("bazaar_koopman_3", 3800, 71, -815));
        npc("z6_koperkop", w("bazaar_smidse", 3712, 71, -809));
        npc("z6_bankier_poort", w("saffraanpoort_bank", 3790, 71, -782));
        npc("z6_knars", w("saffraanpoort_knars", 3703, 71, -846));
        npc("z6_pluk", w("kanaalwijk_pluk", 3702, 71, -772));
        npc("z6_ringeling", naast(TUIN, 3, 3));
        npc("kk_poort_saffraanpoort", naast(SAFFRAAN, -8, 4));
        npc("bond_bord_saffraanpoort", w("saffraanpoort_recruitbord", 3650, 71, -812));
        // profession teachers
        npc("z6_leermeester_smeden", w("bazaar_smidse_leermeester", 3714, 71, -813));
        npc("z6_leermeester_naaien", w("bazaar_naaiatelier", 3730, 71, -815));
        npc("z6_leermeester_koken", w("karavanserai_keuken", 3688, 71, -790));
        npc("z6_leermeester_brouwen", w("sterrenwacht_brouwerij", 3838, 71, -896));
        npc("z6_leermeester_imkeren", naast(TUIN, -6, 4));
        // out in the land
        npc("z6_dadelpit", w("watervallen_voet", 3538, 60, -957));
        npc("z6_spetter", w("koerier_spetter", 3532, 60, -958));
        npc("z6_korrel", w("zoutzieder_korrel", 3876, 69, -542));
        npc("z6_rimpel", w("duinguh_rimpel", 3646, 71, -540));
        npc("z6_stof", w("professor_stof", 3970, 70, -942));
        npc("z6_bron", OASE);   // (moeder_bron, under the Moederpalm)
        npc("z6_mientje", w("mager_mientje", 4150, 75, -760));
        for (int i = 1; i <= 3; i++) {
            npc("z6_vluchteling_" + i, w("oase_tent_" + i, 4142 + 3 * i, 75, -754));
        }
        // citizens
        int[][] bv = {{3396, 69, -708}, {3372, 85, -744}, {3746, 69, -818}, {3780, 69, -852}, {3768, 70, -908}, {3664, 70, -834}};
        for (int i = 1; i <= bv.length; i++) {
            npc("z6_burger_" + i, w("z6_burger_" + i, bv[i - 1][0], bv[i - 1][1], bv[i - 1][2]));
        }
        // guards at the campfires
        int[][] wv = {{3404, 69, -710}, {3400, 120, -778}, {3646, 69, -840}, {3768, 70, -842}, {3924, 69, -646}, {4064, 67, -778}};
        for (int i = 1; i <= wv.length; i++) {
            npc("z6_wacht_" + i, w("z6_wacht_" + i, wv[i - 1][0], wv[i - 1][1], wv[i - 1][2]));
        }
    }

    // ---- POIs: quest targets (radius POI_STRAAL, height POI_HOOGTE) ------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    public static final int POI_STRAAL = 5, POI_HOOGTE = 6, VIND_STRAAL = 3, VIND_HOOGTE = 4;

    static void poi(String id, BlockPos b) {
        POI.put(id, b);
    }

    static {
        poi("z6_staarthaven", STAARTHAVEN);
        poi("z6_luchthaven", AANKOMST);
        poi("z6_plein", PLEIN);
        poi("z6_schatkamer", SCHATKAMER);
        poi("z6_klus_dadels", w("rozenkloof_bobbel", 3414, 70, -722));
        poi("z6_klus_water", w("rozenkloof_fontein", 3406, 70, -715));
        poi("z6_klus_tapijten", w("rozenkloof_tapijtwinkel", 3384, 69, -708));
        poi("z6_siq", SIQ);
        poi("z6_rustplek", w("duinput", 3611, 71, -668));
        poi("z6_bobbeltje", w("bobbeltje_duin", 3684, 75, -662));
        poi("z6_wormelien_zicht", w("duinen_karavaanweg", 3726, 70, -625));
        poi("z6_westpoort", WESTPOORT);
        poi("z6_kijker", KIJKER);
        poi("z6_palmbaken", BAKEN.get("palm"));
        poi("z6_saffraanbrug", BRUG);
        poi("z6_zoutkamp", w("karavaankamp", 3906, 72, -580));
        poi("z6_zoutbaken", BAKEN.get("zout"));
        poi("z6_mikakamp", w("zoutvlakte_mikakamp", 3972, 69, -582));
        poi("z6_sterrenwacht_ingang", KERKER);
        poi("z6_ruinebaken", BAKEN.get("ruine"));
        poi("z6_spiegelzaal", SPIEGELZAAL);
        int[][] dak = {{3762, 71, -812}, {3800, 84, -820}, {3880, 82, -846}};
        for (int i = 1; i <= dak.length; i++) {
            poi("z6_dak_" + i, i == 3 ? OOSTPOORT : w(i == 1 ? "bazaar_dak" : "dak_" + i, dak[i - 1][0], dak[i - 1][1], dak[i - 1][2]));
        }
        poi("z6_duinbaken", BAKEN.get("duin"));
        poi("z6_luchtspiegeling", w("oostroute_luchtspiegeling", 4020, 70, -662));
        poi("z6_oasebaken", BAKEN.get("oase"));
        poi("z6_moederoase", w("moederoase", 4060, 70, -790));
        poi("z6_torenvoet", TORENVOET);
        poi("z6_zeemond", ZEEMOND);
        // side quests
        poi("z6_feest", naast(PLEIN, -3, 4));
        int[][] tuin = {{3361, 83, -740}, {3400, 101, -742}, {3430, 120, -688}};
        for (int i = 1; i <= tuin.length; i++) {
            poi("z6_tuin_" + i, w("rozenkloof_tuin_" + i, tuin[i - 1][0], tuin[i - 1][1], tuin[i - 1][2]));
        }
        int[][] noria = {{3700, 71, -848}, {3794, 71, -832}, {3840, 71, -863}};
        for (int i = 1; i <= noria.length; i++) {
            poi("z6_noria_" + i, w("noria_" + i, noria[i - 1][0], noria[i - 1][1], noria[i - 1][2]));
        }
        int[][] ex = {{3826, 104, -906}, {3790, 82, -848}, {3744, 112, -880}, {3248, 64, -744}};
        for (int i = 1; i <= ex.length; i++) {
            poi("z6_examen_" + i, i == 4 ? ZEEMOND : w("examen_" + i, ex[i - 1][0], ex[i - 1][1], ex[i - 1][2]));
        }
        int[][] wv = {{3420, 70, -984}, {3470, 60, -967}, {3520, 60, -955}};
        for (int i = 1; i <= wv.length; i++) {
            poi("z6_waterval_" + i, w("waterval_" + i, wv[i - 1][0], wv[i - 1][1], wv[i - 1][2]));
        }
        poi("z6_ziedershuisje", w("zoutziedershuisje", 3874, 69, -548));
        poi("z6_zoutgrot", w("zoutgrot_ingang", 3874, 69, -499));
        poi("z6_grot_kristallen", w("zoutgrot_puzzel", 3848, 50, -541));
        poi("z6_grot_schat", w("zoutgrot_schat", 3848, 50, -528));
        poi("z6_wormelien", w("wormelien_arena", 3700, 71, -600));
        poi("z6_kanaalwijk", w("kanaalwijk", 3700, 71, -770));
        poi("z6_dadelpalmen", w("palmenkloven_palmenbos", 3500, 62, -962));
        poi("z6_ruinedal", w("ruinedal", 3985, 90, -975));
        poi("z6_zoutvlakte", w("zoutvlakte", 3945, 69, -598));
        poi("z6_moerbeibomen", w("moerbei_1", 3433, 67, -957));
        // lookouts
        poi("z6_uitkijk_zeemond", w("uitkijk_zeemond", 3300, 96, -770));
        poi("z6_uitkijk_schatkamer", w("schatkamer_koepeltje", 3474, 118, -700));
        poi("z6_uitkijk_minaret", w("paleis_minaret", 3740, 118, -892));
        poi("z6_uitkijk_mesa", w("uitkijk_mesa", 3920, 71, -1010));
    }

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    // ---- areas (fallback boxes; u-z6wereld's own registration wins) + campfires ---------------------------------------------------
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minY, int maxY, int minLv, int maxLv, boolean stad) {
    }

    /** In registration order: each is put IN FRONT of the ones before it (Z6VerhaalRegistry.gebieden), so the zone box first, the cities last. */
    public static final List<Gebied> GEBIEDEN = List.of(
            new Gebied("z6_zijdeland", MIN_X, MIN_Z, MAX_X, MAX_Z, 0, 320, 60, 70, false),
            new Gebied("z6_palmenkloven", 3290, -1060, 3640, -860, 30, 160, 62, 64, false),
            new Gebied("z6_roze_duinen", 3510, -790, 3810, -460, 40, 160, 60, 62, false),
            new Gebied("z6_zoutvlakte", 3833, -716, 4057, -480, 0, 160, 64, 66, false),
            new Gebied("z6_ruinedal", 3860, -1060, 4110, -900, 30, 180, 66, 68, false),
            new Gebied("z6_moederoase", 4000, -880, 4180, -660, 30, 260, 68, 70, false),
            new Gebied("z6_rozenkloof", 3230, -790, 3500, -660, 40, 160, 60, 60, true),
            new Gebied("z6_saffraanpoort", 3634, -950, 3890, -742, 40, 160, 62, 62, true));
    /** Areas that are a safe town (the "veilig" tip). */
    public static final List<String> STEDEN = List.of("z6_rozenkloof", "z6_saffraanpoort");

    /** Area id -> where the tracker points for "in area x". */
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    /** Area id -> its main campfire (the tracker). */
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    /** The campfires (checkpoints; u-z6wereld places the blocks; the Verkenningsboek lists them). */
    public static final List<BlockPos> KAMPVUREN;

    static {
        GEBIED_DOEL.put("z6_zijdeland", SAFFRAAN);
        GEBIED_DOEL.put("z6_rozenkloof", PLEIN);
        GEBIED_DOEL.put("z6_saffraanpoort", SAFFRAAN);
        GEBIED_DOEL.put("z6_roze_duinen", w("duinen_karavaanweg", 3726, 70, -625));
        GEBIED_DOEL.put("z6_palmenkloven", w("watervallen_voet", 3538, 60, -957));
        GEBIED_DOEL.put("z6_zoutvlakte", w("zoutvlakte", 3945, 69, -598));
        GEBIED_DOEL.put("z6_ruinedal", w("ruinedal_kamp", 3960, 80, -940));
        GEBIED_DOEL.put("z6_moederoase", OASE);
        String[] ids = {"z6_rozenkloof", "z6_saffraanpoort", "z6_roze_duinen", "z6_palmenkloven", "z6_zoutvlakte", "z6_ruinedal", "z6_moederoase"};
        String[] vuur = {"rozenkloof_plein", "saffraanpoort_paleis", "duinput", "palmenkloven", "karavaankamp", "ruinedal", "moederoase"};
        int[][] kv = {{3394, 69, -718}, {3770, 71, -846}, {3616, 74, -665}, {3499, 60, -964}, {3908, 74, -580}, {3952, 71, -944}, {4054, 81, -824}};
        BlockPos[] l = new BlockPos[ids.length];
        for (int i = 0; i < ids.length; i++) {
            l[i] = w("kampvuur_" + vuur[i], kv[i][0], kv[i][1], kv[i][2]);
            KAMPVUUR.put(ids[i], l[i]);
        }
        KAMPVUREN = List.of(l);
    }

    // ---- lookouts (kern Uitkijken.panorama; seen = gezien "vr_w_uitkijk_<id>") ------------------------------------------------------
    public static final List<String> UITKIJK_IDS = List.of("z6_uitkijk_zeemond", "z6_uitkijk_schatkamer", "z6_uitkijk_minaret", "z6_uitkijk_mesa");

    // ---- secrets + side-quest finds ---------------------------------------------------------------------------------------------
    /** 8 knuffels hidden in the Zijdeland. */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    /** 3 treasures. */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    /** Side-quest finds per kind (only while their quest runs: {@link #VONDST_QUEST}). */
    public static final Map<String, Map<String, BlockPos>> VONDST = new LinkedHashMap<>();
    /** Find kind -> the quest it belongs to. */
    public static final Map<String, String> VONDST_QUEST = new LinkedHashMap<>();

    private static void vondst(String soort, String quest, String wereldId, int[][] p) {
        Map<String, BlockPos> m = new LinkedHashMap<>();
        String kort = soort.substring(3);
        for (int i = 0; i < p.length; i++) {
            String id = (soort.equals("z6_tabletten") ? "tablet_" : kort + "_") + (i + 1);
            m.put(id, w(wereldId + (i + 1), p[i][0], p[i][1], p[i][2]));
        }
        VONDST.put(soort, m);
        VONDST_QUEST.put(soort, quest);
    }

    static {
        int[][] kn = {{3436, 101, -684}, {3612, 98, -660}, {3480, 66, -986}, {3770, 96, -872}, {3950, 69, -540}, {3826, 104, -910},
                {3754, 72, -894}, {4090, 67, -800}};
        for (int i = 0; i < kn.length; i++) {
            KNUFFEL.put("knuffel_" + (i + 1), w("geheim_knuffel_" + (i + 1), kn[i][0], kn[i][1], kn[i][2]));
        }
        int[][] sc = {{3476, 73, -708}, {3740, 120, -892}, {4012, 122, -928}};
        String[] scId = {"schatkamer", "minaret", "mesa"};
        for (int i = 0; i < sc.length; i++) {
            SCHAT.put(scId[i], w("geheim_schat_" + (i + 1), sc[i][0], sc[i][1], sc[i][2]));
        }
        vondst("z6_taart", "z6_s01_verjaardag", "z6_taart_", new int[][] {{3394, 69, -716}, {3410, 69, -704}, {3388, 69, -724}});
        vondst("z6_post", "z6_s03_post", "rozenkloof_post_", new int[][] {{3350, 85, -752}, {3378, 85, -754}, {3412, 85, -676}, {3366, 101, -678},
                {3400, 101, -750}, {3440, 101, -680}});
        vondst("z6_kwastjes", "z6_s04_kwastjes", "kwastje_", new int[][] {{3744, 88, -810}, {3734, 69, -804}, {3700, 67, -790}, {3800, 84, -822},
                {3770, 76, -880}});
        vondst("z6_eendjes", "z6_s07_eendjes", "kanaalwijk_eend_", new int[][] {{3690, 66, -790}, {3694, 66, -800}, {3702, 66, -806}, {3710, 66, -794},
                {3684, 66, -784}, {3676, 66, -796}, {3698, 66, -778}, {3716, 66, -802}, {3688, 66, -812}, {3706, 66, -786}});
        vondst("z6_dadels", "z6_s09_dadelapen", "z6_dadels_", new int[][] {{3440, 74, -970}, {3452, 76, -980}, {3462, 72, -990}, {3434, 70, -962},
                {3470, 78, -976}, {3446, 72, -994}});
        vondst("z6_zoutkreeft", "z6_s11_zoutkreeft", "z6_zoutkreeft_", new int[][] {{3910, 69, -580}, {3936, 69, -566}, {3958, 69, -600}, {3896, 69, -610},
                {3920, 69, -540}});
        vondst("z6_tabletten", "z6_s14_tabletten", "tablet_", new int[][] {{3950, 90, -960}, {3972, 92, -984}, {4000, 96, -950}, {4020, 100, -990},
                {3990, 88, -1010}, {4040, 104, -970}});
        vondst("z6_cocons", "z6_hq06_tapijt", "moerbei_", new int[][] {{3490, 66, -966}, {3502, 66, -972}, {3512, 67, -962}, {3496, 66, -982},
                {3520, 68, -976}});
    }

    // ---- line E (the airship; Z6Lijn) -------------------------------------------------------------------------------------------
    /** The route from the Staarthaven (a) to Rozenkloof (b), world control points: over the strait, in over the pink cliffs. */
    public static final List<Vec3> LIJN_E_VIA = List.of(new Vec3(2990, 212, -540), new Vec3(3110, 205, -600), new Vec3(3230, 180, -700),
            new Vec3(3330, 150, -770), new Vec3(3385, 130, -785));

    // ---- scenes (camera keyframes: pos, look-at, pos, look-at; world coordinates) ------------------------------------------------
    public static final Map<String, Vec3[]> CAMERA = new LinkedHashMap<>();

    static {
        // the great swim: beside the whale, then racing east over the Wolkenzee to the pink cliffs, the coast below
        CAMERA.put("wolkwaltocht", new Vec3[] {new Vec3(2700, 250, -700), new Vec3(2720, 215, -520), new Vec3(3200, 170, -650), new Vec3(3420, 110, -730)});
        CAMERA.put("aankomst", new Vec3[] {new Vec3(3250, 170, -760), new Vec3(3400, 90, -720), Vec3.atCenterOf(AANKOMST).add(-8, 12, 10),
                Vec3.atCenterOf(PLEIN)});
        CAMERA.put("zoutkolos", new Vec3[] {Vec3.atCenterOf(BAKEN.get("zout")).add(18, 10, 18), Vec3.atCenterOf(BAKEN.get("zout")),
                Vec3.atCenterOf(BAKEN.get("zout")).add(-14, 6, 20), Vec3.atCenterOf(BAKEN.get("zout")).add(0, 4, -10)});
        CAMERA.put("kerker", new Vec3[] {Vec3.atCenterOf(KERKER).add(10, 12, 10), Vec3.atCenterOf(KERKER), Vec3.atCenterOf(KERKER).add(0, 3, 4),
                Vec3.atCenterOf(KERKER).add(0, -6, -6)});
        CAMERA.put("ontmasker", new Vec3[] {Vec3.atCenterOf(SPIEGELZAAL).add(8, 5, 8), Vec3.atCenterOf(SPIEGELZAAL), Vec3.atCenterOf(SPIEGELZAAL).add(-6, 3, -6),
                Vec3.atCenterOf(SPIEGELZAAL).add(0, 2, -6)});
        CAMERA.put("ontsnapt", new Vec3[] {Vec3.atCenterOf(OOSTPOORT).add(0, 6, 6), new Vec3(3990, 120, -760), Vec3.atCenterOf(OOSTPOORT).add(4, 10, 0),
                Vec3.atCenterOf(TOREN_TOP)});
        CAMERA.put("luchtspiegeling", new Vec3[] {Vec3.atCenterOf(poi("z6_luchtspiegeling")).add(-10, 8, 10), new Vec3(4060, 80, -720),
                new Vec3(4040, 110, -700), Vec3.atCenterOf(OASE)});
        CAMERA.put("bron", new Vec3[] {Vec3.atCenterOf(OASE).add(6, 4, 6), Vec3.atCenterOf(OASE), Vec3.atCenterOf(OASE).add(-4, 2, 5), Vec3.atCenterOf(OASE)});
        CAMERA.put("toren", new Vec3[] {Vec3.atCenterOf(TORENVOET).add(-14, 3, 12), Vec3.atCenterOf(TORENVOET).add(0, 30, 0),
                Vec3.atCenterOf(TORENVOET).add(-20, 30, 20), Vec3.atCenterOf(TOREN_TOP)});
        CAMERA.put("ringeling", new Vec3[] {Vec3.atCenterOf(TOREN_TOP).add(8, 3, 8), Vec3.atCenterOf(TOREN_TOP), Vec3.atCenterOf(TOREN_TOP).add(-5, 2, 6),
                Vec3.atCenterOf(TOREN_TOP)});
        // F4: far east, the Keizer's throne room is not built. W-INT: the first half looked at (4900, 200, -700), empty sky east of
        // the map; it now starts on the grey Sterrendovende Toren itself (built zone-6 terrain, the Vinger's tower) seen from the
        // east, and the look then sweeps west over the Zijdeland towards Hoogvads (the Oog turning west), as before.
        CAMERA.put("f4", new Vec3[] {Vec3.atCenterOf(TOREN_TOP).add(36, 18, 22), Vec3.atCenterOf(TOREN_TOP).add(0, -40, 0), new Vec3(4300, 300, -700),
                new Vec3(2700, 330, -520)});
    }

    /** The vision camera at a beacon (or anywhere): a slow lift above the spot towards the east. */
    public static Vec3[] visioen(BlockPos b) {
        Vec3 c = Vec3.atCenterOf(b);
        return new Vec3[] {c.add(4, 4, 4), c.add(0, 6, 0), c.add(-6, 16, 6), c.add(60, 40, 0)};
    }
}
