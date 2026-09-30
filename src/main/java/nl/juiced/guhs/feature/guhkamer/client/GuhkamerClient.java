package nl.juiced.guhs.feature.guhkamer.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.guhkamer.GuhkamerPayloads;

/** Client side of the Guhkamer (2.10): the Guhbel screen. */
public final class GuhkamerClient {
    public static void init(IEventBus modBus) {
        GuhkamerPayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GuhbelScreen s) {
                s.update(p.data());
            } else {
                mc.setScreen(new GuhbelScreen(p.data()));
            }
        });
    }

    private GuhkamerClient() {
    }
}
