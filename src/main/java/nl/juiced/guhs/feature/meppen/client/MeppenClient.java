package nl.juiced.guhs.feature.meppen.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.meppen.MepPayloads;

/** Client side of the meppen feature: the Mepguh's screen (the heads and the hall are plain block models). */
public final class MeppenClient {
    public static void init(IEventBus modBus) {
    }

    /** The server asks to open the Mepguh's screen. */
    public static void open(MepPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new MepScreen(payload));
    }

    private MeppenClient() {
    }
}
