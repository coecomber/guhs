package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import org.slf4j.Logger;

/**
 * einde2 "Sponsguh", slice sp_d1_recept: the camera scenes of part 1 steps 7-10 (CONTRACT_E2_C 5), ids
 * {@code einde2_sp_d1_recept_<naam>}. Every position comes from the sidecar of Bikinibodem ({@link Plekken}): the actors stand
 * on the build's markers, the cameras are the pads {@code d1r_<scene>_camera} this slice added to the build
 * (tools/features/einde2_sp_d1_recept_plekken.py), whose self-check proved every line free. Positions are relative to each
 * scene's anchor marker. Texts: tools/features/einde2_sp_d1_recept.py (scene.guhs.einde2_sp_d1_recept_*).
 * <p>
 * Running times without the click waits (CONTRACT_E2_C 5.2): <b>plan_z 70 s</b> (the whole theft: the empty safe, the flashback
 * to Plan Z with the required line, the night, back to the morning and the plan), knipoog_kloon 10 s, sluip_uitleg 18 s,
 * recept 21 s, feest 33 s, <b>zoete_zege 48 s + the song</b> (its length is {@link SpAudio#lied}'s: about 85 s with a one-minute
 * song). The look scenes of the hints ({@code kijk_*}) are 3 s each.
 */
final class D1rScenes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String S = SpVerhaal.BIKINIBODEM;
    static final String ID = "einde2_sp_d1_recept_";
    /** The narrator card of part 1 (sp_d1_start's): the picture behind the replays of these scenes. */
    static final String KAART = "einde2_sp_d1_start";

    /** A scene and the marker it is anchored on. */
    record Scene(Cutscene cutscene, String anker) {
        String id() {
            return cutscene.id();
        }
    }

    /** Every story scene of the slice by its short name (the dev command {@code d1recept scene <naam>}). */
    static final Map<String, Scene> ALLE = new LinkedHashMap<>();

    static final Scene PLAN_Z = planZ();
    static final Scene KNIPOOG_KLOON = knipoogKloon();
    static final Scene SLUIP_UITLEG = sluipUitleg();
    static final Scene RECEPT = recept();
    static final Scene FEEST = feest();
    static final Scene ZOETE_ZEGE = zoeteZege();
    /** The look scenes of the step hints (StapHint, ~3 s, one run-on caption). */
    static final Cutscene KIJK_KANTOOR = StapHint.kijkScene(ID + "kijk_kantoor", true);
    static final Cutscene KIJK_VETEMMER = StapHint.kijkScene(ID + "kijk_vetemmer", true);
    static final Cutscene KIJK_TERUG = StapHint.kijkScene(ID + "kijk_terug", true);
    static final Cutscene KIJK_PODIUM = StapHint.kijkScene(ID + "kijk_podium", true);

    private D1rScenes() {
    }

    /** (from E2SpD1Recept.register) loads the class: every scene above is registered. */
    static void registreer() {
        LOGGER.debug("einde2 sp_d1_recept: {} scenes", ALLE.size());
    }

    // =====================================================================================================================
    // playing
    // =====================================================================================================================

    /** Plays a scene for this player at the guaranteed copy of Bikinibodem; false: it could not start (nothing happens). */
    static boolean speel(ServerPlayer p, Scene s, @Nullable Consumer<ServerPlayer> daarna) {
        ServerLevel g = E2SpD1Recept.guheinde(p);
        BlockPos anker = g == null ? null : E2SpD1Recept.blok(g, s.anker());
        return anker != null && Cutscenes.speel(p, s.cutscene(), anker, Rotation.NONE, daarna);
    }

    /** The same with a big moment's music under it (SpAudio): it fades when the scene is over, also when it is skipped. */
    static boolean speelMetMuziek(ServerPlayer p, Scene s, String moment, @Nullable Consumer<ServerPlayer> daarna) {
        SpAudio.moment(p, moment);
        boolean ok = speel(p, s, speler -> {
            SpAudio.stopMoment(speler, 40);
            if (daarna != null) {
                daarna.accept(speler);
            }
        });
        if (!ok) {
            SpAudio.stopMoment(p, 10);
        }
        return ok;
    }

    // =====================================================================================================================
    // positions out of the sidecar
    // =====================================================================================================================

    private static BlockPos m(String id) {
        Plekken.Plek p = Plekken.plek(S, id);
        if (p == null) {
            LOGGER.warn("einde2 sp_d1_recept: the sidecar of {} has no marker {} (scene positions fall back to the anchor)", S, id);
            return BlockPos.ZERO;
        }
        return p.pos();
    }

    /** Where an actor stands on marker id (the middle of its block, on its floor) plus an offset, relative to the anchor. */
    private static Vec3 op(String anker, String id, double dx, double dy, double dz) {
        BlockPos a = m(anker), b = m(id);
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    private static Vec3 op(String anker, String id) {
        return op(anker, id, 0, 0, 0);
    }

    /** The middle of a template block, relative to the anchor. */
    private static Vec3 blok(String anker, BlockPos t) {
        BlockPos a = m(anker);
        return new Vec3(t.getX() - a.getX() + 0.5, t.getY() - a.getY() + 0.5, t.getZ() - a.getZ() + 0.5);
    }

    /** A camera pad of the build: its eye points and (last) what it looks at, relative to the anchor. */
    private record Cam(List<Vec3> ogen, Vec3 kijk) {
        Vec3 oog() {
            return ogen.get(0);
        }

        /** The eye moved a little towards what it looks at (a slow dolly): f = blocks. */
        Vec3 dichter(double f) {
            Vec3 d = kijk.subtract(oog());
            return oog().add(d.normalize().scale(Math.min(f, d.length() - 1.5)));
        }
    }

    private static Cam cam(String anker, String scene) {
        Plekken.Plek p = Plekken.plek(S, scene + "_camera");
        if (p == null || p.punten().size() < 2) {
            LOGGER.warn("einde2 sp_d1_recept: no camera {}_camera in the sidecar of {}", scene, S);
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

    /** Somewhere nobody sees: an actor that is not in the shot yet waits here (under the floor). */
    private static Vec3 weg(Vec3 v) {
        return v.add(0, -6, 0);
    }

    private static Cutscene.Builder maak(String naam) {
        return Cutscene.maak(ID + naam).bij(SpVerhaal.RECEPT).kaart(KAART);
    }

    private static Scene reg(String naam, Cutscene.Builder b, String anker) {
        Scene s = new Scene(b.registreer(), anker);
        ALLE.put(naam, s);
        return s;
    }

    // =====================================================================================================================
    // the scenes
    // =====================================================================================================================

    /**
     * Plan Z (big, the required line): Krabguh at his empty safe; the narrator spools back to last night, Plankmika finds Plan Z
     * in his lab ("Bedankt Karen, mijn computervrouw."), the theft in the night; back in the morning: "PLANKMIKA!", Octo hopes
     * for a day off, and Krabguh sends the new one into De Vetemmer. 70 s.
     */
    private static Scene planZ() {
        String a = "vetemmer_ingang";
        Cam kantoor = cam(a, "d1r_kantoor"), kantoor2 = cam(a, "d1r_kantoor_2"), lab = cam(a, "d1r_lab"), straat = cam(a, "d1r_straat");
        Vec3 krab = op(a, "kluis", -1, 0, 2), spons = op(a, "krabguh", 0.5, 0, 2.4), sponsIn = op(a, "krabguh", 0.5, 0, 1.2);
        Vec3 speler = op(a, "krabguh", -1.5, 0, -0.6), octo = op(a, "krabguh", 2.1, 0, 1.1);
        Vec3 plank = op(a, "plan_z_plankmika"), kast = op(a, "plan_z_kast", 0, 0.8, 0), karen = op(a, "karen", 0, 0.8, 0);
        Vec3 buiten = op(a, a, 0, 0, -0.6), kkDeur = op(a, "d1r_kk_deur", 0, 0, -0.4);
        Cutscene.Builder b = maak("plan_z").duur(1400).verbergEcht(48)
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, krab, 180f)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, spons, 180f)
                .acteur("octo", E2SponsguhFeature.OCTO_MIKA, weg(octo), 180f)
                .acteur("plankmika", E2SponsguhFeature.PLANKMIKA, plank, 180f)
                .speler(speler, -90f)
                // the morning: the empty safe
                .camera(0, kantoor.oog(), kantoor.kijk()).camera(246, kantoor.dichter(0.8), kantoor.kijk())
                .animatie("krabguh", 4, "paniek").geluid(4, () -> SoundEvents.IRON_DOOR_OPEN, 0.8f, 0.7f)
                .zeg(6, "krabguh", "weg", 64)
                .animatie("sponsguh", 60, "rennen").loop("sponsguh", 60, 76, sponsIn)
                .zeg(76, "sponsguh", "wat", 46)
                .kijk("krabguh", 124, sponsIn).animatie("krabguh", 126, "praat")
                .zeg(126, "krabguh", "kluis", 70)
                .animatie("sponsguh", 198, "schrik").zeg(200, "sponsguh", "gemeen", 44)
                // last night, in the lab
                .zwart(248, 326).geluid(250, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .zeg(254, "verteller", "terug", 66)
                .cameraKnip(326, lab.oog(), lab.kijk()).camera(666, lab.dichter(0.7), lab.kijk())
                .animatie("plankmika", 328, "plan").zeg(332, "plankmika", "mislukt", 62)
                .geluid(396, SpAudio.geluid("stinger.karen"), 1f, 1f).kijk("plankmika", 396, karen)
                .zeg(398, "karen", "la", 44)
                .kijk("plankmika", 444, kast).geluid(446, () -> SoundEvents.BARREL_OPEN, 0.8f, 1.3f)
                .deeltjes(448, ParticleTypes.END_ROD, kast, 14, 0.4).animatie("plankmika", 446, "schrik")
                .zeg(450, "plankmika", "z", 36)
                .animatie("plankmika", 488, "praat").zeg(490, "plankmika", "stappen", 76)
                .kijk("plankmika", 566, karen).zeg(570, "plankmika", "bedankt", 46)
                .geluid(618, SpAudio.geluid("stinger.karen"), 1f, 1.2f).zeg(620, "karen", "graag", 34)
                .animatie("plankmika", 656, "lach_kwaad").geluid(658, () -> SoundEvents.WITCH_CELEBRATE, 0.7f, 1.9f)
                // the night: across the street
                .zwart(672, 744).geluid(674, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .zeg(678, "verteller", "nacht", 60)
                .loop("plankmika", 700, 701, buiten)
                .cameraKnip(744, straat.oog(), straat.kijk()).camera(930, straat.dichter(0.6), straat.kijk())
                .loop("plankmika", 748, 820, kkDeur)
                .loop("plankmika", 822, 823, weg(kkDeur))
                .geluid(834, () -> SoundEvents.CHEST_OPEN, 0.6f, 1.6f).geluid(852, () -> SoundEvents.IRON_DOOR_OPEN, 0.5f, 1.8f)
                .zeg(828, "verteller", "kraak", 54)
                .loop("plankmika", 884, 885, kkDeur).animatie("plankmika", 885, "vlucht")
                .loop("plankmika", 886, 930, buiten).geluid(888, () -> SoundEvents.WITCH_CELEBRATE, 0.6f, 2.0f)
                .zeg(888, "plankmika", "hihi", 36)
                // back to the morning
                .zwart(932, 1000).zeg(936, "verteller", "ochtend", 56)
                .loop("plankmika", 960, 961, weg(plank)).loop("octo", 960, 961, octo)
                .cameraKnip(1000, kantoor2.oog(), kantoor2.kijk()).camera(1108, kantoor2.dichter(0.5), kantoor2.kijk())
                .kijk("krabguh", 1000, speler).zeg(1004, "krabguh", "een", 66)
                .kijk("krabguh", 1070, sponsIn).animatie("sponsguh", 1072, "schrik").schud(1074, 0.6f, 10)
                .zeg(1074, "sponsguh", "plankmika", 34)
                .cameraKnip(1110, kantoor.oog(), kantoor.kijk()).camera(1400, kantoor.dichter(1.0), kantoor.kijk())
                .kijk("krabguh", 1110, octo).kijk("octo", 1110, krab).zeg(1112, "octo", "vrij", 46)
                .animatie("krabguh", 1158, "paniek").zeg(1160, "krabguh", "nee", 22)
                .animatie("octo", 1184, "zucht")
                .kijk("sponsguh", 1184, krab).animatie("sponsguh", 1186, "salueer").zeg(1188, "sponsguh", "halen", 46)
                .kijk("krabguh", 1236, speler).animatie("krabguh", 1238, "praat").zeg(1240, "krabguh", "jij", 84)
                .kijk("sponsguh", 1326, speler).animatie("sponsguh", 1328, "juich").zeg(1330, "sponsguh", "wacht", 60);
        return reg("plan_z", b, a);
    }

    /** Wink 6 (DESIGN 5c): Professor Knabbelkloon in the lab, "if you clone the recipe, you have two!". 10 s. */
    private static Scene knipoogKloon() {
        String a = "knipoog_kloon_professor";
        Cam c = cam(a, "knipoog_kloon");
        Vec3 prof = op(a, a), plank = op(a, "plankmika"), karen = op(a, "karen", 0, 0.8, 0);
        return reg("knipoog_kloon", maak("knipoog_kloon").duur(190).verbergEcht(8)
                .npc("professor", Kind.KNABBELKLOON, prof, 180f)
                .acteur("plankmika", E2SponsguhFeature.PLANKMIKA, plank, 90f)
                .camera(0, c.oog(), c.kijk()).camera(190, c.dichter(0.6), c.kijk())
                .kijk("professor", 0, plank).kijk("plankmika", 0, prof)
                .animatie("professor", 6, "zwaai").zeg(8, "professor", "kloon", 56)
                .animatie("plankmika", 66, "praat").zeg(68, "plankmika", "geniaal", 46)
                .geluid(116, SpAudio.geluid("stinger.karen"), 1f, 0.8f).zeg(118, "karen", "nee", 22)
                .kijk("plankmika", 142, karen).zeg(144, "plankmika", "nee2", 30), a);
    }

    /** At the door of De Vetemmer: Sponsguh, the worst lookout of Bikinibodem, explains the sneak. 18 s. */
    private static Scene sluipUitleg() {
        String a = "vetemmer_ingang";
        Cam c = cam(a, "d1r_vetemmer_deur");
        Vec3 spons = op(a, a, 1.2, 0, -1.0), speler = op(a, a, -1.0, 0, -2.2);
        return reg("sluip_uitleg", maak("sluip_uitleg").duur(350).verbergEcht(10)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, spons, 135f)
                .speler(speler, -60f)
                .camera(0, c.oog(), c.kijk()).camera(350, c.dichter(0.8), c.kijk())
                .kijk("sponsguh", 0, speler).kijk("speler", 0, spons)
                .animatie("sponsguh", 4, "praat").zeg(6, "sponsguh", "psst", 50)
                .zeg(60, "sponsguh", "camera", 76)
                .animatie("sponsguh", 138, "praat").zeg(140, "sponsguh", "laser", 70)
                .zeg(214, "sponsguh", "rust", 70)
                .animatie("sponsguh", 286, "zwaai").zeg(288, "sponsguh", "succes", 50), a);
    }

    /** The safe at the end of the route: the secret ingredient is... a kaasknabbel; Karen notices "a customer". 21 s. */
    private static Scene recept() {
        String a = "receptkluis";
        Cam c = cam(a, "d1r_recept");
        Vec3 speler = op(a, a, -1, -1, 0), kluis = op(a, a, 0, 0.5, 0);
        Cutscene.Builder b = maak("recept").duur(416).verbergEcht(6)
                .speler(speler, -90f)
                .camera(0, c.oog(), c.kijk()).camera(416, c.dichter(1.2), c.kijk())
                .animatie("speler", 8, "zwaai").geluid(10, () -> SoundEvents.IRON_DOOR_OPEN, 0.8f, 1.4f)
                .deeltjes(12, ParticleTypes.END_ROD, kluis, 12, 0.3)
                .zeg(16, "verteller", "open", 56)
                .zeg(76, "verteller", "lees", 56);
        for (int t = 78; t <= 130; t += 4) {
            b.geluid(t, SoundEvents.NOTE_BLOCK_SNARE::value, 0.5f, 1.0f);           // (a drum roll)
        }
        return reg("recept", b.schud(136, 0.3f, 8).geluid(136, SoundEvents.NOTE_BLOCK_PLING::value, 1f, 1.5f)
                .zeg(138, "verteller", "knabbel", 40)
                .zeg(182, "verteller", "altijd", 40)
                .geluid(226, SpAudio.geluid("stinger.karen"), 1f, 1f).zeg(228, "karen", "iemand", 50)
                .zeg(282, "plankmika", "klant", 72)
                .zeg(358, "verteller", "weg", 50), a);
    }

    /** Back in the office: Krabguh hugs his recipe, a party in the Bellenkom tonight, Octoliam's challenge, Octo has no band. 33 s. */
    private static Scene feest() {
        String a = "krabguh";
        Cam kantoor = cam(a, "d1r_kantoor"), kantoor2 = cam(a, "d1r_kantoor_2");
        Vec3 krab = op(a, a, 1, 0, -1), speler = op(a, a, -1.5, 0, -0.7), spons = op(a, a, 0.1, 0, 0.9), octo = op(a, a, 2.1, 0, 1.1);
        return reg("feest", maak("feest").duur(650).verbergEcht(14)
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, krab, 90f)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, spons, 180f)
                .acteur("octo", E2SponsguhFeature.OCTO_MIKA, octo, 180f)
                .speler(speler, -90f)
                .camera(0, kantoor.oog(), kantoor.kijk()).camera(366, kantoor.dichter(0.8), kantoor.kijk())
                .kijk("krabguh", 0, speler).kijk("sponsguh", 0, speler).kijk("speler", 0, krab)
                .animatie("krabguh", 4, "knuffel").zeg(6, "krabguh", "recept", 60)
                .animatie("sponsguh", 68, "juich").zeg(70, "sponsguh", "held", 50)
                .animatie("krabguh", 122, "praat").zeg(124, "krabguh", "feest", 84)
                .kijk("octo", 210, krab).animatie("octo", 212, "zucht").zeg(214, "octo", "uitdaging", 76)
                .zeg(294, "octo", "band", 70)
                .cameraKnip(368, kantoor2.oog(), kantoor2.kijk()).camera(500, kantoor2.dichter(0.5), kantoor2.kijk())
                .kijk("sponsguh", 368, octo).animatie("sponsguh", 370, "juich").zeg(372, "sponsguh", "stad", 56)
                .zeg(432, "octo", "dom", 66)
                .cameraKnip(500, kantoor.oog(), kantoor.kijk()).camera(650, kantoor.dichter(1.0), kantoor.kijk())
                .kijk("krabguh", 500, speler).kijk("sponsguh", 500, speler).kijk("octo", 500, speler)
                .zeg(504, "krabguh", "ritme", 56)
                .zwart(566, 640).geluid(568, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .zeg(572, "verteller", "avond", 64), a);
    }

    /**
     * Zoete Knabbelzege (big, the song of part 1): the Bellenkom full, Octoliam mocks, Octo counts in, smoke, lights, your drum,
     * Sponsguh sings, the town joins; Octoliam faints, Octo bows, Krabguh counts his money. 48 s + the song.
     */
    private static Scene zoeteZege() {
        String a = "podium_trommel";
        Cam wijd = cam(a, "d1r_zege_wijd"), spons = cam(a, "d1r_zege_sponsguh"), trommel = cam(a, "d1r_zege_trommel"),
                octoCam = cam(a, "d1r_zege_octo"), liam = cam(a, "d1r_zege_octoliam"), publiek = cam(a, "d1r_zege_publiek");
        Vec3 octo = op(a, "podium_octo"), sponsguh = op(a, "podium_sponsguh"), speler = op(a, "d1r_trommel_speler"), octoliam = op(a, "octoliam");
        Vec3 podium = op(a, "podium_octo", 0, 1, 0);
        Cutscene.Builder b = maak("zoete_zege").verbergEcht(40)
                .acteur("octo", E2SponsguhFeature.OCTO_MIKA, octo, 0f)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 0f)
                .acteur("octoliam", E2SponsguhFeature.OCTOLIAM, octoliam, 90f)
                .acteur("patguh", E2SponsguhFeature.PATGUH, op(a, "podium_sponsguh", 2, -2, 5), 180f)
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, op(a, "podium_octo", 2, -2, 8), 180f)
                .speler(speler, 0f);
        for (int i = 1; i <= 6; i++) {
            int soort = i - 1;
            b.acteur("band_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, op(a, "band_" + i), 0f, tag -> tag.putInt("Soort", soort));
        }
        List<String> publiekNamen = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Plekken.Plek plek = Plekken.plek(S, "d1r_publiek_" + i);
            if (plek == null) {
                break;
            }
            Vec3 stoel = op(a, plek.id());
            int soort = (i * 5) % 6;
            b.acteur("publiek_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, stoel, Cutscene.yawVan(podium.subtract(stoel)), tag -> tag.putInt("Soort", soort));
            publiekNamen.add("publiek_" + i);
        }
        b.camera(0, wijd.oog(), wijd.kijk()).camera(74, wijd.dichter(1.5), wijd.kijk())
                .zeg(10, "verteller", "bellenkom", 62)
                .cameraKnip(76, liam.oog(), liam.kijk()).camera(154, liam.dichter(0.6), liam.kijk())
                .animatie("octoliam", 78, "lach").zeg(80, "octoliam", "orkest", 72)
                .cameraKnip(156, octoCam.oog(), octoCam.kijk()).camera(212, octoCam.dichter(0.5), octoCam.kijk())
                .animatie("octo", 158, "zucht").zeg(160, "octo", "vergissing", 50)
                .cameraKnip(214, spons.oog(), spons.kijk()).camera(264, spons.dichter(0.5), spons.kijk())
                .kijk("sponsguh", 214, octo).zeg(216, "sponsguh", "vertrouw", 46)
                .cameraKnip(266, octoCam.oog(), octoCam.kijk()).camera(316, octoCam.dichter(0.6), octoCam.kijk())
                .animatie("octo", 268, "trommel_tel").zeg(270, "octo", "tel", 44);
        // the song: smoke, light, the drum, Sponsguh sings, the town joins
        int t0 = 318;
        b.cameraKnip(t0, wijd.oog(), wijd.kijk()).geluid(t0, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.6f).schud(t0, 0.7f, 12)
                .kijk("sponsguh", t0, plus(sponsguh, 0, 1, 6))
                .animatie("sponsguh", t0, "zing").animatie("octo", t0, "speel");
        int lied = SpAudio.lied(b, "zoete_zege", t0 + 6);
        int e = t0 + 6 + lied;
        for (int t = t0; t < e; t += 40) {
            for (int i = 1; i <= 4; i++) {
                b.deeltjes(t, ParticleTypes.CAMPFIRE_COSY_SMOKE, op(a, "rook_" + i, 0, 1.2, 0), 10, 0.5);
                b.deeltjes(t + 20, ParticleTypes.END_ROD, op(a, "licht_" + i, 0, -1.5, 0), 16, 0.6);
            }
        }
        for (int t = t0 + 6; t < e; t += 24) {                          // (merge_sp1: on beats 1 and 3 of the song, 100 BPM = 12 ticks a beat from t0 + 6)
            b.animatie("speler", t, "zwaai");
            b.geluid(t, SoundEvents.NOTE_BLOCK_BASEDRUM::value, 0.5f, 1.0f);
        }
        for (int t = t0 + 10; t < e; t += 48) {
            for (int i = 1; i <= 6; i++) {
                b.animatie("band_" + i, t + i * 3, (t / 48) % 2 == 0 ? "juich" : "dans");
            }
            for (int i = 0; i < publiekNamen.size(); i++) {
                b.animatie(publiekNamen.get(i), t + i * 2, (t / 48 + i) % 2 == 0 ? "juich" : "dans");
            }
        }
        int fase = Math.max(40, lied / 5);
        b.cameraKnip(t0 + fase, spons.oog(), spons.kijk()).camera(t0 + 2 * fase - 2, spons.dichter(0.8), spons.kijk())
                .cameraKnip(t0 + 2 * fase, trommel.oog(), trommel.kijk()).camera(t0 + 3 * fase - 2, trommel.dichter(0.8), trommel.kijk())
                .cameraKnip(t0 + 3 * fase, publiek.oog(), publiek.kijk()).camera(t0 + 4 * fase - 2, publiek.dichter(1.0), publiek.kijk())
                .cameraKnip(t0 + 4 * fase, wijd.oog(), wijd.kijk()).camera(e, wijd.dichter(2.0), wijd.kijk());
        // the end of the song: cheers, Octoliam faints, Octo bows
        b.animatie("octo", e, "idle").animatie("sponsguh", e, "juich").animatie("patguh", e, "juich")
                .geluid(e + 2, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.0f).geluid(e + 8, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.3f)
                .deeltjes(e + 2, ParticleTypes.FIREWORK, plus(podium, 0, 4, 0), 40, 2.5)
                .cameraKnip(e + 4, wijd.oog(), wijd.kijk()).camera(e + 62, wijd.dichter(1.0), wijd.kijk())
                .zeg(e + 10, "verteller", "stilte", 50)
                .cameraKnip(e + 64, liam.oog(), liam.kijk()).camera(e + 122, liam.dichter(0.5), liam.kijk())
                .zeg(e + 66, "octoliam", "prachtig", 54).animatie("octoliam", e + 112, "flauw")
                .cameraKnip(e + 124, octoCam.oog(), octoCam.kijk()).camera(e + 180, octoCam.dichter(0.5), octoCam.kijk())
                .animatie("octo", e + 126, "buig").zeg(e + 128, "octo", "mij", 50)
                .cameraKnip(e + 182, spons.oog(), spons.kijk()).camera(e + 232, spons.dichter(0.5), spons.kijk())
                .kijk("sponsguh", e + 182, octo).animatie("sponsguh", e + 184, "huil_blij").zeg(e + 186, "sponsguh", "ster", 44)
                .cameraKnip(e + 234, wijd.oog(), wijd.kijk()).camera(e + 400, wijd.dichter(2.5), wijd.kijk())
                .animatie("krabguh", e + 236, "tel_geld").zeg(e + 238, "krabguh", "verkocht", 60)
                .zwart(e + 304, e + 390).geluid(e + 306, SpAudio.geluid("stinger.verteller"), 1f, 1f)
                .zeg(e + 310, "verteller", "einde", 76)
                .duur(e + 400);
        return reg("zoete_zege", b, a);
    }
}
