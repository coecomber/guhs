package nl.juiced.guhs.feature.einde2.zwevers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * einde2, slice zwevers: who can see the Leegtewalvisguh. It is only there (visible, touchable) for players with this
 * flag; by default nobody has it.
 * Skeleton stub: nobody sees it until the zwevers slice fills it in.
 * <p>
 * hook fase B: the questline of the harbour pub sets the flag.
 */
public final class Walviszicht {
    public static void zet(ServerPlayer p, boolean ziet) {
    }

    public static boolean ziet(Player p) {
        return false;
    }

    private Walviszicht() {
    }
}
