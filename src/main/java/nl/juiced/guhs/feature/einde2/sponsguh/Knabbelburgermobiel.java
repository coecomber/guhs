package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice sponsguh_wezens: the Knabbelburgermobiel ({@link KnabbelburgermobielEntity}). It has no item and no recipe:
 * only {@link #zet} makes one.
 * <p>
 * A car made for a player is the ONE car of that player: the player's own record ({@code GuhQuests.saved},
 * {@code guhs_einde2_sponsguh_wezens_mobiel}) names it, and a car that no longer matches the record (tidied up with
 * {@link #ruimOp}, or replaced by a new {@link #zet}) takes itself away as soon as it is loaded while its player is online.
 * <p>
 * hook fase B: the second part of the Sponsguh story puts one down for its player and tidies it up afterwards.
 */
public final class Knabbelburgermobiel {
    private static final String KEY = "guhs_einde2_sponsguh_wezens_mobiel";

    /**
     * Puts a Knabbelburgermobiel down. {@code voor}: only this player may drive it (null: anybody); a car this player had
     * before disappears.
     *
     * @return null when none could be made
     */
    @Nullable
    public static KnabbelburgermobielEntity zet(ServerLevel level, Vec3 plek, float yaw, @Nullable ServerPlayer voor) {
        KnabbelburgermobielEntity mobiel = E2SponsguhFeature.KNABBELBURGERMOBIEL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mobiel == null) {
            return null;
        }
        mobiel.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        if (voor != null) {
            ruimOp(voor);
            mobiel.zetVoor(voor.getUUID());
            GuhQuests.saved(voor).putString(KEY, mobiel.getUUID().toString());
        }
        level.addFreshEntity(mobiel);
        return mobiel;
    }

    /** Takes away the Knabbelburgermobiel that was made for this player (now when it is loaded, else as soon as it is). */
    public static void ruimOp(ServerPlayer voor) {
        KnabbelburgermobielEntity mobiel = van(voor);
        GuhQuests.saved(voor).remove(KEY);
        if (mobiel != null) {
            mobiel.opgeruimd();
        }
    }

    /** The loaded car that was made for this player (in any dimension), or null. */
    @Nullable
    public static KnabbelburgermobielEntity van(ServerPlayer voor) {
        String s = GuhQuests.saved(voor).getStringOr(KEY, "");
        if (s.isEmpty()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(s);
            for (ServerLevel level : voor.level().getServer().getAllLevels()) {
                if (level.getEntity(id) instanceof KnabbelburgermobielEntity m && m.isAlive()) {
                    return m;
                }
            }
        } catch (IllegalArgumentException e) {
            // (a broken record: no car)
        }
        return null;
    }

    static boolean isDeEchte(ServerPlayer voor, UUID mobiel) {
        return mobiel.toString().equals(GuhQuests.saved(voor).getStringOr(KEY, ""));
    }

    private Knabbelburgermobiel() {
    }
}
