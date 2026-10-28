package nl.juiced.guhs.feature.slaapzorg;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * Sung asleep ({@link Slapen#slaapIn}): curled up on the spot with its eyes shut and a zzz now and then, until its time is
 * up (the morning, or the end of a nap: a big yawn), until it is hurt, or until its owner walks away. Holds still: no
 * following, no strolling, no emotes in between.
 */
public class InslaapGoal extends Goal {
    private final GuhEntity guh;

    public InslaapGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return Slapen.isIngezongen(guh) && !Slapen.baasWeg(guh) && !guh.isPassenger();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && guh.hurtTime == 0;
    }

    @Override
    public void start() {
        guh.getNavigation().stop();
        guh.setInSittingPose(true);
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
    }

    @Override
    public void stop() {
        boolean uitgeslapen = guh.getPersistentData().getLongOr(Slapen.SLAAP_TOT, 0L) <= guh.level().getGameTime();
        Slapen.wordWakker(guh, uitgeslapen);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        guh.getNavigation().stop();
        if (!guh.isInSittingPose()) {
            guh.setInSittingPose(true);
        }
        if (!GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)) {
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);   // (the Vadswoud's tick opens eyes not shut by a nest)
        }
        if (guh.level() instanceof ServerLevel level) {
            if (guh.tickCount % 50 == 0) {
                level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.15, 0.05, 0.15, 0.0);
            }
            if (guh.isBaby() && guh.tickCount % 200 == 0) {
                guh.ageUp(10);
            }
        }
    }
}
