package nl.juiced.guhs.feature.wereld;

import net.minecraft.server.level.ServerPlayer;

/**
 * bbq2: the per-player step state a quest NPC works with ({@link QuestRol}). The story engine's {@code Verhaallijn}
 * (nl.juiced.guhs.feature.verhaal) implements it; this interface only exists so the two foundations do not depend on each
 * other's classes. Complete: nobody edits it (CONTRACT_130 5.1).
 */
public interface Stappen {
    /** The step this player is at (0 = not begun; the number of steps = done). */
    int stap(ServerPlayer p);

    /** One step further, but only when the player is exactly at {@code vanStap}; true = changed. */
    boolean verder(ServerPlayer p, int vanStap);

    /** True the first time only (per player and name): once-per-player rewards. */
    boolean eenmalig(ServerPlayer p, String naam);
}
