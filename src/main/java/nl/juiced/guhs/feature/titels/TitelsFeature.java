package nl.juiced.guhs.feature.titels;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The titles (1.2.6): a title for every big adventure, one of them (the player's choice, Guhdex tab "Titels") behind the
 * player's name in the player list, above their head and in chat. See {@link Titels}. No registry content.
 */
public final class TitelsFeature {
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(TitelsEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        TitelsPayloads.register(registrar);
    }

    private TitelsFeature() {
    }
}
