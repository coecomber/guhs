package nl.juiced.guhs.feature.hamsterbuis.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.hamsterbuis.HamsterPayloads;

/** The hamsterbuis station's screen on the client (opened by {@link HamsterPayloads.Open}, refreshed when it is open). */
public final class HamsterbuisClient {
    private HamsterbuisClient() {
    }

    public static void init(IEventBus modBus) {
        HamsterPayloads.KLANT = open -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof HamsterbuisScreen s && s.station().equals(open.station())) {
                s.update(open);
            } else {
                mc.setScreen(new HamsterbuisScreen(open));
            }
        };
    }
}
