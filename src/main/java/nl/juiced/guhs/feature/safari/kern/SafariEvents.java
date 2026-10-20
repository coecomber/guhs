package nl.juiced.guhs.feature.safari.kern;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.server.MinecraftServer;

/**
 * Op Safari! step 2 (S0): tiny internal hooks so the safari slices react to each other without depending on each other's
 * classes. Register in a slice's {@code register(...)}; listeners run on the server thread, right after the change was
 * saved; a listener that throws is logged and skipped (the others still run).
 * <ul>
 *   <li>{@link #opDexNieuw}: a species got a better photo ({@code was} = the previous best stars, 0 = first photo);</li>
 *   <li>{@link #opZoneCompleet}: every dex species of a zone now has a photo (the expert-guh can pay out);</li>
 *   <li>{@link #opGeheimpje}: a geheimpje was found for the first time;</li>
 *   <li>{@link #opMunten}: safarimunten changed (+ earned, - paid).</li>
 * </ul>
 */
public final class SafariEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    public interface DexNieuw {
        void op(MinecraftServer server, UUID speler, String soort, int sterren, int was);
    }

    public interface ZoneCompleet {
        void op(MinecraftServer server, UUID speler, String zone);
    }

    public interface Geheimpje {
        void op(MinecraftServer server, UUID speler, String geheimpje);
    }

    public interface Munten {
        void op(MinecraftServer server, UUID speler, int verschil, String reden);
    }

    private static final List<DexNieuw> DEX = new CopyOnWriteArrayList<>();
    private static final List<ZoneCompleet> ZONE = new CopyOnWriteArrayList<>();
    private static final List<Geheimpje> GEHEIM = new CopyOnWriteArrayList<>();
    private static final List<Munten> MUNTEN = new CopyOnWriteArrayList<>();

    public static void opDexNieuw(DexNieuw l) {
        DEX.add(l);
    }

    public static void opZoneCompleet(ZoneCompleet l) {
        ZONE.add(l);
    }

    public static void opGeheimpje(Geheimpje l) {
        GEHEIM.add(l);
    }

    public static void opMunten(Munten l) {
        MUNTEN.add(l);
    }

    static void dexNieuw(MinecraftServer server, UUID speler, String soort, int sterren, int was) {
        for (DexNieuw l : DEX) {
            veilig(() -> l.op(server, speler, soort, sterren, was));
        }
    }

    static void zoneCompleet(MinecraftServer server, UUID speler, String zone) {
        for (ZoneCompleet l : ZONE) {
            veilig(() -> l.op(server, speler, zone));
        }
    }

    static void geheimpje(MinecraftServer server, UUID speler, String geheimpje) {
        for (Geheimpje l : GEHEIM) {
            veilig(() -> l.op(server, speler, geheimpje));
        }
    }

    static void munten(MinecraftServer server, UUID speler, int verschil, String reden) {
        for (Munten l : MUNTEN) {
            veilig(() -> l.op(server, speler, verschil, reden));
        }
    }

    private static void veilig(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            LOGGER.error("Safari: an event listener failed", e);
        }
    }

    private SafariEvents() {
    }
}
