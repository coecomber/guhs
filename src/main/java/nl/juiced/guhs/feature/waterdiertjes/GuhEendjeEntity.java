package nl.juiced.guhs.feature.waterdiertjes;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * Het guh-eendje (3.0, DESIGN_30 §6): a round, cream-white mama duck with a guh face and round guh ears, paddling on the
 * ponds and the Guhzee with a <b>rijtje kuikentjes</b> behind her: fluffy yellow ducklings (her babies) that each follow the
 * one in front of them ({@link RijtjeGoal}: the first follows mama, the second the first...). Kwak-njeg!
 * <ul>
 *   <li>A wild mama (natural or chunk-generation spawn) comes with {@link #MIN_KUIKENS}-{@link #MAX_KUIKENS} ducklings.</li>
 *   <li>It floats on water (it bobs at the surface and paddles), waddles on land. Not tameable; bread or seeds lure it,
 *       and two mamas with seeds get a kuikentje (who follows the mama that got it).</li>
 *   <li>Kuikentjes grow up slowly ({@link #KUIKEN_TIJD}).</li>
 * </ul>
 */
public class GuhEendjeEntity extends Animal implements GeoEntity {
    public static final int MIN_KUIKENS = 2, MAX_KUIKENS = 4;
    /** Ducklings stay little this long (an hour: long enough to enjoy the rijtje). */
    public static final int KUIKEN_TIJD = -72000;
    /** How close each duckling keeps to the one in front of it. */
    public static final double AFSTAND = 1.1;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** (Ducklings) the mama they follow. */
    @Nullable
    private UUID mama;

    public GuhEendjeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /**
     * On the water surface of a pond, a lake edge or the coast (land within a few blocks), or on the grass right next to
     * the water; not too many families together. Spawn eggs and commands: anywhere.
     */
    public static boolean checkSpawn(EntityType<GuhEendjeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != EntitySpawnReason.NATURAL && spawnType != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        if (!level.getBlockState(pos).isAir() && !level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        boolean opWater = level.getFluidState(pos.below()).is(FluidTags.WATER) || level.getFluidState(pos).is(FluidTags.WATER);
        boolean water = opWater, land = !opWater && level.getBlockState(pos.below()).isSolid();
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-5, -2, -5), pos.offset(5, 1, 5))) {
            if (level.getFluidState(q).is(FluidTags.WATER)) {
                water = true;
            } else if (level.getBlockState(q).isSolid() && level.getBlockState(q.above()).isAir()) {
                land = true;
            }
            if (water && land) {
                break;
            }
        }
        return water && land && level.getEntitiesOfClass(GuhEendjeEntity.class, new AABB(pos).inflate(32), e -> !e.isBaby()).size() < 3;
    }

    // --- a mama with a rijtje -----------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, spawnType, data);
        if (!isBaby() && (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION)) {
            int n = MIN_KUIKENS + level.getRandom().nextInt(MAX_KUIKENS - MIN_KUIKENS + 1);
            for (int i = 0; i < n; i++) {
                GuhEendjeEntity kuiken = WaterdiertjesFeature.GUH_EENDJE.get().create(level.getLevel(), EntitySpawnReason.TRIGGERED);
                if (kuiken == null) {
                    continue;
                }
                Vec3 achter = Vec3.directionFromRotation(0, getYRot()).scale(-(i + 1) * AFSTAND);
                kuiken.snapTo(getX() + achter.x, getY(), getZ() + achter.z, getYRot(), 0);
                kuiken.setAge(KUIKEN_TIJD);
                kuiken.mama = getUUID();
                if (entityTags().contains(nl.juiced.guhs.world.WildeDieren.KOM_EN_GA)) {
                    nl.juiced.guhs.world.WildeDieren.markeer(kuiken);   // 1.1.2: a come-and-go mama's rijtje comes and goes with her
                }
                level.addFreshEntity(kuiken);
            }
        }
        return out;
    }

    /** Hatches a duckling that follows this mama (tests, breeding). */
    public GuhEendjeEntity kuiken(ServerLevel level, Vec3 at) {
        GuhEendjeEntity kuiken = WaterdiertjesFeature.GUH_EENDJE.get().create(level, EntitySpawnReason.TRIGGERED);
        kuiken.snapTo(at.x, at.y, at.z, getYRot(), 0);
        kuiken.setAge(KUIKEN_TIJD);
        kuiken.mama = getUUID();
        level.addFreshEntity(kuiken);
        return kuiken;
    }

    @Nullable
    public UUID mama() {
        return mama;
    }

    /** (A duckling) its mama, when she is close by (within 24). */
    @Nullable
    public GuhEendjeEntity mamaHier() {
        if (mama == null) {
            return null;
        }
        List<GuhEendjeEntity> l = level().getEntitiesOfClass(GuhEendjeEntity.class, getBoundingBox().inflate(24), e -> e.getUUID().equals(mama));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Mama's ducklings close by, in their fixed order (the rijtje). */
    public List<GuhEendjeEntity> rijtje() {
        UUID id = getUUID();
        return level().getEntitiesOfClass(GuhEendjeEntity.class, getBoundingBox().inflate(24), e -> e.isBaby() && id.equals(e.mama))
                .stream().sorted(Comparator.comparing(Entity::getUUID)).toList();
    }

    /** Who this duckling waddles behind: mama, or the duckling before it in the rijtje. */
    @Nullable
    public Entity voorganger() {
        GuhEendjeEntity m = mamaHier();
        if (m == null) {
            return null;
        }
        List<GuhEendjeEntity> rij = m.rijtje();
        int i = rij.indexOf(this);
        return i <= 0 ? m : rij.get(i - 1);
    }

    /** A duckling walks right behind the one in front of it (mama first). */
    class RijtjeGoal extends Goal {
        @Nullable
        private Entity voor;
        private int opnieuw;

        RijtjeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isBaby() || mama == null || --opnieuw > 0) {
                return false;
            }
            opnieuw = 10;
            voor = voorganger();
            return voor != null && distanceToSqr(voor) > AFSTAND * AFSTAND * 1.4;
        }

        @Override
        public boolean canContinueToUse() {
            return voor != null && voor.isAlive() && distanceToSqr(voor) > AFSTAND * AFSTAND && distanceToSqr(voor) < 24 * 24;
        }

        @Override
        public void tick() {
            if (voor == null) {
                return;
            }
            getLookControl().setLookAt(voor, 10f, getMaxHeadXRot());
            if (tickCount % 5 == 0) {
                Vec3 achter = voor.position().add(Vec3.directionFromRotation(0, voor.getYRot()).scale(-AFSTAND * 0.8));
                double speed = distanceToSqr(voor) > 16 ? 1.6 : 1.15;
                if (distanceToSqr(achter) < 3 * 3 && Math.abs(achter.y - getY()) < 0.6) {
                    // close by: waddle straight to the spot (a path stops one block short, the rijtje would gap)
                    getNavigation().stop();
                    getMoveControl().setWantedPosition(achter.x, achter.y, achter.z, speed);
                } else {
                    getNavigation().moveTo(achter.x, achter.y, achter.z, speed);
                }
            }
            if (distanceToSqr(voor) > 20 * 20) {
                snapTo(voor.getX(), voor.getY(), voor.getZ(), getYRot(), getXRot());   // (lost: hop back in line)
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // --- goals, floating --------------------------------------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        goalSelector.addGoal(2, new RijtjeGoal());
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, 1.15, this::isFood, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9, 60) {
            @Override
            public boolean canUse() {
                return (!isBaby() || mama == null || mamaHier() == null) && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isInWater() && getFluidHeight(FluidTags.WATER) > 0.25) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, Math.min(0.08, v.y + 0.05), v.z);   // (it floats: a duck bobs on the water)
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BREAD) || stack.is(TuintjesFeature.KNABBELZAADJES.get())
                || stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult r = super.mobInteract(player, hand);
        if (r.consumesAction() && !level().isClientSide()) {
            triggerAnim("actie", "eet");
        }
        return r;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        GuhEendjeEntity kuiken = WaterdiertjesFeature.GUH_EENDJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (kuiken != null) {
            kuiken.mama = getUUID();
        }
        return kuiken;
    }

    @Override
    public void setAge(int age) {
        super.setAge(age);
        if (age >= 0) {
            mama = null;                                      // (grown up: no more rijtje)
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;                                          // (it flaps its little wings)
    }

    // --- save ---------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (mama != null) {
            tag.store("Mama", UUIDUtil.CODEC, mama);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        mama = tag.read("Mama", UUIDUtil.CODEC).orElse(null);
    }

    // --- sounds --------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? WaterdiertjesFeature.PIEP.get() : WaterdiertjesFeature.KWAK.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isBaby() ? 120 : 200;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide() && !isBaby()) {
            triggerAnim("actie", "kwak");
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isBaby() ? WaterdiertjesFeature.PIEP.get() : WaterdiertjesFeature.KWAK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_SOUNDS.get(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.SoundSet.CLASSIC).adultSounds().deathSound().value();
    }

    @Override
    protected float getSoundVolume() {
        return isBaby() ? 0.5f : 0.7f;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 18) {                                         // (in love: hearts, and a happy flap)
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.HEART, getRandomX(0.6), getRandomY() + 0.3, getRandomZ(0.6), 0, 0, 0);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 3, state -> {
            if (isInWater()) {
                return state.setAndContinue(ZWEM);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("kwak", RawAnimation.begin().thenPlay("kwak"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"))
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
