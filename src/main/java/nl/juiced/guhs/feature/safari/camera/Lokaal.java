package nl.juiced.guhs.feature.safari.camera;

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * (1.6.0 F3b) Who is playing on this client, for tooltips in common code (item classes are loaded on the server too):
 * the client side sets {@link #bron}; on a dedicated server it stays null.
 */
public final class Lokaal {
    public static volatile Supplier<UUID> bron = () -> null;

    @Nullable
    public static UUID speler() {
        return bron.get();
    }

    private Lokaal() {
    }
}
