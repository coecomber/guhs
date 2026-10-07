// WRITTEN BY tools/features/oude_scenes_java.py - do not edit by hand: change oude_scenes_scene.py and run that tool again.

package nl.juiced.guhs.feature.oudescenes;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.beroepen.BeroepenFeature;
import nl.juiced.guhs.feature.hemel.HemelFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (oude-scenes): the six camera scenes of the older stories, as the verhaal engine wants them. Positions are relative to
 * the scene's anchor (a block of the story's own template), in template coordinates, so they fit every copy however it is
 * turned. The scripts themselves, with what every beat is for, are tools/features/oude_scenes_scene.py (which also checks every
 * camera and actor against the template); their texts: scene.guhs.oudescenes_*. {@link OudeScenes} says when each plays.
 */
public final class Scenes {
    // ---- oudescenes_balto: De witte wolf-guh ----
    /** The template block oudescenes_balto is anchored on (template nomguh). */
    public static final BlockPos BALTO_ANKER = new BlockPos(184, 21, 12);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String BALTO_STRUCTUUR = "nomguh", BALTO_STUK = null;
    public static final Effecten BALTO_EFFECTEN = new Effecten(new float[][] {{0.0f, 16.0f, 0.75f, 0.0f, 0.0f, 0.0f}, {70.0f, 4.2f, 1.0f, 0.0f, 0.0f, 0.0f}, {150.0f, 3.6f, 1.0f, 0.0f, 0.0f, 0.0f}, {151.0f, 3.6f, 1.0f, 0.0f, 0.0f, 0.0f}, {196.0f, 14.0f, 0.8f, 0.0f, 0.0f, 0.0f}, {330.0f, 14.0f, 0.8f, 0.0f, 0.0f, 0.0f}, {400.0f, 64.0f, 0.12f, 0.0f, 0.0f, 0.0f}}, new int[] {}, List.of(
            new Effecten.Stroom(152, 153, BaltoFeature.WOLFGLANS::get, new Vec3(0.5, 4.9, -5.0), new Vec3(0.5, 4.9, -5.0), 40.0f, 0.6, new Vec3(0.0, 0.02, 0.0)),
            new Effecten.Stroom(160, 330, BaltoFeature.WOLFGLANS::get, new Vec3(0.5, 4.8, -5.0), new Vec3(0.5, 4.8, -5.0), 1.0f, 0.7, new Vec3(0.0, 0.02, 0.0)),
            new Effecten.Stroom(316, 317, BaltoFeature.WOLFGLANS::get, new Vec3(0.5, 5.2, -5.0), new Vec3(0.5, 5.2, -5.0), 60.0f, 0.9, new Vec3(0.0, 0.02, 0.0)),
            new Effecten.Stroom(372, 373, BaltoFeature.WOLFGLANS::get, new Vec3(0.5, 4.9, -5.0), new Vec3(0.5, 4.9, -5.0), 50.0f, 0.7, new Vec3(0.0, 0.02, 0.0))));
    public static final Cutscene BALTO = Cutscene.maak("oudescenes_balto").duur(420).bij("balto").kaart("oudescenes_balto").verbergEcht(26.0)
            .speler(new Vec3(2.0, 0.0, 1.6), 90.0f)
            .guh("balto", GuhVariant.BALTOGUH, new Vec3(0.9, 0.0, 0.9), 90.0f)
            .npc("wolf", GuhNpcEntity.Kind.WITTE_WOLFGUH, new Vec3(0.5, -40.0, -5.0), 0.0f)
            .camera(0, new Vec3(3.3, 1.5, 4.4), new Vec3(1.2, 0.8, 1.0))
            .camera(150, new Vec3(2.7, 1.3, 3.8), new Vec3(1.1, 0.8, 1.0))
            .cameraKnip(150, new Vec3(-0.7, 0.7, 0.3), new Vec3(0.5, 4.9, -5.0))
            .camera(262, new Vec3(-0.4, 0.9, -0.7), new Vec3(0.5, 4.8, -5.0))
            .cameraKnip(262, new Vec3(-3.4, 1.5, 0.6), new Vec3(0.6, 2.2, -3.0))
            .camera(344, new Vec3(-3.0, 1.7, 1.0), new Vec3(0.6, 2.3, -3.0))
            .cameraKnip(344, new Vec3(6.4, 6.2, 5.6), new Vec3(0.4, 1.4, -1.4))
            .camera(420, new Vec3(8.0, 7.6, 7.0), new Vec3(-2.0, 1.0, -1.2))
            .loop("balto", 34, 62, new Vec3(-1.4, 0.0, -0.4))
            .loop("balto", 70, 96, new Vec3(1.0, 0.0, 2.3))
            .loop("wolf", 149, 150, new Vec3(0.5, 4.0, -5.0))
            .loop("balto", 176, 204, new Vec3(0.4, 0.0, -1.6))
            .loop("speler", 236, 258, new Vec3(1.6, 0.0, -1.1))
            .loop("wolf", 372, 373, new Vec3(0.5, -40.0, -5.0))
            .kijk("speler", 0, new Vec3(-1.0, 0.0, 0.5))
            .kijk("speler", 96, new Vec3(1.0, 0.0, 2.3))
            .kijk("balto", 98, new Vec3(2.0, 0.0, 1.6))
            .kijk("wolf", 150, new Vec3(0.4, 0.0, 0.6))
            .kijk("balto", 156, new Vec3(0.5, 4.0, -5.0))
            .kijk("speler", 160, new Vec3(0.5, 4.0, -5.0))
            .kijk("speler", 259, new Vec3(0.5, 4.0, -5.0))
            .kijk("balto", 350, new Vec3(-10.0, 0.0, 1.0))
            .animatie("balto", 304, "spring")
            .animatie("wolf", 314, "spring")
            .animatie("speler", 322, "spring")
            .zeg(12, "", "storm", 62)
            .zeg(84, "balto", "kwijt", 60)
            .zeg(160, "", "wolf", 58)
            .zeg(222, "boris", "boris", 38)
            .zeg(266, "balto", "half", 40)
            .zeg(308, "speler", "huil", 36)
            .zeg(352, "", "klaart", 52)
            .geluid(4, BaltoFeature.WIND::get, 0.9f, 0.9f)
            .geluid(40, BaltoFeature.SNUIF::get, 0.9f, 1.0f)
            .geluid(64, BaltoFeature.WIND::get, 1.0f, 0.8f)
            .geluid(74, BaltoFeature.SNUIF::get, 0.8f, 1.1f)
            .geluid(150, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f)
            .geluid(306, BaltoFeature.HUIL::get, 1.0f, 1.0f)
            .geluid(316, BaltoFeature.HUIL::get, 0.9f, 1.25f)
            .geluid(350, BaltoFeature.BELLETJES::get, 0.8f, 1.0f)
            .geluid(372, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.0f)
            .deeltjes(308, ParticleTypes.NOTE, new Vec3(0.4, 1.6, -1.6), 4, 0.3)
            .deeltjes(324, ParticleTypes.NOTE, new Vec3(1.6, 2.3, -1.1), 4, 0.3)
            .schud(306, 0.5f, 26)
            .zwart(-10, 4)
            .zwart(406, 420)
            .registreer();

    // ---- oudescenes_mewtwo: Dag 45: de nacht van de knal ----
    /** The template block oudescenes_mewtwo is anchored on (template kloon_eiland). */
    public static final BlockPos MEWTWO_ANKER = new BlockPos(40, 38, 44);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String MEWTWO_STRUCTUUR = "kloon_eiland", MEWTWO_STUK = null;
    public static final int MEWTWO_KRAK = 164;
    public static final Effecten MEWTWO_EFFECTEN = new Effecten(new float[][] {{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f}}, new int[] {34, 164, 300}, List.of(
            new Effecten.Stroom(112, 300, MewtwoFeature.BUBBEL::get, new Vec3(0.5, 0.6, 2.2), new Vec3(0.5, 2.6, 2.2), 2.0f, 0.5, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(198, 252, () -> new DustParticleOptions(0xB45CFF, 1.25f), new Vec3(0.28, 1.55, 2.06), new Vec3(0.28, 1.55, 2.06), 2.0f, 0.0, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(198, 252, () -> new DustParticleOptions(0xB45CFF, 1.25f), new Vec3(0.72, 1.55, 2.06), new Vec3(0.72, 1.55, 2.06), 2.0f, 0.0, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(242, 243, MewtwoFeature.GLOED::get, new Vec3(0.5, 3.2, 0.5), new Vec3(0.5, 3.2, 0.5), 40.0f, 0.7, new Vec3(0.0, 0.02, 0.0)),
            new Effecten.Stroom(244, 300, MewtwoFeature.GLOED::get, new Vec3(0.5, 1.6, 0.5), new Vec3(0.5, 4.6, 0.5), 2.0f, 0.5, new Vec3(0.0, 0.0, 0.0))));
    public static final Cutscene MEWTWO = Cutscene.maak("oudescenes_mewtwo").duur(440).bij("mewtwo").kaart("oudescenes_mewtwo").verbergEcht(34.0)
            .guh("guhtwo", GuhVariant.MEWTWO, new Vec3(0.5, -40.0, 0.5), 0.0f)
            .acteur("mew", MewtwoFeature.MEW, new Vec3(9.5, -40.0, 5.5), 90.0f)
            .camera(0, new Vec3(0.5, 9.6, 23.0), new Vec3(0.5, 5.0, 11.5))
            .camera(112, new Vec3(0.5, 8.2, 19.5), new Vec3(0.5, 5.0, 11.5))
            .cameraKnip(112, new Vec3(0.5, 1.1, 6.3), new Vec3(0.5, 1.6, 0.5))
            .camera(236, new Vec3(0.5, 1.2, 4.6), new Vec3(0.5, 1.7, 0.5))
            .cameraKnip(236, new Vec3(-2.6, 1.4, 5.4), new Vec3(0.5, 2.2, 0.5))
            .camera(300, new Vec3(-3.0, 1.9, 5.8), new Vec3(0.5, 5.0, 0.5))
            .camera(346, new Vec3(-3.2, 2.2, 6.0), new Vec3(0.5, 5.2, 0.5))
            .cameraKnip(346, new Vec3(2.6, 1.5, 7.2), new Vec3(0.5, 4.6, 0.5))
            .camera(440, new Vec3(2.0, 1.7, 7.6), new Vec3(0.2, 4.8, 0.5))
            .loop("guhtwo", 239, 240, new Vec3(0.5, 1.2, 0.5))
            .loop("guhtwo", 242, 300, new Vec3(0.5, 4.3, 0.5))
            .loop("mew", 345, 346, new Vec3(9.0, 4.6, 6.0))
            .loop("mew", 348, 420, new Vec3(-8.0, 5.4, -4.0))
            .kijk("guhtwo", 240, new Vec3(0.5, 0.0, 8.0))
            .kijk("guhtwo", 356, new Vec3(6.0, 4.0, 4.0))
            .kijk("guhtwo", 376, new Vec3(0.0, 4.0, 8.0))
            .kijk("guhtwo", 396, new Vec3(-7.0, 4.0, -3.0))
            .zeg(10, "prof", "dag45", 80)
            .zeg(168, "", "krak", 40)
            .zeg(210, "", "ogen", 44)
            .zeg(272, "guhtwo", "wie", 70)
            .zeg(354, "mew", "hihi", 36)
            .zeg(396, "prof", "begon", 40)
            .geluid(37, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.9f)
            .geluid(70, MewtwoFeature.TANK_BORREL::get, 0.7f, 0.9f)
            .geluid(118, MewtwoFeature.TANK_BORREL::get, 1.0f, 1.0f)
            .geluid(146, MewtwoFeature.TANK_BORREL::get, 1.0f, 1.2f)
            .geluid(164, () -> SoundEvents.GLASS_BREAK, 1.0f, 0.8f)
            .geluid(166, MewtwoFeature.TANK_KLIK::get, 1.0f, 0.7f)
            .geluid(167, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.9f)
            .geluid(198, MewtwoFeature.TELEKINESE::get, 0.8f, 0.7f)
            .geluid(242, MewtwoFeature.TANK_HEEL::get, 0.9f, 0.7f)
            .geluid(250, MewtwoFeature.TELEKINESE::get, 1.0f, 0.9f)
            .geluid(303, () -> SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.9f)
            .geluid(352, MewtwoFeature.MEW_GIECHEL::get, 1.0f, 1.0f)
            .geluid(392, MewtwoFeature.MEW_GIECHEL::get, 0.6f, 1.2f)
            .deeltjes(165, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFF4E8FF), new Vec3(0.5, 1.6, 2.3), 1, 0.0)
            .deeltjes(166, ParticleTypes.END_ROD, new Vec3(0.5, 1.6, 2.3), 14, 0.5)
            .schud(164, 0.6f, 10)
            .schud(244, 0.4f, 14)
            .zwart(-10, 6)
            .zwart(426, 440)
            .registreer();

    // ---- oudescenes_ohana: Ohana, bij het kampvuurtje ----
    /** The template block oudescenes_ohana is anchored on (template guhwaii_ohana). */
    public static final BlockPos OHANA_ANKER = new BlockPos(11, 5, 33);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String OHANA_STRUCTUUR = "guhwaii_ohana", OHANA_STUK = null;
    public static final Effecten OHANA_EFFECTEN = new Effecten(new float[][] {{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f}}, new int[] {}, List.of(
            new Effecten.Stroom(272, 273, BandFeature.HARTJE::get, new Vec3(0.85, 1.2, 0.4), new Vec3(0.85, 1.2, 0.4), 10.0f, 0.35, new Vec3(0.0, 0.02, 0.0)),
            new Effecten.Stroom(280, 326, BandFeature.HARTJE::get, new Vec3(0.85, 1.25, 0.4), new Vec3(0.85, 1.25, 0.4), 0.25f, 0.3, new Vec3(0.0, 0.0, 0.0))));
    public static final Cutscene OHANA = Cutscene.maak("oudescenes_ohana").duur(420).bij("guhwaii").kaart("oudescenes_ohana").verbergEcht(40.0)
            .guh("stitch", GuhVariant.STITCH626, new Vec3(0.2, 0.0, 0.9), 0.0f)
            .npc("lilo", GuhNpcEntity.Kind.LILO_GUH, new Vec3(1.05, -40.0, 0.25), 0.0f)
            .acteur("boek", () -> EntityType.ITEM_DISPLAY, new Vec3(-0.1, 0.03, 0.3), 135.0f, Rekwisieten::prentenboek)
            .camera(0, new Vec3(-8.4, 2.6, -2.4), new Vec3(0.0, 0.6, 0.2))
            .camera(124, new Vec3(-6.0, 1.8, -1.8), new Vec3(0.0, 0.6, 0.3))
            .cameraKnip(124, new Vec3(-1.5, 0.9, -1.5), new Vec3(0.6, 0.6, 0.6))
            .camera(246, new Vec3(-1.7, 1.0, -1.7), new Vec3(0.6, 0.6, 0.6))
            .cameraKnip(246, new Vec3(-0.1, 1.3, -1.5), new Vec3(0.65, 0.6, 0.6))
            .camera(330, new Vec3(-0.3, 1.5, -1.8), new Vec3(0.65, 0.6, 0.6))
            .cameraKnip(330, new Vec3(3.6, 1.5, 3.4), new Vec3(0.7, 0.7, 0.5))
            .camera(372, new Vec3(3.8, 1.6, 3.7), new Vec3(0.0, 3.0, -0.4))
            .camera(420, new Vec3(4.0, 1.7, 4.0), new Vec3(-1.5, 30.0, -1.0))
            .loop("lilo", 123, 124, new Vec3(1.05, 0.0, 0.25))
            .loop("stitch", 254, 270, new Vec3(0.62, 0.0, 0.56))
            .kijk("stitch", 0, new Vec3(-0.5, 0.0, -0.5))
            .kijk("lilo", 124, new Vec3(0.2, 0.0, 0.9))
            .kijk("stitch", 138, new Vec3(1.05, 0.0, 0.25))
            .kijk("lilo", 170, new Vec3(-0.5, 0.0, -0.5))
            .kijk("lilo", 248, new Vec3(0.2, 0.0, 0.9))
            .animatie("lilo", 128, "spring")
            .animatie("lilo", 276, "spring")
            .zeg(12, "", "nacht", 66)
            .zeg(86, "stitch", "alleen", 38)
            .zeg(132, "lilo", "erbij", 34)
            .zeg(172, "lilo", "citaat", 74)
            .zeg(276, "stitch", "ohana", 46)
            .zeg(350, "", "sterren", 56)
            .geluid(30, () -> SoundEvents.CAMPFIRE_CRACKLE, 0.8f, 1.0f)
            .geluid(84, ModSounds.GUH_AMBIENT::get, 0.5f, 0.7f)
            .geluid(128, ModSounds.GUH_AMBIENT::get, 0.8f, 1.35f)
            .geluid(270, ModSounds.GUH_HAPPY::get, 0.9f, 1.1f)
            .geluid(366, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.4f)
            .zwart(-10, 8)
            .zwart(406, 420)
            .registreer();

    // ---- oudescenes_hemel: Het Knuffelhart klopt ----
    /** The template block oudescenes_hemel is anchored on (template hemelkapelletje). */
    public static final BlockPos HEMEL_ANKER = new BlockPos(20, 46, 10);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String HEMEL_STRUCTUUR = "hemelkapelletje", HEMEL_STUK = null;
    public static final int HEMEL_KLOP = 196;
    public static final Effecten HEMEL_EFFECTEN = new Effecten(new float[][] {}, new int[] {}, List.of(
            new Effecten.Stroom(0, 40, () -> ParticleTypes.CLOUD, new Vec3(0.5, 18.0, -8.0), new Vec3(0.5, 18.0, -8.0), 5.0f, 2.4, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(30, 100, () -> ParticleTypes.CLOUD, new Vec3(-1.0, 18.0, -8.0), new Vec3(-9.0, 19.0, -10.0), 4.0f, 1.8, new Vec3(-0.03, 0.0, 0.0)),
            new Effecten.Stroom(30, 100, () -> ParticleTypes.CLOUD, new Vec3(2.0, 18.0, -8.0), new Vec3(10.0, 19.0, -10.0), 4.0f, 1.8, new Vec3(0.03, 0.0, 0.0)),
            new Effecten.Stroom(56, 104, () -> ParticleTypes.END_ROD, new Vec3(0.5, 18.0, -13.9), new Vec3(0.5, 5.0, -3.4), 6.0f, 0.12, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(100, 362, () -> ParticleTypes.END_ROD, new Vec3(0.5, 18.0, -13.9), new Vec3(0.5, 1.55, -0.5), 9.0f, 0.1, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(110, 362, HemelFeature.STERRETJE::get, new Vec3(0.5, 1.55, -0.5), new Vec3(0.5, 1.55, -0.5), 1.2f, 0.45, new Vec3(0.0, 0.0, 0.0))));
    public static final Cutscene HEMEL = Cutscene.maak("oudescenes_hemel").duur(380).bij("hemel").kaart("oudescenes_hemel").verbergEcht(22.0)
            .npc("hoeder", GuhNpcEntity.Kind.WOLKENHOEDER, new Vec3(3.5, 0.0, 1.5), 0.0f)
            .speler(new Vec3(0.5, 0.0, 3.5), 180.0f)
            .camera(0, new Vec3(7.5, 14.5, -16.5), new Vec3(0.5, 9.5, -1.0))
            .camera(104, new Vec3(6.0, 13.0, -15.0), new Vec3(0.5, 8.5, -1.0))
            .cameraKnip(104, new Vec3(0.5, 1.5, 7.5), new Vec3(0.5, 2.0, -0.5))
            .camera(246, new Vec3(0.5, 1.4, 5.2), new Vec3(0.5, 2.0, -0.5))
            .cameraKnip(246, new Vec3(1.2, 1.7, 3.6), new Vec3(2.0, 1.4, 0.4))
            .camera(380, new Vec3(0.6, 2.2, 6.6), new Vec3(1.6, 1.6, 0.2))
            .loop("speler", 104, 105, new Vec3(-0.8, 0.0, 2.6))
            .kijk("hoeder", 0, new Vec3(0.5, 1.55, -0.5))
            .kijk("speler", 0, new Vec3(0.5, 1.55, -0.5))
            .kijk("speler", 106, new Vec3(0.5, 1.55, -0.5))
            .kijk("hoeder", 270, new Vec3(-0.8, 0.0, 2.6))
            .animatie("hoeder", 250, "spring")
            .animatie("hoeder", 264, "spring")
            .zeg(10, "", "drie", 56)
            .zeg(70, "", "wolken", 40)
            .zeg(124, "", "straal", 50)
            .zeg(200, "", "bonk", 44)
            .zeg(254, "hoeder", "vahoeg", 60)
            .zeg(322, "", "nooit", 40)
            .geluid(34, BaltoFeature.WIND::get, 0.5f, 1.4f)
            .geluid(60, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.8f)
            .geluid(110, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.9f)
            .geluid(120, HemelFeature.STER::get, 0.8f, 1.0f)
            .geluid(128, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.02f)
            .geluid(146, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.14f)
            .geluid(164, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.26f)
            .geluid(196, HemelFeature.HARTKLOP::get, 0.8f, 0.9f)
            .geluid(226, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(250, ModSounds.GUH_HAPPY::get, 0.9f, 1.25f)
            .geluid(250, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(272, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(292, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(312, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(332, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .geluid(352, HemelFeature.HARTKLOP::get, 1.0f, 1.0f)
            .deeltjes(198, ParticleTypes.HEART, new Vec3(0.5, 2.4, -0.5), 6, 0.4)
            .deeltjes(274, ParticleTypes.HEART, new Vec3(0.5, 2.6, -0.5), 10, 0.6)
            .schud(196, 0.25f, 2)
            .schud(226, 0.25f, 2)
            .schud(250, 0.25f, 2)
            .schud(272, 0.25f, 2)
            .schud(292, 0.25f, 2)
            .schud(312, 0.25f, 2)
            .schud(332, 0.25f, 2)
            .schud(352, 0.25f, 2)
            .zwart(-10, 6)
            .zwart(366, 380)
            .registreer();

    // ---- oudescenes_grill: Hij brandt weer! ----
    /** The template block oudescenes_grill is anchored on (template barbecueput_groot). */
    public static final BlockPos GRILL_ANKER = new BlockPos(27, 5, 14);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String GRILL_STRUCTUUR = "barbecueput", GRILL_STUK = "barbecueput_groot";
    public static final int GRILL_AAN = 100;
    public static final BlockPos GRILL_FRAME = new BlockPos(17, 6, 26);
    public static final Effecten GRILL_EFFECTEN = new Effecten(new float[][] {}, new int[] {}, List.of(
            new Effecten.Stroom(30, 47, () -> ParticleTypes.FLAME, new Vec3(-9.95, 1.05, 12.5), new Vec3(-8.05, 1.05, 12.5), 3.0f, 0.05, new Vec3(0.0, 0.01, 0.0)),
            new Effecten.Stroom(30, 47, () -> ParticleTypes.SMALL_FLAME, new Vec3(-9.95, 1.05, 12.5), new Vec3(-8.05, 1.05, 12.5), 2.0f, 0.08, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(47, 64, () -> ParticleTypes.FLAME, new Vec3(-8.05, 1.05, 12.5), new Vec3(-8.05, 3.95, 12.5), 3.0f, 0.05, new Vec3(0.0, 0.01, 0.0)),
            new Effecten.Stroom(47, 64, () -> ParticleTypes.SMALL_FLAME, new Vec3(-8.05, 1.05, 12.5), new Vec3(-8.05, 3.95, 12.5), 2.0f, 0.08, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(64, 81, () -> ParticleTypes.FLAME, new Vec3(-8.05, 3.95, 12.5), new Vec3(-9.95, 3.95, 12.5), 3.0f, 0.05, new Vec3(0.0, 0.01, 0.0)),
            new Effecten.Stroom(64, 81, () -> ParticleTypes.SMALL_FLAME, new Vec3(-8.05, 3.95, 12.5), new Vec3(-9.95, 3.95, 12.5), 2.0f, 0.08, new Vec3(0.0, 0.0, 0.0)),
            new Effecten.Stroom(81, 98, () -> ParticleTypes.FLAME, new Vec3(-9.95, 3.95, 12.5), new Vec3(-9.95, 1.05, 12.5), 3.0f, 0.05, new Vec3(0.0, 0.01, 0.0)),
            new Effecten.Stroom(81, 98, () -> ParticleTypes.SMALL_FLAME, new Vec3(-9.95, 3.95, 12.5), new Vec3(-9.95, 1.05, 12.5), 2.0f, 0.08, new Vec3(0.0, 0.0, 0.0))));
    public static final Cutscene GRILL = Cutscene.maak("oudescenes_grill").duur(360).bij("grillguh").kaart("oudescenes_grill").verbergEcht(30.0)
            .speler(new Vec3(-9.0, 0.0, 10.7), 0.0f)
            .npc("grillguh", GuhNpcEntity.Kind.GRILLGUH, new Vec3(0.5, 0.0, 0.5), 90.0f)
            .npc("guhdalf", GuhNpcEntity.Kind.GUHDALF, new Vec3(-19.5, 7.0, -6.5), 0.0f)
            .camera(0, new Vec3(-9.0, 2.5, 17.6), new Vec3(-9.0, 2.5, 12.5))
            .camera(112, new Vec3(-9.0, 2.4, 16.4), new Vec3(-9.0, 2.5, 12.5))
            .cameraKnip(112, new Vec3(-2.8, 1.5, -1.4), new Vec3(0.4, 0.9, 0.5))
            .camera(226, new Vec3(-2.4, 1.4, -1.8), new Vec3(0.4, 0.9, 0.5))
            .cameraKnip(226, new Vec3(-8.5, 6.4, 10.8), new Vec3(-19.5, 8.0, -6.5))
            .camera(318, new Vec3(-13.0, 7.6, 2.0), new Vec3(-19.5, 8.1, -6.5))
            .camera(360, new Vec3(-13.6, 7.8, 0.8), new Vec3(-19.5, 8.1, -6.5))
            .kijk("grillguh", 0, new Vec3(-9.0, 2.0, 12.5))
            .kijk("guhdalf", 0, new Vec3(-9.0, 2.0, 12.5))
            .kijk("speler", 0, new Vec3(-9.0, 2.0, 12.5))
            .kijk("grillguh", 112, new Vec3(-9.0, 2.0, 12.5))
            .kijk("grillguh", 172, new Vec3(-2.8, 0.0, -1.4))
            .kijk("guhdalf", 336, new Vec3(-19.5, 7.0, -20.0))
            .animatie("speler", 18, "zwaai")
            .animatie("speler", 102, "spring")
            .animatie("grillguh", 116, "spring")
            .animatie("grillguh", 130, "spring")
            .animatie("grillguh", 144, "spring")
            .zeg(8, "", "blokje", 44)
            .zeg(54, "", "loopt", 40)
            .zeg(118, "grillguh", "brandt", 52)
            .zeg(174, "grillguh", "ruik", 48)
            .zeg(236, "", "verte", 50)
            .zeg(296, "guhdalf", "begonnen", 44)
            .geluid(20, () -> SoundEvents.FLINTANDSTEEL_USE, 1.0f, 1.0f)
            .geluid(30, () -> SoundEvents.FIRE_AMBIENT, 0.7f, 0.9f)
            .geluid(47, () -> SoundEvents.FIRE_AMBIENT, 0.7f, 1.07f)
            .geluid(64, () -> SoundEvents.FIRE_AMBIENT, 0.7f, 1.24f)
            .geluid(81, () -> SoundEvents.FIRE_AMBIENT, 0.7f, 1.41f)
            .geluid(100, () -> SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f)
            .geluid(116, ModSounds.GUH_HAPPY::get, 1.0f, 0.8f)
            .geluid(338, () -> SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.7f, 0.8f)
            .deeltjes(22, ParticleTypes.FLAME, new Vec3(-9.8, 1.1, 12.5), 6, 0.08)
            .deeltjes(100, ParticleTypes.FLAME, new Vec3(-9.0, 2.5, 12.3), 60, 0.6)
            .deeltjes(101, ParticleTypes.LAVA, new Vec3(-9.0, 1.6, 12.2), 10, 0.4)
            .deeltjes(132, ParticleTypes.FLAME, new Vec3(0.5, 1.3, 0.5), 16, 0.5)
            .schud(100, 0.5f, 10)
            .zwart(-10, 6)
            .zwart(346, 360)
            .registreer();

    // ---- oudescenes_timmer: Het dak zit erop ----
    /** The template block oudescenes_timmer is anchored on (template knuffeldal_stadje/bouwplaats). */
    public static final BlockPos TIMMER_ANKER = new BlockPos(15, 5, 16);
    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */
    public static final String TIMMER_STRUCTUUR = "knuffeldal_stadje", TIMMER_STUK = "bouwplaats";
    public static final int TIMMER_DEUR_OPEN = 262;
    public static final BlockPos TIMMER_DEUR = new BlockPos(11, 5, 16);
    public static final Effecten TIMMER_EFFECTEN = new Effecten(new float[][] {}, new int[] {}, List.of(
            ));
    public static final Cutscene TIMMER = Cutscene.maak("oudescenes_timmer").duur(340).bij("timmerguh").kaart("oudescenes_timmer").verbergEcht(24.0)
            .npc("timmerguh", GuhNpcEntity.Kind.TIMMERGUH, new Vec3(0.5, 0.0, 0.5), 90.0f)
            .acteur("bewoner", ModEntities.GUH, new Vec3(6.5, -40.0, 0.5), 90.0f, tag -> Rekwisieten.bewonertje(tag, GuhVariant.NORMAL))
            .speler(new Vec3(-1.4, 0.0, -1.6), 90.0f)
            .camera(0, new Vec3(4.0, 5.5, 6.5), new Vec3(-8.5, 8.4, 0.2))
            .camera(104, new Vec3(2.6, 6.4, 5.4), new Vec3(-8.5, 9.0, 0.2))
            .cameraKnip(104, new Vec3(2.6, 1.3, -1.6), new Vec3(0.5, 0.9, 0.5))
            .camera(204, new Vec3(2.3, 1.3, -1.3), new Vec3(0.5, 0.9, 0.5))
            .cameraKnip(204, new Vec3(4.8, 1.6, 0.9), new Vec3(-3.5, 1.2, 0.5))
            .camera(262, new Vec3(3.6, 1.5, 0.8), new Vec3(-3.5, 1.1, 0.5))
            .camera(340, new Vec3(2.3, 1.3, 0.7), new Vec3(-4.0, 0.9, 0.5))
            .loop("timmerguh", 203, 205, new Vec3(0.6, 0.0, 2.3))
            .loop("bewoner", 205, 206, new Vec3(7.4, 0.0, 0.5))
            .loop("bewoner", 208, 262, new Vec3(-2.5, 0.0, 0.5))
            .loop("bewoner", 266, 286, new Vec3(-4.7, 0.0, 0.5))
            .kijk("speler", 0, new Vec3(-9.0, 8.0, 0.0))
            .kijk("timmerguh", 0, new Vec3(-9.0, 8.0, 0.0))
            .kijk("timmerguh", 104, new Vec3(2.6, 0.0, -1.6))
            .kijk("speler", 206, new Vec3(6.0, 0.0, 0.5))
            .kijk("timmerguh", 206, new Vec3(6.0, 0.0, 0.5))
            .kijk("timmerguh", 236, new Vec3(1.0, 0.0, 0.5))
            .kijk("speler", 240, new Vec3(0.0, 0.0, 0.5))
            .kijk("speler", 262, new Vec3(-3.5, 0.0, 0.5))
            .kijk("timmerguh", 262, new Vec3(-3.5, 0.0, 0.5))
            .kijk("bewoner", 288, new Vec3(1.0, 0.0, 0.5))
            .animatie("timmerguh", 108, "spring")
            .animatie("bewoner", 294, "spring")
            .zeg(8, "", "tok", 46)
            .zeg(60, "", "vlag", 38)
            .zeg(110, "timmerguh", "dak", 50)
            .zeg(164, "timmerguh", "wie", 38)
            .zeg(216, "", "trippel", 44)
            .zeg(284, "", "knus", 44)
            .geluid(10, BeroepenFeature.HAMER::get, 0.9f, 1.0f)
            .geluid(22, BeroepenFeature.HAMER::get, 0.9f, 1.1f)
            .geluid(34, BeroepenFeature.HAMER::get, 0.9f, 1.2f)
            .geluid(52, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.8f, 1.0f)
            .geluid(108, ModSounds.GUH_HAPPY::get, 1.0f, 0.9f)
            .geluid(210, ModSounds.GUH_AMBIENT::get, 0.8f, 1.5f)
            .geluid(262, () -> SoundEvents.CHERRY_WOOD_DOOR_OPEN, 0.9f, 1.0f)
            .geluid(294, ModSounds.GUH_HAPPY::get, 0.9f, 1.5f)
            .deeltjes(11, ParticleTypes.HAPPY_VILLAGER, new Vec3(-8.5, 9.6, 0.5), 6, 0.5)
            .deeltjes(23, ParticleTypes.HAPPY_VILLAGER, new Vec3(-8.5, 9.6, 0.5), 6, 0.5)
            .deeltjes(35, ParticleTypes.HAPPY_VILLAGER, new Vec3(-8.5, 9.6, 0.5), 6, 0.5)
            .deeltjes(52, ParticleTypes.FIREWORK, new Vec3(-8.5, 12.2, 0.5), 40, 0.9)
            .deeltjes(296, ParticleTypes.HEART, new Vec3(-4.4, 1.2, 0.5), 8, 0.4)
            .zwart(-10, 6)
            .zwart(326, 340)
            .registreer();

    /** (called from OudeScenesFeature.register: the fields above register the scenes when this class loads) */
    static void registreer() {
    }

    private Scenes() {
    }
}
