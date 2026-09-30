package nl.juiced.guhs.feature.spiesburcht;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * De Vonk-Mika: the blaze of the Barbecuether. A glowing Mika head in a whirl of grillspiesjes, hovering in the
 * Spiesburcht (its spawner) and now and then in the Rookdelta. Throws three gloeiende kooltjes, then catches its breath.
 * Drops grillspiesen (for grillspiespoeder, the fuel of the Guhbrouwketel). Water makes it sizzle.
 */
public class VonkMikaEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_CHARGED = SynchedEntityData.defineId(VonkMikaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vonk_mika.idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("animation.vonk_mika.shoot");
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.1f), 0.9f);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private float allowedHeightOffset = 0.5f;
    private int nextHeightOffsetChangeTick;

    public VonkMikaEntity(EntityType<? extends VonkMikaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0f);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.LAVA, 8.0f);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE, 0.0f);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DAMAGE_FIRE, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new KooltjesGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isCharged() {
        return this.entityData.get(DATA_CHARGED);
    }

    void setCharged(boolean charged) {
        this.entityData.set(DATA_CHARGED, charged);
    }

    @Override
    public void aiStep() {
        if (!this.onGround() && this.getDeltaMovement().y < 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }
        if (this.level().isClientSide()) {
            if (this.random.nextInt(24) == 0 && !this.isSilent()) {
                this.level().playLocalSound(getX() + 0.5, getY() + 0.5, getZ() + 0.5, SoundEvents.BLAZE_BURN, getSoundSource(),
                        1.0f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f, false);
            }
            this.level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(SPARK, getRandomX(0.6), getY() + random.nextDouble() * 1.2, getRandomZ(0.6), 0, 0.05, 0);
            }
            if (isCharged() && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.FLAME, getRandomX(0.4), getY(0.8), getRandomZ(0.4), 0, 0.02, 0);
            }
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if (--this.nextHeightOffsetChangeTick <= 0) {
            this.nextHeightOffsetChangeTick = 100;
            this.allowedHeightOffset = (float) this.random.triangle(0.5, 6.891);
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.getEyeY() > this.getEyeY() + this.allowedHeightOffset && this.canAttack(target)) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.add(0.0, (0.3 - v.y) * 0.3, 0.0));
            this.hasImpulse = true;
        }
        super.customServerAiStep();
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIKA_AMBIENT.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + (random.nextFloat() - random.nextFloat()) * 0.1f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIKA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP).triggerableAnim("shoot", SHOOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Three kooltjes in a row, then a long breath (the blaze's attack). Up close it bites. */
    static class KooltjesGoal extends Goal {
        private final VonkMikaEntity mika;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        KooltjesGoal(VonkMikaEntity mika) {
            this.mika = mika;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mika.getTarget();
            return target != null && target.isAlive() && mika.canAttack(target);
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            mika.setCharged(false);
            this.lastSeen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.attackTime--;
            LivingEntity target = mika.getTarget();
            if (target == null) {
                return;
            }
            boolean see = mika.getSensing().hasLineOfSight(target);
            this.lastSeen = see ? 0 : this.lastSeen + 1;
            double dist = mika.distanceToSqr(target);
            if (dist < 4.0) {
                if (!see) {
                    return;
                }
                if (this.attackTime <= 0) {
                    this.attackTime = 20;
                    mika.doHurtTarget(target);
                }
                mika.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            } else if (dist < 40 * 40 && see) {
                double dx = target.getX() - mika.getX();
                double dy = target.getY(0.5) - mika.getY(0.5);
                double dz = target.getZ() - mika.getZ();
                if (this.attackTime <= 0) {
                    this.attackStep++;
                    if (this.attackStep == 1) {
                        this.attackTime = 60;
                        mika.setCharged(true);
                    } else if (this.attackStep <= 4) {
                        this.attackTime = 6;
                    } else {
                        this.attackTime = 100;
                        this.attackStep = 0;
                        mika.setCharged(false);
                    }
                    if (this.attackStep > 1) {
                        double spread = Math.sqrt(Math.sqrt(dist)) * 0.5;
                        mika.playSound(SoundEvents.BLAZE_SHOOT, 1.0f, 1.3f);
                        mika.triggerAnim("action", "shoot");
                        Vec3 dir = new Vec3(mika.getRandom().triangle(dx, 2.297 * spread), dy, mika.getRandom().triangle(dz, 2.297 * spread));
                        GloeiendKooltje kooltje = new GloeiendKooltje(SpiesburchtFeature.GLOEIEND_KOOLTJE.get(), mika, dir.normalize(), mika.level());
                        kooltje.setPos(kooltje.getX(), mika.getY(0.5) + 0.5, kooltje.getZ());
                        mika.level().addFreshEntity(kooltje);
                    }
                }
                mika.getLookControl().setLookAt(target, 10.0f, 10.0f);
            } else if (this.lastSeen < 5) {
                mika.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            }
        }
    }
}
