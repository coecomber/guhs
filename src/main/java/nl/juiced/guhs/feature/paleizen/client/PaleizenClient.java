package nl.juiced.guhs.feature.paleizen.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.paleizen.PaleizenFeature;

/** Client side of bbq2 (paleizen): the F0 stub (CONTRACT_130 5.1), the slice fills it in place. */
public final class PaleizenClient {
    public static void init(IEventBus modBus) {
        // (the placeholders of the fixed entity ids: invisible until the slice gives them a model)
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(PaleizenFeature.WORSTZWIJNTJE.get(), NoopRenderer::new);
        });
    }

    private PaleizenClient() {
    }
}
