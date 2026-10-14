package nl.juiced.guhs.feature.park.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.park.ParkFeature;

/**
 * 1.4.2 (Het Knabbelpark), client side: the statues' renderer. The parkwachter-guh and the kooi-verzorger-guh are plain
 * sitting guhs in their own colours (textures/entity/npc_&lt;kind&gt;.png); the Hertguhtje is a variant of the guh itself
 * (its antlers are variant bones of guh.geo.json, drawn by the guh's own renderer).
 */
public final class ParkClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ParkFeature.BEELD_BE.get(), BeeldRenderer::new));
        nl.juiced.guhs.feature.park.plek.client.PlekAClient.init(modBus);   // <park-a>
    }

    private ParkClient() {
    }
}
