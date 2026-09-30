package nl.juiced.guhs.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** "Passive (flee)": when a mob hurts the guh, it runs away from that mob for a while. */
public class GuhFleeGoal extends Goal {
    private static final int FLEE_TICKS = 100;
    private final GuhEntity guh;
    private LivingEntity attacker;

    public GuhFleeGoal(GuhEntity guh) {
        this.guh = guh;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!guh.isTame() || guh.getBehavior() != GuhEntity.Behavior.PASSIVE_FLEE || guh.isOrderedToSit()
                || guh.getPersonality() == GuhPersonality.BRAVE) { // brave guhs never run
            return false;
        }
        LivingEntity last = guh.getLastHurtByMob();
        if (last == null || !last.isAlive() || guh.tickCount - guh.getLastHurtByMobTimestamp() > FLEE_TICKS) {
            return false;
        }
        attacker = last;
        return true;
    }

    @Override
    public void start() {
        runAway();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && (!guh.getNavigation().isDone() || guh.distanceToSqr(attacker) < 64);
    }

    @Override
    public void tick() {
        if (guh.getNavigation().isDone()) {
            runAway();
        }
    }

    private void runAway() {
        Vec3 away = DefaultRandomPos.getPosAway(guh, 16, 7, attacker.position());
        if (away != null) {
            guh.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
        }
    }
}
