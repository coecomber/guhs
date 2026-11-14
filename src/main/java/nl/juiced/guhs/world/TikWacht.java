package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 1.10.2 (live crash 2026-11-14 15:27, {@code Sfeer.tick}: a null from {@code getAllEntities()} while entities were discarded during
 * the walk): two small guards for server-tick code.
 * <ul>
 *   <li>{@link #alle}: a COPY of a level's entities without nulls. Walk that, and discard/remove freely: the live entity lookup is
 *       never changed while it is being iterated.</li>
 *   <li>{@link #post}/{@link #pre}/{@link #veilig}: a server-tick hook whose exception is logged (the first time with its stack
 *       trace, after that at most once a minute with a count) instead of crashing the server. The feature skips that tick only.</li>
 * </ul>
 */
public final class TikWacht {
    private static final Logger LOG = LogUtils.getLogger();
    /** Per hook: [last log time ms, errors since that log]. */
    private static final Map<String, long[]> FOUTEN = new ConcurrentHashMap<>();
    static final long LOG_PAUZE_MS = 60_000;

    private TikWacht() {
    }

    /** A copy of these entities without nulls (e.g. {@code TikWacht.alle(sl.getAllEntities())}); safe to discard while walking it. */
    public static List<Entity> alle(Iterable<? extends Entity> bron) {
        List<Entity> l = new ArrayList<>();
        if (bron == null) {
            return l;
        }
        for (Entity e : bron) {
            if (e != null) {
                l.add(e);
            }
        }
        return l;
    }

    /** Registers a guarded {@link ServerTickEvent.Post} listener. */
    public static void post(String naam, Consumer<ServerTickEvent.Post> h) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> veilig(naam, () -> h.accept(e)));
    }

    /** Registers a guarded {@link ServerTickEvent.Pre} listener. */
    public static void pre(String naam, Consumer<ServerTickEvent.Pre> h) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> veilig(naam, () -> h.accept(e)));
    }

    /** Runs {@code r}; an exception is logged (rate-limited per {@code naam}) and swallowed. True = it ran without one. */
    public static boolean veilig(String naam, Runnable r) {
        try {
            r.run();
            return true;
        } catch (RuntimeException | LinkageError | AssertionError t) {
            meld(naam, t);
            return false;
        }
    }

    static void meld(String naam, Throwable t) {
        long nu = System.currentTimeMillis();
        long[] f = FOUTEN.computeIfAbsent(naam, k -> new long[] {0, 0});
        synchronized (f) {
            f[1]++;
            if (f[0] == 0) {
                f[0] = nu;
                f[1] = 0;
                LOG.error("[Guhs] tick hook '{}' threw (the server keeps running; this hook skips the tick)", naam, t);
            } else if (nu - f[0] >= LOG_PAUZE_MS) {
                LOG.error("[Guhs] tick hook '{}' threw {} more time(s) in the last {} s, latest: {}", naam, f[1], (nu - f[0]) / 1000, t.toString());
                f[0] = nu;
                f[1] = 0;
            }
        }
    }

    /** (tests) how many errors were swallowed for this hook since its last log line; -1 = never. */
    public static long fouten(String naam) {
        long[] f = FOUTEN.get(naam);
        return f == null ? -1 : f[1];
    }
}
