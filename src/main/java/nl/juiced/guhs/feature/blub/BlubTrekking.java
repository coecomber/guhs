package nl.juiced.guhs.feature.blub;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 1.8.0 (Guhdromenland): the dice of a Blub-guh (ontwerp-guhs180-dromen.md, the Blub-Guh-Meter). A wild one gets its values
 * once, when it spawns, and they never change after that:
 * <ul>
 *   <li>{@link #meter}: how guh it is, 0..100 (0 = a pure Blub, 100 = almost a guh): a bell around {@value #METER_MIDDEN}
 *       (sigma {@value #METER_SIGMA}), cut off at 0 and 100;</li>
 *   <li>{@link Soort}: blauw 50%, weide 22%, schildje 15%, ravijn 9%, zwam 4%;</li>
 *   <li>tint: 0 = the normal colour (60%), 1 and 2 = a pastel one (20% each);</li>
 *   <li>grootte: whole percents 75..125 (a bell around 100, sigma {@value #GROOTTE_SIGMA}; 75 and 125 are rare, no title).</li>
 * </ul>
 * A babyblubje gets every number somewhere between its parents' and the soort and tint of one of them ({@link #erf}).
 */
public final class BlubTrekking {
    public static final int METER_MIDDEN = 35, METER_SIGMA = 18;
    public static final float GROOTTE_MIN = 0.75f, GROOTTE_MAX = 1.25f;
    /** The size in whole percents (1.8.0, round 2 of the finish: 75..125, sigma 8.5). */
    public static final int GROOTTE_MIN_PROCENT = 75, GROOTTE_MAX_PROCENT = 125;
    public static final double GROOTTE_SIGMA = 8.5;

    /** The kinds, as in Minecraft Dungeons II (the order is saved: append only). */
    public enum Soort {
        BLAUW(50), WEIDE(22), SCHILD(15), RAVIJN(9), ZWAM(4);

        /** Chance in percent (they add up to 100). */
        public final int kans;

        Soort(int kans) {
            this.kans = kans;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Soort van(int i) {
            Soort[] all = values();
            return all[Math.floorMod(i, all.length)];
        }

        public static Soort van(String id) {
            for (Soort s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return BLAUW;
        }
    }

    /** One set of dice. */
    public record Waarden(int meter, Soort soort, int tint, float grootte) {
    }

    private BlubTrekking() {
    }

    public static int meter(RandomSource random) {
        return Mth.clamp((int) Math.round(METER_MIDDEN + random.nextGaussian() * METER_SIGMA), 0, 100);
    }

    public static Soort soort(RandomSource random) {
        int r = random.nextInt(100);
        for (Soort s : Soort.values()) {
            if (r < s.kans) {
                return s;
            }
            r -= s.kans;
        }
        return Soort.BLAUW;
    }

    public static int tint(RandomSource random) {
        int r = random.nextInt(10);
        return r < 6 ? 0 : r < 8 ? 1 : 2;
    }

    public static float grootte(RandomSource random) {
        return uitProcent((int) Math.round(100 + random.nextGaussian() * GROOTTE_SIGMA));
    }

    /** A size as whole percents, clamped to 75..125 (older Blubs saved a float like 1.0734: that becomes 107). */
    public static int procent(float grootte) {
        return Mth.clamp(Math.round(grootte * 100f), GROOTTE_MIN_PROCENT, GROOTTE_MAX_PROCENT);
    }

    /** Whole percents (clamped) to the scale factor. */
    public static float uitProcent(int procent) {
        return Mth.clamp(procent, GROOTTE_MIN_PROCENT, GROOTTE_MAX_PROCENT) / 100f;
    }

    /** Rounds any size to a whole percent in 75..125. */
    public static float rond(float grootte) {
        return uitProcent(procent(grootte));
    }

    public static Waarden trek(RandomSource random) {
        return new Waarden(meter(random), soort(random), tint(random), grootte(random));
    }

    /**
     * 1.8.0: a Blub of a part of De Zingende Weide. A blue one anywhere (about half of them); else the kind of its part
     * ({@code deel}: weide, schildje, ravijn or zwam), and in a part without a kind of its own (null) the usual draw.
     */
    public static Waarden trek(RandomSource random, @Nullable Soort deel) {
        if (deel == null) {
            return trek(random);
        }
        Soort soort = random.nextInt(100) < 50 ? Soort.BLAUW : deel;
        return new Waarden(meter(random), soort, tint(random), grootte(random));
    }

    /** A babyblubje: meter and grootte (whole percents) somewhere between the parents' (both ends included), soort and tint of one of them. */
    public static Waarden erf(Waarden a, Waarden b, RandomSource random) {
        int lo = Math.min(a.meter(), b.meter()), hi = Math.max(a.meter(), b.meter());
        int meter = lo + random.nextInt(hi - lo + 1);
        int glo = Math.min(procent(a.grootte()), procent(b.grootte())), ghi = Math.max(procent(a.grootte()), procent(b.grootte()));
        float grootte = uitProcent(glo + random.nextInt(ghi - glo + 1));
        Soort soort = random.nextBoolean() ? a.soort() : b.soort();
        int tint = random.nextBoolean() ? a.tint() : b.tint();
        return new Waarden(meter, soort, tint, grootte);
    }

    /** The stage of the meter (what the model shows): 0 puur, 1 blosjes, 2 guh-oogjes + streepjesmondje (20-49), 3 heel gezicht met :3 (50-59), 4 roze oortjes, 5 ronde puntjes, 6 guh-oren. */
    public static int stap(int meter) {
        if (meter <= 0) {
            return 0;
        }
        if (meter >= 100) {
            return 6;
        }
        return meter < 20 ? 1 : meter < 50 ? 2 : meter < 60 ? 3 : meter < 80 ? 4 : 5;
    }

    /** "Blub ■■■······· Guh 30%". */
    public static String metertje(int meter) {
        int n = Math.round(meter / 10f);
        return "■".repeat(n) + "·".repeat(10 - n);
    }
}
