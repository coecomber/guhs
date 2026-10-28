package nl.juiced.guhs.feature.slaapzorg.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.juiced.guhs.feature.slaapzorg.SlaapzorgFeature;

/** Client side of slaapzorg: the Mobiel's turning moon and stars ({@link MobielRenderer}). */
public final class SlaapzorgClient {
    private SlaapzorgClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(SlaapzorgFeature.MOBIEL_BE.get(), MobielRenderer::new));
        modBus.addListener((ModelEvent.RegisterStandalone event) -> MobielRenderer.registerModels(event));
    }
}
