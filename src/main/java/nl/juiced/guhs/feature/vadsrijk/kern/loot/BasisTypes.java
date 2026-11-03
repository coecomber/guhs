package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/** All base types. Register them from your registry class's register() (kern: 3 weapons + 2 armour pieces). */
public final class BasisTypes {
    private static final Map<String, BasisType> ALLE = new LinkedHashMap<>();

    private BasisTypes() {
    }

    public static synchronized BasisType registreer(BasisType t) {
        ALLE.put(t.id(), t);
        return t;
    }

    @Nullable
    public static BasisType van(String id) {
        return ALLE.get(id);
    }

    public static Collection<BasisType> alle() {
        return Collections.unmodifiableCollection(ALLE.values());
    }
}
