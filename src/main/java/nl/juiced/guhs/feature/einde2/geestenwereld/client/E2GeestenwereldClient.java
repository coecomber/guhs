package nl.juiced.guhs.feature.einde2.geestenwereld.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice geestenwereld, client side: the sky of the Geestenwereld ({@link E2GeestenwereldSky}, the attribute
 * {@code "neoforge:custom_skybox": "guhs:geestenwereld"} of the dimension type). The blocks' render types are in their
 * models (tools/features/einde2_geestenwereld.py).
 */
public final class E2GeestenwereldClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent event) ->
                event.registerSkyboxRenderer(Guhs.id("geestenwereld"), new E2GeestenwereldSky()));
    }

    private E2GeestenwereldClient() {
    }
}
