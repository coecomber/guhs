package nl.juiced.guhs.feature.speelgoed;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * One play session of one guh (or muisje) with one toy: the {@link KlusTaak} that the huisje's HuisjeGoal (residents)
 * or {@link SpeelGoal} (everyone else) ticks. Walking there is shared here.
 */
public abstract class SpeelTaak implements KlusTaak {
    protected final Mob mob;
    protected final ServerLevel level;
    protected int ticks;
    private int loopTicks;
    private Vec3 laatstePlek = Vec3.ZERO;
    private int vast, bijna;

    protected SpeelTaak(Mob mob, ServerLevel level) {
        this.mob = mob;
        this.level = level;
    }

    @Override
    public final boolean tick() {
        ticks++;
        if (!mob.isAlive() || mob.level() != level) {
            return false;
        }
        return speel();
    }

    /** One tick of playing: true = keep going. */
    protected abstract boolean speel();

    /** Walks towards doel: true once within dichtbij (horizontally, and about the same height), or as close as it gets. */
    protected boolean loopNaar(Vec3 doel, double snelheid, double dichtbij) {
        double dx = mob.getX() - doel.x, dz = mob.getZ() - doel.z;
        double d2 = dx * dx + dz * dz;
        boolean hoogte = Math.abs(mob.getY() - doel.y) < 1.6;
        if (d2 <= dichtbij * dichtbij && hoogte) {
            mob.getNavigation().stop();
            return true;
        }
        loopTicks++;
        var nav = mob.getNavigation();
        if (nav.isDone() && hoogte && d2 <= (dichtbij + 1.0) * (dichtbij + 1.0) && ++bijna > 10) {
            return true;     // (as close as it gets: the last bit is fine)
        }
        if (nav.isDone() || loopTicks % 20 == 0) {
            net.minecraft.world.level.pathfinder.Path pad = nav.createPath(net.minecraft.core.BlockPos.containing(doel), 0);
            if (pad != null) {
                nav.moveTo(pad, snelheid);
            } else {
                nav.moveTo(doel.x, doel.y, doel.z, snelheid);
            }
        }
        if (loopTicks % 40 == 0) {   // not getting anywhere?
            vast = mob.position().distanceToSqr(laatstePlek) < 0.25 ? vast + 1 : 0;
            laatstePlek = mob.position();
        }
        return false;
    }

    @Override
    public String toString() {
        var nav = mob.getNavigation();
        return getClass().getSimpleName() + "[ticks " + ticks + ", loop " + loopTicks + ", vast " + vast + ", nav done " + nav.isDone()
                + ", target " + nav.getTargetPos() + ", path " + (nav.getPath() == null ? "-" : nav.getPath().getEndNode() + " reach " + nav.getPath().canReach()) + "]";
    }

    /** Stuck for a while on the way (can't reach it). */
    protected boolean vast() {
        return vast >= 3;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
