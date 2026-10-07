package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: the snail Gerrit: the character, and with an owner the pet that follows a player (see {@link Gerrit}).
 * Skeleton stub (a stand-in that does nothing): the sponsguh_wezens slice makes the real creature under this name.
 */
public class GerritEntity extends SponsguhWezen {
    public GerritEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
}
