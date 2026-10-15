package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 phase B, slice h1_lucht: the eight cutscenes of chapter 1 (CONTRACT_E2_B 5), main's engine. Every scene stands
 * still after each spoken line until the viewer clicks (1.4.4); the lengths below are the running time without the waits.
 * Positions are relative to the anchor named with each scene (template coordinates of the temple: the copy is never turned).
 * Texts: tools/features/einde2_h1_lucht_tekst.py {@code SCENES}.
 * <ul>
 *   <li>{@link #KAASBOL} (57 s, big): the ball drifts in, opens; Aang-guh and Appa-guh; your eyes glow; the Korra joke.
 *       Anchor: the ball.</li>
 *   <li>{@link #INTRO} (51 s, big): "Water. Aarde. Vuur. Lucht." Visions of the four peoples in the sky. Anchor: the ball.</li>
 *   <li>{@link #AANKOMST} (16.5 s): the temple from the south, Aang-guh in the gate. Anchor: {@code aankomst_speler}.</li>
 *   <li>{@link #HAL} (29.75 s): Roku-guh and Kyoshi-guh, the empty pedestal. Anchor: {@code voetstuk_leeg}.</li>
 *   <li>{@link #MOMO} (14.5 s): the lemurguhtje gives the knabbel back and chooses you. Anchor: {@code momo_eind}.</li>
 *   <li>{@link #ZUKA_STEIGER} (29.5 s): the ship rams the pier, "Eindelijk, de Guhvatar...". Anchor: {@code steiger}.</li>
 *   <li>{@link #GELEERD} (23 s): the fireball blown aside, Appa takes off. Anchor: {@code appa_kamp}.</li>
 *   <li>{@link #KNIPOOG_SJOKKEL} (9 s): wink 1, Sjokkel starts his race. Anchor: the look-at point of {@code knipoog_sjokkel_camera}.</li>
 * </ul>
 */
public final class H1Scenes {
    private static final String LIJN = E2Verhaal.LUCHT, KAART = E2H1Lucht.KAART;

    private H1Scenes() {
    }

    private static Cutscene.Builder maak(String naam) {
        return Cutscene.maak("einde2_h1_lucht_" + naam).bij(LIJN).kaart(KAART).cameraOntwijkt();
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    // =====================================================================================================================
    // the kaasbol: anchor = the block of the ball. The ball drifts in from the south-east, lands, cracks and opens.
    // =====================================================================================================================
    private static final Vec3 BOL = v(0.5, 0, 0.5), SPELER = v(-3.5, 0, -2.5), AANG = v(0.5, 0, -1.5), APPA = v(4.0, 0, 3.5);

    public static final Cutscene KAASBOL = maak("kaasbol").duur(1150).verbergEcht(5)
            .acteur("bal", E2H1LuchtFeature.KAASBOL, v(14.5, 7, 20.5), 200f)
            .npc("aang", Kind.AANG_GUH, v(14.5, 7.2, 20.5), 200f)
            .acteur("appa", E2AppaFeature.APPA_GUH, v(4.0, -6, 3.5), 200f)
            .speler(SPELER, -53f)
            .zwart(0, 20)
            .camera(0, v(-6, 4, -8), v(10, 5, 14))
            .camera(140, v(-4.5, 2.4, -5.5), v(0.5, 0.8, 0.5))
            .loop("bal", 10, 140, BOL).loop("aang", 10, 140, v(0.5, 0.2, 0.5))
            .zeg(20, "", "drijft", 100)
            .geluid(140, () -> SoundEvents.WOOL_FALL, 1.0f, 0.6f)
            .deeltjes(140, ParticleTypes.POOF, v(0.5, 0.3, 0.5), 24, 0.6)
            .schud(140, 0.5f, 8)
            .zeg(150, "", "plof", 60)
            .cameraKnip(215, v(-2.6, 1.4, -2.8), v(0.5, 0.9, 0.5))
            .deeltjes(220, ParticleTypes.END_ROD, v(0.5, 1.0, 0.5), 20, 0.5)
            .geluid(220, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f)
            .zeg(222, "", "barst", 70)
            .animatie("bal", 292, "open")
            .deeltjes(296, ParticleTypes.END_ROD, v(0.5, 1.4, 0.5), 70, 0.9)
            .deeltjes(296, ParticleTypes.CLOUD, v(0.5, 1.0, 0.5), 30, 0.8)
            .geluid(296, () -> SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f)
            .schud(296, 0.7f, 12)
            .loop("aang", 330, 352, AANG).animatie("aang", 330, "spring")
            .kijk("aang", 352, v(-3.5, 1, -2.5))
            .cameraKnipVolgt(372, v(-1.4, 1.5, -3.6), "aang")
            .zeg(380, "aang", "gaap", 80)
            .animatie("aang", 380, "praat")
            .cameraKnip(462, v(-7, 3.6, -6), v(1.8, 1.4, 1.6))
            .zeg(470, "aang", "hallo", 90)
            .loop("appa", 500, 520, APPA)
            .deeltjes(500, ParticleTypes.CLOUD, v(2.5, 0.6, 2.0), 60, 1.6)
            .geluid(500, () -> SoundEvents.WOOL_PLACE, 1.0f, 0.5f)
            .kijk("appa", 522, v(-3.5, 1, -2.5))
            .zeg(570, "appa", "brom", 40)
            .geluid(570, E2AppaFeature.BRUL, 1.0f, 1.0f)
            .cameraKnipVolgt(612, v(-1.9, 1.75, -1.3), Cutscene.SPELER)
            .zeg(620, "", "ogen", 80)
            .deeltjes(626, ParticleTypes.END_ROD, v(-3.5, 1.65, -2.5), 30, 0.25)
            .deeltjes(640, ParticleTypes.ELECTRIC_SPARK, v(-3.5, 1.65, -2.5), 20, 0.3)
            .geluid(626, () -> SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.4f)
            .cameraKnipVolgt(702, v(-1.2, 1.4, -3.9), "aang")
            .zeg(710, "aang", "guhvatar", 80)
            .animatie("aang", 712, "spring")
            .zeg(800, "aang", "korra", 60)
            .zeg(870, "aang", "korra2", 70)
            .animatie("aang", 872, "lach")
            .cameraKnip(942, v(-7.5, 3.2, -5.5), v(0.5, 1.1, 0.5))
            .zeg(950, "aang", "stokje", 100)
            .camera(1040, v(-8.5, 4.8, -7.5), v(0.5, 1.4, 0.5))
            .zeg(1055, "aang", "verhaal", 80)
            .zwart(1132, 1150)
            .registreer();

    // =====================================================================================================================
    // the opening film: anchor = the ball. Visions of the four peoples hang in the sky above the island.
    // =====================================================================================================================
    private static final Vec3 KATARA = v(-4.5, 5, 8.5), TOPH = v(-1.5, 5, 8.5), MIKAI = v(1.5, 4.6, 8.5), LUCHT = v(4.5, 5, 8.5);

    public static final Cutscene INTRO = maak("intro").duur(1020).verbergEcht(5)
            .acteur("bal", E2H1LuchtFeature.KAASBOL, BOL, 200f)
            .npc("aang", Kind.AANG_GUH, AANG, 140f)
            .acteur("appa", E2AppaFeature.APPA_GUH, APPA, 200f)
            .speler(SPELER, -53f)
            .npc("katara", Kind.KATARA_GUH, KATARA, 180f)
            .npc("toph", Kind.TOPH_GUH, TOPH, 180f)
            .npc("mikai", Kind.VUURHEER_MIKAI, MIKAI, 180f)
            .npc("aang_visioen", Kind.AANG_GUH, LUCHT, 180f)
            .npc("soldaat1", Kind.VUURNATIE_MIKA, v(-1.5, -12, 12.5), 180f)
            .npc("soldaat2", Kind.VUURNATIE_MIKA, v(4.5, -12, 12.5), 180f)
            .acteur("visioenbol", E2H1LuchtFeature.KAASBOL, v(4.5, -14, 8.5), 180f)
            .animatie("bal", 0, "open")
            .zwart(0, 20)
            // Water. Aarde. Vuur. Lucht.
            .cameraKnip(0, v(-4.5, 6.4, 4.6), v(-4.5, 6.0, 8.5))
            .deeltjes(24, ParticleTypes.SPLASH, v(-4.5, 6.0, 8.5), 60, 0.7)
            .deeltjes(26, ParticleTypes.BUBBLE_POP, v(-4.5, 6.4, 8.5), 30, 0.6)
            .geluid(24, () -> SoundEvents.GENERIC_SPLASH, 0.8f, 1.2f)
            .zeg(30, "", "water", 40)
            .animatie("katara", 30, "waterzweep")
            .cameraKnip(76, v(-1.5, 6.4, 4.6), v(-1.5, 6.0, 8.5))
            .deeltjes(80, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), v(-1.5, 5.4, 8.5), 60, 0.8)
            .geluid(80, () -> SoundEvents.STONE_BREAK, 1.0f, 0.6f)
            .schud(80, 0.5f, 8)
            .zeg(86, "", "aarde", 40)
            .animatie("toph", 86, "stamp")
            .cameraKnip(132, v(1.5, 6.9, 4.0), v(1.5, 6.9, 8.5))
            .deeltjes(136, ParticleTypes.FLAME, v(1.5, 6.6, 8.5), 60, 0.7)
            .geluid(136, () -> SoundEvents.FIRECHARGE_USE, 0.9f, 0.9f)
            .zeg(142, "", "vuur", 40)
            .animatie("mikai", 142, "vuurstoot")
            .cameraKnip(188, v(4.5, 6.4, 4.6), v(4.5, 6.0, 8.5))
            .deeltjes(192, ParticleTypes.CLOUD, v(4.5, 6.0, 8.5), 50, 0.8)
            .geluid(192, () -> SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 1.6f)
            .zeg(198, "", "lucht", 40)
            .animatie("aang_visioen", 198, "zweef")
            // the four together
            .cameraKnip(244, v(0, 5.2, -1.5), v(0, 6, 8.5))
            .camera(330, v(0, 6.4, 0.5), v(0, 6, 8.5))
            .deeltjes(250, ParticleTypes.HEART, v(0, 6.8, 8.5), 16, 3.0)
            .zeg(254, "", "vroeger", 90)
            // ... then the Fire Nation got hungry
            .loop("mikai", 350, 400, v(0.5, 4.6, 6.5))
            .loop("katara", 350, 400, v(-7.5, 5, 10.5)).loop("toph", 350, 400, v(-5.0, 5, 11.5)).loop("aang_visioen", 350, 400, v(7.5, 5, 10.5))
            .loop("soldaat1", 350, 380, v(-1.5, 4.6, 9.5)).loop("soldaat2", 350, 380, v(2.5, 4.6, 9.5))
            .deeltjes(372, ParticleTypes.FLAME, v(0.5, 6, 6.5), 120, 1.6)
            .deeltjes(380, ParticleTypes.LARGE_SMOKE, v(0.5, 7.5, 7.5), 40, 2.0)
            .geluid(372, () -> SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f)
            .schud(372, 0.6f, 14)
            .zeg(366, "", "trek", 90)
            // the Guhvatar
            .cameraKnip(462, v(5.5, 6.5, 6.0), v(4.0, 6.2, 9.0))
            .loop("aang_visioen", 462, 500, v(3.5, 5.4, 8.5))
            .deeltjes(470, ParticleTypes.SPLASH, v(3.5, 6.4, 8.5), 30, 0.6)
            .deeltjes(480, ParticleTypes.FLAME, v(3.5, 6.4, 8.5), 20, 0.6)
            .deeltjes(490, ParticleTypes.CLOUD, v(3.5, 6.4, 8.5), 20, 0.6)
            .deeltjes(500, ParticleTypes.END_ROD, v(3.5, 6.4, 8.5), 40, 0.8)
            .zeg(470, "", "guhvatar", 100)
            // ... who rolled himself up in a kaasbol for a nap
            .loop("visioenbol", 570, 590, v(3.5, 5.0, 8.5))
            .loop("aang_visioen", 570, 590, v(3.5, 5.0, 8.5))
            .deeltjes(586, ParticleTypes.SNOWFLAKE, v(3.5, 6, 8.5), 40, 0.9)
            .geluid(586, () -> SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f)
            .zeg(592, "", "slaap", 100)
            // Mikai ate and ate
            .cameraKnip(696, v(0.5, 3.8, 3.0), v(0.5, 7.4, 6.5))
            .deeltjes(700, ParticleTypes.FLAME, v(0.5, 7.0, 6.5), 80, 1.2)
            .animatie("mikai", 704, "brul")
            .zeg(706, "", "mikai", 90)
            // ... and now it is you
            .cameraKnipVolgt(806, v(-1.9, 1.75, -1.3), Cutscene.SPELER)
            .deeltjes(810, ParticleTypes.END_ROD, v(-3.5, 1.4, -2.5), 30, 0.6)
            .zeg(814, "", "jij", 80)
            .cameraKnip(902, v(-5.5, 2.6, -6.0), v(1.0, 1.2, 0.0))
            .kijk("aang", 902, v(-3.5, 1, -2.5))
            .zeg(906, "aang", "jipjip", 80)
            .animatie("aang", 906, "praat")
            .geluid(990, E2AppaFeature.JIP_JIP, 1.0f, 1.0f)
            .animatie("appa", 990, "spring")
            .zwart(1000, 1020)
            .registreer();

    // =====================================================================================================================
    // arriving at the temple: anchor = aankomst_speler (template 96, 41, 172). The gate is at (0, 8, -21), Aang-guh's spot at
    // (0, 8, -27); the opening shot comes from uitzicht (0, 33, 54).
    // =====================================================================================================================
    public static final Cutscene AANKOMST = maak("aankomst").duur(330).verbergEcht(30)
            .speler(v(0.5, 0, 0.5), 180f)
            .npc("aang", Kind.AANG_GUH, v(0.5, 8, -26.5), 0f)
            .zwart(0, 16)
            .camera(0, v(0.5, 30, 50), v(0.5, 14, -60))
            .camera(190, v(3.5, 5, 7), v(0.5, 8.5, -24))
            .zeg(40, "", "tempel", 100)
            .cameraKnipVolgt(196, v(2.5, 9.6, -23.0), "aang")
            .zeg(206, "aang", "thuis", 110)
            .animatie("aang", 206, "praat")
            .zwart(316, 330)
            .registreer();

    // =====================================================================================================================
    // the Hal van de Guhvatars: anchor = voetstuk_leeg (96, 53, 96), the hall is -20..20 around it. Roku-guh at (10, 0, 5),
    // Kyoshi-guh at (10, 0, -4), both in front of their statues and looking west.
    // =====================================================================================================================
    private static final Vec3 ROKU = v(10.5, 0, 5.5), KYOSHI = v(10.5, 0, -3.5);

    public static final Cutscene HAL = maak("hal").duur(595).verbergEcht(16)
            .speler(v(6.5, 0, 1.0), -90f)
            .npc("roku", Kind.ROKU_GUH, ROKU, 90f)
            .npc("kyoshi", Kind.KYOSHI_GUH, KYOSHI, 90f)
            .camera(0, v(-14, 5, 14), v(-14, 3, -14))
            .camera(120, v(14, 5, 14), v(14, 3, -14))
            .zeg(16, "", "beelden", 100)
            .cameraKnipVolgt(126, v(7.2, 2.0, 7.8), "roku")
            .deeltjes(128, ParticleTypes.END_ROD, ROKU.add(0, 1.2, 0), 24, 0.4)
            .geluid(128, () -> SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f)
            .zeg(136, "roku", "roku1", 90)
            .animatie("roku", 136, "praat")
            .cameraKnipVolgt(232, v(7.2, 2.0, -6.0), "kyoshi")
            .deeltjes(234, ParticleTypes.END_ROD, KYOSHI.add(0, 1.4, 0), 24, 0.4)
            .zeg(242, "kyoshi", "kyoshi1", 70)
            .animatie("kyoshi", 242, "waaier")
            .cameraKnip(318, v(4.5, 3.2, 1.0), v(10.5, 1.6, 1.0))
            .zeg(324, "roku", "roku2", 90)
            .animatie("roku", 324, "raad")
            .zeg(420, "kyoshi", "kyoshi2", 70)
            .animatie("kyoshi", 420, "praat")
            .cameraKnip(498, v(-3.5, 4.5, -3.5), v(0.5, 0.6, 0.5))
            .camera(595, v(-2.2, 2.6, -2.2), v(0.5, 0.8, 0.5))
            .deeltjes(504, ParticleTypes.END_ROD, v(0.5, 1.5, 0.5), 30, 0.4)
            .geluid(504, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f)
            .zeg(505, "", "voetstuk", 80)
            .registreer();

    // =====================================================================================================================
    // Momo: anchor = momo_eind (116, 77, 47), the nest under the roof of the windtoren; the chase ends one block south.
    // =====================================================================================================================
    public static final Cutscene MOMO = maak("momo").duur(290).verbergEcht(6)
            .speler(v(0.5, 0, 2.5), 180f)
            .acteur("momo", E2AppaFeature.MOMO_GUH, v(0.5, 0.3, 0.5), 0f)
            .camera(0, v(-2.4, 1.7, 3.6), v(0.5, 0.8, 0.8))
            .zeg(12, "", "gevangen", 70)
            .geluid(90, E2AppaFeature.MOMO, 1.0f, 1.1f)
            .zeg(92, "momo", "kwiek", 40)
            .animatie("momo", 92, "blij")
            .loop("momo", 136, 160, v(0.85, 1.55, 2.5))
            .cameraKnipVolgt(136, v(-1.5, 2.0, 4.6), Cutscene.SPELER)
            .zeg(142, "", "terug", 70)
            .deeltjes(214, ParticleTypes.HEART, v(0.6, 2.2, 2.5), 8, 0.4)
            .zeg(216, "", "naam", 70)
            .registreer();

    // =====================================================================================================================
    // Zuka: anchor = steiger (155, 41, 171), on the head of the pier. Zuka lands at (8, 0, 0) = zuka_confrontatie. His ship
    // (the overlay laag_zuka_schip, shown to this player from the start of the scene) lies east of x 12, its deck at y 2.
    // =====================================================================================================================
    public static final Cutscene ZUKA_STEIGER = maak("zuka_steiger").duur(590).verbergEcht(10)
            .speler(v(0.5, 0, 0.5), -90f)
            .npc("zuka", Kind.ZUKA_BALLING, v(14.5, 2, 0.5), 90f)
            .npc("mika1", Kind.VUURNATIE_MIKA, v(17.5, 2, -1.5), 90f)
            .npc("mika2", Kind.VUURNATIE_MIKA, v(17.5, 2, 2.5), 90f)
            .zwart(0, 28)
            .camera(0, v(4, 4.2, -11), v(11, 2.2, 0.5))
            .camera(150, v(2.5, 3.0, -8.0), v(9, 1.8, 0.5))
            .schud(18, 1.3f, 16)
            .geluid(18, () -> SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f)
            .geluid(20, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.2f)
            .deeltjes(20, ParticleTypes.EXPLOSION, v(11.5, 1.2, 0.5), 5, 1.2)
            .deeltjes(22, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()), v(11, 1.0, 0.5), 80, 1.4)
            .zeg(36, "", "boem", 80)
            .animatie("zuka", 120, "spring")
            .loop("zuka", 120, 146, v(8.5, 0, 0.5))
            .deeltjes(146, ParticleTypes.POOF, v(8.5, 0.2, 0.5), 14, 0.4)
            .cameraKnipVolgt(156, v(5.0, 1.8, -2.4), "zuka")
            .zeg(162, "zuka", "eindelijk", 60)
            .animatie("zuka", 162, "praat")
            .zeg(232, "zuka", "eer", 100)
            .animatie("zuka", 232, "mijn_eer")
            .deeltjes(240, ParticleTypes.FLAME, v(8.5, 1.4, 0.5), 20, 0.3)
            .cameraKnip(340, v(2.0, 5.0, -10.5), v(12, 2.4, 0.5))
            .loop("mika1", 346, 372, v(10.5, 0, -2.5)).loop("mika2", 352, 378, v(10.5, 0, 3.5))
            .animatie("mika1", 346, "spring").animatie("mika2", 352, "spring")
            .zeg(350, "", "wie", 100)
            .cameraKnipVolgt(452, v(4.6, 1.9, 2.2), "zuka")
            .zeg(458, "zuka", "pak", 70)
            .animatie("zuka", 458, "vuur")
            .deeltjes(464, ParticleTypes.FLAME, v(8.0, 1.2, 0.5), 30, 0.4)
            .geluid(464, () -> SoundEvents.FIRECHARGE_USE, 0.8f, 1.0f)
            .cameraKnipVolgt(526, v(-2.2, 1.8, 1.8), Cutscene.SPELER)
            .zeg(530, "", "rennen", 50)
            .registreer();

    // =====================================================================================================================
    // Air bending learnt: anchor = appa_kamp (70, 41, 172). Zuka stands at the end of the terrace (24, 0, 0); Appa takes
    // off to the west with Aang-guh and you on his back.
    // =====================================================================================================================
    public static final Cutscene GELEERD = maak("geleerd").duur(460).verbergEcht(8)
            .acteur("appa", E2AppaFeature.APPA_GUH, v(0.5, 0, 0.5), 90f)
            .npc("aang", Kind.AANG_GUH, v(-0.6, 2.6, 0.5), 90f)
            .speler(v(1.2, 2.4, 0.5), -90f)
            .npc("zuka", Kind.ZUKA_BALLING, v(24.5, 0, 0.5), 90f)
            .zwart(0, 14)
            .camera(0, v(-6, 3.2, -6), v(14, 1.6, 0.5))
            .zeg(14, "zuka", "eer", 40)
            .animatie("zuka", 14, "mijn_eer")
            .deeltjes(58, ParticleTypes.FLAME, v(21, 1.6, 0.5), 20, 0.3)
            .deeltjes(64, ParticleTypes.FLAME, v(16, 2.0, 0.5), 20, 0.3)
            .deeltjes(70, ParticleTypes.FLAME, v(11, 2.4, 0.5), 20, 0.3)
            .deeltjes(76, ParticleTypes.FLAME, v(6, 2.8, 0.5), 20, 0.3)
            .geluid(58, () -> SoundEvents.BLAZE_SHOOT, 0.9f, 1.0f)
            .animatie(Cutscene.SPELER, 80, "zwaai")
            .deeltjes(82, ParticleTypes.CLOUD, v(3.0, 3.4, 0.5), 50, 0.8)
            .deeltjes(84, ParticleTypes.GUST, v(3.0, 3.4, 0.5), 3, 0.4)
            .geluid(82, () -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0f, 1.0f)
            .deeltjes(88, ParticleTypes.FLAME, v(4.0, 6.0, 4.0), 20, 0.4)
            .deeltjes(94, ParticleTypes.FLAME, v(5.0, 9.0, 8.0), 20, 0.4)
            .zeg(96, "", "blaas", 70)
            .loop("appa", 170, 330, v(-20.5, 10, 0.5)).loop("aang", 170, 330, v(-21.6, 12.6, 0.5))
            .loop(Cutscene.SPELER, 170, 330, v(-19.8, 12.4, 0.5))
            .geluid(170, E2AppaFeature.JIP_JIP, 1.0f, 1.0f)
            .geluid(180, () -> SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 0.8f)
            .cameraKnipVolgt(170, v(-3.5, 4.5, -6.5), "aang")
            .cameraVolgt(330, v(-24, 14.5, -7.5), "aang")
            .zeg(176, "aang", "zag_je", 80)
            .animatie("aang", 176, "lach")
            .zeg(262, "aang", "geleerd", 80)
            .zeg(344, "appa", "brom", 30)
            .geluid(344, E2AppaFeature.BRUL, 1.0f, 1.0f)
            .zeg(378, "aang", "volgende", 70)
            .zwart(446, 460)
            .registreer();

    // =====================================================================================================================
    // Wink 1 (DESIGN 5c): Sjokkel on the luchtscooter-baan. Anchor = the look-at point of knipoog_sjokkel_camera (31, 50, 123);
    // he creeps from (2, 0, 4) to (-2, 0, -4); the camera stands at (9, 1, -4).
    // =====================================================================================================================
    public static final Cutscene KNIPOOG_SJOKKEL = maak("knipoog_sjokkel").duur(180).verbergEcht(4)
            .acteur("sjokkel", LanddiertjesFeature.SHUCKLE, v(2.5, 0, 4.5), 200f)
            .camera(0, v(9.5, 1.6, -3.5), v(1.5, 0.4, 2.5))
            .camera(180, v(8.5, 1.4, -4.5), v(-1.0, 0.4, -2.0))
            .loop("sjokkel", 10, 175, v(-1.5, 0, -3.5))
            .zeg(30, "", "sjokkel", 100)
            .registreer();

    /** Every scene of the chapter (the dev command, the test). */
    public static final List<Cutscene> ALLE = List.of(KAASBOL, INTRO, AANKOMST, HAL, MOMO, ZUKA_STEIGER, GELEERD, KNIPOOG_SJOKKEL);

    /** (register) Touches the class: the scenes register themselves when it loads. */
    static void laad() {
    }
}
