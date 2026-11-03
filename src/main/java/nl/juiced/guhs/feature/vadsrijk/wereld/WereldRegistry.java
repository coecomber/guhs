package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "wereld" (CONTRACT_VADSRIJK 1): the hand-built map tiles (kern.wereld.KaartFeature reads them), moving set pieces (temple doors), landmarks, skybox.
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the wereld slice fills it.
 */
public final class WereldRegistry {
    private WereldRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
