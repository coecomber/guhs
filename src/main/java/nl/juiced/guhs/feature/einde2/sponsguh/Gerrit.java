package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * einde2, slice sponsguh_wezens: a player's own pet snail Gerrit (one per player, never lost).
 * Skeleton stub: nobody has one until the sponsguh_wezens slice fills it in (then {@link #geef} never returns null).
 * <p>
 * hook fase B: nothing in phase A gives one in survival; the Sponsguh story calls {@link #geef} as its reward.
 */
public final class Gerrit {
    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives this player theirs, once: when they have one already, that one comes. */
    @Nullable
    public static GerritEntity geef(ServerPlayer p) {
        return null;
    }

    /** Brings the player's own one to them (null: they have none). */
    @Nullable
    public static GerritEntity roep(ServerPlayer p) {
        return null;
    }

    private Gerrit() {
    }
}
