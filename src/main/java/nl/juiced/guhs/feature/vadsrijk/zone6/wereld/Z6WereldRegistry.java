package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import net.neoforged.bus.api.IEventBus;

/**
 * Het Vadsrijk zone 6 "Het Zijdeland", slice U-Z6WERELD: the map and its living parts. Called from {@code wereld.WereldRegistry}
 * (one marked line).
 */
public final class Z6WereldRegistry {
    private Z6WereldRegistry() {
    }

    public static void register(IEventBus modBus) {
        Z6Blokken.register(modBus);
        Z6Wereld.register();   // areas, biomes, townsfolk + mounts, dev commands
    }
}
