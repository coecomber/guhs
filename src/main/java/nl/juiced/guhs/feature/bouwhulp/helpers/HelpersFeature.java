package nl.juiced.guhs.feature.bouwhulp.helpers;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Deel 2 F10b (design 2.8 + 2.9): the helper and build animals. Their jobs are run from the verblijfsbord's Bouwopdracht
 * tab ({@link Bouwopdracht}), with adaptive templates ({@link Sjablonen}) and "Bouw passend verblijf" ({@link PassendVerblijf});
 * here only the hooks: pointing with the vormstaf ({@link Aanwijzen}). Texts: tools/features/bouwhulp_helpers.py.
 */
public final class HelpersFeature {
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.RightClickBlock event) -> Aanwijzen.klik(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> Aanwijzen.weg(event.getEntity().getUUID()));
    }

    private HelpersFeature() {
    }
}
