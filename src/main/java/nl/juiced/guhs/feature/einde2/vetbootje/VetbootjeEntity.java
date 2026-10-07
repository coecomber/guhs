package nl.juiced.guhs.feature.einde2.vetbootje;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice vetbootje: the player's own small, slow flying ship.
 * Skeleton stub (a stand-in that does nothing): the vetbootje slice makes the real vehicle under this name.
 */
public class VetbootjeEntity extends Einde2Plaatshouder {
    public VetbootjeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
}
