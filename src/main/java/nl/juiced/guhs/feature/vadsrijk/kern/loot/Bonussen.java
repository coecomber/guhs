package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;

/**
 * The random bonus pool: per stat a range at level 1 plus growth per level. An item gets 1-5 distinct bonuses (by rarity);
 * rarer items roll closer to the top of the range. {@link #bereik} is what the tooltip/test may rely on.
 */
public final class Bonussen {
    /** min and max at level 1, growth of both per level, pool weight. */
    public record Bereik(double min, double max, double groei, int gewicht) {
    }

    private static final Map<Stat, Bereik> POOL = new EnumMap<>(Stat.class);

    static {
        POOL.put(Stat.VADS, new Bereik(4, 10, 1.5, 10));
        POOL.put(Stat.KRACHT, new Bereik(1, 3, 0.35, 10));
        POOL.put(Stat.BEHENDIGHEID, new Bereik(1, 3, 0.35, 8));
        POOL.put(Stat.WIJSHEID, new Bereik(1, 3, 0.35, 8));
        POOL.put(Stat.VERDEDIGING, new Bereik(1, 2, 0.22, 8));   // (balans 2026-11: was 1-3 +0.3/level, ~-25 %)
        POOL.put(Stat.KRITIEK, new Bereik(1, 3, 0.05, 7));
        POOL.put(Stat.KRITIEKSCHADE, new Bereik(4, 10, 0.2, 6));
        POOL.put(Stat.AANVALSSNELHEID, new Bereik(2, 6, 0.1, 5));
        POOL.put(Stat.LOOPSNELHEID, new Bereik(1, 4, 0.05, 5));
        POOL.put(Stat.COOLDOWNREDUCTIE, new Bereik(1, 4, 0.05, 5));
        POOL.put(Stat.ELEMENTSCHADE, new Bereik(3, 8, 0.15, 6));
        POOL.put(Stat.ELEMENTWEERSTAND, new Bereik(3, 8, 0.15, 6));
        POOL.put(Stat.LEVENSROOF, new Bereik(1, 3, 0.05, 4));
        POOL.put(Stat.HEALKRACHT, new Bereik(3, 8, 0.15, 4));
    }

    private Bonussen() {
    }

    /** The [min, max] a bonus of this stat can roll on an item of this level. */
    public static double[] bereik(Stat s, int level) {
        Bereik b = POOL.get(s);
        int l = Math.max(0, level - 1);
        return new double[] {b.min + b.groei * l, b.max + b.groei * l};
    }

    /** {@code n} distinct bonuses for an item of this level and rarity. */
    public static List<Bonus> rol(int n, int level, Zeldzaamheid z, RandomSource r) {
        List<Stat> vrij = new ArrayList<>(POOL.keySet());
        List<Bonus> uit = new ArrayList<>();
        for (int i = 0; i < n && !vrij.isEmpty(); i++) {
            int som = 0;
            for (Stat s : vrij) {
                som += POOL.get(s).gewicht;
            }
            int x = r.nextInt(som);
            Stat gekozen = vrij.get(0);
            for (Stat s : vrij) {
                x -= POOL.get(s).gewicht;
                if (x < 0) {
                    gekozen = s;
                    break;
                }
            }
            vrij.remove(gekozen);
            double[] b = bereik(gekozen, level);
            double u = Math.pow(r.nextDouble(), 1.0 / (1 + 0.35 * z.ordinal()));   // rarer: closer to the top
            double v = b[0] + (b[1] - b[0]) * u;
            v = gekozen.procent ? Math.round(v * 10) / 10.0 : Math.round(v);
            v = Math.max(b[0], Math.min(b[1], v));
            uit.add(new Bonus(gekozen, v));
        }
        return uit;
    }
}
