package nl.juiced.guhs.feature.einde2.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.einde2.Einde2;

/**
 * einde2, client side (see {@link Einde2}): calls the client side of the seventeen slices and gives every stand-in
 * entity type that is still there ({@link Einde2#plaatshouders}) an empty renderer. Only called on the client.
 */
public final class Einde2Client {
    public static void init(IEventBus modBus) {
        modBus.addListener(Einde2Client::renderers);
        nl.juiced.guhs.feature.einde2.biomes.client.E2BiomesClient.init(modBus);
        nl.juiced.guhs.feature.einde2.biomeguhs.client.E2BiomeguhsClient.init(modBus);
        nl.juiced.guhs.feature.einde2.mikas.client.E2MikasClient.init(modBus);
        nl.juiced.guhs.feature.einde2.zwevers.client.E2ZweversClient.init(modBus);
        nl.juiced.guhs.feature.einde2.appa.client.E2AppaClient.init(modBus);
        nl.juiced.guhs.feature.einde2.vetbootje.client.E2VetbootjeClient.init(modBus);
        nl.juiced.guhs.feature.einde2.vesting.client.E2VestingClient.init(modBus);
        nl.juiced.guhs.feature.einde2.deco.client.E2DecoClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwlucht.client.E2BouwLuchtClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwwater.client.E2BouwWaterClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwaarde.client.E2BouwAardeClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwvuur.client.E2BouwVuurClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwzij1.client.E2BouwZij1Client.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwzij2.client.E2BouwZij2Client.init(modBus);
        nl.juiced.guhs.feature.einde2.sponsguh.client.E2SponsguhClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwbikinibodem.client.E2BouwBikinibodemClient.init(modBus);
        nl.juiced.guhs.feature.einde2.bouwschelpenstad.client.E2BouwSchelpenstadClient.init(modBus);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var type : Einde2.plaatshouders()) {
            event.registerEntityRenderer(type.get(), NoopRenderer::new);
        }
    }

    private Einde2Client() {
    }
}
