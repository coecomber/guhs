package nl.juiced.guhs.feature.vadsrijk.klassen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "klassen" (CONTRACT_VADSRIJK 1): the Pluisjager and the Vadsmagier (kern.klasse.Klassen.registreer), the skill trees (BoomKnoop), respec, job advancement at 10/20, the class change at the ordetempel.
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the klassen slice fills it.
 */
public final class KlassenRegistry {
    private KlassenRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
