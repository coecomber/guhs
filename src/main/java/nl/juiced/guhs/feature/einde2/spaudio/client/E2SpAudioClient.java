package nl.juiced.guhs.feature.einde2.spaudio.client;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * einde2 "Sponsguh", slice sp_audio, client side: the background music of Bikinibodem, Schelpenstad and the loopgraaf, the
 * moments, the songs on their scene's tick and the minigame tunes ({@link SpAudioSpeler}). The music listener runs at the
 * lowest priority: after the Guhvatar music's own (low) listener, so a Sponsguh moment also silences that.
 */
public final class E2SpAudioClient {
    private E2SpAudioClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SpAudioSpeler::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SpAudioSpeler::onSelectMusic);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> SpAudioSpeler.wis());
    }
}
