package nl.juiced.guhs.feature.ringh2;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-h2): chapter 2 of the Knabbelring: De Raad van Guhrond (Guhvendel).
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_h2.py.
 */
public final class RingH2Feature {
    /**
     * The questline of this part of the story. F0 placeholder of one step (CONTRACT_130 6.2.5): the travel map of ring-kern
     * has its halte before the slice exists; the slice gives it its real steps. Texts: tools/features/ring_h2.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h2", "knabbelring").stappen(1).na("ring_h1").registreer();

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingH2Feature() {
    }
}
