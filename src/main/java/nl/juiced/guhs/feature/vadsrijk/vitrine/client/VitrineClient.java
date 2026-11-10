package nl.juiced.guhs.feature.vadsrijk.vitrine.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.vadsrijk.vitrine.Vitrine;

/** Y-VITRINE: the client side of the Vadsrijk-vitrine (its renderer). */
public final class VitrineClient {
    private VitrineClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(Vitrine.BE.get(), VitrineRenderer::new));
    }
}
