package nl.juiced.guhs.feature.kaasmoeras;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * De kaasmot: a tiny Mika moth with cheese-holed wings. It flutters around cheese and lights (the block tag
 * guhs:kaasmot_lokkers: kaasknabbelblokken, borrelende kaassaus, motknabbels, lampions...), and, like every Mika,
 * steals kaasknabbels: it nibbles up any that lie on the ground. Harmless to you; kikkerguhs love to snap them up
 * (a motknabbel pops out, {@link KikkerguhEntity#snap}).
 */
public class KaasmotEntity extends AmbientCreature implements GeoEntity {
    public static final TagKey<Block> LURES = TagKey.create(Registries.BLOCK, Guhs.id("kaasmot_lokkers"));
    /** How far a kaasmot looks for cheese (blocks) and dropped kaasknabbels. */
    public static final int LURE_RANGE = 8;
    public static final double STEAL_RANGE = 8.0;
    private static final int MAX_NEARBY = 8;

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.kaasmot.fly");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private BlockPos targetPosition;
    @Nullable
    private BlockPos lure;
    private int lureCheck;
    @Nullable
    private ItemEntity snack;

    public KaasmotEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0);
    }

    /** In the open air near the ground (above land or water), never too many together. */
    public static boolean checkKaasmotSpawnRules(EntityType<KaasmotEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos,
                                                 RandomSource random) {
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE) {
            return true;
        }
        if (!level.getBlockState(pos).isAir() || level.getBlockState(pos.below(3)).isAir()) {
            return false;
        }
        return level.getEntitiesOfClass(KaasmotEntity.class, new AABB(pos).inflate(24)).size() < MAX_NEARBY;
    }

    @Nullable
    public BlockPos getLure() {
        return lure;
    }

    // --- flying (like a bat, but it flutters around its lure) ---------------------------------------------------------

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
        if (level().isClientSide() && random.nextInt(30) == 0) {
            level().addParticle(BorrelendeKaassausBlock.CHEESE_DUST, getX(), getY() + 0.1, getZ(), 0, -0.02, 0);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (--lureCheck <= 0) {
            lureCheck = 40 + random.nextInt(40);
            snack = findSnack();
            if (lure == null || !level().getBlockState(lure).is(LURES) || random.nextInt(4) == 0) {
                lure = findLure();
            }
        }
        if (snack != null && (!snack.isAlive() || !snack.getItem().is(ModItems.KAAS_KNABBELS.get()))) {
            snack = null;
        }
        if (snack != null) {
            if (distanceToSqr(snack) < 0.8) {
                nibble(snack);
                snack = null;
                targetPosition = null;
            } else {
                flyTowards(snack.position().add(0, 0.3, 0), 0.6);
            }
            return;
        }
        if (targetPosition != null && (!level().isEmptyBlock(targetPosition) || targetPosition.getY() <= level().getMinY())) {
            targetPosition = null;
        }
        if (targetPosition == null || random.nextInt(30) == 0 || targetPosition.closerToCenterThan(position(), 1.5)) {
            targetPosition = nextTarget();
        }
        flyTowards(Vec3.atBottomCenterOf(targetPosition).add(0, 0.1, 0), 0.4);
    }

    private BlockPos nextTarget() {
        if (lure != null) {   // flutter in a little cloud around the cheese / the light
            return lure.offset(random.nextInt(5) - 2, 1 + random.nextInt(3), random.nextInt(5) - 2);
        }
        return BlockPos.containing(getX() + random.nextInt(7) - random.nextInt(7), getY() + random.nextInt(5) - 2,
                getZ() + random.nextInt(7) - random.nextInt(7));
    }

    private void flyTowards(Vec3 to, double speed) {
        double dx = to.x - getX(), dy = to.y - getY(), dz = to.z - getZ();
        Vec3 v = getDeltaMovement();
        Vec3 nv = v.add((Math.signum(dx) * speed - v.x) * 0.1, (Math.signum(dy) * 0.7 - v.y) * 0.1, (Math.signum(dz) * speed - v.z) * 0.1);
        setDeltaMovement(nv);
        float yaw = (float) (Mth.atan2(nv.z, nv.x) * 180.0 / Math.PI) - 90.0f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
        this.zza = 0.5f;
    }

    /** The nearest cheese or light within {@link #LURE_RANGE} blocks, or null. */
    @Nullable
    public BlockPos findLure() {
        BlockPos here = blockPosition(), best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-LURE_RANGE, -4, -LURE_RANGE), here.offset(LURE_RANGE, 4, LURE_RANGE))) {
            if (level().getBlockState(p).is(LURES)) {
                double d = p.distSqr(here);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** Kaasknabbels lying on the ground nearby (a Mika can't resist), or null. */
    @Nullable
    private ItemEntity findSnack() {
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(STEAL_RANGE),
                i -> i.isAlive() && i.getItem().is(ModItems.KAAS_KNABBELS.get()));
        ItemEntity best = null;
        for (ItemEntity i : items) {
            if (best == null || distanceToSqr(i) < distanceToSqr(best)) {
                best = i;
            }
        }
        return best;
    }

    /** Nibbles one kaasknabbel off a stack lying on the ground. */
    public void nibble(ItemEntity item) {
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) {
            item.discard();
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.4f, 1.8f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(BorrelendeKaassausBlock.CHEESE_DUST, getX(), getY(), getZ(), 5, 0.15, 0.15, 0.15, 0.01);
        }
    }

    /** Reeled in by a kikkerguh's tongue. */
    public void pullTowards(Entity frog) {
        Vec3 to = frog.position().add(0, frog.getBbHeight() * 0.6, 0).subtract(position());
        setDeltaMovement(to.scale(0.25));
    }

    /** Eaten by a kikkerguh. */
    public void eaten() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 4, 0.1, 0.1, 0.1, 0.01);
            server.sendParticles(BorrelendeKaassausBlock.CHEESE_DUST, getX(), getY() + 0.1, getZ(), 8, 0.2, 0.2, 0.2, 0.01);
        }
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_FLY;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }

    @Override
    public float getVoicePitch() {
        return 1.8f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
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
