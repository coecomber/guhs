package nl.juiced.guhs.feature.sausdieren.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;

/** Client side of bbq2 (sausdieren): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class SausdierenClient {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SausdierenFeature.SAUSBLUBJE.get(), NoopRenderer::new);
            event.registerEntityRenderer(SausdierenFeature.SAUSLOPER.get(), NoopRenderer::new);
        });
    }

    private SausdierenClient() {
    }
}
