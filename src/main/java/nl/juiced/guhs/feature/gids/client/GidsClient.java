package nl.juiced.guhs.feature.gids.client;

import net.neoforged.bus.api.IEventBus;

/** Client side of gids: the cache of the Guhdex tab "Verhalen" (the other tabs read the caches of their own features). */
public final class GidsClient {
    public static void init(IEventBus modBus) {
        VerhalenCache.init();
    }

    private GidsClient() {
    }
}
