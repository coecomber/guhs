package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;

/**
 * einde2 phase B, slice h4_vuur: "a moment later" for one player (a scene that waits until the chunks round a teleport are
 * there, the night that falls before the Leeuwschildpad-guh). Not saved: a player who logs out loses what was planned, and
 * the story simply starts it again from their step ({@link E2H4Vuur#regie}).
 */
final class Later {
    private record Taak(long op, Consumer<ServerPlayer> wat) {
    }

    private static final Map<UUID, List<Taak>> TAKEN = new ConcurrentHashMap<>();

    private Later() {
    }

    /** Runs {@code wat} for this player in {@code ticks} ticks (their own ticks: only while they are online). */
    static void plan(ServerPlayer p, int ticks, Consumer<ServerPlayer> wat) {
        TAKEN.computeIfAbsent(p.getUUID(), u -> new ArrayList<>()).add(new Taak(p.tickCount + Math.max(1, ticks), wat));
    }

    /** Is something planned for this player? (the story waits for it) */
    static boolean gepland(ServerPlayer p) {
        List<Taak> l = TAKEN.get(p.getUUID());
        return l != null && !l.isEmpty();
    }

    /** (the player's tick) runs what is due. */
    static void tick(ServerPlayer p) {
        List<Taak> l = TAKEN.get(p.getUUID());
        if (l == null || l.isEmpty()) {
            return;
        }
        List<Taak> nu = new ArrayList<>();
        l.removeIf(t -> {
            if (p.tickCount >= t.op()) {
                nu.add(t);
                return true;
            }
            return false;
        });
        for (Taak t : nu) {
            t.wat().accept(p);
        }
    }

    static void vergeet(UUID speler) {
        TAKEN.remove(speler);
    }
}
