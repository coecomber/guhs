package nl.juiced.guhs.feature.raket;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The launch of the Kaasraket (texts scene.guhs.raket_lancering.*), anchored on the Lanceerplatform (the rocket stands
 * above it, see {@link RaketBouw}): the countdown, the engines, smoke everywhere, the camera rising with the rocket into the
 * sky, black, and the Kaasmaan (Raket.naLancering).
 */
public final class RaketScene {
    private static final Vec3 VOET = new Vec3(0.5, 1, 0.5), MIDDEN = new Vec3(0.5, 8, 0.5);

    public static final Cutscene LANCERING = Cutscene.maak("raket_lancering").duur(220)
            .speler(new Vec3(7.5, 1, 7.5), 135f)
            .camera(0, new Vec3(10.5, 5, 13.5), MIDDEN)
            .zeg(10, "", "klaar", 30)
            .zeg(45, "", "aftel5", 18)
            .zeg(63, "", "aftel4", 18)
            .zeg(81, "", "aftel3", 18)
            .zeg(99, "", "aftel2", 18)
            .zeg(117, "", "aftel1", 18)
            .geluid(45, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 0.8f)
            .geluid(63, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 0.9f)
            .geluid(81, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.0f)
            .geluid(99, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.1f)
            .geluid(117, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.3f)
            .cameraKnip(135, new Vec3(6.5, 2, 8.5), VOET.add(0, 1, 0))
            .zeg(135, "", "vuur", 30)
            .geluid(135, () -> SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f)
            .geluid(137, () -> SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f)
            .deeltjes(135, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, VOET, 80, 3)
            .deeltjes(138, ParticleTypes.FLAME, VOET, 60, 1.5)
            .deeltjes(145, ParticleTypes.CLOUD, VOET, 80, 4)
            .deeltjes(150, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, VOET.add(0, 2, 0), 80, 4)
            .schud(135, 0.6f, 60)
            .camera(200, new Vec3(12.5, 40, 16.5), new Vec3(0.5, 60, 0.5))
            .deeltjes(170, ParticleTypes.FIREWORK, MIDDEN.add(0, 10, 0), 60, 4)
            .zeg(165, "", "weg", 35)
            .zwart(205, 220)
            .registreer();

    /** Loads the class (registers the scene on both sides: RaketFeature.register runs on both). */
    static void init() {
    }

    private RaketScene() {
    }
}
