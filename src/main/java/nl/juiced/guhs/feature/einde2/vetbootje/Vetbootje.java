package nl.juiced.guhs.feature.einde2.vetbootje;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

/**
 * einde2, slice vetbootje: a player's own vetbootje (one per player, never lost).
 * Skeleton stub: nobody has a vetbootje until the vetbootje slice fills it in (then {@link #geef} never returns null).
 * <p>
 * hook fase B: the questline gives it ({@link #geef}) and the shipyard of the Leegtehaven opens {@link #openWerf}.
 */
public final class Vetbootje {
    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives this player their vetbootje, once: when they have one already, that one comes. */
    @Nullable
    public static VetbootjeEntity geef(ServerPlayer p) {
        return null;
    }

    @Nullable
    public static VetbootjeEntity roep(ServerPlayer p) {
        return null;
    }

    /** Sets the looks of the player's vetbootje: hull colour, sail and name. */
    public static void stel(ServerPlayer p, DyeColor kleur, Zeil zeil, Component naam) {
    }

    /** Opens the customise screen (the werfboekje, later the shipyard). */
    public static void openWerf(ServerPlayer p) {
    }

    private Vetbootje() {
    }
}
