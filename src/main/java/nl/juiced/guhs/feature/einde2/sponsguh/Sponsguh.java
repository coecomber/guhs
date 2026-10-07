package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * einde2, slice sponsguh_wezens: a player's own Sponsguh buddy (exactly one per player, never lost).
 * Skeleton stub: nobody has one until the sponsguh_wezens slice fills it in (then {@link #geef} never returns null).
 * <p>
 * hook fase B: nothing in phase A gives one in survival; the Sponsguh story calls {@link #geef} as its reward.
 */
public final class Sponsguh {
    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives this player theirs, once: when they have one already, that one comes. */
    @Nullable
    public static SponsguhEntity geef(ServerPlayer p) {
        return null;
    }

    /** Brings the player's own one to them (null: they have none). */
    @Nullable
    public static SponsguhEntity roep(ServerPlayer p) {
        return null;
    }

    /** The buddy stays at home where it stands (true) or follows its owner again (false). */
    public static void zetThuis(ServerPlayer p, boolean thuis) {
    }

    private Sponsguh() {
    }
}
