package nl.juiced.guhs.feature.einde2.appa;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice appa: the Momo-guh, a lemur-like guh that rides on its owner's right shoulder and picks things up.
 * Skeleton stub (a stand-in that does nothing): the appa slice makes the real creature under this name.
 */
public class MomoGuhEntity extends Einde2Plaatshouder {
    public MomoGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
}
