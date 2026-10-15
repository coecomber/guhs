package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import org.slf4j.Logger;

/**
 * The camera scenes of chapter 3 (CONTRACT_E2_B 5): {@code einde2_h3_aarde_<naam>}. Every position comes from the sidecar of
 * the build ({@link Plekken}): the actors stand on the markers, the cameras are the pads {@code <scene>_camera} that the
 * build's own self-check proved free (tools/features/einde2_bouw_aarde_verhaal.py). So a scene is written once, against the
 * one guaranteed copy, and never looks into a wall. Positions are relative to the scene's anchor marker ({@link Scene#anker}).
 * <p>
 * Lengths (running time without the waits for a click; CONTRACT_E2_B 5): aankomst 17 s, loket 14 s, aardrommel 26 s,
 * jasmijn_draak 27 s, <b>appa_ontvoerd 55 s</b>, long_feng 26 s, weerzien 23 s, <b>lu_ten 75 s</b> (only the song's
 * captions and one line at the end), <b>val 84 s</b>, knipoog_mewtwo 9 s, knipoog_sjokkel 8 s. Texts:
 * scene.guhs.einde2_h3_aarde_*.
 */
public final class H3Scenes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String S = E2H3Aarde.STRUCTUUR;

    /** A scene and the marker it is anchored on. */
    public record Scene(Cutscene cutscene, String anker) {
        public String id() {
            return cutscene.id();
        }
    }

    /** Every scene of the chapter by its short name (the dev command: {@code /guhs einde2 h3 scene <naam>}). */
    public static final Map<String, Scene> ALLE = new LinkedHashMap<>();

    public static final Scene AANKOMST = aankomst();
    public static final Scene LOKET = loket();
    public static final Scene AARDROMMEL = aardrommel();
    public static final Scene JASMIJN_DRAAK = jasmijnDraak();
    public static final Scene APPA_ONTVOERD = appaOntvoerd();
    public static final Scene LONG_FENG = longFeng();
    public static final Scene WEERZIEN = weerzien();
    public static final Scene LU_TEN = luTen();
    public static final Scene VAL = val();
    public static final Scene KNIPOOG_MEWTWO = knipoogMewtwo();
    public static final Scene KNIPOOG_SJOKKEL = knipoogSjokkel();

    /** The song of {@link #LU_TEN}: the tick each caption starts at (8 lines; own words, own tune, CONTRACT_E2_B 6.3). */
    public static final int[] LIED = {240, 362, 484, 606, 728, 850, 972, 1094};

    private H3Scenes() {
    }

    /** (from E2H3AardeFeature.register) loads the class: every scene above is registered. */
    static void registreer() {
        LOGGER.debug("einde2 h3_aarde: {} scenes", ALLE.size());
    }

    // =====================================================================================================================
    // playing
    // =====================================================================================================================

    /** Plays a scene of the chapter for this player in this copy of the city; false: it could not start (nothing happens). */
    public static boolean speel(ServerPlayer p, E2H3Aarde.Stad stad, Scene s, @Nullable Consumer<ServerPlayer> daarna) {
        BlockPos anker = stad.pos(s.anker());
        return anker != null && Cutscenes.speel(p, s.cutscene(), anker, Rotation.NONE, daarna);
    }

    /** The same with a big moment's music under it (E2Audio): it fades when the scene is over, also when it is skipped. */
    public static boolean speelMetMuziek(ServerPlayer p, E2H3Aarde.Stad stad, Scene s, String moment, @Nullable Consumer<ServerPlayer> daarna) {
        E2Audio.moment(p, moment);
        boolean ok = speel(p, stad, s, speler -> {
            E2Audio.stopMoment(speler, 40);
            if (daarna != null) {
                daarna.accept(speler);
            }
        });
        if (!ok) {
            E2Audio.stopMoment(p, 10);
        }
        return ok;
    }

    // =====================================================================================================================
    // positions out of the sidecar
    // =====================================================================================================================

    private static BlockPos m(String id) {
        Plekken.Plek p = Plekken.plek(S, id);
        if (p == null) {
            LOGGER.warn("einde2 h3_aarde: the sidecar of {} has no marker {} (scene positions fall back to the anchor)", S, id);
            return BlockPos.ZERO;
        }
        return p.pos();
    }

    /** Where an actor stands on marker id (the middle of its block, on the floor), relative to the anchor. */
    private static Vec3 op(String anker, String id) {
        return op(anker, id, 0, 0, 0);
    }

    private static Vec3 op(String anker, String id, double dx, double dy, double dz) {
        BlockPos a = m(anker), b = m(id);
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    /** The middle of a template block, relative to the anchor. */
    private static Vec3 blok(String anker, BlockPos t) {
        BlockPos a = m(anker);
        return new Vec3(t.getX() - a.getX() + 0.5, t.getY() - a.getY() + 0.5, t.getZ() - a.getZ() + 0.5);
    }

    /** A camera pad of the build: its eye points and (last) what it looks at, relative to the anchor. */
    private record Cam(List<Vec3> ogen, Vec3 kijk) {
        Vec3 oog(int i) {
            return ogen.get(Math.min(i, ogen.size() - 1));
        }
    }

    private static Cam cam(String anker, String scene) {
        Plekken.Plek p = Plekken.plek(S, scene + "_camera");
        if (p == null || p.punten().size() < 2) {
            LOGGER.warn("einde2 h3_aarde: no camera {}_camera in the sidecar of {}", scene, S);
            return new Cam(List.of(new Vec3(4, 3, 4)), new Vec3(0.5, 1, 0.5));
        }
        List<Vec3> ogen = new ArrayList<>();
        for (int i = 0; i < p.punten().size() - 1; i++) {
            ogen.add(blok(anker, p.punten().get(i)));
        }
        return new Cam(ogen, blok(anker, p.punten().get(p.punten().size() - 1)));
    }

    private static Vec3 plus(Vec3 v, double dx, double dy, double dz) {
        return v.add(dx, dy, dz);
    }

    private static Cutscene.Builder maak(String naam) {
        return Cutscene.maak("einde2_h3_aarde_" + naam).bij(E2H3Aarde.LIJN.id()).kaart(E2H3Aarde.KAART);
    }

    private static Scene reg(String naam, Cutscene.Builder b, String anker) {
        Scene s = new Scene(b.registreer(), anker);
        ALLE.put(naam, s);
        return s;
    }

    // =====================================================================================================================
    // the scenes
    // =====================================================================================================================

    /** Arriving: from the lobe over the open outer gate; the team says what Ba Sing Guh means to them. 17 s. */
    private static Scene aankomst() {
        String a = "aankomst_speler";
        Cam c = cam(a, "h3_aankomst");
        Vec3 poort = op(a, "buitenpoort");
        return reg("aankomst", maak("aankomst").duur(340).verbergEcht(10)
                .speler(op(a, a), -90f)
                .npc("aang", Kind.AANG_GUH, op(a, a, 1.2, 0, -1.6), -90f)
                .npc("katara", Kind.KATARA_GUH, op(a, a, -0.8, 0, 1.6), -90f)
                .npc("sokka", Kind.SOKKA_GUH, op(a, a, 1.0, 0, 2.6), -90f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, op(a, a, 2.0, 0, -0.4), -90f)
                .acteur("appa", E2AppaFeature.APPA_GUH, op(a, a, -6, 0, 0), -90f)
                .camera(0, c.oog(0), c.kijk()).camera(180, c.oog(1), c.kijk()).camera(340, plus(c.oog(1), 1.5, -0.5, 0), c.kijk())
                .zeg(12, "", "begin", 64)
                .kijk("sokka", 76, poort).zeg(80, "sokka", "muur", 52)
                .kijk("katara", 136, op(a, a)).zeg(140, "katara", "hoop", 56)
                .animatie("aang", 196, "lach").zeg(200, "aang", "knabbels", 54)
                .zeg(262, "", "poort", 60)
                .loop("aang", 270, 330, plus(poort, -3, 0, -1)).loop("sokka", 278, 338, plus(poort, -4, 0, 2))
                .loop("katara", 284, 338, plus(poort, -5, 0, 1)).loop("momo", 270, 320, plus(poort, -2, 0, 0)), a);
    }

    /** The passport desk: one stamp, then another, then the gate opens with a sigh. 14 s. */
    private static Scene loket() {
        String a = "loket";
        Cam c = cam(a, "h3_loket");
        return reg("loket", maak("loket").duur(280).verbergEcht(6)
                .npc("loketguh", Kind.LOKETGUH, op(a, "loketguh"), 0f)
                .speler(op(a, a, 0, 0, 1.5), 180f)
                .camera(0, c.oog(0), c.kijk()).camera(280, plus(c.oog(0), 0.6, -0.3, -0.6), c.kijk())
                .animatie("loketguh", 16, "stempel").geluid(22, () -> SoundEvents.WOOD_HIT, 1f, 0.7f)
                .zeg(12, "loketguh", "stempel", 40)
                .animatie("loketguh", 62, "stempel").geluid(68, () -> SoundEvents.WOOD_HIT, 1f, 0.8f)
                .zeg(60, "loketguh", "papieren", 62)
                .animatie("loketguh", 128, "praat").zeg(130, "loketguh", "welkom", 66)
                .geluid(204, () -> SoundEvents.PISTON_EXTEND, 0.8f, 0.6f).schud(206, 0.4f, 10)
                .zeg(204, "", "poort", 64), a);
    }

    /** The Aardrommel: the announcer, De Kei-guh, and one stamp of De Blinde Bandiet. 26 s. */
    private static Scene aardrommel() {
        String a = "toph";
        Cam wijd = cam(a, "h3_aardrommel"), laag = cam(a, "h3_aardrommel_toph");
        Vec3 kei = op(a, "kei");
        return reg("aardrommel", maak("aardrommel").duur(520).verbergEcht(22)
                .npc("toph", Kind.TOPH_GUH, op(a, a), 0f)
                .npc("kei", Kind.KEI_GUH, kei, 180f)
                .npc("omroeper", Kind.AARDROMMEL_OMROEPER, op(a, "omroeper"), 0f)
                .npc("burger_1", Kind.BA_SING_GUH_BURGER, op(a, "omroeper", -3, 0, 0), 0f)
                .npc("burger_2", Kind.BA_SING_GUH_BURGER, op(a, "omroeper", 3, 0, 0), 0f)
                .speler(op(a, "rustpunt_2"), -45f)
                .camera(0, wijd.oog(0), wijd.kijk()).camera(200, plus(wijd.oog(0), 1, -0.5, 1), wijd.kijk())
                .cameraKnipVolgt(204, laag.oog(0), "toph").camera(258, plus(laag.oog(0), 0.3, 0, 0.3), laag.kijk())
                .cameraKnip(262, wijd.oog(0), wijd.kijk()).camera(380, plus(wijd.oog(0), 1, -0.5, 1), wijd.kijk())
                .cameraKnipVolgt(384, laag.oog(0), "toph").camera(520, plus(laag.oog(0), -0.4, 0.2, -0.4), laag.kijk())
                .animatie("omroeper", 10, "roep").zeg(10, "omroeper", "kei", 62)
                .animatie("kei", 78, "spierbal").zeg(80, "kei", "rotsen", 52)
                .animatie("omroeper", 138, "roep").zeg(140, "omroeper", "bandiet", 64)
                .animatie("toph", 208, "lach").zeg(210, "toph", "kom", 44)
                .animatie("toph", 264, "stamp").schud(270, 1.6f, 12).geluid(270, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.4f)
                .deeltjes(270, ParticleTypes.CLOUD, plus(kei, 0, 0.5, 0), 30, 1.2)
                .loop("kei", 272, 288, plus(kei, 0, 4, 7)).loop("kei", 288, 302, plus(kei, 0, 0, 11))
                .geluid(302, () -> SoundEvents.STONE_FALL, 1f, 0.6f)
                .zeg(320, "kei", "verslagen", 62)
                .zeg(390, "toph", "wie", 42)
                .kijk("toph", 436, op(a, "rustpunt_2")).animatie("toph", 438, "wijs").zeg(440, "toph", "jij", 74), a);
    }

    /** De Jasmijn Draak: Miroh's welcome, Zuka the waiter, the look that lasts too long. 27 s. */
    private static Scene jasmijnDraak() {
        String a = "miroh";
        Cam buiten = cam(a, "h3_thee_buiten"), binnen = cam(a, "h3_thee_binnen"), zuka = cam(a, "h3_thee_zuka");
        Vec3 speler = op(a, a, 0, 0, -5.5);
        return reg("jasmijn_draak", maak("jasmijn_draak").duur(540).verbergEcht(12)
                .npc("miroh", Kind.OOM_MIROH, op(a, a), 180f)
                .npc("zuka", Kind.ZUKA, op(a, "zuka_ober"), 180f)
                .npc("klant_1", Kind.BA_SING_GUH_BURGER, op(a, "klant_1"), 180f)
                .npc("klant_2", Kind.BA_SING_GUH_BURGER, op(a, "klant_2"), 180f)
                .npc("klant_3", Kind.BA_SING_GUH_BURGER, op(a, "klant_3"), 0f)
                .speler(speler, 0f)
                .camera(0, buiten.oog(0), buiten.kijk()).camera(60, plus(buiten.oog(0), 0, -0.5, 1.5), buiten.kijk())
                .cameraKnip(64, binnen.oog(0), binnen.kijk()).camera(196, plus(binnen.oog(0), -0.5, 0, 0.5), binnen.kijk())
                .cameraKnipVolgt(200, zuka.oog(0), "zuka").camera(380, plus(zuka.oog(0), 0.5, 0, 0.5), zuka.kijk())
                .cameraKnip(384, binnen.oog(0), binnen.kijk()).camera(540, plus(binnen.oog(0), -0.8, 0, 0.8), binnen.kijk())
                .geluid(0, E2Audio.geluid("deuntje.theehuis"), 0.7f, 1f)
                .zeg(10, "", "draak", 54)
                .animatie("miroh", 68, "lach").zeg(70, "miroh", "welkom", 54)
                .animatie("miroh", 128, "thee").zeg(130, "miroh", "vreemde", 84)
                .loop("zuka", 200, 240, plus(speler, 0.8, 0, 1.2)).kijk("zuka", 240, speler).animatie("zuka", 242, "zucht")
                .zeg(244, "zuka", "thee", 34).geluid(244, () -> SoundEvents.DECORATED_POT_INSERT, 0.8f, 1.2f)
                .animatie("miroh", 290, "wijsheid").zeg(290, "miroh", "zuur", 64)
                .zeg(360, "zuka", "neef", 54)
                .zeg(420, "", "blik", 62)
                .loop("zuka", 470, 520, op(a, "zuka_ober"))
                .animatie("miroh", 470, "praat").zeg(476, "miroh", "ketel", 64), a);
    }

    /**
     * "Appa's verloren dagen": the Dai Li take Appa-guh from the camp; days under the lake, told over the city at night;
     * the empty camp. 55 s (a big emotional moment, DESIGN 5 h3).
     */
    private static Scene appaOntvoerd() {
        String a = "appa_kamp";
        Cam kamp = cam(a, "h3_appa_kamp"), stad = cam(a, "h3_appa_stad"), leeg = cam(a, "h3_appa_leeg");
        Vec3 appa = op(a, a), weg = op(a, a, -14, 0, -4), diep = op(a, a, 0, -40, 0);
        Cutscene.Builder b = maak("appa_ontvoerd").duur(1090).verbergEcht(14)
                .acteur("appa", E2AppaFeature.APPA_GUH, appa, 90f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, plus(appa, 0.6, 2.6, 0.2), 90f)
                .camera(0, kamp.oog(0), kamp.kijk()).camera(370, plus(kamp.oog(0), 1.5, -0.5, 1.5), kamp.kijk())
                .zwart(376, 440)
                .cameraKnip(440, stad.oog(0), stad.kijk()).camera(780, plus(stad.oog(0), 3, -1, 2), stad.kijk())
                .zwart(782, 830)
                .cameraKnip(830, leeg.oog(0), leeg.kijk()).camera(1090, plus(leeg.oog(0), 0.6, 0.4, 0.6), leeg.kijk())
                .zeg(20, "", "dut", 74);
        double[][] dl = {{-3, -2.5}, {3, -2.5}, {-3, 2.5}, {3, 2.5}};
        for (int i = 0; i < dl.length; i++) {
            String n = "dai_li_" + (i + 1);
            Vec3 plek = plus(appa, dl[i][0], 0, dl[i][1]);
            b.npc(n, Kind.DAI_LI_MIKA, plus(plek, 0, -3, 0), dl[i][0] < 0 ? -90f : 90f)
                    .loop(n, 118 + i * 4, 134 + i * 4, plek).animatie(n, 140, "stenen_hand")
                    .loop(n, 300 + i * 3, 372, plus(weg, dl[i][0] * 0.5, 0, dl[i][1] * 0.5))
                    .loop(n, 441, 442, plus(diep, i, 0, 0));
        }
        return reg("appa_ontvoerd", b
                .schud(120, 0.9f, 20).geluid(120, () -> SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.5f)
                .deeltjes(120, ParticleTypes.CLOUD, plus(appa, 0, 0.5, 0), 40, 3)
                .zeg(130, "", "stenen", 50)
                .zeg(190, "dai_li_1", "mee", 54)
                .geluid(252, E2AppaFeature.BRUL::get, 1f, 0.8f)
                .zeg(256, "", "brul", 56)
                .loop("appa", 300, 372, weg).loop("momo", 250, 300, plus(appa, -4, 0, 6))
                .zeg(326, "", "momo", 46)
                .loop("appa", 441, 442, diep).loop("momo", 441, 442, plus(diep, 0, 0, 2))
                .zeg(452, "", "dag1", 80)
                .zeg(540, "", "dag2", 84)
                .zeg(632, "", "dag3", 84)
                .zeg(722, "", "wacht", 56)
                .loop("momo", 829, 830, plus(appa, -1.5, 0, -0.5)).kijk("momo", 830, appa)
                .deeltjes(840, ParticleTypes.WHITE_ASH, plus(appa, 0, 0.3, 0), 18, 0.8)
                .zeg(846, "", "leeg", 62)
                .geluid(926, E2AppaFeature.MOMO::get, 0.7f, 0.7f)
                .zeg(930, "", "momo2", 64)
                .zeg(1010, "", "zoek", 64), a);
    }

    /** Meer Laogai: Long Feng-Mika steps out of the dark at Appa's bars; the team arrives; he sinks away. 26 s. */
    private static Scene longFeng() {
        String a = "appa_poort";
        Cam c = cam(a, "h3_laogai"), dicht = cam(a, "h3_laogai_long_feng");
        Vec3 lf = op(a, "laogai_long_feng"), team = op(a, "laogai_team");
        return reg("long_feng", maak("long_feng").duur(520).verbergEcht(12)
                .speler(op(a, a, 0, 0, -1.2), 0f)
                .acteur("appa", E2AppaFeature.APPA_GUH, op(a, "appa_gevangen"), 180f)
                .npc("long_feng", Kind.LONG_FENG_MIKA, plus(lf, -5, 0, 0), -90f)
                .npc("dai_li_1", Kind.DAI_LI_MIKA, plus(lf, -7, 0, -1), -90f)
                .npc("dai_li_2", Kind.DAI_LI_MIKA, plus(lf, -7, 0, 1), -90f)
                .npc("aang", Kind.AANG_GUH, plus(team, -1, 0, -6), 0f)
                .npc("katara", Kind.KATARA_GUH, plus(team, 1, 0, -6), 0f)
                .npc("sokka", Kind.SOKKA_GUH, plus(team, 0, 0, -7), 0f)
                .camera(0, c.oog(0), c.kijk()).camera(196, plus(c.oog(0), -0.6, 0, 0.4), c.kijk())
                .cameraKnipVolgt(200, dicht.oog(0), "long_feng").camera(326, plus(dicht.oog(0), 0.3, 0, 0.3), dicht.kijk())
                .cameraKnip(330, c.oog(0), c.kijk()).camera(520, plus(c.oog(0), 0.5, 0.2, -0.5), c.kijk())
                .zeg(10, "", "tralies", 54)
                .loop("long_feng", 56, 110, lf).loop("dai_li_1", 60, 116, plus(lf, -2, 0, -1)).loop("dai_li_2", 64, 118, plus(lf, -2, 0, 1))
                .kijk("long_feng", 112, op(a, a, 0, 0, -1.2))
                .animatie("long_feng", 116, "buig").zeg(118, "long_feng", "knap", 62)
                .animatie("long_feng", 182, "praat").zeg(184, "long_feng", "niets", 60)
                .animatie("long_feng", 246, "fluister").zeg(248, "long_feng", "knabbel", 66)
                .loop("aang", 296, 336, plus(team, -1, 0, 0)).loop("katara", 300, 340, plus(team, 1, 0, 0)).loop("sokka", 304, 344, plus(team, 0, 0, -1))
                .animatie("sokka", 346, "plan").zeg(346, "sokka", "dit", 54)
                .animatie("katara", 404, "armen").zeg(404, "katara", "liegen", 62)
                .zeg(470, "long_feng", "weg", 50)
                .geluid(490, () -> SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.6f)
                .loop("long_feng", 490, 516, plus(lf, 0, -3, 0)).loop("dai_li_1", 490, 516, plus(lf, -2, -3, -1)).loop("dai_li_2", 490, 516, plus(lf, -2, -3, 1)),
                a);
    }

    /** The reunion: the bars give, Appa-guh sniffs, and then licks you from head to toe. 23 s. */
    private static Scene weerzien() {
        String a = "appa_poort";
        Cam c = cam(a, "h3_weerzien"), gang = cam(a, "h3_laogai");
        Vec3 speler = op(a, a, 0, 0, -1.2), team = op(a, "laogai_team");
        return reg("weerzien", maak("weerzien").duur(450).verbergEcht(12)
                .speler(speler, 0f)
                .acteur("appa", E2AppaFeature.APPA_GUH, op(a, "appa_gevangen"), 180f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, plus(speler, 1.2, 0, -0.4), 0f)
                .npc("aang", Kind.AANG_GUH, plus(team, -1, 0, 0), 0f)
                .npc("katara", Kind.KATARA_GUH, plus(team, 1, 0, 0), 0f)
                .npc("sokka", Kind.SOKKA_GUH, plus(team, 0, 0, -1), 0f)
                .camera(0, c.oog(0), c.kijk()).camera(246, plus(c.oog(0), 0.6, 0, 0.6), c.kijk())
                .cameraKnip(250, gang.oog(0), gang.kijk()).camera(450, plus(gang.oog(0), -0.5, 0, 0.5), gang.kijk())
                .schud(2, 1.2f, 14).geluid(2, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.6f)
                .deeltjes(4, ParticleTypes.CLOUD, op(a, a, 0, 1.5, 0), 40, 2.0)
                .zeg(10, "", "krak", 46)
                .loop("appa", 60, 140, op(a, a, 0, 0, 2.0))
                .zeg(90, "", "appa", 52)
                .geluid(160, E2AppaFeature.BRUL::get, 1f, 1.2f).animatie("momo", 162, "spring")
                .animatie("aang", 170, "lach").zeg(172, "aang", "appa", 54)
                .deeltjes(236, ParticleTypes.SPLASH, plus(speler, 0, 1.2, 0), 40, 0.6)
                .zeg(240, "", "lik", 56)
                .zeg(310, "katara", "thuis", 52)
                .animatie("sokka", 378, "boemerang").zeg(380, "sokka", "jipjip", 44).geluid(384, E2AppaFeature.JIP_JIP::get, 1f, 1f), a);
    }

    /**
     * Lu Ten: no dialogue. Miroh kneels under the tree with a knabbel; his own lullaby as captions that do not wait
     * ({@code Builder.lied}, CONTRACT_E2_B 12a.1); the camera pulls slowly back over the void; one quiet line at the end.
     * 75 s (a big emotional moment).
     */
    private static Scene luTen() {
        String a = "lu_ten_boom";
        Cam dicht = cam(a, "lu_ten_dicht"), weg = cam(a, "lu_ten");
        Vec3 miroh = op(a, "lu_ten_miroh"), boom = op(a, a);
        Cutscene.Builder b = maak("lu_ten").duur(1500).verbergEcht(10)
                .npc("miroh", Kind.OOM_MIROH, miroh, 90f)
                .speler(op(a, "lu_ten_speler"), 135f)
                .camera(0, dicht.oog(0), dicht.kijk()).camera(260, plus(dicht.oog(0), -0.6, 0.2, 0.3), dicht.kijk())
                .camera(320, weg.oog(0), weg.kijk()).camera(620, weg.oog(1), weg.kijk()).camera(960, weg.oog(2), weg.kijk())
                .camera(1300, weg.oog(3), weg.kijk()).camera(1396, plus(weg.oog(3), -0.5, 0.3, -0.5), weg.kijk())
                .cameraKnip(1400, dicht.oog(0), dicht.kijk()).camera(1500, plus(dicht.oog(0), -0.4, 0, 0.2), dicht.kijk())
                .kijk("miroh", 30, boom).animatie("miroh", 40, "thee")
                .deeltjes(60, ParticleTypes.CHERRY_LEAVES, plus(boom, 0, 4, 0), 14, 2.5)
                .kijk("miroh", 200, plus(miroh, -6, 0, 0))
                .kijk("speler", 10, miroh);
        for (int t = 300; t < 1400; t += 160) {
            b.deeltjes(t, ParticleTypes.CHERRY_LEAVES, plus(boom, 0, 4, 0), 10, 3.0);
        }
        for (int i = 0; i < LIED.length; i++) {
            b.lied(LIED[i], "lied." + i, 116);
        }
        return reg("lu_ten", b.animatie("miroh", 1404, "wijsheid").zeg(1410, "miroh", "delen", 84), a);
    }

    /**
     * The fall of Ba Sing Guh: Azula-Mika, Zuka's choice, the Guhvatar state, the lightning (it stuns, nothing more),
     * Katara-guh's spirit water, Miroh holding them off, the flight up the Koningsput on Appa. 84 s (the big moment of the
     * chapter).
     */
    private static Scene val() {
        String a = "bliksem_speler";
        Cam wijd = cam(a, "h3_val_wijd"), zuka = cam(a, "h3_val_zuka"), bliksem = cam(a, "h3_val_bliksem"), katara = cam(a, "h3_val_katara"),
                schacht = cam(a, "h3_val_schacht");
        Vec3 sp = op(a, a, 2, 0, 2), azula = op(a, "azula"), keuze = op(a, "zuka_keuze"), redt = op(a, "katara_redt"), miroh = op(a, a, 3, 0, 4),
                gevangen = op(a, "miroh_gevangen"), appaBoven = op(a, "val_appa_boven"), appaOnder = plus(appaBoven, 0, -22, 0),
                bazula = op(a, "bliksem_azula");
        Cutscene.Builder b = maak("val").duur(1680).verbergEcht(24)
                .speler(sp, 90f)
                .npc("katara", Kind.KATARA_GUH, plus(sp, 0, 0, 2), 90f)
                .npc("aang", Kind.AANG_GUH, plus(sp, 1.5, 0, -1), 90f)
                .npc("miroh", Kind.OOM_MIROH, miroh, 90f)
                .npc("zuka", Kind.ZUKA, plus(sp, -2, 0, 2.5), 90f)
                .npc("azula", Kind.AZULA_MIKA, plus(azula, -10, 0, 0), -90f)
                .acteur("appa", E2AppaFeature.APPA_GUH, appaBoven, 0f)
                .camera(0, wijd.oog(0), wijd.kijk()).camera(320, plus(wijd.oog(0), -1, -1, -1), wijd.kijk())
                .cameraKnipVolgt(324, zuka.oog(0), "zuka").camera(516, plus(zuka.oog(0), 0.4, 0, 0.4), zuka.kijk())
                .cameraKnip(520, wijd.oog(0), wijd.kijk()).camera(756, plus(wijd.oog(0), -2, -1, -1), wijd.kijk())
                .cameraKnip(760, bliksem.oog(0), bliksem.kijk()).camera(950, plus(bliksem.oog(0), 0.5, 0.4, -0.5), bliksem.kijk())
                .cameraKnip(1012, katara.oog(0), katara.kijk()).camera(1240, plus(katara.oog(0), 0.4, 0, 0.4), katara.kijk())
                .cameraKnip(1244, schacht.oog(0), schacht.kijk()).camera(1420, plus(schacht.oog(0), 0.5, 0.5, 0.5), schacht.kijk())
                .camera(1560, plus(appaOnder, -2.5, 24, -2.5), plus(appaOnder, 0, 34, 0))
                .camera(1680, plus(appaOnder, -2.0, 30, -2.0), plus(appaOnder, 0, 40, 0))
                .zeg(10, "", "kristal", 64)
                .loop("azula", 60, 120, azula).animatie("azula", 80, "grijns").zeg(82, "azula", "reunie", 64)
                .animatie("azula", 158, "lok").zeg(160, "azula", "zuka", 74)
                .kijk("miroh", 244, plus(sp, -2, 0, 2.5)).animatie("miroh", 246, "wijsheid").zeg(250, "miroh", "weg", 84)
                .zeg(340, "", "stil", 62)
                .loop("zuka", 410, 470, keuze).kijk("zuka", 472, op(a, a, 2, 0, 2))
                .zeg(430, "miroh", "zuka_naam", 40)
                .zeg(480, "", "keuze", 52);
        double[][] dl = {{-5, -4}, {-5, 8}, {-10, 2}};
        for (int i = 0; i < dl.length; i++) {
            String n = "dai_li_" + (i + 1);
            Vec3 plek = plus(sp, dl[i][0], 0, dl[i][1]);
            b.npc(n, Kind.DAI_LI_MIKA, plus(plek, 0, -3, 0), -90f).loop(n, 540 + i * 6, 560 + i * 6, plek).animatie(n, 566, "stenen_hand");
        }
        b.schud(540, 0.8f, 16).geluid(540, () -> SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.5f)
                .animatie("aang", 560, "zweef").zeg(562, "aang", "nu", 62)
                .loop("speler", 640, 700, plus(sp, 0, 3, 0))
                .deeltjes(640, ParticleTypes.END_ROD, plus(sp, 0, 1.5, 0), 40, 1.0).deeltjes(680, ParticleTypes.END_ROD, plus(sp, 0, 4, 0), 50, 1.4)
                .geluid(640, () -> SoundEvents.BEACON_ACTIVATE, 1f, 1.2f)
                .zeg(704, "", "staat", 62)
                .loop("azula", 760, 776, bazula).kijk("azula", 776, plus(sp, 0, 3, 0)).animatie("azula", 778, "bliksem").zeg(780, "azula", "bliksem", 30)
                .geluid(810, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f, 1.3f).schud(810, 2.6f, 18).zwart(808, 812)
                .deeltjes(810, ParticleTypes.ELECTRIC_SPARK, plus(sp, 0, 4, 0), 80, 1.2)
                .deeltjes(810, ParticleTypes.ELECTRIC_SPARK, plus(bazula, 0, 1.5, 0).add(plus(sp, 0, 4, 0)).scale(0.5), 60, 2.5)
                .loop("speler", 820, 880, plus(redt, 0, 0, 0.6))
                .zeg(900, "", "val", 62)
                .zwart(960, 1010)
                .loop("katara", 958, 1010, plus(redt, 1, 0, 0.6)).kijk("katara", 1010, redt)
                .animatie("katara", 1018, "genees").zeg(1020, "katara", "water", 76)
                .deeltjes(1030, ParticleTypes.GLOW, plus(redt, 0, 0.6, 0.6), 30, 0.5).deeltjes(1080, ParticleTypes.GLOW, plus(redt, 0, 0.6, 0.6), 30, 0.5)
                .zeg(1100, "", "ogen", 56)
                .loop("miroh", 1150, 1180, plus(sp, -3, 0, 1)).kijk("miroh", 1180, azula)
                .deeltjes(1176, ParticleTypes.FLAME, plus(sp, -4, 1, 1), 40, 1.2)
                .animatie("miroh", 1170, "wijsheid").zeg(1172, "miroh", "ga", 66)
                .loop("appa", 1250, 1340, appaOnder).geluid(1250, E2AppaFeature.BRUL::get, 1f, 1f)
                .animatie("aang", 1258, "lach").zeg(1260, "aang", "appa", 44)
                .loop("speler", 1350, 1400, plus(appaOnder, 0, 0, 1.5)).loop("katara", 1350, 1404, plus(appaOnder, 1, 0, 1.5))
                .loop("aang", 1350, 1400, plus(appaOnder, -1, 0, 1.5))
                .loop("miroh", 1340, 1380, gevangen).kijk("miroh", 1382, appaOnder)
                .deeltjes(1384, ParticleTypes.HAPPY_VILLAGER, plus(gevangen, 0, 1, 0), 40, 0.9)
                .zeg(1380, "", "miroh", 70);
        for (String n : List.of("appa", "speler", "katara", "aang")) {
            Vec3 van = n.equals("appa") ? appaOnder : n.equals("speler") ? plus(appaOnder, 0, 0, 1.5)
                    : n.equals("katara") ? plus(appaOnder, 1, 0, 1.5) : plus(appaOnder, -1, 0, 1.5);
            b.loop(n, 1420, 1600, plus(van, 0, 34, 0));
        }
        return reg("val", b.geluid(1420, E2AppaFeature.JIP_JIP::get, 1f, 1f).zeg(1590, "", "gevallen", 84), a);
    }

    /** Wink 3 (DESIGN 5c): Mewtwo-guh sits in his tank of his own free will; Mew-guh giggles past. 9 s. */
    private static Scene knipoogMewtwo() {
        String a = "knipoog_mewtwo_tank";
        Cam c = cam(a, "knipoog_mewtwo");
        Plekken.Plek mew = Plekken.plek(S, "knipoog_mewtwo_mew");
        Vec3 mew0 = mew == null ? new Vec3(-4, 2, -3) : blok(a, mew.punten().get(0)), mew1 = mew == null ? new Vec3(-4, 2, 3) : blok(a, mew.punten().get(1));
        return reg("knipoog_mewtwo", maak("knipoog_mewtwo").duur(180).verbergEcht(6)
                .guh("mewtwo", GuhVariant.MEWTWO, op(a, a), 90f)
                .npc("dai_li", Kind.DAI_LI_MIKA, op(a, "knipoog_mewtwo_dai_li"), -90f)
                .acteur("mew", MewtwoFeature.MEW, mew0, 0f)
                .camera(0, c.oog(0), c.kijk()).camera(180, plus(c.oog(0), 0.4, 0, -0.4), c.kijk())
                .zeg(10, "dai_li", "vrijwillig", 56)
                .zeg(80, "mewtwo", "portie", 44)
                .loop("mew", 110, 170, mew1)
                .zeg(124, "", "giechel", 46), a);
    }

    /** Wink 1 (DESIGN 5c, a running joke): Sjokkel creeps on along the wall between two rings. 8 s. */
    private static Scene knipoogSjokkel() {
        Plekken.Plek camera = Plekken.plek(S, "knipoog_sjokkel_camera"), pad = Plekken.plek(S, "knipoog_sjokkel");
        String a = "station_midden";
        Cam c = cam(a, "knipoog_sjokkel");
        Vec3 van = pad == null ? new Vec3(-12, -5, -8) : blok(a, pad.punten().get(0)).add(0, -0.5, 0);
        Vec3 naar = pad == null ? new Vec3(-9, -5, -15) : blok(a, pad.punten().get(1)).add(0, -0.5, 0);
        if (camera == null) {
            LOGGER.warn("einde2 h3_aarde: no knipoog_sjokkel_camera");
        }
        return reg("knipoog_sjokkel", maak("knipoog_sjokkel").duur(160)
                .acteur("sjokkel", LanddiertjesFeature.SHUCKLE, van, 0f)
                .camera(0, c.oog(0), c.kijk()).camera(160, plus(c.oog(0), 0.3, 0, -0.3), c.kijk())
                .loop("sjokkel", 10, 150, van.lerp(naar, 0.6))
                .zeg(20, "", "sjokkel", 74), a);
    }
}
