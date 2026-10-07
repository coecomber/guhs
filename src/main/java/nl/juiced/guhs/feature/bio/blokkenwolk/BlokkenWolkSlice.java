package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * biomes3 slice "blokken-wolk": cloud and rainbow blocks, cloud furniture, petals, waterfall effects. A stub until its slice fills it in: the slice owns its
 * DeferredRegisters in this class and registers them in {@link #register}. BioFeature calls the three methods.
 * Resources: tools/features/bio_blokken_wolk.py.
 */
public final class BlokkenWolkSlice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BlokkenWolkSlice() {
    }
}
