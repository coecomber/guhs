package nl.juiced.guhs.feature.slaapzorg;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * A tame guh near a krabpaal now and then hops on top and sits there a while, looking around like a cat on its post
 * (and waves at you from up there). Only by day, only when it is free (not sitting on command, not busy, no emote).
 */
public class KrabpaalGoal extends Goal {
    public static final int ZOEK = 6;
    /** One in this many checks (every 2-4 seconds) it goes. */
    public static final int KANS = 40;
    /** (Game tests) guhs that always go. */
    public static final java.util.Set<java.util.UUID> TEST_ALTIJD = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final GuhEntity guh;
    @Nullable
    private BlockPos paal;
    private int cooldown, lopen, zitten;
    private boolean boven;

    public KrabpaalGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    private boolean vrij() {
        Dagdeel d = Slapen.dagdeel(guh);
        return guh.isTame() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle() && !guh.isLeashed() && guh.getTarget() == null
                && !GuhHooks.isBezig(guh) && !Slapen.slaapt(guh) && (d == Dagdeel.DAG || d == Dagdeel.DUTJE || d == Dagdeel.OCHTEND)
                && !Slapen.baasWeg(guh);
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 40 + guh.getRandom().nextInt(40);
        if (!vrij() || guh.emotes.current() != null || (!TEST_ALTIJD.contains(guh.getUUID()) && guh.getRandom().nextInt(KANS) != 0)
                || !(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        paal = zoek(level);
        return paal != null;
    }

    @Override
    public boolean canContinueToUse() {
        return paal != null && vrij() && SlaapzorgFeature.isKrabpaal(guh.level().getBlockState(paal)) && lopen < 300 && zitten > 0
                && guh.hurtTime == 0;
    }

    @Override
    public void start() {
        lopen = 0;
        boven = false;
        zitten = 400 + guh.getRandom().nextInt(400);
        guh.getNavigation().moveTo(paal.getX() + 0.5, paal.getY(), paal.getZ() + 0.5, 1.0);
    }

    @Override
    public void stop() {
        if (boven && !guh.isOrderedToSit()) {
            guh.setInSittingPose(false);
        }
        boven = false;
        paal = null;
        cooldown = 600;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        double cx = paal.getX() + 0.5, cz = paal.getZ() + 0.5, top = paal.getY() + 1.0;
        if (!boven) {
            lopen++;
            double dx = guh.getX() - cx, dz = guh.getZ() - cz;
            if (dx * dx + dz * dz < 2.6) {
                hupOp(cx, top, cz);
            } else if (lopen % 15 == 0 || guh.getNavigation().isDone()) {
                guh.getNavigation().moveTo(cx, paal.getY(), cz, 1.0);
            }
            return;
        }
        zitten--;
        guh.getNavigation().stop();
        if (Math.abs(guh.getX() - cx) > 0.3 || Math.abs(guh.getZ() - cz) > 0.3 || guh.getY() < top - 0.2) {
            guh.snapTo(cx, top, cz, guh.getYRot(), guh.getXRot());
        }
        if (zitten == 120 && guh.getOwner() != null) {
            guh.emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF);
        }
    }

    /** Hups! up onto the little platform. */
    private void hupOp(double cx, double top, double cz) {
        boven = true;
        guh.getNavigation().stop();
        guh.snapTo(cx, top, cz, guh.getYRot(), guh.getXRot());
        guh.setDeltaMovement(0, 0.2, 0);
        guh.setInSittingPose(true);
        if (guh.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, cx, top, cz, 4, 0.2, 0.05, 0.2, 0.01);
        }
    }

    @Nullable
    private BlockPos zoek(ServerLevel level) {
        BlockPos hier = guh.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(hier.offset(-ZOEK, -2, -ZOEK), hier.offset(ZOEK, 2, ZOEK))) {
            if (SlaapzorgFeature.isKrabpaal(level.getBlockState(p)) && level.getBlockState(p.above()).isAir()
                    && level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(p.above())).isEmpty()) {
                return p.immutable();
            }
        }
        return null;
    }
}
