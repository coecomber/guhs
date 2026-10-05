package nl.juiced.guhs.feature.ringh6;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-h6): chapter 6 of the Knabbelring: De Frituurberg.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_h6.py.
 */
public final class RingH6Feature {
    /**
     * The questline of this part of the story. F0 placeholder of one step (CONTRACT_130 6.2.5): the travel map of ring-kern
     * has its halte before the slice exists; the slice gives it its real steps. Texts: tools/features/ring_h6.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h6", "knabbelring").stappen(1).na("ring_h5").registreer();

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingH6Feature() {
    }
}
