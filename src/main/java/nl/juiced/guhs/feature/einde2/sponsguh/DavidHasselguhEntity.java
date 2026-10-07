package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: David Hasselguh, a giant guh that swims through the void with players on its back (see
 * {@link Hasselguh}).
 * Skeleton stub (a stand-in that does nothing): the sponsguh_wezens slice makes the real creature under this name.
 */
public class DavidHasselguhEntity extends SponsguhWezen {
    public DavidHasselguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** hook fase B: one more rider (up to two: the cast). */
    public void zetMee(Entity e) {
    }
}
