package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import nl.juiced.guhs.feature.einde2.audio.LuTenLied;
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
 * captions and one line at the end), <b>val 96 s</b> (fix3-h3: 12 s more for Zuka's choice), knipoog_mewtwo 9 s,
 * knipoog_sjokkel 8 s; fix3-h3 (playtest 2: the reason for every step, in scenes): leraar 20 s, middenring 24 s,
 * uitnodiging 22 s, gevolgd 18 s, na_appa 22 s, waarom_miroh 28 s, katara_weg 30 s, <b>kristalcel 38 s</b>,
 * <b>miroh_gevangen 32 s</b>, ontsnapping 25 s, koers 26 s. Texts: scene.guhs.einde2_h3_aarde_*.
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

    /** Miroh's lullaby ends its captions here, before his one spoken line of {@link #LU_TEN}. */
    private static final int LIED_EINDE = 1400;

    /** Appa's flight out over the outer wall to the camp and away (template spots: Appa's feet). */
    private static final double[][] VLUCHT = {{48, 82, 134}, {18, 70, 143}, {15.5, 50, 145.5}, {0, 60, 138}, {-40, 70, 128}};

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
    // fix3-h3 (playtest 2): the story between the steps, Zuka in the crystal cell, Miroh's capture, the escape with Toph
    public static final Scene LERAAR = leraar();
    public static final Scene MIDDENRING = middenring();
    public static final Scene UITNODIGING = uitnodiging();
    public static final Scene GEVOLGD = gevolgd();
    public static final Scene NA_APPA = naAppa();
    public static final Scene WAAROM_MIROH = waaromMiroh();
    public static final Scene KATARA_WEG = kataraWeg();
    public static final Scene KRISTALCEL = kristalcel();
    public static final Scene MIROH_GEVANGEN = mirohGevangen();
    public static final Scene ONTSNAPPING = ontsnapping();
    public static final Scene KOERS = koers();

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
        // the song is audio's (LuTenLied: its bars of moment.lu_ten, which speelMetMuziek starts with the scene); the words are
        // under this scene's own keys lied.<neuriet|1..8> (a scene caption is always scene.guhs.<id>.<key>), same text as audio's
        for (LuTenLied.Regel r : LuTenLied.REGELS) {
            if (r.tick() < LIED_EINDE) {
                b.lied(r.tick(), "lied." + r.key().substring(r.key().lastIndexOf('.') + 1), Math.min(r.ticks(), LIED_EINDE - r.tick()));
            }
        }
        return reg("lu_ten", b.animatie("miroh", 1404, "wijsheid").zeg(1410, "miroh", "delen", 84), a);
    }

    /**
     * The fall of Ba Sing Guh: Azula-Mika, Zuka's choice, the Guhvatar state, the lightning (it stuns, nothing more),
     * Katara-guh's spirit water, Miroh holding them off, the flight up the Koningsput on Appa. 96 s (the big moment of the
     * chapter). fix3-h3 (playtest 2 item 8): {@link #VAL_EXTRA} ticks more in the middle of Zuka's choice, from tick
     * {@link #VAL_IN} on: Azula offers him his home back, Zuka wavers, Miroh asks him to be good without anyone's pride, Zuka
     * says sorry; everything after it (from "keuze" on) runs as before, only later ({@link #vt}).
     */
    private static final int VAL_IN = 470, VAL_EXTRA = 240;

    /** A tick of the first version of {@link #val}: from {@link #VAL_IN} on it comes {@link #VAL_EXTRA} ticks later. */
    private static int vt(int t) {
        return t < VAL_IN ? t : t + VAL_EXTRA;
    }

    private static Scene val() {
        String a = "bliksem_speler";
        Cam wijd = cam(a, "h3_val_wijd"), zuka = cam(a, "h3_val_zuka"), bliksem = cam(a, "h3_val_bliksem"), katara = cam(a, "h3_val_katara"),
                schacht = cam(a, "h3_val_schacht");
        Vec3 sp = op(a, a, 2, 0, 2), azula = op(a, "azula"), keuze = op(a, "zuka_keuze"), redt = op(a, "katara_redt"), miroh = op(a, a, 3, 0, 4),
                gevangen = op(a, "miroh_gevangen"), appaBoven = op(a, "val_appa_boven"), appaOnder = plus(appaBoven, 0, -22, 0),
                bazula = op(a, "bliksem_azula");
        Cutscene.Builder b = maak("val").duur(vt(1680)).verbergEcht(24)
                .speler(sp, 90f)
                .npc("katara", Kind.KATARA_GUH, plus(sp, 0, 0, 2), 90f)
                .npc("aang", Kind.AANG_GUH, plus(sp, 1.5, 0, -1), 90f)
                .npc("miroh", Kind.OOM_MIROH, miroh, 90f)
                .npc("zuka", Kind.ZUKA, plus(sp, -2, 0, 2.5), 90f)
                .npc("azula", Kind.AZULA_MIKA, plus(azula, -10, 0, 0), -90f)
                .acteur("appa", E2AppaFeature.APPA_GUH, appaBoven, 0f)
                .camera(0, wijd.oog(0), wijd.kijk()).camera(320, plus(wijd.oog(0), -1, -1, -1), wijd.kijk())
                .cameraKnipVolgt(324, zuka.oog(0), "zuka").camera(vt(516), plus(zuka.oog(0), 0.4, 0, 0.4), zuka.kijk())
                .cameraKnip(vt(520), wijd.oog(0), wijd.kijk()).camera(vt(756), plus(wijd.oog(0), -2, -1, -1), wijd.kijk())
                .cameraKnip(vt(760), bliksem.oog(0), bliksem.kijk()).camera(vt(950), plus(bliksem.oog(0), 0.5, 0.4, -0.5), bliksem.kijk())
                .cameraKnip(vt(1012), katara.oog(0), katara.kijk()).camera(vt(1240), plus(katara.oog(0), 0.4, 0, 0.4), katara.kijk())
                .cameraKnip(vt(1244), schacht.oog(0), schacht.kijk()).camera(vt(1420), plus(schacht.oog(0), 0.5, 0.5, 0.5), schacht.kijk())
                .camera(vt(1560), plus(appaOnder, -2.5, 24, -2.5), plus(appaOnder, 0, 34, 0))
                .camera(vt(1680), plus(appaOnder, -2.0, 30, -2.0), plus(appaOnder, 0, 40, 0))
                .zeg(10, "", "kristal", 64)
                .loop("azula", 60, 120, azula).animatie("azula", 80, "grijns").zeg(82, "azula", "reunie", 64)
                .animatie("azula", 158, "lok").zeg(160, "azula", "zuka", 74)
                .kijk("miroh", 244, plus(sp, -2, 0, 2.5)).animatie("miroh", 246, "wijsheid").zeg(250, "miroh", "weg", 84)
                .zeg(340, "", "stil", 62)
                .loop("zuka", 410, 470, keuze).kijk("zuka", vt(472), op(a, a, 2, 0, 2))
                .zeg(430, "miroh", "zuka_naam", 40)
                // (fix3-h3, playtest 2 item 8: the choice with more heart: what Azula offers him, what Miroh asks of him)
                .kijk("zuka", 470, azula).animatie("azula", 470, "lok").zeg(472, "azula", "thuis", 66)
                .animatie("zuka", 542, "zucht").zeg(546, "zuka", "eer", 50)
                .kijk("zuka", 600, miroh).animatie("miroh", 600, "wijsheid").zeg(604, "miroh", "trots", 72)
                .zeg(680, "zuka", "sorry", 36)
                .zeg(vt(480), "", "keuze", 52);
        double[][] dl = {{-5, -4}, {-5, 8}, {-10, 2}};
        for (int i = 0; i < dl.length; i++) {
            String n = "dai_li_" + (i + 1);
            Vec3 plek = plus(sp, dl[i][0], 0, dl[i][1]);
            b.npc(n, Kind.DAI_LI_MIKA, plus(plek, 0, -3, 0), -90f).loop(n, vt(540) + i * 6, vt(560) + i * 6, plek).animatie(n, vt(566), "stenen_hand");
        }
        b.schud(vt(540), 0.8f, 16).geluid(vt(540), () -> SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.5f)
                .animatie("aang", vt(560), "zweef").zeg(vt(562), "aang", "nu", 62)
                .loop("speler", vt(640), vt(700), plus(sp, 0, 3, 0))
                .deeltjes(vt(640), ParticleTypes.END_ROD, plus(sp, 0, 1.5, 0), 40, 1.0).deeltjes(vt(680), ParticleTypes.END_ROD, plus(sp, 0, 4, 0), 50, 1.4)
                .geluid(vt(640), () -> SoundEvents.BEACON_ACTIVATE, 1f, 1.2f)
                .zeg(vt(704), "", "staat", 62)
                .loop("azula", vt(760), vt(776), bazula).kijk("azula", vt(776), plus(sp, 0, 3, 0)).animatie("azula", vt(778), "bliksem").zeg(vt(780), "azula", "bliksem", 30)
                .geluid(vt(810), () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f, 1.3f).schud(vt(810), 2.6f, 18).zwart(vt(808), vt(812))
                .deeltjes(vt(810), ParticleTypes.ELECTRIC_SPARK, plus(sp, 0, 4, 0), 80, 1.2)
                .deeltjes(vt(810), ParticleTypes.ELECTRIC_SPARK, plus(bazula, 0, 1.5, 0).add(plus(sp, 0, 4, 0)).scale(0.5), 60, 2.5)
                .loop("speler", vt(820), vt(880), plus(redt, 0, 0, 0.6))
                .zeg(vt(900), "", "val", 62)
                .zwart(vt(960), vt(1010))
                .loop("katara", vt(958), vt(1010), plus(redt, 1, 0, 0.6)).kijk("katara", vt(1010), redt)
                .animatie("katara", vt(1018), "genees").zeg(vt(1020), "katara", "water", 76)
                .deeltjes(vt(1030), ParticleTypes.GLOW, plus(redt, 0, 0.6, 0.6), 30, 0.5).deeltjes(vt(1080), ParticleTypes.GLOW, plus(redt, 0, 0.6, 0.6), 30, 0.5)
                .zeg(vt(1100), "", "ogen", 56)
                .loop("miroh", vt(1150), vt(1180), plus(sp, -3, 0, 1)).kijk("miroh", vt(1180), azula)
                .deeltjes(vt(1176), ParticleTypes.FLAME, plus(sp, -4, 1, 1), 40, 1.2)
                .animatie("miroh", vt(1170), "wijsheid").zeg(vt(1172), "miroh", "ga", 66)
                .loop("appa", vt(1250), vt(1340), appaOnder).geluid(vt(1250), E2AppaFeature.BRUL::get, 1f, 1f)
                .animatie("aang", vt(1258), "lach").zeg(vt(1260), "aang", "appa", 44)
                .loop("speler", vt(1350), vt(1400), plus(appaOnder, 0, 0, 1.5)).loop("katara", vt(1350), vt(1404), plus(appaOnder, 1, 0, 1.5))
                .loop("aang", vt(1350), vt(1400), plus(appaOnder, -1, 0, 1.5))
                .loop("miroh", vt(1340), vt(1380), gevangen).kijk("miroh", vt(1382), appaOnder)
                .deeltjes(vt(1384), ParticleTypes.HAPPY_VILLAGER, plus(gevangen, 0, 1, 0), 40, 0.9)
                .zeg(vt(1380), "", "miroh", 70);
        for (String n : List.of("appa", "speler", "katara", "aang")) {
            Vec3 van = n.equals("appa") ? appaOnder : n.equals("speler") ? plus(appaOnder, 0, 0, 1.5)
                    : n.equals("katara") ? plus(appaOnder, 1, 0, 1.5) : plus(appaOnder, -1, 0, 1.5);
            b.loop(n, vt(1420), vt(1600), plus(van, 0, 34, 0));
        }
        return reg("val", b.geluid(vt(1420), E2AppaFeature.JIP_JIP::get, 1f, 1f).zeg(vt(1590), "", "gevallen", 84), a);
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

    // =====================================================================================================================
    // fix3-h3 (playtest 2 items 1, 8, 9, 10): the story between the steps. Every task gets its reason in a short scene with
    // the team (who, why, what is at stake); positions are template spots (t), cameras are free points that step out of a
    // wall by themselves (cameraOntwijkt) or the build's proven camera pads.
    // =====================================================================================================================

    /** A template spot (block x + fraction, feet y, block z + fraction) relative to the anchor marker. */
    private static Vec3 t(String anker, double x, double y, double z) {
        BlockPos a = m(anker);
        return new Vec3(x - a.getX(), y - a.getY(), z - a.getZ());
    }

    /**
     * After the gate (step 2, Aardrommel): in the passage of the outer gate. Aang needs an earth teacher; Sokka has a
     * flyer of the Aardrommel, where the strongest earth benders of the city fight. 20 s.
     */
    private static Scene leraar() {
        String a = "loket";
        Vec3 sp = t(a, 27.5, 46, 126.6), aang = t(a, 29.6, 46, 126.6), katara = t(a, 29.2, 46, 128.9), sokka = t(a, 30.6, 46, 128.0),
                momo = t(a, 29.5, 46, 125.7);
        Vec3 camA = t(a, 24.6, 48.6, 129.8), kijkA = t(a, 29.0, 47.2, 127.6), camS = t(a, 27.4, 48.0, 130.4), camC = t(a, 25.5, 49.0, 128.5),
                stadKijk = t(a, 46.5, 50.0, 128.5);
        return reg("leraar", maak("leraar").duur(400).verbergEcht(8).cameraOntwijkt()
                .speler(sp, -90f)
                .npc("loketguh", Kind.LOKETGUH, op(a, "loketguh"), 0f)
                .npc("aang", Kind.AANG_GUH, aang, 90f)
                .npc("katara", Kind.KATARA_GUH, katara, 120f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, 90f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, momo, 90f)
                .camera(0, camA, kijkA).camera(130, plus(camA, 0.4, 0, -0.3), kijkA)
                .cameraKnipVolgt(134, camS, "sokka").camera(300, plus(camS, 0.3, 0, -0.2), kijkA)
                .cameraKnip(316, camC, stadKijk).camera(400, plus(camC, 1.5, 0, 0), stadKijk)
                .animatie("aang", 8, "lach").zeg(10, "aang", "binnen", 60)
                .kijk("katara", 70, aang).zeg(74, "katara", "geduld", 58)
                .animatie("sokka", 136, "plan").zeg(138, "sokka", "affiche", 72)
                .animatie("aang", 212, "lach").zeg(214, "aang", "kampioen", 48)
                .animatie("sokka", 264, "honger").zeg(266, "sokka", "pauze", 44)
                .geluid(318, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.25f, 0.6f).schud(318, 0.4f, 10)
                .zeg(322, "", "arena", 60)
                .loop("aang", 330, 400, plus(aang, 7, 0, 1.5)).loop("katara", 334, 400, plus(katara, 7, 0, -0.5))
                .loop("sokka", 330, 400, plus(sokka, 6, 0, 0.3)).loop("momo", 326, 380, plus(momo, 8, 0, 2)), a);
    }

    /**
     * After the lesson (step 4, monorail): in the ring of the Aardrommel. Earth bending learnt; now the Aardekoning must hear
     * of the Fire Nation, but nobody reaches him. Toph sends you up with the monorail to wait in the middle ring. 24 s.
     */
    private static Scene middenring() {
        String a = "toph";
        Cam laag = cam(a, "h3_aardrommel_toph");
        Vec3 toph = op(a, a), sp = t(a, 88.5, 42, 205.4), aang = t(a, 90.4, 42, 205.6), katara = t(a, 92.0, 42, 204.6), sokka = t(a, 91.6, 42, 206.6),
                momo = t(a, 89.6, 42, 206.4), midden = t(a, 90.6, 43, 204.4), zuid = t(a, 89.6, 43.6, 208.4);
        return reg("middenring", maak("middenring").duur(470).verbergEcht(10).cameraOntwijkt()
                .npc("toph", Kind.TOPH_GUH, toph, 0f)
                .speler(sp, 180f)
                .npc("aang", Kind.AANG_GUH, aang, 180f)
                .npc("katara", Kind.KATARA_GUH, katara, 170f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, 190f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, momo, 180f)
                .camera(0, laag.oog(0), midden).camera(196, plus(laag.oog(0), -0.4, 0.2, -0.3), midden)
                .cameraKnipVolgt(200, zuid, "toph").camera(330, plus(zuid, 0.6, 0, 0), toph.add(0, 1, 0))
                .cameraKnip(334, laag.oog(0), midden).camera(470, plus(laag.oog(0), -0.6, 0.3, -0.4), midden)
                .animatie("toph", 8, "lach").zeg(10, "toph", "kiezel", 56)
                .zeg(74, "katara", "koning", 62)
                .animatie("toph", 138, "wijs").zeg(140, "toph", "muren", 64)
                .animatie("sokka", 208, "plan").zeg(210, "sokka", "afspraak", 50)
                .animatie("toph", 266, "stamp").schud(272, 0.6f, 8).zeg(270, "toph", "monorail", 74)
                .zeg(350, "aang", "mee", 42)
                .animatie("toph", 398, "lach").zeg(400, "toph", "titel", 62), a);
    }

    /**
     * After Pai Sho (step 8, Joo Dee): in De Jasmijn Draak. Miroh warns, softly, that in this city even the cups have ears:
     * the Dai Li keep the peace a little too well. Katara: our guide Joo Dee-guh can take us to the king. 22 s.
     */
    private static Scene uitnodiging() {
        String a = "miroh";
        Cam binnen = cam(a, "h3_thee_binnen"), zuka = cam(a, "h3_thee_zuka");
        Vec3 sp = op(a, a, 0, 0, -5.5), aang = plus(sp, 1.2, 0, 0.6), katara = plus(sp, -1.2, 0, 0.4), sokka = plus(sp, 0.6, 0, -1.0);
        return reg("uitnodiging", maak("uitnodiging").duur(440).verbergEcht(12)
                .npc("miroh", Kind.OOM_MIROH, op(a, a), 180f)
                .npc("zuka", Kind.ZUKA, op(a, "zuka_ober"), 90f)
                .npc("klant_1", Kind.BA_SING_GUH_BURGER, op(a, "klant_1"), 180f)
                .npc("klant_2", Kind.BA_SING_GUH_BURGER, op(a, "klant_2"), 180f)
                .speler(sp, 0f)
                .npc("aang", Kind.AANG_GUH, aang, 0f)
                .npc("katara", Kind.KATARA_GUH, katara, 0f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, 0f)
                .camera(0, binnen.oog(0), binnen.kijk()).camera(336, plus(binnen.oog(0), -0.6, 0, 0.6), binnen.kijk())
                .cameraKnipVolgt(340, zuka.oog(0), "zuka").camera(440, plus(zuka.oog(0), 0.4, 0, 0.3), zuka.kijk())
                .animatie("miroh", 8, "thee").zeg(10, "miroh", "oren", 70)
                .zeg(84, "sokka", "wie", 40)
                .animatie("miroh", 128, "wijsheid").zeg(130, "miroh", "dai_li", 76)
                .zeg(210, "katara", "gids", 64)
                .animatie("miroh", 278, "praat").zeg(280, "miroh", "luister", 56)
                .kijk("zuka", 330, sp).animatie("zuka", 344, "zucht").zeg(346, "", "zuka", 70), a);
    }

    /**
     * After Joo Dee (step 9, the camp): in front of her house. She smiles and smiles; two Dai Li-Mika's stand by the wall and
     * look away. Appa and Momo are alone at the camp. 18 s.
     */
    private static Scene gevolgd() {
        String a = "joo_dee";
        Vec3 jd = op(a, a), sp = op(a, a, 0, 0, 3), aang = op(a, a, 1.6, 0, 3.6), katara = op(a, a, -1.4, 0, 3.8), sokka = op(a, a, 0.8, 0, 5.0),
                dl1 = op(a, "dai_li_4"), dl2 = op(a, "dai_li_4", -0.6, 0, 1.8);
        Vec3 camA = op(a, a, 3.4, 2.4, 7.6), kijkA = op(a, a, 0, 1, 2.6), camD = op(a, a, 1.6, 2.0, 6.0);
        return reg("gevolgd", maak("gevolgd").duur(360).verbergEcht(12).cameraOntwijkt()
                .npc("joo_dee", Kind.JOO_DEE_GUH, jd, 0f)
                .npc("dai_li_1", Kind.DAI_LI_MIKA, dl1, 0f)
                .npc("dai_li_2", Kind.DAI_LI_MIKA, dl2, 0f)
                .speler(sp, 180f)
                .npc("aang", Kind.AANG_GUH, aang, 160f)
                .npc("katara", Kind.KATARA_GUH, katara, 200f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, 180f)
                .camera(0, camA, kijkA).camera(136, plus(camA, -0.5, 0, 0.3), kijkA)
                .cameraKnipVolgt(140, camD, "dai_li_1").camera(212, plus(camD, -0.3, 0, 0.2), dl1.add(0, 1, 0))
                .cameraKnip(216, camA, kijkA).camera(360, plus(camA, 0.6, 0.2, 0.4), kijkA)
                .animatie("joo_dee", 6, "glimlach").zeg(10, "", "glimlach", 60)
                .animatie("sokka", 72, "plan").zeg(74, "sokka", "raar", 60)
                .kijk("dai_li_1", 140, sp).kijk("dai_li_2", 140, sp)
                .kijk("dai_li_1", 170, plus(dl1, -3, 0, -2)).kijk("dai_li_2", 172, plus(dl2, -3, 0, 2))
                .animatie("katara", 148, "armen").zeg(150, "katara", "miroh", 58)
                .zeg(220, "aang", "appa", 60)
                .animatie("sokka", 286, "boemerang").zeg(290, "sokka", "kamp", 50), a);
    }

    /**
     * After "Appa's verloren dagen" (step 10, the king): the empty camp. Whose stone hands were these? The Dai Li serve the
     * city, so the king. No more forms: to the palace. 22 s.
     */
    private static Scene naAppa() {
        String a = "appa_kamp";
        Cam leeg = cam(a, "h3_appa_leeg"), kamp = cam(a, "h3_appa_kamp");
        Vec3 appa = op(a, a), sp = op(a, a, -1.6, 0, -1.6), aang = op(a, a, -0.4, 0, -0.6), katara = op(a, a, -2.4, 0, 0.4), sokka = op(a, a, 1.2, 0, -2.2),
                momo = op(a, a, -1.0, 0, -2.6);
        return reg("na_appa", maak("na_appa").duur(440).verbergEcht(10)
                .speler(sp, 45f)
                .npc("aang", Kind.AANG_GUH, aang, 0f)
                .npc("katara", Kind.KATARA_GUH, katara, 90f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, -30f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, momo, 0f)
                .camera(0, leeg.oog(0), leeg.kijk()).camera(212, plus(leeg.oog(0), 0.4, 0.1, 0.3), leeg.kijk())
                .cameraKnip(216, kamp.oog(0), kamp.kijk()).camera(440, plus(kamp.oog(0), 1.0, -0.4, 1.0), kamp.kijk())
                .animatie("aang", 6, "buk").zeg(10, "aang", "schuld", 60)
                .animatie("katara", 70, "armen").zeg(76, "katara", "stenen", 66)
                .deeltjes(80, ParticleTypes.WHITE_ASH, plus(appa, 0, 0.3, 0), 14, 0.8)
                .animatie("aang", 140, "sta").animatie("sokka", 148, "plan").zeg(150, "sokka", "dai_li", 62)
                .zeg(220, "katara", "koning", 64)
                .animatie("sokka", 288, "boemerang").zeg(290, "sokka", "paleis", 60)
                .loop("momo", 350, 362, plus(sp, 0.3, 1.6, 0)).geluid(352, E2AppaFeature.MOMO::get, 0.7f, 1.1f)
                .zeg(364, "", "momo", 66), a);
    }

    /**
     * After the reunion (step 12, Miroh): at Appa's bent bars. Long Feng dropped a letter: "A" is coming for the banished
     * prince Zuka and his uncle, who hide in De Jasmijn Draak. The waiter was Zuka, the prince who chases us since the Air
     * Temple; the round guh with the tea was on his ship. But Miroh was kind to us: we warn him, at the hill. 28 s.
     */
    private static Scene waaromMiroh() {
        String a = "appa_poort";
        Cam c = cam(a, "h3_laogai"), dicht = cam(a, "h3_laogai_long_feng"), kamer = cam(a, "h3_weerzien");
        Vec3 sp = op(a, a, 0, 0, -1.2), team = op(a, "laogai_team"), lf = op(a, "laogai_long_feng"), aang = plus(team, -1, 0, 3), katara = plus(team, 1, 0, 3),
                sokka = plus(team, 0, 0, 2);
        return reg("waarom_miroh", maak("waarom_miroh").duur(560).verbergEcht(12)
                .speler(sp, 0f)
                .acteur("appa", E2AppaFeature.APPA_GUH, op(a, a, 0, 0, 2.0), 180f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, plus(sp, 1.2, 0, -0.4), 0f)
                .npc("aang", Kind.AANG_GUH, aang, 0f)
                .npc("katara", Kind.KATARA_GUH, katara, 0f)
                .npc("sokka", Kind.SOKKA_GUH, sokka, 0f)
                .camera(0, c.oog(0), c.kijk()).camera(36, plus(c.oog(0), -0.3, 0, 0.2), c.kijk())
                .cameraKnipVolgt(40, dicht.oog(0), "sokka").camera(196, plus(dicht.oog(0), 0.3, 0, 0.3), dicht.kijk())
                .cameraKnip(200, c.oog(0), c.kijk()).camera(400, plus(c.oog(0), -0.6, 0.1, 0.4), c.kijk())
                .cameraKnip(404, kamer.oog(0), kamer.kijk()).camera(560, plus(kamer.oog(0), 0.5, 0, 0.5), kamer.kijk())
                .deeltjes(6, ParticleTypes.END_ROD, plus(lf, 0, 0.3, 0), 6, 0.2)
                .zeg(10, "", "brief", 50)
                .loop("sokka", 40, 80, plus(lf, 0.6, 0, 0)).animatie("sokka", 82, "buk").animatie("sokka", 100, "sta")
                .kijk("sokka", 102, plus(lf, -1, 1, 0)).zeg(104, "sokka", "lees", 90)
                .loop("sokka", 196, 230, plus(team, 0, 0, 4.5)).kijk("sokka", 232, sp)
                .animatie("katara", 198, "armen").zeg(200, "katara", "zuka", 66)
                .animatie("sokka", 268, "plan").zeg(270, "sokka", "oom", 62)
                .zeg(340, "aang", "aardig", 64)
                .zeg(410, "katara", "heuvel", 66)
                .animatie("aang", 482, "lach").zeg(484, "aang", "waarschuwen", 62)
                .geluid(500, E2AppaFeature.BRUL::get, 0.6f, 1.2f), a);
    }

    /**
     * After Miroh's song (step 13, the catacombs): on the hill. Sokka runs up: Katara went to warn Zuka, and the Dai Li took
     * them both, deep under the palace. Miroh tells who Zuka is (sent away by his father, he hunts a home, not you) and goes
     * with you; Sokka will keep Appa ready. 30 s.
     */
    private static Scene kataraWeg() {
        String a = "lu_ten_boom";
        Vec3 miroh = op(a, "lu_ten_miroh"), sp = op(a, "lu_ten_speler"), sokkaStart = t(a, 17.5, 46, 116.5), sokka = t(a, 16.5, 48, 109.5),
                aang = t(a, 14.5, 48, 111.5);
        Vec3 camA = t(a, 10.5, 52.0, 113.5), kijkA = t(a, 14.2, 49.4, 110.0);
        Cam dicht = cam(a, "lu_ten_dicht");
        return reg("katara_weg", maak("katara_weg").duur(600).verbergEcht(10).cameraOntwijkt()
                .npc("miroh", Kind.OOM_MIROH, miroh, 90f)
                .speler(sp, 135f)
                .npc("aang", Kind.AANG_GUH, aang, 160f)
                .npc("sokka", Kind.SOKKA_GUH, sokkaStart, 180f)
                .camera(0, camA, kijkA).camera(150, plus(camA, 0.4, 0, -0.3), kijkA)
                .cameraKnipVolgt(154, dicht.oog(0), "miroh").camera(470, plus(dicht.oog(0), -0.4, 0.2, 0.3), dicht.kijk())
                .cameraKnip(474, camA, kijkA).camera(600, plus(camA, -0.5, 0.3, 0.4), kijkA)
                .zeg(10, "", "rennen", 50)
                .loop("sokka", 10, 70, sokka).kijk("sokka", 72, sp)
                .animatie("sokka", 76, "plan").zeg(78, "sokka", "katara", 72)
                .kijk("miroh", 150, sokka).zeg(156, "miroh", "neefje", 40)
                .zeg(200, "aang", "waarom", 56)
                .kijk("miroh", 254, sp).animatie("miroh", 256, "wijsheid").zeg(258, "miroh", "vader", 84)
                .animatie("miroh", 346, "praat").zeg(348, "miroh", "thuis", 64)
                .zeg(416, "miroh", "azula", 72)
                .animatie("sokka", 490, "boemerang").zeg(492, "sokka", "appa", 60)
                .loop("sokka", 556, 600, sokkaStart), a);
    }

    /**
     * In the catacombs (step 13), before the fall: at the crystal cell. You hear Katara and Zuka talking in the dark: his
     * scar, his father, the home he hunts. Miroh melts the crystal: his nephew sees his uncle at the Guhvatar's side. That
     * is why Zuka stands with you when the crystal chamber lights up. 38 s.
     */
    private static Scene kristalcel() {
        String a = "kristalcel";
        Vec3 katara = t(a, 121.6, 17, 139.2), zuka = t(a, 123.6, 17, 137.4), sp = t(a, 128.5, 17, 138.5), miroh = t(a, 128.6, 17, 137.1),
                aang = t(a, 128.4, 17, 139.9), opening = t(a, 125.7, 17, 138.5);
        Vec3 camGang = t(a, 128.6, 19.3, 143.4), kijkGang = t(a, 126.0, 18.0, 138.5), camCel = t(a, 124.5, 19.4, 136.5), kijkCel = t(a, 122.6, 17.9, 138.3),
                camUit = t(a, 121.5, 19.4, 137.5), kijkUit = t(a, 126.2, 18.0, 138.5);
        return reg("kristalcel", maak("kristalcel").duur(760).verbergEcht(8).cameraOntwijkt()
                .npc("katara", Kind.KATARA_GUH, katara, -60f)
                .npc("zuka", Kind.ZUKA, zuka, 160f)
                .speler(sp, 90f)
                .npc("miroh", Kind.OOM_MIROH, miroh, 90f)
                .npc("aang", Kind.AANG_GUH, aang, 90f)
                .camera(0, camGang, kijkGang).camera(66, plus(camGang, 0, -0.2, -0.6), kijkGang)
                .cameraKnip(70, camCel, kijkCel).camera(426, plus(camCel, -0.3, -0.1, 0.3), kijkCel)
                .cameraKnip(430, camUit, kijkUit).camera(664, plus(camUit, 0.4, 0, 0.2), kijkUit)
                .cameraKnip(668, camGang, kijkGang).camera(760, plus(camGang, 0, 0, -0.8), kijkGang)
                .zeg(10, "", "stemmen", 56)
                .kijk("katara", 70, zuka).zeg(74, "katara", "oor", 66)
                .animatie("zuka", 144, "zucht").zeg(146, "zuka", "vader", 70)
                .zeg(222, "zuka", "thuis", 70)
                .animatie("katara", 296, "genees").zeg(298, "katara", "water", 66)
                .kijk("zuka", 366, katara).zeg(370, "zuka", "dank", 52)
                .loop("miroh", 424, 452, opening)
                .deeltjes(454, ParticleTypes.FLAME, plus(opening, -0.6, 1.0, 0), 30, 0.5).geluid(454, () -> SoundEvents.FIRECHARGE_USE, 0.8f, 1.0f)
                .deeltjes(462, ParticleTypes.HAPPY_VILLAGER, plus(opening, -0.6, 1.0, 0), 20, 0.6)
                .animatie("miroh", 456, "lach").zeg(458, "miroh", "neefje", 66)
                .kijk("zuka", 520, opening).loop("zuka", 520, 548, plus(opening, -1.4, 0, -0.4)).zeg(530, "zuka", "oom", 50)
                .animatie("miroh", 592, "wijsheid").zeg(594, "miroh", "harten", 68)
                .schud(668, 0.7f, 14).geluid(668, () -> SoundEvents.ROOTED_DIRT_BREAK, 0.8f, 0.5f)
                .zeg(672, "", "kamer", 60)
                .zwart(740, 760), a);
    }

    /**
     * After the flight up the Koningsput (finale of step 13): deep under the city, Oom Miroh in his cage of crystal. Azula
     * sends him to the Fire Nation; Zuka steps forward, and for the first time his uncle has no wise word for him. 32 s.
     */
    private static Scene mirohGevangen() {
        String a = "miroh_gevangen";
        Vec3 miroh = op(a, a), azula = t(a, 146.5, 17, 124.6), zuka = t(a, 148.6, 17, 125.6), zukaVoor = t(a, 147.6, 17, 122.4),
                dl1 = t(a, 143.6, 17, 121.6), dl2 = t(a, 149.6, 17, 121.2), dl3 = t(a, 145.0, 17, 126.4), weg = t(a, 158.5, 17, 121.5);
        Vec3 camWijd = t(a, 146.5, 19.6, 128.0), kijkWijd = t(a, 146.5, 18.0, 120.5), camMiroh = t(a, 144.6, 18.6, 122.6), camZuka = t(a, 150.6, 18.8, 123.2);
        DustParticleOptions kristal = new DustParticleOptions(0x6BE07A, 1.4f);
        Cutscene.Builder b = maak("miroh_gevangen").duur(640).verbergEcht(12).cameraOntwijkt()
                .npc("miroh", Kind.OOM_MIROH, miroh, 0f)
                .npc("azula", Kind.AZULA_MIKA, azula, 180f)
                .npc("zuka", Kind.ZUKA, zuka, 180f)
                .npc("dai_li_1", Kind.DAI_LI_MIKA, dl1, -120f)
                .npc("dai_li_2", Kind.DAI_LI_MIKA, dl2, 120f)
                .npc("dai_li_3", Kind.DAI_LI_MIKA, dl3, 180f)
                .zwart(0, 24)
                .camera(0, camWijd, kijkWijd).camera(142, plus(camWijd, 0, -0.3, -0.8), kijkWijd)
                .cameraKnipVolgt(146, camMiroh, "miroh").camera(288, plus(camMiroh, 0.3, 0, -0.2), miroh.add(0, 1.2, 0))
                .cameraKnipVolgt(292, camZuka, "zuka").camera(450, plus(camZuka, 0.3, 0, 0.3), zukaVoor.add(0, 1.2, 0))
                .cameraKnip(454, camWijd, kijkWijd).camera(640, plus(camWijd, 0, 0.6, 1.2), kijkWijd)
                .zeg(12, "", "stilte", 56)
                .animatie("dai_li_1", 64, "stenen_hand").animatie("dai_li_2", 66, "stenen_hand")
                .deeltjes(70, kristal, plus(miroh, 0, 1.0, 0), 50, 0.9).geluid(70, () -> SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 0.7f)
                .animatie("azula", 78, "grijns").zeg(80, "azula", "kooi", 62)
                .animatie("miroh", 148, "wijsheid").zeg(150, "miroh", "thee", 70)
                .animatie("azula", 224, "praat").zeg(226, "azula", "vader", 62)
                .loop("zuka", 292, 322, zukaVoor).kijk("zuka", 324, miroh).zeg(326, "zuka", "oom", 40)
                .kijk("miroh", 368, zukaVoor).zeg(372, "", "blik", 80)
                .animatie("zuka", 454, "zucht").kijk("azula", 456, zukaVoor).zeg(458, "azula", "huis", 56);
        b.loop("dai_li_1", 530, 620, plus(weg, -1, 0, -1)).loop("dai_li_2", 530, 620, plus(weg, -1, 0, 1)).loop("miroh", 536, 626, weg)
                .deeltjes(536, kristal, plus(miroh, 0, 1.0, 0), 30, 0.8).zwart(610, 640);
        return reg("miroh_gevangen", b, a);
    }


    /** Where each rider sits on Appa's back (from his feet). */
    private static Vec3 rug(String wie) {
        return switch (wie) {
            case "speler" -> new Vec3(0.4, 2.6, 0.6);
            case "aang" -> new Vec3(-0.7, 2.6, -0.6);
            case "katara" -> new Vec3(0.7, 2.6, -0.5);
            case "sokka" -> new Vec3(0, 2.6, -1.7);
            case "momo" -> new Vec3(0.5, 2.9, 1.4);
            default -> new Vec3(-0.6, 2.6, 1.1);   // toph
        };
    }

    private static Vec3 vlucht(String anker, int i) {
        return t(anker, VLUCHT[i][0], VLUCHT[i][1], VLUCHT[i][2]);
    }

    /** Appa and his riders fly to waypoint naar between t0 and t1. */
    private static void vlieg(Cutscene.Builder b, String anker, List<String> rijders, int naar, int t0, int t1) {
        b.loop("appa", t0, t1, vlucht(anker, naar));
        for (String r : rijders) {
            b.loop(r, t0, t1, vlucht(anker, naar).add(rug(r)));
        }
    }

    /**
     * The escape (step 14, playtest 2 item 9): Appa shoots out of the city, over the outer wall, low over the camp; there
     * Toph-guh stamps, a pillar of rock throws her onto Appa's back, and she says she will help you, and the Guhvatar.
     * 25 s; {@link #KOERS} follows at once.
     */
    private static Scene ontsnapping() {
        String a = "appa_kamp";
        List<String> rijders = List.of("speler", "aang", "katara", "sokka", "momo");
        Vec3 w0 = vlucht(a, 0), w2 = vlucht(a, 2), toph = op(a, "team_toph");
        Vec3 camA = t(a, 4.5, 50.5, 157.5), camB = t(a, 8.5, 48.2, 152.5), camC = plus(w2, -5.5, 3.0, 5.0);
        Cutscene.Builder b = maak("ontsnapping").duur(500).verbergEcht(16).cameraOntwijkt()
                .acteur("appa", E2AppaFeature.APPA_GUH, w0, 90f)
                .speler(w0.add(rug("speler")), 90f)
                .npc("aang", Kind.AANG_GUH, w0.add(rug("aang")), 90f)
                .npc("katara", Kind.KATARA_GUH, w0.add(rug("katara")), 90f)
                .npc("sokka", Kind.SOKKA_GUH, w0.add(rug("sokka")), 90f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, w0.add(rug("momo")), 90f)
                .npc("toph", Kind.TOPH_GUH, toph, -160f)
                .zwart(0, 24)
                .cameraVolgt(0, camA, "appa").cameraVolgt(196, plus(camA, 0.5, -0.3, 0), "appa")
                .cameraKnipVolgt(204, camB, "toph").cameraVolgt(286, plus(camB, 0.4, 0.6, 0), "toph")
                .cameraKnipVolgt(290, camC, "appa").cameraVolgt(500, plus(camC, 0.5, 0.3, -0.4), "appa")
                .geluid(4, E2AppaFeature.BRUL::get, 1f, 1f)
                .zeg(10, "", "vlucht", 70);
        vlieg(b, a, rijders, 1, 0, 120);
        vlieg(b, a, rijders, 2, 120, 200);
        Vec3 tophOp = w2.add(rug("toph"));
        b.kijk("toph", 202, w2.add(0, 2, 0)).zeg(206, "toph", "wacht", 50)
                .animatie("toph", 260, "stamp").schud(266, 1.4f, 12).geluid(266, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.3f)
                .deeltjes(266, ParticleTypes.CLOUD, toph.add(0, 0.3, 0), 30, 0.9)
                .deeltjes(270, ParticleTypes.CLOUD, toph.lerp(tophOp, 0.5), 20, 0.6)
                .loop("toph", 268, 286, tophOp).kijk("toph", 288, w2.add(0, 2.6, -3))
                .animatie("toph", 292, "lach").zeg(294, "toph", "hier", 58)
                .kijk("katara", 350, tophOp).zeg(356, "katara", "vliegen", 40)
                .animatie("toph", 398, "wijs").zeg(400, "toph", "help", 84);
        return reg("ontsnapping", b, a);
    }

    /**
     * In flight, right after {@link #ONTSNAPPING}: where now? Ba Sing Guh fell, Azula takes Miroh to the Fire Nation, and
     * the Guhvatar still has to learn fire: so the Fire Nation it is, in disguise (maybe even to school). Into chapter 4.
     * 26 s.
     */
    private static Scene koers() {
        String a = "appa_kamp";
        List<String> rijders = List.of("speler", "aang", "katara", "sokka", "momo", "toph");
        Vec3 w2 = vlucht(a, 2), w3 = vlucht(a, 3), w4 = vlucht(a, 4);
        Vec3 naast = new Vec3(-5.5, 3.0, 5.0);
        Cutscene.Builder b = maak("koers").duur(520).verbergEcht(16).cameraOntwijkt()
                .acteur("appa", E2AppaFeature.APPA_GUH, w2, 90f)
                .speler(w2.add(rug("speler")), 90f)
                .npc("aang", Kind.AANG_GUH, w2.add(rug("aang")), 90f)
                .npc("katara", Kind.KATARA_GUH, w2.add(rug("katara")), 90f)
                .npc("sokka", Kind.SOKKA_GUH, w2.add(rug("sokka")), 90f)
                .acteur("momo", E2AppaFeature.MOMO_GUH, w2.add(rug("momo")), 90f)
                .npc("toph", Kind.TOPH_GUH, w2.add(rug("toph")), 90f)
                .cameraVolgt(0, w2.add(naast), "appa").cameraVolgt(140, w3.add(naast), "appa").cameraVolgt(520, w4.add(naast), "appa")
                .geluid(2, E2AppaFeature.JIP_JIP::get, 1f, 1f);
        vlieg(b, a, rijders, 3, 0, 140);
        vlieg(b, a, rijders, 4, 140, 520);
        b.animatie("sokka", 8, "plan").zeg(10, "sokka", "waarheen", 66)
                .zeg(80, "aang", "miroh", 60)
                .zeg(146, "katara", "vuur", 66)
                .animatie("sokka", 214, "plan").zeg(216, "sokka", "vermomming", 84)
                .animatie("toph", 304, "stamp").zeg(306, "toph", "snor", 52)
                .animatie("aang", 362, "lach").zeg(364, "aang", "school", 50)
                .zeg(420, "", "west", 84);
        return reg("koers", b, a);
    }
}
