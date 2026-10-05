package nl.juiced.guhs.feature.ringh5.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;

/** Client side of bbq2 (ring-h5): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class RingH5Client {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingH5Feature.OOG_VAN_SAUSRON.get(), NoopRenderer::new);
        });
    }

    private RingH5Client() {
    }
}
