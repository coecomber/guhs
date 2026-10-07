package nl.juiced.guhs.feature.einde2.zwevers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice zwevers: who can see the Leegtewalvisguh. It is only there (sent to the client, with a back to stand on,
 * a song to hear, a mouth to feed) for players with this flag; by default nobody has it. The flag is the player's own
 * (it survives death and is kept per player, so any number of players can have it).
 * <p>
 * How it holds: {@link LeegtewalvisguhEntity#broadcastToPlayer} sends the whale only to players with the flag, its back
 * only carries them, and for everything else in the world the whale is not there. The player's own client is told the
 * flag ({@link E2ZweversPayloads.Zicht}), so {@link #ziet} also answers there (for the local player).
 * <p>
 * hook fase B: the questline of the harbour pub sets the flag ({@code Walviszicht.zet(player, true)} when the old
 * kapitein-guh's quest is accepted).
 */
public final class Walviszicht {
    /** The player key (GuhQuests.saved). */
    public static final String SLEUTEL = "guhs_einde2_zwevers_walviszicht";

    /** (Client) the local player's flag, as the server last told it. */
    private static boolean lokaal;

    /** Gives or takes the flag; a whale that is around appears or vanishes for this player at once. */
    public static void zet(ServerPlayer p, boolean ziet) {
        boolean was = ziet(p);
        GuhQuests.saved(p).putBoolean(SLEUTEL, ziet);
        stuur(p);
        if (was != ziet && p.level() instanceof ServerLevel level) {
            LeegtewalvisguhEntity.herzie(level);
        }
    }

    /** Does this player see the Leegtewalvisguh? On a client only the local player's own answer is known. */
    public static boolean ziet(Player p) {
        if (p instanceof ServerPlayer) {
            return GuhQuests.saved(p).getBooleanOr(SLEUTEL, false);
        }
        return p.isLocalPlayer() && lokaal;
    }

    /** Tells the player's client its flag (at login, and whenever it changes). */
    public static void stuur(ServerPlayer p) {
        ModNetworking.sendTo(p, new E2ZweversPayloads.Zicht(ziet(p)));
    }

    /** (Client) what the server said. */
    static void lokaal(boolean ziet) {
        lokaal = ziet;
    }

    private Walviszicht() {
    }
}
