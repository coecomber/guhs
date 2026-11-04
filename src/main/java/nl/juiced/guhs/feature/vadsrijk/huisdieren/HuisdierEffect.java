package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import net.minecraft.server.level.ServerPlayer;

/**
 * Extension point (G-PETS, UNUSED for now: pets are purely cosmetic): what an active pet could do for its owner later on
 * (small stats, an attack, a buff, a heal). Give it to a pet via {@link Huisdier#effect}; {@code Huisdieren} would then call
 * {@link #tick} for the owner of an active pet (every second) and could add {@link #extraStats} to kern's
 * {@code Spelers.EXTRA_STATS}. Nothing calls this yet.
 */
public interface HuisdierEffect {
    /** Every second while the pet walks along with its owner in the Vadsrijk (an attack, a heal, a buff...). */
    default void tick(ServerPlayer eigenaar, HuisdierEntity huisdier) {
    }

    /** Extra stats while this pet is active (null = none). */
    default nl.juiced.guhs.feature.vadsrijk.kern.Stats extraStats(ServerPlayer eigenaar) {
        return null;
    }
}
