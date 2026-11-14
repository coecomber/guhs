package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * 1101-balans: "in combat" for the Vads regeneration. A player is in combat for {@link #RUST_TICKS} (5 s) after hitting or being
 * hit by a Vadsrijk creature/boss (Schade.klap marks both). In combat the Vads come back at {@link #REGEN_GEVECHT} (0.3 % of max
 * per second), outside combat unchanged at {@link #REGEN_RUST} (1 %/s): before, the 1 %/s also ran during fights, so one mob could
 * never bring you down.
 */
public final class Gevecht {
    public static final int RUST_TICKS = 100;
    public static final double REGEN_RUST = 0.01;
    public static final double REGEN_GEVECHT = 0.003;

    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private Gevecht() {
    }

    public static void gevochten(ServerPlayer p) {
        LAATST.put(p.getUUID(), p.level().getGameTime());
    }

    public static boolean in(ServerPlayer p) {
        Long t = LAATST.get(p.getUUID());
        return t != null && p.level().getGameTime() - t < RUST_TICKS;
    }

    /** The share of max Vads that comes back per second now. */
    public static double regen(ServerPlayer p) {
        return in(p) ? REGEN_GEVECHT : REGEN_RUST;
    }

    /** Tests/dev: clear (or pretend a fight). */
    public static void zet(ServerPlayer p, boolean aan) {
        if (aan) {
            gevochten(p);
        } else {
            LAATST.remove(p.getUUID());
        }
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
    }
}
