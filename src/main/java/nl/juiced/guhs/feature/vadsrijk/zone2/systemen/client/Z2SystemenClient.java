package nl.juiced.guhs.feature.vadsrijk.zone2.systemen.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.JuwelierPayloads;

/** Zone 2 systemen, the client: the juwelier screen. */
public final class Z2SystemenClient {
    private Z2SystemenClient() {
    }

    public static void init(IEventBus modBus) {
        JuwelierPayloads.ontvanger = s -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof JuwelierScherm j) {
                j.update(s.data(), s.items());
            } else {
                mc.setScreen(new JuwelierScherm(s.data(), s.items()));
            }
        };
    }
}
