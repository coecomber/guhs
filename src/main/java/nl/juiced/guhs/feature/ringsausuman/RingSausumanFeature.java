package nl.juiced.guhs.feature.ringsausuman;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-sausuman): the extra stop of the Knabbelring: the Toren van Sausuman.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_sausuman.py.
 */
public final class RingSausumanFeature {
    /**
     * The questline of this part of the story. F0 placeholder of one step (CONTRACT_130 6.2.5): the travel map of ring-kern
     * has its halte before the slice exists; the slice gives it its real steps. Texts: tools/features/ring_sausuman.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_sausuman", "knabbelring").stappen(1).na("ring_h4").registreer();

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingSausumanFeature() {
    }
}
