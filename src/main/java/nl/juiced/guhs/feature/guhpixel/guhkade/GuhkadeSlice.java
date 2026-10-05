package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Guhpixel slice "guhkade": the Guhkade cabinets (Flappy Guh, Mika-Pong). An empty stub from the foundation: the slice fills it in. It owns its
 * own DeferredRegisters in this class and registers them in {@link #register}; GuhpixelFeature calls the three methods.
 */
public final class GuhkadeSlice {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhkadeSlice() {
    }
}
