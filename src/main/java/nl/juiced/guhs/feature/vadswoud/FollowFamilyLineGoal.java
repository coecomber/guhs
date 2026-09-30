package nl.juiced.guhs.feature.vadswoud;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * A baby guh walks in a line behind its parent: the first baby right behind the parent, the second behind the first,
 * and so on (their "Plek" in the family, see {@link GuhGezin}). A baby without a family (or whose family is out of
 * sight) follows the nearest grown-up guh, like vanilla's FollowParentGoal (which this replaces).
 */
public class FollowFamilyLineGoal extends Goal {
    private static final double LOOK = 16, ALONE_LOOK = 8;
    private final GuhEntity baby;
    @Nullable
    private GuhEntity leader;
    private int recheck;
    private int repath;

    public FollowFamilyLineGoal(GuhEntity baby) {
        this.baby = baby;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** How close behind its leader a baby walks. */
    private double gap(GuhEntity ahead) {
        return (ahead.getBbWidth() + baby.getBbWidth()) * 0.5 + 0.45;
    }

    @Override
    public boolean canUse() {
        if (!baby.isBaby() || baby.isOrderedToSit() || baby.isPassenger() || baby.isLeashed() || !baby.mayWander()) {
            return false;
        }
        if (--recheck > 0) {
            return false;
        }
        recheck = 8 + baby.getRandom().nextInt(6);
        leader = findLeader();
        return leader != null && baby.distanceTo(leader) > gap(leader) + 1.0;
    }

    @Override
    public boolean canContinueToUse() {
        if (leader == null || !leader.isAlive() || !baby.isBaby() || baby.isOrderedToSit() || baby.isPassenger() || !baby.mayWander()) {
            return false;
        }
        double d = baby.distanceTo(leader);
        return d > gap(leader) && d < LOOK + 8;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        leader = null;
        baby.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (leader == null) {
            return;
        }
        baby.getLookControl().setLookAt(leader, 10f, baby.getMaxHeadXRot());
        if (--repath <= 0) {
            repath = 5;
            double d = baby.distanceTo(leader);
            baby.getNavigation().moveTo(leader, d > 7 ? 1.4 : 1.15);
        }
        if (GuhGezin.familyOf(baby) != 0) {
            GuhGezin.noticeFamily(baby);
        }
    }

    /** The one to walk behind: the baby just ahead in the line, else a parent, else (no family near) any grown-up guh. */
    @Nullable
    GuhEntity findLeader() {
        long family = GuhGezin.familyOf(baby);
        if (family != 0) {
            int place = GuhGezin.placeOf(baby);
            List<GuhEntity> members = baby.level().getEntitiesOfClass(GuhEntity.class, baby.getBoundingBox().inflate(LOOK),
                    g -> g != baby && g.isAlive() && GuhGezin.familyOf(g) == family);
            GuhEntity ahead = members.stream().filter(g -> g.isBaby() && GuhGezin.placeOf(g) > 0 && GuhGezin.placeOf(g) < place)
                    .max(Comparator.comparingInt(GuhGezin::placeOf)).orElse(null);
            if (ahead != null) {
                return ahead;
            }
            GuhEntity parent = members.stream().filter(g -> !g.isBaby()).min(Comparator.comparingDouble(baby::distanceToSqr)).orElse(null);
            if (parent != null) {
                return parent;
            }
        }
        return baby.level().getEntitiesOfClass(GuhEntity.class, baby.getBoundingBox().inflate(ALONE_LOOK, 4, ALONE_LOOK), g -> !g.isBaby() && g.isAlive())
                .stream().min(Comparator.comparingDouble(baby::distanceToSqr)).orElse(null);
    }
}
