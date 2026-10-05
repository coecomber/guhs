package nl.juiced.guhs.feature.ring.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.ring.RingFeature;

/** Client side of bbq2 (ring-kern): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class RingClient {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingFeature.SMIKAGOL.get(), NoopRenderer::new);
            event.registerEntityRenderer(RingFeature.KNEKEL_RUITER.get(), NoopRenderer::new);
        });
    }

    private RingClient() {
    }
}
