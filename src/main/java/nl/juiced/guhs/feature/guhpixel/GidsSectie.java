package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.server.level.ServerPlayer;

/**
 * A section of the Guhdex tab "Guhpixel &amp; uitjes". A slice only writes this server-side builder (no Guhdex client
 * code) and registers it with {@link GidsBlad#registreer}. Order ({@link #volgorde}): lobby 10, grap1 20, grap2 30,
 * among 40, guhkade 50, kantoor 60, bioscoop 70, reisbureau 80, parkour 90.
 */
public interface GidsSectie {
    String id();

    int volgorde();

    /** Shown for this player at all? (Whether it needs the Guhpixel unlock: {@link #zonderToegang}.) */
    boolean zichtbaar(ServerPlayer p);

    void vul(ServerPlayer p, Bouwer b);

    /** True for sections that also show before the player unlocked Guhpixel (reisbureau, parkour). */
    default boolean zonderToegang() {
        return false;
    }
}
