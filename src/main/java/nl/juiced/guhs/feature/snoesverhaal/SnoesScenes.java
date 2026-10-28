package nl.juiced.guhs.feature.snoesverhaal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The finale of Snoes' dream: his own family day on the Droomplein (ontwerp 1b). Anchor = the plein's arrival spot
 * ({@link SnoesVerhaal#pleinAnker}), unrotated; the player stands on the anchor looking south (+z), where the box
 * ({@link SnoesVerhaal#DOOS}), the kattenhuisje ({@link SnoesVerhaal#KATTENHUISJE}) and the pink cakes stand. Everybody of
 * the islands walks in, Snoes sniffs the kattenhuisje... and crawls into the box. Laughter, a star rain, the lullaby, sleep.
 * Texts: scene.guhs.snoesverhaal_finale.* (tools/features/snoesverhaal.py).
 */
public final class SnoesScenes {
    public static final String FINALE_ID = "snoesverhaal_finale";

    public static final Cutscene FINALE = Cutscene.maak(FINALE_ID).duur(620).verbergEcht(16).cameraOntwijkt()
            .speler(new Vec3(0.5, 0, 0.5), 0)
            .acteur("gids", BlubFeature.BLUBGUH, new Vec3(1.5, 0, 1.5), 180)
            .npc("snoes", Kind.SNOES, new Vec3(0.5, 0, 3.5), 180)
            .npc("oma", Kind.DROOM_OMAGUH, new Vec3(-7.5, 0, 9.5), 90)
            .npc("rommel", Kind.ROMMELGUH, new Vec3(7.5, 0, 9.5), 270)
            .npc("zand", Kind.ZANDGUH, new Vec3(-8.5, 0, 5.5), 90)
            .npc("kussen1", Kind.KUSSENGEVECHT_GUH, new Vec3(8.5, 0, 5.5), 270)
            .npc("kussen2", Kind.KUSSENGEVECHT_GUH, new Vec3(9.5, 0, 3.5), 270)
            .npc("wekker", Kind.WEKKERGUH, new Vec3(-6.5, 0, 2.5), 90)
            .acteur("eend1", KnuffelbadFeature.BADEENDJE, new Vec3(-1.5, 0, 11.5), 180)
            .acteur("eend2", KnuffelbadFeature.BADEENDJE, new Vec3(1.5, 0, 11.5), 180)
            // everybody walks in
            .loop("oma", 40, 130, new Vec3(-3.5, 0, 8.5)).loop("rommel", 50, 140, new Vec3(3.5, 0, 8.5))
            .loop("zand", 60, 150, new Vec3(-4.5, 0, 6.5)).loop("kussen1", 70, 150, new Vec3(4.5, 0, 6.5))
            .loop("kussen2", 80, 160, new Vec3(5.5, 0, 4.5)).loop("wekker", 90, 170, new Vec3(-3.5, 0, 3.5))
            .loop("eend1", 60, 160, new Vec3(-1.5, 0, 9.5)).loop("eend2", 70, 170, new Vec3(1.5, 0, 9.5))
            // Snoes looks at the kattenhuisje... and then the box
            .loop("snoes", 230, 280, new Vec3(2.5, 0, 4.5)).kijk("snoes", 282, new Vec3(2.5, 0.5, 5.5))
            .loop("snoes", 320, 380, new Vec3(-1.5, 0, 4.5)).kijk("snoes", 382, new Vec3(-1.5, 0.3, 5.5))
            .loop("snoes", 384, 400, new Vec3(-1.5, 0, 5.5))
            .animatie("kussen1", 400, "gooi").animatie("kussen2", 410, "gooi")
            // the camera: over the player's shoulder, around the party, close to the box, up into the star rain
            .camera(0, new Vec3(0.5, 2.4, -3.0), new Vec3(0.5, 1, 4.5))
            .camera(170, new Vec3(7.0, 4.0, 1.0), new Vec3(0, 1, 6.5))
            .camera(300, new Vec3(3.5, 2.0, 2.0), new Vec3(0, 0.8, 5.0))
            .cameraKnip(385, new Vec3(-1.5, 1.6, 2.0), new Vec3(-1.5, 0.6, 5.5))
            .camera(470, new Vec3(-1.5, 2.6, 2.5), new Vec3(-1.5, 0.6, 5.5))
            .camera(600, new Vec3(0.5, 9.0, -4.0), new Vec3(0, 1, 6))
            .zeg(15, "gids", "gids", 70)
            .zeg(70, "", "verteller1", 70)
            .zeg(140, "oma", "oma", 80)
            .zeg(185, "rommel", "rommel", 70)
            .zeg(215, "snoes", "snoes1", 50)
            .zeg(250, "", "verteller2", 60)
            .zeg(290, "snoes", "snoes2", 50)
            .zeg(345, "", "verteller3", 60)
            .zeg(405, "kussen1", "lachen", 50)
            .zeg(440, "zand", "zand", 60)
            .lied(480, "liedje", 70)
            .zeg(530, "snoes", "snoes3", 50)
            .zeg(575, "", "einde", 45)
            .deeltjes(400, ParticleTypes.HEART, new Vec3(-1.5, 1, 5.5), 10, 0.5)
            .deeltjes(410, ParticleTypes.FIREWORK, new Vec3(0, 6, 6), 40, 4)
            .deeltjes(440, ParticleTypes.END_ROD, new Vec3(0, 9, 6), 60, 6)
            .deeltjes(500, ParticleTypes.END_ROD, new Vec3(0, 9, 6), 60, 6)
            .deeltjes(560, ParticleTypes.END_ROD, new Vec3(0, 9, 6), 60, 6)
            .geluid(470, BlubGeluid.LIEDJE::get, 0.9f, 1f)
            .registreer();

    private SnoesScenes() {
    }

    /** Loads the class (the scene registers itself, on both sides). */
    static void init() {
    }
}
