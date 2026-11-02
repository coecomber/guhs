package nl.juiced.guhs.feature.erfenis.kaart;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.9 "De Erfenis" (POC), phase 2: DE WERELDKAART of all bases, Tribal Wars / Grepolis style, with living NPC villages.
 * <ul>
 *   <li>data: {@link KaartData} (NPC villages {@link NpcDorp} with a building plan {@link KaartGebouw}, the news
 *   {@link KaartNieuws}, the growth clock); life: {@link KaartLeven} (seeding in islets, growth), {@link NpcBouwer} (plans
 *   within the base rules), {@link NpcNamen} (guh names)</li>
 *   <li>server: {@link KaartServer}; network: {@link KaartPayloads}; commands: {@link KaartCommando} (/guhs erfenis kaart)</li>
 *   <li>client: client/KaartClient (key M "Wereldkaart"), client/KaartScherm (the map), client/KaartLand (the pastel land)</li>
 *   <li>THE SEAM to the core: {@link KaartBron} in {@link #BRON} (stand-alone: {@link KaartBronStandaard})</li>
 * </ul>
 * MERGE with feature/erfenis (branch 190-erfenis-poc): set {@link #BRON} to a bron on ErfenisData/Erfenis (players = its
 * non-NPC bases, visit = Erfenis.gaNaar + the read-only overview, npcVeranderd = make/update the NPC base in the village's
 * cell from its plan). Resources: tools/features/erfenis_kaart.py.
 */
public final class KaartFeature {
    /** The source of the real bases (swap at merge). */
    public static KaartBron BRON = new KaartBronStandaard();

    private KaartFeature() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(KaartCommando::register);
        NeoForge.EVENT_BUS.addListener(KaartServer::tick);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> KaartServer.herstart());
    }

    public static void payloads(PayloadRegistrar registrar) {
        KaartPayloads.register(registrar);
    }
}
