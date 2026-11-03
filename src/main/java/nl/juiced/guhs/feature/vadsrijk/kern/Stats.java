package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.EnumMap;
import java.util.Map;

/** A bag of stat values (mutable). Missing = 0. */
public final class Stats {
    private final EnumMap<Stat, Double> w = new EnumMap<>(Stat.class);

    public static Stats van(Object... paren) {
        Stats s = new Stats();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            s.plus((Stat) paren[i], ((Number) paren[i + 1]).doubleValue());
        }
        return s;
    }

    public double get(Stat s) {
        return w.getOrDefault(s, 0.0);
    }

    public Stats zet(Stat s, double v) {
        w.put(s, v);
        return this;
    }

    public Stats plus(Stat s, double v) {
        w.merge(s, v, Double::sum);
        return this;
    }

    public Stats plus(Stats o) {
        if (o != null) {
            o.w.forEach(this::plus);
        }
        return this;
    }

    public Stats maal(double f) {
        w.replaceAll((k, v) -> v * f);
        return this;
    }

    public Stats kopie() {
        return new Stats().plus(this);
    }

    public Map<Stat, Double> alles() {
        return java.util.Collections.unmodifiableMap(w);
    }

    @Override
    public String toString() {
        return w.toString();
    }
}
