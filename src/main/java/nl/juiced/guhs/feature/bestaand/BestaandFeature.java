package nl.juiced.guhs.feature.bestaand;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (bestaand): the Wachter-guh of the existing Spiesburcht and the knuffelmaker-guh of the existing Mika-grillpaleis.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/bestaand.py.
 */
public final class BestaandFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BestaandFeature() {
    }
}
