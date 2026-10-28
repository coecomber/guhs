package nl.juiced.guhs.feature.guhpixel.gezel.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.guhpixel.gezel.GezelPayloads;

/**
 * Client side of the guhpixel slice "gezel": the board game screen opens (or refreshes) when the server sends its state.
 * The Kussengevecht is server-driven (overlay messages, chat) and the three lobby NPCs use the normal sitting guh model with
 * their own colour (tools/features/guhpixel_gezel.py).
 */
public final class GezelClient {
    public static void init(IEventBus modBus) {
        GezelPayloads.opener = p -> {
            Minecraft mc = Minecraft.getInstance();
            Runnable r = () -> {
                if (mc.screen instanceof GezelScherm s && s.spel().equals(p.data().getStringOr("Spel", ""))) {
                    s.update(p.data());
                } else {
                    mc.setScreen(new GezelScherm(p.data()));
                }
            };
            if (mc.isSameThread()) {
                r.run();
            } else {
                mc.execute(r);
            }
        };
    }

    private GezelClient() {
    }
}
