package nl.juiced.guhs.feature.droomwezens;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * Het sterrenvisje (1.8.0 Guhdromenland, bouwplan C): a tiny glowing fish with star-shaped fins, in four pastel colours
 * ({@link Kleur}). It swims in little schools in the maanmeertje of the Maanmolentje (placed there by the island; no natural
 * spawns of its own). Always lief: it never darts away from players, and a water bucket scoops one up (its colour stays).
 * Now and then it leaves a sparkle in the water.
 */
public class SterrenvisjeEntity extends AbstractSchoolingFish implements GeoEntity {
    public enum Kleur {
        GEEL, ROZE, LILA, MUNT;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Kleur van(String id) {
            for (Kleur k : values()) {
                if (k.id().equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(SterrenvisjeEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private static final RawAnimation SPARTEL = RawAnimation.begin().thenLoop("spartel");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SterrenvisjeEntity(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
    }

    public Kleur kleur() {
        Kleur[] alle = Kleur.values();
        return alle[Math.floorMod(entityData.get(DATA_KLEUR), alle.length)];
    }

    public void setKleur(Kleur kleur) {
        entityData.set(DATA_KLEUR, kleur.ordinal());
    }

    @Override
    public int getMaxSchoolSize() {
        return 6;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", kleur().id());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        Kleur k = Kleur.van(tag.getStringOr("Kleur", ""));
        if (k != null) {
            setKleur(k);
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reden, @Nullable SpawnGroupData data) {
        setKleur(Kleur.values()[random.nextInt(Kleur.values().length)]);   // (from a bucket: loadFromBucketTag puts its own back)
        return super.finalizeSpawn(level, difficulty, reden, data);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> g instanceof AvoidEntityGoal);   // lief: never darts away from players
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && isInWater() && random.nextInt(60) == 0) {
            server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.1, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    // --- the bucket ---------------------------------------------------------------------------------------------------------

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(DroomwezensFeature.STERRENVISJE_EMMER.get());
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.putString("Kleur", kleur().id()));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        Kleur k = Kleur.van(tag.getStringOr("Kleur", ""));
        if (k != null) {
            setKleur(k);
        }
    }

    // --- sounds -------------------------------------------------------------------------------------------------------------

    /** (droomsfeer) a tiny twinkly plink now and then: its own sound event, inspiration/Dromen audio/sterrenvisje */
    @Override
    protected SoundEvent getAmbientSound() {
        return nl.juiced.guhs.feature.droomsfeer.DroomGeluid.STERRENVISJE.get();
    }

    @Override
    protected float getSoundVolume() {
        return nl.juiced.guhs.feature.droomsfeer.DroomGeluid.STERRENVISJE.volume;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zwem", 4, state -> state.setAndContinue(isInWater() ? ZWEM : SPARTEL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** (Tests) a random colour from this source. */
    static Kleur willekeurig(RandomSource random) {
        return Kleur.values()[random.nextInt(Kleur.values().length)];
    }
}
