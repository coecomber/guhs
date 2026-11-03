package nl.juiced.guhs.feature.vadsrijk.wezens.draak;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Vahoegnir, de Vadsloze Draak: zone 1's NON-combat flyover entity (design 9: once the pink protector dragon with feather
 * wings and guh ears as horns, now grey-purple and crackling with black fluff). It has no AI, no hitbox to hit and is never
 * saved: {@link DraakVlucht} moves it along its path every server tick and removes it at the end. The client draws the big
 * GeckoLib model (x2.5), flaps or glides depending on whether it climbs, and rains grey fluff below it.
 */
public class Vahoegnir extends Entity implements GeoEntity {
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("vlieg"), ZWEEF = RawAnimation.begin().thenLoop("zweef"),
            BRUL = RawAnimation.begin().thenPlay("brul");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: smoothed climb rate (flap while climbing or level, glide while sinking). */
    private double klim;

    public Vahoegnir(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            klim = klim * 0.9 + (getY() - yo) * 0.1;
            for (int i = 0; i < 3; i++) {     // grey fluff rains from the dragon, black fluff crackles on its back
                double rx = (random.nextDouble() - 0.5) * 10, rz = (random.nextDouble() - 0.5) * 10;
                level().addAlwaysVisibleParticle(new DustParticleOptions(i == 0 ? 0x3A2C44 : 0x8A7898, 3.0f), getX() + rx, getY() + random.nextDouble() * 3,
                        getZ() + rz, 0, -0.08, 0);
            }
            if (random.nextInt(2) == 0) {
                level().addAlwaysVisibleParticle(ParticleTypes.WHITE_ASH, getX() + (random.nextDouble() - 0.5) * 14, getY() - 1, getZ() + (random.nextDouble() - 0.5) * 14,
                        0, -0.1, 0);
            }
            if (random.nextInt(4) == 0) {
                level().addAlwaysVisibleParticle(ParticleTypes.WITCH, getX() + (random.nextDouble() - 0.5) * 6, getY() + 4, getZ() + (random.nextDouble() - 0.5) * 6,
                        0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 6, getY() + 3,
                        getZ() + (random.nextDouble() - 0.5) * 6, 0, 0.02, 0);
            }
        }
    }

    public boolean klimt() {
        return klim > -0.05;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 640 * 640;
    }

    /** The renderer culls with this (the model is far bigger than the entity box). */
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(20, 8, 20);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Vahoegnir>("vlieg", 10, s -> s.setAndContinue(s.animatable().klimt() ? VLIEG : ZWEEF)));
        controllers.add(new AnimationController<Vahoegnir>("brul", 4, s -> PlayState.STOP).triggerableAnim("brul", BRUL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
