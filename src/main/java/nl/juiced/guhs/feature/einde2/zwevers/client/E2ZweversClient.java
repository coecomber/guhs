package nl.juiced.guhs.feature.einde2.zwevers.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.Elementdier;
import nl.juiced.guhs.feature.einde2.zwevers.KruimelkwalEntity;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;

/**
 * einde2, slice zwevers, client side: the GeckoLib renderers of the six creatures (models, animations, textures and glow
 * masks from tools/features/einde2_zwevers_modellen.py).
 * <ul>
 *   <li>the Kruimelkwal is see-through and glows ({@code kruimelkwal_glowmask.png});</li>
 *   <li>the Leegtewalvisguh (drawn at its scale attribute, {@link LeegtewalvisguhEntity#SCHAAL} times its model) is
 *       never culled by its (small) own box, and its stars and knabbelkoor flowers glow;</li>
 *   <li>the four element animals: their own model, the "head" bone follows where they look.</li>
 * </ul>
 */
public final class E2ZweversClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(E2ZweversClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(E2ZweversFeature.KRUIMELKWAL.get(), KwalRenderer::new);
        event.registerEntityRenderer(E2ZweversFeature.LEEGTEWALVISGUH.get(), WalvisRenderer::new);
        event.registerEntityRenderer(E2ZweversFeature.LEEGTEWALVISGUH_RUG.get(), NoopRenderer::new);   // (never drawn: the whale's model is its back)
        event.registerEntityRenderer(E2ZweversFeature.VLEERMUIS_LEMURGUH.get(), c -> new DierRenderer<>(c, "vleermuis_lemurguh", 0.22f));
        event.registerEntityRenderer(E2ZweversFeature.OTTER_PINGUINGUH.get(), c -> new DierRenderer<>(c, "otter_pinguinguh", 0.3f));
        event.registerEntityRenderer(E2ZweversFeature.DAS_MOLGUH.get(), c -> new DierRenderer<>(c, "das_molguh", 0.42f));
        event.registerEntityRenderer(E2ZweversFeature.SCHILDPAD_EENDGUH.get(), c -> new DierRenderer<>(c, "schildpad_eendguh", 0.28f));
    }

    /** An element animal: its own model (the "head" bone follows where it looks). */
    public static class DierRenderer<T extends Elementdier> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public DierRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam)));
            this.shadowRadius = schaduw;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    /** The Kruimelkwal: a see-through bell that glows. */
    public static class KwalRenderer extends GeoEntityRenderer<KruimelkwalEntity, LivingEntityRenderState> {
        public KwalRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("kruimelkwal")));
            this.shadowRadius = 0.2f;
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            return RenderTypes.entityTranslucent(texture);
        }
    }

    /** The Leegtewalvisguh: big, glowing stars, drawn whenever it is there (its own box says nothing about its size). */
    public static class WalvisRenderer extends GeoEntityRenderer<LeegtewalvisguhEntity, LivingEntityRenderState> {
        public WalvisRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("leegtewalvisguh")));
            this.shadowRadius = 0f;
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        protected boolean affectedByCulling(LeegtewalvisguhEntity walvis) {
            return false;
        }
    }

    private E2ZweversClient() {
    }
}
