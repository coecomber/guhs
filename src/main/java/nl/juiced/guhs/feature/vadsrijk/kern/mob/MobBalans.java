package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import nl.juiced.guhs.feature.vadsrijk.kern.Balans;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;

/**
 * 1101-balans: the HP side of the damage-curve fix (option A, BALANS_1101_ANALYSE.md section 5).
 * <ul>
 *   <li>Player damage: the main stat gives 1 % per point instead of 2 % ({@link Schade#hoofdFactor}), so player damage grows
 *   about linearly instead of quadratically.</li>
 *   <li>Open-world creatures: their per-level HP slope goes 0.30 -> 0.45 ({@link #helling}), times {@code [mobs] leven} (x1.4).</li>
 *   <li>Bosses and boss helpers: only {@link #compensatie} = the player's damage change at that level, so a boss takes as long as
 *   in 1.10.0 (the user: boss loot and Zwamgrot difficulty unchanged); 1102: times {@code [bazen] leven} ({@link #baas}).</li>
 * </ul>
 */
public final class MobBalans {
    public static final double HELLING_OUD = 0.30, HELLING = 0.45;

    private MobBalans() {
    }

    /** The slope fix: (1 + 0.45 (lv-1)) / (1 + 0.30 (lv-1)); 1 at lv 1, x1.40 at lv 14, x1.48 at lv 70. */
    public static double helling(int level) {
        int l = Math.max(1, level) - 1;
        return (1 + HELLING * l) / (1 + HELLING_OUD * l);
    }

    /** Open-world creature HP factor: [mobs] leven x the slope fix. */
    public static double leven(int level) {
        return Balans.mobLeven() * helling(level);
    }

    /** 1102-balans: the real HP factor of bosses and boss helpers = the compensation x {@code [bazen] leven} (x1.25). */
    public static double baas(int level) {
        return compensatie(level) * Balans.bazenLeven();
    }

    /** Boss HP compensation: how much the player's damage changed at this level (model Kracht, see Schade#modelHoofd). */
    public static double compensatie(int level) {
        return Schade.damageVerandering(level);
    }
}
