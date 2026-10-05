package nl.juiced.guhs.feature.guhriow2;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (guhrio-w2): world 2 of Super Guhrio (the kelders): levels 2-1 and 2-2.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/guhrio_w2.py.
 */
public final class GuhrioW2Feature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhrioW2Feature() {
    }
}
