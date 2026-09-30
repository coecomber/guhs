package nl.juiced.guhs.feature.band;

import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * The 2.10 bits of the synced {@code KnusVlaggen} int of a guh (see {@link GuhHooks#heeft}/{@link GuhHooks#zet}). Taken
 * before 2.10: 1, 2, 4, 8, 16, 32..224, 1&lt;&lt;12. Always use these constants, never raw numbers.
 */
public final class BandVlaggen {
    /** Inside its Guhhuisje (sleeping): not drawn, no name, not pushable (fundament, HuisjeGoal). */
    public static final int HUISJE_BINNEN = 1 << 13;
    /** A level-3 guh ("zielsguh bff 5evr &lt;3"): fundament keeps it set; samen draws the sparkling heart next to the name. */
    public static final int ZIELSGUH = 1 << 14;
    /** Happy buff (a favourite, see {@link Band#maakBlij}): subtle sparkles, faster chores, more hearts. */
    public static final int BLIJ = 1 << 15;
    /** Playing with a toy (speelgoed). */
    public static final int SPEELT = 1 << 16;
    /** Doing a chore (klusjes: a small tool or basket is drawn, optional). */
    public static final int KLUSJE = 1 << 17;
    /** 2.10.1: a guest of the Guhkamer (mirrors the server-side mark Guhkamer.GAST, so the Guh menu knows it). */
    public static final int GUHKAMER_GAST = 1 << 18;

    /** Does this entity (a guh) have this flag? False for anything that isn't a guh. */
    public static boolean heeft(Entity e, int vlag) {
        return e instanceof GuhEntity guh && GuhHooks.heeft(guh, vlag);
    }

    /** Sets or clears a flag on a guh (no-op for other entities). */
    public static void zet(Entity e, int vlag, boolean aan) {
        if (e instanceof GuhEntity guh && GuhHooks.heeft(guh, vlag) != aan) {
            GuhHooks.zet(guh, vlag, aan);
        }
    }

    private BandVlaggen() {
    }
}
