package nl.juiced.guhs.feature.wereld;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (F3 wereld): the world helpers. This is the F0 stub (CONTRACT_130 5.1): F3 fills it in place. Resources come from
 * tools/features/wereld.py.
 */
public final class WereldFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private WereldFeature() {
    }
}
