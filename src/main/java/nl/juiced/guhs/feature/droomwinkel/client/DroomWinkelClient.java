package nl.juiced.guhs.feature.droomwinkel.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.droomwinkel.DroomWinkelPayloads;

/** The client side of the droomwinkeltje: opens (or refreshes) its screen. */
public final class DroomWinkelClient {
    public static void init(IEventBus modBus) {
        DroomWinkelPayloads.opener = p -> {
            Minecraft mc = Minecraft.getInstance();
            Runnable r = () -> {
                if (mc.screen instanceof DroomWinkelScherm s) {
                    s.update(p.data());
                } else {
                    mc.setScreen(new DroomWinkelScherm(p.data()));
                }
            };
            if (mc.isSameThread()) {
                r.run();
            } else {
                mc.execute(r);
            }
        };
    }

    private DroomWinkelClient() {
    }
}
