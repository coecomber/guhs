package nl.juiced.guhs.feature.onderwol.wezens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * 1.7.0 "Onder de Wol": a glimwormpje of the Glitterspelonk. The little glow worm of the safari (its baby model and its
 * glowing tail) hanging on its thread from a cave ceiling, swaying a bit. Pure atmosphere: it does nothing, never moves
 * from its spot, and comes and goes with the players around (the top-up in {@link OnderWolWezens}). It drops when what it
 * hangs on is gone.
 */
public class GrotGlimwormEntity extends AmbientCreature implements GeoEntity {
    private static final RawAnimation HANG = RawAnimation.begin().thenLoop("hang");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GrotGlimwormEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0);
    }

    /** Is there a ceiling right above it to hang on? */
    public boolean hangt() {
        BlockPos boven = BlockPos.containing(getX(), getY() + getBbHeight() + 0.2, getZ());
        return level().getBlockState(boven).isFaceSturdy(level(), boven, Direction.DOWN);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 40 == 0 && isNoGravity() && !hangt()) {
            setNoGravity(false);                               // (its ceiling is gone: it drops)
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoGravity()) {
            setDeltaMovement(0, 0, 0);
        }
        if (level().isClientSide() && random.nextInt(20) == 0) {
            level().addParticle(ParticleTypes.GLOW, getX(), getY(), getZ(), 0, -0.01, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasCustomName();
    }

    @Override
    public boolean shouldBeSaved() {
        return hasCustomName() && super.shouldBeSaved();       // (a wild one is never saved: the top-up brings new ones)
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;                                            // (it glows by itself)
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("hang", 0, state -> state.setAndContinue(HANG)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
