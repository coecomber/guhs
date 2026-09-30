package nl.juiced.guhs.feature.vadswoud;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * At night a guh crawls into a guh nest nearby and sleeps there (Zzz), with its family: several guhs fit in one nest.
 * Wild guhs and tame ones (as long as their owner is close: a tame guh never stays behind when you walk away). It
 * wakes up in the morning, when hurt, or when it's told to sit. Sleeping heals, and babies grow a little.
 */
public class SleepInNestGoal extends Goal {
    /** How far a guh looks for a nest. */
    public static final int SEARCH = 20;
    /** How many guhs sleep in one nest. */
    public static final int PER_NEST = 6;
    /** A tame guh only goes to bed while its owner is this close. */
    public static final double OWNER_NEAR = 24;
    /** Where a guh is going to sleep / sleeping (persistent data, a BlockPos as long). */
    public static final String NEST = "GuhsNestje";
    /** Guhs for which it is always night (the game tests: the test level's clock is shared). */
    public static final Set<UUID> TEST_NIGHT = ConcurrentHashMap.newKeySet();
    /** KnusVlaggen bit: asleep in a nest, so the renderer closes its eyes (the SLAPEN emote does that too). */
    public static final int OOGJES_DICHT = 1 << 12;

    private final GuhEntity guh;
    @Nullable
    private BlockPos nest;
    private boolean asleep;
    private int cooldown;
    private int walking;

    public SleepInNestGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    /** Bedtime: night in a world with a day and a night. */
    public static boolean isNight(GuhEntity guh) {
        if (TEST_NIGHT.contains(guh.getUUID())) {
            return true;
        }
        Level level = guh.level();
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long t = nl.juiced.guhs.world.GuhTime.timeOfDay(level);
        return t >= 12600 && t < 23300;
    }

    /** Is this guh asleep in a nest right now? */
    public static boolean isAsleep(GuhEntity guh) {
        return guh.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.isRunning() && w.getGoal() instanceof SleepInNestGoal g && g.asleep);
    }

    private boolean mayGo() {
        if (guh.isOrderedToSit() || guh.isPassenger() || guh.isVehicle() || guh.isLeashed() || !guh.mayWander() || guh.getTarget() != null
                || guh.isInLove() || guh.getHiddenBy() != null || !isNight(guh)) {
            return false;
        }
        if (nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(guh)) {
            return false;   // 2.10: a Guhhuisje resident sleeps in its own huisje
        }
        if (guh.isTame()) {
            LivingEntity owner = guh.getOwner();
            return owner != null && owner.level() == guh.level() && owner.distanceTo(guh) < OWNER_NEAR;
        }
        return true;
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0 || !mayGo()) {
            return false;
        }
        cooldown = 40 + guh.getRandom().nextInt(40);
        nest = findNest();
        return nest != null;
    }

    @Override
    public boolean canContinueToUse() {
        return nest != null && mayGo() && guh.level().getBlockState(nest).is(VadswoudFeature.GUHNESTJE.get()) && walking < 400;
    }

    @Override
    public void start() {
        walking = 0;
        asleep = false;
        guh.getPersistentData().putLong(NEST, nest.asLong());
        guh.getNavigation().moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, 1.0);
    }

    @Override
    public void stop() {
        if (asleep && !guh.isOrderedToSit()) {
            guh.setInSittingPose(false);
        }
        asleep = false;
        GuhHooks.zet(guh, OOGJES_DICHT, false);
        nest = null;
        cooldown = 100;
        guh.getPersistentData().remove(NEST);
        guh.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (nest == null) {
            return;
        }
        double cx = nest.getX() + 0.5, cz = nest.getZ() + 0.5;
        double dx = guh.getX() - cx, dz = guh.getZ() - cz;
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean there = flat < 0.75 && Math.abs(guh.getY() - nest.getY()) < 0.9;
        if (!asleep) {
            if (there) {
                fallAsleep();
                return;
            }
            walking++;
            if (flat < 1.8) {
                guh.getMoveControl().setWantedPosition(cx, nest.getY() + 0.2, cz, 0.7);   // the last step, right into the bowl
            } else if (walking % 15 == 0 || guh.getNavigation().isDone()) {
                guh.getNavigation().moveTo(cx, nest.getY(), cz, 1.0);
            }
            return;
        }
        guh.getNavigation().stop();
        if (!guh.isInSittingPose()) {
            guh.setInSittingPose(true);
        }
        if (flat > 0.6) {
            guh.getMoveControl().setWantedPosition(cx, nest.getY() + 0.2, cz, 0.4);
        }
        if (guh.level() instanceof ServerLevel level) {
            if (guh.tickCount % 50 == 0) {
                level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.15, 0.05, 0.15, 0.0);
            }
            if (guh.tickCount % 100 == 0 && guh.getHealth() < guh.getMaxHealth()) {
                guh.heal(guh.isTame() ? 20f : 1f);
            }
            if (guh.isBaby() && guh.tickCount % 200 == 0) {
                guh.ageUp(10);   // (babies grow in their sleep)
            }
        }
    }

    private void fallAsleep() {
        asleep = true;
        GuhHooks.zet(guh, OOGJES_DICHT, true);
        guh.getNavigation().stop();
        guh.setInSittingPose(true);
        if (guh.isTame() && guh.getOwner() instanceof ServerPlayer owner) {
            VadsAdvancements.grant(owner, "vadswoud_geslapen");
            VadsAdvancements.award(owner, "guhmension/vadswoud_nestje");
            owner.sendOverlayMessage(Component.translatable("gui.guhs.vadswoud.slaapt", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The nearest guh nest within {@link #SEARCH} blocks that isn't full yet. */
    @Nullable
    private BlockPos findNest() {
        if (!(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        PoiManager poi = level.getPoiManager();
        BlockPos here = guh.blockPosition();
        Optional<BlockPos> found = poi.findAll(t -> t.is(VadswoudFeature.NEST_POI.getKey()), p -> true, here, SEARCH, PoiManager.Occupancy.ANY)
                .filter(p -> level.getBlockState(p).is(VadswoudFeature.GUHNESTJE.get()) && sleepers(level, p) < PER_NEST)
                .min(Comparator.comparingDouble(p -> p.distSqr(here)));
        return found.orElse(null);
    }

    /** How many guhs (other than this one) sleep in, or are on their way to, this nest. */
    private int sleepers(ServerLevel level, BlockPos nest) {
        long key = nest.asLong();
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(nest).inflate(SEARCH),
                g -> g != guh && g.getPersistentData().contains(NEST) && g.getPersistentData().getLongOr(NEST, 0L) == key).size();
    }
}
