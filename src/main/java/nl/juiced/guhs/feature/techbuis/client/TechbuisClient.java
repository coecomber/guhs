package nl.juiced.guhs.feature.techbuis.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.techbuis.BuisPayloads;
import nl.juiced.guhs.feature.techbuis.TechbuisFeature;

/**
 * Client side of bbq2 (tech-buizen): the items that roll through the Knabbelbuizen ({@link BuisRitten}, drawn by
 * {@link BuisStukRenderer} on the Richtingstuk / Filterstuk that sent them), and the screens of the Filterstuk and the
 * Voorraadmeter ({@link FilterScreen}) and of the Opzuiger ({@link OpzuigerScreen}).
 */
public final class TechbuisClient {
    public static void init(IEventBus modBus) {
        BuisPayloads.ontvanger = BuisRitten::ontvang;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> BuisRitten.tick());
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(TechbuisFeature.RICHTING_BE.get(), BuisStukRenderer::new);
            event.registerBlockEntityRenderer(TechbuisFeature.FILTER_BE.get(), BuisStukRenderer::new);
        });
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(TechbuisFeature.FILTER_MENU.get(), FilterScreen::new);
            event.register(TechbuisFeature.VOORRAADMETER_MENU.get(), FilterScreen::new);
            event.register(TechbuisFeature.OPZUIGER_MENU.get(), OpzuigerScreen::new);
        });
    }

    private TechbuisClient() {
    }
}
