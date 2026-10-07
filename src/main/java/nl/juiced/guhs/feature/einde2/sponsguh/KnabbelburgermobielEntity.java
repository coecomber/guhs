package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice sponsguh_wezens: the Knabbelburgermobiel, a car shaped like a Knabbelburger for a driver and two passengers
 * (see {@link Knabbelburgermobiel}). Not one of the cast: a vehicle.
 * Skeleton stub (a stand-in that does nothing): the sponsguh_wezens slice makes the real vehicle under this name.
 */
public class KnabbelburgermobielEntity extends Einde2Plaatshouder {
    public KnabbelburgermobielEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
}
