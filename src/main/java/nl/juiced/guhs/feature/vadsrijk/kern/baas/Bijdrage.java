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

    public void schade(UUID p, double n) {
        per.computeIfAbsent(p, k -> new double[3])[0] += n;
    }

    public void heal(UUID p, double n) {
        per.computeIfAbsent(p, k -> new double[3])[1] += n;
    }

    public void tank(UUID p, double n) {
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
    }
}
