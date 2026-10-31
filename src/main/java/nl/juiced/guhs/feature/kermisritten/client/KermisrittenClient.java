package nl.juiced.guhs.feature.kermisritten.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.juiced.guhs.feature.kermisritten.KermisrittenFeature;

/** Client side of the kermis rides: the moving parts ({@link KermisritRenderer}); seats are invisible. */
public final class KermisrittenClient {
    private KermisrittenClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(KermisrittenFeature.RIT_BE.get(), KermisritRenderer::new);
            event.registerEntityRenderer(KermisrittenFeature.STOEL.get(), NoopRenderer::new);
        });
        modBus.addListener((ModelEvent.RegisterStandalone event) -> KermisritRenderer.registerModels(event));
    }
}
