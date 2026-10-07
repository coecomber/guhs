package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * biomes3 slice "bouw-wolk1": wolkenhoeder-hut, sterrenwacht-ruine, luchtballon-haven. A stub until its slice fills it in: the slice owns its
 * DeferredRegisters in this class and registers them in {@link #register}. BioFeature calls the three methods.
 * Resources: tools/features/bio_bouw_wolk1.py.
 */
public final class BouwWolk1Slice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BouwWolk1Slice() {
    }
}
