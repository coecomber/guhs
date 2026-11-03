package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The courtroom cutscenes of the finale (click-to-continue, verhaal engine), anchored at {@link Rechtbank#anker}: the opening
 * with Morris' first claim, and three times "BEZWAAR, NJEG!" (the third ends with the verdict and Morris' sweet ending).
 * Texts: {@code scene.guhs.<id>.<key>} from tools/features/erfenis_sherlock.py.
 */
public final class SherlockScenes {
    // where everybody stands in the courtroom (relative to the anchor, see Rechtbank)
    static final Vec3 RECHTER = new Vec3(0.5, 2, -8.5), MORRIS = new Vec3(4.5, 0, -2.5), PIM = new Vec3(6.5, 0, -2.5),
            SPELER = new Vec3(-4.5, 0, -2.5), SHERLOCK = new Vec3(-6.5, 0, -2.5), VADSON = new Vec3(-2.5, 0, -2.5),
            OEHOE = new Vec3(-3.5, 0, 2.2), GUHRRIT = new Vec3(3.5, 0, 2.2), OMA = new Vec3(-5.5, 0, 2.2);

    public static final Cutscene BEGIN = cast(Cutscene.maak("sherlock_rechtbank_begin").duur(300))
            .camera(0, new Vec3(0.5, 4.5, 9), new Vec3(0.5, 1.5, -8)).camera(110, new Vec3(0.5, 2.6, 0.5), new Vec3(0.5, 2, -8))
            .geluid(15, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 0.8f)
            .zeg(20, "rechter", "open", 70)
            .zeg(100, "rechter", "zaak", 70)
            .animatie("rechter", 100, "happy")
            .cameraKnip(170, new Vec3(3.6, 1.6, -5.6), MORRIS.add(0, 0.8, 0))
            .zeg(180, "morris", "bewering1", 80)
            .cameraKnip(250, new Vec3(-4.5, 1.7, -6.0), SPELER.add(0, 1.2, 0))
            .zeg(255, "sherlock", "wakker", 40)
            .registreer();

    public static final List<Cutscene> BEZWAAR = List.of(bezwaar(1), bezwaar(2), bezwaar(3));

    private SherlockScenes() {
    }

    private static Cutscene.Builder cast(Cutscene.Builder b) {
        return b.verbergEcht(24).speler(SPELER, 180)
                .npc("rechter", Kind.KNABBELHAMER, RECHTER, 0)
                .npc("morris", Kind.MORRIS_MIKA, MORRIS, 180)
                .npc("pim", Kind.PIM_MIKA, PIM, 180)
                .npc("sherlock", Kind.SHERLOCK_GUH, SHERLOCK, 180)
                .npc("vadson", Kind.DOKTER_VADSON, VADSON, 180)
                .npc("oehoe", Kind.OEHOE, OEHOE, 180)
                .npc("guhrrit", Kind.GUHRRIT_VAKANTIE, GUHRRIT, 180)
                .npc("oma", Kind.DROOM_OMAGUH, OMA, 180);
    }

    private static Cutscene bezwaar(int r) {
        Cutscene.Builder b = cast(Cutscene.maak("sherlock_rechtbank_bezwaar" + r).duur(r < 3 ? 280 : 560))
                .cameraKnip(0, new Vec3(-4.5, 1.6, -5.4), SPELER.add(0, 1.2, 0))
                .zeg(5, "speler", "bezwaar", 50)
                .schud(5, 2.2f, 18)
                .geluid(5, () -> SoundEvents.ANVIL_LAND, 0.5f, 1.6f)
                .deeltjes(6, ParticleTypes.CRIT, SPELER.add(0, 1.6, -0.6), 25, 0.4)
                .zeg(60, "speler", "bewijs", 70)
                .cameraKnip(130, new Vec3(0.5, 3.3, -5.0), RECHTER.add(0, 1.0, 0))
                .zeg(135, "rechter", "toegewezen", 60)
                .geluid(140, () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 0.8f)
                .cameraKnip(200, new Vec3(3.6, 1.6, -5.6), MORRIS.add(0, 0.8, 0))
                .zeg(205, "morris", "reactie", 60);
        if (r < 3) {
            b.zeg(245, "morris", "bewering" + (r + 1), 60);
            return b.registreer();
        }
        // the verdict and Morris' sweet ending
        return b.cameraKnip(270, new Vec3(0.5, 3.3, -5.0), RECHTER.add(0, 1.0, 0))
                .zeg(275, "rechter", "uitspraak", 80)
                .geluid(330, () -> SoundEvents.GENERIC_EAT.value(), 1f, 1.1f)
                .zeg(335, "rechter", "hapje", 40)
                .cameraKnip(380, new Vec3(3.6, 1.6, -5.6), MORRIS.add(0, 0.8, 0))
                .zeg(385, "morris", "zucht", 60)
                .cameraKnip(430, new Vec3(0.5, 3.2, 1.5), new Vec3(0.5, 1, -3))
                .zeg(435, "speler", "plekje", 50)
                .zeg(470, "morris", "bloemetjes", 50)
                .zeg(505, "morris", "knipoog", 50)
                .deeltjes(520, ParticleTypes.HEART, MORRIS.add(0, 1.8, 0), 8, 0.4)
                .zeg(540, "sherlock", "zoals_gezegd", 20)
                .registreer();
    }

    /** Loads the class (registers the scenes on both sides). */
    static void laad() {
    }
}
