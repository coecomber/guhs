package nl.juiced.guhs.feature.waterdiertjes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;

/**
 * Het lieveheersbeestje (3.0, DESIGN_30 §6): a ladybird whose black spots are little guh heads. By day it flies to growing
 * guhtuintjes (TuinBlock: a guh-bloempot or guh-moestuinbak with something growing) and flowers, and lands on them. On a
 * growing tuintje it gives a gentle <b>growth help</b>: after sitting {@link #HULP_NA} ticks it may help the plant one step
 * ({@link #HULP_KANS}), then that beetle rests {@link #HULP_RUST} ticks before it helps again. "Een tikje sneller".
 */
public class LieveheersbeestjeEntity extends FladderDiertje {
    public static final int BEREIK = 12;
    /** Sitting on a growing tuintje this long, then a chance to help it grow a step; then a long rest. */
    public static final int HULP_NA = 20 * 20, HULP_RUST = 20 * 60 * 3;
    public static final float HULP_KANS = 0.5f;

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");

    private int zatAl;
    private long hulpRust;

    public LieveheersbeestjeEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    /** By day, in the open air near flowers or tuintjes; never too many together. */
    public static boolean checkSpawn(EntityType<LieveheersbeestjeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE || spawnType == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnType == EntitySpawnReason.COMMAND || spawnType == EntitySpawnReason.EVENT) {
            return true;
        }
        if (!(level instanceof Level l) || !l.isDay() || !level.getBlockState(pos).isAir() || !level.canSeeSky(pos)) {
            return false;
        }
        if (level.getEntitiesOfClass(LieveheersbeestjeEntity.class, new AABB(pos).inflate(24)).size() >= 4) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-5, -3, -5), pos.offset(5, 1, 5))) {
            if (level.getBlockState(p).is(BlockTags.FLOWERS) || TuinBlock.isTuin(level.getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    protected BlockPos kiesLandplek() {
        BlockPos here = blockPosition(), tuin = null, bloem = null;
        int nt = 0, nb = 0;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-BEREIK, -4, -BEREIK), here.offset(BEREIK, 3, BEREIK))) {
            var s = level().getBlockState(p);
            if (TuinBlock.groeit(s) && vrijBoven(p)) {
                if (random.nextInt(++nt) == 0) {
                    tuin = p.immutable();
                }
            } else if (s.is(BlockTags.FLOWERS) && vrijBoven(p) && random.nextInt(++nb) == 0) {
                bloem = p.immutable();
            }
        }
        return tuin != null ? tuin : bloem;                     // (a growing tuintje first: that's where it can help)
    }

    private boolean vrijBoven(BlockPos p) {
        return level().getBlockState(p.above()).getCollisionShape(level(), p.above()).isEmpty();
    }

    @Override
    protected boolean goedeLandplek(BlockPos pos) {
        var s = level().getBlockState(pos);
        return (TuinBlock.isTuin(s) || s.is(BlockTags.FLOWERS)) && vrijBoven(pos);
    }

    @Override
    protected void landen(net.minecraft.world.phys.Vec3 op) {
        super.landen(op);
        zatAl = 0;
    }

    @Override
    protected int zitDuur() {
        return landplek != null && TuinBlock.groeit(level().getBlockState(landplek)) ? HULP_NA + 200 + random.nextInt(400) : 200 + random.nextInt(300);
    }

    @Override
    protected void terwijlZit() {
        zatAl++;
        if (landplek != null && zatAl == HULP_NA && level() instanceof ServerLevel sl) {
            if (random.nextFloat() < HULP_KANS) {
                help(sl, landplek);
            }
        }
    }

    /**
     * Helps the tuintje at pos grow one step (if something grows there and this beetle isn't resting): growth sparkles, a
     * happy little hop, and the nearby players see it (advancement). Returns whether it grew.
     */
    public boolean help(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if (now < hulpRust || !TuinBlock.groeit(level.getBlockState(pos))) {
            return false;
        }
        hulpRust = now + HULP_RUST;
        TuinBlock.groei(level, pos);
        triggerAnim("actie", "helpen");
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 0.4f, 1.6f);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16))) {
            WaterdiertjesEvents.tuinhulp(p);
        }
        return true;
    }

    public boolean rust() {
        return level().getGameTime() < hulpRust;
    }

    @Override
    protected void opstijgen() {
        super.opstijgen();
        playSound(WaterdiertjesFeature.ZOEM.get(), 0.3f, 1.2f);
    }

    @Override
    protected double snelheid() {
        return 0.22;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasCustomName();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("HulpRust", hulpRust);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hulpRust = tag.getLongOr("HulpRust", 0L);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.2f;
    }

    @Override
    public float getVoicePitch() {
        return 2.0f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 2, state -> state.setAndContinue(zit() ? ZIT : FLY)));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("helpen", RawAnimation.begin().thenPlay("helpen")));
    }
}
