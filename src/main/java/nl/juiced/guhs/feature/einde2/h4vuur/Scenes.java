package nl.juiced.guhs.feature.einde2.h4vuur;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 phase B, slice h4_vuur: the scripts of the cutscenes of chapter 4 and the finale (CONTRACT_E2_B 5; texts:
 * tools/features/einde2_h4_vuur_tekst.py SCENES, keys {@code scene.guhs.einde2_h4_vuur_<naam>.*}). Every position is relative
 * to the scene's anchor marker and is worked out from the build's own markers ({@link #r}), so a scene stands where the
 * build has its spots; every scene steps its camera out of a wall ({@code cameraOntwijkt}). 1.4.4: the story waits for a
 * click after every spoken line; only the credit roll of {@link #AFTITELING} runs on ({@code lied}).
 * <p>
 * Lengths (running time without the waits; DESIGN: normal 10-30 s, big moments 45-90 s): see {@link #LENGTES} and the
 * slice report.
 */
public final class Scenes {
    public static final String ID = "einde2_h4_vuur_";
    private static final String S = Plek.STRUCTUUR;

    public static Cutscene AANKOMST, SCHOOL, ZWARTE_ZON, GEVANGENIS, KAMP, KOKEND_ROTSBLOK, DRAKEN, TONEEL, TENT, TOP, LEEUWSCHILDPAD,
            AGNI_KAI, OMSLAG, TREK_WEG, KRONING, AFTITELING, KNIPOOG_STITCH, KNIPOOG_SJOKKEL;
    /** The marker of the Geestenwereld the Top scene is anchored to (top_landing once geest_bouw has it). */
    public static String TOP_ANKER = "fang_landing";
    /** Scene name -> {least, most} seconds it may last (the contract's bounds; checked by the game test). */
    public static final java.util.Map<String, int[]> LENGTES = java.util.Map.ofEntries(
            java.util.Map.entry("aankomst", new int[] {10, 20}), java.util.Map.entry("school", new int[] {10, 20}),
            java.util.Map.entry("zwarte_zon", new int[] {60, 90}), java.util.Map.entry("gevangenis", new int[] {20, 30}),
            java.util.Map.entry("kamp", new int[] {20, 30}), java.util.Map.entry("kokend_rotsblok", new int[] {20, 30}),
            java.util.Map.entry("draken", new int[] {45, 60}), java.util.Map.entry("toneel", new int[] {45, 60}),
            java.util.Map.entry("tent", new int[] {45, 75}), java.util.Map.entry("top", new int[] {30, 45}),
            java.util.Map.entry("leeuwschildpad", new int[] {45, 60}), java.util.Map.entry("agni_kai", new int[] {45, 75}),
            java.util.Map.entry("omslag", new int[] {30, 45}), java.util.Map.entry("trek_weg", new int[] {30, 45}),
            java.util.Map.entry("kroning", new int[] {45, 90}), java.util.Map.entry("aftiteling", new int[] {60, 90}),
            java.util.Map.entry("knipoog_stitch", new int[] {5, 10}), java.util.Map.entry("knipoog_sjokkel_finish", new int[] {5, 10}));

    private Scenes() {
    }

    static void registreer() {
        AANKOMST = aankomst();
        SCHOOL = school();
        ZWARTE_ZON = zwarteZon();
        GEVANGENIS = gevangenis();
        KAMP = kamp();
        KOKEND_ROTSBLOK = kokendRotsblok();
        DRAKEN = draken();
        TONEEL = toneel();
        TENT = tent();
        String landing = Finale.eerste(Finale.LANDING);
        TOP_ANKER = landing == null ? "fang_landing" : landing;
        TOP = top();
        LEEUWSCHILDPAD = leeuwschildpad();
        AGNI_KAI = agniKai();
        OMSLAG = omslag();
        TREK_WEG = trekWeg();
        KRONING = kroning();
        AFTITELING = aftiteling();
        KNIPOOG_STITCH = knipoogStitch();
        KNIPOOG_SJOKKEL = knipoogSjokkel();
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** The template position of a marker (null: not in the sidecar). */
    private static BlockPos pos(String structuur, String id) {
        Plekken.Plek p = Plekken.plek(structuur, id);
        return p == null ? null : p.pos();
    }

    /**
     * Where marker {@code plek} is, seen from marker {@code anker} (the middle of its floor), plus an offset; a marker that
     * is not there (an older build) counts as the anchor itself.
     */
    static Vec3 r(String anker, String plek, double dx, double dy, double dz) {
        return r(S, anker, plek, dx, dy, dz);
    }

    static Vec3 r(String structuur, String anker, String plek, double dx, double dy, double dz) {
        BlockPos a = pos(structuur, anker), b = pos(structuur, plek);
        if (a == null || b == null) {
            return new Vec3(0.5 + dx, dy, 0.5 + dz);
        }
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static Cutscene.Builder vuur(String naam, int duur) {
        return Cutscene.maak(ID + naam).duur(duur).bij(E2Verhaal.VUUR).kaart(E2H4Vuur.KAART).cameraOntwijkt();
    }

    private static Cutscene.Builder finale(String naam, int duur) {
        return Cutscene.maak(ID + naam).duur(duur).bij(E2Verhaal.FINALE).kaart(E2H4Vuur.KAART).cameraOntwijkt();
    }

    // =====================================================================================================================
    // chapter 4
    // =====================================================================================================================

    /** Arrival at the landing field, in disguise (anchor aankomst_speler). 16 s. */
    private static Cutscene aankomst() {
        String a = "aankomst_speler";
        Cutscene.Builder b = vuur("aankomst", 320).verbergEcht(14)
                .speler(v(0.5, 0, 0.5), 180)
                .npc("aang", Kind.AANG_GUH, v(-1.3, 0, -0.4), 180).npc("sokka", Kind.SOKKA_GUH, v(2.2, 0, 1.0), 160)
                .npc("katara", Kind.KATARA_GUH, v(-2.4, 0, 1.6), 200).npc("toph", Kind.TOPH_GUH, v(1.0, 0, 2.6), 180)
                .acteur("appa", E2AppaFeature.APPA_GUH, v(-5.0, 0, 4.5), 180).acteur("momo", E2AppaFeature.MOMO_GUH, v(-1.0, 0, -1.6), 180);
        b.zwart(0, 18)
                .camera(0, v(7, 9, 12), v(0.5, 1, -2)).camera(100, v(4, 4, 7), v(0.5, 1.2, 0))
                .zeg(22, "", "verteller", 60)
                .geluid(22, () -> SoundEvents.FIRECHARGE_USE, 0.4f, 0.6f).deeltjes(30, ParticleTypes.CAMPFIRE_COSY_SMOKE, r(a, "kraterpoort", 0, 14, 0), 20, 6)
                .cameraKnipVolgt(86, v(3.5, 1.6, -2.5), "sokka").zeg(88, "sokka", "plan", 60).animatie("sokka", 88, "plan")
                .cameraKnipVolgt(150, v(-4.5, 1.6, -0.5), "katara").zeg(152, "katara", "moe", 55).animatie("katara", 152, "praat")
                .cameraKnipVolgt(209, v(3.0, 1.4, 5.5), "toph").zeg(210, "toph", "schoenen", 50).animatie("toph", 210, "stamp")
                .cameraKnip(262, v(-1.0, 1.7, -4.0), v(-1.0, 1.0, 0.5)).zeg(264, "aang", "welkom", 50).animatie("aang", 264, "lach")
                .animatie("momo", 270, "spring").zwart(300, 320);
        return b.registreer();
    }

    /** The Vuurnatie school (anchor schoolbank_speler). 16 s. */
    private static Cutscene school() {
        String a = "schoolbank_speler";
        Cutscene.Builder b = vuur("school", 330).verbergEcht(15)
                .speler(v(0.5, 0, 0.5), 90)
                .npc("juf", Kind.VUURNATIE_JUF, r(a, "juf", 0, 0, 0), -90)
                .npc("leerling_1", Kind.VUURNATIE_LEERLING, r(a, "leerling_1", 0, 0, 0), 90)
                .npc("leerling_2", Kind.VUURNATIE_LEERLING, r(a, "leerling_2", 0, 0, 0), 90)
                .npc("leerling_3", Kind.VUURNATIE_LEERLING, r(a, "leerling_3", 0, 0, 0), 90)
                .npc("leerling_4", Kind.VUURNATIE_LEERLING, r(a, "leerling_4", 0, 0, 0), 90)
                .npc("leerling_5", Kind.VUURNATIE_LEERLING, r(a, "leerling_5", 0, 0, 0), 90);
        Vec3 juf = r(a, "juf", 0, 0, 0);
        b.zwart(0, 14)
                .camera(0, juf.add(1.5, 2.6, -3.5), v(-3, 1, 0)).camera(90, juf.add(2.0, 2.0, -2.0), v(-2, 1, 0))
                .zeg(16, "juf", "beste", 60).animatie("juf", 16, "tik")
                .zeg(80, "leerling_1", "mikai", 40).animatie("leerling_1", 80, "hand_op").animatie("leerling_3", 82, "hand_op")
                .cameraKnipVolgt(124, juf.add(3.0, 1.6, 1.5), "juf").zeg(126, "juf", "knabbels", 55)
                .zeg(185, "leerling_4", "opgegeten", 55).animatie("leerling_4", 185, "hand_op")
                .cameraKnip(244, v(-1.5, 1.4, 2.5), v(0.5, 1.2, 0.5)).zeg(246, "juf", "oren", 45).kijk("juf", 240, v(0.5, 1, 0.5))
                .animatie(Cutscene.SPELER, 250, "buk").animatie(Cutscene.SPELER, 290, "sta")
                .zeg(294, "leerling_5", "fluister", 30);
        return b.registreer();
    }

    /** The Dag van de Zwarte Zon: the invasion, the empty throne room, the bunker, the speech (anchor rustpunt_2). 75 s. */
    private static Cutscene zwarteZon() {
        String a = "rustpunt_2";
        Vec3 troon = r(a, "troon", 0, 0, 0), vader = r(a, "zuka_vader", 0, 0, 0), mikai = r(a, "mikai_bunker", 0, 0, 0);
        Cutscene.Builder b = vuur("zwarte_zon", 1500).verbergEcht(10)
                .speler(v(0.5, 0, 2.5), 180)
                .npc("aang", Kind.AANG_GUH, v(-1.0, 0, 5.0), 180).npc("sokka", Kind.SOKKA_GUH, v(1.5, 0, 5.5), 180)
                .npc("katara", Kind.KATARA_GUH, v(-2.0, 0, 7.0), 180).npc("toph", Kind.TOPH_GUH, v(2.5, 0, 7.5), 180)
                .npc("azula", Kind.AZULA_MIKA, troon.add(0, -40, 0), 0)
                .npc("zuka", Kind.ZUKA_PRINS, vader.add(0, 0, -1.5), 0)
                .npc("mikai", Kind.VUURHEER_MIKAI, mikai, 180);
        // 1. the sky goes dark over the city
        b.zwart(0, 20)
                .camera(0, v(-12, 26, 18), v(0, 8, -60)).camera(150, v(-6, 18, 10), v(0, 10, -50))
                .zeg(22, "", "zon", 70)
                .deeltjes(40, ParticleTypes.LARGE_SMOKE, v(0, 40, -50), 80, 25).deeltjes(90, ParticleTypes.LARGE_SMOKE, v(0, 42, -40), 80, 25)
                .zeg(96, "", "acht_minuten", 60).schud(96, 0.3f, 30);
        // 2. the team storms in through the crater gate
        b.cameraKnipVolgt(160, v(3.0, 2.0, -4.0), "sokka")
                .loop("aang", 160, 280, v(-1.0, 0, -6.0)).loop("sokka", 165, 285, v(1.5, 0, -5.5))
                .loop("katara", 170, 290, v(-2.0, 0, -4.0)).loop("toph", 175, 295, v(2.5, 0, -3.5)).loop(Cutscene.SPELER, 165, 285, v(0.5, 0, -4.5))
                .zeg(162, "sokka", "plan", 65).animatie("sokka", 162, "plan")
                .zeg(230, "aang", "nu", 50);
        // 3. the throne room: empty. Azula knew.
        b.cameraKnip(290, troon.add(0, 3.5, 15), troon.add(0, 1.5, 0))
                .loop("aang", 288, 289, troon.add(-1.2, -3, 13)).loop("sokka", 288, 289, troon.add(1.2, -3, 13))
                .loop("aang", 300, 360, troon.add(-1.0, -3, 9)).loop("sokka", 300, 360, troon.add(1.0, -3, 9))
                .zeg(292, "", "leeg", 60)
                .zeg(356, "aang", "waar", 45)
                .loop("azula", 405, 406, troon.add(0, 0, 0.5)).deeltjes(405, ParticleTypes.SOUL_FIRE_FLAME, troon.add(0, 1, 0.5), 40, 0.6)
                .geluid(405, () -> SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f)
                .cameraKnipVolgt(406, troon.add(2.5, 1.8, 4.0), "azula")
                .zeg(410, "azula", "wist", 70).animatie("azula", 410, "grijns")
                .cameraKnipVolgt(484, troon.add(-2.5, -1.4, 11.0), "sokka").zeg(486, "sokka", "val", 60).animatie("sokka", 486, "praat");
        // 4. the bunker: Zuka and his father
        b.cameraKnip(555, vader.add(-3.0, 1.8, -2.5), vader.add(0.0, 1.2, 2.5))
                .zeg(558, "zuka", "luister", 70).animatie("zuka", 558, "praat")
                .cameraKnipVolgt(632, vader.add(1.5, 1.6, 1.5), "mikai").zeg(634, "mikai", "durft", 60).animatie("mikai", 634, "praat")
                .cameraKnipVolgt(698, mikai.add(-1.5, 1.8, -2.5), "zuka").zeg(700, "zuka", "weggestuurd", 80)
                .zeg(782, "zuka", "bang", 80).animatie("zuka", 782, "mijn_eer")
                .cameraKnip(866, vader.add(-3.5, 1.5, 1.0), vader.add(0, 1.4, 2.0))
                .zeg(868, "mikai", "les", 55).animatie("mikai", 868, "vuurstoot")
                .deeltjes(925, ParticleTypes.ELECTRIC_SPARK, mikai.add(0, 1.6, -1.5), 60, 0.4)
                .deeltjes(928, ParticleTypes.ELECTRIC_SPARK, vader.add(0, 1.4, 0), 60, 0.4).deeltjes(935, ParticleTypes.ELECTRIC_SPARK, vader.add(0, 4, 0), 50, 0.6)
                .geluid(926, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.4f).schud(926, 1.2f, 20)
                .animatie("zuka", 928, "vuur")
                .zeg(934, "", "bliksem", 80)
                .cameraKnipVolgt(1016, vader.add(-1.5, 1.6, -2.5), "zuka").zeg(1018, "zuka", "dag", 70).animatie("zuka", 1018, "buig")
                .loop("zuka", 1090, 1160, vader.add(-3.0, 0, -5.0)).zeg(1092, "", "weg", 60);
        // 5. back at the gate: the team retreats
        b.cameraKnip(1160, v(2.5, 1.8, -7.0), v(0.5, 1.0, -3.0))
                .loop("aang", 1158, 1159, v(-1.0, 0, -4.0)).loop("sokka", 1158, 1159, v(1.5, 0, -4.0))
                .loop("katara", 1158, 1159, v(-2.0, 0, -3.0)).loop("toph", 1158, 1159, v(2.5, 0, -3.0)).loop(Cutscene.SPELER, 1158, 1159, v(0.5, 0, -2.5))
                .zeg(1162, "katara", "terug", 70).animatie("katara", 1162, "armen")
                .zeg(1236, "sokka", "plan_b", 60).animatie("sokka", 1236, "honger")
                .cameraKnip(1300, v(-6, 14, 6), vader.add(0, 10, 0))
                .zeg(1302, "", "oom", 80)
                .zwart(1460, 1500);
        return b.registreer();
    }

    /** The cell under the palace: Zuka visits his uncle; Miroh says nothing (anchor miroh_cel). 25 s. */
    private static Cutscene gevangenis() {
        Cutscene.Builder b = vuur("gevangenis", 500).verbergEcht(8)
                .npc("miroh", Kind.OOM_MIROH_GEVANGENE, v(0.5, 0, 1.2), 180)
                .npc("zuka", Kind.ZUKA_PRINS, v(0.5, 0, -3.6), 0);
        b.zwart(0, 14)
                .camera(0, v(2.8, 1.7, -5.5), v(0.5, 1.2, -1.0))
                .zeg(18, "zuka", "nee_gezegd", 70).animatie("zuka", 18, "praat")
                .kijk("miroh", 90, v(0.5, 1, 4.0)).zeg(94, "", "draait", 60)
                .cameraKnipVolgt(158, v(-1.5, 1.5, -5.0), "zuka").zeg(160, "zuka", "goedmaken", 80).animatie("zuka", 160, "zucht")
                .zeg(244, "", "stilte", 50).geluid(250, () -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 1.0f, 1.0f)
                .geluid(280, () -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 1.0f, 0.9f)
                .zeg(298, "zuka", "ga", 55)
                .loop("zuka", 355, 400, v(-2.5, 0, -8.0))
                .cameraKnip(362, v(-1.2, 1.4, 3.6), v(0.5, 0.6, 1.2))
                .zeg(366, "", "opdrukjes", 70)
                .animatie("miroh", 366, "buk").animatie("miroh", 384, "sta").animatie("miroh", 400, "buk").animatie("miroh", 416, "sta")
                .geluid(420, () -> SoundEvents.CHAIN_BREAK, 0.8f, 0.7f)
                .zeg(440, "miroh", "sterk", 55)
                .zwart(485, 500);
        return b.registreer();
    }

    /** Zuka at the camp: nobody trusts him (anchor appa_kamp). 29 s. */
    private static Cutscene kamp() {
        String a = "appa_kamp";
        Vec3 vuur = r(a, "team_zuka", -0.5, 0, -2.5);
        Cutscene.Builder b = vuur("kamp", 590).verbergEcht(16)
                .speler(r(a, "team_zuka", -2.0, 0, 1.0), 200)
                .npc("aang", Kind.AANG_GUH, r(a, "team_aang", 0, 0, 0), 0).npc("katara", Kind.KATARA_GUH, r(a, "team_katara", 0, 0, 0), 60)
                .npc("sokka", Kind.SOKKA_GUH, r(a, "team_sokka", 0, 0, 0), 90).npc("toph", Kind.TOPH_GUH, r(a, "team_toph", 0, 0, 0), -60)
                .npc("zuka", Kind.ZUKA_PRINS, r(a, "team_zuka", 12, 0, 0), 90)
                .acteur("appa", E2AppaFeature.APPA_GUH, v(0.5, 0, 0.5), 90).acteur("momo", E2AppaFeature.MOMO_GUH, r(a, "team_momo", 0, 0, 0), 0);
        b.zwart(0, 18)
                .camera(0, vuur.add(-6, 5, 7), vuur).camera(80, vuur.add(-4, 3, 5), vuur)
                .deeltjes(10, ParticleTypes.CAMPFIRE_COSY_SMOKE, vuur, 6, 0.3).deeltjes(10, ParticleTypes.FLAME, vuur, 12, 0.3)
                .zeg(20, "", "avond", 60)
                .loop("zuka", 70, 140, r(a, "team_zuka", 3, 0, 0))
                .cameraKnipVolgt(82, vuur.add(-1.5, 1.6, 3.5), "zuka").zeg(84, "zuka", "hoi", 70).animatie("zuka", 84, "praat")
                .cameraKnipVolgt(156, vuur.add(2.5, 1.6, -1.5), "katara").zeg(158, "katara", "eruit", 60).animatie("katara", 158, "waterzweep")
                .cameraKnipVolgt(220, vuur.add(-2.5, 1.6, -1.0), "sokka").zeg(222, "sokka", "eer", 70).animatie("sokka", 222, "praat")
                .cameraKnipVolgt(294, vuur.add(-1.0, 1.6, 3.0), "zuka").zeg(296, "zuka", "leraar", 65).animatie("zuka", 296, "vuur")
                .cameraKnipVolgt(362, vuur.add(1.0, 1.4, 3.0), "toph").zeg(364, "toph", "voeten", 85).animatie("toph", 364, "wijs")
                .zeg(450, "zuka", "sorry", 45).animatie("zuka", 450, "zucht")
                .cameraKnipVolgt(496, vuur.add(-2.0, 1.6, -2.0), "aang").zeg(498, "aang", "tweede", 55).animatie("aang", 498, "lach")
                .cameraKnip(555, vuur.add(-6, 5, 7), vuur).zeg(556, "", "eten", 30);
        return b.registreer();
    }

    /** The escape from Het Kokend Rotsblok in the gondola (anchor gondel_onder). 28 s. */
    private static Cutscene kokendRotsblok() {
        String a = "gondel_onder";
        Vec3 boven = r(a, "gondel_boven", 0, 0, 0);
        Cutscene.Builder b = vuur("kokend_rotsblok", 560).verbergEcht(10)
                .speler(v(0.5, 0, 0.5), 90)
                .npc("sokka", Kind.SOKKA_GUH, v(0.5, 0, 1.6), 90).npc("suki", Kind.SUKI_GUH, v(0.5, 0, -0.6), 90)
                .npc("zuka", Kind.ZUKA_PRINS, v(-0.6, 0, 0.6), 90)
                .npc("directeur", Kind.GEVANGENIS_DIRECTEUR_MIKA, v(6.5, 0, -2.0), 90).npc("ty_lee", Kind.TY_LEE_MIKA, r(a, "ty_lee", 0, 0, 0), 90)
                .npc("azula", Kind.AZULA_MIKA, r(a, "azula_gondel", 3.0, 0, 0), 90);
        b.zwart(0, 14)
                .camera(0, v(-4.5, 2.5, 4.0), v(1.0, 1.0, 0.5))
                .zeg(16, "sokka", "suki", 70).animatie("sokka", 16, "plan")
                .zeg(88, "suki", "rennen", 45).animatie("suki", 88, "waaier")
                .cameraKnipVolgt(134, v(3.5, 2.0, 2.5), "directeur").zeg(136, "directeur", "kabel", 55).animatie("directeur", 136, "tier")
                .cameraKnipVolgt(192, v(2.5, 1.8, 3.5), "ty_lee").zeg(194, "ty_lee", "mee", 55).animatie("ty_lee", 194, "radslag")
                .cameraKnipVolgt(250, v(-1.0, 3.5, 3.5), "azula").zeg(252, "azula", "broertje", 60).animatie("azula", 252, "grijns")
                .cameraKnipVolgt(314, v(-2.0, 1.6, -2.5), "zuka").zeg(316, "zuka", "eenzaam", 50)
                .zwart(366, 392)
                .loop(Cutscene.SPELER, 380, 381, boven.add(-2.0, 0, 0)).loop("sokka", 380, 381, boven.add(-2.0, 0, 1.2))
                .loop("suki", 380, 381, boven.add(-2.0, 0, -1.2)).loop("zuka", 380, 381, boven.add(-3.2, 0, 0.2))
                .loop("azula", 380, 381, boven.add(14, 30, 0)).loop("ty_lee", 380, 381, boven.add(14, 30, 2)).loop("directeur", 380, 381, boven.add(14, 30, -2))
                .cameraKnip(392, boven.add(-6, 3, 4), boven.add(4, 0, 0))
                .geluid(400, () -> SoundEvents.CHAIN_BREAK, 1.0f, 0.5f).geluid(420, () -> SoundEvents.GENERIC_SPLASH, 1.2f, 0.6f)
                .deeltjes(420, ParticleTypes.CLOUD, boven.add(12, -8, 0), 40, 3)
                .zeg(396, "", "kabel_zakt", 90)
                .zeg(490, "suki", "dank", 55).kijk("suki", 488, boven.add(-3.2, 1, 0.2)).animatie("suki", 490, "groet");
        return b.registreer();
    }

    /** The dragons of the Zonnekrijgers judge the dancers (anchor trap_boven, the top of the long stair). 50 s. */
    private static Cutscene draken() {
        String a = "trap_boven";
        Vec3 rood = r(a, "draak_rood", 0, 0, 0), blauw = r(a, "draak_blauw", 0, 0, 0);
        // the middle of the terrace: the average of both dance routes; the last step of both routes is the same spot
        Vec3 midden = r(a, "drakendans", 0, 0, 0), laatste = midden;
        BlockPos ap = pos(S, a);
        java.util.List<BlockPos> stappen = new java.util.ArrayList<>();
        for (String route : new String[] {"drakendans_speler", "drakendans_zuka"}) {
            Plekken.Plek p = Plekken.plek(S, route);
            if (p != null) {
                stappen.addAll(p.punten());
            }
        }
        if (ap != null && !stappen.isEmpty()) {
            double x = 0, y = 0, z = 0;
            for (BlockPos q : stappen) {
                x += q.getX();
                y += q.getY();
                z += q.getZ();
            }
            int n = stappen.size();
            midden = new Vec3(x / n - ap.getX() + 0.5, y / n - ap.getY(), z / n - ap.getZ() + 0.5);
            BlockPos l = Plekken.plek(S, "drakendans_speler").punten().get(Plekken.plek(S, "drakendans_speler").punten().size() - 1);
            laatste = new Vec3(l.getX() - ap.getX() + 0.5, l.getY() - ap.getY(), l.getZ() - ap.getZ() + 0.5);
        }
        Cutscene.Builder b = vuur("draken", 1000).verbergEcht(26)
                .speler(laatste.add(-0.8, 0, 0), 0)
                .npc("zuka", Kind.ZUKA_PRINS, laatste.add(0.8, 0, 0), 0)
                .npc("zonnekrijger", Kind.ZONNEKRIJGER_GUH, midden.add(0, 0, 6), 180)
                .acteur("rood", E2CastSchurkFeature.DRAAKGUH, rood, -90, t -> t.putInt("Soort", 0))
                .acteur("blauw", E2CastSchurkFeature.DRAAKGUH, blauw, 90, t -> t.putInt("Soort", 1));
        b.zwart(0, 15)
                .camera(0, midden.add(0, 11, 12), midden.add(0, 1, 0)).camera(160, midden.add(0, 7, 9), midden)
                .zeg(18, "", "laatste_stap", 70)
                .zeg(92, "zonnekrijger", "oordeel", 70).animatie("zonnekrijger", 92, "zonnegroet")
                .geluid(168, () -> SoundEvents.ENDER_DRAGON_GROWL, 0.35f, 1.4f).schud(168, 0.6f, 30)
                .loop("rood", 170, 250, midden.add(-7, 6, -2)).loop("blauw", 175, 255, midden.add(7, 6, 2))
                .animatie("rood", 170, "vlieg_rond").animatie("blauw", 175, "vlieg_rond")
                .cameraKnipVolgt(172, laatste.add(2.5, 1.5, 3.5), "zuka").zeg(176, "zuka", "menu", 60).kijk("zuka", 176, midden.add(0, 6, 0))
                .cameraKnip(240, midden.add(0, 2.0, 11), midden.add(0, 6, 0))
                .zeg(242, "", "cirkel", 60)
                .loop("rood", 260, 330, midden.add(0, 7, -8)).loop("blauw", 262, 332, midden.add(0, 8, 8))
                .loop("rood", 330, 400, midden.add(8, 6, 0)).loop("blauw", 332, 402, midden.add(-8, 7, 0))
                .loop("rood", 400, 470, midden.add(0, 6, 8)).loop("blauw", 402, 472, midden.add(0, 7, -8))
                .animatie("rood", 310, "vuur").animatie("blauw", 316, "vuur")
                .deeltjes(312, ParticleTypes.FLAME, laatste.add(0, 1.5, 0), 120, 1.6).deeltjes(318, ParticleTypes.SOUL_FIRE_FLAME, laatste.add(0, 1.5, 0), 120, 1.6)
                .deeltjes(330, ParticleTypes.FIREWORK, laatste.add(0, 2.5, 0), 80, 1.8).deeltjes(345, ParticleTypes.HAPPY_VILLAGER, laatste.add(0, 2, 0), 40, 1.6)
                .deeltjes(360, ParticleTypes.END_ROD, laatste.add(0, 2, 0), 60, 1.8).deeltjes(375, ParticleTypes.CHERRY_LEAVES, laatste.add(0, 3, 0), 60, 2.0)
                .geluid(312, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.6f).geluid(318, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.9f)
                .cameraKnip(380, laatste.add(0, 1.6, 4.5), laatste.add(0, 1.4, 0))
                .zeg(382, "", "regenboog", 90)
                .zeg(476, "zuka", "leven", 80).animatie("zuka", 476, "vuur")
                .zeg(560, "", "jij", 80)
                .loop("rood", 645, 760, rood).loop("blauw", 645, 760, blauw)
                .cameraKnipVolgt(646, laatste.add(-2.5, 1.6, 4.0), "zonnekrijger").loop("zonnekrijger", 640, 690, laatste.add(0, 0, 3.0))
                .zeg(650, "zonnekrijger", "gezien", 80)
                .cameraKnipVolgt(734, laatste.add(3.0, 1.6, 2.5), "zuka").kijk("zuka", 734, laatste.add(-0.8, 1, 0))
                .zeg(736, "zuka", "dank", 80)
                .cameraKnip(820, midden.add(0, 12, 14), midden.add(0, 0, 0)).zeg(822, "", "vuur_van_jou", 70)
                .zwart(965, 1000);
        return b.registreer();
    }

    /** The Sintelstrand theatre: "De jongen in de kaasbol" (anchor publiek_speler). 50 s. */
    private static Cutscene toneel() {
        String a = "publiek_speler";
        Vec3 t1 = r(a, "acteur_1", 0, 0, 0), t5 = r(a, "acteur_5", 0, 0, 0);
        Vec3 toneel = t1.lerp(t5, 0.5);
        Cutscene.Builder b = vuur("toneel", 1000).verbergEcht(18)
                .speler(v(0.5, 0, 0.5), 0)
                .npc("aang", Kind.AANG_GUH, v(-0.5, 0, 0.5), 0).npc("katara", Kind.KATARA_GUH, v(-1.5, 0, 0.5), 0)
                .npc("sokka", Kind.SOKKA_GUH, v(1.5, 0, 0.5), 0).npc("toph", Kind.TOPH_GUH, v(2.5, 0, 0.5), 0)
                .npc("zuka", Kind.ZUKA_TEAM, v(3.5, 0, 0.5), 0)
                .npc("acteur_1", Kind.TONEELSPELER_GUH, t1, 180).npc("acteur_2", Kind.TONEELSPELER_GUH_KATARA, r(a, "acteur_2", 0, 0, 0), 180)
                .npc("acteur_3", Kind.TONEELSPELER_GUH_SOKKA, r(a, "acteur_3", 0, 0, 0), 180)
                .npc("acteur_4", Kind.TONEELSPELER_GUH_TOPH, r(a, "acteur_4", 0, 0, 0), 180).npc("acteur_5", Kind.TONEELSPELER_GUH_ZUKA, t5, 180);
        Vec3 publiek = v(1.5, 1.2, 0.5);
        b.zwart(0, 15)
                .geluid(4, E2Audio.geluid("stinger.toneel_fanfare"), 1.0f, 1.0f)
                .camera(0, v(1.5, 3.2, -4.5), toneel.add(0, 1, 0)).camera(95, v(1.5, 2.6, -2.0), toneel.add(0, 1, 0))
                .zeg(18, "", "vanavond", 75)
                .cameraKnipVolgt(98, t1.add(2.0, 1.5, -3.0), "acteur_1").zeg(100, "acteur_1", "guhvatar", 70).animatie("acteur_1", 100, "drama")
                .cameraKnip(172, toneel.add(0, 1.6, -3.0), publiek).zeg(174, "aang", "lach", 55).animatie("aang", 174, "praat")
                .cameraKnipVolgt(232, r(a, "acteur_2", 1.5, 1.5, -3.0), "acteur_2").zeg(234, "acteur_2", "hoop", 65).animatie("acteur_2", 234, "drama")
                .cameraKnip(302, toneel.add(-1, 1.6, -3.0), v(-1.5, 1.2, 0.5)).zeg(304, "katara", "soms", 55).animatie("katara", 304, "armen")
                .cameraKnipVolgt(362, r(a, "acteur_3", 1.5, 1.5, -3.0), "acteur_3").zeg(364, "acteur_3", "honger", 60).animatie("acteur_3", 364, "drama")
                .cameraKnip(427, toneel.add(1, 1.6, -3.0), v(1.5, 1.2, 0.5)).zeg(429, "sokka", "goed", 45).animatie("sokka", 429, "honger")
                .cameraKnipVolgt(477, r(a, "acteur_4", 1.5, 2.0, -3.5), "acteur_4").zeg(479, "acteur_4", "reus", 60).animatie("acteur_4", 479, "drama")
                .cameraKnip(542, toneel.add(2, 1.6, -3.0), v(2.5, 1.2, 0.5)).zeg(544, "toph", "tien", 60).animatie("toph", 544, "lach")
                .cameraKnipVolgt(607, t5.add(-1.5, 1.5, -3.0), "acteur_5").zeg(609, "acteur_5", "eer", 60).animatie("acteur_5", 609, "drama")
                .cameraKnip(672, toneel.add(3, 1.6, -3.0), v(3.5, 1.2, 0.5)).zeg(674, "zuka", "kant", 70).animatie("zuka", 674, "zucht")
                .kijk("aang", 745, v(3.5, 1, 0.5)).kijk("katara", 745, v(3.5, 1, 0.5)).kijk("sokka", 745, v(3.5, 1, 0.5)).kijk("toph", 745, v(3.5, 1, 0.5))
                .zeg(748, "", "iedereen_kijkt", 40)
                .zeg(792, "zuka", "vaak", 45).animatie("zuka", 792, "mijn_eer")
                .cameraKnip(840, v(1.5, 4.5, -6.0), toneel.add(0, 1, 0))
                .animatie("acteur_1", 842, "buig").animatie("acteur_2", 844, "buig").animatie("acteur_3", 846, "buig").animatie("acteur_4", 848, "buig")
                .animatie("acteur_5", 850, "buig").geluid(842, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.8f, 1.0f)
                .zeg(842, "", "doek", 60)
                .cameraKnip(905, v(0.5, 1.6, 4.0), v(0.5, 1.2, 0.5)).zeg(907, "aang", "samen", 65).animatie("aang", 907, "lach")
                .kijk("aang", 905, v(0.5, 1, 0.5))
                .zwart(975, 1000);
        return b.registreer();
    }

    /** The tent of the White Lotus: Zuka and Oom Miroh (anchor verzoening_zuka). 55 s. */
    private static Cutscene tent() {
        String a = "verzoening_zuka";
        Vec3 miroh = r(a, "miroh_tent", 0, 0, 0);
        Vec3 buiten = v(-7.5, 0, 0.5);
        Cutscene.Builder b = vuur("tent", 1100).verbergEcht(20)
                .speler(buiten.add(-1.5, 0, 2.0), -90)
                .npc("zuka", Kind.ZUKA_TEAM, buiten, -90)
                .npc("miroh", Kind.OOM_MIROH_LOTUS, miroh, 90)
                .npc("bumi", Kind.BUMI_GUH, r(a, "bumi", 0, 0, 0), 0).npc("pakguh", Kind.PAKGUH, r(a, "pakguh_lotus", 0, 0, 0), 180)
                .npc("piandao", Kind.PIANDAO_GUH, r(a, "piandao", 0, 0, 0), 90);
        b.zwart(0, 20)
                .camera(0, buiten.add(-7, 6, 8), v(0.5, 2, 0.5)).camera(95, buiten.add(-4, 3, 5), v(0.5, 1.5, 0.5))
                .deeltjes(10, ParticleTypes.CAMPFIRE_COSY_SMOKE, r(a, "rustpunt_10", 0, 0, 0), 8, 0.6)
                .zeg(22, "", "kamp", 75)
                .cameraKnipVolgt(98, buiten.add(-2.0, 1.6, 2.5), "zuka").zeg(100, "zuka", "nooit", 65).animatie("zuka", 100, "zucht")
                .loop("zuka", 170, 235, v(0.5, 0, 0.5)).cameraKnip(171, v(2.5, 2.0, 3.0), v(0.5, 1.0, 0.5))
                .animatie("zuka", 236, "buig")
                .cameraKnipVolgt(238, miroh.add(-0.5, 1.4, 2.5), "zuka").zeg(240, "zuka", "spijt", 100)
                .loop("miroh", 345, 395, miroh.add(-2.2, 0, 0)).zeg(346, "", "opstaan", 50)
                .cameraKnip(398, v(1.5, 1.6, 2.8), v(1.2, 1.2, 0.5))
                .zeg(400, "miroh", "nooit_boos", 110).animatie("miroh", 400, "praat")
                .animatie("zuka", 470, "sta")
                .cameraKnipVolgt(513, v(-1.5, 1.6, 2.0), "zuka").zeg(515, "zuka", "haten", 60)
                .cameraKnipVolgt(578, v(2.5, 1.6, 2.0), "miroh").zeg(580, "miroh", "neefje", 100).animatie("miroh", 580, "wijsheid")
                .zeg(684, "miroh", "thee", 60).animatie("miroh", 684, "thee")
                .cameraKnip(748, v(0.5, 4.0, 6.0), v(1.0, 1.0, 0.5)).zeg(750, "", "thee_drinken", 70)
                .loop(Cutscene.SPELER, 760, 820, v(0.5, 0, 2.5))
                .cameraKnipVolgt(828, v(-1.5, 1.6, 4.5), "miroh").kijk("miroh", 826, v(0.5, 1, 2.5)).zeg(830, "miroh", "lot", 90)
                .zeg(924, "miroh", "kopje", 85).animatie("miroh", 924, "lach")
                .zwart(1060, 1100);
        return b.registreer();
    }

    // =====================================================================================================================
    // the finale
    // =====================================================================================================================

    /** The Top der Voorgangers in the Geestenwereld (anchor TOP_ANKER). 37 s. */
    private static Cutscene top() {
        String g = Geestenwereld.NAAM, a = TOP_ANKER;
        Vec3 roku = r(g, a, "roku", 0, 0, 0), kyoshi = r(g, a, "kyoshi", 0, 0, 0);
        Vec3 midden = roku.lerp(kyoshi, 0.5);
        Vec3 terug = midden.subtract(v(0.5, 0, 0.5)).normalize();
        if (!Double.isFinite(terug.x) || terug.lengthSqr() < 1e-6) {
            terug = v(0, 0, -1);
        }
        Vec3 achter = midden.add(terug.scale(4));
        Cutscene.Builder b = finale("top", 740).verbergEcht(14)
                .speler(v(0.5, 0, 0.5), Cutscene.yawVan(midden.subtract(v(0.5, 0, 0.5))))
                .npc("roku", Kind.ROKU_GUH, roku, Cutscene.yawVan(v(0.5, 0, 0.5).subtract(roku)))
                .npc("kyoshi", Kind.KYOSHI_GUH, kyoshi, Cutscene.yawVan(v(0.5, 0, 0.5).subtract(kyoshi)))
                .npc("kuruk", Kind.KURUK_GUH, achter.add(-3.5, 1.0, 0), Cutscene.yawVan(terug.scale(-1)))
                .npc("yangchen", Kind.YANGCHEN_GUH, achter.add(0, 1.5, 0), Cutscene.yawVan(terug.scale(-1)))
                .npc("szeto", Kind.SZETO_GUH, achter.add(3.5, 1.0, 0), Cutscene.yawVan(terug.scale(-1)));
        b.zwart(0, 15)
                .camera(0, v(0.5, 6, 0.5).subtract(terug.scale(8)), midden.add(0, 2, 0)).camera(90, v(0.5, 3, 0.5).subtract(terug.scale(4)), midden.add(0, 1.5, 0))
                .deeltjes(10, ParticleTypes.END_ROD, midden.add(0, 2, 0), 40, 3)
                .zeg(18, "", "top", 70)
                .cameraKnipVolgt(92, midden.add(-1.5, 1.6, 0).subtract(terug.scale(-3)), "roku").zeg(94, "roku", "komeet", 80).animatie("roku", 94, "raad")
                .zeg(178, "roku", "twijfel", 100)
                .cameraKnipVolgt(282, midden.add(1.5, 1.6, 0).subtract(terug.scale(-3)), "kyoshi").zeg(284, "kyoshi", "hard", 90)
                .animatie("kyoshi", 284, "waaier")
                .cameraKnipVolgt(378, achter.add(0, 2.0, 0).subtract(terug.scale(-4)), "yangchen").zeg(380, "yangchen", "geluk", 90)
                .cameraKnipVolgt(474, achter.add(-3.5, 2.0, 0).subtract(terug.scale(-4)), "kuruk").zeg(476, "kuruk", "vissen", 70)
                .cameraKnipVolgt(548, achter.add(3.5, 2.0, 0).subtract(terug.scale(-4)), "szeto").zeg(550, "szeto", "papier", 60)
                .cameraKnip(614, v(0.5, 2.0, 0.5).subtract(terug.scale(3)), midden.add(0, 1.5, 0)).zeg(616, "roku", "alleen", 90)
                .deeltjes(700, ParticleTypes.END_ROD, v(0.5, 1, 0.5), 60, 1.2)
                .zwart(705, 740);
        return b.registreer();
    }

    /** The night on the back of the Leeuwschildpad-guh: energy bending (anchor leeuwschildpad_rug). 52 s. */
    private static Cutscene leeuwschildpad() {
        String a = "leeuwschildpad_rug";
        Vec3 kop = r(a, "leeuwschildpad_kop", 0, 0, 0), gezicht = r(a, "leeuwschildpad", 0, 0, 0);
        Cutscene.Builder b = finale("leeuwschildpad", 1040)
                .speler(v(0.5, 0, 0.5), Cutscene.yawVan(gezicht.subtract(v(0.5, 0, 0.5))))
                .acteur("schildpad", E2CastSchurkFeature.LEEUWSCHILDPAD_GUH, kop, 0, t -> t.putInt("Soort", 1));
        Vec3 jij = v(0.5, 0, 0.5);
        Vec3 naarKop = gezicht.subtract(jij).normalize();
        b.zwart(0, 40)
                .camera(0, jij.add(1.5, 1.2, -1.5), jij.add(0, 0.6, 0)).camera(110, jij.add(2.0, 1.8, -2.0), jij.add(0, 1.0, 0))
                .animatie(Cutscene.SPELER, 0, "buk").schud(40, 0.25f, 80)
                .zeg(44, "", "wakker", 70)
                .animatie(Cutscene.SPELER, 116, "sta")
                .cameraKnip(120, jij.add(-6, 10, -6), gezicht.add(0, 2, 0)).camera(220, jij.add(-12, 22, -10), gezicht.add(0, 2, 0))
                .zeg(122, "", "schildpad", 70)
                .animatie("schildpad", 150, "ontwaak").geluid(150, () -> SoundEvents.ELDER_GUARDIAN_AMBIENT, 0.6f, 0.5f)
                .cameraKnip(198, jij.add(naarKop.scale(4)).add(0, 2.0, 0), gezicht.add(0, 3, 0))
                .zeg(200, "schildpad", "bang", 80).animatie("schildpad", 200, "praat")
                .zeg(284, "schildpad", "hart", 80)
                .cameraKnip(366, jij.add(naarKop.scale(-2)).add(1.5, 1.8, 0), jij.add(naarKop.scale(6)).add(0, 2, 0))
                .zeg(368, "schildpad", "vroeger", 110)
                .zeg(482, "schildpad", "honger", 110)
                .animatie("schildpad", 596, "zegen")
                .cameraKnipVolgt(596, jij.add(2.0, 1.6, 2.0), Cutscene.SPELER)
                .deeltjes(600, ParticleTypes.END_ROD, jij.add(0, 1.6, 0), 40, 0.4).deeltjes(630, ParticleTypes.END_ROD, jij.add(0, 1.0, 0), 50, 0.6)
                .deeltjes(660, ParticleTypes.GLOW, jij.add(0, 1.2, 0), 50, 0.8).geluid(602, () -> SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f)
                .zeg(604, "", "licht", 80)
                .cameraKnip(688, jij.add(naarKop.scale(3)).add(-1.5, 2.2, 0), gezicht.add(0, 3, 0)).zeg(690, "schildpad", "eten", 70)
                .cameraKnip(764, jij.add(-14, 18, -14), gezicht).camera(990, jij.add(-20, 28, -18), gezicht)
                .zeg(766, "", "zakt", 70).schud(840, 0.4f, 120).geluid(840, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 0.4f)
                .zwart(990, 1040);
        return b.registreer();
    }

    /** The Agni Kai on the palace square: Zuka and Azula (anchor agni_kai_midden). 55 s. */
    private static Cutscene agniKai() {
        String a = Plekken.plek(S, "agni_kai_midden") != null ? "agni_kai_midden" : "kroning_zuka";
        double dy = "kroning_zuka".equals(a) ? -2 : 0, dz = "kroning_zuka".equals(a) ? 9 : 0;
        Vec3 m = v(0.5, dy, 0.5 + dz);
        Cutscene.Builder b = finale("agni_kai", 1100).verbergEcht(20)
                .speler(m.add(0, 0, 9), 180)
                .acteur("appa", E2AppaFeature.APPA_GUH, m.add(-4, 0, 11), 180)
                .npc("zuka", Kind.ZUKA_TEAM, m.add(-5, 0, 0), -90).npc("azula", Kind.AZULA_MIKA, m.add(5, 0, 0), 90)
                .npc("katara", Kind.KATARA_GUH, m.add(-8, 0, 4), -120);
        b.zwart(0, 15)
                .camera(0, m.add(0, 14, 14), m.add(0, 1, 0)).camera(90, m.add(0, 8, 11), m.add(0, 1, 0))
                .deeltjes(20, ParticleTypes.FLAME, m.add(0, 30, -20), 80, 12)
                .zeg(18, "", "komeet", 70)
                .cameraKnipVolgt(94, m.add(2, 1.6, 3), "azula").zeg(96, "azula", "agni_kai", 75).animatie("azula", 96, "lok")
                .cameraKnipVolgt(174, m.add(-2, 1.6, 3), "zuka").zeg(176, "zuka", "niet_in_orde", 80).animatie("zuka", 176, "praat")
                .cameraKnipVolgt(258, m.add(2.5, 1.6, -2.5), "azula").zeg(260, "azula", "perfect", 55).animatie("azula", 260, "grijns")
                .cameraKnip(318, m.add(0, 3, 7), m.add(0, 1.2, 0))
                .deeltjes(320, ParticleTypes.SOUL_FIRE_FLAME, m.add(2, 1.4, 0), 80, 1.2).deeltjes(330, ParticleTypes.FLAME, m.add(-2, 1.4, 0), 80, 1.2)
                .deeltjes(350, ParticleTypes.SOUL_FIRE_FLAME, m.add(0, 1.4, 0), 100, 1.6).deeltjes(360, ParticleTypes.FLAME, m.add(0, 1.4, 0), 100, 1.6)
                .geluid(320, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f).geluid(352, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.6f)
                .animatie("azula", 320, "bliksem").animatie("zuka", 330, "vuur")
                .zeg(322, "", "vlammen", 70)
                .cameraKnipVolgt(394, m.add(1.5, 1.6, 4), "azula").kijk("azula", 394, m.add(-8, 1, 4)).zeg(396, "azula", "waterguh", 60)
                .animatie("azula", 456, "bliksem")
                .loop("zuka", 458, 474, m.add(-7, 0, 2.5)).zeg(458, "zuka", "katara", 25)
                .deeltjes(470, ParticleTypes.ELECTRIC_SPARK, m.add(-3, 1.4, 1.5), 80, 0.8).deeltjes(476, ParticleTypes.ELECTRIC_SPARK, m.add(-7, 1.2, 2.5), 60, 0.4)
                .geluid(472, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f).schud(472, 1.0f, 18)
                .cameraKnip(486, m.add(-9, 2, 0), m.add(-7, 0.6, 2.5))
                .animatie("zuka", 486, "buk")
                .zeg(490, "", "zuka_valt", 90)
                .loop("katara", 586, 610, m.add(-8, 0, 3)).zeg(588, "katara", "hou_vol", 60).animatie("katara", 588, "armen")
                .cameraKnip(652, m.add(4, 4, 8), m.add(2, 0.6, 0))
                .loop("azula", 652, 700, m.add(2, 0, -1)).loop("katara", 660, 700, m.add(-1, 0, 1))
                .deeltjes(700, ParticleTypes.SPLASH, m.add(1, 0.5, 0), 160, 2.0).deeltjes(708, ParticleTypes.SNOWFLAKE, m.add(1.5, 1.0, -0.5), 120, 1.5)
                .geluid(700, () -> SoundEvents.GENERIC_SPLASH, 1.0f, 0.8f).geluid(712, () -> SoundEvents.GLASS_PLACE, 1.0f, 0.6f)
                .animatie("katara", 700, "waterzweep")
                .zeg(654, "", "ketting", 100)
                .cameraKnipVolgt(758, m.add(4, 1.4, 1.5), "azula").zeg(760, "azula", "nee", 60).animatie("azula", 760, "buk")
                .zeg(822, "", "ijs", 80)
                .loop("katara", 905, 960, m.add(-6.5, 0, 2.5)).cameraKnip(905, m.add(-9, 1.6, 4.5), m.add(-7, 0.6, 2.5))
                .zeg(910, "katara", "dank", 70).animatie("katara", 960, "genees")
                .deeltjes(962, ParticleTypes.SPLASH, m.add(-7, 1, 2.5), 40, 0.4).deeltjes(970, ParticleTypes.GLOW, m.add(-7, 1, 2.5), 30, 0.5)
                .animatie("zuka", 980, "sta")
                .zeg(984, "zuka", "ga", 80)
                .zwart(1070, 1100);
        return b.registreer();
    }

    /** The turning point of the duel: the rock touches the scar, the Guhvatar state (anchor duel_speler). 32 s. */
    private static Cutscene omslag() {
        String a = "duel_speler";
        Vec3 mikai = r(a, "duel_mikai", 0, 0, 0);
        Vec3 jij = v(0.5, 0, 0.5);
        Vec3 naarMikai = mikai.subtract(jij).multiply(1, 0, 1).normalize();
        Vec3 achter = jij.subtract(naarMikai.scale(3));
        Vec3 opzij = v(-naarMikai.z, 0, naarMikai.x);
        float kijk = Cutscene.yawVan(naarMikai);
        Cutscene.Builder b = finale("omslag", 640)
                .speler(jij, kijk)
                .npc("mikai", Kind.VUURHEER_MIKAI_KOMEET, mikai, Cutscene.yawVan(naarMikai.scale(-1)))
                .npc("aang", Kind.AANG_GUH, achter.add(opzij.scale(1.2)).add(0, -12, 0), kijk)
                .npc("roku", Kind.ROKU_GUH, achter.add(opzij.scale(-1.2)).add(0, -12, 0), kijk)
                .npc("kyoshi", Kind.KYOSHI_GUH, achter.subtract(naarMikai.scale(1.5)).add(opzij.scale(3)).add(0, -12, 0), kijk)
                .npc("kuruk", Kind.KURUK_GUH, achter.subtract(naarMikai.scale(1.5)).add(opzij.scale(-3)).add(0, -12, 0), kijk)
                .npc("yangchen", Kind.YANGCHEN_GUH, achter.subtract(naarMikai.scale(3)).add(opzij.scale(1.5)).add(0, -12, 0), kijk)
                .npc("szeto", Kind.SZETO_GUH, achter.subtract(naarMikai.scale(3)).add(opzij.scale(-1.5)).add(0, -12, 0), kijk);
        b.camera(0, jij.add(naarMikai.scale(2.5)).add(opzij.scale(1.5)).add(0, 1.6, 0), jij.add(0, 1.0, 0))
                .animatie("mikai", 0, "vuurstoot").deeltjes(4, ParticleTypes.FLAME, jij.add(naarMikai.scale(2)).add(0, 1.2, 0), 80, 0.8)
                .animatie(Cutscene.SPELER, 8, "buk").schud(8, 1.0f, 16).geluid(8, () -> SoundEvents.STONE_BREAK, 1.0f, 0.6f)
                .zeg(14, "", "litteken", 90)
                .zeg(108, "", "open", 50)
                .deeltjes(160, ParticleTypes.END_ROD, jij.add(0, 1, 0), 120, 1.5).geluid(160, () -> SoundEvents.BEACON_ACTIVATE, 1.0f, 0.8f)
                .geluid(166, E2Audio.geluid("moment.duel_koor"), 0.6f, 1.0f)
                .animatie(Cutscene.SPELER, 160, "sta").schud(160, 0.6f, 40)
                .loop("aang", 165, 200, achter.add(opzij.scale(1.2)).add(0, 0.4, 0)).loop("roku", 170, 205, achter.add(opzij.scale(-1.2)).add(0, 0.4, 0))
                .loop("kyoshi", 175, 210, achter.subtract(naarMikai.scale(1.5)).add(opzij.scale(3)).add(0, 0.8, 0))
                .loop("kuruk", 180, 215, achter.subtract(naarMikai.scale(1.5)).add(opzij.scale(-3)).add(0, 0.8, 0))
                .loop("yangchen", 185, 220, achter.subtract(naarMikai.scale(3)).add(opzij.scale(1.5)).add(0, 1.4, 0))
                .loop("szeto", 190, 225, achter.subtract(naarMikai.scale(3)).add(opzij.scale(-1.5)).add(0, 1.4, 0))
                .cameraKnip(168, jij.add(naarMikai.scale(5)).add(0, 2.5, 0), achter.add(0, 1.5, 0))
                .zeg(230, "roku", "bij_je", 50)
                .zeg(282, "kyoshi", "alle", 45)
                .zeg(329, "aang", "ga", 55).animatie("aang", 329, "lach")
                .cameraKnipVolgt(386, jij.add(naarMikai.scale(1.8)).add(0, 1.7, 0), Cutscene.SPELER)
                .deeltjes(390, ParticleTypes.CLOUD, jij.add(0, 1, 0), 40, 1.4).deeltjes(400, ParticleTypes.SPLASH, jij.add(0, 1, 0), 60, 1.4)
                .deeltjes(410, ParticleTypes.FLAME, jij.add(0, 1, 0), 60, 1.4).deeltjes(420, ParticleTypes.END_ROD, jij.add(0, 1.6, 0), 40, 1.0)
                .zeg(388, "", "ogen", 90)
                .cameraKnipVolgt(480, mikai.subtract(naarMikai.scale(3)).add(0, 2.2, 0), "mikai").zeg(482, "mikai", "wat_ben_jij", 60)
                .animatie("mikai", 482, "brul")
                .cameraKnip(545, jij.subtract(naarMikai.scale(5)).add(0, 3, 0), mikai.add(0, 2, 0)).zeg(547, "", "laatste_ronde", 60)
                .zwart(615, 640);
        return b.registreer();
    }

    /** You take Mikai's endless appetite away: a round, content Mika (anchor duel_speler). 34 s. */
    private static Cutscene trekWeg() {
        String a = "duel_speler";
        Vec3 jij = v(0.5, 0, 0.5);
        Vec3 mikai = r(a, "duel_mikai", 0, 0, 0);
        Vec3 naarMikai = mikai.subtract(jij).multiply(1, 0, 1).normalize();
        Vec3 ligt = jij.add(naarMikai.scale(1.8));
        Cutscene.Builder b = finale("trek_weg", 680)
                .speler(jij, Cutscene.yawVan(naarMikai))
                .npc("mikai", Kind.VUURHEER_MIKAI_KOMEET, ligt, Cutscene.yawVan(naarMikai.scale(-1)))
                .npc("mikai_vadsig", Kind.VUURHEER_MIKAI_VADSIG, ligt.add(0, -20, 0), Cutscene.yawVan(naarMikai.scale(-1)));
        Vec3 zij = v(-naarMikai.z, 0, naarMikai.x);
        b.zwart(0, 15)
                .camera(0, jij.add(zij.scale(3.5)).add(0, 2.2, 0), ligt.add(0, 0.8, 0))
                .animatie("mikai", 0, "buk")
                .zeg(18, "", "ademt", 80)
                .cameraKnipVolgt(102, ligt.add(zij.scale(-2)).add(0, 1.2, 0), "mikai").zeg(104, "mikai", "maak_af", 45)
                .loop(Cutscene.SPELER, 150, 170, ligt.subtract(naarMikai.scale(0.9)))
                .cameraKnip(152, ligt.add(zij.scale(2.5)).add(0, 1.6, 0), ligt.add(0, 1.0, 0)).zeg(154, "", "poten", 80)
                .animatie(Cutscene.SPELER, 160, "buk")
                .deeltjes(236, ParticleTypes.END_ROD, ligt.add(0, 1.2, 0), 80, 0.8).deeltjes(250, ParticleTypes.SQUID_INK, ligt.add(0, 1.0, 0), 40, 0.6)
                .deeltjes(280, ParticleTypes.END_ROD, ligt.add(0, 1.2, 0), 120, 1.2).geluid(236, () -> SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.8f)
                .schud(250, 0.8f, 60)
                .zeg(238, "", "licht", 90)
                .zwart(330, 352)
                .loop("mikai", 340, 341, ligt.add(0, -20, 0)).loop("mikai_vadsig", 340, 341, ligt)
                .animatie(Cutscene.SPELER, 342, "sta")
                .cameraKnipVolgt(352, ligt.add(zij.scale(2)).add(0, 1.3, 0), "mikai_vadsig")
                .zeg(356, "mikai_vadsig", "vol", 60).animatie("mikai_vadsig", 356, "smul")
                .zeg(420, "", "tevreden", 100)
                .zeg(524, "mikai_vadsig", "dutje", 60).animatie("mikai_vadsig", 560, "boer")
                .cameraKnip(590, jij.add(zij.scale(6)).add(0, 4, 0), ligt).zeg(592, "", "snurkt", 60)
                .zwart(655, 680);
        return b.registreer();
    }

    /** The coronation of Zuka on the palace forecourt (anchor kroning_zuka). 65 s. */
    private static Cutscene kroning() {
        Vec3 zuka = v(0.5, 0, 0.5);
        Vec3 plein = v(0.5, -2, 5.5);
        Cutscene.Builder b = finale("kroning", 1300).verbergEcht(26)
                .speler(plein, 180)
                .npc("zuka", Kind.ZUKA_VUURHEER, zuka, 0).npc("miroh", Kind.OOM_MIROH_LOTUS, zuka.add(-2.0, 0, 0.3), 30)
                .npc("aang", Kind.AANG_GUH, plein.add(-1.4, 0, -0.2), 180).npc("katara", Kind.KATARA_GUH, plein.add(-2.8, 0, 0.3), 180)
                .npc("sokka", Kind.SOKKA_GUH, plein.add(1.4, 0, -0.2), 180).npc("toph", Kind.TOPH_GUH, plein.add(2.8, 0, 0.3), 180)
                .npc("suki", Kind.SUKI_GUH, plein.add(4.2, 0, 0.6), 180)
                .npc("burger_1", Kind.VUURNATIE_BURGER_MIKA, plein.add(-6, 0, 4), 180).npc("burger_2", Kind.VUURNATIE_BURGER_MIKA, plein.add(-3, 0, 6), 180)
                .npc("burger_3", Kind.GEVANGENE_GUH, plein.add(0, 0, 7), 180).npc("burger_4", Kind.ZONNEKRIJGER_GUH, plein.add(3, 0, 6), 180)
                .npc("burger_5", Kind.VUURNATIE_LEERLING, plein.add(6, 0, 4), 180).npc("burger_6", Kind.BA_SING_GUH_BURGER, plein.add(-5, 0, 8), 180)
                .npc("burger_7", Kind.WATERSTAM_GUH, plein.add(5, 0, 8), 180)
                .acteur("appa", E2AppaFeature.APPA_GUH, plein.add(-10, 0, 3), 150).acteur("momo", E2AppaFeature.MOMO_GUH, plein.add(-1.0, 0, 0.8), 180);
        b.zwart(0, 20)
                .camera(0, plein.add(0, 14, 20), zuka.add(0, 1, 0)).camera(105, plein.add(0, 6, 12), zuka.add(0, 1.5, 0))
                .deeltjes(30, ParticleTypes.FIREWORK, zuka.add(0, 12, -6), 60, 6).geluid(30, () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.0f)
                .zeg(24, "", "ochtend", 80)
                .cameraKnip(108, zuka.add(-2.5, 1.8, 3.5), zuka.add(-0.8, 1.6, 0)).zeg(110, "", "kroon", 70)
                .animatie("miroh", 112, "wijsheid").deeltjes(150, ParticleTypes.WAX_ON, zuka.add(0, 2.2, 0), 30, 0.4)
                .geluid(150, () -> SoundEvents.PLAYER_LEVELUP, 0.8f, 1.0f)
                .cameraKnipVolgt(182, zuka.add(1.5, 1.4, 4.0), "zuka").zeg(184, "zuka", "voorbij", 90).animatie("zuka", 184, "praat")
                .zeg(278, "zuka", "proberen", 90)
                .zeg(372, "zuka", "oom", 60).animatie("miroh", 380, "lach")
                .cameraKnip(436, plein.add(0, 4, 10), plein.add(0, 1, 2))
                .animatie("burger_1", 438, "groet").animatie("burger_3", 438, "juich").animatie("burger_5", 440, "dans").animatie("burger_4", 440, "zonnegroet")
                .deeltjes(440, ParticleTypes.FIREWORK, plein.add(0, 10, -4), 80, 6).geluid(440, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f)
                .cameraKnipVolgt(444, plein.add(2.5, 1.6, -2.0), "sokka").zeg(446, "sokka", "plan", 70).animatie("sokka", 446, "plan")
                .cameraKnipVolgt(518, plein.add(4.0, 1.4, -2.0), "toph").zeg(520, "toph", "voeten", 55).animatie("toph", 520, "lach")
                .cameraKnipVolgt(577, plein.add(-4.0, 1.6, -2.0), "katara").zeg(579, "katara", "samen", 55).animatie("katara", 579, "armen")
                .cameraKnip(636, plein.add(-1.0, 1.6, -2.5), plein.add(-0.7, 1.2, 0)).kijk("aang", 634, plein.add(0, 1, 0))
                .zeg(638, "aang", "juiste_guh", 90).animatie("aang", 638, "lach")
                .loop("zuka", 728, 780, plein.add(0, 0, -1.6)).cameraKnip(730, plein.add(1.5, 1.7, -4.0), plein.add(0, 1.4, -1.0))
                .zeg(782, "zuka", "dank", 90).kijk("zuka", 780, plein.add(0, 1, 0)).animatie("zuka", 782, "buig")
                .cameraKnipVolgt(876, zuka.add(-3.5, 1.6, 3.0), "miroh").zeg(878, "miroh", "mooiste_dag", 100).animatie("miroh", 878, "thee")
                .zeg(980, "miroh", "theehuis", 70).animatie("miroh", 980, "lach")
                .cameraKnip(1054, plein.add(0, 5, 16), zuka.add(0, 2, 0)).camera(1250, plein.add(0, 18, 30), zuka.add(0, 4, 0))
                .zeg(1056, "", "zo", 80)
                .deeltjes(1140, ParticleTypes.FIREWORK, zuka.add(-6, 14, -4), 100, 7).deeltjes(1170, ParticleTypes.FIREWORK, zuka.add(6, 16, -4), 100, 7)
                .deeltjes(1200, ParticleTypes.FIREWORK, zuka.add(0, 20, -8), 140, 9)
                .geluid(1140, () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f).geluid(1170, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.9f)
                .geluid(1200, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.0f, 1.0f)
                .zwart(1262, 1300);
        return b.registreer();
    }

    /** The credits: the camera rises over the capital while the names roll by (anchor kroning_zuka). 75 s. */
    private static Cutscene aftiteling() {
        Vec3 m = v(0.5, 0, 0.5);
        Cutscene.Builder b = finale("aftiteling", 1500)
                .camera(0, m.add(0, 6, 20), m.add(0, 3, 0)).camera(300, m.add(-30, 30, 40), m.add(0, 5, 0))
                .camera(700, m.add(-60, 60, 0), m.add(0, 0, 40)).camera(1100, m.add(0, 80, -60), m.add(0, 0, 60))
                .camera(1440, m.add(40, 100, -20), m.add(0, 40, 200));
        String[] regels = {"titel", "jij", "aang", "waterstam", "toph", "zuka_miroh", "appa_momo", "suki", "mikai", "azula", "voorgangers",
                "wezens", "muziek", "knipoog", "iedereen"};
        int t = 20, stap = 86;
        for (String r : regels) {
            b.lied(t, r, stap - 6);
            t += stap;
        }
        b.deeltjes(40, ParticleTypes.FIREWORK, m.add(0, 30, -10), 80, 10).deeltjes(400, ParticleTypes.FIREWORK, m.add(-30, 40, 10), 80, 12)
                .deeltjes(800, ParticleTypes.FIREWORK, m.add(-10, 50, 40), 100, 14)
                .zeg(t + 10, "", "einde", 90)
                .zwart(1460, 1500);
        return b.registreer();
    }

    /** Wink 4: 626-guh at the control desk of Het Kokend Rotsblok (anchor knipoog_stitch_paneel). 8.5 s. */
    private static Cutscene knipoogStitch() {
        String a = "knipoog_stitch_paneel";
        Vec3 deur1 = r(a, "knipoog_stitch_celdeur_1", 0, 0, 0), deur2 = r(a, "knipoog_stitch_celdeur_2", 0, 0, 0),
                deur3 = r(a, "knipoog_stitch_celdeur_3", 0, 0, 0);
        Vec3 oog = r(a, "knipoog_stitch_camera", 0, 0, 0);
        Plekken.Plek cam = Plekken.plek(S, "knipoog_stitch_camera");
        BlockPos ap = Plekken.plek(S, a) == null ? BlockPos.ZERO : Plekken.plek(S, a).pos();
        if (cam != null && !cam.punten().isEmpty()) {
            BlockPos o = cam.punten().get(0);
            oog = new Vec3(o.getX() - ap.getX() + 0.5, o.getY() - ap.getY() + 0.5, o.getZ() - ap.getZ() + 0.5);
        }
        Cutscene.Builder b = Cutscene.maak(ID + "knipoog_stitch").duur(170).bij(E2Verhaal.VUUR).kaart(E2H4Vuur.KAART).cameraOntwijkt().verbergEcht(6)
                .guh("stitch", GuhVariant.STITCH626, v(0.5, 1, 0.5), 90)
                .npc("bewaker", Kind.VUURNATIE_MIKA, r(a, "knipoog_stitch_bewaker", 0, 0, 0), 180);
        b.camera(0, oog, v(0.5, 1.4, 0.5))
                .zeg(8, "stitch", "aloha", 40).animatie("stitch", 8, "spring")
                .geluid(52, () -> SoundEvents.STONE_BUTTON_CLICK_ON, 1.0f, 1.0f).geluid(59, () -> SoundEvents.STONE_BUTTON_CLICK_ON, 1.0f, 1.2f)
                .geluid(66, () -> SoundEvents.LEVER_CLICK, 1.0f, 1.0f)
                .cameraKnip(72, deur2.add(0, 4, 5), deur2.add(0, 1, 0))
                .geluid(74, () -> SoundEvents.IRON_DOOR_OPEN, 1.0f, 1.0f).geluid(78, () -> SoundEvents.IRON_DOOR_OPEN, 1.0f, 0.9f)
                .geluid(82, () -> SoundEvents.IRON_DOOR_OPEN, 1.0f, 1.1f)
                .deeltjes(74, ParticleTypes.POOF, deur1.add(0, 1, 0), 10, 0.3).deeltjes(78, ParticleTypes.POOF, deur2.add(0, 1, 0), 10, 0.3)
                .deeltjes(82, ParticleTypes.POOF, deur3.add(0, 1, 0), 10, 0.3)
                .cameraKnipVolgt(100, r(a, "knipoog_stitch_bewaker", 1.5, 1.6, -2.0), "bewaker")
                .zeg(102, "bewaker", "wie", 60).animatie("bewaker", 102, "duw");
        return b.registreer();
    }

    /** Wink 1, the finish: Sjokkel crosses the line of the luchtscooter-baan at last (anchor knipoog_sjokkel_finish). 9 s. */
    private static Cutscene knipoogSjokkel() {
        String a = "knipoog_sjokkel_finish";
        Plekken.Plek pad = Plekken.plek(S, "knipoog_sjokkel"), cam = Plekken.plek(S, "knipoog_sjokkel_camera");
        BlockPos ap = Plekken.plek(S, a) == null ? BlockPos.ZERO : Plekken.plek(S, a).pos();
        java.util.List<Vec3> punten = new java.util.ArrayList<>();
        if (pad != null) {
            for (BlockPos q : pad.punten()) {
                punten.add(new Vec3(q.getX() - ap.getX() + 0.5, q.getY() - ap.getY(), q.getZ() - ap.getZ() + 0.5));
            }
        }
        if (punten.size() < 2) {
            punten = java.util.List.of(v(6.5, 1, 6.5), v(3.5, 1, 3.5), v(1.5, 1, 0.5));
        }
        Vec3 oog = v(9.5, 4.5, 3.5);
        if (cam != null && !cam.punten().isEmpty()) {
            BlockPos o = cam.punten().get(0);
            oog = new Vec3(o.getX() - ap.getX() + 0.5, o.getY() - ap.getY() + 0.5, o.getZ() - ap.getZ() + 0.5);
        }
        Vec3 finish = punten.get(punten.size() - 1);
        Cutscene.Builder b = Cutscene.maak(ID + "knipoog_sjokkel_finish").duur(180).bij(E2Verhaal.FINALE).kaart(E2H4Vuur.KAART).cameraOntwijkt()
                .acteur("sjokkel", LanddiertjesFeature.SHUCKLE, punten.get(0), Cutscene.yawVan(finish.subtract(punten.get(0))))
                .npc("aang", Kind.AANG_GUH, finish.add(-2.0, 0, 2.5), 140);
        int t = 0, stuk = 110 / (punten.size() - 1);
        for (int i = 1; i < punten.size(); i++) {
            b.loop("sjokkel", t, t + stuk, punten.get(i));
            t += stuk;
        }
        b.camera(0, oog, finish.add(0, 0.3, 0))
                .zeg(6, "", "sjokkel", 70)
                .deeltjes(118, ParticleTypes.FIREWORK, finish.add(0, 1.5, 0), 30, 0.8).geluid(118, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f)
                .zeg(124, "aang", "gehaald", 45).animatie("aang", 124, "lach");
        return b.registreer();
    }
}
