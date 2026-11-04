package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * EVERY coordinate the npc slice uses (SLICE_BRIEF: one class). Integration 1.10.0: aligned with the real map (WereldPlekken /
 * POI_VADSRIJK.md v1; Zwamzoet stand y 64, camp plateau 87, temple 152); before: w-wereld/POI_VADSRIJK.md v0 (2026-11-03; y values marked
 * "~" there become exact with v1, integration aligns them). NPCs snap to the nearest floor within 12 blocks of their y
 * ({@link Npcs}), so a y close to the ground is enough. Spots POI v0 does not list (the camp's trainers, the citizens) are
 * chosen near the buildings it does list; requests in guhs_vadsrijk/POI_VERZOEKEN.md, section "npc".
 * <p>Points of interest ({@link #POI}) are what quest objectives ("poi:&lt;id&gt;"), clues, treasures and knuffels point at.
 */
public final class NpcPlekken {
    private NpcPlekken() {
    }

    static BlockPos p(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    /** A spot of the real map (integration 1.10.0: WereldPlekken / POI_VADSRIJK.md v1). */
    static BlockPos w(String id) {
        return WereldPlekken.plek(id);
    }

    /** A spot of the real map, moved a little (NPCs that stand next to a POI). */
    static BlockPos w(String id, int dx, int dz) {
        return WereldPlekken.plek(id).offset(dx, 0, dz);
    }

    // ---- Knuffelkamp (island (-128,900)..(128,1150), cliff plateau: stand y 87; POI sections 1-3) ----------------------------
    /** Where the airship lands you at the camp (the east dock on the cliff top). You first ARRIVE at kern's Gebieden.AANKOMST
     * (the low south jetty (0,65,1130), a stair leads up the cliff). */
    public static final BlockPos DOK = WereldPlekken.KAMP_LANDING;
    public static final BlockPos KAMPPLEIN = w("kampvuur_kamp");
    public static final BlockPos ORDETENT = w("ordetent");
    public static final BlockPos TRAINING = w("training");
    public static final BlockPos TEMPELTJE = w("tempeltje");
    /** The pond with the slimes: the Grauwe Klodderreus lands here (the BAM; POI arena centre/radius/door). */
    public static final BlockPos KAMPVIJVER = w("arena_klodderreus.centrum");
    public static final int KAMPVIJVER_STRAAL = 18;
    public static final BlockPos KAMPVIJVER_DEUR = w("arena_klodderreus.deur");
    public static final BlockPos VUURTOREN = w("kampvuur_vuurtoren");

    // ---- Zwamzoet (ground stand y 64) + De Haven (stand y 65) -------------------------------------------------------------------
    public static final BlockPos PLEIN = w("plein");
    public static final BlockPos MARKT = w("mika_spion_kraam", 10, 0);
    /** In front of the Knuffeldeur of the Ordetempel on the highest cap. */
    public static final BlockPos ORDETEMPEL = WereldPlekken.DEUREN.get(0).voor();   // (the "ordetempel" door)
    /** The airship dock of De Haven (bell + captain at the foot of the mast). */
    public static final BlockPos HAVEN = WereldPlekken.HAVEN_BEL;
    /** Where the airship lands you at the haven. */
    public static final BlockPos HAVEN_AANKOMST = WereldPlekken.HAVEN_LANDING;
    /** The Mika-spion's stall (only for our fallback disguised NPC; bazen's MikaSpion uses BazenPlekken.MIKA_KRAAM). */
    public static final BlockPos MIKA_KRAAM = w("mika_spion_kraam");

    // ---- the zone ------------------------------------------------------------------------------------------------------------
    public static final BlockPos KONING_KLODDER_ARENA = w("arena_koning_klodder.centrum");
    public static final BlockPos GROTKAMP = w("kampvuur_druipgrotten_1");
    public static final BlockPos ZWAMTEMPEL_TRAP = p(330, 152, -380);

    // ---- NPC spots (id -> spot); yaw in Npcs ----------------------------------------------------------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();
    static {
        // Knuffelkamp
        NPC.put("rekruteerder", w("aankomst", 1, 4));   // (on the south jetty, where you arrive)
        NPC.put("kapitein", w("luchtschip_kapitein"));
        NPC.put("kok", w("kampvuur_kamp", 4, 4));
        NPC.put("priesteres", w("tempeltje"));   // (on the plinth in front of the klasse-tempeltje)
        NPC.put("trainer_ridder", w("training", -5, -1));   // (fix round 1: -4 stood inside a dummy's fence arm)
        NPC.put("trainer_jager", w("training", 5, -1));
        NPC.put("trainer_magier", w("training", 0, 6));
        NPC.put("visser_dobber", p(36, 87, 1072));
        // Zwamzoet: the order, trade and services (POI section 5: inside the buildings)
        NPC.put("grootmeester", w("ordetempel_binnen"));
        NPC.put("smid", w("gebouw.smidse.1.binnen"));
        NPC.put("winkelier", w("gebouw.kruidenier.1.binnen"));
        NPC.put("wapenhandelaar", w("gebouw.uitrusting.1.binnen"));
        NPC.put("marktmeester", w("gebouw.veilinghuis.1.binnen"));
        NPC.put("bankier", w("gebouw.bank.1.binnen"));
        NPC.put("kleermaker", w("gebouw.kleermaker.1.binnen"));
        NPC.put("klerk", w("questbord", 2, 1));
        NPC.put("stalmeester", w("stal.binnen"));
        NPC.put("havenmeester", w("gebouw.havenmeester.1.binnen"));
        NPC.put("luchtschipper", w("luchtschip_haven_kapitein"));
        NPC.put("akim_kaasguh", MIKA_KRAAM);
        // Zwamzoet: citizens with side quests (around the plein and the market)
        NPC.put("oma_truus", p(-18, 64, 36));
        NPC.put("bakker_bram", p(22, 64, 62));
        NPC.put("kleine_kiki", p(6, 64, 44));
        NPC.put("dichter_daan", p(-10, 64, 64));
        NPC.put("postguh_pim", p(12, 64, 30));
        NPC.put("tuinier_tilly", p(30, 64, 36));
        NPC.put("muzikant_mo", p(-4, 64, 56));
        NPC.put("professor_pluis", p(-20, 64, 24));
        NPC.put("ridder_rammel", p(16, 64, 74));
        NPC.put("kapster_fohnie", p(-22, 64, 62));
        NPC.put("schilder_sien", p(-30, 64, 40));
        NPC.put("slaapkop_sjors", p(24, 64, 44));
        NPC.put("koopman_kees", p(-12, 64, 76));
        NPC.put("weerguh_wolkje", p(8, 64, 22));
        NPC.put("tweeling_tip", p(-8, 64, 42));
        NPC.put("wachter_dikkie", p(5, 64, 80));
        // out in the zone
        NPC.put("boswachter_mos", w("kampvuur_sporenwoud_1", 6, -4));
        NPC.put("tweeling_tap", p(-382, 68, 62));
        NPC.put("mijnwerker_mopje", w("npc_mopje"));
        NPC.put("tempelwachter_steeltje", p(326, 152, -382));
    }

    /** The campfires with a ridder-guh (ronde 12: 2-3 per area): next to the POI campfires (y: the floor search finds it). */
    public static final Map<String, BlockPos> KAMPWACHT = new LinkedHashMap<>();
    static {
        KAMPWACHT.put("kampwacht_weide_1", w("ridder_klodderweide_1"));
        KAMPWACHT.put("kampwacht_weide_2", w("ridder_klodderweide_2"));
        KAMPWACHT.put("kampwacht_weide_3", w("ridder_klodderweide_3"));
        KAMPWACHT.put("kampwacht_woud_1", w("ridder_sporenwoud_1"));
        KAMPWACHT.put("kampwacht_woud_2", w("ridder_sporenwoud_2"));
        KAMPWACHT.put("kampwacht_woud_3", w("ridder_sporenwoud_3"));
        KAMPWACHT.put("kampwacht_grotten_1", w("ridder_druipgrotten_1"));
        KAMPWACHT.put("kampwacht_grotten_2", w("ridder_druipgrotten_2"));
        KAMPWACHT.put("kampwacht_grotten_3", w("ridder_druipgrotten_3"));
        KAMPWACHT.put("kampwacht_tempel_1", w("ridder_zwamtempel_1"));
        KAMPWACHT.put("kampwacht_tempel_2", w("ridder_zwamtempel_2"));
        KAMPWACHT.put("kampwacht_tempel_3", w("kampvuur_zwamtempel_trap", 2, 2));
    }

    // ---- points of interest (quest objectives "poi:<id>") --------------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    static {
        POI.put("kampplein", KAMPPLEIN);
        POI.put("vuurtoren", VUURTOREN);
        POI.put("dobberhoed", w("quest_dobber_hoed"));
        POI.put("kampvuur_weide_1", w("kampvuur_klodderweide_1"));
        POI.put("grotkamp", GROTKAMP);
        POI.put("tempelpoort", w("deur_zwamtempel.voor"));
        POI.put("hoedhupperwei", w("hoedhupperwei"));
        POI.put("tuba", w("quest_tuba"));
        POI.put("kristalkamer", w("kristalkamer"));
        // the grey scars (POI section 8; wereld recolours them after zone1_klaar)
        POI.put("litteken_weide", p(380, 68, 320));
        POI.put("litteken_woud", p(-240, 68, 300));
        POI.put("litteken_tempel", p(145, 88, -265));   // (integration: on the scar beside the rerouted mountain path; 160,-280 is in the Kloof wall now)
        // the lookouts (no teleport, ronde 11; top stands, y ~)
        POI.put("uitkijk_klodderkijk", w("uitkijk_klodderkijk"));
        POI.put("uitkijk_zwamnaald", w("uitkijk_zwamnaald"));
        POI.put("uitkijk_druipspits", w("uitkijk_druipspits"));
        POI.put("uitkijk_haventand", w("uitkijk_haventand"));
        POI.put("uitkijk_slijmnaald", w("uitkijk_slijmnaald"));
    }
    // ---- quest tracker targets (fix round 1: the tracker + beam point here; texts vadsrijk.npc.plek.*) -------------------------
    /** Where the tracker points for "go to / cheer up in area x" (a campfire or the entrance of the area). */
    public static final Map<String, BlockPos> GEBIED_DOEL = new LinkedHashMap<>();
    /** The first campfire of each area (KAMPVUUR objectives). */
    public static final Map<String, BlockPos> KAMPVUUR = new LinkedHashMap<>();
    /** The boss arenas: "vr_klodderreus" (an entity path) or an area id (BAAS "gebied:x"). */
    public static final Map<String, BlockPos> BAAS = new LinkedHashMap<>();
    static {
        GEBIED_DOEL.put("knuffelkamp", KAMPPLEIN);
        GEBIED_DOEL.put("zwamzoet", PLEIN);
        GEBIED_DOEL.put("haven", HAVEN_AANKOMST);
        GEBIED_DOEL.put("klodderweide", w("kampvuur_klodderweide_1"));
        GEBIED_DOEL.put("sporenwoud", w("kampvuur_sporenwoud_1"));
        GEBIED_DOEL.put("druipgrotten", w("grot_ingangen.1"));
        GEBIED_DOEL.put("zwamtempel", w("deur_zwamtempel.voor"));
        KAMPVUUR.put("knuffelkamp", KAMPPLEIN);
        KAMPVUUR.put("zwamzoet", w("kampvuur_zwamzoet"));
        KAMPVUUR.put("haven", w("kampvuur_haven"));
        KAMPVUUR.put("klodderweide", w("kampvuur_klodderweide_1"));
        KAMPVUUR.put("sporenwoud", w("kampvuur_sporenwoud_1"));
        KAMPVUUR.put("druipgrotten", GROTKAMP);
        KAMPVUUR.put("zwamtempel", w("kampvuur_zwamtempel_1"));
        BAAS.put("vr_klodderreus", KAMPVIJVER);
        BAAS.put("knuffelkamp", KAMPVIJVER);
        BAAS.put("klodderweide", w("arena_koning_klodder.deur"));
        BAAS.put("zwamtempel", w("arena_opperzwam.deur"));
    }

    /** Horizontal radius / vertical tolerance for "you are there" (lookout tops are only known after the tiles: 40). */
    public static final double POI_STRAAL = 6, POI_HOOGTE = 10, UITKIJK_HOOGTE = 40;

    /** The 5 old clues to the Mika-spion (POI section 9). Found by walking up to them (radius 3). */
    public static final Map<String, BlockPos> AANWIJZING = new LinkedHashMap<>();
    static {
        AANWIJZING.put("pluisplukje", w("mika_aanwijzing_1"));     // black fluff under a market stall
        AANWIJZING.put("pootjes", w("mika_aanwijzing_2"));        // Mika footprints in the haven warehouse
        AANWIJZING.put("vermomming", w("mika_aanwijzing_3"));      // a torn disguise cloak in a hollow mushroom, Sporenwoud
        AANWIJZING.put("krassen", w("mika_aanwijzing_4"));       // Mika scratches in the crystal chamber, Druipgrotten
        AANWIJZING.put("uitkijkpost", w("mika_aanwijzing_5"));     // the spy's lookout on the roof of the east cap
    }

    /** Hidden treasure spots / rooms (POI section 9; one-time rewards). */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    static {
        SCHAT.put("kristalkamer", w("kristalkamer", -2, -4));
        SCHAT.put("waterval", w("geheim_waterval"));
        SCHAT.put("slaapheuvel", w("geheim_slaapheuvel"));
        SCHAT.put("schildpad", w("geheim_schildpad"));
        SCHAT.put("vuurtorenkelder", w("geheim_vuurtorenkelder"));
        SCHAT.put("hoedenschat", w("geheim_hoedenschat"));         // (fix verticaal) the secret hut on the Hoedenpaden
    }

    /** The 10 lost knuffels (collectibles; Kleine Kiki's quest). */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    static {
        KNUFFEL.put("vuurtoren", w("knuffel_1"));
        KNUFFEL.put("tentje", w("knuffel_2"));
        KNUFFEL.put("plein", w("knuffel_3"));
        KNUFFEL.put("steeltje", w("knuffel_4"));
        KNUFFEL.put("haven", w("knuffel_5"));
        KNUFFEL.put("slaapheuvel", w("knuffel_6"));
        KNUFFEL.put("weidevijver", w("knuffel_7"));
        KNUFFEL.put("holle_boom", w("knuffel_8"));
        KNUFFEL.put("druipnis", w("knuffel_9"));
        KNUFFEL.put("tempeltrap", w("knuffel_10"));
    }
    public static final double VIND_STRAAL = 3, VIND_HOOGTE = 8;

    @Nullable
    public static BlockPos poi(String id) {
        return POI.get(id);
    }

    public static double hoogteVan(String poi) {
        return poi.startsWith("uitkijk_") ? UITKIJK_HOOGTE : POI_HOOGTE;
    }

    // ---- scenes (camera keyframes, world coordinates) ----------------------------------------------------------------------
    /** Opening: low over the sea east of the island at sunrise, gliding to the jetty, then the far Zwamzoet skyline. */
    public static final Vec3[] OPENING = {
            new Vec3(190, 78, 1260), new Vec3(10, 82, 1110),        // far out at sea south-east, the cliff island ahead
            new Vec3(70, 72, 1180), new Vec3(0, 74, 1120),          // gliding in towards the south jetty and the cliff stair
            new Vec3(20, 90, 1150), new Vec3(0, 140, 0),            // turn: the far skyline of Zwamzoet
            new Vec3(1, 67, 1134), new Vec3(0, 73, 1118)};          // down to the jetty (the player's eyes, looking up the stair)        // down to the jetty (the player's eyes)
    /** The BAM: from the campfire, looking up over the water to the north-east, where the dragon comes over. */
    public static final Vec3[] BAM = {
            new Vec3(0, 89, 1031), new Vec3(60, 120, 960),
            new Vec3(-4, 89, 1033), new Vec3(-40, 125, 1060),
            new Vec3(30, 93, 1045), new Vec3(54, 87, 1069)};
    /** The dragon's flight line over the camp (fallback FX when the wezens DraakVlucht is not in this tree). */
    public static final Vec3 DRAAK_VAN = new Vec3(260, 140, 880), DRAAK_NAAR = new Vec3(-260, 130, 1180);
    /** The airship flight (fallback when wereld's WereldReizen is not in this tree): jetty -> over the sea -> haven. */
    public static final Vec3[] VLUCHT_NAAR_STAD = {
            new Vec3(100, 95, 1012), new Vec3(90, 100, 900),
            new Vec3(95, 120, 820), new Vec3(0, 150, 0),
            new Vec3(90, 105, 560), new Vec3(86, 90, 488),
            new Vec3(88, 72, 494), new Vec3(60, 67, 470)};
    public static final Vec3[] VLUCHT_NAAR_KAMP = {
            new Vec3(86, 90, 488), new Vec3(95, 100, 600),
            new Vec3(95, 110, 760), new Vec3(96, 90, 1015),
            new Vec3(100, 92, 1010), new Vec3(80, 88, 1020)};
    /** The cliffhanger: the Ordetempel on the highest cap, the far horizon where the dragon circles. */
    public static final Vec3[] CLIFFHANGER = {
            new Vec3(4, 184, -18), new Vec3(0, 182, -35),
            new Vec3(10, 195, -10), new Vec3(600, 240, -900),
            new Vec3(-20, 205, 30), new Vec3(500, 250, -800)};
    public static final BlockPos KAMP_AANKOMST_TERUG = WereldPlekken.KAMP_LANDING;
}
