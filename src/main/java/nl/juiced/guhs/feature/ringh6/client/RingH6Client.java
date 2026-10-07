package nl.juiced.guhs.feature.ringh6.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.feature.ring.client.CastAnimaties;
import nl.juiced.guhs.feature.ringh6.GekooideRookguhEntity;
import nl.juiced.guhs.feature.ringh6.KrokanteSmikagolEntity;
import nl.juiced.guhs.feature.ringh6.RingH6Feature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Client side of bbq2 (ring-h6): the renderers of the chapter's three entities.
 * <ul>
 *   <li>The Rookguhje: the Rookguh's own model, texture and animations ({@code rookguh}), {@link RookguhjeRenderer#SCHAAL}
 *       blocks big instead of 3.375, thin and grey (a hungry little thing in a cage).</li>
 *   <li>The falling coal: the gloeiend kooltje as a thrown item, big and at full brightness.</li>
 *   <li>The crispy Smikagol: ring-kern's Smikagol model and animations with the golden brown texture
 *       textures/entity/ringh6_krokante_smikagol.png, and the same named cutscene animations ({@link CastAnimaties}).</li>
 * </ul>
 */
public final class RingH6Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingH6Feature.ROOKGUH.get(), RookguhjeRenderer::new);
            event.registerEntityRenderer(RingH6Feature.VALKOOL.get(), context -> new ThrownItemRenderer<>(context, 2.4f, true));
            event.registerEntityRenderer(RingH6Feature.KROKANTE_SMIKAGOL.get(), KrokantRenderer::new);
        });
    }

    /** The Rookguhje: the Rookguh, a good deal smaller. */
    static class RookguhjeRenderer extends GeoEntityRenderer<GekooideRookguhEntity, LivingEntityRenderState> {
        static final float SCHAAL = 1.25f;

        RookguhjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GekooideRookguhEntity>(Guhs.id("rookguh")));
            this.shadowRadius = 0.5f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            bones.ifPresent("cheeks", b -> b.skipRender(true).skipChildrenRender(true));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * SCHAAL, heightScale * SCHAAL);
        }

        @Override
        public int getRenderColor(GekooideRookguhEntity guh, @Nullable Void related, float partialTick) {
            return ARGB.colorFromFloat(1.0f, 0.92f, 0.9f, 0.92f);
        }
    }

    /** Smikagol, deep-fried: ring-kern's model and animation file, another texture, the cutscene animations on top. */
    static class KrokantRenderer extends GeoEntityRenderer<KrokanteSmikagolEntity, LivingEntityRenderState> {
        KrokantRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KrokanteSmikagolEntity>(Guhs.id("ringh6_krokante_smikagol")).withAltModel(Guhs.id("smikagol"))
                    .withAltAnimations(Guhs.id("smikagol")));
            this.shadowRadius = 0.4f;
            withScale(0.85f, 0.85f);
        }

        @Override
        public void addRenderData(KrokanteSmikagolEntity wezen, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            String naam = Cutscenes.animatie(wezen);
            GuhRenderFrame.BoneMove move = naam.isEmpty() ? null : CastAnimaties.maak(naam, Cutscenes.animatieTicks(wezen) + partialTick, CastAnimaties.Lijf.VIERPOOT);
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

    private RingH6Client() {
    }
}
