package nl.juiced.guhs.feature.einde2.audio.client;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * einde2 phase B/C, slice audio, client side: the background music per area and the epic moments ({@link AudioSpeler}).
 * The music listener runs late (low priority), as NeoForge advises for music of a place inside a dimension or biome.
 */
public final class E2AudioClient {
    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(AudioSpeler::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, AudioSpeler::onSelectMusic);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> AudioSpeler.wis());
    }

    private E2AudioClient() {
    }
}
