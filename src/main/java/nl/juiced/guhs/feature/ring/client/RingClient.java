package nl.juiced.guhs.feature.ring.client;

import java.util.Map;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.KnekelRuiterEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ring.client.CastAnimaties.Lijf;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * Client side of bbq2 (ring-kern): the cast of the Knabbelring (models and textures: tools/features/ring_modellen.py).
 * <ul>
 *   <li>The ten character kinds get their own model (and Boromika and Smikagol their own animation file: they are Mika's),
 *       and one animator each: the named cutscene animation the scene asked for ({@link CastAnimaties}), a hop when a scene
 *       moves them, the engine's wave, and what is theirs alone: Guhdalf is grey or white (white for a viewer whose chapter
 *       3 is done, or as the actor says: {@code Cast.acteur(kind, "wit")} / the animation names wit and grijs), Araguh
 *       wears his knabbel crown once the viewer's story is done (or "kroon"), beards and long hair sway a little.</li>
 *   <li>Smikagol (the walking entity) and the Knekel-Mika rider have their own renderers; both play the named cutscene
 *       animations on top of their own.</li>
 *   <li>Sam-guh is a guh: his cutscene animations are hooked into the GuhRenderer for the variant SAM_GUH.</li>
 * </ul>
 */
public final class RingClient {
    /** The bones of Guhdalf's two looks. */
    private static final String[] GRIJS = {"grijs_hoed", "grijs_mantel", "grijs_knop"}, WIT = {"wit_hoed", "wit_mantel", "wit_knop"};
    /** What sways on whom (a beard, long hair). */
    private static final Map<GuhNpcEntity.Kind, String> WUIFT = Map.of(GuhNpcEntity.Kind.GUHDALF, "guhdalf_baard", GuhNpcEntity.Kind.GIMGUH, "gimguh_baard",
            GuhNpcEntity.Kind.LEGUHLAS, "leguhlas_haar", GuhNpcEntity.Kind.GUHROND, "guhrond_haar", GuhNpcEntity.Kind.GUHLADRIEL, "guhladriel_haar");

    /** Who holds something long in a paw (that arm keeps still in a scene). */
    private static final Map<GuhNpcEntity.Kind, Lijf> VOL = Map.of(GuhNpcEntity.Kind.GUHDALF, Lijf.RECHTS_VOL, GuhNpcEntity.Kind.GIMGUH, Lijf.RECHTS_VOL,
            GuhNpcEntity.Kind.LEGUHLAS, Lijf.LINKS_VOL, GuhNpcEntity.Kind.MERRIE, Lijf.LINKS_VOL, GuhNpcEntity.Kind.PIPPGUH, Lijf.LINKS_VOL);

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingFeature.SMIKAGOL.get(), ctx -> new WezenRenderer<SmikagolEntity>(ctx, "smikagol", 0.4f, 0.85f, Lijf.VIERPOOT));
            event.registerEntityRenderer(RingFeature.KNEKEL_RUITER.get(), ctx -> new WezenRenderer<KnekelRuiterEntity>(ctx, "knekel_ruiter", 0.8f, 1f, Lijf.ZITTEND));
        });
        for (GuhNpcEntity.Kind kind : Cast.KINDS) {
            boolean mika = kind == GuhNpcEntity.Kind.BOROMIKA || kind == GuhNpcEntity.Kind.SMIKAGOL;
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
            if (mika) {
                SittingGuhRenderers.NPC_ANIMATIES.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
            }
            SittingGuhRenderers.NPC_ANIMATORS.put(kind, animator(kind, mika ? Lijf.VIERPOOT : VOL.getOrDefault(kind, Lijf.ZITTEND)));
        }
        // Sam-guh in a scene: the same names, on four paws
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (frame.variant != GuhVariant.SAM_GUH) {
                return;
            }
            GuhRenderFrame.BoneMove move = cutscene(guh, partialTick, Lijf.VIERPOOT);
            if (move != null) {
                frame.bones(move);
            }
        });
    }

    /** The bone moves of the cutscene animation this entity plays right now (null: none). */
    @Nullable
    static GuhRenderFrame.BoneMove cutscene(LivingEntity e, float partialTick, Lijf lijf) {
        String naam = Cutscenes.animatie(e);
        GuhRenderFrame.BoneMove move = naam.isEmpty() ? null : CastAnimaties.maak(naam, Cutscenes.animatieTicks(e) + partialTick, lijf);
        if (e.swinging) {
            move = CastAnimaties.samen(move, CastAnimaties.zwaai(e.getAttackAnim(partialTick), lijf));
        }
        return move;
    }

    private static SittingGuhRenderers.NpcAnimator animator(GuhNpcEntity.Kind kind, Lijf lijf) {
        return (npc, tick) -> {
            float t = (float) tick, partial = (float) (tick - Math.floor(tick));
            GuhRenderFrame.BoneMove move = cutscene(npc, partial, lijf);
            // a scene walks this character somewhere: it hops along
            double dx = npc.getX() - npc.xo, dz = npc.getZ() - npc.zo;
            float snelheid = (float) Math.sqrt(dx * dx + dz * dz);
            if (snelheid > 0.01f) {
                move = CastAnimaties.samen(move, CastAnimaties.huppel(t, snelheid));
            }
            String wuift = WUIFT.get(kind);
            if (wuift != null) {
                float zwaai = Mth.sin(t * 0.045f) * 0.035f;
                move = CastAnimaties.samen(move, b -> b.ifPresent(wuift, s -> s.setRotX(s.getRotX() + zwaai)));
            }
            if (kind == GuhNpcEntity.Kind.GUHDALF) {
                String[] weg = wit(npc) ? GRIJS : WIT;
                move = CastAnimaties.samen(move, b -> {
                    for (String bot : weg) {
                        b.ifPresent(bot, s -> s.skipRender(true).skipChildrenRender(true));
                    }
                });
            } else if (kind == GuhNpcEntity.Kind.ARAGUH && !gekroond(npc)) {
                move = CastAnimaties.samen(move, b -> b.ifPresent("araguh_kroon", s -> s.skipRender(true).skipChildrenRender(true)));
            }
            return move;
        };
    }

    /** Guhdalf de Witte? As the scene says (the animation, the actor's look), else: for a viewer whose chapter 3 is done. */
    static boolean wit(GuhNpcEntity npc) {
        String anim = Cutscenes.animatie(npc);
        if (anim.equals("wit") || anim.equals("grijs")) {
            return anim.equals("wit");
        }
        String look = npc.roleData.getStringOr(Cast.LOOK, "");
        if (!look.isEmpty()) {
            return look.equals("wit");
        }
        Verhaallijn mijn = Ring.lijn(3);
        return Verhaallijn.stapClient(mijn.id()) >= mijn.stappen();
    }

    /** Araguh with his knabbel crown? As the scene says, else: for a viewer whose whole story is done. */
    static boolean gekroond(GuhNpcEntity npc) {
        if (Cutscenes.animatie(npc).equals("kroon") || npc.roleData.getStringOr(Cast.LOOK, "").equals("kroon")) {
            return true;
        }
        Verhaallijn einde = Ring.lijn(6);
        return Verhaallijn.stapClient(einde.id()) >= einde.stappen();
    }

    /** Smikagol and the Knekel-Mika rider: their own model and animation file, a turning head, the cutscene animations on top. */
    static class WezenRenderer<T extends LivingEntity & com.geckolib.animatable.GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final Lijf lijf;

        WezenRenderer(EntityRendererProvider.Context context, String naam, float schaduw, float schaal, Lijf lijf) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(naam)));
            this.shadowRadius = schaduw;
            this.lijf = lijf;
            if (schaal != 1f) {
                withScale(schaal, schaal);
            }
        }

        @Override
        public void addRenderData(T wezen, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            GuhRenderFrame.BoneMove move = cutscene(wezen, partialTick, lijf);
            if (move != null) {
                state.addGeckolibData(SittingGuhRenderers.NPC_BONES, move);
            }
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            GuhRenderFrame.BoneMove move = info.getGeckolibData(SittingGuhRenderers.NPC_BONES);
            if (move != null) {
                move.apply(bones);
            }
        }
    }

    private RingClient() {
    }
}
