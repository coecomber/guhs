package nl.juiced.guhs.feature.herinnering;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * The five retro films of the Herinneringseiland: the Guh-familiedag as Snoes saw it from the bushes. Each plays at its
 * scene (the anchor of its {@link HerinneringBellen.Bel}, in the island's own unrotated coordinates) with the memory's
 * people as actors; the real people there are hidden while it plays. Texts: {@code scene.guhs.herinnering_<naam>.*}
 * (tools/features/herinnering.py). The last one is Snoes himself.
 */
public final class HerinneringScenes {
    /** 1. The guh-quiz (anchor: the quiz stall, the quizmaster's spot). */
    public static final Cutscene QUIZ = Cutscene.maak("herinnering_quiz").duur(380).verbergEcht(9)
            .npc("master", Kind.SPELLEIDERGUH, new Vec3(0.5, 0, 0.5), 180)
            .npc("kind", Kind.GUHKINDJE, new Vec3(0.5, 0, -4.5), 0)
            .npc("opa", Kind.OPA_GUH, new Vec3(-2.5, 0, -4.5), 0)
            .npc("kind2", Kind.GUHKINDJE, new Vec3(3.5, 0, -4.5), 0)
            .zwart(0, 12)
            .camera(0, new Vec3(-6, 3.5, -10), new Vec3(0.5, 1, -2)).camera(240, new Vec3(5, 2.5, -8.5), new Vec3(0.5, 1, -2))
            .zeg(20, "", "q1", 70)
            .zeg(100, "master", "q2", 60)
            .zeg(170, "kind", "q3", 70)
            .deeltjes(180, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 1.5, -4.5), 12, 1.5)
            .cameraKnip(250, new Vec3(-8, 3, 4), new Vec3(-23, 1.5, 13)).camera(370, new Vec3(-11, 2.5, 6), new Vec3(-23, 1.2, 13))
            .zeg(260, "", "q4", 90)
            .registreer();

    /** 2. Guhtje prik (anchor: the middle of the play mat). */
    public static final Cutscene PRIK = Cutscene.maak("herinnering_prik").duur(380).verbergEcht(8)
            .npc("kind", Kind.GUHKINDJE, new Vec3(0.5, 0.07, -2.5), 0)
            .npc("kijker1", Kind.GUHKINDJE, new Vec3(-2.5, 0.07, -1.5), 0)
            .npc("kijker2", Kind.OPA_GUH, new Vec3(3.5, 0.07, -1.5), 0)
            .zwart(0, 12)
            .camera(0, new Vec3(-5, 3, -7), new Vec3(0.5, 1, 1)).camera(240, new Vec3(-3, 2.2, -5), new Vec3(1.5, 1, 2.5))
            .zeg(20, "", "p1", 70)
            .loop("kind", 60, 110, new Vec3(-0.5, 0.07, 1.0)).loop("kind", 115, 160, new Vec3(2.0, 0.07, 3.0))
            .zeg(110, "kind", "p2", 70)
            .zeg(190, "", "p3", 70)
            .deeltjes(195, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 3.5, 3.5), 10, 1.0)
            .cameraKnip(260, new Vec3(6, 2.5, 2), new Vec3(12, 1.2, 8)).camera(370, new Vec3(8, 2, 4), new Vec3(12, 1.2, 8.5))
            .zeg(270, "", "p4", 90)
            .registreer();

    /** 3. The roze guh koeken (anchor: the middle of the long table). */
    public static final Cutscene KOEKEN = Cutscene.maak("herinnering_koeken").duur(400).verbergEcht(13)
            .npc("opa", Kind.OPA_GUH, new Vec3(-0.5, 0.5, -0.5), 180)
            .npc("kind1", Kind.GUHKINDJE, new Vec3(-4.5, 0.5, -0.5), 180)
            .npc("kind2", Kind.GUHKINDJE, new Vec3(2.5, 0.5, 1.5), 0)
            .npc("kind3", Kind.GUHKINDJE, new Vec3(-2.5, 0.5, 1.5), 0)
            .zwart(0, 12)
            .camera(0, new Vec3(-12, 5, -6), new Vec3(0.5, 1, 0.5)).camera(180, new Vec3(-6, 3.5, -5), new Vec3(0.5, 1.2, 0.5))
            .zeg(20, "", "k1", 70)
            .zeg(110, "opa", "k2", 60)
            .zeg(190, "", "k3", 70)
            .deeltjes(200, ParticleTypes.HEART, new Vec3(0.5, 2, 0.5), 16, 4.0)
            .cameraKnip(280, new Vec3(11.5, 2.4, -2.5), new Vec3(9.5, 1.3, 0.5)).camera(390, new Vec3(10.5, 2.0, -1.5), new Vec3(9.5, 1.2, 0.5))
            .zeg(290, "", "k4", 90)
            .registreer();

    /** 4. Oma and the family photo at the Guhssenburg (anchor: in front of Oma's bench). */
    public static final Cutscene OMA = Cutscene.maak("herinnering_oma").duur(420).verbergEcht(9)
            .npc("oma", Kind.DROOM_OMAGUH, new Vec3(1.0, 0.5, -1.5), 0)
            .npc("kind1", Kind.GUHKINDJE, new Vec3(-0.5, 0.07, 2.5), 180)
            .npc("kind2", Kind.GUHKINDJE, new Vec3(1.5, 0.07, 2.5), 180)
            .npc("kind3", Kind.GUHKINDJE, new Vec3(-2.5, 0.07, 1.5), 150)
            .npc("foto1", Kind.GUHKINDJE, new Vec3(-40, 0, 0.5), 0)
            .npc("foto2", Kind.OPA_GUH, new Vec3(-37, 0, 0.5), 0)
            .zwart(0, 12)
            .camera(0, new Vec3(3, 2.5, 6), new Vec3(1, 1, -1)).camera(190, new Vec3(-2, 3, 5), new Vec3(0, 0.8, 1.5))
            .zeg(20, "oma", "o1", 70)
            .zeg(110, "", "o2", 70)
            .zeg(200, "oma", "o3", 60)
            .zwart(262, 274)
            .cameraKnip(270, new Vec3(-38.5, 3.5, 9), new Vec3(-38.5, 3, -6)).camera(410, new Vec3(-38.5, 4.5, 11), new Vec3(-38.5, 3.5, -6))
            .zeg(290, "", "o4", 100)
            .deeltjes(300, ParticleTypes.END_ROD, new Vec3(-38.5, 1.5, 4.5), 20, 0.4)
            .registreer();

    /** 5. Snoes himself, between the leaves (anchor: his spot in the bushes). */
    public static final Cutscene SNOES = Cutscene.maak("herinnering_snoes").duur(540).verbergEcht(5)
            .npc("snoes", Kind.SNOES, new Vec3(0.5, 0, -1.2), 180)
            .zwart(0, 20)
            .camera(0, new Vec3(0.5, 1.5, 2.6), new Vec3(0.5, 1.2, -12)).camera(160, new Vec3(1.2, 1.4, 1.6), new Vec3(0.5, 1, -12))
            .zeg(25, "", "s1", 60)
            .zeg(100, "", "s2", 80)
            .cameraKnip(185, new Vec3(0.5, 1.1, -4.2), new Vec3(0.5, 0.7, -1.2)).camera(330, new Vec3(0.8, 1.0, -3.4), new Vec3(0.5, 0.6, -1.2))
            .zeg(195, "snoes", "s3", 70)
            .zeg(275, "", "s4", 90)
            .zwart(360, 385)
            .cameraKnip(372, new Vec3(-2.5, 1.0, -3.5), new Vec3(0.5, 0.5, -1.2)).camera(530, new Vec3(-1.5, 1.3, -4.5), new Vec3(0.5, 0.6, -1.2))
            .zeg(395, "snoes", "s5", 60)
            .zeg(460, "", "s6", 70)
            .registreer();

    /** The films in the order they are told; the last one (Snoes) opens after the other four. */
    public static final List<Cutscene> ALLE = List.of(QUIZ, PRIK, KOEKEN, OMA, SNOES);

    /** Loads the class, so both sides know the scenes (Cutscene.registreer). */
    static void laad() {
    }

    private HerinneringScenes() {
    }
}
