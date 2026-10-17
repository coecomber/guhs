package nl.juiced.guhs.feature.einde2.spcast;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.2), slice sp_cast: the cast of the Sponsguh story: the story looks (SpCast.zetLook), story animations, the Mikas of De Ruige Kroeg, the residents' visibility, new guh clothes. STUB of the skelet (wired in feature/einde2/Einde2 with
 * the marker {@code <einde2_sp_cast/>}): this package is yours; keep the three method names.
 */
public final class E2SpCastFeature {
    private E2SpCastFeature() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
