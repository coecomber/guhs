package nl.juiced.guhs.feature.einde2.bouwzij1.client;

import net.neoforged.bus.api.IEventBus;

/**
 * einde2, slice bouw_zij1, client side. Nothing to set up: the three places are plain blocks, and the postgoot pushes
 * its rider from the block's own stepOn (which the client runs for its own player).
 */
public final class E2BouwZij1Client {
    public static void init(IEventBus modBus) {
    }

    private E2BouwZij1Client() {
    }
}
