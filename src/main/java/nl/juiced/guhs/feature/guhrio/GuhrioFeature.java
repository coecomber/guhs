package nl.juiced.guhs.feature.guhrio;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (guhrio-engine): Super Guhrio: the side-view lane engine, obstacles, power-ups, coins, flags and records, Guhshi, the castle.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/guhrio.py.
 */
public final class GuhrioFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhrioFeature() {
    }
}
