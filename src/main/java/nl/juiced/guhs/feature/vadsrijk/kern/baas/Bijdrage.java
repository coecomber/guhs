package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who helped against a boss (ronde 11): damage dealt, healing done (in the arena during the fight) and damage tanked. A player's
 * share = the mean of their share in each kind that anybody did; more than {@link #DREMPEL} = a personal loot chest.
 */
public final class Bijdrage {
    public static final double DREMPEL = 0.05;
    private final Map<UUID, double[]> per = new HashMap<>();
    /** 1.10.3 (fight log): wall-clock time of the first contribution of this fight (0 = none yet). */
    private long startMs;

    public void schade(UUID p, double n) {
        begin();
        per.computeIfAbsent(p, k -> new double[3])[0] += n;
    }

    private void begin() {
        if (startMs == 0) {
            startMs = System.currentTimeMillis();
        }
    }

    /** 1.10.3 (fight log): seconds since the first contribution. */
    public double seconden() {
        return startMs == 0 ? 0 : (System.currentTimeMillis() - startMs) / 1000.0;
    }

    /** 1.10.3 (fight log): {damage, healing, tanked} of this player. */
    public double[] van(UUID p) {
        double[] w = per.get(p);
        return w == null ? new double[3] : w.clone();
    }

    public void heal(UUID p, double n) {
        begin();
        per.computeIfAbsent(p, k -> new double[3])[1] += n;
    }

    public void tank(UUID p, double n) {
        begin();
        per.computeIfAbsent(p, k -> new double[3])[2] += n;
    }

    public double aandeel(UUID p) {
        double[] mij = per.get(p);
        if (mij == null) {
            return 0;
        }
        double som = 0;
        int soorten = 0;
        for (int i = 0; i < 3; i++) {
            double tot = 0;
            for (double[] w : per.values()) {
                tot += w[i];
            }
            if (tot > 0) {
                som += mij[i] / tot;
                soorten++;
            }
        }
        return soorten == 0 ? 0 : som / soorten;
    }

    public boolean verdientKist(UUID p) {
        return aandeel(p) > DREMPEL;
    }

    public Set<UUID> spelers() {
        return per.keySet();
    }

    public void wis() {
        per.clear();
        startMs = 0;
    }
}
