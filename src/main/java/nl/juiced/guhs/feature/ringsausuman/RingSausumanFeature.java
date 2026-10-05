package nl.juiced.guhs.feature.ringsausuman;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (ring-sausuman): the extra stop of the Knabbelring: the Toren van Sausuman.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_sausuman.py.
 */
public final class RingSausumanFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingSausumanFeature() {
    }
}
