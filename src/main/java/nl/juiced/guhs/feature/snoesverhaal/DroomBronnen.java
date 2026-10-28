package nl.juiced.guhs.feature.snoesverhaal;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerUnlocks;
import nl.juiced.guhs.feature.weer.Weertje;

/**
 * 1.8.0: the dream sources of the weerpotjes ({@link WeerFeature#DROOMBRONNEN}). Every source gives its weerpotje once per
 * player ({@link WeerUnlocks#potjeEenmalig}: nothing when the player already has that weertje or got the potje before).
 * <ul>
 *   <li>the five pieces of Snoes' dream: given by the story itself the moment a piece is found
 *       ({@link DroomVoortgang#geef} → {@link SnoesVerhaal}), so the island agents only call {@code DroomVoortgang.geef};</li>
 *   <li>the finale: the regenboog, given by {@link SnoesVerhaal#naFinale};</li>
 *   <li>the Droomplein fountain: the plein calls {@link #fontein} when a player throws a knabbel in.</li>
 * </ul>
 */
public final class DroomBronnen {
    private DroomBronnen() {
    }

    /** The weertje of a piece of Snoes' dream (its island's dream source). */
    public static Weertje weertjeVoor(DroomVoortgang.Stukje stukje) {
        return switch (stukje) {
            case LIEDJE -> Weertje.PLUISJES;      // Slaapliedjesheuvels
            case BAD -> Weertje.BELLEN;           // Wolkenbadje
            case STER -> Weertje.STERREN;         // Sterrenkussenveld
            case DROOMZAND -> Weertje.KNUSMIST;   // Maanmolentje
            case HERINNERING -> Weertje.HARTJES;  // Herinneringseiland
        };
    }

    /** The weerpotje of this dream source, once per player; true when it was given now. */
    public static boolean geef(ServerPlayer player, Weertje weertje) {
        return WeerUnlocks.potjeEenmalig(player, weertje);
    }

    /** The Droomplein fountain (a knabbel thrown in): the knabbelbuitje-potje, once. */
    public static boolean fontein(ServerPlayer player) {
        return geef(player, Weertje.KNABBEL);
    }
}
