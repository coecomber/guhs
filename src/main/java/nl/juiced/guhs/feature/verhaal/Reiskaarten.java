package nl.juiced.guhs.feature.verhaal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.5): the registered travel maps. A group with a map gets a "Reiskaart" row in the
 * Guhdex tab Verhalen: the route, per halte its steps with ticks, "Je bent hier" at the current line, one sentence "Dit
 * moet je nu doen", later haltes as "???" and the replay buttons of the scenes and cards seen. Register from common code:
 * <pre>
 * Reiskaarten.registreer(new Reiskaart("knabbelring", "knabbelring", List.of(
 *         new Halte("ring_h1", 1, 40, 120, "knabbelgouw"), new Halte("ring_h2", 2, 86, 96, "guhvendel"))));
 * </pre>
 */
public final class Reiskaarten {
    private static final Map<String, Reiskaart> ALLE = new LinkedHashMap<>();

    public static void registreer(Reiskaart k) {
        synchronized (ALLE) {
            ALLE.put(k.id(), k);
        }
    }

    @Nullable
    public static Reiskaart van(@Nullable String id) {
        synchronized (ALLE) {
            return id == null ? null : ALLE.get(id);
        }
    }

    /** The map of this group (null: it has none). */
    @Nullable
    public static Reiskaart vanGroep(String groep) {
        synchronized (ALLE) {
            return ALLE.values().stream().filter(k -> k.groep().equals(groep)).findFirst().orElse(null);
        }
    }

    public static List<Reiskaart> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.values());
        }
    }

    private Reiskaarten() {
    }
}
