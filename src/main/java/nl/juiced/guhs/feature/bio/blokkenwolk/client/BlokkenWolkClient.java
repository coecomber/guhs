package nl.juiced.guhs.feature.bio.blokkenwolk.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client side of the biomes3 slice "blokken-wolk": the particles ({@link Deeltjes}) and the foam, mist and sound of big
 * waterfalls ({@link WatervalEffecten}). BioClient calls {@link #init}.
 */
public final class BlokkenWolkClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(Deeltjes::registreer);
        NeoForge.EVENT_BUS.addListener(WatervalEffecten::onTick);
    }

    private BlokkenWolkClient() {
    }
}
