package nl.juiced.guhs.feature.huisje;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

/** The registered chores ({@link Klus}), in registration order (= the order in the huisje screen). */
public final class Klusjes {
    private static final List<Klus> ALLE = new CopyOnWriteArrayList<>();

    private Klusjes() {
    }

    /** Registers a chore (once per id; a second one with the same id replaces the first). */
    public static void registreer(Klus k) {
        for (int i = 0; i < ALLE.size(); i++) {
            if (ALLE.get(i).id().equals(k.id())) {
                ALLE.set(i, k);
                return;
            }
        }
        ALLE.add(k);
    }

    public static List<Klus> alle() {
        return List.copyOf(ALLE);
    }

    @Nullable
    public static Klus van(String id) {
        for (Klus k : ALLE) {
            if (k.id().equals(id)) {
                return k;
            }
        }
        return null;
    }
}
