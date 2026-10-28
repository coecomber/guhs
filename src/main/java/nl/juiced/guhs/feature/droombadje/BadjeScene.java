package nl.juiced.guhs.feature.droombadje;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * Snoes' first bubble bath (the end of the Wolkenbadje's chapter). Positions are relative to the island's centre block, like
 * badje_route.json (tools/features/droombadje.py): Snoes' little bath is centred on (-7.5, 2, 22.5), Snoes stands south of it
 * at (-7.5, 1, 25.5). Texts: scene.guhs.droombadje_snoes_bad.*.
 */
public final class BadjeScene {
    private static final Vec3 IN_BAD = new Vec3(-7.5, 2.15, 22.5), SNOES = new Vec3(-7.5, 1, 25.5), RAND = new Vec3(-7.5, 3, 24.2),
            SPELER = new Vec3(-3.5, 1, 26.5);

    public static final Cutscene BAD = Cutscene.maak("droombadje_snoes_bad").duur(260)
            .npc("snoes", Kind.SNOES, SNOES, 180f)
            .speler(SPELER, 135f)
            .verbergEcht(3.0)
            .camera(0, new Vec3(-1.5, 4.5, 29.5), new Vec3(-7.5, 2, 23.5))
            .zeg(10, "snoes", "kijk", 60)
            .camera(70, new Vec3(-3.5, 3.5, 27.5), new Vec3(-7.5, 2.4, 23.5))
            .loop("snoes", 70, 100, RAND)
            .zeg(80, "snoes", "teen", 50)
            .loop("snoes", 115, 125, IN_BAD)
            .zeg(118, "snoes", "plons", 30)
            .geluid(118, () -> SoundEvents.GENERIC_SPLASH, 1f, 1.2f)
            .deeltjes(118, ParticleTypes.SPLASH, IN_BAD.add(0, 0.6, 0), 60, 1.2)
            .deeltjes(120, ParticleTypes.BUBBLE_POP, IN_BAD.add(0, 0.8, 0), 40, 1.0)
            .cameraKnip(126, new Vec3(-5.5, 4, 25.5), new Vec3(-7.5, 2.4, 22.5))
            .geluid(140, () -> SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1f, 1.4f)
            .deeltjes(140, ParticleTypes.BUBBLE_POP, IN_BAD.add(0, 1.2, 0), 50, 1.4)
            .deeltjes(160, ParticleTypes.END_ROD, IN_BAD.add(0, 2.5, 0), 20, 1.5)
            .zeg(150, "snoes", "blub", 50)
            .deeltjes(200, ParticleTypes.HEART, IN_BAD.add(0, 1.5, 0), 12, 0.8)
            .geluid(200, () -> SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.4f)
            .zeg(188, "snoes", "dank", 32)
            .camera(250, new Vec3(-2.5, 6, 30.5), new Vec3(-7.5, 2, 22.5))
            .zeg(222, "", "verteller", 36)
            .registreer();

    /** Loads the class (registers the scene on both sides: DroombadjeFeature.register runs on both). */
    static void init() {
    }

    private BadjeScene() {
    }
}
