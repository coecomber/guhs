package nl.juiced.guhs.feature.gatenkaas.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature;
import nl.juiced.guhs.feature.gatenkaas.VadswakerEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * Client side of the gatenkaas caves: the Vadswaker (a big blind Mika: geo/entity/vadswaker.geo.json, with glowing
 * cheese holes in its fur from vadswaker_glowmask.png). The blocks only need their models (render types in the JSON).
 */
public final class GatenkaasClient {
    public static final float SCALE = 2.1f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(GatenkaasFeature.VADSWAKER.get(), context -> {
            GeoEntityRenderer<VadswakerEntity> renderer = new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<VadswakerEntity>(Guhs.id("vadswaker"))) {
                {
                    this.shadowRadius = 0.45f * SCALE;
                }
            };
            renderer.withScale(SCALE);
            renderer.addRenderLayer(new AutoGlowingGeoLayer<>(renderer));
            return renderer;
        }));
    }

    private GatenkaasClient() {
    }
}
