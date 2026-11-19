package nl.juiced.guhs.feature.guhwinter.kern;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * 1.10.10 Guhwinter: one-shot moment listeners ({@code Guhwinter.opMoment}). Every listener has an id that is unique and
 * stays the same between versions (it is what the world remembers) and a time. {@link #vuur} fires, oldest time first,
 * every listener whose time has come and whose id is not in the fired set yet; the id goes into the set BEFORE the
 * listener runs, so a listener that throws never runs twice. Because the set is saved with the world, a moment that
 * passed while the server was down fires at the next start (catch-up).
 */
public final class Momenten {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** One listener. {@code volgorde}: registration order (same time: first registered fires first). */
    public record Luisteraar(String id, long tijd, Consumer<MinecraftServer> actie, int volgorde) {
    }

    private final List<Luisteraar> lijst = new CopyOnWriteArrayList<>();

    /** Adds a listener; an id that is already taken is a programming mistake (IllegalArgumentException). */
    public void voegToe(String id, ZonedDateTime wanneer, Consumer<MinecraftServer> actie) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Guhwinter moment without id");
        }
        synchronized (lijst) {
            if (lijst.stream().anyMatch(l -> l.id().equals(id))) {
                throw new IllegalArgumentException("Guhwinter moment id used twice: " + id);
            }
            lijst.add(new Luisteraar(id, wanneer.toInstant().toEpochMilli(), actie, lijst.size()));
        }
    }

    /** All listeners, by time. */
    public List<Luisteraar> alle() {
        List<Luisteraar> l = new ArrayList<>(lijst);
        l.sort(Comparator.comparingLong(Luisteraar::tijd).thenComparingInt(Luisteraar::volgorde));
        return l;
    }

    /**
     * Fires every listener with {@code tijd <= nu} whose id is not in {@code gevuurd} (adds it there, then calls
     * {@code opgeslagen} once when anything fired). Returns the ids that fired, in order.
     */
    public List<String> vuur(MinecraftServer server, long nu, Set<String> gevuurd, Runnable opgeslagen) {
        List<String> uit = new ArrayList<>();
        for (Luisteraar l : alle()) {
            if (l.tijd() > nu) {
                break;
            }
            if (!gevuurd.add(l.id())) {
                continue;
            }
            uit.add(l.id());
            opgeslagen.run();
            try {
                l.actie().accept(server);
            } catch (RuntimeException e) {
                LOGGER.error("Guhwinter: moment {} failed", l.id(), e);
            }
        }
        return uit;
    }
}
