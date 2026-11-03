package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

/**
 * De Erfenis: where the village's vadskracht comes from. The koppeling slice implements it (the Dorpszender in your base on
 * your vadskracht net sends to the Vadsmast in the village, with loss) and registers it with {@link #zet}. Default: 0, so
 * the VK workshops run at reduced speed and the Mammoetwei and Kaarsenmakerij sleep until then. The kern caps what arrives
 * at the Vadsmast's own cap ({@link Productie#vadsmastCap}); without a Vadsmast nothing arrives.
 */
@FunctionalInterface
public interface ErfenisVadskracht {
    /** The VK arriving at the Vadsmast of this owner's village right now (after the transmission loss). */
    int beschikbaar(MinecraftServer s, UUID eigenaar);

    /** The source in use (the koppeling slice replaces it; the dev command /guhs erfenis vk sets a fixed number on top). */
    final class Bron {
        static ErfenisVadskracht bron = (s, e) -> 0;

        private Bron() {
        }
    }

    static void zet(ErfenisVadskracht bron) {
        Bron.bron = bron == null ? (s, e) -> 0 : bron;
    }

    static ErfenisVadskracht huidig() {
        return Bron.bron;
    }
}
