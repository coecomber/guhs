package nl.juiced.guhs.feature.erfenis.kaart;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.9 "De Erfenis": DE WERELDKAART of all bases, Tribal Wars / Grepolis style, with the NPC neighbour villages, their
 * ORDERS and "Handje helpen".
 * <ul>
 *   <li>data: {@link KaartData} (NPC villages {@link NpcDorp} with a building plan {@link KaartGebouw}, the news
 *   {@link KaartNieuws}, the growth clock); life: {@link KaartLeven} (the 1.9 rule: 3 villages around the first player, 1
 *   per next player, all following their player's Hoeve level), {@link NpcBouwer} (plans within the base rules),
 *   {@link NpcNamen} (guh names)</li>
 *   <li>orders: {@link Bestellingen} (carts, star order; data {@link BestelData}, one cart {@link Bestelling}, goods
 *   {@link BestelGoed}); helping: {@link Handje}</li>
 *   <li>server: {@link KaartServer}; network: {@link KaartPayloads}; commands: {@link KaartCommando} (/guhs erfenis kaart)</li>
 *   <li>client: client/KaartClient (key F10 "Wereldkaart"), client/KaartScherm (the map), client/KaartLand (the pastel land)</li>
 *   <li>THE SEAM to the core: {@link KaartBron} in {@link #BRON} = {@link KaartBronErfenis} (players = the core's non-NPC
 *   bases, visit = Erfenis.gaNaar + the read-only overview, every NPC village = a real NPC base in its own cell)</li>
 * </ul>
 * Resources: tools/features/erfenis_kaart.py.
 */
public final class KaartFeature {
    /** The source of the real bases. */
    public static KaartBron BRON = new KaartBronErfenis();

    private KaartFeature() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(KaartCommando::register);
        nl.juiced.guhs.world.TikWacht.post("KaartServer.tick", KaartServer::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
                Handje.bijInloggen(p);   // ("X hielp je een handje!" while you were away)
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            KaartServer.herstart();
            KaartBronErfenis.zorgNpcBases(e.getServer());   // (a world from before the merge: its NPC villages get their bases)
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        KaartPayloads.register(registrar);
    }
}
