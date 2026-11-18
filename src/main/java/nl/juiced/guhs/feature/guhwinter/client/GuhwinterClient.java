package nl.juiced.guhs.feature.guhwinter.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Guhs 1.10.9 "Guhwinter", client side: the counterpart of {@link nl.juiced.guhs.feature.guhwinter.GuhwinterFeature},
 * called once from FeaturesClient. Every slice with client code adds ONE line below.
 */
public final class GuhwinterClient {
    private GuhwinterClient() {
    }

    public static void init(IEventBus modBus) {
        // --- slices (one line each) ---
        nl.juiced.guhs.feature.guhwinter.hoentjes.client.HoentjesClient.init(modBus);   // the hoentjes' looks
        nl.juiced.guhs.feature.guhwinter.tekenen.client.TekenenClient.init(modBus);   // editor, album, drawing renderers
        nl.juiced.guhs.feature.guhwinter.npc.client.GuhwinterNpcClient.init(modBus);   // the Winterplein guhs' looks
        nl.juiced.guhs.feature.guhwinter.spellen.client.SpellenClient.init(modBus);   // race, oliebollen and shop screens
        nl.juiced.guhs.feature.guhwinter.lootjes.client.LootjesClient.init(modBus);   // the placed Surprise, its preview and turning
    }
}
