package nl.juiced.guhs.feature.vadsrijk.tijd;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 1.10.1 (p1101-tijd): the Vadsrijk's own day/night cycle ({@link VadsrijkTijd}, {@link TijdConfig}, {@link TijdCommando}).
 * See SPIEK_1101_TIJD.md.
 */
public final class TijdRegistry {
    private TijdRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> TijdConfig.lees());
        nl.juiced.guhs.world.TikWacht.pre("TijdRegistry:18", e -> VadsrijkTijd.pas(e.getServer()));
        NeoForge.EVENT_BUS.addListener(TijdCommando::register);
    }
}
