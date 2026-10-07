package nl.juiced.guhs.feature.einde2.vesting;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice vesting: the cells with magere guhs in the modular Mika-vesting.
 * Skeleton stub: the counter is real, the cells themselves come with the vesting slice.
 */
public final class VestingCellen {
    /** Player data (GuhQuests.saved): how many magere guhs this player freed from vesting cells. */
    public static final String BEVRIJD = "guhs_einde2_vesting_bevrijd";

    /** hook fase B: how many vesting guhs this player freed (the title asks). */
    public static int bevrijd(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(BEVRIJD, 0);
    }

    private VestingCellen() {
    }
}
