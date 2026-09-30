package nl.juiced.guhs.feature.kamperen;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * A guh listening to Opa Guh's story: it walks to a spot in a ring around the campfire (its own spot, by its id), sits
 * down there looking into the fire, and stays as long as the story goes on (Verhalen keeps calling {@link #luister}).
 * It counts as busy (GuhHooks.bezig), so the day rhythm leaves it alone meanwhile.
 */
public class LuisterGoal extends Goal {
    static final String VUUR = "guhs_kamperen_vuur", TOT = "guhs_kamperen_luister_tot";
    private final GuhEntity guh;
    @Nullable
    private BlockPos vuur;
    private Vec3 plek = Vec3.ZERO;
    private int opnieuw;

    public LuisterGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** This guh listens for the next {@code ticks} ticks, by this campfire. */
    public static void luister(GuhEntity guh, BlockPos vuur, int ticks) {
        guh.getPersistentData().putLong(VUUR, vuur.asLong());
        guh.getPersistentData().putLong(TOT, guh.level().getGameTime() + ticks);
        GuhHooks.bezig(guh, ticks);
    }

    /** Is this guh listening to a story right now? */
    public static boolean luistert(GuhEntity guh) {
        return guh.getPersistentData().getLongOr(TOT, 0L) > guh.level().getGameTime();
    }

    @Override
    public boolean canUse() {
        if (!luistert(guh) || guh.isOrderedToSit() || guh.isPassenger() || guh.isLeashed()) {
            return false;
        }
        vuur = BlockPos.of(guh.getPersistentData().getLongOr(VUUR, 0L));
        double hoek = (guh.getId() * 2.39996) % (Math.PI * 2);
        double r = 2.6 + (guh.getId() % 3) * 0.6;
        plek = Vec3.atBottomCenterOf(vuur).add(Math.cos(hoek) * r, 0, Math.sin(hoek) * r);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return luistert(guh) && !guh.isOrderedToSit() && !guh.isPassenger();
    }

    @Override
    public void start() {
        opnieuw = 0;
    }

    @Override
    public void stop() {
        guh.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (vuur == null) {
            return;
        }
        guh.getLookControl().setLookAt(vuur.getX() + 0.5, vuur.getY() + 0.5, vuur.getZ() + 0.5);
        double d = guh.position().distanceToSqr(plek);
        if (d > 1.2 && --opnieuw <= 0) {
            guh.getNavigation().moveTo(plek.x, plek.y, plek.z, 0.8);
            opnieuw = 20;
        } else if (d <= 1.2) {
            guh.getNavigation().stop();
        }
    }
}
