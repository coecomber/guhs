package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import net.minecraft.world.entity.player.Player;

/**
 * einde2, slice bouw_bikinibodem: the purple leegtemist inside Bikinibodem's dome (the town's "sea water").
 * Skeleton stub: nobody is in the mist until the bouw_bikinibodem slice fills it in.
 * <p>
 * hook fase B: the cutscenes of the Sponsguh story ask it.
 */
public final class Leegtemist {
    /** Is this player inside the dome (the zone {@code koepel} of the one Bikinibodem)? */
    public static boolean inMist(Player p) {
        return false;
    }

    private Leegtemist() {
    }
}
