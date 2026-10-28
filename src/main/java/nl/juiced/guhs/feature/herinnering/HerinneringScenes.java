package nl.juiced.guhs.feature.herinnering;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The three retro films of the Herinneringseiland (1.8.0 revamp): three little memories of Snoes, from long before he had a
 * place. Each plays at its little scene (the anchor of its {@link HerinneringBellen.Bel}, in the island's own unrotated
 * coordinates); the real memory-Snoes (and the Blub of the nest) are hidden while it plays. Texts:
 * {@code scene.guhs.herinnering_<naam>.*} (tools/features/herinnering.py). The last one, the stars, opens after the other two.
 */
public final class HerinneringScenes {
    /** 1. The first kaasknabbel (anchor: the golden knabbel in the grass). */
    public static final Cutscene KNABBEL = Cutscene.maak("herinnering_knabbel").duur(440).verbergEcht(4)
            .npc("snoes", Kind.SNOES, new Vec3(3.5, 0, 0.5), 90)
            .zwart(0, 12)
            .camera(0, new Vec3(-2.5, 1.6, 4.5), new Vec3(1.5, 0.5, 0.5)).camera(430, new Vec3(-1.5, 1.2, 3.0), new Vec3(1.0, 0.4, 0.5))
            .zeg(20, "", "k1", 60)
            .zeg(85, "", "k2", 60)
            .loop("snoes", 120, 170, new Vec3(1.5, 0, 0.5))
            .zeg(150, "snoes", "k3", 60)
            .zeg(215, "", "k4", 70)
            .zeg(290, "snoes", "k5", 40)
            .deeltjes(292, ParticleTypes.HAPPY_VILLAGER, new Vec3(1.0, 0.8, 0.5), 14, 0.6)
            .zeg(335, "", "k6", 95)
            .registreer();

    /** 2. The first night with a Blub (anchor: the nest under its own little night sky). */
    public static final Cutscene BLUB = Cutscene.maak("herinnering_blub").duur(490).verbergEcht(5)
            .npc("snoes", Kind.SNOES, new Vec3(0.5, 0.45, 0.5), 0)
            .acteur("blub", BlubFeature.BLUBGUH, new Vec3(-0.5, 0, -3.5), 0, HerinneringScenes::blubje)
            .zwart(0, 20)
            .camera(0, new Vec3(2.5, 2.2, -2.5), new Vec3(0, 0.5, 0.5)).camera(480, new Vec3(1.8, 1.5, -1.8), new Vec3(0, 0.4, 0.5))
            .zeg(25, "", "b1", 70)
            .zeg(105, "snoes", "b2", 70)
            .loop("blub", 150, 200, new Vec3(-0.5, 0, 0.5))
            .zeg(180, "", "b3", 70)
            .zeg(255, "blub", "b4", 30)
            .deeltjes(260, ParticleTypes.HEART, new Vec3(-0.2, 1.0, 0.5), 8, 0.4)
            .zeg(295, "", "b5", 80)
            .zeg(385, "", "b6", 95)
            .deeltjes(390, ParticleTypes.END_ROD, new Vec3(0.2, 1.2, 0.5), 6, 0.3)
            .registreer();

    /** 3. The first stargazing (anchor: the top of the little hill). */
    public static final Cutscene STERREN = Cutscene.maak("herinnering_sterren").duur(540).verbergEcht(4)
            .npc("snoes", Kind.SNOES, new Vec3(0.5, 0, 1.5), 180)
            .zwart(0, 15)
            .camera(0, new Vec3(2.5, 1.4, 4.5), new Vec3(0.5, 6, -8)).camera(260, new Vec3(1.5, 1.0, 3.0), new Vec3(0.5, 10, -10))
            .zeg(20, "", "s1", 70)
            .zeg(95, "snoes", "s2", 60)
            .zeg(165, "", "s3", 70)
            .deeltjes(170, ParticleTypes.END_ROD, new Vec3(0.5, 14, -14), 30, 3.0)
            .zeg(240, "snoes", "s4", 70)
            .cameraKnip(280, new Vec3(0.5, 1.6, -1.5), new Vec3(0.5, 0.6, 1.5)).camera(420, new Vec3(1.2, 1.4, -1.0), new Vec3(0.5, 0.6, 1.5))
            .zeg(290, "", "s5", 50)
            .deeltjes(300, ParticleTypes.END_ROD, new Vec3(0.5, 12, -12), 20, 2.0)
            .zeg(345, "snoes", "s6", 70)
            .cameraKnip(425, new Vec3(3.5, 2.0, 5.0), new Vec3(0.5, 8, -10)).camera(535, new Vec3(4.0, 2.5, 6.0), new Vec3(0.5, 12, -12))
            .zeg(425, "", "s7", 105)
            .registreer();

    /** The films in the order they are told; the last one (the stars) opens after the other two. */
    public static final List<Cutscene> ALLE = List.of(KNABBEL, BLUB, STERREN);

    /** The Blub of the second film: a plain blue one, a little bit guh. */
    private static void blubje(CompoundTag tag) {
        tag.putString("BlubSoort", "blauw");
        tag.putInt("Meter", 30);
        tag.putInt("Tint", 0);
        tag.putFloat("Grootte", 1.0f);
    }

    /** Loads the class, so both sides know the scenes (Cutscene.registreer). */
    static void laad() {
    }

    private HerinneringScenes() {
    }
}
