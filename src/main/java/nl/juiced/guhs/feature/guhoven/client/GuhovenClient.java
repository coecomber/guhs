package nl.juiced.guhs.feature.guhoven.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;

/** Client side of the Guhoven (1.2.5): its screen. */
public final class GuhovenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(GuhovenFeature.GUH_OVEN_MENU.get(), GuhOvenScreen::new));
    }

    private GuhovenClient() {
    }
}
