package nl.juiced.guhs.feature.slaapzorg;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * At night a guh looks for a sleeping spot of {@link Slapen#SLAAPPLEKKEN} close by (a guh bed, a box with knabbels, a cat
 * house...; one guh per spot), walks there, curls up in it and sleeps in: through the whole morning, so you can bring it
 * breakfast in bed ({@link OntbijtOpBed}). It wakes up by itself when the day starts (a big yawn), when it is hurt, or when
 * its owner walks away. Like the nest (which has its own goal): never required, and residents of a Guhhuisje sleep at home.
 */
public class SlaapplekGoal extends Goal {
    /** How far a guh looks for a spot (sideways, and up or down). */
    public static final int ZOEK = 12, ZOEK_DY = 3;
    /** (Game tests) guhs for which it is always bedtime. */
    public static final Set<UUID> TEST_NACHT = ConcurrentHashMap.newKeySet();
    private static final String GEWEKT_TOT = "guhs_slaapzorg_gewekt_tot";

    private final GuhEntity guh;
    @Nullable
    private BlockPos plek;
    private boolean slaapt;
    private int cooldown, lopen;

    public SlaapplekGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    /** Is this guh asleep in a sleeping spot right now? */
    public static boolean slaaptInPlek(GuhEntity guh) {
        return guh.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.isRunning() && w.getGoal() instanceof SlaapplekGoal g && g.slaapt);
    }

    /** Wakes it (breakfast, the command): no spot for it again until the next night. */
    static void wek(GuhEntity guh) {
        guh.getPersistentData().putLong(GEWEKT_TOT, guh.level().getGameTime() + 20 * 60 * 5);
    }

    /** Bedtime: night, and it sleeps in through the morning. */
    private boolean bedtijd(boolean alSlapend) {
        if (TEST_NACHT.contains(guh.getUUID())) {
            return true;
        }
        Dagdeel d = Slapen.dagdeel(guh);
        return d == Dagdeel.NACHT || (alSlapend && d == Dagdeel.OCHTEND);
    }

    private boolean mag(boolean alSlapend) {
        if (guh.isOrderedToSit() || guh.isPassenger() || guh.isVehicle() || guh.isLeashed() || !guh.mayWander() || guh.getTarget() != null
                || guh.isInLove() || guh.getHiddenBy() != null || !bedtijd(alSlapend) || Slapen.isIngezongen(guh)
                || guh.getPersistentData().getLongOr(GEWEKT_TOT, 0L) > guh.level().getGameTime()) {
            return false;
        }
        if (nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(guh) || GuhHooks.isBewoner(guh)) {
            return false;   // residents sleep at home
        }
        return !Slapen.baasWeg(guh);
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0 || !mag(false)) {
            return false;
        }
        cooldown = 100 + guh.getRandom().nextInt(100);
        if (!(guh.level() instanceof ServerLevel level) || (!TEST_NACHT.contains(guh.getUUID())
                && !level.hasNearbyAlivePlayer(guh.getX(), guh.getY(), guh.getZ(), 48))) {
            return false;   // (nobody around to see it: it sleeps where it is, cheaply)
        }
        plek = zoekPlek(level);
        return plek != null;
    }

    @Override
    public boolean canContinueToUse() {
        return plek != null && mag(slaapt) && Slapen.isSlaapplek(guh.level().getBlockState(plek)) && lopen < 400 && guh.hurtTime == 0;
    }

    @Override
    public void start() {
        lopen = 0;
        slaapt = false;
        guh.getPersistentData().putLong(Slapen.PLEK, plek.asLong());
        guh.getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.0);
    }

    @Override
    public void stop() {
        boolean wasSlapend = slaapt;
        slaapt = false;
        guh.getPersistentData().remove(Slapen.PLEK);
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
        if (!guh.isOrderedToSit()) {
            guh.setInSittingPose(false);
        }
        if (wasSlapend && Slapen.dagdeel(guh) == Dagdeel.DAG) {
            guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);   // slept in: good morning!
        }
        plek = null;
        cooldown = 100;
        guh.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (plek == null) {
            return;
        }
        double cx = plek.getX() + 0.5, cz = plek.getZ() + 0.5, cy = Slapen.ligHoogte(guh.level(), plek);
        double dx = guh.getX() - cx, dz = guh.getZ() - cz, flat = Math.sqrt(dx * dx + dz * dz);
        if (!slaapt) {
            if (flat < 0.7 && Math.abs(guh.getY() - cy) < 1.0) {
                inslapen(cx, cy, cz);
                return;
            }
            lopen++;
            if (flat < 1.0 && cy - guh.getY() > 0.6) {
                inslapen(cx, cy, cz);   // (the upper bunk of a Stapelbedje: it climbs the little ladder)
                return;
            }
            if (flat < 1.8) {
                guh.getMoveControl().setWantedPosition(cx, cy, cz, 0.7);
                if (guh.onGround() && cy > guh.getY() + 0.2) {
                    guh.getJumpControl().jump();   // (into the box / onto the bed)
                }
            } else if (lopen % 15 == 0 || guh.getNavigation().isDone()) {
                guh.getNavigation().moveTo(cx, plek.getY(), cz, 1.0);
            }
            return;
        }
        guh.getNavigation().stop();
        if (!guh.isInSittingPose()) {
            guh.setInSittingPose(true);
        }
        if (flat > 0.35) {
            guh.snapTo(cx, Math.max(guh.getY(), cy), cz, guh.getYRot(), guh.getXRot());
        }
        if (guh.level() instanceof ServerLevel level) {
            if (guh.tickCount % 50 == 0) {
                level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.15, 0.05, 0.15, 0.0);
            }
            if (guh.tickCount % 100 == 0 && guh.getHealth() < guh.getMaxHealth()) {
                guh.heal(guh.isTame() ? 20f : 1f);
            }
            if (guh.isBaby() && guh.tickCount % 200 == 0) {
                guh.ageUp(10);
            }
        }
    }

    private void inslapen(double cx, double cy, double cz) {
        slaapt = true;
        guh.getNavigation().stop();
        guh.snapTo(cx, cy, cz, guh.getYRot(), guh.getXRot());
        guh.setDeltaMovement(0, 0, 0);
        guh.setInSittingPose(true);
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
    }

    /** The nearest free sleeping spot (not the nest: that has its own goal). */
    @Nullable
    private BlockPos zoekPlek(ServerLevel level) {
        BlockPos hier = guh.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(hier.offset(-ZOEK, -ZOEK_DY, -ZOEK), hier.offset(ZOEK, ZOEK_DY, ZOEK))) {
            double d = p.distSqr(hier);
            BlockState s = level.getBlockState(p);
            if (d < bestD && Slapen.isSlaapplek(s) && (level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                    || nl.juiced.guhs.feature.bedden.StapelbedBlock.isOnder(s))   // (the lower bunk has the upper one right above it)
                    && !bezet(level, p)) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    /** Is another guh sleeping in (or walking to) this spot? */
    private boolean bezet(ServerLevel level, BlockPos p) {
        long key = p.asLong();
        List<GuhEntity> anderen = level.getEntitiesOfClass(GuhEntity.class, new AABB(p).inflate(ZOEK * 2),
                g -> g != guh && g.getPersistentData().getLongOr(Slapen.PLEK, Long.MIN_VALUE) == key);
        return !anderen.isEmpty();
    }
}
