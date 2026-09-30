package nl.juiced.guhs.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import nl.juiced.guhs.registry.ModEntities;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * The guh bee: a fluffy pink bee with guh eyes. Collects pollen like a normal bee and fills a knabbelkorf (or a
 * beehive), but never gets angry and never stings.
 */
public class GuhBeeEntity extends Bee implements GeoEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.guh_bee.fly");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuhBeeEntity(EntityType<? extends Bee> type, Level level) {
        super(type, level);
    }

    // --- made during world generation ---------------------------------------------------------------------------------
    /**
     * Vanilla bee goals use the world's random in their constructors. Bees are normally never made during world
     * generation, but guh bees are (they spawn with new chunks), on a worldgen thread: using the world's random there
     * crashes the server ("Accessing LegacyRandomSource from multiple threads"). So off the server thread the goals
     * are set up on the bee's first tick instead. (No initializer: registerGoals runs inside the super constructor.)
     */
    private boolean goalsPending;

    @Override
    protected void registerGoals() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server && !server.getServer().isSameThread()) {
            goalsPending = true;
            return;
        }
        super.registerGoals();
    }

    @Override
    public void tick() {
        if (goalsPending && !this.level().isClientSide()) {
            goalsPending = false;
            super.registerGoals();
        }
        super.tick();
    }

    // --- never angry ------------------------------------------------------------------------------------------------

    @Override
    public boolean isAngry() {
        return false;
    }

    /** 26.1: anger is an end time now (was setRemainingPersistentAngerTime): never angry. */
    @Override
    public void setPersistentAngerEndTime(long endTime) {
        super.setPersistentAngerEndTime(-1L);
    }

    @Override
    public void startPersistentAngerTimer() {
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(null);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, Entity target) {
        return false;
    }

    @Nullable
    @Override
    public Bee getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.GUH_BEE.get().create(level, EntitySpawnReason.TRIGGERED);
    }

    /** Anywhere with room to fly: the Guhmension has no grass for vanilla bee rules. */
    public static boolean checkGuhBeeSpawnRules(EntityType<GuhBeeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isSolid() && level.getRawBrightness(pos, 0) > 8;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("fly", 0, state -> state.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
