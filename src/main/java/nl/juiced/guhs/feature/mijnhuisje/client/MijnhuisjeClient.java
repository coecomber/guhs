package nl.juiced.guhs.feature.mijnhuisje.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import nl.juiced.guhs.feature.mijnhuisje.MijnhuisjeFeature;

/** 1.7.0: the Mijn-guhhuisje's screen. */
public final class MijnhuisjeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(MijnhuisjeFeature.MENU.get(), MijnhuisjeScherm::new));
    }

    private MijnhuisjeClient() {
    }
}
