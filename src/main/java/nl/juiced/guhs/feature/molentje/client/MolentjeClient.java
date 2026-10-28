package nl.juiced.guhs.feature.molentje.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.juiced.guhs.feature.molentje.MolentjeFeature;

/** Client side of the Maanmolentje: the turning sails ({@link WiekenRenderer}). */
public final class MolentjeClient {
    private MolentjeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(MolentjeFeature.WIEKEN_BE.get(), WiekenRenderer::new));
        modBus.addListener((ModelEvent.RegisterStandalone event) -> WiekenRenderer.registerModels(event));
    }
}
