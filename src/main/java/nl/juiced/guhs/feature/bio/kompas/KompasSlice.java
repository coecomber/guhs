package nl.juiced.guhs.feature.bio.kompas;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * biomes3 slice "kompas": the Superkompas tab Biomes. A stub until its slice fills it in: the slice owns its
 * DeferredRegisters in this class and registers them in {@link #register}. BioFeature calls the three methods.
 * Resources: tools/features/bio_kompas.py.
 */
public final class KompasSlice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private KompasSlice() {
    }
}
