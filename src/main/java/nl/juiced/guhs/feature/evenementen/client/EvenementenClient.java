package nl.juiced.guhs.feature.evenementen.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.evenementen.EvenementenFeature;

/**
 * Client side of the guh events: the parade guhs are drawn like any guh (with their clothes), a falling knabbel like a
 * thrown item and a falling star as a big, glowing bit of sterrenstof. The boss bar and chat come from the server.
 */
public final class EvenementenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(EvenementenFeature.PARADE_GUH.get(), GuhRenderer::new);
            event.registerEntityRenderer(EvenementenFeature.VALLENDE_KNABBEL.get(), context -> new ThrownItemRenderer<>(context, 1.1f, false));
            event.registerEntityRenderer(EvenementenFeature.VALLENDE_STER.get(), context -> new ThrownItemRenderer<>(context, 2.2f, true));
        });
    }

    private EvenementenClient() {
    }
}
