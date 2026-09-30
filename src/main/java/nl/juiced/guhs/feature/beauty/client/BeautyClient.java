package nl.juiced.guhs.feature.beauty.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.beauty.BeautyPayloads;

/**
 * Client side of the beauty feature: the Showguh's screen and the loaner wardrobe screen (opened by the server with
 * {@link BeautyPayloads.Open}). The model, the jury and the score cards are ordinary guhs and text displays, so no renderers.
 */
public final class BeautyClient {
    public static void init(IEventBus modBus) {
    }

    /** Opens (or closes) a screen for the server. */
    public static void open(BeautyPayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        switch (payload.data().getString("Mode")) {
            case "lobby" -> mc.setScreen(new ShowguhScreen(payload.npcId(), payload.data()));
            case "dress" -> mc.setScreen(new DressScreen(payload.npcId(), payload.data()));
            case "close" -> {
                if (mc.screen instanceof DressScreen) {
                    mc.setScreen(null);
                }
            }
            default -> {
            }
        }
    }

    private BeautyClient() {
    }
}
