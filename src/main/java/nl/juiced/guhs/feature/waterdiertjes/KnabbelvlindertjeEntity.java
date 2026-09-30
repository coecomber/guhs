package nl.juiced.guhs.feature.waterdiertjes;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

/**
 * Het knabbelvlindertje (3.0, DESIGN_30 §6): a butterfly with a tiny guh head and big wings with knabbel dots and a
 * guh-head spot, in four colours ({@link Kleur}). By day it flutters from flower to flower (it lands on them and slowly
 * opens and closes its wings); at night it sleeps on a flower.
 */
public class KnabbelvlindertjeEntity extends FladderDiertje {
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(KnabbelvlindertjeEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");
    /** How far it looks for flowers. */
    public static final int BLOEM_BEREIK = 8;

    public enum Kleur {
        KAASGEEL, ROZE, MINT, LILA;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kleur van(int i) {
            Kleur[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    public KnabbelvlindertjeEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    /** By day, in the open air close to flowers; never too many together. */
    public static boolean checkSpawn(EntityType<KnabbelvlindertjeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE || spawnType == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnType == EntitySpawnReason.COMMAND) {
            return true;
        }
        if (!(level instanceof Level l) || !l.isDay() || !level.getBlockState(pos).isAir() || !level.canSeeSky(pos)) {
            return false;
        }
        return bloemIn(level, pos, 5) && level.getEntitiesOfClass(KnabbelvlindertjeEntity.class, new AABB(pos).inflate(24)).size() < 6;
    }

    static boolean bloemIn(LevelAccessor level, BlockPos pos, int r) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -3, -r), pos.offset(r, 1, r))) {
            if (level.getBlockState(p).is(BlockTags.FLOWERS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KLEUR, 0);
    }

    public Kleur kleur() {
        return Kleur.van(entityData.get(DATA_KLEUR));
    }

    public void setKleur(Kleur k) {
        entityData.set(DATA_KLEUR, k.ordinal());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        setKleur(Kleur.van(level.getRandom().nextInt(Kleur.values().length)));
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Nullable
    @Override
    protected BlockPos kiesLandplek() {
        BlockPos here = blockPosition(), best = null;
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-BLOEM_BEREIK, -4, -BLOEM_BEREIK), here.offset(BLOEM_BEREIK, 3, BLOEM_BEREIK))) {
            if (goedeLandplek(p) && random.nextInt(++n) == 0) {
                best = p.immutable();                           // (a random flower of the ones around)
            }
        }
        return best;
    }

    @Override
    protected boolean goedeLandplek(BlockPos pos) {
        return level().getBlockState(pos).is(BlockTags.FLOWERS) && level().getBlockState(pos.above()).getCollisionShape(level(), pos.above()).isEmpty();
    }

    @Override
    protected int zitDuur() {
        return level().isNight() ? 20 * 60 * 3 : 120 + random.nextInt(300);
    }

    @Override
    protected boolean schrikt() {
        return !level().isNight() && super.schrikt();
    }

    @Override
    protected double snelheid() {
        return 0.25;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasCustomName();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kleur", kleur().id());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (Kleur k : Kleur.values()) {
            if (k.id().equals(tag.getStringOr("Kleur", ""))) {
                setKleur(k);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 2.0f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.2f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("fladder", 2, state -> state.setAndContinue(zit() ? ZIT : FLY)));
    }
}
