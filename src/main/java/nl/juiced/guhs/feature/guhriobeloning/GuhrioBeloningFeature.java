package nl.juiced.guhs.feature.guhriobeloning;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * bbq2 (guhrio-beloning): the forecourt of Super Guhrio: Pad-guh and his shop, the highscore board, Guhshi, outfits and building blocks.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/guhrio_beloning.py.
 */
public final class GuhrioBeloningFeature {
    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private GuhrioBeloningFeature() {
    }
}
