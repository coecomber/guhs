package nl.juiced.guhs.feature.park.plek.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.park.plek.Knabbelkoi;
import nl.juiced.guhs.feature.park.plek.PlekA;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * 1.4.2 (Het Knabbelpark, plekjes A), client side: the seat of the swings and seesaws is invisible (the block shows the
 * seat), the Knabbelkoi is the koi of the Klaterdal (koi.geo.json and its animations) with its own kaasknabbel texture.
 * The playground blocks are plain block models.
 */
public final class PlekAClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(PlekA.ZITJE.get(), NoopRenderer::new);
            event.registerEntityRenderer(PlekA.KNABBELKOI.get(), KnabbelkoiRenderer::new);
        });
    }

    /** The koi model in kaasknabbel colours. */
    public static class KnabbelkoiRenderer extends GeoEntityRenderer<Knabbelkoi, LivingEntityRenderState> {
        public KnabbelkoiRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<Knabbelkoi>(Guhs.id("koi")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/entity/knabbelkoi.png");
                }
            });
            this.shadowRadius = 0.25f;
        }
    }

    private PlekAClient() {
    }
}
