package nl.juiced.guhs.feature.spiesburcht;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhAdvancements;
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
 * De Aangebrande Mika: the wither of the Barbecuether, a huge charred three-headed Mika. Called up (in any dimension)
 * with a T of four as_blok and three verkoolde mikakoppen on top ({@link MikakopBlock#checkSpawn}).
 * <p>
 * Heavy but forgiving:
 * <ul>
 *   <li>it first bakes up for {@link #SPAWN_TICKS} ticks (it can't be hurt then), then roars and pushes everyone back
 *       a little (no damage, no explosion);</li>
 *   <li>it hovers above its target and fires a volley of three slow burning coals (one per head), always announced
 *       by a sizzle and glowing heads, so you can dodge;</li>
 *   <li>below half health it's "doorgebakken": it calls two Vonk-Mika's once and shoots faster, but after every third
 *       volley it has to catch its breath close to the ground, and then it takes extra damage;</li>
 *   <li>it never breaks or burns a single block (its coals only singe what they hit) and it only goes after players.</li>
 * </ul>
 * Dies with "NJEG... IK BEN... DOORGEBAKKEN!" and drops the gloeister (loot table entities/aangebrande_mika).
 */
public class AangebrandeMikaEntity extends Monster implements GeoEntity {
    public static final float MAX_HEALTH = 300f;
    public static final int SPAWN_TICKS = 140;
    public static final int CHARGE_TICKS = 24;
    public static final int REST_TICKS = 70;
    public static final float REST_DAMAGE_BONUS = 1.5f;
    private static final EntityDataAccessor<Integer> DATA_SPAWNING = SynchedEntityData.defineId(AangebrandeMikaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(AangebrandeMikaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RESTING = SynchedEntityData.defineId(AangebrandeMikaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.aangebrande_mika.idle");
    private static final RawAnimation SPAWN = RawAnimation.begin().thenLoop("animation.aangebrande_mika.spawn");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.aangebrande_mika.charge");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("animation.aangebrande_mika.rest");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("animation.aangebrande_mika.shoot");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(net.minecraft.util.Mth.createInsecureUUID(this.random), Component.translatable("entity.guhs.aangebrande_mika"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Ticks until the next volley starts charging (and the step within a volley). */
    private int attackCooldown = 60;
    private int chargeTime;
    private int shotsLeft;
    private int volleys;
    private int restTime;
    private boolean doorgebakken;
    @Nullable
    private BlockPos home;

    public AangebrandeMikaEntity(EntityType<? extends AangebrandeMikaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 60;
        this.setHealth(this.getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FLYING_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPAWNING, 0);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_RESTING, false);
    }

    @Override
    protected void registerGoals() {
        // (all its behaviour is in aiStep: it only ever goes after players)
    }

    /** Freshly summoned: it bakes up first (can't be hurt), like the wither. */
    public void startSpawning() {
        this.entityData.set(DATA_SPAWNING, SPAWN_TICKS);
        this.setHealth(this.getMaxHealth() / 3f);
        this.home = this.blockPosition();
        this.setPersistenceRequired();
    }

    public int spawningTicks() {
        return this.entityData.get(DATA_SPAWNING);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    public boolean isResting() {
        return this.entityData.get(DATA_RESTING);
    }

    public boolean isDoorgebakken() {
        return doorgebakken;
    }

    /** Game tests: skip the baking up. */
    public void finishSpawning() {
        this.entityData.set(DATA_SPAWNING, 1);
    }

    // --- behaviour -------------------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        Vec3 v = this.getDeltaMovement().scale(0.82);
        if (!this.level().isClientSide() && spawningTicks() <= 0) {
            Vec3 wanted = wantedPosition();
            if (wanted != null) {
                Vec3 to = wanted.subtract(position());
                double len = to.length();
                if (len > 0.5) {
                    double speed = isResting() ? 0.035 : 0.06;
                    v = v.add(to.normalize().scale(speed * Math.min(1.0, len / 3.0)));
                }
            }
            LivingEntity target = getTarget();
            if (target != null) {
                this.getLookControl().setLookAt(target, 20f, 20f);
            }
        }
        this.setDeltaMovement(v);
        super.aiStep();
        if (this.level().isClientSide()) {
            clientParticles();
        }
    }

    /** Where it wants to float: above and a bit away from its target (low down when catching its breath). */
    @Nullable
    private Vec3 wantedPosition() {
        LivingEntity target = getTarget();
        if (target == null) {
            if (home == null) {
                return null;
            }
            return Vec3.atCenterOf(home).add(0, 3 + Math.sin(tickCount / 30.0), 0);
        }
        Vec3 away = position().subtract(target.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-3) {
            away = new Vec3(1, 0, 0);
        }
        double angle = tickCount / 80.0;
        away = away.normalize().yRot((float) (Math.sin(angle) * 0.5));
        if (isResting()) {
            return target.position().add(away.scale(4)).add(0, 1.0, 0);
        }
        return target.position().add(away.scale(7)).add(0, 4.5 + Math.sin(tickCount / 25.0), 0);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        int spawning = spawningTicks();
        if (spawning > 0) {
            int left = spawning - 1;
            this.entityData.set(DATA_SPAWNING, left);
            this.setHealth(Math.min(getMaxHealth(), getHealth() + getMaxHealth() / (SPAWN_TICKS * 1.5f)));
            bossEvent.setProgress(1f - left / (float) SPAWN_TICKS);
            if (left % 10 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 10, 0.8, 1.0, 0.8, 0.02);
                level.playSound(null, blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.6f);
            }
            if (left == 0) {
                roar(level);
            }
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = findTarget(level);
        setTarget(target);
        if (!doorgebakken && getHealth() <= getMaxHealth() / 2f) {
            becomeDoorgebakken(level);
        }
        if (isResting()) {
            if (--restTime <= 0) {
                this.entityData.set(DATA_RESTING, false);
            } else if (restTime % 8 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY(0.8), getZ(), 3, 0.5, 0.2, 0.5, 0.01);
            }
            return;
        }
        if (target == null) {
            return;
        }
        if (isCharging()) {
            chargeTime++;
            if (chargeTime == 1) {
                level.playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 2.0f, 0.5f);
            }
            if (chargeTime % 4 == 0) {
                level.sendParticles(ParticleTypes.FLAME, getX(), getY(0.85), getZ(), 6, 0.9, 0.3, 0.9, 0.01);
            }
            if (chargeTime >= CHARGE_TICKS && (chargeTime - CHARGE_TICKS) % 6 == 0 && shotsLeft > 0) {
                shoot(level, target, 3 - shotsLeft);
                shotsLeft--;
            }
            if (shotsLeft <= 0) {
                this.entityData.set(DATA_CHARGING, false);
                volleys++;
                attackCooldown = doorgebakken ? 45 : 70;
                if (doorgebakken && volleys % 3 == 0) {
                    startResting(level);
                }
            }
        } else if (--attackCooldown <= 0 && hasLineOfSight(target)) {
            this.entityData.set(DATA_CHARGING, true);
            chargeTime = 0;
            shotsLeft = 3;
        }
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel level) {
        LivingEntity current = getTarget();
        if (current instanceof Player p && p.isAlive() && !p.getAbilities().instabuild && !p.isSpectator() && distanceToSqr(p) < 48 * 48) {
            return current;
        }
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(40),
                p -> p.isAlive() && !p.getAbilities().instabuild && !p.isSpectator());
        return players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    /** One burning coal from one of the three heads (0 = left, 1 = middle, 2 = right). */
    private void shoot(ServerLevel level, LivingEntity target, int head) {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double side = (head - 1) * 1.1;
        double hx = getX() + Mth.cos(yaw) * side;
        double hz = getZ() + Mth.sin(yaw) * side;
        double hy = getY() + (head == 1 ? 2.6 : 2.2);
        Vec3 dir = new Vec3(target.getX() - hx, target.getY(0.5) - hy, target.getZ() - hz).normalize();
        GloeiendKooltje coal = new GloeiendKooltje(SpiesburchtFeature.BRANDEND_KOOLTJE.get(), this, dir, level);
        coal.accelerationPower = doorgebakken ? 0.07 : 0.055;
        coal.setPos(hx + dir.x, hy, hz + dir.z);
        level.addFreshEntity(coal);
        this.triggerAnim("action", "shoot");
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.6f + head * 0.1f);
    }

    private void startResting(ServerLevel level) {
        this.entityData.set(DATA_RESTING, true);
        restTime = REST_TICKS;
        level.playSound(null, blockPosition(), ModSounds.MIKA_HURT.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
        say(level, "quest.guhs.aangebrande_mika.hijgen", ChatFormatting.GOLD, 32);
    }

    private void becomeDoorgebakken(ServerLevel level) {
        doorgebakken = true;
        say(level, "quest.guhs.aangebrande_mika.doorgebakken", ChatFormatting.RED, 48);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.5f, 1.4f);
        for (int i = 0; i < 2; i++) {
            VonkMikaEntity helper = SpiesburchtFeature.VONK_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (helper != null) {
                helper.snapTo(getX() + (i == 0 ? -2 : 2), getY(), getZ(), getYRot(), 0);
                helper.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
                helper.setTarget(getTarget());
                level.addFreshEntity(helper);
                level.sendParticles(ParticleTypes.FLAME, helper.getX(), helper.getY(0.5), helper.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            }
        }
    }

    /** The end of baking up: "who burnt me?!" and a hot puff that pushes everyone back a little (no damage). */
    private void roar(ServerLevel level) {
        say(level, "quest.guhs.aangebrande_mika.wakker", ChatFormatting.RED, 48);
        level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.3f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(0.5), getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 60, 2.0, 1.0, 2.0, 0.1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7), e -> e != this)) {
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1);
            if (push.lengthSqr() < 1.0E-3) {
                push = new Vec3(1, 0, 0);
            }
            push = push.normalize().scale(1.1);
            e.push(push.x, 0.45, push.z);
            e.hurtMarked = true;
        }
    }

    private void say(ServerLevel level, String key, ChatFormatting colour, double range) {
        Component text = Component.translatable(key).withStyle(colour, ChatFormatting.BOLD);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(range))) {
            p.sendSystemMessage(Component.translatable("chat.type.text", getDisplayName(), text));
        }
    }

    private void clientParticles() {
        if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.8), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.8), 0, 0.03, 0);
        }
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.LAVA, getRandomX(0.6), getY(0.6), getRandomZ(0.6), 0, 0, 0);
        }
        if (isCharging()) {
            for (int head = 0; head < 3; head++) {
                float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
                double side = (head - 1) * 1.1;
                level().addParticle(ParticleTypes.FLAME, getX() + Mth.cos(yaw) * side, getY() + (head == 1 ? 2.7 : 2.3), getZ() + Mth.sin(yaw) * side,
                        0, 0.03, 0);
            }
        }
        if (spawningTicks() > 0) {
            level().addParticle(ParticleTypes.ASH, getRandomX(1.5), getY() + random.nextDouble() * 3, getRandomZ(1.5), 0, 0, 0);
        }
    }

    // --- damage ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (spawningTicks() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (source.getEntity() instanceof AangebrandeMikaEntity || source.getEntity() instanceof VonkMikaEntity) {
            return false;
        }
        if (isResting()) {
            amount *= REST_DAMAGE_BONUS;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.getEffect().value().isBeneficial() && effect.getEffect().value() != net.minecraft.world.effect.MobEffects.WITHER.value()
                && super.canBeAffected(effect);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && !this.isRemoved() && !this.dead) {
            say(level, "quest.guhs.aangebrande_mika.dood", ChatFormatting.GOLD, 64);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 80, 1.5, 1.5, 1.5, 0.08);
            level.sendParticles(ParticleTypes.ASH, getX(), getY(0.5), getZ(), 120, 2.5, 2.0, 2.5, 0.0);
            level.sendParticles(ParticleTypes.FLAME, getX(), getY(0.5), getZ(), 40, 1.0, 1.0, 1.0, 0.06);
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(64))) {
                GuhAdvancements.grant(p, "aangebrande_mika_verslagen");
            }
        }
        super.die(source);
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    // --- boss bar, saving, sounds -------------------------------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Spawning", spawningTicks());
        tag.putBoolean("Doorgebakken", doorgebakken);
        tag.putInt("Volleys", volleys);
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_SPAWNING, tag.getIntOr("Spawning", 0));
        doorgebakken = tag.getBooleanOr("Doorgebakken", false);
        volleys = tag.getIntOr("Volleys", 0);
        home = tag.keySet().contains("Home") ? BlockPos.of(tag.getLongOr("Home", 0L)) : null;
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIKA_AMBIENT.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.45f;
    }

    @Override
    protected float getSoundVolume() {
        return 2.5f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIKA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 6, this::mainAnimation));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP).triggerableAnim("shoot", SHOOT));
    }

    private PlayState mainAnimation(AnimationTest<AangebrandeMikaEntity> state) {
        if (spawningTicks() > 0) {
            return state.setAndContinue(SPAWN);
        }
        if (isResting()) {
            return state.setAndContinue(REST);
        }
        return state.setAndContinue(isCharging() ? CHARGE : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
