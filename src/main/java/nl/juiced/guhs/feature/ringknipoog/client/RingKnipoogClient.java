package nl.juiced.guhs.feature.ringknipoog.client;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.ring.client.CastAnimaties;
import nl.juiced.guhs.feature.ring.client.CastAnimaties.Lijf;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * Client side of bbq2 (ring-knipogen): the guests of the winks act. Their models, textures and own animations are their
 * stories'; here they only learn to play the cast's named cutscene animations (ring-kern's {@link CastAnimaties}: praat,
 * kijk, schaam, grijp, juich, eet, wijs...) WHILE THEY ARE AN ACTOR of a scene: outside a cutscene nothing about them
 * changes.
 * <ul>
 *   <li>the story guhs Baltoguh, Mewtwo-guh and the 626-guh: a hook in the GuhRenderer, on four paws;</li>
 *   <li>Boris the goose, Professor Knabbelkloon and Pad-guh (sitting-guh characters): an animator next to the one they may
 *       have already, with the hop of a character that a scene moves; Boris also knows "vlieg" (he flaps and bobs).</li>
 * </ul>
 */
public final class RingKnipoogClient {
    public static void init(IEventBus modBus) {
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if ((frame.variant == GuhVariant.BALTOGUH || frame.variant == GuhVariant.MEWTWO || frame.variant == GuhVariant.STITCH626)
                    && CutsceneSpeler.isActeur(guh)) {
                GuhRenderFrame.BoneMove move = scene(guh, partialTick, Lijf.VIERPOOT);
                if (move != null) {
                    frame.bones(move);
                }
            }
        });
        gast(GuhNpcEntity.Kind.BORIS, true);
        gast(GuhNpcEntity.Kind.KNABBELKLOON, false);
        gast(GuhNpcEntity.Kind.PADGUH, false);
    }

    /** The bone moves of the cutscene animation this actor plays right now, and of the engine's wave (null: none). */
    @Nullable
    private static GuhRenderFrame.BoneMove scene(LivingEntity e, float partialTick, Lijf lijf) {
        String naam = Cutscenes.animatie(e);
        GuhRenderFrame.BoneMove move = naam.isEmpty() ? null : CastAnimaties.maak(naam, Cutscenes.animatieTicks(e) + partialTick, lijf);
        if (e.swinging) {
            move = CastAnimaties.samen(move, CastAnimaties.zwaai(e.getAttackAnim(partialTick), lijf));
        }
        return move;
    }

    /** A sitting-guh character as a guest: the named animations and the hop when a scene moves it, only while it is an actor. */
    private static void gast(GuhNpcEntity.Kind kind, boolean vliegt) {
        SittingGuhRenderers.NpcAnimator gast = (npc, tick) -> {
            if (!CutsceneSpeler.isActeur(npc)) {
                return null;
            }
            float t = (float) tick, partial = (float) (tick - Math.floor(tick));
            if (vliegt && Cutscenes.animatie(npc).equals("vlieg")) {
                return vlieg(t);
            }
            GuhRenderFrame.BoneMove move = scene(npc, partial, Lijf.ZITTEND);
            double dx = npc.getX() - npc.xo, dz = npc.getZ() - npc.zo;
            float snelheid = (float) Math.sqrt(dx * dx + dz * dz);
            if (snelheid > 0.01f) {
                move = CastAnimaties.samen(move, CastAnimaties.huppel(t, snelheid));
            }
            return move;
        };
        SittingGuhRenderers.NPC_ANIMATORS.merge(kind, gast,
                (eigen, nieuw) -> (npc, tick) -> CastAnimaties.samen(eigen.animeer(npc, tick), nieuw.animeer(npc, tick)));
    }

    /** Boris in the air: the wings beat, the body bobs, the neck stretches out. */
    private static GuhRenderFrame.BoneMove vlieg(float t) {
        float slag = Mth.sin(t * 0.9f) * 0.75f, wip = Mth.sin(t * 0.9f + 1.2f) * 1.4f;
        return b -> {
            b.ifPresent("wing_left", s -> s.setRotZ(s.getRotZ() - 0.55f - slag));
            b.ifPresent("wing_right", s -> s.setRotZ(s.getRotZ() + 0.55f + slag));
            b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + wip));
            b.ifPresent("hals", s -> s.setRotX(s.getRotX() + 0.25f));
            b.ifPresent("leg_left", s -> s.setRotX(s.getRotX() + 0.6f));
            b.ifPresent("leg_right", s -> s.setRotX(s.getRotX() + 0.6f));
        };
    }

    private RingKnipoogClient() {
    }
}
