package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice geest_modellen. The cosmetic libelkonijnguh pet (one per player, never
 * lost; the reward of the lantern questline). Skelet stand-in: nobody has one, none is given.
 */
public final class Libelkonijnguh {
    private Libelkonijnguh() {
    }

    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives p their pet (or calls it back); null: none. */
    @Nullable
    public static Entity geef(ServerPlayer p) {
        return null;
    }
}
