package nl.juiced.guhs.feature.vadsrijk.economie;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "economie" (CONTRACT_VADSRIJK 1): the other loot base types and tables, Vadsmunten shops, the market, trading, stars/reroll/repair (kern.loot hooks), mounts.
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the economie slice fills it.
 */
public final class EconomieRegistry {
    private EconomieRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
