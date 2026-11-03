package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * EVERY coordinate the npc slice uses (SLICE_BRIEF: one class), after w-wereld/POI_VADSRIJK.md v0 (2026-11-03; y values marked
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

    // ---- Knuffelkamp (island (-128,900)..(128,1150), cliff plateau: stand y 87; POI sections 1-3) ----------------------------
    /** Where you arrive (POI AANKOMST, the land end of the jetty; kern's Gebieden.AANKOMST still says (0,65,1130)). */
    public static final BlockPos DOK = p(96, 87, 1015);
    public static final BlockPos KAMPPLEIN = p(0, 87, 1025);
    public static final BlockPos ORDETENT = p(0, 87, 1000);
    public static final BlockPos TRAINING = p(45, 87, 1000);
    public static final BlockPos TEMPELTJE = p(-45, 87, 1055);
    /** The pond with the slimes: the Grauwe Klodderreus lands here (the BAM; POI arena centre/radius/door). */
    public static final BlockPos KAMPVIJVER = p(54, 87, 1069);
    public static final int KAMPVIJVER_STRAAL = 18;
    public static final BlockPos KAMPVIJVER_DEUR = p(38, 87, 1058);
    public static final BlockPos VUURTOREN = p(-74, 92, 946);

    // ---- Zwamzoet (ground stand y 67) + De Haven (stand y 66) -------------------------------------------------------------------
    public static final BlockPos PLEIN = p(0, 67, 40);
    public static final BlockPos MARKT = p(0, 67, 50);
    /** In front of the Knuffeldeur of the Ordetempel on the highest cap. */
    public static final BlockPos ORDETEMPEL = p(0, 180, -29);
    /** The airship dock of De Haven (bell + captain at the foot of the mast). */
    public static final BlockPos HAVEN = p(86, 66, 488);
    /** Where the airship lands you at the haven. */
    public static final BlockPos HAVEN_AANKOMST = p(88, 66, 484);
    /** The Mika-spion's stall (only for our fallback disguised NPC; bazen's MikaSpion uses BazenPlekken.MIKA_KRAAM). */
    public static final BlockPos MIKA_KRAAM = p(-27, 67, 50);

    // ---- the zone ------------------------------------------------------------------------------------------------------------
    public static final BlockPos KONING_KLODDER_ARENA = p(410, 70, 90);
    public static final BlockPos GROTKAMP = p(-260, 68, -300);
    public static final BlockPos ZWAMTEMPEL_TRAP = p(330, 152, -380);

    // ---- NPC spots (id -> spot); yaw in Npcs ----------------------------------------------------------------------------------
    public static final Map<String, BlockPos> NPC = new LinkedHashMap<>();
    static {
        // Knuffelkamp
        NPC.put("rekruteerder", p(92, 87, 1020));
        NPC.put("kapitein", p(102, 87, 1018));
        NPC.put("kok", p(4, 87, 1029));
        NPC.put("priesteres", p(-45, 87, 1048));
        NPC.put("trainer_ridder", p(40, 87, 995));
        NPC.put("trainer_jager", p(51, 87, 995));
        NPC.put("trainer_magier", p(46, 87, 1006));
        NPC.put("visser_dobber", p(36, 87, 1072));
        // Zwamzoet: the order, trade and services (POI section 5: inside the buildings)
        NPC.put("grootmeester", p(3, 180, -27));
        NPC.put("smid", p(-44, 67, 49));
        NPC.put("winkelier", p(-38, 67, 71));
        NPC.put("wapenhandelaar", p(-29, 67, 18));
        NPC.put("marktmeester", p(43, 67, 49));
        NPC.put("bankier", p(29, 67, 18));
        NPC.put("kleermaker", p(38, 67, 71));
        NPC.put("klerk", p(-4, 67, 32));
        NPC.put("stalmeester", p(-146, 67, 2));
        NPC.put("havenmeester", p(-64, 67, 74));
        NPC.put("luchtschipper", p(84, 66, 492));
        NPC.put("akim_kaasguh", MIKA_KRAAM);
        // Zwamzoet: citizens with side quests (around the plein and the market)
        NPC.put("oma_truus", p(-18, 67, 36));
        NPC.put("bakker_bram", p(22, 67, 62));
        NPC.put("kleine_kiki", p(6, 67, 44));
        NPC.put("dichter_daan", p(-10, 67, 64));
        NPC.put("postguh_pim", p(12, 67, 30));
        NPC.put("tuinier_tilly", p(30, 67, 36));
        NPC.put("muzikant_mo", p(-4, 67, 56));
        NPC.put("professor_pluis", p(-20, 67, 24));
        NPC.put("ridder_rammel", p(16, 67, 74));
        NPC.put("kapster_fohnie", p(-22, 67, 62));
        NPC.put("schilder_sien", p(-30, 67, 40));
        NPC.put("slaapkop_sjors", p(24, 67, 44));
        NPC.put("koopman_kees", p(-12, 67, 76));
        NPC.put("weerguh_wolkje", p(8, 67, 22));
        NPC.put("tweeling_tip", p(-8, 67, 42));
        NPC.put("wachter_dikkie", p(5, 67, 80));
        // out in the zone
        NPC.put("boswachter_mos", p(-214, 68, 6));
        NPC.put("tweeling_tap", p(-382, 68, 62));
        NPC.put("mijnwerker_mopje", p(-430, 50, -310));
        NPC.put("tempelwachter_steeltje", p(326, 152, -382));
    }

    /** The campfires with a ridder-guh (ronde 12: 2-3 per area): next to the POI campfires (y: the floor search finds it). */
    public static final Map<String, BlockPos> KAMPWACHT = new LinkedHashMap<>();
    static {
        KAMPWACHT.put("kampwacht_weide_1", p(232, 68, -128));
        KAMPWACHT.put("kampwacht_weide_2", p(412, 68, 202));
        KAMPWACHT.put("kampwacht_weide_3", p(482, 68, -78));
        KAMPWACHT.put("kampwacht_woud_1", p(-218, 68, 12));
        KAMPWACHT.put("kampwacht_woud_2", p(-428, 68, 232));
        KAMPWACHT.put("kampwacht_woud_3", p(-298, 68, 342));
        KAMPWACHT.put("kampwacht_grotten_1", p(-258, 68, -298));
        KAMPWACHT.put("kampwacht_grotten_2", p(-458, 68, -458));
        KAMPWACHT.put("kampwacht_grotten_3", p(-178, 68, -478));
        KAMPWACHT.put("kampwacht_tempel_1", p(202, 70, -238));
        KAMPWACHT.put("kampwacht_tempel_2", p(402, 70, -298));
        KAMPWACHT.put("kampwacht_tempel_3", p(333, 152, -378));
    }

    // ---- points of interest (quest objectives "poi:<id>") --------------------------------------------------------------------
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();
    static {
        POI.put("kampplein", KAMPPLEIN);
        POI.put("vuurtoren", VUURTOREN);
        POI.put("dobberhoed", p(-60, 87, 955));
        POI.put("kampvuur_weide_1", p(230, 68, -130));
        POI.put("grotkamp", GROTKAMP);
        POI.put("tempelpoort", p(330, 152, -386));
        POI.put("hoedhupperwei", p(520, 68, 300));
        POI.put("tuba", p(-470, 68, 330));
        POI.put("kristalkamer", p(-490, 52, -520));
        // the grey scars (POI section 8; wereld recolours them after zone1_klaar)
        POI.put("litteken_weide", p(380, 68, 320));
        POI.put("litteken_woud", p(-240, 68, 300));
        POI.put("litteken_tempel", p(160, 68, -280));
        // the lookouts (no teleport, ronde 11; top stands, y ~)
        POI.put("uitkijk_klodderkijk", p(520, 150, -240));
        POI.put("uitkijk_zwamnaald", p(-510, 150, -40));
        POI.put("uitkijk_druipspits", p(-100, 160, -510));
        POI.put("uitkijk_haventand", p(160, 140, 380));
        POI.put("uitkijk_slijmnaald", p(460, 140, 380));
    }
    /** Horizontal radius / vertical tolerance for "you are there" (lookout tops are only known after the tiles: 40). */
    public static final double POI_STRAAL = 6, POI_HOOGTE = 10, UITKIJK_HOOGTE = 40;

    /** The 5 old clues to the Mika-spion (POI section 9). Found by walking up to them (radius 3). */
    public static final Map<String, BlockPos> AANWIJZING = new LinkedHashMap<>();
    static {
        AANWIJZING.put("pluisplukje", p(-27, 67, 54));     // black fluff under a market stall
        AANWIJZING.put("pootjes", p(106, 66, 522));        // Mika footprints in the haven warehouse
        AANWIJZING.put("vermomming", p(-380, 68, 60));      // a torn disguise cloak in a hollow mushroom, Sporenwoud
        AANWIJZING.put("krassen", p(-488, 52, -516));       // Mika scratches in the crystal chamber, Druipgrotten
        AANWIJZING.put("uitkijkpost", p(66, 157, -36));     // the spy's lookout on the roof of the east cap
    }

    /** Hidden treasure spots / rooms (POI section 9; one-time rewards). */
    public static final Map<String, BlockPos> SCHAT = new LinkedHashMap<>();
    static {
        SCHAT.put("kristalkamer", p(-492, 52, -524));
        SCHAT.put("waterval", p(-470, 70, 0));
        SCHAT.put("slaapheuvel", p(300, 66, 300));
        SCHAT.put("schildpad", p(520, 66, 720));
        SCHAT.put("vuurtorenkelder", p(-84, 86, 937));
    }

    /** The 10 lost knuffels (collectibles; Kleine Kiki's quest). */
    public static final Map<String, BlockPos> KNUFFEL = new LinkedHashMap<>();
    static {
        KNUFFEL.put("vuurtoren", p(-68, 92, 952));
        KNUFFEL.put("tentje", p(30, 87, 1040));
        KNUFFEL.put("plein", p(-40, 67, 30));
        KNUFFEL.put("steeltje", p(70, 67, -70));
        KNUFFEL.put("haven", p(110, 66, 470));
        KNUFFEL.put("slaapheuvel", p(300, 70, 250));
        KNUFFEL.put("weidevijver", p(470, 68, 185));
        KNUFFEL.put("holle_boom", p(-400, 68, -100));
        KNUFFEL.put("druipnis", p(-300, 62, -226));
        KNUFFEL.put("tempeltrap", p(326, 152, -374));
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
            new Vec3(270, 92, 1070), new Vec3(60, 90, 1030),        // far out at sea, the cliff island ahead
            new Vec3(170, 91, 1025), new Vec3(96, 88, 1015),        // the airship nears the jetty
            new Vec3(112, 100, 1010), new Vec3(0, 140, 0),          // turn: the far skyline of Zwamzoet
            new Vec3(99, 89, 1016), new Vec3(80, 88, 1020)};        // down to the jetty (the player's eyes)
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
    public static final BlockPos KAMP_AANKOMST_TERUG = DOK;
}
