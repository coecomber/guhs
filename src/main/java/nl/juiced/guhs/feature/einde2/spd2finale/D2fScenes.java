package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: the scripts of the four story scenes of part 2's finale and the three look scenes of
 * its hints (CONTRACT_E2_C 5; texts: tools/features/einde2_sp_d2_finale.py SCENES / KIJK, keys
 * {@code scene.guhs.einde2_sp_d2_finale_<naam>.*}). Every position is relative to the scene's anchor marker and worked out from
 * the builds' own markers ({@link #r}), so a scene stands where the build has its spots; every camera steps out of a wall.
 * 1.4.4: the story waits for a click after every spoken line; only the song's lyrics run on ({@link SpAudio#lied}).
 * <p>
 * Cameras frame the speakers (playtest 3): the cast faces the camera or the player while it talks, big heads (David
 * Hasselguh lies flat, nine blocks long, his head at the front; Neptuguh and Octo are tall) are framed at their head height.
 * Lengths (running time without the click waits; normal 15-30 s, big 45-90 s): {@link #LENGTES}, the report.
 */
public final class D2fScenes {
    public static final String ID = "einde2_sp_d2_finale_";
    /** The anchors (markers): the pier head of Schelpenstad, David's landing at Bikinibodem, the middle of the Bellenkom stage. */
    public static final String ANKER_PIER = "hasselguh_opstap", ANKER_THUIS = "hasselguh_landing", ANKER_PODIUM = "podium_octo";

    public static Cutscene HASSELGUH, THUIS, GUHFY_GUHBER, BEDANKT;
    public static Cutscene KIJK_PIER, KIJK_EMMERS, KIJK_PODIUM;
    /** The tick the song starts in {@link #GUHFY_GUHBER} and how long it runs (from {@link SpAudio#lied}). */
    static int liedStart, liedDuur;
    /** Scene name -> {least, most} seconds (the contract's bounds; the song scene: without the song, which is sp_audio's). */
    public static final java.util.Map<String, int[]> LENGTES = java.util.Map.of(
            "hasselguh", new int[] {15, 25}, "thuis", new int[] {20, 30}, "guhfy_guhber", new int[] {40, 90}, "bedankt", new int[] {15, 25});

    private D2fScenes() {
    }

    /** From E2SpD2Finale.register (common code: both sides know the scenes by id). */
    static void registreer() {
        HASSELGUH = hasselguh();
        THUIS = thuis();
        GUHFY_GUHBER = guhfyGuhber();
        BEDANKT = bedankt();
        KIJK_PIER = StapHint.kijkScene(ID + "kijk_pier", true);
        KIJK_EMMERS = StapHint.kijkScene(ID + "kijk_emmers", true);
        KIJK_PODIUM = StapHint.kijkScene(ID + "kijk_podium", true);
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** Where marker {@code plek} is, seen from marker {@code anker} (the middle of its floor), plus an offset (absent: the anchor). */
    static Vec3 r(String structuur, String anker, String plek, double dx, double dy, double dz) {
        Plekken.Plek a = Plekken.plek(structuur, anker), b = Plekken.plek(structuur, plek);
        if (a == null || b == null) {
            return new Vec3(0.5 + dx, dy, 0.5 + dz);
        }
        BlockPos pa = a.pos(), pb = b.pos();
        return new Vec3(pb.getX() - pa.getX() + 0.5 + dx, pb.getY() - pa.getY() + dy, pb.getZ() - pa.getZ() + 0.5 + dz);
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static Cutscene.Builder scene(String naam, int duur) {
        return Cutscene.maak(ID + naam).duur(duur).bij(SpVerhaal.KROON).cameraOntwijkt();
    }

    /** The entity data of a cast actor: its looks (SpCast; hook: the stand-in keeps them in the persistent data, sp_cast's
     *  real looks read the same), and more (a flag, its Soort, standing still). */
    static Consumer<CompoundTag> nbt(String[] looks, Consumer<CompoundTag> meer) {
        return tag -> {
            tag.putBoolean("Stil", true);
            if (looks.length > 0) {
                CompoundTag data = tag.getCompoundOrEmpty("NeoForgeData").copy();
                for (String look : looks) {
                    data.putBoolean("guhs_einde2_sp_cast_look_" + look, true);     // (= SpCast's stand-in key; hook: sp_cast's own NBT)
                }
                tag.put("NeoForgeData", data);
            }
            if (meer != null) {
                meer.accept(tag);
            }
        };
    }

    static Consumer<CompoundTag> looks(String... looks) {
        return nbt(looks, null);
    }

    static Consumer<CompoundTag> soort(int soort, String... looks) {
        return nbt(looks, t -> t.putInt("Soort", soort));
    }

    private static Supplier<SoundEvent> s(SoundEvent e) {
        return () -> e;
    }

    /** A title card of the narrator: a black fade with his line (CONTRACT_E2_C 5.1). */
    private static void kaartje(Cutscene.Builder b, int t0, int t1, String key) {
        b.zwart(t0, t1).zeg(t0 + 6, "verteller", key, t1 - t0 - 10).geluid(t0 + 4, SpAudio.geluid("stinger.verteller"), 0.9f, 1f);
    }

    // =====================================================================================================================
    // step 9: Mindy-guh at the pier, David Hasselguh rises (anchor schelpenstad/hasselguh_opstap). 23 s.
    // =====================================================================================================================
    private static Cutscene hasselguh() {
        String S = SpVerhaal.SCHELPENSTAD, a = ANKER_PIER;
        // the pier head is 6 x 9 (x -3..+2, z -6..+2 of the anchor), open to the void on the south: David rises there, his
        // head (front of nine blocks) towards the pier; at the end he lies at d2f_hasselguh_start, where the real ride begins
        Vec3 davidOnder = v(-0.5, -9.0, 8.5), davidBoven = v(-0.5, -1.2, 8.5), davidKop = v(-0.5, 0.2, 4.6);
        Vec3 start = r(S, a, "d2f_hasselguh_start", 0, 0, 0);
        Cutscene.Builder b = scene("hasselguh", 470).verbergEcht(14)
                .speler(v(0.5, 0, 0.5), 0)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, v(-1.0, 0, -0.5), 0, looks(SpCast.KROON_VAST))
                .acteur("patguh", E2SponsguhFeature.PATGUH, v(1.8, 0, -0.8), 0, looks())
                .acteur("mindy", E2SponsguhFeature.MINDY_GUH, v(-1.5, 0, 2.0), 180, looks())
                .acteur("david", E2SponsguhFeature.DAVID_HASSELGUH, davidOnder, 180, looks());
        b.zwart(0, 14)
                // wide: the three on the pier head, the void behind them
                .camera(0, v(9.0, 4.0, 9.0), v(-0.5, 1.2, 0.0)).camera(54, v(7.5, 3.2, 7.5), v(-0.5, 1.2, 0.5))
                .zeg(16, "mindy", "mindy", 40).animatie("mindy", 16, "zwaai").kijk("mindy", 14, v(0.0, 1.4, 0.0))
                // Sponsguh holds up the crown
                .cameraKnip(56, v(0.6, 1.5, 3.0), v(-1.0, 1.0, -0.5)).kijk("sponsguh", 56, v(0.6, 1.5, 3.0))
                .zeg(58, "sponsguh", "kroon", 42).animatie("sponsguh", 58, "lach")
                .cameraKnip(102, v(-0.2, 1.7, 2.8), v(1.8, 1.2, -0.8)).kijk("patguh", 100, v(-0.2, 1.7, 2.8))
                .zeg(104, "patguh", "rozijn", 40)
                // Mindy: the deadline (no real timer: "voor de zeepbelzon ondergaat")
                .cameraKnip(146, v(0.2, 1.9, -1.8), v(-1.5, 1.3, 2.0)).kijk("mindy", 146, v(0.2, 1.9, -1.8))
                .zeg(148, "mindy", "haast", 46)
                .cameraKnipVolgt(196, v(1.4, 1.5, 2.6), "sponsguh").kijk("sponsguh", 196, v(1.4, 1.5, 2.6))
                .zeg(198, "sponsguh", "paniek", 40).animatie("sponsguh", 198, "rennen").animatie("patguh", 200, "schrik")
                // Mindy blows her shell horn
                .cameraKnip(240, v(1.2, 1.4, 4.2), v(-1.5, 1.4, 2.0)).kijk("mindy", 238, v(1.2, 1.4, 4.2))
                .zeg(242, "mindy", "vriend", 26).animatie("mindy", 244, "blaas")
                .geluid(250, s(SoundEvents.FIRECHARGE_USE), 0.2f, 0.4f)
                .geluid(252, E2SponsguhFeature.HASSELGUH_PLONS::get, 1.0f, 0.6f)
                // David rises out of the void mist
                .cameraKnip(270, v(3.5, 2.6, -0.5), davidBoven.add(0, 0.6, 0)).camera(330, v(2.6, 1.9, 1.2), davidKop)
                .loop("david", 272, 312, davidBoven).schud(276, 0.4f, 30)
                .deeltjes(274, ParticleTypes.BUBBLE_POP, davidBoven.add(0, 1.0, 0), 60, 3.0)
                .deeltjes(296, ParticleTypes.CLOUD, davidBoven.add(0, 1.2, 0), 30, 2.5)
                .geluid(276, E2SponsguhFeature.HASSELGUH_PLONS::get, 1.2f, 0.7f)
                .kijk("sponsguh", 272, davidKop).kijk("patguh", 272, davidKop).kijk("mindy", 272, davidKop).kijk(Cutscene.SPELER, 272, davidKop)
                // his face, from the pier edge
                .cameraKnip(314, v(1.6, 1.3, 1.9), davidKop).zeg(316, "david", "lift", 40)
                .cameraKnipVolgt(358, v(0.4, 1.6, 2.6), "patguh").kijk("patguh", 356, v(0.4, 1.6, 2.6))
                .zeg(360, "patguh", "berg", 34)
                .cameraKnip(396, v(1.6, 1.3, 1.9), davidKop).zeg(398, "david", "train", 40)
                // off we go: he swims to the pier head's side, where the ride begins
                .cameraKnip(440, v(5.0, 3.0, 5.0), v(-0.5, 0.8, 2.0)).loop("david", 440, 468, start.add(0, 0, 0))
                .zeg(442, "sponsguh", "vahoeg", 26).animatie("sponsguh", 442, "juich").animatie("patguh", 444, "juich")
                .geluid(442, SpAudio.geluid("stinger.lach"), 0.9f, 1f);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 10: home, and buckets on every head (anchor bikinibodem/hasselguh_landing). 24 s.
    // =====================================================================================================================
    private static Cutscene thuis() {
        String B = SpVerhaal.BIKINIBODEM, a = ANKER_THUIS;
        Vec3 poort = r(B, a, "stadspoort", 0, 0, 0);
        Vec3 m1 = poort.add(-5.0, 0, -1.0), m2 = poort.add(-8.0, 0, 1.0), m3 = poort.add(-11.0, 0, -1.0);
        Cutscene.Builder b = scene("thuis", 480).verbergEcht(16)
                .speler(v(0.5, 0, 0.5), 135)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, v(-0.8, 0, 1.8), 135, looks(SpCast.KROON_VAST))
                .acteur("patguh", E2SponsguhFeature.PATGUH, v(1.8, 0, -0.4), 135, looks())
                // three guhs with bucket helmets, marching stiffly into town (they look straight ahead: they can't see)
                .acteur("emmer_1", E2SponsguhFeature.BIKINIBODEM_GUH, m1, 90, soort(0, SpCast.EMMER))
                .acteur("emmer_2", E2SponsguhFeature.BIKINIBODEM_GUH, m2, 90, soort(2, SpCast.EMMER))
                .acteur("emmer_3", E2SponsguhFeature.BIKINIBODEM_GUH, m3, 90, soort(4, SpCast.EMMER));
        kaartje(b, 0, 52, "titel");
        b.camera(0, v(6.0, 3.0, 6.0), v(0.5, 1.0, 0.5))
                // behind the three: through the gate, the town
                .cameraKnip(52, v(3.4, 2.2, 3.6), poort.add(0, 1.5, 0)).camera(104, v(2.6, 2.0, 2.8), poort.add(-2, 1.4, 0))
                .zeg(56, "sponsguh", "ruik", 44).animatie("sponsguh", 56, "lach")
                .loop("emmer_1", 60, 420, m1.add(-30, 0, 0)).loop("emmer_2", 60, 420, m2.add(-30, 0, 0)).loop("emmer_3", 60, 420, m3.add(-30, 0, 0))
                // nearer: the marching buckets
                .cameraKnip(106, poort.add(4.0, 1.8, 3.5), m2.add(-6, 1.0, 0)).camera(150, poort.add(2.0, 1.6, 3.0), m2.add(-10, 1.0, 0))
                .zeg(108, "patguh", "mode", 40).animatie("patguh", 108, "schrik")
                // the loudspeaker: Karen and Plankmika (voices only)
                .geluid(150, SpAudio.geluid("stinger.karen"), 1.0f, 1.0f)
                .cameraKnip(152, v(1.0, 1.4, -2.2), poort.add(-6, 4.0, 0)).camera(330, v(0.4, 1.6, -1.6), poort.add(-10, 5.0, 0))
                .zeg(154, "karen", "piep", 44)
                .zeg(200, "plankmika_stem", "muahaha", 44).schud(202, 0.15f, 20)
                .zeg(246, "karen", "limonade", 44)
                .zeg(292, "plankmika_stem", "karen", 34)
                // back to the three: Sponsguh's plan (their faces: the camera in front of them)
                .kijk("sponsguh", 330, v(-1.6, 1.6, -1.2)).kijk("patguh", 330, v(-1.6, 1.6, -1.2)).kijk(Cutscene.SPELER, 330, v(-0.8, 1.2, 1.8))
                .cameraKnipVolgt(332, v(-1.6, 1.6, -1.2), "sponsguh")
                .zeg(334, "sponsguh", "blazer", 46).animatie("sponsguh", 334, "salueer")
                .deeltjes(350, ParticleTypes.BUBBLE_POP, v(-0.2, 1.3, 1.2), 12, 0.3)
                .zeg(382, "sponsguh", "plan", 40)
                .cameraKnipVolgt(424, v(0.2, 1.6, -2.4), "patguh").zeg(426, "patguh", "aanmoedigen", 40).animatie("patguh", 428, "juich")
                .zwart(466, 480);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 11: the rock song (anchor bikinibodem/podium_octo, the middle of the stage). 15 s + the song + 29 s.
    // =====================================================================================================================
    private static Cutscene guhfyGuhber() {
        String B = SpVerhaal.BIKINIBODEM, a = ANKER_PODIUM;
        // the stage x -8.5..+9.5, z -2.5..+3.5 (stand y 0); the field two lower (y -2), z +5.5 .. +17.5; the gate at z +22
        Vec3 trommel = r(B, a, "podium_trommel", 0, 0, 0), zanger = r(B, a, "podium_sponsguh", 0, 0, 0);
        Vec3 poort = r(B, a, "d2f_bellenkom_poort", 0, 0, 0);
        Vec3 nept = v(-1.5, -2, 6.5), ijs = v(2.0, -2, 7.5), plank = v(0.5, 0, 0.0), patguh = v(-0.5, 0, 2.0);
        Vec3 sponsRand = v(-1.5, 0, 3.2);
        Cutscene.Builder b = scene("guhfy_guhber", 2000).verbergEcht(34)
                .speler(trommel, 0)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, zanger, 0, looks(SpCast.KROON_VAST))
                .acteur("sponsguh_m", E2SponsguhFeature.SPONSGUH, sponsRand.add(0, -40, 0), 0, looks(SpCast.MANAGER))
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, 0, looks())
                .acteur("plankmika", E2SponsguhFeature.PLANKMIKA, plank, 0, looks())
                .acteur("neptuguh", E2SponsguhFeature.NEPTUGUH, poort.add(0, 0, 0), 180, nbt(new String[0], t -> t.putBoolean("Kroon", false)))
                .acteur("neptuguh_k", E2SponsguhFeature.NEPTUGUH, nept.add(0, -40, 0), 180, nbt(new String[0], t -> t.putBoolean("Kroon", true)))
                .acteur("ijs", E2SponsguhFeature.MENEER_KRABGUH, poort.add(2.0, 0, 1.0), 180, nbt(new String[0], t -> t.putBoolean("Bevroren", true)))
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, ijs.add(0, -40, 0), 180, nbt(new String[0], t -> t.putBoolean("Bevroren", false)))
                .acteur("octo", E2SponsguhFeature.OCTO_MIKA, v(7.5, -2, 14.5), 180, looks(SpCast.EMMER))
                .acteur("octo_vrij", E2SponsguhFeature.OCTO_MIKA, v(7.5, -42, 14.5), 180, looks());
        // the crowd: in front the six guhs the player freed (no buckets), behind them four still under a bucket (and their
        // freed twins under the floor: the song swaps them)
        for (int i = 0; i < 6; i++) {
            b.acteur("vrij_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, v(-5.5 + i * 2.2, -2, 9.5 + (i % 2) * 0.8), 180, soort(i));
        }
        for (int i = 0; i < 4; i++) {
            Vec3 plek = v(-4.5 + i * 3.0, -2, 13.0 + (i % 2) * 0.7);
            b.acteur("emmer_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, plek, 180, soort(5 - i, SpCast.EMMER));
            b.acteur("bevrijd_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, plek.add(0, -40, 0), 180, soort(5 - i));
        }
        // --- part A: the stand-off (Plankmika on his little box in the middle of the stage) ------------------------------
        b.zwart(0, 16)
                .camera(0, v(0.5, 2.0, 16.0), v(0.5, 1.0, 0.5))
                .cameraKnip(14, v(0.5, 0.7, 3.4), v(0.5, 0.35, 0.0))
                .zeg(16, "plankmika", "te_laat", 52).animatie("plankmika", 16, "lach_kwaad")
                .cameraKnipVolgt(70, v(-2.5, 1.5, 6.0), "sponsguh").zeg(72, "sponsguh", "zes", 40).kijk("sponsguh", 70, plank)
                .cameraKnip(114, v(0.5, 0.7, 3.4), v(0.5, 0.35, 0.0)).zeg(116, "plankmika", "honderden", 44)
                .cameraKnipVolgt(162, v(0.0, 1.6, 5.8), "patguh").zeg(164, "patguh", "wat", 30).kijk("patguh", 160, plank)
                .cameraKnip(196, v(0.5, 0.7, 3.4), v(0.5, 0.35, 0.0)).zeg(198, "plankmika", "onzin", 42).animatie("plankmika", 198, "lach_kwaad")
                // the idea: Sponsguh looks at the player
                .kijk("sponsguh", 240, trommel.add(0, 1.4, 0)).animatie("sponsguh", 242, "knipper")
                .cameraKnip(242, v(-1.0, 1.5, 6.5), zanger.add(0, 1.0, 0)).zeg(244, "sponsguh", "idee", 46)
                .kijk("sponsguh", 290, v(0.5, 1.4, 14.0)).cameraKnip(292, v(0.5, 3.0, 15.0), v(0.5, 1.2, 1.0))
                .zeg(294, "sponsguh", "voor_jullie", 30).animatie("sponsguh", 294, "salueer");
        // --- part B: the song (lyrics and timing: sp_audio) ----------------------------------------------------------------
        int t = 326;
        int l = Math.max(200, SpAudio.lied(b, "guhfy_guhber", t));
        liedStart = t;
        liedDuur = l;
        int q1 = t + l / 4, q2 = t + l / 2, q3 = t + 3 * l / 4;
        Vec3[] lichten = {r(B, a, "licht_1", 0, 0, 0), r(B, a, "licht_2", 0, 0, 0), r(B, a, "licht_3", 0, 0, 0), r(B, a, "licht_4", 0, 0, 0)};
        Vec3[] rook = {r(B, a, "rook_1", 0, 0, 0), r(B, a, "rook_2", 0, 0, 0), r(B, a, "rook_3", 0, 0, 0), r(B, a, "rook_4", 0, 0, 0)};
        b.animatie("sponsguh", t, "zing").animatie("patguh", t, "zing").animatie("plankmika", t + 10, "schrik")
                .camera(t, v(0.5, 1.4, 7.0), v(0.5, 1.2, 2.0)).camera(t + l / 6, v(3.5, 1.6, 6.0), trommel.add(0, 1.0, 0))
                .camera(q1, v(-6.0, 1.0, 12.0), v(0.0, -1.0, 11.0)).camera(q2 - 10, v(0.0, 0.5, 18.0), v(0.0, -1.0, 13.0));
        for (int i = 0; i < 4; i++) {
            b.deeltjes(t + 6 + i * 4, ParticleTypes.END_ROD, lichten[i].add(0, -3.0, 0), 30, 1.2)
                    .deeltjes(t + 8 + i * 4, ParticleTypes.CAMPFIRE_COSY_SMOKE, rook[i].add(0, 0.6, 0), 14, 0.8)
                    .deeltjes(q2 + i * 3, ParticleTypes.END_ROD, lichten[i].add(0, -3.0, 0), 30, 1.4);
        }
        for (int k = t + 20; k < t + l; k += 40) {
            b.animatie(Cutscene.SPELER, k, "spring").deeltjes(k, ParticleTypes.NOTE, trommel.add(0, 1.6, 0), 3, 0.5);
        }
        b.animatie("vrij_0", t + 30, "juich").animatie("vrij_2", t + 34, "juich").animatie("vrij_4", t + 38, "juich")
                .animatie("vrij_1", q1, "dans").animatie("vrij_3", q1 + 4, "dans").animatie("vrij_5", q1 + 8, "dans");
        // BOEM: the buckets fly off (the bucketed twins drop under the floor, the freed ones come up), the crowd cheers
        b.schud(q2, 0.6f, 24).geluid(q2, s(SoundEvents.ANVIL_LAND), 0.6f, 1.8f).geluid(q2 + 3, s(SoundEvents.ANVIL_LAND), 0.5f, 2.0f);
        for (int i = 0; i < 4; i++) {
            Vec3 plek = v(-4.5 + i * 3.0, -2, 13.0 + (i % 2) * 0.7);
            b.loop("emmer_" + i, q2, q2 + 1, plek.add(0, -40, 0)).loop("bevrijd_" + i, q2, q2 + 1, plek)
                    .deeltjes(q2, ParticleTypes.CRIT, plek.add(0, 1.4, 0), 16, 0.4)
                    .deeltjes(q2 + 1, ParticleTypes.BUBBLE_POP, plek.add(0, 1.6, 0), 10, 0.5)
                    .animatie("bevrijd_" + i, q2 + 6, "juich").animatie("bevrijd_" + i, q3 + i * 3, "dans");
        }
        b.loop("octo", q2, q2 + 1, v(7.5, -42, 14.5)).loop("octo_vrij", q2, q2 + 1, v(7.5, -2, 14.5))
                .deeltjes(q2, ParticleTypes.CRIT, v(7.5, 0.8, 14.5), 16, 0.5)
                .camera(q2 + 2, v(0.0, 0.6, 18.5), v(0.0, -0.6, 12.0)).camera(q3, v(0.5, 2.6, 16.0), v(0.5, 1.0, 1.0))
                .camera(t + l - 10, v(0.5, 1.6, 9.0), v(0.5, 1.0, 1.5))
                .deeltjes(q3, ParticleTypes.FIREWORK, v(0.5, 4.0, 2.0), 40, 3.0).deeltjes(q3 + 20, ParticleTypes.FIREWORK, v(-4.0, 4.0, 2.0), 40, 3.0);
        // --- part C: the crown, the thaw, the manager ------------------------------------------------------------------------
        int e = t + l;
        b.animatie("sponsguh", e, "idle").animatie("patguh", e, "idle")
                .cameraKnip(e, v(0.5, 0.7, 3.4), v(0.5, 0.35, 0.0)).zeg(e + 2, "plankmika", "nee", 40).animatie("plankmika", e + 2, "schrik")
                // Neptuguh comes in through the gate with the block of ice (his deadline)
                .cameraKnip(e + 44, nept.add(3.5, 1.8, 6.0), poort.add(0, 2.5, 0)).camera(e + 110, nept.add(3.0, 1.6, 4.5), nept.add(0, 2.4, 0))
                .loop("neptuguh", e + 44, e + 104, nept).loop("ijs", e + 44, e + 104, ijs)
                .zeg(e + 46, "neptuguh", "wie", 52).geluid(e + 46, s(SoundEvents.FIRECHARGE_USE), 0.3f, 0.5f)
                .kijk("neptuguh", e + 106, sponsRand.add(0, 1.0, 0))
                // Sponsguh to the edge of the stage, the crown held up
                .loop("sponsguh", e + 100, e + 130, sponsRand).kijk("sponsguh", e + 132, nept.add(0, 2.5, 0))
                .cameraKnip(e + 132, v(-4.5, 0.6, 7.5), sponsRand.add(0, 1.0, 0)).zeg(e + 134, "sponsguh", "uw_kroon", 46)
                .cameraKnip(e + 182, v(-0.5, 1.0, 10.5), nept.add(0, 2.5, 0)).zeg(e + 184, "neptuguh", "maar_wie", 46)
                // Karen tells: the camera finds the little one
                .geluid(e + 230, SpAudio.geluid("stinger.karen"), 1.0f, 1.0f)
                .kijk("neptuguh", e + 230, plank).kijk("patguh", e + 230, plank).kijk(Cutscene.SPELER, e + 230, plank)
                .cameraKnip(e + 232, v(0.5, 0.7, 3.4), v(0.5, 0.35, 0.0)).zeg(e + 234, "karen", "opgenomen", 46)
                .zeg(e + 282, "plankmika", "weg", 36).animatie("plankmika", e + 284, "vlucht")
                // he flees in a bucket: off the stage, west
                .loop("plankmika", e + 320, e + 346, v(-10.0, 0, -1.0)).deeltjes(e + 322, ParticleTypes.CLOUD, plank.add(0, 0.3, 0), 12, 0.3)
                .geluid(e + 322, s(SoundEvents.ANVIL_LAND), 0.3f, 2.0f)
                // the crown on the king's head (cut), and the thaw
                .loop("neptuguh", e + 348, e + 349, nept.add(0, -40, 0)).loop("neptuguh_k", e + 348, e + 349, nept)
                .loop("sponsguh", e + 348, e + 360, zanger)
                .cameraKnip(e + 350, v(-0.5, 1.0, 11.0), nept.add(0, 2.5, 0)).zeg(e + 352, "neptuguh_k", "vrij", 44).animatie("neptuguh_k", e + 352, "lach")
                .animatie("neptuguh_k", e + 398, "blaas").geluid(e + 400, s(SoundEvents.GLASS_BREAK), 0.9f, 0.7f)
                .deeltjes(e + 400, ParticleTypes.SNOWFLAKE, ijs.add(0, 1.0, 0), 50, 0.8)
                .loop("ijs", e + 402, e + 403, ijs.add(0, -40, 0)).loop("krabguh", e + 402, e + 403, ijs)
                .cameraKnip(e + 404, ijs.add(1.5, 1.4, 3.0), ijs.add(0, 0.9, 0)).zeg(e + 406, "krabguh", "brrr", 42).animatie("krabguh", e + 406, "paniek")
                .kijk("krabguh", e + 450, zanger.add(0, 1.0, 0))
                .cameraKnip(e + 450, ijs.add(-2.0, 1.6, 3.5), zanger.add(0, 1.0, 0)).zeg(e + 452, "krabguh", "manager", 48)
                // Sponsguh, manager: the cap (cut to the one with the manager's cap)
                .loop("sponsguh", e + 500, e + 501, zanger.add(0, -40, 0)).loop("sponsguh_m", e + 500, e + 501, zanger)
                .deeltjes(e + 500, ParticleTypes.HAPPY_VILLAGER, zanger.add(0, 1.2, 0), 20, 0.6)
                .cameraKnipVolgt(e + 502, v(-2.0, 1.4, 6.0), "sponsguh_m").zeg(e + 504, "sponsguh_m", "ik", 48).animatie("sponsguh_m", e + 504, "huil_blij")
                .geluid(e + 506, SpAudio.geluid("stinger.lach"), 1.0f, 1.0f)
                .cameraKnipVolgt(e + 554, v(0.5, 1.6, 6.0), "patguh").zeg(e + 556, "patguh", "klant", 36).animatie("patguh", e + 556, "trots")
                // Octo, freed, in the back row
                .cameraKnip(e + 594, v(5.5, 1.2, 18.5), v(7.5, 0.7, 14.5)).kijk("octo_vrij", e + 594, v(5.5, 1.2, 18.5))
                .zeg(e + 596, "octo_vrij", "emmer_terug", 46).animatie("octo_vrij", e + 596, "zucht")
                // everybody cheers, the camera pulls back over the field
                .cameraKnip(e + 644, v(0.5, 2.0, 8.0), v(0.5, 1.0, 1.0)).camera(e + 700, v(0.5, 6.0, 19.0), v(0.5, 1.0, 2.0))
                .geluid(e + 646, SpAudio.geluid("stinger.opgelost"), 1.0f, 1.0f)
                .deeltjes(e + 650, ParticleTypes.FIREWORK, v(0.5, 5.0, 2.0), 60, 4.0);
        for (int i = 0; i < 6; i++) {
            b.animatie("vrij_" + i, e + 646 + i * 2, "juich");
        }
        for (int i = 0; i < 4; i++) {
            b.animatie("bevrijd_" + i, e + 650 + i * 2, "juich");
        }
        b.animatie("krabguh", e + 650, "juich").animatie("patguh", e + 648, "juich").animatie("sponsguh_m", e + 646, "juich")
                .zwart(e + 690, e + 704).duur(e + 704);
        return b.registreer();
    }

    // =====================================================================================================================
    // after the song: Sponsguh's thanks, Gerrit comes along (anchor podium_octo). 15 s.
    // =====================================================================================================================
    private static Cutscene bedankt() {
        Vec3 speler = v(0.5, 0, 2.5);
        Cutscene.Builder b = scene("bedankt", 320).verbergEcht(20)
                .speler(speler, 0)
                .acteur("sponsguh_m", E2SponsguhFeature.SPONSGUH, v(-1.5, 0, 2.5), 0, looks(SpCast.MANAGER))
                .acteur("patguh", E2SponsguhFeature.PATGUH, v(2.6, 0, 2.4), 0, looks())
                .acteur("gerrit", E2SponsguhFeature.GERRIT, v(-7.5, 0, 1.5), -90, looks());
        kaartje(b, 0, 44, "titel");
        b.camera(0, v(0.5, 0.6, 8.0), v(0.5, 1.0, 2.5))
                .kijk("sponsguh_m", 44, speler.add(0, 1.4, 0)).kijk(Cutscene.SPELER, 44, v(-1.5, 1.0, 2.5))
                .cameraKnip(44, v(-0.5, 1.5, 6.0), v(-1.2, 1.0, 2.5)).zeg(48, "sponsguh_m", "collega", 46)
                .zeg(96, "sponsguh_m", "logeren", 42).animatie("sponsguh_m", 96, "lach")
                // Gerrit slides in from the side
                .loop("gerrit", 120, 150, v(-2.6, 0, 3.4)).cameraKnip(140, v(-2.2, 0.7, 6.0), v(-2.6, 0.3, 3.4))
                .zeg(152, "gerrit", "njeg", 20).animatie("gerrit", 152, "njeg")
                .kijk("sponsguh_m", 150, v(-2.6, 0.3, 3.4)).cameraKnip(174, v(-1.0, 1.3, 6.0), v(-2.0, 0.7, 3.0))
                .zeg(176, "sponsguh_m", "gerrit", 42)
                .cameraKnipVolgt(220, v(2.4, 1.5, 5.6), "patguh").kijk("patguh", 218, v(2.4, 1.5, 5.6))
                .zeg(222, "patguh", "waar", 34).animatie("patguh", 222, "schrik")
                .cameraKnip(258, v(0.5, 2.2, 9.0), v(0.5, 1.0, 2.5)).camera(318, v(0.5, 3.4, 12.0), v(0.5, 1.0, 2.5))
                .zeg(260, "verteller", "slot", 50).geluid(260, SpAudio.geluid("stinger.verteller"), 0.9f, 1f)
                .animatie("sponsguh_m", 262, "juich").animatie("patguh", 264, "juich").zwart(306, 320);
        return b.registreer();
    }

    /** (tests, report) the running time of a scene in seconds. */
    static double seconden(Cutscene c) {
        return c.duur() / 20.0;
    }

    /** (tests) the entity type of an actor, or null for the player's stand-in. */
    static EntityType<?> type(Cutscene c, String acteur) {
        Cutscene.Acteur a = c.acteur(acteur);
        return a == null || a.type() == null ? null : a.type().get();
    }
}
