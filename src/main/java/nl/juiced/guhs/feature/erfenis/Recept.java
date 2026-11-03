package nl.juiced.guhs.feature.erfenis;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * De Erfenis: one thing a building can make. A source (animals, fields, Bosje...) has no {@link #in}; a workshop takes its
 * inputs from the village storage while it works. {@link #tabel}: OUTPUT units per hour at balans level 1..5 (full staff, no
 * bonus); our 10 levels are mapped onto it by {@link Productie#perUur}. {@link #bij}: byproducts as a factor of the output
 * (veren = half the eggs, mest 1.2x the angora...). {@link #minLevel}: from which building level it may be chosen.
 * Lang: {@code erfenis.recept.<id>}.
 */
public record Recept(String id, Goed uit, int uitN, Map<Goed, Integer> in, double[] tabel, Map<Goed, Double> bij, int minLevel) {

    /** A source: makes {@code uit} at this rate table. */
    static Recept bron(String id, Goed uit, double[] tabel) {
        return new Recept(id, uit, 1, Map.of(), tabel, Map.of(), 1);
    }

    Recept met(Goed bijproduct, double factor) {
        Map<Goed, Double> b = new LinkedHashMap<>(bij);
        b.put(bijproduct, factor);
        return new Recept(id, uit, uitN, in, tabel, b, minLevel);
    }

    Recept vanaf(int level) {
        return new Recept(id, uit, uitN, in, tabel, bij, level);
    }

    /** A workshop recipe: {@code uitN} of {@code uit} per job from these inputs (goed, n, goed, n...), at the building's table x mult. */
    static Recept werk(String id, Goed uit, int uitN, double[] tabel, double mult, Object... invoer) {
        Map<Goed, Integer> in = new LinkedHashMap<>();
        for (int i = 0; i + 1 < invoer.length; i += 2) {
            in.put((Goed) invoer[i], (Integer) invoer[i + 1]);
        }
        double[] t = new double[tabel.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = tabel[i] * mult;
        }
        return new Recept(id, uit, uitN, in, t, Map.of(), 1);
    }

    public String naamKey() {
        return "erfenis.recept." + id;
    }
}
