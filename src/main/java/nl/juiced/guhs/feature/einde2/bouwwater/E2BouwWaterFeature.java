package nl.juiced.guhs.feature.einde2.bouwwater;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2, slice bouw_water: the build of the Noordelijke Waterstam.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_bouw_water.py.
 */
public final class E2BouwWaterFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2BouwWaterFeature() {
    }
}
