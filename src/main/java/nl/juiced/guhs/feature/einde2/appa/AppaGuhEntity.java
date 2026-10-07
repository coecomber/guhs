package nl.juiced.guhs.feature.einde2.appa;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice appa: the Appa-guh, a big six-legged flying bison with a guh face that carries its owner, three passengers
 * and a saddle chest.
 * Skeleton stub (a stand-in that does nothing): the appa slice makes the real creature under this name.
 */
public class AppaGuhEntity extends Einde2Plaatshouder {
    /** Extra decorative seats that only {@link #zetTeam} fills (the team of the story). */
    public static final int TEAM_STOELEN = 5;

    public AppaGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** hook fase B: puts a team member on one of the team seats. */
    public void zetTeam(Entity e) {
    }

    /** hook fase B: the team steps off. */
    public void teamAf() {
    }
}
