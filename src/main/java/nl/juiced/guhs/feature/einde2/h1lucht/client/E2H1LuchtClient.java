package nl.juiced.guhs.feature.einde2.h1lucht.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.h1lucht.E2H1LuchtFeature;
import nl.juiced.guhs.feature.einde2.h1lucht.KaasbolEntity;
import nl.juiced.guhs.feature.einde2.h1lucht.LuchtbalEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * einde2 phase B, slice h1_lucht, client side: the renderers of the kaasbol (its cheese, and its glow: the core, the holes
 * and the seam light up) and of the luchtbal ball (a glowing swirl of air).
 */
public final class E2H1LuchtClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(E2H1LuchtFeature.KAASBOL.get(), KaasbolRenderer::new);
            event.registerEntityRenderer(E2H1LuchtFeature.LUCHTBAL.get(), LuchtbalRenderer::new);
        });
    }

    /** The kaasbol: geckolib/models/entity/kaasbol.geo.json, glow layer kaasbol_gloed.png. */
    public static class KaasbolRenderer extends GeoEntityRenderer<KaasbolEntity, EntityRenderState> {
        private static final Identifier GLOED = Guhs.id("textures/entity/kaasbol_gloed.png");

        public KaasbolRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KaasbolEntity>(Guhs.id("kaasbol")));
            this.shadowRadius = 0.8f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (info.willRender()) {
                        getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(GLOED));
                    }
                }
            });
        }
    }

    /** The luchtbal ball: geckolib/models/entity/einde2_h1_lucht_luchtbal.geo.json, glowing a little all over. */
    public static class LuchtbalRenderer extends GeoEntityRenderer<LuchtbalEntity, EntityRenderState> {
        private static final Identifier GLOED = Guhs.id("textures/entity/einde2_h1_lucht_luchtbal_gloed.png");

        public LuchtbalRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LuchtbalEntity>(Guhs.id("einde2_h1_lucht_luchtbal")));
            this.shadowRadius = 0.3f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (info.willRender()) {
                        getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(GLOED));
                    }
                }
            });
        }
    }

    private E2H1LuchtClient() {
    }
}
