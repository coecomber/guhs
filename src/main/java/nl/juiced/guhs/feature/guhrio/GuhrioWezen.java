package nl.juiced.guhs.feature.guhrio;

import net.minecraft.server.level.ServerPlayer;

/**
 * A creature of a Super Guhrio level (an entity). The player's own game sees what happens between the two of you, the
 * moment it happens (it is the player's game that moves the player), and reports it; the server checks it and calls:
 * {@link #stamp} when the player landed on it, {@link #raakt} when it touched the player any other way. Nobody is hurt:
 * a creature that is "dangerous" calls {@link GuhrioSpel#geraakt} (lose your power-up, or back to your flag).
 */
public interface GuhrioWezen {
    /** Can you land on it right now (your game bounces you off it and reports a stamp)? */
    boolean stampbaar();

    /** Does it send you back when it touches you right now? */
    boolean gevaarlijk();

    /** The player landed on it. */
    void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie);

    /** It touched the player (not from above). */
    default void raakt(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (gevaarlijk()) {
            GuhrioSpel.geraakt(player, sessie);
        }
    }
}
