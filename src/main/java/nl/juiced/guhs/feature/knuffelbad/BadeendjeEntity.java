package nl.juiced.guhs.feature.knuffelbad;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A rubber duck. On a slide (a ride's duck): it floats still on its spot until the rider picks it up with the ring (or
 * the ride is over); never saved. As decoration (in the Knuffelbad's pools, "Deco"): it bobs on the water, drifts a
 * little, and squeaks when you poke it. The kind (plain or one of the twelve special ducks) is {@link Eendsoort}.
 */
public class BadeendjeEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Byte> DATA_SOORT = SynchedEntityData.defineId(BadeendjeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_RIT = SynchedEntityData.defineId(BadeendjeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** The ride it belongs to (server), or null: a decoration duck. */
    @Nullable
    private UUID rit;
    /** Client: the rider's own game already picked it up (it hides until the server takes it away). */
    public boolean lokaalGepakt;

    public BadeendjeEntity(EntityType<? extends BadeendjeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SOORT, (byte) 0);
        builder.define(DATA_RIT, false);
    }

    public Eendsoort getSoort() {
        return Eendsoort.byIndex(entityData.get(DATA_SOORT));
    }

    public void setSoort(Eendsoort soort) {
        entityData.set(DATA_SOORT, (byte) soort.ordinal());
    }

    /** A duck on a slide (for a ride), not a decoration. */
    public boolean vanRit() {
        return entityData.get(DATA_RIT);
    }

    public void setRit(@Nullable UUID rit) {
        this.rit = rit;
        entityData.set(DATA_RIT, rit != null);
        this.noPhysics = rit != null;
        setNoGravity(rit != null);
    }

    @Nullable
    public UUID getRit() {
        return rit;
    }

    @Override
    public void tick() {
        super.tick();
        if (vanRit()) {
            if (!level().isClientSide && (rit == null || !GlijRit.bestaat(rit)) && tickCount > 40) {
                discard();                                    // (its ride is over)
            }
            return;
        }
        // a decoration duck: floats on the water, drifts a bit
        Vec3 v = getDeltaMovement();
        if (isInWater() || level().getFluidState(blockPosition()).is(FluidTags.WATER)) {
            double surface = blockPosition().getY() + level().getFluidState(blockPosition()).getHeight(level(), blockPosition());
            double lift = (surface - 0.08 - getY()) * 0.2;
            v = new Vec3(v.x * 0.9, Math.max(-0.05, Math.min(0.06, v.y * 0.6 + lift)), v.z * 0.9);
            if (!level().isClientSide && random.nextInt(80) == 0) {
                v = v.add((random.nextDouble() - 0.5) * 0.04, 0, (random.nextDouble() - 0.5) * 0.04);
                setYRot(getYRot() + (random.nextFloat() - 0.5f) * 40);
            }
        } else if (!onGround()) {
            v = v.add(0, -0.04, 0).scale(0.98);
        } else {
            v = v.multiply(0.5, 0, 0.5);
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (vanRit()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            playSound(KnuffelbadFeature.EENDJE_PIEP.get(), 1f, 0.9f + random.nextFloat() * 0.4f);
            setDeltaMovement(getDeltaMovement().add(0, 0.25, 0));
            ((ServerLevel) level()).sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), getX(), getY() + 0.4, getZ(), 4, 0.15, 0.1, 0.15, 0.01);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || vanRit()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            discard();
            return true;
        }
        playSound(KnuffelbadFeature.EENDJE_PIEP.get(), 1f, 1.3f);
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved() && !vanRit();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !vanRit() && super.shouldBeSaved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSoort(Eendsoort.byIndex(tag.getByte("Soort")));
        setRit(null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Soort", (byte) getSoort().ordinal());
        tag.putBoolean("Deco", true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
