package nl.juiced.guhs.feature.einde2.mikas;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice mikas: the Kaasmijten that pop out of a knabbelkoorvrucht.
 * Skeleton stub: nothing pops out until the mikas slice fills it in.
 */
public final class Kaasmijten {
    /**
     * Called by the biomes slice when a knabbelkoorvrucht is eaten at this spot. Rolls the chance itself (about 1 in 8).
     *
     * @return true when a Kaasmijt popped out
     */
    public static boolean uitVrucht(ServerLevel level, Vec3 plek, @Nullable ServerPlayer eter) {
        return false;
    }

    private Kaasmijten() {
    }
}
