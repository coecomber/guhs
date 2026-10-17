package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.5), slice sp_d1_recept: part 1 steps 7-10 of the Sponsguh story (Plan Z, the sneak
 * through De Vetemmer with wink 6, the recipe back, the drum in the Bellenkom and the song "Zoete Knabbelzege"), part 1's
 * rewards. Wired in feature/einde2/Einde2 with the marker {@code <einde2_sp_d1_recept/>}. The story: {@link E2SpD1Recept}; the
 * scenes: {@link D1rScenes}; the games: {@link Sluipen}, {@link Trommel}; dev: {@link D1rCommando}. No items or blocks of its own,
 * no payloads (titles, particles and sounds go through vanilla packets and {@code Schijn}).
 */
public final class E2SpD1ReceptFeature {
    private E2SpD1ReceptFeature() {
    }

    public static void register(IEventBus modBus) {
        E2SpD1Recept.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
