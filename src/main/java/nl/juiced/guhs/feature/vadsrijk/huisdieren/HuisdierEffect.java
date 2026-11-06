package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import net.minecraft.server.level.ServerPlayer;

/**
 * Extension point (G-PETS): what an active pet does for its owner (small stats, a buff, a heal; never damage: decision R2).
 * T-RIJDIER uses it: {@code rijdier.Maatjes} calls {@link #tick} every second for the owner of a pet that walks along in the
 * Vadsrijk and adds {@link #extraStats} through kern's {@code Spelers.EXTRA_STATS}. Give a pet one via {@link Huisdier#effect}, or
 * (simpler, with a role and texts) {@code Maatjes.zet(petId, kracht)}.
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
