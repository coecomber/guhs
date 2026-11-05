package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/** What things are worth to a shop guh (selling loot: by rarity and level, stars add a little). In brons. */
public final class Waarde {
    /** Per rarity (GEWOON .. GUHDDELIJK). */
    public static final int[] ZELDZAAM_FACTOR = {1, 2, 4, 9, 20, 50};

    /** T-AMBACHT: extra sale values per ONE item (the Ambachten materials, dishes, potions); 0 = not theirs. */
    public static final java.util.List<java.util.function.ToLongFunction<ItemStack>> EXTRA = new java.util.concurrent.CopyOnWriteArrayList<>();

    private Waarde() {
    }

    /** Sale value of one piece of gear. */
    public static long gear(Uitrusting u) {
        long basis = 8L + 4L * Math.max(1, u.level());
        double ster = 1 + 0.15 * u.sterren();
        return Math.max(1, Math.round(basis * ZELDZAAM_FACTOR[u.zeldzaamheid().ordinal()] * ster));
    }

    /** Sale value of a whole stack (0 = the shop doesn't want it: things from outside the Vadsrijk, coins). */
    public static long verkoop(ItemStack s) {
        if (s.isEmpty()) {
            return 0;
        }
        Uitrusting u = Rollen.van(s);
        if (u != null) {
            return gear(u);
        }
        Item i = s.getItem();
        if (i instanceof EcoItems.Munt) {
            return 0;
        }
        for (java.util.function.ToLongFunction<ItemStack> f : EXTRA) {   // T-AMBACHT
            long v = f.applyAsLong(s);
            if (v > 0) {
                return v * s.getCount();
            }
        }
        long stuk = Winkels.koopPrijs(i) / 2;
        return stuk * s.getCount();
    }
}
