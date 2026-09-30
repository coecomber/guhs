package nl.juiced.guhs.feature.gatenkaas;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * De Vadswaker: a huge, blind Mika who guards the Stille Voorraadkelder (the Warden of the Guhmension). He sleeps under
 * the floor of the larder with his belly full of stolen knabbels; the knabbelschreeuwers wake him. He can't see a thing,
 * but his big ears hear you walk, dig and above all <b>chew</b> ({@link Knabbelgeluid}), and now and then he sniffs the
 * air. Whoever he hears enough of, he chases, and his paw really hurts; from further away he roars a NJEG-brul.
 * Run, or sneak away! He never breaks blocks, and after a while of silence he digs himself back into the ground.
 */
public class VadswakerEntity extends Monster implements GeoEntity {
    public static final float HEALTH = 300f;
    public static final int EMERGE_TICKS = 60, DIG_TICKS = 60, ROAR_TICKS = 34, SNIFF_TICKS = 36;
    /** Anger: from this much on he chases you; at most this much. */
    public static final int ANGRY = 80, MAX_ANGER = 150;
    /** Digs back in after this long without hearing anyone, or after this long in total (when he isn't chasing). */
    public static final int CALM_DIG_AFTER = 20 * 60, MAX_LIFE = 20 * 60 * 5;
    public static final double SNIFF_RANGE = 6.0, HEAR_RANGE = 24.0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vadswaker.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.vadswaker.walk");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlayAndHold("animation.vadswaker.emerge");
    private static final RawAnimation DIG = RawAnimation.begin().thenPlayAndHold("animation.vadswaker.dig");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.vadswaker.roar");
    private static final RawAnimation SNIFF = RawAnimation.begin().thenPlay("animation.vadswaker.sniff");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.vadswaker.attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> anger = new HashMap<>();
    @Nullable
    private BlockPos heardAt;
    private int poseTicks, calmTicks, life, sniffCooldown = 100, roarCooldown = 60, listenCooldown;

    public VadswakerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // --- goals: no eyes, so no "look at player" and no target finding: his ears pick the target ---------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !busy() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new InvestigateGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5, 0.001f) {
            @Override
            public boolean canUse() {
                return !busy() && super.canUse();
            }
        });
    }

    /** Walks to where he last heard something (when he isn't chasing anyone). */
    private class InvestigateGoal extends Goal {
        InvestigateGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !busy() && getTarget() == null && heardAt != null && heardAt.distToCenterSqr(position()) > 4;
        }

        @Override
        public void start() {
            getNavigation().moveTo(heardAt.getX() + 0.5, heardAt.getY(), heardAt.getZ() + 0.5, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return !busy() && getTarget() == null && heardAt != null && !getNavigation().isDone();
        }

        @Override
        public void stop() {
            heardAt = null;
        }
    }

    /** Coming out of or going into the ground, roaring or sniffing: then he doesn't walk or fight. */
    public boolean busy() {
        Pose pose = getPose();
        return pose == Pose.EMERGING || pose == Pose.DIGGING || pose == Pose.ROARING || pose == Pose.SNIFFING;
    }

    public boolean isEmerging() {
        return getPose() == Pose.EMERGING;
    }

    public boolean isDigging() {
        return getPose() == Pose.DIGGING;
    }

    private void setPoseAndReset(Pose pose) {
        setPose(pose);
        poseTicks = 0;
        if (pose != Pose.STANDING) {
            getNavigation().stop();
        }
    }

    /** Called right after he's put in the world by a knabbelschreeuwer (or a spawn egg): climb out of the ground. */
    public void startEmerging() {
        setPoseAndReset(Pose.EMERGING);
        playSound(SoundEvents.WARDEN_EMERGE, 2f, 1.25f);
    }

    /** (Game tests: done climbing out right away / quiet for this long already.) */
    void standUp() {
        setPoseAndReset(Pose.STANDING);
    }

    void quietFor(int ticks) {
        anger.clear();
        calmTicks = ticks;
    }

    public void startDigging() {
        setPoseAndReset(Pose.DIGGING);
        setTarget(null);
        playSound(SoundEvents.WARDEN_DIG, 2f, 1.25f);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        startEmerging();
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    // --- hearing ------------------------------------------------------------------------------------------------------

    /** He hears this player at this spot (a step, a bite, a hack...): angrier at them, and he goes to look. */
    public void hear(Player player, BlockPos at, int amount) {
        if (isDigging() || !player.isAlive() || Knabbelgeluid.ghostly(player) || StilEffect.isStil(player)) {
            return;
        }
        anger.merge(player.getUUID(), amount, (a, b) -> Math.min(MAX_ANGER, a + b));
        heardAt = at.immutable();
        calmTicks = 0;
        if (listenCooldown <= 0) {
            listenCooldown = 30;
            playSound(anger(player) >= ANGRY ? SoundEvents.WARDEN_LISTENING_ANGRY : SoundEvents.WARDEN_LISTENING, 1.5f, 1.3f);
        }
    }

    public int anger(Entity entity) {
        return anger.getOrDefault(entity.getUUID(), 0);
    }

    /** The one he's angriest at, if that's enough to chase them. */
    @Nullable
    private LivingEntity suspect() {
        if (!(level() instanceof ServerLevel level)) {
            return null;
        }
        LivingEntity best = null;
        int most = ANGRY - 1;
        for (Map.Entry<UUID, Integer> e : anger.entrySet()) {
            if (e.getValue() > most && level.getEntity(e.getKey()) instanceof LivingEntity living && canChase(living)) {
                best = living;
                most = e.getValue();
            }
        }
        return best;
    }

    private boolean canChase(LivingEntity living) {
        return living.isAlive() && living.level() == level() && living.distanceToSqr(this) < 48 * 48
                && !(living instanceof Player p && Knabbelgeluid.ghostly(p));
    }

    /** Sniff the air: a player close by gets noticed, even when sneaking (a little). */
    private void sniff() {
        playSound(SoundEvents.WARDEN_SNIFF, 1.5f, 1.25f);
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(SNIFF_RANGE))) {
            if (!StilEffect.isStil(player) && player.distanceToSqr(this) <= SNIFF_RANGE * SNIFF_RANGE) {
                hear(player, player.blockPosition(), player.isSteppingCarefully() ? 20 : 35);
            }
        }
    }

    // --- every tick ---------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        life++;
        poseTicks++;
        listenCooldown--;
        switch (getPose()) {
            case EMERGING -> {
                groundParticles(level);
                if (poseTicks >= EMERGE_TICKS) {
                    setPoseAndReset(Pose.STANDING);
                    playSound(SoundEvents.WARDEN_ROAR, 2f, 1.3f);
                }
                return;
            }
            case DIGGING -> {
                groundParticles(level);
                if (poseTicks >= DIG_TICKS) {
                    discard();
                }
                return;
            }
            case ROARING -> {
                if (poseTicks == 18) {
                    njegBrul(level);
                }
                if (poseTicks >= ROAR_TICKS) {
                    setPoseAndReset(Pose.STANDING);
                }
            }
            case SNIFFING -> {
                if (poseTicks == 16) {
                    sniff();
                }
                if (poseTicks >= SNIFF_TICKS) {
                    setPoseAndReset(Pose.STANDING);
                }
            }
            default -> {
            }
        }
        // anger fades (much faster for someone who has gone quiet with guhs:stil)
        if (tickCount % 20 == 0) {
            for (Iterator<Map.Entry<UUID, Integer>> it = anger.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Integer> e = it.next();
                Entity who = level.getEntity(e.getKey());
                int left = e.getValue() - (who instanceof LivingEntity l && StilEffect.isStil(l) ? 6 : 1);
                if (left <= 0 || !(who instanceof LivingEntity living) || !canChase(living)) {
                    it.remove();
                } else {
                    e.setValue(left);
                }
            }
        }
        LivingEntity suspect = suspect();
        if (suspect != getTarget()) {
            setTarget(suspect);
        }
        calmTicks = anger.isEmpty() ? calmTicks + 1 : 0;
        if (!busy() && (calmTicks > CALM_DIG_AFTER || life > MAX_LIFE && getTarget() == null)) {
            startDigging();
            return;
        }
        if (getPose() != Pose.STANDING) {
            return;
        }
        if (getTarget() == null && --sniffCooldown <= 0) {
            sniffCooldown = 100 + random.nextInt(100);
            setPoseAndReset(Pose.SNIFFING);
        } else if (getTarget() != null && --roarCooldown <= 0) {
            double d = distanceTo(getTarget());
            if (d > 5 && d < 15) {
                roarCooldown = 160;
                setPoseAndReset(Pose.ROARING);
                playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2f, 1.3f);
            }
        }
    }

    /** The NJEG-brul: a shock wave of pure grumpiness, straight through armour. */
    private void njegBrul(ServerLevel level) {
        LivingEntity target = getTarget();
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 3f, 1.2f);
        playSound(ModSounds.MIKA_HURT.get(), 3f, 0.5f);
        if (target == null || distanceTo(target) > 16) {
            return;
        }
        Vec3 from = position().add(0, 1.2, 0);
        Vec3 to = target.getEyePosition().subtract(from);
        Vec3 step = to.normalize();
        for (int i = 1; i < Mth.floor(to.length()) + 4; i++) {
            Vec3 p = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (target.hurt(damageSources().sonicBoom(this), 10f)) {
            double kb = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            target.push(step.x * 2.2 * kb, 0.5 * kb, step.z * 2.2 * kb);
            target.hurtMarked = true;
        }
    }

    private void groundParticles(ServerLevel level) {
        BlockState below = level.getBlockState(blockPosition().below());
        if (!below.isAir() && tickCount % 2 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.1, getZ(), 12, 0.8, 0.1, 0.8, 0.1);
        }
        if (poseTicks % 12 == 0) {
            playSound(below.getSoundType(level, blockPosition().below(), this).getBreakSound(), 1.5f, 0.7f);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", "attack");
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 1.2f);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player player && !isDigging()) {
            anger.merge(player.getUUID(), 100, (a, b) -> Math.min(MAX_ANGER, a + b));   // (hit him and he knows where you are)
            calmTicks = 0;
        }
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return (isEmerging() || isDigging()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return !busy() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
        tag.putInt("Calm", calmTicks);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getIntOr("Life", 0);
        calmTicks = tag.getIntOr("Calm", 0);
    }

    // --- sounds: he chews all the time (stolen knabbels), and grumbles like a Mika, only much deeper -----------------

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? ModSounds.MIKA_AMBIENT.get() : SoundEvents.GENERIC_EAT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIKA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIKA_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.5f + random.nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WARDEN_STEP, 0.8f, 1.2f);
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, this::mainAnimation));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    private PlayState mainAnimation(AnimationTest<VadswakerEntity> state) {
        return state.setAndContinue(switch (getPose()) {
            case EMERGING -> EMERGE;
            case DIGGING -> DIG;
            case ROARING -> ROAR;
            case SNIFFING -> SNIFF;
            default -> state.isMoving() ? WALK : IDLE;
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
