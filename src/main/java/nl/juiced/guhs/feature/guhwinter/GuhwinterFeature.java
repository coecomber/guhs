package nl.juiced.guhs.feature.guhwinter;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.guhwinter.kern.KernEvents;
import nl.juiced.guhs.feature.guhwinter.kern.KernRegistry;

/**
 * Guhs 1.10.9 "Guhwinter": the winter event on the Winterplein in the Guhmension (design: guhswinter/GUHSWINTER.md,
 * API: guhs_winter/CONTRACT.md). Kern first (the clock, moments, zone, pepernoot, commands); every slice adds ONE line
 * per method below.
 */
public final class GuhwinterFeature {
    private GuhwinterFeature() {
    }

    public static void register(IEventBus modBus) {
        KernRegistry.register(modBus);
        GuhwinterZone.register();
        KernEvents.register();
        GuhwinterCommando.register();
        // --- slices (one line each) ---
    }

    public static void payloads(PayloadRegistrar registrar) {
        // --- slices (one line each) ---
    }

    public static void creative(Consumer<ItemStack> output) {
        KernRegistry.creative(output);
        // --- slices (one line each) ---
    }
}
