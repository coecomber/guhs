package nl.juiced.guhs.feature.vadsrijk.ruiters.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EconomieClient;
import nl.juiced.guhs.feature.vadsrijk.ruiters.RuiterRegistry;

/** U-RUITERS, client: the Pluimguh with the mounts' own renderer (colours, pattern, tack), the rider with the guh renderer. */
public final class RuitersClient {
    private RuitersClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(RuiterRegistry.PLUIM.get(), c -> new EconomieClient.RijdierRenderer(c, RijdierSoort.PLUIMGUH));
            e.registerEntityRenderer(RuiterRegistry.GUH.get(), GuhRenderer::new);
        });
    }
}
