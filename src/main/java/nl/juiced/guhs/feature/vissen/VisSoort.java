package nl.juiced.guhs.feature.vissen;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Rarity;

/**
 * The special fish of the Guhvis-wedstrijd (only caught in the contest pond during a contest). Every catch gets a
 * random weight between min and max (light ones are more common) and points: base + perKg per kilo. The Mika-meerval
 * is the bad one: it costs points.
 */
public enum VisSoort {
    KAASVIS(380, 0.2f, 1.6f, 5, 10, Rarity.COMMON, ChatFormatting.YELLOW),
    VADSBAARS(280, 0.8f, 4.5f, 8, 8, Rarity.COMMON, ChatFormatting.LIGHT_PURPLE),
    GUHPUFFER(140, 0.3f, 2.0f, 20, 10, Rarity.UNCOMMON, ChatFormatting.GREEN),
    NJEGFOREL(100, 1.0f, 3.8f, 35, 12, Rarity.RARE, ChatFormatting.AQUA),
    MIKA_MEERVAL(80, 2.0f, 7.0f, -25, 0, Rarity.COMMON, ChatFormatting.RED),
    GOUDEN_GUHVIS(20, 3.0f, 9.0f, 150, 15, Rarity.EPIC, ChatFormatting.GOLD);

    /** Chance (out of the total of all chances) to bite. */
    public final int chance;
    public final float minKg, maxKg;
    public final int base, perKg;
    public final Rarity rarity;
    public final ChatFormatting colour;

    VisSoort(int chance, float minKg, float maxKg, int base, int perKg, Rarity rarity, ChatFormatting colour) {
        this.chance = chance;
        this.minKg = minKg;
        this.maxKg = maxKg;
        this.base = base;
        this.perKg = perKg;
        this.rarity = rarity;
        this.colour = colour;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Points for a catch of this weight. */
    public int points(int grams) {
        return Math.round(base + perKg * grams / 1000f);
    }

    /** A random weight in grams (skewed to the light end: a heavy one is something to brag about). */
    public int randomGrams(RandomSource random) {
        float r = (float) Math.pow(random.nextFloat(), 1.8);
        return Math.round((minKg + (maxKg - minKg) * r) * 1000f);
    }

    public boolean isBad() {
        return base < 0;
    }

    public static VisSoort random(RandomSource random) {
        int total = 0;
        for (VisSoort s : values()) {
            total += s.chance;
        }
        int roll = random.nextInt(total);
        for (VisSoort s : values()) {
            roll -= s.chance;
            if (roll < 0) {
                return s;
            }
        }
        return KAASVIS;
    }

    @Nullable
    public static VisSoort byId(String id) {
        for (VisSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** "2,35 kg" (Dutch decimal comma). */
    public static String kg(int grams) {
        return String.format(Locale.ROOT, "%.2f", grams / 1000f).replace('.', ',') + " kg";
    }
}
