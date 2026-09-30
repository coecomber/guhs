package nl.juiced.guhs.feature.waterdiertjes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

/**
 * Het glimguhtje (3.0, DESIGN_30 §6): a firefly that is a tiny round guh with see-through wings and a glowing belly
 * lantern (full-bright, the glowmask). They only come out at night: in slow, dreamy swirls low over the Kaasmoeras and the
 * Guhweides (grass, reeds and water), blinking soft little lights (and now and then a glassy "ting"). When the sun comes up
 * they fade away with a sparkle (unless you named one).
 */
public class GlimguhtjeEntity extends FladderDiertje {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GLIM = RawAnimation.begin().thenLoop("glim");

    public GlimguhtjeEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    /** At night, in the open air a little above grass, plants or water; never too many together. */
    public static boolean checkSpawn(EntityType<GlimguhtjeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE || spawnType == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnType == EntitySpawnReason.COMMAND) {
            return true;
        }
        if (!(level instanceof Level l) || !l.isDarkOutside() || !level.getBlockState(pos).isAir() || !level.canSeeSky(pos)) {
            return false;
        }
        int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (pos.getY() - grond > 4 || pos.getY() < grond || !fijneGrond(level, new BlockPos(pos.getX(), grond, pos.getZ()))) {
            return false;
        }
        return level.getEntitiesOfClass(GlimguhtjeEntity.class, new AABB(pos).inflate(20)).size() < 10;
    }

    @Nullable
    @Override
    protected BlockPos kiesLandplek() {
        return null;                                            // (it never lands: it glows while it floats)
    }

    @Override
    protected boolean goedeLandplek(BlockPos pos) {
        return false;
    }

    @Override
    protected boolean magLanden() {
        return false;
    }

    @Override
    protected double snelheid() {
        return 0.12;
    }

    /** Dreamy swirls, low over the ground or the water (1-3 blocks up). */
    @Override
    protected BlockPos volgendDoel() {
        BlockPos grond = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPosition());
        int y = grond.getY() + 1 + random.nextInt(3);
        return new BlockPos(getBlockX() + random.nextInt(9) - 4, y, getBlockZ() + random.nextInt(9) - 4);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && random.nextInt(12) == 0) {
            level().addParticle(ParticleTypes.GLOW, getX(), getY() + 0.1, getZ(), 0, 0.005, 0);
        }
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        if (level().isBrightOutside() && !hasCustomName() && random.nextInt(200) == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.1, getZ(), 6, 0.15, 0.15, 0.15, 0.01);
            discard();                                          // (morning: it fades away)
        }
    }

    /** Is it glowing over the right sort of ground (grass, plants, water)? (The spawn check.) */
    public static boolean fijneGrond(LevelAccessor level, BlockPos pos) {
        BlockPos g = pos.below();
        return level.getBlockState(g).is(BlockTags.DIRT) || level.getFluidState(g).is(FluidTags.WATER) || level.getBlockState(g).is(BlockTags.REPLACEABLE);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasCustomName();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WaterdiertjesFeature.TING.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
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
    public float getLightLevelDependentMagicValue() {
        return 1.0f;                                            // (it glows by itself)
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("vleugels", 0, state -> state.setAndContinue(FLY)));
        controllers.add(new AnimationController<>("glim", 0, state -> state.setAndContinue(GLIM)));
    }
}
