package nl.juiced.guhs.feature.einde2.biomes.client;

import net.neoforged.bus.api.IEventBus;

/**
 * einde2, slice biomes, client side. Nothing to set up: the blocks name their render type in their models, the sparks and
 * notes are vanilla particles and the singing is played from the flower's own animateTick.
 */
public final class E2BiomesClient {
    public static void init(IEventBus modBus) {
    }

    private E2BiomesClient() {
    }
}
