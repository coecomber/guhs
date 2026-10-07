package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * biomes3 slice "bouw-dal": the Klaterdal mini structures and het weebhuisje. A stub until its slice fills it in: the slice owns its
 * DeferredRegisters in this class and registers them in {@link #register}. BioFeature calls the three methods.
 * Resources: tools/features/bio_bouw_dal.py.
 */
public final class BouwDalSlice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BouwDalSlice() {
    }
}
