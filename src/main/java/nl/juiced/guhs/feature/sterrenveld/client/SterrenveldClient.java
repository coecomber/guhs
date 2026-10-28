package nl.juiced.guhs.feature.sterrenveld.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.sterrenveld.SterrenveldFeature;

/** The falling stars: their item look (bright, glowing, fading per phase), big and always full bright. */
public final class SterrenveldClient {
    private SterrenveldClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(SterrenveldFeature.VALLENDE_STER.get(), c -> new ThrownItemRenderer<>(c, 1.6f, true)));
    }
}
