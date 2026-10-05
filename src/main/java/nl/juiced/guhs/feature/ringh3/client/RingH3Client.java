package nl.juiced.guhs.feature.ringh3.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;

/** Client side of bbq2 (ring-h3): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class RingH3Client {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingH3Feature.BARBECUEROG.get(), NoopRenderer::new);
        });
    }

    private RingH3Client() {
    }
}
