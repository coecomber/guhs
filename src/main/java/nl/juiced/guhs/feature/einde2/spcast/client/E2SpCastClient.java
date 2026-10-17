package nl.juiced.guhs.feature.einde2.spcast.client;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.feature.einde2.spcast.KroegMikas;

/**
 * einde2 "Sponsguh", slice sp_cast, client side: the Kroeg Mikas get their own model and animation file in the stock NPC
 * renderer (one file per Kind: {@code guh_npc_<kind>}). The cast's looks are drawn by the cast's own renderer
 * (sponsguh/client/E2SponsguhClient.WezenRenderer).
 */
public final class E2SpCastClient {
    private E2SpCastClient() {
    }

    public static void init(IEventBus modBus) {
        for (KroegMikas r : KroegMikas.values()) {
            SittingGuhRenderers.NPC_MODELEN.put(r.kind, Guhs.id("entity/guh_npc_" + r.kind.id()));
            SittingGuhRenderers.NPC_ANIMATIES.put(r.kind, Guhs.id("entity/guh_npc_" + r.kind.id()));
        }
    }
}
