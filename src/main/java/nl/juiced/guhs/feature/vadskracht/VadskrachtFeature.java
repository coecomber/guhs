package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (F1 vadskracht): vadskracht: nets of Guhdraad, sources, consumers, the hover readout, the machine base classes.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/vadskracht.py.
 */
public final class VadskrachtFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private VadskrachtFeature() {
    }
}
