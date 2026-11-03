package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.Locale;

import net.minecraft.util.RandomSource;

/** The rarities (design 4.3): colour, number of random bonuses, base-stat multiplier, drop weight. */
public enum Zeldzaamheid {
    GEWOON(0xB0B0B0, 1, 1, 1.00, 600),
    ONGEWOON(0x55DD55, 1, 2, 1.10, 260),
    ZELDZAAM(0x4A9DFF, 2, 3, 1.22, 100),
    EPISCH(0xB45CFF, 3, 4, 1.36, 30),
    LEGENDARISCH(0xFF9A1F, 4, 5, 1.52, 8),
    GUHDDELIJK(0xFF5FC8, 5, 5, 1.75, 2);

    public final int rgb, minBonus, maxBonus, gewicht;
    public final double factor;

    Zeldzaamheid(int rgb, int minBonus, int maxBonus, double factor, int gewicht) {
        this.rgb = rgb;
        this.minBonus = minBonus;
        this.maxBonus = maxBonus;
        this.factor = factor;
        this.gewicht = gewicht;
    }

    public String key() {
        return "vadsrijk.kern.zeldzaamheid." + name().toLowerCase(Locale.ROOT);
    }

    public static Zeldzaamheid van(String n) {
        try {
            return valueOf(n.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GEWOON;
        }
    }

    /** A weighted random rarity; {@code geluk} >= 1 shifts weight to the rarer ones (bosses use 3). */
    public static Zeldzaamheid rol(RandomSource r, double geluk) {
        double[] w = new double[values().length];
        double som = 0;
        for (Zeldzaamheid z : values()) {
            w[z.ordinal()] = z.gewicht * Math.pow(geluk, z.ordinal());
            som += w[z.ordinal()];
        }
        double x = r.nextDouble() * som;
        for (Zeldzaamheid z : values()) {
            x -= w[z.ordinal()];
            if (x < 0) {
                return z;
            }
        }
        return GEWOON;
    }
}
