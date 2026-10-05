package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Guhpixel slice "bioscoop": the Guhbioscoop (projector, screen, seats, films). An empty stub from the foundation: the slice fills it in. It owns its
 * own DeferredRegisters in this class and registers them in {@link #register}; GuhpixelFeature calls the three methods.
 */
public final class BioscoopSlice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BioscoopSlice() {
    }
}
