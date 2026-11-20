package nl.juiced.guhs.feature.sterrenveld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 1.10.10 "Sterrenreeks": catch {@value #REEKS} droomsterretjes in a row and the last one gives {@value #BONUS} droomzand
 * extra (then it counts again from 0). A star that fades uncaught within {@value #BIJ} blocks of you breaks your row.
 * Per player, only in memory (a restart or a new dream simply starts over somewhere in a row, never a loss).
 */
public final class Sterrenreeks {
    public static final int REEKS = 10, BONUS = 5;
    /** How near a fading star must be to break your row. */
    public static final double BIJ = 20;
    private static final Map<UUID, Integer> TEL = new HashMap<>();

    private Sterrenreeks() {
    }

    /** One more caught: the bonus when this one completes a row (0 otherwise). */
    public static int gevangen(ServerPlayer p) {
        int n = TEL.getOrDefault(p.getUUID(), 0) + 1;
        if (n >= REEKS) {
            TEL.remove(p.getUUID());
            return BONUS;
        }
        TEL.put(p.getUUID(), n);
        return 0;
    }

    /** A star faded uncaught here: the rows of the players near it break. */
    public static void gemist(ServerLevel level, Vec3 waar) {
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(waar) <= BIJ * BIJ) {
                TEL.remove(p.getUUID());
            }
        }
    }

    /** How many in a row this player has now (tests, debugging). */
    public static int stand(ServerPlayer p) {
        return TEL.getOrDefault(p.getUUID(), 0);
    }

    /** Forget this player's row (tests). */
    public static void vergeet(ServerPlayer p) {
        TEL.remove(p.getUUID());
    }
}
