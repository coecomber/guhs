package nl.juiced.guhs.feature.vadsrijk.bazen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "bazen" (CONTRACT_VADSRIJK 1): Koning Klodder, de Opperzwam, de Mika-spion on kern.baas.VadsrijkBaas + their arenas (kern.baas.Arenas).
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the bazen slice fills it.
 */
public final class BazenRegistry {
    private BazenRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
