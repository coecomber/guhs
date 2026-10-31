package nl.juiced.guhs.feature.kermisuitbouw;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 1.8.1: see {@link KermisUitbouw}. */
public final class KermisUitbouwFeature {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> KermisUitbouw.tick());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> KermisUitbouw.stop());
    }

    private KermisUitbouwFeature() {
    }
}
