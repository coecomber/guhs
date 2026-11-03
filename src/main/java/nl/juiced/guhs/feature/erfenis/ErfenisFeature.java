package nl.juiced.guhs.feature.erfenis;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.9 "Een opknapper!" (De Erfenis): an overgrown farm inherited for one cheese knabbel, on a plot in the dimension
 * {@code guhs:erfenis}: clear it, build a guh farm village (animals, fields, workshops, residents), deliver orders, send
 * goods home, all in a top-down overview, plus a world map of all villages. Completely separate from the Guhhuisjes.
 * The economy: {@link GebouwType}, {@link Productie}, {@link Prijzen}, {@link Goed}; the API for the other slices:
 * {@link ErfenisEconomie}, {@link ErfenisMomenten}, {@link ErfenisVadskracht}, {@link Knusheid}.
 * <ul>
 *   <li>data: {@link Base} (+ {@link Gebouw}, {@link GebouwType}), all bases in {@link ErfenisData}; plots: {@link Plot}</li>
 *   <li>world: {@link Stempel} (+ the shapes in vorm.GebouwVormen); server logic: {@link Erfenis}; network: {@link ErfenisPayloads}</li>
 *   <li>client: {@code client/ErfenisClient} (the key), {@code client/Overzicht} (camera), {@code client/OverzichtScherm} (the UI)</li>
 * </ul>
 * Resources: tools/features/erfenis.py. Commands: /guhs erfenis (see {@link ErfenisCommando}).
 */
public final class ErfenisFeature {
    private ErfenisFeature() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ErfenisCommando::register);
        NeoForge.EVENT_BUS.addListener(Erfenis::tick);
        NeoForge.EVENT_BUS.addListener(Erfenis::opBreken);
        NeoForge.EVENT_BUS.addListener(Erfenis::opPlaatsen);
        NeoForge.EVENT_BUS.addListener(Erfenis::opKlikBlok);
        NeoForge.EVENT_BUS.addListener(Erfenis::opSlaBlok);
        NeoForge.EVENT_BUS.addListener(Erfenis::opItem);
        NeoForge.EVENT_BUS.addListener(Erfenis::opSchade);
        NeoForge.EVENT_BUS.addListener(ErfenisMomenten::opLogin);
        Makelaarstour.init();
        Knusheid.titels();
        NeoForge.EVENT_BUS.addListener(Guhpension::opJoin);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> Guhpension.allesWeg(e.getServer()));
    }

    public static void payloads(PayloadRegistrar registrar) {
        ErfenisPayloads.register(registrar);
    }
}
