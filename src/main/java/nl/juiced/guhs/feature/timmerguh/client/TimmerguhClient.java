package nl.juiced.guhs.feature.timmerguh.client;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * 3.0 (Guhverhalen), slice timmerguh, client side: the Timmerguh's own model (a cream timmermanshelmpje with a pink ridge and
 * little ear bumps, a pencil behind his ear, a denim tuinbroek, a gereedschapsriem with hamertje and duimstok;
 * geckolib/models/entity/guh_npc_timmerguh.geo.json from tools/features/timmerguh.py). The read-only huisje screen for other players lives
 * in feature.huisje.client.HuisjeScreen; the ghost tiles' render type comes from their block model (translucent).
 */
public final class TimmerguhClient {
    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.TIMMERGUH, Guhs.id("entity/guh_npc_timmerguh"));
    }

    private TimmerguhClient() {
    }
}
