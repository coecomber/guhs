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

    /** Does the server want to hear that it touched the player from the side (default: when it is dangerous)? */
    default boolean aanraakbaar() {
        return gevaarlijk();
    }

    /**
     * (the player's game) Does it touch this box right now? Default: its own box. A creature that is not a box (a turning
     * grill spit) answers for its real shape.
     */
    default boolean raaktVak(net.minecraft.world.phys.AABB vak) {
        return ((net.minecraft.world.entity.Entity) this).getBoundingBox().intersects(vak);
    }

    /** Something you stand on that moves (a platform): the player's game carries you along with it. */
    default boolean draagt() {
        return false;
    }

    /** A knabbel thrown by {@code gooier} (the Vuurpeper) hit it. True: it did something (the knabbel is gone either way). */
    default boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        return false;
    }

    /** A sliding shell hit it ({@code schopper}: who kicked the shell, or null). True: the shell goes on, false: it bounces back. */
    default boolean schild(@javax.annotation.Nullable ServerPlayer schopper) {
        return true;
    }

    /** Guhshi's tongue reached it. True: eaten (it is gone for a while). */
    default boolean tong(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        return false;
    }
}
