package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2, slice vetbootje: the player's own vetbootje and the personal heteluchtballon.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_vetbootje.py.
 */
public final class E2VetbootjeFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2VetbootjeFeature() {
    }
}
