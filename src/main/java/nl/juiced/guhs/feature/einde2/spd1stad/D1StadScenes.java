package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_d1_stad: the scripts of its scenes (CONTRACT_E2_C 5.2; texts:
 * tools/features/einde2_sp_d1_stad_tekst.py SCENES, keys {@code scene.guhs.einde2_sp_d1_stad_<naam>.*}). Every position is
 * relative to the scene's anchor marker of Bikinibodem and worked out from the build's own markers ({@link #r}); every scene
 * steps its camera out of a wall ({@code cameraOntwijkt}) and hides the residents near its anchor ({@code verbergEcht}), so the
 * real Patguh / juf Pufguh never stands beside their actor. 1.4.4: every spoken line waits for a click. The actors stand still
 * and face the camera or each other (playtest 2 item 2); the camera frames the speaker's head (the cast is small: Pufguh 1.15,
 * Sponsguh 1.25, Patguh 1.6 blocks).
 * <p>
 * Running time (without the waits for a click; {@link #LENGTES}): patguh 24 s, knipoog_rookguh 10 s, kwallen_klaar 15 s,
 * pufguh 25 s, knipoog_guhdalf 9.75 s, examen_klaar 20 s. None over 30 s.
 */
public final class D1StadScenes {
    public static final String ID = "einde2_sp_d1_stad_";
    private static final String S = D1Stad.STRUCTUUR;

    public static Cutscene PATGUH, KNIPOOG_ROOKGUH, KWALLEN_KLAAR, PUFGUH, KNIPOOG_GUHDALF, EXAMEN_KLAAR;
    /** The look scenes of the hints (~3 s, one caption each, never counted). */
    public static Cutscene KIJK_PATGUH, KIJK_KWALLEN, KIJK_PUFGUH, KIJK_BOOT;
    /** The scene names in order (the dev command, the python self-check). */
    public static final String[] NAMEN = {"patguh", "knipoog_rookguh", "kwallen_klaar", "pufguh", "knipoog_guhdalf", "examen_klaar"};
    static final String[] KIJK = {"patguh", "kwallen", "pufguh", "boot"};
    /** Scene name -> {least, most} seconds (CONTRACT_E2_C 5.2; the game test checks them). */
    public static final Map<String, int[]> LENGTES = Map.of("patguh", new int[] {15, 25}, "knipoog_rookguh", new int[] {5, 10},
            "kwallen_klaar", new int[] {10, 15}, "pufguh", new int[] {20, 25}, "knipoog_guhdalf", new int[] {5, 10},
            "examen_klaar", new int[] {15, 20});

    private D1StadScenes() {
    }

    /** From E2SpD1StadFeature.register (common code: both sides know the scenes by id). */
    static void registreer() {
        PATGUH = patguh();
        KNIPOOG_ROOKGUH = knipoogRookguh();
        KWALLEN_KLAAR = kwallenKlaar();
        PUFGUH = pufguh();
        KNIPOOG_GUHDALF = knipoogGuhdalf();
        EXAMEN_KLAAR = examenKlaar();
        KIJK_PATGUH = StapHint.kijkScene(ID + "kijk_patguh", true);
        KIJK_KWALLEN = StapHint.kijkScene(ID + "kijk_kwallen", true);
        KIJK_PUFGUH = StapHint.kijkScene(ID + "kijk_pufguh", true);
        KIJK_BOOT = StapHint.kijkScene(ID + "kijk_boot", true);
    }

    /** A scene of this slice by its name (null: none). */
    @Nullable
    static Cutscene van(String naam) {
        return switch (naam) {
            case "patguh" -> PATGUH;
            case "knipoog_rookguh" -> KNIPOOG_ROOKGUH;
            case "kwallen_klaar" -> KWALLEN_KLAAR;
            case "pufguh" -> PUFGUH;
            case "knipoog_guhdalf" -> KNIPOOG_GUHDALF;
            case "examen_klaar" -> EXAMEN_KLAAR;
            default -> null;
        };
    }

    /** The anchor marker of each scene. */
    static String anker(String naam) {
        return switch (naam) {
            case "patguh" -> "patguh";
            case "knipoog_rookguh" -> "knipoog_rookguh_patguh";
            case "kwallen_klaar" -> "kwallen_start";
            case "pufguh" -> "pufguh";
            case "knipoog_guhdalf" -> "knipoog_guhdalf_brug";
            default -> "examen_finish";
        };
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    @Nullable
    private static BlockPos pos(String id) {
        Plekken.Plek p = Plekken.plek(S, id);
        return p == null ? null : p.pos();
    }

    /** Where marker {@code plek} is seen from marker {@code anker} (the middle of its floor), plus an offset. */
    static Vec3 r(String anker, String plek, double dx, double dy, double dz) {
        BlockPos a = pos(anker), b = pos(plek);
        if (a == null || b == null) {
            return new Vec3(0.5 + dx, dy, 0.5 + dz);
        }
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    /** Point {@code i} of a pad marker seen from {@code anker} (the pad's first point when it has fewer). */
    static Vec3 rp(String anker, String pad, int i, double dx, double dy, double dz) {
        BlockPos a = pos(anker);
        Plekken.Plek p = Plekken.plek(S, pad);
        if (a == null || p == null || p.punten().isEmpty()) {
            return new Vec3(0.5 + dx, dy, 0.5 + dz);
        }
        BlockPos b = p.punten().get(Math.min(i, p.punten().size() - 1));
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    /** The yaw that looks from a to b. */
    static float yaw(Vec3 a, Vec3 b) {
        return Cutscene.yawVan(b.subtract(a));
    }

    /** NBT of a cast actor: on its mark, no AI. */
    private static Consumer<CompoundTag> stil() {
        return t -> {
            t.putBoolean("Stil", true);
            t.putBoolean("NoAI", true);
        };
    }

    private static Cutscene.Builder scene(String naam, int duur) {
        return Cutscene.maak(ID + naam).duur(duur).bij(D1Stad.LIJN).kaart("einde2_sp_d1_start").cameraOntwijkt();
    }

    private static Supplier<? extends EntityType<?>> boot() {
        return E2SpD1StadFeature.LESBOOT;
    }

    // =====================================================================================================================
    // step 5: jellyfishing
    // =====================================================================================================================

    /** Patguh wakes in front of his rock, why the jellies (the restaurant's kwallenjam), the net (anchor patguh). 24 s. */
    private static Cutscene patguh() {
        Vec3 pat = v(0.5, 0, 0.5), speler = v(0.5, 0, -5.5), dicht = v(0.5, 0, -2.4);
        Cutscene.Builder b = scene("patguh", 480).verbergEcht(10)
                .speler(speler, 0)
                .acteur("patguh", E2SponsguhFeature.PATGUH, pat, yaw(pat, dicht), stil());
        b.camera(0, v(6.5, 4.0, -8.0), v(0.5, 1.0, -1.0)).camera(110, v(4.5, 2.6, -5.5), v(0.5, 1.0, -0.5))
                .animatie("patguh", 0, "slaap")
                .zeg(10, "verteller", "intro", 60)
                .loop(Cutscene.SPELER, 30, 90, dicht)
                .zeg(80, "patguh", "snurk", 55)
                // he wakes: a close-up from the front left, his big head in the picture
                .cameraKnip(138, v(-1.6, 1.7, -2.4), v(0.5, 1.3, 0.5))
                .animatie("patguh", 140, "schrik").geluid(140, () -> SoundEvents.PUFFER_FISH_BLOW_UP, 0.7f, 1.3f)
                .zeg(142, "patguh", "wakker", 55)
                .animatie("patguh", 200, "praat").zeg(200, "patguh", "jam", 70)
                // the net: two of them, side on
                .cameraKnip(272, v(4.2, 1.8, -1.6), v(0.5, 1.1, -1.0))
                .animatie("patguh", 275, "juich").zeg(275, "patguh", "net", 65)
                .deeltjes(280, ParticleTypes.HAPPY_VILLAGER, v(0.5, 1.2, -1.4), 10, 0.4)
                .animatie("patguh", 345, "praat").zeg(345, "patguh", "veld", 70)
                .kijk(Cutscene.SPELER, 345, v(-8, 1, -4))
                // and off he runs, west along the street
                .camera(420, v(3.5, 2.4, -3.0), v(-4.0, 1.0, -3.5))
                .loop("patguh", 420, 475, v(-10.5, 0, -3.5));
        return b.registreer();
    }

    /** Wink 7: a Rookguh floats among the jellies, Patguh catches "the biggest"; it sighs and floats off with net and all. 10 s. */
    private static Cutscene knipoogRookguh() {
        String a = "knipoog_rookguh_patguh";
        Vec3 cam = rp(a, "knipoog_rookguh_camera", 0, 0, 0, 0), pat = v(0.5, 0, 0.5);
        Vec3 vang = v(-0.3, 1.5, -0.6);
        Cutscene.Builder b = Cutscene.maak(ID + "knipoog_rookguh").duur(200).bij(D1Stad.LIJN).cameraOntwijkt().verbergEcht(24)
                .acteur("patguh", E2SponsguhFeature.PATGUH, pat, yaw(pat, cam), stil())
                .guh("rookguh", GuhVariant.ROOKGUH, rp(a, "knipoog_rookguh_zweef", 0, 0, -1.5, 0), 120)
                .acteur("kwal_1", E2ZweversFeature.KRUIMELKWAL, v(-3.0, 2.4, 2.0), 0, stil())
                .acteur("kwal_2", E2ZweversFeature.KRUIMELKWAL, v(2.5, 3.2, -3.0), 0, stil());
        b.camera(0, cam, v(-0.5, 1.8, -0.6)).camera(200, cam.add(-1.5, -0.5, -1.5), v(0.5, 1.8, -1.0))
                .loop("rookguh", 0, 50, vang)
                .zeg(4, "rookguh", "bloep", 40)
                .kijk("patguh", 40, vang).animatie("patguh", 52, "zwaai")
                .geluid(54, () -> SoundEvents.FISHING_BOBBER_SPLASH, 0.6f, 1.4f)
                .kijk("patguh", 58, cam).animatie("patguh", 58, "juich")
                .zeg(60, "patguh", "grootste", 50)
                .zeg(115, "rookguh", "zucht", 40)
                .loop("rookguh", 120, 160, rp(a, "knipoog_rookguh_zweef", 2, 0, 0, 0))
                .loop("rookguh", 160, 196, rp(a, "knipoog_rookguh_zweef", 3, 0, 0, 0))
                .kijk("patguh", 125, rp(a, "knipoog_rookguh_zweef", 3, 0, 0, 0))
                .animatie("patguh", 150, "schrik").zeg(150, "patguh", "netje", 40);
        return b.registreer();
    }

    /** Eight in the jar: Patguh proud, the jam and his flowery trousers, Sponsguh's exam (anchor kwallen_start). 15 s. */
    private static Cutscene kwallenKlaar() {
        String a = "kwallen_start";
        Vec3 pat = r(a, "kwallen_patguh", 0, 0, 0), speler = v(-1.0, 0, 1.5);
        Vec3 midden = pat.add(speler).scale(0.5);
        Cutscene.Builder b = scene("kwallen_klaar", 300).verbergEcht(8)
                .speler(speler, yaw(speler, pat))
                .acteur("patguh", E2SponsguhFeature.PATGUH, pat, yaw(pat, speler), stil());
        b.camera(0, midden.add(3.0, 2.0, 3.4), midden.add(0, 1.1, 0)).camera(300, midden.add(2.4, 1.7, 2.8), midden.add(0, 1.1, 0))
                .zeg(8, "verteller", "held", 60)
                .deeltjes(10, ParticleTypes.GLOW, speler.add(0, 1.0, 0), 16, 0.4)
                .cameraKnip(72, pat.add(-1.6, 1.6, 2.2), pat.add(0, 1.3, 0))
                .animatie("patguh", 74, "juich").zeg(74, "patguh", "acht", 60)
                .animatie("patguh", 140, "praat").zeg(140, "patguh", "jam", 70)
                .deeltjes(150, ParticleTypes.HAPPY_VILLAGER, speler.add(0, 1.2, 0), 12, 0.4)
                .cameraKnip(212, midden.add(-2.6, 1.8, 3.0), midden.add(0, 1.1, 0))
                .animatie("patguh", 214, "praat").zeg(214, "patguh", "examen", 75);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 6: the boating exam
    // =====================================================================================================================

    /** Exam number thirty-eight: juf Pufguh's nerves, Sponsguh's courage, you are his rij-maatje (anchor pufguh). 25 s. */
    private static Cutscene pufguh() {
        Vec3 puf = v(0.5, 0, 0.5), spons = v(3.5, 0, 1.9), speler = v(3.5, 0, -0.6);
        Cutscene.Builder b = scene("pufguh", 500).verbergEcht(10)
                .speler(speler, yaw(speler, puf))
                .acteur("pufguh", E2SponsguhFeature.JUF_PUFGUH, puf, yaw(puf, v(3.5, 0, 0.6)), stil())
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, spons, yaw(spons, puf), stil());
        // the narrator's title card
        b.zwart(0, 44).geluid(4, SpAudio.geluid("stinger.verteller"), 1.0f, 1.0f)
                .zeg(6, "verteller", "ondertussen", 60)
                .camera(0, v(9.0, 5.0, -6.5), v(2.0, 1.0, 0.5)).camera(68, v(7.0, 3.5, -4.5), v(2.0, 1.0, 0.5))
                .cameraKnip(68, v(2.6, 1.3, -1.4), v(0.5, 0.9, 0.5))
                .animatie("pufguh", 70, "zucht").zeg(70, "pufguh", "nummer", 60)
                .cameraKnip(132, v(1.6, 1.3, 3.9), v(3.5, 1.0, 1.9))
                .animatie("sponsguh", 135, "juich").zeg(135, "sponsguh", "klaar", 70)
                .geluid(140, SpAudio.geluid("stinger.lach"), 0.8f, 1.0f)
                .cameraKnip(208, v(2.6, 1.3, -1.4), v(0.5, 0.9, 0.5))
                .kijk("pufguh", 205, speler.add(0, 1.4, 0)).animatie("pufguh", 210, "praat").zeg(210, "pufguh", "maatje", 70)
                .animatie("pufguh", 285, "praat").zeg(285, "pufguh", "regels", 80)
                .cameraKnip(368, v(5.8, 1.7, 0.6), v(2.4, 1.0, 0.7))
                .kijk("sponsguh", 365, speler.add(0, 1.4, 0)).animatie("sponsguh", 370, "schrik").zeg(370, "sponsguh", "paniek", 60)
                .kijk("pufguh", 430, v(5.5, 1.0, -22.0)).animatie("pufguh", 435, "praat").zeg(435, "pufguh", "boot", 60)
                .kijk(Cutscene.SPELER, 435, v(5.5, 1.0, -22.0));
        return b.registreer();
    }

    /** Wink 5: Guhdalf on the bridge, "zonder rijbewijs"; the boat goes round him past the pond; juf Pufguh blows up. 9.75 s. */
    private static Cutscene knipoogGuhdalf() {
        String a = "knipoog_guhdalf_brug";
        Vec3 cam = rp(a, "knipoog_guhdalf_camera", 0, 0, 0, 0);
        Vec3 voor = rp(a, "examen_route", 4, 0, 0, -1.0);           // the course before the bridge
        Vec3 o0 = rp(a, "knipoog_guhdalf_omweg", 0, 0, 0, 0), o1 = rp(a, "knipoog_guhdalf_omweg", 1, 0, 0, 0),
                o2 = rp(a, "knipoog_guhdalf_omweg", 2, 0, 0, 0), o3 = rp(a, "knipoog_guhdalf_omweg", 3, 0, 0, 0);
        Vec3 puf = r(a, "pufguh", 0, 0, 0);
        Vec3 zit = v(0, 0.55, 0), voorin = v(0, 0.6, 0);
        Cutscene.Builder b = Cutscene.maak(ID + "knipoog_guhdalf").duur(195).bij(D1Stad.LIJN).cameraOntwijkt().verbergEcht(28)
                .npc("guhdalf", Kind.GUHDALF, v(0.5, 0, 0.5), 180)
                .acteur("boot", boot(), voor, 0)
                .speler(voor.add(zit).add(0, 0, -0.4), 0)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, voor.add(voorin).add(0, 0, 0.5), 0, stil())
                .acteur("pufguh", E2SponsguhFeature.JUF_PUFGUH, puf, yaw(puf, v(0.5, 0, 0.5)), stil());
        b.camera(0, cam, v(0.5, 1.4, -3.0)).camera(150, cam.add(0, -0.5, 0), v(6.0, 0.5, 0.5));
        // up to the bridge... Guhdalf
        b.loop("boot", 0, 30, o0).loop(Cutscene.SPELER, 0, 30, o0.add(zit).add(0, 0, -0.4)).loop("sponsguh", 0, 30, o0.add(voorin).add(0, 0, 0.5))
                .animatie("guhdalf", 6, "zwaai").zeg(6, "guhdalf", "passeren", 55)
                .animatie("sponsguh", 64, "schrik").zeg(64, "sponsguh", "omweg", 40).geluid(64, SpAudio.geluid("stinger.lach"), 0.6f, 1.4f);
        // round him: off the course, past the pond, back on
        int[] t = {68, 96, 126, 148};
        Vec3[] punt = {o1, o2, o3};
        for (int i = 0; i < 3; i++) {
            b.loop("boot", t[i], t[i + 1], punt[i]).loop(Cutscene.SPELER, t[i], t[i + 1], punt[i].add(zit))
                    .loop("sponsguh", t[i], t[i + 1], punt[i].add(voorin));
        }
        b.kijk("guhdalf", 100, o2.add(0, 1, 0));
        // and at the start line, juf Pufguh saw it all
        b.cameraKnip(150, puf.add(2.6, 1.3, -1.6), puf.add(0, 0.9, 0))
                .animatie("pufguh", 152, "blaas_op").geluid(152, () -> SoundEvents.PUFFER_FISH_BLOW_UP, 1.0f, 1.0f)
                .zeg(156, "pufguh", "puf", 38);
        return b.registreer();
    }

    /** Passed! Pufguh blows up with relief, a "voorlopig" pass, and far away Krabguh's alarm bell (anchor examen_finish). 20 s. */
    private static Cutscene examenKlaar() {
        Vec3 puf = v(-2.6, 0, 0.5), speler = v(0.6, 0, -0.4), spons = v(0.6, 0, 1.5);
        Cutscene.Builder b = scene("examen_klaar", 400).verbergEcht(24)
                .speler(speler, yaw(speler, puf))
                .acteur("pufguh", E2SponsguhFeature.JUF_PUFGUH, puf, yaw(puf, v(0.6, 0, 0.6)), stil())
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, spons, yaw(spons, puf), stil());
        b.camera(0, v(2.6, 2.0, -3.6), v(-1.0, 0.9, 0.5)).camera(130, v(2.2, 1.8, -3.0), v(-1.0, 0.9, 0.5))
                .animatie("pufguh", 8, "blaas_op").geluid(8, () -> SoundEvents.PUFFER_FISH_BLOW_UP, 1.0f, 0.9f)
                .zeg(10, "pufguh", "geslaagd", 55)
                .cameraKnip(66, spons.add(1.8, 1.2, 1.6), spons.add(0, 1.0, 0))
                .animatie("sponsguh", 68, "juich").zeg(68, "sponsguh", "rijbewijs", 55)
                .deeltjes(70, ParticleTypes.HAPPY_VILLAGER, spons.add(0, 1.4, 0), 14, 0.5)
                .cameraKnip(124, puf.add(2.4, 1.3, -1.4), puf.add(0, 0.9, 0))
                .animatie("pufguh", 126, "zucht").zeg(126, "pufguh", "voorlopig", 70)
                .cameraKnip(198, v(2.6, 2.0, -3.6), v(-1.0, 0.9, 0.5))
                .animatie("sponsguh", 200, "lach").zeg(200, "sponsguh", "team", 50)
                // far away: the bell of De Krokante Knabbel
                .geluid(252, () -> SoundEvents.BELL_BLOCK, 0.6f, 1.0f).geluid(262, () -> SoundEvents.BELL_BLOCK, 0.6f, 1.0f)
                .geluid(272, () -> SoundEvents.BELL_BLOCK, 0.6f, 1.0f).schud(252, 0.2f, 20)
                .camera(254, v(1.5, 4.0, -6.0), v(30.0, 3.0, -10.0))
                .kijk("sponsguh", 254, v(40, 2, -10)).kijk("pufguh", 254, v(40, 2, -10)).kijk(Cutscene.SPELER, 254, v(40, 2, -10))
                .zeg(256, "verteller", "bel", 60)
                .cameraKnip(320, spons.add(1.8, 1.2, 1.6), spons.add(0, 1.0, 0))
                .kijk("sponsguh", 318, speler.add(0, 1.4, 0)).animatie("sponsguh", 322, "schrik").zeg(322, "sponsguh", "alarm", 70);
        return b.registreer();
    }
}
