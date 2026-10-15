package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bestaand.Schijn;

/**
 * einde2 phase B, slice h4_vuur: the tiles that light up for ONE player (the square of the dance floor, the next step of the
 * Drakendans), shown with main's {@link Schijn}: only that player's client gets the bright block, the world keeps its own.
 */
final class Oplichten {
    private static final Map<UUID, Map<BlockPos, BlockState>> NU = new ConcurrentHashMap<>();

    private Oplichten() {
    }

    /** From now on this player sees these blocks (instead of the earlier ones). */
    static void zet(ServerPlayer p, Map<BlockPos, BlockState> blokken) {
        if (blokken.isEmpty()) {
            NU.remove(p.getUUID());
        } else {
            NU.put(p.getUUID(), Map.copyOf(blokken));
        }
        Schijn.straks(p);
    }

    static void wis(ServerPlayer p) {
        if (NU.remove(p.getUUID()) != null) {
            Schijn.straks(p);
        }
    }

    /** (the Schijn source) */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        Map<BlockPos, BlockState> m = NU.get(p.getUUID());
        if (m != null) {
            m.forEach((pos, state) -> {
                if (Schijn.dichtbij(p, pos)) {
                    gewenst.put(pos, state);
                }
            });
        }
    }
}
