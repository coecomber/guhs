package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/** All loot tables by id ("<slice>:<name>", e.g. "kern:proefklodder", "wezens:klodder"). */
public final class LootTabellen {
    private static final Map<String, LootTabel> ALLE = new LinkedHashMap<>();

    private LootTabellen() {
    }

    public static synchronized void registreer(String id, LootTabel t) {
        ALLE.put(id, t);
    }

    @Nullable
    public static LootTabel van(String id) {
        return ALLE.get(id);
    }

    /** Every table by id (read-only; integration check). */
    public static Map<String, LootTabel> alle() {
        return java.util.Collections.unmodifiableMap(ALLE);
    }
}
