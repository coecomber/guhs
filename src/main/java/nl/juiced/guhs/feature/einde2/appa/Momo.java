package nl.juiced.guhs.feature.einde2.appa;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * einde2, slice appa: a player's own Momo-guh (one per player).
 * Skeleton stub: nobody has a Momo until the appa slice fills it in (then {@link #geef} never returns null).
 * <p>
 * hook fase B: the story gives it ({@link #geef}).
 */
public final class Momo {
    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives this player their Momo-guh, once: when they have one already, that one comes. */
    @Nullable
    public static MomoGuhEntity geef(ServerPlayer p) {
        return null;
    }

    @Nullable
    public static MomoGuhEntity roep(ServerPlayer p) {
        return null;
    }

    /** Tells Momo to stay at home (where it sits now) or to come along again. */
    public static void zetThuis(ServerPlayer p, boolean thuis) {
    }

    private Momo() {
    }
}
