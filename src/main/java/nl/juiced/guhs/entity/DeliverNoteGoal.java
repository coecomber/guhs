package nl.juiced.guhs.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * A guh with a secret note walks up to the nearest player, whispers to them and hands over the note.
 */
public class DeliverNoteGoal extends Goal {
    private static final double FIND_RANGE = 24.0;
    private static final double HAND_OVER_RANGE = 2.2;

    private final GuhEntity guh;
    @Nullable
    private Player target;
    private int repath;

    public DeliverNoteGoal(GuhEntity guh) {
        this.guh = guh;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!guh.hasSecretNote() || guh.isOrderedToSit()) {
            return false;
        }
        target = guh.level().getNearestPlayer(guh, FIND_RANGE);
        return target != null && !target.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        return guh.hasSecretNote() && target != null && target.isAlive() && !target.isSpectator()
                && guh.distanceToSqr(target) < FIND_RANGE * FIND_RANGE * 1.5;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        target = null;
        guh.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        guh.getLookControl().setLookAt(target, 30f, 30f);
        if (guh.distanceTo(target) <= HAND_OVER_RANGE + guh.getBbWidth() / 2) {
            guh.deliverSecretNote(target);
        } else if (--repath <= 0) {
            repath = 10;
            guh.getNavigation().moveTo(target, 1.15);
        }
    }
}
