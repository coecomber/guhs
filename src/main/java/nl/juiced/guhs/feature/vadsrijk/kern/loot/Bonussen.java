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
    /** T-ITEMS: stats that may only roll on these slots (missing = every slot). */
    private static final Map<Stat, java.util.Set<Slot>> ALLEEN_OP = new EnumMap<>(Stat.class);

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
        // T-ITEMS (itemisatie 2.0): the 4 new stats, only on some slots (ALLEEN_OP), so they don't dilute every roll
        POOL.put(Stat.VERDEDIGING_NEGEREN, new Bereik(2, 5, 0.08, 4));
        POOL.put(Stat.BAZENSCHADE, new Bereik(2, 5, 0.1, 4));
        POOL.put(Stat.LEVEN_PER_TREFFER, new Bereik(1, 3, 0.25, 4));
        POOL.put(Stat.KANS_OP_EFFECT, new Bereik(2, 5, 0.08, 4));
        ALLEEN_OP.put(Stat.VERDEDIGING_NEGEREN, java.util.EnumSet.of(Slot.WAPEN, Slot.HANDSCHOENEN, Slot.RING, Slot.ARMBAND));
        ALLEEN_OP.put(Stat.BAZENSCHADE, java.util.EnumSet.of(Slot.WAPEN, Slot.RING, Slot.KETTING, Slot.CAPE));
        ALLEEN_OP.put(Stat.LEVEN_PER_TREFFER, java.util.EnumSet.of(Slot.WAPEN, Slot.HANDSCHOENEN, Slot.ARMBAND, Slot.OORBEL));
        ALLEEN_OP.put(Stat.KANS_OP_EFFECT, java.util.EnumSet.of(Slot.WAPEN, Slot.HANDSCHOENEN, Slot.KETTING, Slot.OORBEL, Slot.ARMBAND));
    }

    /** T-ITEMS: may this stat roll on a piece for this slot? (null slot = the old pool: only the stats without a slot limit.) */
    public static boolean mag(Stat s, @javax.annotation.Nullable Slot slot) {
        if (!POOL.containsKey(s)) {
            return false;
        }
        java.util.Set<Slot> op = ALLEEN_OP.get(s);
        return op == null || (slot != null && op.contains(slot));
    }

    /** T-ITEMS: the pool weight of a stat on a slot (the armband leans on Aanvalssnelheid and kortere wachttijd: x2). */
    static int gewicht(Stat s, @javax.annotation.Nullable Slot slot) {
        int w = POOL.get(s).gewicht;
        if (slot == Slot.ARMBAND && (s == Stat.AANVALSSNELHEID || s == Stat.COOLDOWNREDUCTIE)) {
            w *= 2;
        }
        return w;
    }

    private Bonussen() {
    }

    /** One value for this stat on a piece of this level and rarity (rarer: closer to the top). T-ITEMS: also polijsten. */
    public static double waarde(Stat gekozen, int level, Zeldzaamheid z, RandomSource r) {
        double[] b = bereik(gekozen, level);
        double u = Math.pow(r.nextDouble(), 1.0 / (1 + 0.35 * z.ordinal()));   // rarer: closer to the top
        double v = b[0] + (b[1] - b[0]) * u;
        return rond(gekozen, Math.max(b[0], Math.min(b[1], v)), b);
    }

    /** T-ITEMS: rounded the way the rolls are (percent: 1 decimal, flat: whole), kept inside [min, max]. */
    public static double rond(Stat s, double v, double[] b) {
        v = s.procent ? Math.round(v * 10) / 10.0 : Math.round(v);
        return Math.max(b[0], Math.min(b[1], v));
    }

    /** The [min, max] a bonus of this stat can roll on an item of this level. */
    public static double[] bereik(Stat s, int level) {
        Bereik b = POOL.get(s);
        int l = Math.max(0, level - 1);
        return new double[] {b.min + b.groei * l, b.max + b.groei * l};
    }

    /** {@code n} distinct bonuses for an item of this level and rarity. */
    public static List<Bonus> rol(int n, int level, Zeldzaamheid z, RandomSource r) {
        return rol(n, level, z, r, null);
    }

    /** T-ITEMS: {@code n} distinct bonuses for a piece for this slot (the slot-limited stats only where they may roll). */
    public static List<Bonus> rol(int n, int level, Zeldzaamheid z, RandomSource r, @javax.annotation.Nullable Slot slot) {
        List<Stat> vrij = new ArrayList<>();
        for (Stat st : POOL.keySet()) {
            if (mag(st, slot)) {
                vrij.add(st);
            }
        }
        List<Bonus> uit = new ArrayList<>();
        for (int i = 0; i < n && !vrij.isEmpty(); i++) {
            int som = 0;
            for (Stat s : vrij) {
                som += gewicht(s, slot);
            }
            int x = r.nextInt(som);
            Stat gekozen = vrij.get(0);
            for (Stat s : vrij) {
                x -= gewicht(s, slot);
                if (x < 0) {
                    gekozen = s;
                    break;
                }
            }
            vrij.remove(gekozen);
            uit.add(new Bonus(gekozen, waarde(gekozen, level, z, r)));
        }
        return uit;
    }
}
