package nl.juiced.guhs.feature.ringh2.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Client side of bbq2 (ring-h2): nothing of its own. The cast's models and named cutscene animations are ring-kern's
 * (feature/ring/client), the card and the cutscenes are played by the verhaal engine (feature/verhaal/client).
 */
public final class RingH2Client {
    public static void init(IEventBus modBus) {
    }

    private RingH2Client() {
    }
}
