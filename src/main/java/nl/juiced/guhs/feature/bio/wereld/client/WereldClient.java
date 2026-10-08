package nl.juiced.guhs.feature.bio.wereld.client;

import net.neoforged.bus.api.IEventBus;

/** Client side of the biomes3 slice "wereld" (a stub until its slice fills it in). BioClient calls {@link #init}. */
public final class WereldClient {
    public static void init(IEventBus modBus) {
        WolkClient.init(modBus); // biomes3 wereld-wolk
    }

    private WereldClient() {
    }
}
