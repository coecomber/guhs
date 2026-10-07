package nl.juiced.guhs.feature.einde2.bouwlucht.client;

import net.neoforged.bus.api.IEventBus;

/**
 * einde2, slice bouw_lucht, client side. Nothing to set up: the blocks of the Zuidelijke Luchttempel are plain models
 * (tools/features/einde2_bouw_lucht.py) and the build has no renderer, screen or particle of its own.
 * hook fase B / fase C: what the story and the workshop show on the client (the veil, the stages of the Vredesballon for
 * one player) starts here; it is already wired in feature/einde2/client/Einde2Client.
 */
public final class E2BouwLuchtClient {
    public static void init(IEventBus modBus) {
    }

    private E2BouwLuchtClient() {
    }
}
