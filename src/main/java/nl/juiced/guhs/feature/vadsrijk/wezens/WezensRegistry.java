package nl.juiced.guhs.feature.vadsrijk.wezens;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "wezens" (CONTRACT_VADSRIJK 1): the ~12 zone-1 mobs + elites on kern.mob.VadsrijkMob, their renderers, loot tables and spawners (kern.mob.Spawners).
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the wezens slice fills it.
 */
public final class WezensRegistry {
    private WezensRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
