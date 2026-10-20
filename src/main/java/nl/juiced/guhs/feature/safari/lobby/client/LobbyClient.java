package nl.juiced.guhs.feature.safari.lobby.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.safari.lobby.LobbyPayloads;

/** The client side of the safari lobby: opens (or refreshes) the giftshop screen. */
public final class LobbyClient {
    public static void init(IEventBus modBus) {
        LobbyPayloads.winkelOpener = p -> {
            Minecraft mc = Minecraft.getInstance();
            Runnable r = () -> {
                if (mc.screen instanceof SafariWinkelScherm s) {
                    s.update(p.data());
                } else {
                    mc.setScreen(new SafariWinkelScherm(p.data()));
                }
            };
            if (mc.isSameThread()) {
                r.run();
            } else {
                mc.execute(r);
            }
        };
    }

    private LobbyClient() {
    }
}
