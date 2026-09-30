package nl.juiced.guhs.feature.tuintjes;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * A tamed guh looks after the tuintjes around it: now and then it looks for a thirsty plant within {@link #BEREIK} blocks,
 * trots over and waters it (a little arc of gieterdruppels from its snoet to the plant). Not while sitting, riding or busy
 * with something else (GuhHooks.isBezig); the owner's Knus tab counts it.
 */
public class GuhGietGoal extends Goal {
    public static final int BEREIK = 8;
    private final GuhEntity guh;
    @Nullable
    private BlockPos plant;
    private long volgendeKeer;
    private int ticks;

    public GuhGietGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long now = guh.level().getGameTime();
        if (now < volgendeKeer) {
            return false;
        }
        volgendeKeer = now + 60 + guh.getRandom().nextInt(60);
        if (!guh.isTame() || guh.isOrderedToSit() || guh.isVehicle() || guh.isPassenger() || GuhHooks.isBezig(guh)) {
            return false;
        }
        plant = vindDorstig(guh.level(), guh.blockPosition(), BEREIK);
        return plant != null;
    }

    /** The nearest thirsty plant, or null. */
    @Nullable
    public static BlockPos vindDorstig(Level level, BlockPos from, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-r, -3, -r), from.offset(r, 3, r))) {
            if (TuinBlock.dorstig(level.getBlockState(p))) {
                double d = p.distSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        return plant != null && ticks < 200 && !guh.isOrderedToSit() && TuinBlock.dorstig(guh.level().getBlockState(plant));
    }

    @Override
    public void start() {
        ticks = 0;
        guh.getNavigation().moveTo(plant.getX() + 0.5, plant.getY() + 1, plant.getZ() + 0.5, 1.0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (plant == null) {
            return;                                            // (done: running goals tick once more before they stop)
        }
        ticks++;
        guh.getLookControl().setLookAt(plant.getX() + 0.5, plant.getY() + 0.8, plant.getZ() + 0.5);
        double dx = plant.getX() + 0.5 - guh.getX(), dz = plant.getZ() + 0.5 - guh.getZ();
        if (dx * dx + dz * dz <= 2.4 * 2.4 && Math.abs(plant.getY() - guh.getY()) <= 2.5) {
            giet();
            plant = null;
        } else if (ticks % 20 == 0) {
            guh.getNavigation().moveTo(plant.getX() + 0.5, plant.getY() + 1, plant.getZ() + 0.5, 1.0);
        }
    }

    private void giet() {
        guh.getNavigation().stop();
        if (!(guh.level() instanceof ServerLevel level) || !TuinBlock.water(level, plant)) {
            return;
        }
        // a little arc of drops from its snoet to the plant
        double sx = guh.getX(), sy = guh.getEyeY(), sz = guh.getZ();
        double ex = plant.getX() + 0.5, ey = plant.getY() + 1.0, ez = plant.getZ() + 0.5;
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            level.sendParticles(TuintjesFeature.GIETERDRUPPEL.get(), sx + (ex - sx) * t, sy + (ey - sy) * t + Math.sin(t * Math.PI) * 0.6,
                    sz + (ez - sz) * t, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.playSound(null, plant, TuintjesFeature.GIETER_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        GuhHooks.bezig(guh, 30);
        if (guh.getOwner() instanceof ServerPlayer owner) {
            TuintjesVoortgang.guhGoot(owner);
        }
    }

    @Override
    public void stop() {
        plant = null;
    }
}
