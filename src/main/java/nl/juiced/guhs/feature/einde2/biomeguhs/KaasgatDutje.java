package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;

/**
 * einde2, slice biomeguhs: a tamed Korstguh naps in a kaasgat. Now and then, when it has nothing to do, it looks for a hole
 * close by (a spot one block deep with a wall on at least three sides: what the kaasgaten of the Gatenkaasheuvels leave,
 * or what you dig for it), walks into it, rolls up with its eyes shut and sleeps for a while.
 */
public class KaasgatDutje extends Goal {
    /** One in this many looks (a look every other tick) starts a nap: about once in three minutes of doing nothing. */
    public static final int KANS = 1800;
    /** How far it looks for a hole, how long it walks at most, how long a nap lasts (ticks, plus up to as much again). */
    public static final int ZOEK = 8, LOOP_TICKS = 200, DUTJE = 400;

    private final GuhEntity guh;
    @Nullable
    private BlockPos gat;
    private int loopt, slaapt;

    public KaasgatDutje(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean mag() {
        return guh.getVariant() == GuhVariant.KORSTGUH && Trucs.tam(guh) && !guh.isOrderedToSit() && !guh.isVehicle() && !guh.isPassenger()
                && !guh.isLeashed() && !guh.isBaby();
    }

    @Override
    public boolean canUse() {
        if (!mag() || Trucs.doet(guh) || GuhHooks.isBezig(guh) || guh.getRandom().nextInt(KANS) != 0) {
            return false;
        }
        gat = zoekGat(guh.level(), guh.blockPosition(), ZOEK);
        return gat != null;
    }

    @Override
    public void start() {
        loopt = LOOP_TICKS;
        slaapt = 0;
        guh.getNavigation().moveTo(gat.getX() + 0.5, gat.getY(), gat.getZ() + 0.5, 1.0);
    }

    @Override
    public boolean canContinueToUse() {
        return gat != null && mag() && (slaapt > 0 ? Korstbal.opgerold(guh) : loopt > 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (slaapt > 0) {
            if (--slaapt == 0) {
                gat = null;   // slept enough
            }
            return;
        }
        loopt--;
        if (guh.position().distanceToSqr(Vec3.atBottomCenterOf(gat)) < 0.8 * 0.8) {
            slaapt = DUTJE + guh.getRandom().nextInt(DUTJE);
            slaap(guh, slaapt);
        } else if (guh.getNavigation().isDone()) {
            guh.getNavigation().moveTo(gat.getX() + 0.5, gat.getY(), gat.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        if (slaapt > 0 || GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)) {
            Korstbal.rolUit(guh);
            GuhHooks.bezig(guh, 0);
        }
        gat = null;
        slaapt = 0;
    }

    /** Rolled up with its eyes shut for this many ticks (the day rhythm leaves it alone that long). */
    public static void slaap(GuhEntity guh, int ticks) {
        guh.getNavigation().stop();
        Korstbal.rolOp(guh, ticks);
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
        GuhHooks.bezig(guh, ticks);
    }

    /** The nearest kaasgat within {@code straal} blocks (2 up or down): the spot to lie in, or null. */
    @Nullable
    public static BlockPos zoekGat(Level level, BlockPos bij, int straal) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(bij.offset(-straal, -2, -straal), bij.offset(straal, 2, straal))) {
            double d = p.distSqr(bij);
            if (d < bestD && level.isLoaded(p) && isGat(level, p)) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    /** A hole a guh fits in: free here and above, a floor under it, a wall on at least three sides, open sky side up. */
    public static boolean isGat(Level level, BlockPos p) {
        if (!vrij(level, p) || !vrij(level, p.above()) || vrij(level, p.below()) || !level.getFluidState(p).isEmpty()) {
            return false;
        }
        int wanden = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!vrij(level, p.relative(d))) {
                wanden++;
            }
        }
        return wanden >= 3;
    }

    private static boolean vrij(Level level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }
}
