package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate of the zone-5 story slice (T-Z5VERHAAL): Hoogvads, the capital on the back of Moeder Wolkwal, floating above
 * the east end of zone 4 (WERELDINDELING W-3: whale x 2440..3150, z -660..-380, y 170-320; the head points WEST over the
 * Opstijgpier of Lampionhaven).
 * <p><b>PLACEHOLDERS.</b> The city geometry is t-z5wereld's (later). Until then every spot is {@code w("<poi id>", x, y, z)}: the
 * generated {@code zone5.wereld.Z5WereldPlekken.plek(id)} wins when that class exists (looked up by reflection, so this compiles
 * without it), else the number here. The numbers come from the concept renders (renders-hoogvads/_bron: stad.py ZONES/NIV,
 * wijken.py, markt2.py, kathedraal.py) through export.py's placement {@code gx = 2702 - x, gz = -520 - z, gy = y} (180 degrees:
 * local east = world west). District centres (world): palace/throne room (2816..2833, 253, -520), Raadszaal (2784, 256, -520),
 * market + fountain (2742, 239, -520), Grote Soepkeuken (2724, 239, -535), university (2737, 235, -465), Academie van de Wolkwacht
 * (2737, 235, -575), square (2696, 241, -520), Ordekathedraal (gate 2687, altar 2619, y 245), Luchthaven (2631, 237, -444),
 * Staartpark (2878, 236, -520), Ooggaarde (2551, 240, -580; eye balcony 2552, 230, -610). NPCs hover when no floor is built yet (Npcs.zet).
 * <p>(2026-11-06) Checked against renders-hoogvads/_bron/_export_nieuw/plekken.json (zones, cathedral gate/altar, balkon_oog);
 * the whale may still be lowered ~20 blocks: t-z5wereld's POI_Z5 / Z5WereldPlekken then wins by id.
 * <p>Ids are checked by tools/features/vadsrijk_t_z5verhaal.py (every NPC/POI needs a name/"where" text there).
 */
public final class Z5VerhaalPlekken {
    private Z5VerhaalPlekken() {
    }

    // ---- t-z5wereld's generated spots (reflection: the class does not exist yet) -------------------------------------------------
    private static Method wereldPlek;
    private static boolean gezocht;

    /** t-z5wereld's spot for this id, or null (no Z5WereldPlekken in this tree, or the id is not on its map). */
    @Nullable
    public static synchronized BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                wereldPlek = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken").getMethod("plek", String.class);
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

    // ---- the zone box (T-WERELD's VadsrijkZones.Zone.Z5: tile box x 2432..3199, z -768..-257, zone 5 from y 150 up) -------------
    public static final int MIN_X = 2432, MAX_X = 3199, MIN_Z = -768, MAX_Z = -257, VANAF_Y = 150;

    /** Is this point in Hoogvads (zone 5)? Above y 150 in the tile box (below it is zone 4's sea). */
    public static boolean zone5(double x, double y, double z) {
        return y >= VANAF_Y && x >= MIN_X && x < MAX_X + 1 && z >= MIN_Z && z < MAX_Z + 1;
    }

    // ---- key places ------------------------------------------------------------------------------------------------------------
    /** Where the first airship (and every later one) sets you down: the middle pier of the Luchthaven (north flank). */
    public static final BlockPos AANKOMST = w("luchthaven_aankomst", 2631, 237, -424);
    public static final BlockPos STATION = w("luchthaven_station", 2631, 237, -444);
    public static final BlockPos LAAN = w("zachtmoedlaan", 2696, 241, -520);
    public static final BlockPos MARKT = w("markt_fontein", 2748, 239, -520);
    public static final BlockPos SOEPKEUKEN = w("soepkeuken_ketel", 2724, 239, -535);
    public static final BlockPos RAADSZAAL = w("raadszaal_lessenaar", 2780, 256, -520);
    public static final BlockPos TROON = w("troonzaal_troon", 2831, 253, -520);
    public static final BlockPos ACADEMIE = w("academie_oefenplein", 2737, 235, -575);
    public static final BlockPos UNIVERSITEIT = w("universiteit_binnenhof", 2737, 235, -465);
    public static final BlockPos KATHEDRAAL_POORT = w("kathedraal_poort", 2687, 245, -520);
    public static final BlockPos KATHEDRAAL_ALTAAR = w("kathedraal_altaar", 2619, 248, -520);
    public static final BlockPos STAARTPARK = w("staartpark_vijver", 2878, 236, -520);
    public static final BlockPos OOGGAARDE = w("ooggaarde_tuin", 2551, 240, -580);
    public static final BlockPos BALKON_OOG = w("balkon_oog", 2552, 230, -610);
    /** The middle blowhole: the way down into the whale's dream (De Wolkwaldroom, the finale). */
    public static final BlockPos SPUITGAT = w("spuitgat_midden", 2557, 240, -520);
    /** The dream room inside the whale (t-z5wezens's arena; the tracker's target for the finale). */
    public static final BlockPos WOLKWALDROOM = w("wolkwaldroom_arena", 2600, 200, -520);
    /** Zone 4: the Opstijgpier of Lampionhaven under the whale's head (zone 4's id; a placeholder at sea level). */
    // U-QUESTROBOT: "opstijgpier" is zone 4's map id, not on the zone-5 map, so this was the placeholder 2452,66,-520 (open sea, ~75
    // blocks from the real pier: the tracker sent you into the water and the zone-5 ticker never ran on the pier); now zone 4's spot
    public static final BlockPos OPSTIJGPIER = wereld("opstijgpier") != null ? wereld("opstijgpier")
            : nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4VerhaalPlekken.POI.getOrDefault("z4_opstijgpier", new BlockPos(2512, 72, -565));

    // ---- the NPCs (id -> stand spot; they snap to the nearest floor within 12 blocks, or hover) ----------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();

    static void npc(String id, BlockPos b) {
        NPC.put(id, b);
    }

    static BlockPos naast(BlockPos b, int dx, int dz) {
        return b.offset(dx, 0, dz);
    }

    static {
        // story
        npc("z5_koningin", naast(TROON, -2, 0));
        npc("z5_inspecteur", w("raadszaal_zijkamer", 2776, 256, -530));
        npc("z5_havenmeester", w("luchthaven_havenmeester", 2635, 237, -447));
        npc("z5_veerkracht", w("academie_kapitein", 2740, 235, -568));
        npc("z5_plukje", w("academie_kazerne", 2726, 235, -586));
        npc("z5_professor", w("universiteit_bibliotheek", 2740, 235, -470));
        npc("z5_kok", naast(SOEPKEUKEN, 2, -2));
        npc("z5_mientje", w("soepkeuken_tafel", 2718, 239, -540));
        // the Hoge Raad: the four suspects
        npc("z5_gouddraad", w("bank_balie", 2762, 239, -502));
        npc("z5_inktvlek", w("universiteit_archief", 2732, 235, -458));
        npc("z5_donsveer", w("academie_toren", 2748, 235, -585));
        npc("z5_zachtpels", w("paleis_kanselarij", 2808, 253, -532));
        // the refugees (De Uitgevadsten) in the soup kitchen
        npc("z5_pluisje", w("soepkeuken_hoek", 2730, 239, -544));
        npc("z5_grauwsnor", w("soepkeuken_bank", 2714, 239, -530));
        for (int i = 1; i <= 4; i++) {
            npc("z5_vluchteling_" + i, w("soepkeuken_rij_" + i, 2716 + 2 * i, 239, -527));
        }
        // the city
        npc("z5_zuster", naast(KATHEDRAAL_ALTAAR, 4, 0));
        npc("z5_hofdame", w("paleis_hof", 2812, 253, -508));
        npc("z5_stekje", naast(STAARTPARK, -3, 3));
        npc("z5_lientje", naast(MARKT, -4, 6));
        npc("z5_bakker", w("markt_bakker", 2734, 239, -510));
        npc("z5_kleermaker", w("markt_kleermaker", 2756, 239, -530));
        npc("z5_smid", w("markt_smid", 2766, 239, -536));
        npc("z5_juwelier", w("markt_juwelier", 2730, 239, -506));
        npc("z5_stalmeester", naast(STATION, 10, 6));
        npc("z5_kadet_dons", w("luchthaven_pier_west", 2660, 237, -426));
        // the Ambachtsgilde at the university (professions capital, VOORSTEL_PROFESSIONS phase D: hooks only)
        npc("z5_gildemeester", w("gilde_hal", 2712, 235, -460));
        npc("z5_leermeester_koken", w("gilde_keuken", 2708, 235, -455));
        npc("z5_leermeester_brouwen", w("gilde_brouwerij", 2716, 235, -455));
        npc("z5_leermeester_smeden", w("gilde_smidse", 2708, 235, -466));
        npc("z5_leermeester_naaien", w("gilde_atelier", 2716, 235, -466));
        npc("z5_leermeester_imkeren", w("gilde_bijenstal", 2720, 235, -460));
        // the 4th-class teasers (not playable)
        npc("z5_vadszanger", naast(KATHEDRAAL_POORT, 4, 6));
        npc("z5_pluisninja", naast(ACADEMIE, -6, 8));
        // citizens
        int[][] bv = {{2700, 241, -510}, {2760, 239, -512}, {2690, 241, -530}, {2860, 236, -514}, {2560, 240, -570}, {2640, 237, -452}};
        for (int i = 1; i <= bv.length; i++) {
            npc("z5_burger_" + i, w("burger_" + i, bv[i - 1][0], bv[i - 1][1], bv[i - 1][2]));
        }
        // the Wolkwacht at the gates and campfires
        int[][] wv = {{2742, 253, -504}, {2742, 253, -536}, {2683, 245, -512}, {2645, 237, -452}, {2868, 236, -506}, {2557, 240, -568}};
        for (int i = 1; i <= wv.length; i++) {
            npc("z5_wacht_" + i, w("wolkwacht_" + i, wv[i - 1][0], wv[i - 1][1], wv[i - 1][2]));
        }
    }

    // ---- POIs: quest targets (radius POI_STRAAL, height POI_HOOGTE) ------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    public static final int POI_STRAAL = 5, POI_HOOGTE = 6, VIND_STRAAL = 3, VIND_HOOGTE = 4;

    static void poi(String id, BlockPos b) {
        POI.put(id, b);
    }

    static {
        poi("z5_opstijgpier", OPSTIJGPIER);
        poi("z5_aankomst", AANKOMST);
        poi("z5_laan", LAAN);
        poi("z5_troonzaal", TROON);
        poi("z5_raadszaal", RAADSZAAL);
        poi("z5_lessenaar", naast(RAADSZAAL, -1, 0));
        poi("z5_kastje", w("academie_kastjes", 2730, 235, -588));
        poi("z5_soepkeuken", SOEPKEUKEN);
        poi("z5_logboek", w("luchthaven_logboek", 2639, 237, -447));
        poi("z5_spuitgat", SPUITGAT);
        poi("z5_wolkwaldroom", WOLKWALDROOM);
        poi("z5_ooggaarde_balkon", BALKON_OOG);
        poi("z5_pier_oost", w("luchthaven_pier_oost", 2600, 237, -424));
        poi("z5_kanselarij", w("paleis_kanselarij_bureau", 2806, 253, -535));
        // side quests
        poi("z5_sterrenwacht", w("universiteit_sterrenwacht", 2772, 250, -440));
        poi("z5_spuitgat_noord", w("spuitgat_noord", 2562, 240, -495));
        poi("z5_spuitgat_zuid", w("spuitgat_zuid", 2562, 240, -545));
        for (int i = 1; i <= 4; i++) {
            int[][] pv = {{2722, 235, -565}, {2752, 235, -565}, {2752, 235, -590}, {2722, 235, -590}};
            poi("z5_parcours_" + i, w("academie_parcours_" + i, pv[i - 1][0], pv[i - 1][1], pv[i - 1][2]));
        }
        poi("z5_academie", ACADEMIE);
        for (int i = 1; i <= 4; i++) {
            int[][] bv = {{2870, 236, -540}, {2890, 236, -535}, {2890, 236, -505}, {2870, 236, -500}};
            poi("z5_bloesemboom_" + i, w("staartpark_boom_" + i, bv[i - 1][0], bv[i - 1][1], bv[i - 1][2]));
        }
        poi("z5_flap", w("kathedraal_apsis", 2612, 248, -520));
        // W-BEREIK: the generated spot lies on the light pool right under the window, behind the transept balustrade (7 from the
        // nearest walkable cell, POI_STRAAL 5): the quest spot is 4 blocks into the transept, in front of the balustrade
        poi("z5_roosvenster_noord", w("kathedraal_roos_noord", 2619, 248, -504).offset(0, 0, -4));
        poi("z5_roosvenster_zuid", w("kathedraal_roos_zuid", 2619, 248, -536).offset(0, 0, 4));
        poi("z5_eerste_knuffel", KATHEDRAAL_ALTAAR);
        // lookouts
        poi("z5_uitkijk_oog", BALKON_OOG);
        poi("z5_uitkijk_geveltoren", w("kathedraal_geveltoren", 2690, 290, -535));
        poi("z5_uitkijk_staartvin", w("staartvin_top", 2905, 248, -520));
        poi("z5_uitkijk_sterrenwacht", w("universiteit_sterrenwacht_top", 2772, 262, -440));
    }

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    // ---- areas (fallback boxes; t-z5wereld's own registration wins) + campfires + boss arenas --------------------------------------
    public record Gebied(String id, int minX, int minZ, int maxX, int maxZ, int minY, int maxY, int minLv, int maxLv, boolean stad) {
    }

    /** Small ones first (they are put in FRONT of the city box in this order reversed: see Z5VerhaalRegistry.gebieden). */
    public static final List<Gebied> GEBIEDEN = List.of(
            new Gebied("z5_hoogvads", 2440, -660, 3150, -380, 200, 320, 56, 60, true),
            new Gebied("z5_luchthaven", 2582, -466, 2680, -410, 200, 320, 56, 60, true),
            new Gebied("z5_staartpark", 2850, -580, 3150, -460, 200, 320, 56, 60, true),
            new Gebied("z5_ooggaarde", 2500, -625, 2580, -550, 200, 320, 56, 60, true),
            new Gebied("z5_wolkwaldroom", 2540, -600, 2700, -440, 150, 225, 58, 60, false));

    /** Area id -> where the tracker points for "in area x". */
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    /** Area id -> its main campfire (the tracker). */
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    /** Arena id -> its door. */
    public static final Map<String, BlockPos> BAAS = new LinkedHashMap<>();
    /** The campfires of Hoogvads (checkpoints; t-z5wereld places the blocks; the Verkenningsboek lists them). */
    public static final List<BlockPos> KAMPVUREN;

    static {
        GEBIED_DOEL.put("z5_hoogvads", LAAN);
        GEBIED_DOEL.put("z5_luchthaven", STATION);
        GEBIED_DOEL.put("z5_staartpark", STAARTPARK);
        GEBIED_DOEL.put("z5_ooggaarde", OOGGAARDE);
        GEBIED_DOEL.put("z5_wolkwaldroom", WOLKWALDROOM);
        BlockPos vuurHaven = w("kampvuur_luchthaven", 2650, 237, -452);
        BlockPos vuurMarkt = w("kampvuur_markt", 2735, 239, -505);
        BlockPos vuurPark = w("kampvuur_staartpark", 2868, 236, -510);
        BlockPos vuurOog = w("kampvuur_ooggaarde", 2555, 240, -572);
        KAMPVUREN = List.of(vuurHaven, vuurMarkt, vuurPark, vuurOog);
        KAMPVUUR.put("z5_luchthaven", vuurHaven);
        KAMPVUUR.put("z5_hoogvads", vuurMarkt);
        KAMPVUUR.put("z5_staartpark", vuurPark);
        KAMPVUUR.put("z5_ooggaarde", vuurOog);
        BAAS.put("z5_wolkwaldroom", SPUITGAT);
    }

    // ---- lookouts (kern Uitkijken.panorama; seen = gezien "vr_w_uitkijk_<id>") ------------------------------------------------------
    public static final List<String> UITKIJK_IDS = List.of("z5_uitkijk_oog", "z5_uitkijk_geveltoren", "z5_uitkijk_staartvin", "z5_uitkijk_sterrenwacht");

    // ---- secrets -----------------------------------------------------------------------------------------------------------------
    /** 8 knuffels hidden in Hoogvads. */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    /** 3 treasures. */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    /** 8 cloud berries (side quest "Een kom voor iedereen", only while it runs). */
    public static final Map<String, BlockPos> BES = new LinkedHashMap<>();
    /** 3 balls of wool for the queen's crown (side quest, only while it runs). */
    public static final Map<String, BlockPos> BOL = new LinkedHashMap<>();

    static {
        int[][] kn = {{2690, 290, -505}, {2905, 240, -540}, {2545, 241, -600}, {2665, 236, -418}, {2775, 262, -438}, {2760, 251, -548},
                {2835, 260, -520}, {2655, 248, -540}};
        for (int i = 0; i < kn.length; i++) {
            KNUFFEL.put("knuffel_" + (i + 1), w("geheim_knuffel_" + (i + 1), kn[i][0], kn[i][1], kn[i][2]));
        }
        int[][] sc = {{2912, 236, -520}, {2838, 253, -530}, {2540, 241, -560}};
        String[] scId = {"staartvin", "troonzaal", "ooggaarde"};
        for (int i = 0; i < sc.length; i++) {
            SCHAT.put(scId[i], w("geheim_schat_" + (i + 1), sc[i][0], sc[i][1], sc[i][2]));
        }
        int[][] be = {{2860, 236, -545}, {2885, 236, -548}, {2900, 236, -520}, {2884, 236, -492}, {2862, 236, -495}, {2540, 240, -575},
                {2548, 240, -590}, {2565, 240, -585}};
        for (int i = 0; i < be.length; i++) {
            BES.put("bes_" + (i + 1), w("wolkbes_" + (i + 1), be[i][0], be[i][1], be[i][2]));
        }
        int[][] bo = {{2745, 239, -528}, {2818, 253, -512}, {2735, 235, -472}};
        for (int i = 0; i < bo.length; i++) {
            BOL.put("bol_" + (i + 1), w("wolbol_" + (i + 1), bo[i][0], bo[i][1], bo[i][2]));
        }
    }

    // ---- scenes (camera keyframes: pos, look-at; world coordinates) --------------------------------------------------------------
    /** The first arrival: from below the head (the Opstijgpier), up along the whale's flank, over the rim, the city in gold. */
    public static final Vec3[] AANKOMST_SCENE = {
            new Vec3(2420, 90, -470), new Vec3(2480, 200, -520),        // 0: low over the sea, the whale's head above
            new Vec3(2470, 215, -455), new Vec3(2560, 230, -520),       // 140: along the flank, the eye with lashes
            new Vec3(2580, 290, -400), new Vec3(2690, 260, -520),       // 260: over the rim: the cathedral, towers, banners
            new Vec3(2640, 250, -405), Vec3.atCenterOf(AANKOMST)};      // 380: down to the Luchthaven
    public static final int[] AANKOMST_T = {0, 140, 260, 380};
    /** The unmasking in the Raadszaal (hq07). */
    public static final Vec3[] ONTMASKER = {
            Vec3.atCenterOf(RAADSZAAL).add(8, 4, 8), Vec3.atCenterOf(RAADSZAAL).add(0, 1, 0),
            Vec3.atCenterOf(RAADSZAAL).add(-6, 3, -6), Vec3.atCenterOf(RAADSZAAL).add(0, 1.5, 0)};
    /** The flight of the spy over the roofs to the blowhole. */
    public static final Vec3[] VLUCHT_SPION = {Vec3.atCenterOf(RAADSZAAL), Vec3.atCenterOf(MARKT).add(0, 8, 0), Vec3.atCenterOf(SPUITGAT).add(0, 2, 0)};
    /** Flash F3 (Mager Mientje's memory): a slow drift over the city to the grey east. */
    public static final Vec3[] HERINNERING = {
            Vec3.atCenterOf(SOEPKEUKEN).add(4, 6, 10), Vec3.atCenterOf(SOEPKEUKEN),
            new Vec3(2900, 280, -520), new Vec3(3400, 230, -520)};
    /** The finale fallback (no fight in this tree): into the dream, the grey cloud whale, the lullaby. */
    public static final Vec3[] DROOM = {
            Vec3.atCenterOf(SPUITGAT).add(0, 6, 0), Vec3.atCenterOf(SPUITGAT),
            Vec3.atCenterOf(WOLKWALDROOM).add(10, 6, 10), Vec3.atCenterOf(WOLKWALDROOM)};
    /** Waking Moeder Wolkwal at the eye (hq09). */
    public static final Vec3[] ONTWAKEN = {
            Vec3.atCenterOf(BALKON_OOG).add(6, 3, 6), new Vec3(2470, 225, -600),
            new Vec3(2420, 260, -660), new Vec3(2520, 230, -560)};
    /** The cliffhanger (hq10): from the east pier towards the Stormrand: the storm wall, De Gouden Greep. */
    public static final Vec3[] CLIFFHANGER = {
            Vec3.atCenterOf(poi("z5_pier_oost")).add(0, 3, 0), new Vec3(3300, 250, -520),
            new Vec3(3150, 290, -500), new Vec3(3600, 240, -520)};
    /** Where the grey mist bank and the flagship are drawn in the cliffhanger. */
    public static final Vec3 STORMRAND = new Vec3(3500, 240, -520);
}
