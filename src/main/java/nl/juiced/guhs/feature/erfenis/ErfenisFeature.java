package nl.juiced.guhs.feature.erfenis;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.9 "De Erfenis" (PROOF OF CONCEPT, branch 190-erfenis-poc): your own Clash-style base on a plot in the dimension
 * {@code guhs:erfenis}, with base levels, a top-down overview to place/move/upgrade buildings, and (phase 2) a world map of
 * all bases. Completely separate from the Guhhuisjes.
 * <ul>
 *   <li>data: {@link Base} (+ {@link Gebouw}, {@link GebouwType}), all bases in {@link ErfenisData}; plots: {@link Plot}</li>
 *   <li>world: {@link Stempel} (+ the shapes in {@link Vormen}); server logic: {@link Erfenis}; network: {@link ErfenisPayloads}</li>
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
    }

    public static void payloads(PayloadRegistrar registrar) {
        ErfenisPayloads.register(registrar);
    }
}
