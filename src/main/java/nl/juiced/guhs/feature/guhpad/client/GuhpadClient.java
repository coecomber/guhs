package nl.juiced.guhs.feature.guhpad.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.client.screen.GuhDexScreen;
import nl.juiced.guhs.feature.gids.client.GidsVerhalenTab;
import nl.juiced.guhs.feature.guhpad.GuhpadPayloads;

/**
 * Client side of het Guhpad: the layout of the Guhdex tab Verhalen ({@link GuhpadTab}: the path map, the stories per world,
 * the preview of "Het echte Guheinde"), redrawn when the server tells where the player is on the path. The Superkompas
 * option "Mijn verhaal" is drawn by client.screen.SuperkompasScreen itself.
 */
public final class GuhpadClient {
    public static void init(IEventBus modBus) {
        GidsVerhalenTab.indeling = new GuhpadTab();
        GuhpadPayloads.Client.vernieuwd = () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.screen instanceof GuhDexScreen dex) {
                    dex.verhalenVernieuwd();
                }
            });
        };
    }

    private GuhpadClient() {
    }
}
