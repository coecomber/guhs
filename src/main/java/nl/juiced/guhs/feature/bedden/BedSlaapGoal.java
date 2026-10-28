package nl.juiced.guhs.feature.bedden;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * At night a tame critter (a guh, a Blub, a pieppiepmuisje, Schilly or Poepschilly) looks for a free vakje of its own kind in
 * a critter or family bed close by ({@link Vakjes}), walks there, snuggles into it and sleeps (zzz; a Blub sleeps its own Blub
 * way). It wakes up in the morning, when it is hurt, when the bed is broken or when its owner walks far away. Never required.
 */
public class BedSlaapGoal extends Goal {
    /** How far a critter looks for its vakje. */
    public static final int ZOEK = 12;
    /** (Game tests) critters for which it is always bedtime. */
    public static final Set<UUID> TEST_NACHT = ConcurrentHashMap.newKeySet();

    private final TamableAnimal mob;
    private final Vakjes.Soort soort;
    @Nullable
    private Vakjes.Plek plek;
    private boolean slaapt;
    private int cooldown, lopen;

    public BedSlaapGoal(TamableAnimal mob, Vakjes.Soort soort) {
        this.mob = mob;
        this.soort = soort;
        this.cooldown = 20 + mob.getRandom().nextInt(80);
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    /** Does this critter sleep in a vakje right now? */
    public static boolean slaaptInVakje(TamableAnimal mob) {
        return mob.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.isRunning() && w.getGoal() instanceof BedSlaapGoal g && g.slaapt);
    }

    private boolean nacht() {
        return TEST_NACHT.contains(mob.getUUID()) || mob.level().isDarkOutside();
    }

    private boolean mag() {
        if (!mob.isTame() || mob.isOrderedToSit() || mob.isPassenger() || mob.isVehicle() || mob.isLeashed() || mob.getTarget() != null
                || mob.isInLove() || mob.isInWater() || !nacht()) {
            return false;
        }
        if (mob instanceof GuhEntity guh && (nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(guh) || GuhHooks.isBewoner(guh))) {
            return false;   // residents sleep at home
        }
        LivingEntity baas = mob.getOwner();
        return TEST_NACHT.contains(mob.getUUID()) || (baas != null && baas.level() == mob.level() && baas.distanceTo(mob) < 24);
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) {
            return false;
        }
        cooldown = 100 + mob.getRandom().nextInt(100);
        if (!mag() || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<Vakjes.Plek> p = Vakjes.zoek(level, mob, soort, ZOEK);
        plek = p.orElse(null);
        return plek != null;
    }

    @Override
    public boolean canContinueToUse() {
        return plek != null && mag() && lopen < 400 && mob.hurtTime == 0 && mob.level() instanceof ServerLevel l && Vakjes.bestaat(l, plek.sleutel());
    }

    @Override
    public void start() {
        lopen = 0;
        slaapt = false;
        mob.getPersistentData().putString(Vakjes.VAKJE, plek.sleutel());
        mob.getNavigation().moveTo(plek.pos().x, plek.pos().y, plek.pos().z, 1.0);
    }

    @Override
    public void stop() {
        if (slaapt) {
            if (mob instanceof GuhEntity guh) {
                GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
            }
            if (mob instanceof BlubGuhEntity blub) {
                blub.setSlaapt(false);
            }
        }
        slaapt = false;
        mob.getPersistentData().remove(Vakjes.VAKJE);
        if (!mob.isOrderedToSit()) {
            mob.setInSittingPose(false);
        }
        plek = null;
        cooldown = 100;
        mob.getNavigation().stop();
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
        Vec3 doel = plek.pos();
        double dx = mob.getX() - doel.x, dz = mob.getZ() - doel.z, plat = Math.sqrt(dx * dx + dz * dz);
        if (!slaapt) {
            lopen++;
            // close by (or almost there and stuck at the bed's edge): it snuggles in
            if ((plat < 1.4 && Math.abs(mob.getY() - doel.y) < 1.6) || (plat < 3.0 && lopen > 200)) {
                inslapen();
            } else if (lopen % 20 == 0 || mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(doel.x, doel.y, doel.z, 1.0);
            }
            return;
        }
        mob.getNavigation().stop();
        if (plat > 0.25 || Math.abs(mob.getY() - doel.y) > 0.3) {
            mob.snapTo(doel.x, doel.y, doel.z, plek.yaw() + 180, 0);
        }
        mob.setDeltaMovement(Vec3.ZERO);
        if (!(mob instanceof BlubGuhEntity) && mob.level() instanceof ServerLevel level && mob.tickCount % 50 == 0) {
            level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(), 1, 0.12, 0.05, 0.12, 0.0);
        }
    }

    private void inslapen() {
        slaapt = true;
        Vec3 doel = plek.pos();
        mob.getNavigation().stop();
        mob.snapTo(doel.x, doel.y, doel.z, plek.yaw() + 180, 0);
        mob.setYHeadRot(plek.yaw() + 180);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setInSittingPose(true);
        if (mob instanceof GuhEntity guh) {
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
        }
        if (mob instanceof BlubGuhEntity blub) {
            blub.setSlaapt(true);
        }
    }
}
