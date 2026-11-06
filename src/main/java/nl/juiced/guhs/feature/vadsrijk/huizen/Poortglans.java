package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** U-HUIZEN: the pink shimmer in the gate arch of every ward somebody is in (once a second, only near players). */
final class Poortglans {
    private Poortglans() {
    }

    static void tick(MinecraftServer server) {
        ServerLevel l = Kwartier.level(server);
        if (l == null || l.players().isEmpty()) {
            return;
        }
        Set<Integer> wijken = new HashSet<>();
        for (ServerPlayer p : l.players()) {
            BlockPos m = WijkPlan.midden(WijkPlan.wijkVan(p.getX()));
            if (p.blockPosition().distSqr(m) < 48 * 48) {
                wijken.add(WijkPlan.wijkVan(p.getX()));
            }
        }
        for (int w : wijken) {
            BlockPos m = WijkPlan.midden(w);
            l.sendParticles(ParticleTypes.CHERRY_LEAVES, m.getX() + 0.5, m.getY() + 1.8, m.getZ() - 9.5, 6, 1.0, 1.0, 0.1, 0.01);
            l.sendParticles(ParticleTypes.END_ROD, m.getX() + 0.5, m.getY() + 1.5, m.getZ() - 9.5, 2, 1.0, 1.0, 0.1, 0.0);
        }
    }
}
