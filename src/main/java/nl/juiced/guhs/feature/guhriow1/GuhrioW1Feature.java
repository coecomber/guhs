package nl.juiced.guhs.feature.guhriow1;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (guhrio-w1): world 1 of Super Guhrio (the binnentuin): levels 1-1 and 1-2.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/guhrio_w1.py.
 */
public final class GuhrioW1Feature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhrioW1Feature() {
    }
}
