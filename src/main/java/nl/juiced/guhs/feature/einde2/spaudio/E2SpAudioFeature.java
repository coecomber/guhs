package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.9), slice sp_audio: the music of the two places, the three songs (own melodies and lyrics), tunes and stingers, licences. STUB of the skelet (wired in feature/einde2/Einde2 with
 * the marker {@code <einde2_sp_audio/>}): this package is yours; keep the three method names.
 */
public final class E2SpAudioFeature {
    private E2SpAudioFeature() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
