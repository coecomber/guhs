package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.7), slice sp_d2_weg: part 2 steps 5-8 of the Sponsguh story: the moustaches (wink 9),
 * the loopgraaf and its singing monsters, Dennis and the moustache, the Cycloop and the tear. Wired in feature/einde2/Einde2
 * with the marker {@code <einde2_sp_d2_weg/>}. The direction is {@link E2SpD2Weg}; the scenes {@link D2WegScenes}; the
 * minigames {@link Jacht} and {@link Traan}; the walking companions {@link Volgers}; dev commands {@link E2SpD2WegCommando}
 * ({@code /guhs einde2 sponsguh d2weg ...}). Resources: tools/features/einde2_sp_d2_weg.py (chunk c116_e2_sp_d2_weg).
 */
public final class E2SpD2WegFeature {
    private E2SpD2WegFeature() {
    }

    public static void register(IEventBus modBus) {
        E2SpD2Weg.register();
        KledingBronnen.bron(GuhClothes.EINDE2_SP_D2_WEG_SNOR, E2SpD2Weg.BRON);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
