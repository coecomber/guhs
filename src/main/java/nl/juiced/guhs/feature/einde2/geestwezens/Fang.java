package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice geest_modellen. Fang's flight: the player rides Fang's {@code ZADEL} along
 * the points and cannot get off. Skelet stand-in: a teleport to the last point, then {@code daarna}.
 */
public final class Fang {
    private Fang() {
    }

    /** Flies p along the points (in p's dimension); {@code daarna} on arrival. False: could not start. */
    public static boolean vlieg(ServerPlayer p, List<Vec3> pad, @Nullable Consumer<ServerPlayer> daarna) {
        if (pad.isEmpty()) {
            return false;
        }
        Vec3 eind = pad.get(pad.size() - 1);
        p.teleportTo(eind.x, eind.y, eind.z);
        p.resetFallDistance();
        if (daarna != null) {
            daarna.accept(p);
        }
        return true;
    }
}
