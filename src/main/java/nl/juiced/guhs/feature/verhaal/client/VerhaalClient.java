package nl.juiced.guhs.feature.verhaal.client;

import net.neoforged.bus.api.IEventBus;

/**
 * 3.0 (Guhverhalen), client side of the fundament. The per-variant looks live in {@link VariantUiterlijk} (the owner slices
 * register them); the talking screen is the 2.8 PraatScherm (knuffeldal client), which now also shows scenes.
 */
public final class VerhaalClient {
    public static void init(IEventBus modBus) {
    }

    private VerhaalClient() {
    }
}
