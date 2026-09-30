package nl.juiced.guhs.feature.speelgoed.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;

/**
 * Client side of the speelgoed (2.10): the knabbelbal renderer (a rolling fluffy guh ball), the invisible seats, and
 * the swinging parts of the wip (its plank) and the schommel (its seat) drawn by {@link ToestelRenderer}.
 */
public final class SpeelgoedClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SpeelgoedFeature.KNABBELBAL.get(), KnabbelbalRenderer::new);
            event.registerEntityRenderer(SpeelgoedFeature.ZITJE.get(), NoopRenderer::new);
            event.registerBlockEntityRenderer(SpeelgoedFeature.TOESTEL_BE.get(), ToestelRenderer::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(KnabbelbalRenderer.LAYER, KnabbelbalRenderer::createLayer));
        modBus.addListener((ModelEvent.RegisterAdditional event) -> {
            event.register(ToestelRenderer.PLANK);
            event.register(ToestelRenderer.ZITJE);
        });
    }

    private SpeelgoedClient() {
    }
}
