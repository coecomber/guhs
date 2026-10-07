package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice sponsguh_wezens: the Knabbelburgermobiel. It has no item and no recipe: only {@link #zet} makes one.
 * Skeleton stub: none is made until the sponsguh_wezens slice fills it in.
 * <p>
 * hook fase B: the second part of the Sponsguh story puts one down for its player and tidies it up afterwards.
 */
public final class Knabbelburgermobiel {
    /**
     * Puts a Knabbelburgermobiel down. {@code voor}: only this player may drive it (null: anybody).
     *
     * @return null when none could be made
     */
    @Nullable
    public static KnabbelburgermobielEntity zet(ServerLevel level, Vec3 plek, float yaw, @Nullable ServerPlayer voor) {
        return null;
    }

    /** Takes away the Knabbelburgermobiel that was made for this player. */
    public static void ruimOp(ServerPlayer voor) {
    }

    private Knabbelburgermobiel() {
    }
}
