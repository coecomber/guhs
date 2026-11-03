package nl.juiced.guhs.feature.vadsrijk.kern.klasse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/** All classes. Kern registers the Knuffelridder; the klassen slice adds the Pluisjager and the Vadsmagiër. */
public final class Klassen {
    public static final String RIDDER = "knuffelridder", JAGER = "pluisjager", MAGIER = "vadsmagier";
    private static final Map<String, Klasse> ALLE = new LinkedHashMap<>();

    private Klassen() {
    }

    public static synchronized void registreer(Klasse k) {
        ALLE.put(k.id(), k);
    }

    @Nullable
    public static Klasse van(String id) {
        return ALLE.get(id);
    }

    /** The class with this id, or the Knuffelridder. */
    public static Klasse ofRidder(String id) {
        Klasse k = ALLE.get(id);
        return k != null ? k : ALLE.get(RIDDER);
    }

    public static Collection<Klasse> alle() {
        return Collections.unmodifiableCollection(ALLE.values());
    }
}
