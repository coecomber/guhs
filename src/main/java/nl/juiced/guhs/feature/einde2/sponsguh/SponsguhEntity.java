package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: Sponsguh Kaasbroek: the character, and with an owner the buddy that follows a player (see {@link Sponsguh}).
 * Skeleton stub (a stand-in that does nothing): the sponsguh_wezens slice makes the real creature under this name.
 */
public class SponsguhEntity extends SponsguhWezen {
    public SponsguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
}
