package nl.juiced.guhs.feature.disco.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.disco.DiscoPayloads;

/** Client side of the disco feature: the DJ-guh's screen (only loaded on the client). */
public final class DiscoClient {
    public static void init(IEventBus modBus) {
        // nothing to register: the blocks bring their render types in their models, the screen opens from a payload
    }

    public static void open(DiscoPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new DiscoScreen(payload));
    }

    private DiscoClient() {
    }
}
