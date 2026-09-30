package nl.juiced.guhs.feature.vissen.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vissen.VissenFeature;
import nl.juiced.guhs.feature.vissen.VissenPayloads;

/** Client side of the vissen feature: the Visguh's screen and the "cast" look of the Guhvis-hengel. */
public final class VissenClient {
    public static void init(IEventBus modBus) {
        // 1.1.0: the "cast" look of the Guhvis-hengel is data now (client item definition with vanilla's
        // minecraft:fishing_rod/cast condition, same rule: the line is out and the rod is in the fishing hand)
    }

    public static void open(VissenPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new VissenScreen(payload));
    }

    private VissenClient() {
    }
}
