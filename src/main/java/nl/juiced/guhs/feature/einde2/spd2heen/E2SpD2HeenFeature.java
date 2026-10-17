package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.6), slice sp_d2_heen: part 2 steps 0-4: the manager, the Guhfy Guhber ice cream (wink 8),
 * the crown and the frozen Krabguh, the drive, the key in De Ruige Kroeg; the narrator card of part 2 (wired in
 * feature/einde2/Einde2 with the marker {@code <einde2_sp_d2_heen/>}). The direction is {@link SpD2Heen}; the drive
 * {@link Rit}, the stand-still game {@link Kroeg}, the scripts {@link D2Scenes}, the hints {@link D2Hints}, the dev commands
 * {@link E2SpD2HeenCommando}. Nothing to register on the mod bus, no payloads, no items of its own (the reward is the existing
 * guh clothes {@code SPONSGUH_IJSHOEDJE}).
 */
public final class E2SpD2HeenFeature {
    private E2SpD2HeenFeature() {
    }

    public static void register(IEventBus modBus) {
        SpD2Heen.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
