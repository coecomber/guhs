package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 2.3, 2.4), owner: slice geest_spel. The way into the Geestenwereld for a player and the
 * guests round them, and which zones are open for whom. Skelet stand-in: the guide goes in alone, every zone is open.
 */
public final class GeestToegang {
    private GeestToegang() {
    }

    /** The guide and every player within 5 blocks into the Geestenwereld, at the marker {@code plek}. False: nobody went. */
    public static boolean stuur(ServerPlayer gids, String plek) {
        return Geestenwereld.stuur(gids, plek);
    }

    /** The guests who came along with this guide (and are still inside). */
    public static List<ServerPlayer> gastenBij(ServerPlayer gids) {
        return List.of();
    }

    /** Is this zone of the Geestenwereld open for this player (CONTRACT_E2_B 2.3; a guest: the guide's answer)? */
    public static boolean zoneOpen(ServerPlayer p, String zone) {
        return true;
    }
}
