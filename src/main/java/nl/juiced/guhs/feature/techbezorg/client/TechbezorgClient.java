package nl.juiced.guhs.feature.techbezorg.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.techbezorg.TechbezorgFeature;

/** Client side of bbq2 (tech-bezorg): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class TechbezorgClient {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(TechbezorgFeature.BEZORGGUHTJE.get(), NoopRenderer::new);
        });
    }

    private TechbezorgClient() {
    }
}
