package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: the scripts of this slice's cutscenes (CONTRACT_E2_C 5; texts:
 * tools/features/einde2_sp_d2_heen_tekst.py SCENES, keys {@code scene.guhs.einde2_sp_d2_heen_<naam>.*}). Every position is
 * relative to the scene's anchor marker and worked out from the build's own markers ({@link #r}), so a scene stands where the
 * build has its spots; the opening shots are this slice's camera markers ({@code d2h_*_camera}, line-checked by the build).
 * 1.4.4: the story waits for a click after every spoken line; the silent beats between lines run on by themselves.
 * <p>
 * Cast actors carry their story looks in their entity data ({@link #look}: {@link SpCast#IJSJE} on the sundae eaters); the
 * animations named here are the phase-A ones or the ones CONTRACT_E2_C 7.2 gives sp_cast (an unknown name plays nothing).
 * Kroeg Mikas (GuhNpcEntity Kinds of sp_cast) only use the engine's own moves ({@code buk}), as E2AnimatieGameTests wants.
 * Lengths: {@link #LENGTES} (running time without the waits).
 */
public final class D2Scenes {
    public static final String ID = "einde2_sp_d2_heen_";
    private static final String BB = D2Plek.BB, SP = D2Plek.SP;

    public static Cutscene MANAGER, IJSSALON, KNIPOOG_IJS, BEVROREN, RIT, KROEG, BABY, SLEUTEL_TERUG;
    /** Scene name -> {least, most} seconds it may last (CONTRACT_E2_C 5.2; checked by the game test). */
    public static final Map<String, int[]> LENGTES = Map.of(
            "manager", new int[] {20, 30}, "ijssalon", new int[] {20, 31}, "knipoog_ijs", new int[] {5, 10},
            "bevroren", new int[] {45, 60}, "rit", new int[] {10, 16}, "kroeg", new int[] {15, 27},
            "baby", new int[] {15, 25}, "sleutel_terug", new int[] {10, 15});

    private D2Scenes() {
    }

    static void registreer() {
        MANAGER = manager();
        IJSSALON = ijssalon();
        KNIPOOG_IJS = knipoogIjs();
        BEVROREN = bevroren();
        RIT = rit();
        KROEG = kroeg();
        BABY = baby();
        SLEUTEL_TERUG = sleutelTerug();
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** The template position of a marker (null: not in the sidecar, e.g. before the chain wrote this slice's markers). */
    private static BlockPos pos(String structuur, String id) {
        Plekken.Plek p = Plekken.plek(structuur, id);
        return p == null ? null : p.pos();
    }

    /**
     * Where block {@code b} (template coordinates) is, seen from marker {@code anker} (the middle of its floor), plus an
     * offset: the middle of b's floor.
     */
    static Vec3 r(String structuur, String anker, BlockPos b, double dx, double dy, double dz) {
        BlockPos a = pos(structuur, anker);
        if (a == null || b == null) {
            return new Vec3(0.5 + dx, dy, 0.5 + dz);
        }
        return new Vec3(b.getX() - a.getX() + 0.5 + dx, b.getY() - a.getY() + dy, b.getZ() - a.getZ() + 0.5 + dz);
    }

    /** The same for a marker (a marker that is not there counts as the anchor itself). */
    static Vec3 r(String structuur, String anker, String plek, double dx, double dy, double dz) {
        return r(structuur, anker, pos(structuur, plek), dx, dy, dz);
    }

    /** The eye (first point) and the look-at point (last point) of a camera marker, relative to the anchor; fallback when unknown. */
    static Vec3[] camera(String structuur, String anker, String id, Vec3 ogFallback, Vec3 kijkFallback) {
        Plekken.Plek p = Plekken.plek(structuur, id + "_camera");
        if (p == null || p.punten().size() < 2 || pos(structuur, anker) == null) {
            return new Vec3[] {ogFallback, kijkFallback};
        }
        BlockPos og = p.punten().get(0), kijk = p.punten().get(p.punten().size() - 1);
        return new Vec3[] {r(structuur, anker, og, 0, 0.5, 0), r(structuur, anker, kijk, 0, 0.5, 0)};
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static Cutscene.Builder scene(String naam, int duur) {
        return Cutscene.maak(ID + naam).duur(duur).bij(SpVerhaal.KROON).kaart(SpD2Heen.KAART).cameraOntwijkt();
    }

    /** Entity data of a cast actor with these story looks on (CONTRACT_E2_C 7.2: an actor's NBT carries the look state). */
    static Consumer<CompoundTag> look(String... looks) {
        return tag -> {
            // hook: sp_cast keeps the looks as synced state of SponsguhWezen; until then the stand-in of SpCast reads them from the
            // persistent data (NeoForgeData), which an actor's save tag carries just the same
            CompoundTag data = tag.getCompoundOrEmpty("NeoForgeData").copy();
            for (String l : looks) {
                data.putBoolean("guhs_einde2_sp_cast_look_" + l, true);
            }
            tag.put("NeoForgeData", data);
        };
    }

    private static Consumer<CompoundTag> vlag(String naam, boolean aan) {
        return tag -> tag.putBoolean(naam, aan);
    }

    private static Consumer<CompoundTag> soort(int s) {
        return tag -> tag.putInt("Soort", s);
    }

    private static Supplier<SoundEvent> s(SoundEvent e) {
        return () -> e;
    }

    // =====================================================================================================================
    // step 0: the new manager (anchor d2h_zaal, the middle of the hall of De Krokante Knabbel). ~29 s
    // =====================================================================================================================

    private static Cutscene manager() {
        String a = "d2h_zaal";
        Vec3 krabguh = v(0.5, 0, -4.5), octo = r(BB, a, "octo_kassa", 0, 0, 0), sponsguh = v(-1.5, 0, -0.5), speler = v(1.8, 0, 0.3);
        Vec3 deur = v(0.0, 0, 8.0), patguh = v(-0.6, 0, 1.6);
        Vec3[] cam = camera(BB, a, "d2h_manager", v(6.5, 3.5, 5.5), v(0.5, 1.5, -3.5));
        Cutscene.Builder b = scene("manager", 576).verbergEcht(14)
                .speler(speler, 180)
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, krabguh, 0)
                .acteur("octo", E2SponsguhFeature.OCTO_MIKA, octo, 0)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 180, look(SpCast.PETJE))
                .acteur("patguh", E2SponsguhFeature.PATGUH, deur, 180)
                .acteur("klant_1", E2SponsguhFeature.BIKINIBODEM_GUH, r(BB, a, "klant_1", 0, 0, 0), 90, soort(1))
                .acteur("klant_2", E2SponsguhFeature.BIKINIBODEM_GUH, r(BB, a, "klant_2", 0, 0, 0), 90, soort(3));
        b.zwart(0, 14)
                .camera(0, cam[0], cam[1]).camera(80, cam[0].add(-1.5, -0.6, -1.5), cam[1])
                // Krabguh: the big news
                .cameraKnipVolgt(14, krabguh.add(2.0, 1.4, 3.0), "krabguh").zeg(14, "krabguh", "nieuws", 60).animatie("krabguh", 14, "juich")
                .geluid(16, s(SoundEvents.NOTE_BLOCK_BELL.value()), 0.7f, 1.2f)
                // Sponsguh whispers to you: his manager face
                .kijk("sponsguh", 74, speler.add(0, 1.4, 0)).cameraKnipVolgt(74, sponsguh.add(0.6, 1.3, -1.8), "sponsguh")
                .zeg(76, "sponsguh", "fluister", 56).animatie("sponsguh", 76, "lach")
                .kijk("sponsguh", 132, krabguh.add(0, 1, 0))
                // the drum roll... Octo
                .cameraKnipVolgt(132, krabguh.add(1.5, 1.3, 2.4), "krabguh").zeg(134, "krabguh", "wordt", 40)
                .geluid(136, s(SoundEvents.NOTE_BLOCK_SNARE.value()), 0.8f, 1.0f).geluid(146, s(SoundEvents.NOTE_BLOCK_SNARE.value()), 0.8f, 1.1f)
                .geluid(156, s(SoundEvents.NOTE_BLOCK_SNARE.value()), 0.8f, 1.2f).geluid(166, s(SoundEvents.NOTE_BLOCK_SNARE.value()), 0.9f, 1.3f)
                .cameraKnipVolgt(176, octo.add(1.2, 2.2, 3.2), "octo").zeg(176, "krabguh", "octo", 36)
                .geluid(176, s(SoundEvents.NOTE_BLOCK_BELL.value()), 0.9f, 0.8f)
                .zeg(214, "octo", "zucht", 50).animatie("octo", 214, "zucht")
                // Sponsguh: but...
                .cameraKnipVolgt(264, sponsguh.add(0.4, 1.2, -1.6), "sponsguh").zeg(266, "sponsguh", "maar", 52).animatie("sponsguh", 266, "schrik")
                // just a kid
                .cameraKnipVolgt(318, sponsguh.add(-0.6, 1.5, 0.9), "krabguh").zeg(320, "krabguh", "kind", 58)
                // the ears droop (the narrator)
                .cameraKnip(378, sponsguh.add(0.2, 1.0, -1.4), sponsguh.add(0, 1.1, 0)).camera(430, sponsguh.add(0.2, 0.8, -1.1), sponsguh.add(0, 0.9, 0))
                .zeg(380, "verteller", "oortjes", 50)
                // Patguh bursts in
                .loop("patguh", 412, 456, patguh).cameraKnipVolgt(430, v(1.5, 1.8, 4.5), "patguh")
                .zeg(432, "patguh", "ijs", 58).animatie("patguh", 432, "juich").kijk("sponsguh", 432, patguh.add(0, 1.2, 0))
                .kijk("patguh", 490, speler.add(0, 1.4, 0)).cameraKnipVolgt(490, speler.add(-1.2, 1.6, -0.6), "patguh")
                .zeg(492, "patguh", "mee", 60).animatie("sponsguh", 500, "juich")
                .zwart(556, 576);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 1: the ice cream (anchor ijs_speler, your stool). THE line of CONTRACT_E2_C 5.3 with a silent beat before and
    // after it. ~30 s
    // =====================================================================================================================

    private static Cutscene ijssalon() {
        String a = "ijs_speler";
        Vec3 speler = v(0.5, 1, 0.5), sponsguh = r(BB, a, "ijs_sponsguh", 0, 0, 0), patguh = r(BB, a, "ijs_patguh", 0, 0, 0);
        Vec3 verkoper = r(BB, a, "ijsverkoper", 0, 0, 0);
        Vec3[] cam = camera(BB, a, "d2h_ijs", v(-0.5, 2.5, -3.5), v(-1.5, 1.5, 0.5));
        Vec3 sHoofd = sponsguh.add(0, 1.0, 0), pHoofd = patguh.add(0, 1.3, 0);
        Cutscene.Builder b = scene("ijssalon", 604).verbergEcht(10)
                .speler(speler, 180)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 180, look(SpCast.IJSJE))
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, 180, look(SpCast.IJSJE))
                .acteur("ijsverkoper", E2SponsguhFeature.BIKINIBODEM_GUH, verkoper, 0, soort(2));
        b.zwart(0, 14).geluid(2, SpAudio.geluid("deuntje.ijssalon"), 0.6f, 1.0f)
                .camera(0, cam[0], cam[1])
                // the three towers land on the bar
                .cameraKnipVolgt(12, v(-2.5, 2.4, 2.4), "ijsverkoper").zeg(14, "ijsverkoper", "torens", 54).animatie("ijsverkoper", 14, "praat")
                .geluid(40, s(SoundEvents.WOOD_PLACE), 1.0f, 0.6f).schud(40, 0.3f, 8)
                // Patguh looks up the tower
                .cameraKnip(70, patguh.add(0.1, 0.8, -1.6), patguh.add(0, 3.2, -0.6)).camera(118, patguh.add(0.1, 0.9, -1.5), patguh.add(0, 4.6, -0.6))
                .zeg(70, "patguh", "top", 50)
                // Sponsguh: beautiful
                .cameraKnipVolgt(122, sponsguh.add(0.3, 1.2, -1.9), "sponsguh").zeg(122, "sponsguh", "mooi", 52).animatie("sponsguh", 122, "huil_blij")
                // the silent beat: the camera creeps up to his face...
                .camera(176, sponsguh.add(0.3, 1.2, -1.9), sHoofd).camera(206, sponsguh.add(0.1, 1.05, -1.05), sHoofd)
                .animatie("sponsguh", 190, "knipper")
                // ...THE line
                .zeg(206, "sponsguh", "poppetje", 60)
                // hard cut: the whole parlour stands still; a scoop clatters
                .cameraKnip(266, cam[0], v(-2.0, 1.6, 0.5)).geluid(270, s(SoundEvents.WOOD_FALL), 1.0f, 1.4f)
                .kijk("patguh", 266, sHoofd).kijk("ijsverkoper", 266, sHoofd)
                .cameraKnipVolgt(284, verkoper.add(0.2, 2.0, 2.6), "ijsverkoper").zeg(286, "ijsverkoper", "speelgoed", 54)
                .cameraKnipVolgt(340, patguh.add(0.4, 1.4, -1.6), "patguh").zeg(342, "patguh", "gelijk", 54)
                // Sponsguh does not mind: the ice IS the toy
                .cameraKnipVolgt(396, sponsguh.add(0.3, 1.2, -1.6), "sponsguh").zeg(398, "sponsguh", "hap", 58).animatie("sponsguh", 398, "slurp")
                .geluid(420, s(SoundEvents.GENERIC_EAT.value()), 1.0f, 1.2f).schud(426, 0.25f, 8)
                .deeltjes(426, ParticleTypes.SNOWFLAKE, sHoofd.add(0, 0.4, 0), 20, 0.4)
                // and they chant
                .camera(456, cam[0], v(-2.0, 1.6, 0.5)).camera(510, cam[0].add(-0.8, -0.2, 0.4), v(-2.0, 1.6, 0.5))
                .zeg(458, "samen", "zingen", 58).animatie("sponsguh", 458, "juich").animatie("patguh", 458, "juich")
                .geluid(460, s(SoundEvents.NOTE_BLOCK_BELL.value()), 0.8f, 1.0f).geluid(470, s(SoundEvents.NOTE_BLOCK_BELL.value()), 0.8f, 1.26f)
                .geluid(480, s(SoundEvents.NOTE_BLOCK_BELL.value()), 0.8f, 1.5f)
                .deeltjes(462, ParticleTypes.NOTE, sHoofd.add(0, 0.6, 0), 4, 0.4).deeltjes(474, ParticleTypes.NOTE, pHoofd.add(0, 0.6, 0), 4, 0.4)
                // the hat, on the house
                .cameraKnipVolgt(516, verkoper.add(0.6, 2.0, 2.6), "ijsverkoper").zeg(518, "ijsverkoper", "hoedje", 60).animatie("ijsverkoper", 518, "praat")
                .deeltjes(540, ParticleTypes.HAPPY_VILLAGER, speler.add(0, 2.1, 0), 10, 0.3)
                .zwart(588, 604);
        return b.registreer();
    }

    // =====================================================================================================================
    // wink 8: Baltoguh behind his towering sundae (anchor knipoog_ijs_coupe). ~8 s
    // =====================================================================================================================

    private static Cutscene knipoogIjs() {
        String a = "knipoog_ijs_coupe";
        Vec3 balto = r(BB, a, "knipoog_ijs_balto", 0, 0, 0), kistje = r(BB, a, "knipoog_ijs_kistje", 0, 0, 0);
        Vec3[] cam = camera(BB, a, "knipoog_ijs", v(6.5, 2.5, 4.5), v(0.5, 2.5, 0.5));
        Cutscene.Builder b = scene("knipoog_ijs", 170).verbergEcht(4)
                .guh("balto", GuhVariant.BALTOGUH, balto, -90);
        b.zwart(0, 10)
                .camera(0, cam[0], balto.add(0, 0.8, 0)).camera(60, cam[0].add(-2.0, -0.3, -1.5), balto.add(0, 0.8, 0))
                .zeg(10, "balto", "ijs", 46)
                .zeg(58, "balto", "zoeken", 44).animatie("balto", 58, "zwaai")
                .cameraKnip(104, kistje.add(2.0, 1.0, 1.5), kistje.add(0, 0.2, 0)).zeg(106, "verteller", "kistje", 50)
                .zwart(158, 170);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 2: the crown is gone, Krabguh frozen (anchor neptuguh_troon). Big: ~57 s
    // =====================================================================================================================

    private static Cutscene bevroren() {
        String a = "neptuguh_troon";
        Vec3 troon = v(0.5, 0, 0.5), weg = v(0.5, -40, 0.5);
        Vec3 krabguh = r(BB, a, "bevroren_krabguh", 0, 0, 0);
        Vec3 sponsguh = v(5.0, 0, -3.5), patguh = v(7.0, 0, -3.0), speler = v(6.0, 0, -4.8), plank = v(9.5, 0, -0.5);
        Vec3 nHoofd = troon.add(0, 2.8, 0);
        Vec3[] cam = camera(BB, a, "d2h_plein", v(9.5, 3.5, -8.5), v(1.5, 2.5, 0.5));
        float naarKoning = D2Plek.yaw(sponsguh, troon);
        Cutscene.Builder b = scene("bevroren", 1150).verbergEcht(18)
                .acteur("neptuguh_kroon", E2SponsguhFeature.NEPTUGUH, troon, -130, vlag("Kroon", true))
                .acteur("neptuguh", E2SponsguhFeature.NEPTUGUH, weg, -130, vlag("Kroon", false))
                .acteur("plankmika", E2SponsguhFeature.PLANKMIKA, v(7.0, 0, 6.0), 160)
                .acteur("krabguh", E2SponsguhFeature.MENEER_KRABGUH, krabguh.add(0, -40, 0), 80, vlag("Bevroren", false))
                .acteur("krabguh_ijs", E2SponsguhFeature.MENEER_KRABGUH, krabguh.add(0, -40, 0), 80, vlag("Bevroren", true))
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh.add(0, -40, 0), naarKoning)
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh.add(0, -40, 0), naarKoning)
                .speler(speler.add(0, -40, 0), naarKoning);
        int[][] menigte = {{3, -7}, {5, -8}, {9, -6}, {10, -2}, {9, 2}};
        for (int i = 0; i < menigte.length; i++) {
            Vec3 m = v(menigte[i][0] + 0.5, 0, menigte[i][1] + 0.5);
            b.acteur("burger_" + i, E2SponsguhFeature.BIKINIBODEM_GUH, m.add(0, -40, 0), D2Plek.yaw(m, troon), soort(i));
            b.loop("burger_" + i, 300, 301, m);
        }
        // --- the night: Plankmika takes the crown, posts it to Schelpenstad and leaves the empty cushion with Krabguh -------
        b.zwart(0, 30).geluid(4, SpAudio.geluid("stinger.verteller"), 1.0f, 1.0f)
                .camera(0, troon.add(6.0, 1.2, 4.0), nHoofd).camera(94, troon.add(4.5, 1.0, 3.0), nHoofd)
                .zeg(34, "verteller", "nacht", 60)
                .loop("plankmika", 96, 150, v(2.0, 0, 1.5)).cameraKnipVolgt(96, v(4.5, 0.7, 4.5), "plankmika")
                .zeg(98, "plankmika", "plan", 52).animatie("plankmika", 98, "plan")
                .cameraKnip(150, troon.add(3.5, 2.2, 2.5), troon.add(0.3, 2.7, 0.3))
                .deeltjes(158, ParticleTypes.END_ROD, troon.add(0, 3.1, 0), 12, 0.3).geluid(158, s(SoundEvents.AMETHYST_BLOCK_CHIME), 1.0f, 1.4f)
                .loop("neptuguh_kroon", 160, 161, weg).loop("neptuguh", 160, 161, troon)
                .cameraKnipVolgt(164, v(3.6, 0.7, 3.4), "plankmika").zeg(166, "plankmika", "pakket", 52).animatie("plankmika", 166, "lach_kwaad")
                .loop("plankmika", 222, 290, v(14.0, 0, -6.0))
                .cameraKnip(220, cam[0], cam[1]).zeg(222, "verteller", "verstopt", 54)
                // --- the next morning -----------------------------------------------------------------------------------------------
                .zwart(278, 318).zeg(284, "verteller", "ochtend", 32)
                .loop("plankmika", 300, 301, plank).loop("krabguh", 300, 301, krabguh).loop("sponsguh", 300, 301, sponsguh)
                .loop("patguh", 300, 301, patguh).loop(Cutscene.SPELER, 300, 301, speler)
                .kijk("plankmika", 302, nHoofd).kijk("krabguh", 302, nHoofd)
                // the horn: WHO STOLE MY CROWN?
                .cameraKnipVolgt(320, troon.add(4.0, 1.4, -2.5), "neptuguh")
                .geluid(320, s(SoundEvents.RAID_HORN.value()), 1.0f, 1.0f).schud(320, 0.6f, 25)
                .zeg(322, "neptuguh", "wie", 50).animatie("neptuguh", 320, "blaas")
                .cameraKnipVolgt(374, plank.add(-1.2, 0.8, -1.8), "plankmika").zeg(376, "plankmika", "zag", 50).animatie("plankmika", 376, "plan")
                .cameraKnipVolgt(428, krabguh.add(-1.2, 1.5, -2.0), "krabguh").zeg(428, "krabguh", "kussentje", 58).animatie("krabguh", 428, "lach")
                .cameraKnipVolgt(488, troon.add(3.5, 1.6, 2.5), "neptuguh").zeg(488, "neptuguh", "waar", 40).schud(490, 0.3f, 10)
                .cameraKnipVolgt(530, krabguh.add(-1.2, 1.5, -2.0), "krabguh").zeg(530, "krabguh", "weet_niet", 60).animatie("krabguh", 530, "paniek")
                // Plankmika, aside to us
                .kijk("plankmika", 590, plank.add(0, 0.4, -1.5)).cameraKnip(590, plank.add(0, 0.7, -1.5), plank.add(0, 0.4, 0))
                .zeg(592, "plankmika", "fluister", 40).animatie("plankmika", 592, "lach_kwaad")
                // THIEF! The trident: Krabguh freezes
                .cameraKnipVolgt(632, troon.add(4.0, 1.4, -2.5), "neptuguh").zeg(634, "neptuguh", "dief", 40).animatie("neptuguh", 640, "bevries")
                .cameraKnip(652, krabguh.add(2.0, 1.8, -2.5), krabguh.add(0, 0.9, 0))
                .geluid(650, SpAudio.geluid("stinger.bevries"), 1.0f, 1.0f).geluid(650, s(SoundEvents.GLASS_BREAK), 0.8f, 0.6f).schud(650, 0.8f, 15)
                .deeltjes(650, ParticleTypes.SNOWFLAKE, krabguh.add(0, 1.0, 0), 50, 0.8)
                .loop("krabguh", 652, 653, krabguh.add(0, -40, 0)).loop("krabguh_ijs", 652, 653, krabguh)
                // Sponsguh: he would never!
                .loop("sponsguh", 672, 700, krabguh.add(-0.5, 0, -2.2)).cameraKnipVolgt(676, krabguh.add(-2.4, 1.3, -3.6), "sponsguh")
                .zeg(678, "sponsguh", "nooit", 58).animatie("sponsguh", 678, "rennen")
                .kijk("sponsguh", 700, nHoofd)
                .cameraKnipVolgt(738, troon.add(3.5, 1.6, -1.5), "neptuguh").zeg(738, "neptuguh", "schelpenstad", 60)
                // we will get it back!
                .kijk("sponsguh", 798, speler.add(0, 1.4, 0)).cameraKnip(798, troon.add(3.0, 1.8, -2.0), v(6.0, 1.1, -3.5))
                .zeg(800, "sponsguh", "wij", 55).animatie("sponsguh", 800, "salueer")
                .kijk("sponsguh", 856, nHoofd)
                .cameraKnipVolgt(855, v(8.5, 1.5, -7.5), "neptuguh").zeg(857, "neptuguh", "lach", 45).animatie("neptuguh", 857, "lach")
                .zeg(904, "neptuguh", "zonsondergang", 62)
                .cameraKnipVolgt(966, patguh.add(-1.4, 1.7, 1.2), "patguh").zeg(968, "patguh", "zeepbel", 58)
                .cameraKnipVolgt(1026, troon.add(3.0, 2.0, -1.0), "neptuguh").zeg(1028, "neptuguh", "probleem", 38)
                // off to the car
                .cameraKnip(1066, cam[0], v(6.0, 1.2, -3.5)).zeg(1068, "sponsguh", "mobiel", 50).animatie("sponsguh", 1070, "rennen")
                .loop("sponsguh", 1090, 1150, v(18.0, 0, -6.0)).loop("patguh", 1094, 1150, v(18.0, 0, -5.0))
                .loop(Cutscene.SPELER, 1098, 1150, v(18.0, 0, -7.0))
                .zwart(1120, 1150);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 3: into the car (anchor mobiel_start, in front of De Krokante Knabbel). ~15 s
    // =====================================================================================================================

    private static Cutscene rit() {
        String a = "mobiel_start";
        Vec3 sponsguh = v(-0.6, 0, -2.2), patguh = v(1.6, 0, -2.2), speler = v(0.5, 0, -3.6);
        Vec3[] cam = camera(BB, a, "d2h_mobiel", v(6.5, 2.5, 5.5), v(0.5, 1.5, 0.5));
        Cutscene.Builder b = scene("rit", 310).verbergEcht(8)
                .acteur("mobiel", E2SponsguhFeature.KNABBELBURGERMOBIEL, v(0.5, 0, 0.5), -90)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 0)
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, 0)
                .speler(speler, 0);
        b.zwart(0, 14)
                .camera(0, cam[0], cam[1])
                .cameraKnipVolgt(12, sponsguh.add(-0.3, 1.3, 1.5), "sponsguh").zeg(14, "sponsguh", "kijk", 56).animatie("sponsguh", 14, "juich")
                .kijk("patguh", 70, sponsguh.add(0, 1, 0)).cameraKnipVolgt(70, patguh.add(0.3, 1.5, 1.5), "patguh").zeg(72, "patguh", "wie", 40)
                .kijk("sponsguh", 112, patguh.add(0, 1.2, 0)).cameraKnipVolgt(112, sponsguh.add(-0.3, 1.3, 1.5), "sponsguh")
                .zeg(114, "sponsguh", "voorlopig", 58).animatie("sponsguh", 114, "schrik")
                .kijk("sponsguh", 172, speler.add(0, 1.5, 0)).kijk("patguh", 172, speler.add(0, 1.5, 0))
                .cameraKnipVolgt(172, speler.add(0.4, 1.6, 2.4), Cutscene.SPELER).zeg(174, "patguh", "jij", 36).animatie("patguh", 174, "juich")
                .cameraKnip(210, v(8.0, 2.5, 1.5), v(-2.0, 1.0, -1.0)).zeg(212, "sponsguh", "weg", 64)
                .loop("sponsguh", 270, 296, v(0.8, 0, -1.0)).loop("patguh", 270, 296, v(0.2, 0, 1.8)).loop(Cutscene.SPELER, 272, 298, v(-0.2, 0, -1.0))
                .zwart(290, 310);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 3 end: arrival at De Ruige Kroeg, the key snatched (anchor kroeg_parkeer). ~27 s
    // =====================================================================================================================

    private static Cutscene kroeg() {
        String a = "kroeg_parkeer";
        Vec3 deur = r(SP, a, "kroeg_sluip", 0, 0, 0), slaper = r(SP, a, "slapende_mika", 0, 0, 0);
        Vec3 sponsguh = v(-2.0, 0, -2.2), patguh = v(-3.2, 0, -1.0), speler = v(-0.8, 0, -1.0);
        float naarDeur = D2Plek.yaw(sponsguh, deur);
        Vec3[] cam = camera(SP, a, "d2h_kroeg", v(6.5, 3.5, 5.5), v(-4.5, 1.5, -3.5));
        Cutscene.Builder b = scene("kroeg", 540).verbergEcht(16)
                .acteur("mobiel", E2SponsguhFeature.KNABBELBURGERMOBIEL, v(0.5, 0, 0.5), 180)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, naarDeur)
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, naarDeur)
                .speler(speler, naarDeur)
                .npc("mika", Kind.SLAPENDE_MIKA, deur, 180);
        Vec3 sVoor = sponsguh.add(-1.2, 1.3, -1.0);
        b.zwart(0, 14)
                .camera(0, cam[0], cam[1])
                .cameraKnipVolgt(12, sVoor, "sponsguh").zeg(14, "sponsguh", "kroeg", 54)
                .cameraKnipVolgt(68, patguh.add(-1.2, 1.5, -0.8), "patguh").zeg(70, "patguh", "stoer", 50).animatie("patguh", 70, "trots")
                .kijk("sponsguh", 120, speler.add(0, 1.5, 0)).cameraKnipVolgt(120, sVoor, "sponsguh").zeg(122, "sponsguh", "sleutel", 56)
                .animatie("sponsguh", 122, "juich")
                // the tough Mika comes out and takes the key
                .loop("mika", 150, 205, sponsguh.add(-0.9, 0, -0.9)).cameraKnip(178, v(1.5, 2.2, 0.5), sponsguh.add(-2.5, 1.2, -2.5))
                .kijk("sponsguh", 190, deur.add(0, 1.6, 0))
                .cameraKnipVolgt(206, sponsguh.add(0.6, 1.8, 0.4), "mika").zeg(208, "mika", "mooi", 44)
                .geluid(210, s(SoundEvents.CHAIN_PLACE), 1.0f, 1.6f)
                .cameraKnipVolgt(252, sVoor, "sponsguh").zeg(254, "sponsguh", "he", 42).animatie("sponsguh", 254, "schrik")
                .cameraKnipVolgt(296, sponsguh.add(0.6, 1.8, 0.4), "mika").zeg(298, "mika", "baby", 50)
                .loop("mika", 330, 372, deur)
                // five minutes later: asleep at his table, the key on his belt
                .zwart(360, 392).zeg(364, "verteller", "later", 28)
                .loop("mika", 380, 381, slaper).animatie("mika", 384, "buk")
                .cameraKnip(394, deur.add(0.0, 1.8, -0.2), slaper.add(0, 0.8, 0)).zeg(396, "verteller", "slaapt", 54)
                .geluid(420, s(SoundEvents.FOX_SLEEP), 0.8f, 0.6f)
                // back outside: the three peek
                .cameraKnip(452, sponsguh.add(-2.8, 1.6, -1.8), sponsguh.add(0, 1.1, 0))
                .kijk("patguh", 452, deur.add(0, 1.4, 0)).zeg(454, "patguh", "makkie", 36).animatie("patguh", 454, "juich")
                .kijk("sponsguh", 490, patguh.add(0, 1.2, 0)).zeg(492, "sponsguh", "tien", 46)
                .zwart(526, 540);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 4: inside: who was the baby? (anchor kroeg_sluip, just inside the swing gates). ~24 s
    // =====================================================================================================================

    private static Cutscene baby() {
        String a = "kroeg_sluip";
        Vec3 speler = v(0.5, 0, 0.3), sponsguh = v(1.6, 0, 0.3), patguh = v(2.7, 0, 0.3), uitsmijter = v(-1.6, 0, 0.2);
        Vec3 barman = r(SP, a, "barman", 0, 0, 0);
        Vec3[] cam = camera(SP, a, "d2h_binnen", v(9.5, 3.5, -1.5), v(-0.5, 1.5, -6.5));
        Cutscene.Builder b = scene("baby", 490).verbergEcht(20)
                .speler(speler, 180)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 180)
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, 180)
                .npc("barman", Kind.KROEG_BARMAN_MIKA, barman, 0)
                .npc("uitsmijter", Kind.KROEG_UITSMIJTER_MIKA, uitsmijter, -90)
                .npc("slaper", Kind.SLAPENDE_MIKA, r(SP, a, "slapende_mika", 0, 0, 0), -90);
        java.util.List<String> mikas = new java.util.ArrayList<>(java.util.List.of("barman", "uitsmijter"));
        for (int i = 1; i <= 6; i++) {
            Plekken.Plek m = Plekken.plek(SP, "stoere_mika_" + i);
            b.npc("stoer_" + i, Kind.STOERE_MIKA, r(SP, a, "stoere_mika_" + i, 0, 0, 0), m == null ? 0 : m.yaw());
            mikas.add("stoer_" + i);
        }
        Vec3 bubbel = sponsguh.add(0, 1.4, 0);
        b.zwart(0, 14)
                .camera(0, cam[0], cam[1]).camera(70, cam[0].add(-1.5, -0.4, 0.5), cam[1])
                .animatie("slaper", 2, "buk")
                .zeg(14, "verteller", "ruikt", 56)
                .cameraKnipVolgt(70, sponsguh.add(0.4, 1.3, -1.7), "sponsguh").zeg(72, "sponsguh", "stoer_doen", 50)
                // a bubble escapes...
                .cameraKnip(122, sponsguh.add(0.3, 1.4, -1.4), bubbel.add(0, 0.5, 0))
                .deeltjes(124, ParticleTypes.BUBBLE_POP, bubbel, 6, 0.1).deeltjes(132, ParticleTypes.BUBBLE_POP, bubbel.add(0, 0.6, 0), 6, 0.1)
                .geluid(126, s(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP), 1.0f, 1.3f).geluid(134, s(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP), 1.0f, 1.6f);
        for (String m : mikas) {
            b.kijk(m, 138, bubbel);                                   // the whole bar turns to look
        }
        b.cameraKnipVolgt(138, barman.add(0.0, 1.8, 2.2), "barman").zeg(140, "barman", "wie", 44)
                .cameraKnipVolgt(184, speler.add(-0.4, 1.9, -0.9), "uitsmijter").zeg(186, "uitsmijter", "baby", 44)
                .cameraKnip(230, v(1.5, 2.4, 0.9), v(1.0, 1.2, -8.0)).zeg(232, "kroeg", "allemaal", 44).schud(234, 0.5f, 15)
                .zeg(278, "verteller", "stil", 60);
        for (String m : mikas) {
            b.kijk(m, 312, barman.add(0, 1.0, -4.0));                 // and away again, to the bar
        }
        b.cameraKnipVolgt(340, patguh.add(0.4, 1.4, -1.7), "patguh").zeg(342, "patguh", "huiswerk", 58)
                .kijk("patguh", 342, speler.add(0, 1.5, 0))
                .cameraKnipVolgt(402, sponsguh.add(0.4, 1.3, -1.7), "sponsguh").zeg(404, "sponsguh", "glas", 64)
                .geluid(408, s(SoundEvents.AMETHYST_BLOCK_CHIME), 1.0f, 1.8f).geluid(416, s(SoundEvents.AMETHYST_BLOCK_CHIME), 1.0f, 2.0f)
                .kijk("sponsguh", 404, speler.add(0, 1.5, 0))
                .zwart(474, 490);
        return b.registreer();
    }

    // =====================================================================================================================
    // step 4 end: the key back, outside by the bench (anchor rustpunt_1 of Schelpenstad). ~11 s
    // =====================================================================================================================

    private static Cutscene sleutelTerug() {
        String a = "rustpunt_1";
        Vec3 speler = v(0.5, 0, 0.5), sponsguh = v(2.0, 0, -0.5), patguh = v(2.0, 0, 1.5);
        Cutscene.Builder b = scene("sleutel_terug", 214).verbergEcht(10)
                .speler(speler, -90)
                .acteur("sponsguh", E2SponsguhFeature.SPONSGUH, sponsguh, 90)
                .acteur("patguh", E2SponsguhFeature.PATGUH, patguh, 90)
                .acteur("mobiel", E2SponsguhFeature.KNABBELBURGERMOBIEL, r(SP, a, "kroeg_parkeer", 0, 0, 0), 180);
        b.zwart(0, 12)
                .camera(0, v(4.5, 2.2, 3.5), v(1.2, 1.2, 0.5))
                .geluid(10, s(SoundEvents.CHAIN_PLACE), 1.0f, 1.6f)
                .cameraKnipVolgt(12, sponsguh.add(-1.4, 1.3, -0.4), "sponsguh").zeg(14, "sponsguh", "held", 56).animatie("sponsguh", 14, "juich")
                .cameraKnipVolgt(70, patguh.add(-1.4, 1.5, 0.4), "patguh").zeg(72, "patguh", "stil", 56).animatie("patguh", 72, "trots")
                .cameraKnip(128, v(-2.5, 2.0, 0.5), v(2.0, 1.2, 0.5)).zeg(130, "sponsguh", "verder", 64).animatie("sponsguh", 130, "salueer")
                .zwart(200, 214);
        return b.registreer();
    }

    /** The scenes of this slice by name (dev command, tests). */
    static Cutscene van(String naam) {
        return Cutscene.van(ID + naam);
    }
}
