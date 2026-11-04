package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;

/**
 * Who gets what after a caravan attack (pure, gametested). A helper is a player who cheered up at least one of the attackers
 * (kern counts everyone who contributed to a defeat as a helper of it). Every helper gets
 * {@code (BASIS + PER_WEZEN x attackers) x the share of attackers cheered up} Vadsmunten (brons), +{@link #EIGEN} per attacker
 * they helped with themselves; when ALL attackers were cheered up, each helper also has a {@link #KANS} chance at a rare item.
 * Nobody helped: nothing. (The attackers' own xp and loot come from kern as usual.)
 */
public final class KaravaanBeloning {
    public static final int BASIS = 60, PER_WEZEN = 15, EIGEN = 5;
    public static final double KANS = 0.25;
    /** The rare items (ids for {@link Karavaan#zeldzaam}). */
    public static final String[] ZELDZAAM = {"regenboogdrankje", "herrolknabbel", "verbetersteentjes"};

    public record Beloning(long munten, @Nullable String zeldzaam) {
    }

    private KaravaanBeloning() {
    }

    /**
     * @param geholpen per helper: how many of the attackers they helped cheer up
     * @param totaal   attackers in this attack
     * @param verslagen attackers cheered up (by anyone)
     */
    public static Map<UUID, Beloning> verdeel(Map<UUID, Integer> geholpen, int totaal, int verslagen, RandomSource r) {
        Map<UUID, Beloning> uit = new LinkedHashMap<>();
        if (totaal <= 0 || verslagen <= 0) {
            return uit;
        }
        double deel = Math.min(1.0, verslagen / (double) totaal);
        boolean alles = verslagen >= totaal;
        for (Map.Entry<UUID, Integer> e : geholpen.entrySet()) {
            int eigen = e.getValue();
            if (eigen <= 0) {
                continue;
            }
            long munten = Math.round((BASIS + PER_WEZEN * totaal) * deel) + (long) EIGEN * eigen;
            String item = alles && r.nextDouble() < KANS ? ZELDZAAM[r.nextInt(ZELDZAAM.length)] : null;
            uit.put(e.getKey(), new Beloning(munten, item));
        }
        return uit;
    }
}
