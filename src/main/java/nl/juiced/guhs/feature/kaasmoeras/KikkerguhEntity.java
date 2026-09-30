package nl.juiced.guhs.feature.kaasmoeras;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * De kikkerguh: "een guh die een kikker is" (2.8: the real guh head with its round ears, glossy guh eyes, snoet and
 * blush on a frog's body, legs and throat pouch; tools/features/kaasmoeras.py), in three colours ({@link MotknabbelBlock.Kleur}: roze,
 * mint, geel). Always friendly and passive: it hops around the kaasmoeras, croaks, loves kaasknabbels (tempt and
 * breed) and snaps up kaasmotten with its long tongue: the mot becomes a glowing motknabbel in the kikkerguh's colour.
 */
public class KikkerguhEntity extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(KikkerguhEntity.class, EntityDataSerializers.INT);
    /** How far the tongue reaches, and the pause between two snaps. */
    public static final double TONGUE_RANGE = 5.0;
    public static final int SNAP_COOLDOWN = 100;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kikkerguh.idle");
    private static final RawAnimation HOP = RawAnimation.begin().thenLoop("animation.kikkerguh.hop");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.kikkerguh.swim");
    private static final RawAnimation CROAK = RawAnimation.begin().thenPlay("animation.kikkerguh.croak");
    private static final RawAnimation TONGUE = RawAnimation.begin().thenPlay("animation.kikkerguh.tongue");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int snapCooldown;

    public KikkerguhEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    /** Kikkerguhs spawn on the soggy ground of the kaasmoeras (and any grass or mud), in the light. */
    public static boolean checkKikkerguhSpawnRules(EntityType<KikkerguhEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos,
                                                   RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(KaasmoerasFeature.MODDERIG_KAASGRAS.get()) || below.is(KaasmoerasFeature.KAASMODDER.get())
                || below.is(BlockTags.DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
        return ground && (EntitySpawnReason.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
    }

    public MotknabbelBlock.Kleur getKleur() {
        return MotknabbelBlock.Kleur.byIndex(entityData.get(DATA_KLEUR));
    }

    public void setKleur(MotknabbelBlock.Kleur kleur) {
        entityData.set(DATA_KLEUR, kleur.ordinal());
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", getKleur().getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        for (MotknabbelBlock.Kleur k : MotknabbelBlock.Kleur.values()) {
            if (k.getSerializedName().equals(tag.getStringOr("Kleur", ""))) {
                setKleur(k);
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        if (spawnType != EntitySpawnReason.STRUCTURE) {
            setKleur(MotknabbelBlock.Kleur.byIndex(random.nextInt(3)));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new SnapKaasmotGoal());
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        KikkerguhEntity baby = KaasmoerasFeature.KIKKERGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baby != null) {
            baby.setKleur(other instanceof KikkerguhEntity o && random.nextBoolean() ? o.getKleur() : getKleur());
        }
        return baby;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && snapCooldown > 0) {
            snapCooldown--;
        }
    }

    // --- snapping up kaasmotten -----------------------------------------------------------------------------------------

    /** The nearest kaasmot it can see within tongue range, or null. */
    @Nullable
    public KaasmotEntity nearestMot() {
        List<KaasmotEntity> mots = level().getEntitiesOfClass(KaasmotEntity.class, getBoundingBox().inflate(TONGUE_RANGE),
                m -> m.isAlive() && distanceToSqr(m) <= TONGUE_RANGE * TONGUE_RANGE && hasLineOfSight(m));
        KaasmotEntity best = null;
        for (KaasmotEntity m : mots) {
            if (best == null || distanceToSqr(m) < distanceToSqr(best)) {
                best = m;
            }
        }
        return best;
    }

    /** Snap! The kaasmot is gone and a motknabbel in this kikkerguh's colour pops out. Returns the motknabbel. */
    public ItemEntity snap(KaasmotEntity mot) {
        snapCooldown = SNAP_COOLDOWN;
        triggerAnim("action", "tongue");
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FROG_TONGUE, SoundSource.NEUTRAL, 1f, 1.2f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FROG_EAT, SoundSource.NEUTRAL, 1f, 1.3f);
        mot.eaten();
        ItemEntity drop = new ItemEntity(level(), getX(), getY() + 0.4, getZ(), MotknabbelBlock.stack(getKleur(), 1));
        drop.setDefaultPickUpDelay();
        drop.setDeltaMovement((random.nextDouble() - 0.5) * 0.1, 0.25, (random.nextDouble() - 0.5) * 0.1);
        level().addFreshEntity(drop);
        return drop;
    }

    public boolean canSnap() {
        return snapCooldown <= 0 && !isBaby();
    }

    private class SnapKaasmotGoal extends Goal {
        private KaasmotEntity target;
        private int aimTicks;

        SnapKaasmotGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canSnap()) {
                return false;
            }
            target = nearestMot();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && aimTicks < 40 && distanceToSqr(target) <= TONGUE_RANGE * TONGUE_RANGE * 1.5;
        }

        @Override
        public void start() {
            aimTicks = 0;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;                                        // (1.1.1: snapped already: ticked once more before it stops)
            }
            getLookControl().setLookAt(target, 60f, 60f);
            target.pullTowards(KikkerguhEntity.this);
            if (++aimTicks >= 8 && target.isAlive()) {
                snap(target);
                target = null;
            }
        }

        @Override
        public void stop() {
            target = null;
        }
    }

    // --- sounds and looks --------------------------------------------------------------------------------------------

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("action", "croak");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.FROG_STEP, 0.15f, 1.2f);
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.6f : 1.25f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
        return super.calculateFallDamage(fallDistance, damageMultiplier) - 5;
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("move", 3, state -> {
            if (isInWater()) {
                return state.setAndContinue(SWIM);
            }
            return state.setAndContinue(state.isMoving() ? HOP : IDLE);
        }));
        controllers.add(new AnimationController<>("action", 1, state -> PlayState.STOP)
                .triggerableAnim("croak", CROAK).triggerableAnim("tongue", TONGUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
