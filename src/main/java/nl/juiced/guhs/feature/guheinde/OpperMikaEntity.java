package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Opper-Mika: the boss of all Mika's, who stole every kaasknabbel of the guh kingdom. 150 HP, "pittig maar vergevend".
 * <ul>
 *   <li>Phase 1: he rides his starved Enderguh ({@link HongerigeEnderguhEntity}) and throws Mika-vetballen. While he
 *       sits on it he can't go below {@link #RIDING_FLOOR} HP; the knabbelkristallen heal him (GuheindeGevecht).</li>
 *   <li>Phase 2: fed, the Enderguh throws him off. On foot he runs at you, steals your kaasknabbels (he drops them all
 *       when he's beaten), leaves vetplassen, does a buikplof and calls Mika-hulpjes.</li>
 * </ul>
 * Same model as a Mika (bigger, darker, with the Knabbelkroon on his head: client OpperMikaRenderer).
 */
public class OpperMikaEntity extends Monster implements GeoEntity {
    public static final float HEALTH = 150f;
    /** While he's riding his Enderguh he never goes below this. */
    public static final float RIDING_FLOOR = 75f;
    public static final int VETBAL_COOLDOWN = 70, VETPLAS_COOLDOWN = 160, PLOF_COOLDOWN = 140;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.guhs.opper_mika"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation POUNCE = RawAnimation.begin().thenPlay("animation.guh.happy");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int vetbalCooldown = 40, vetplasCooldown = 100, plofCooldown = 100, shoutCooldown;
    /** The kaasknabbels he stole in this fight (he drops them when he's beaten). */
    private int buit;
    /** Mika-hulpjes already called (at 2/3 and 1/3 of his health on foot). */
    private int hulpjes;
    private boolean plofferen;

    public OpperMikaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.guhs.opper_mika"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.SCALE, 1.9);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isRiding() {
        return this.getVehicle() instanceof HongerigeEnderguhEntity;
    }

    public int buit() {
        return buit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof MikaEntity || source.getEntity() instanceof HongerigeEnderguhEntity || source.getDirectEntity() instanceof MikaVetbalEntity) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isRiding() && this.getHealth() < RIDING_FLOOR) {
            this.setHealth(RIDING_FLOOR);
            if (shoutCooldown <= 0) {
                shout("gui.guhs.guheinde.opper.vloer");
            }
        }
        return hurt;
    }

    /** A line from Opper-Mika for every player in the Guheinde near him. */
    public void shout(String key) {
        shoutCooldown = 200;
        if (this.level() instanceof ServerLevel level) {
            Component line = Component.literal("<").append(this.getDisplayName()).append("> ").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(this) < 160 * 160) {
                    player.sendSystemMessage(line);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        if (shoutCooldown > 0) {
            shoutCooldown--;
        }
        if (vetbalCooldown > 0) {
            vetbalCooldown--;
        }
        if (vetplasCooldown > 0) {
            vetplasCooldown--;
        }
        if (plofCooldown > 0) {
            plofCooldown--;
        }
        LivingEntity target = isRiding() ? nearestPlayer(48) : this.getTarget();
        if (target == null) {
            return;
        }
        if (vetbalCooldown == 0 && this.hasLineOfSight(target) && this.distanceToSqr(target) < 40 * 40) {
            throwVetbal(target);
            vetbalCooldown = isRiding() ? VETBAL_COOLDOWN : VETBAL_COOLDOWN * 2;
        }
        if (isRiding()) {
            return;
        }
        // --- on foot ---
        if (vetplasCooldown == 0 && this.distanceToSqr(target) < 12 * 12) {
            vetplas(target.position());
            vetplasCooldown = VETPLAS_COOLDOWN;
        }
        double d = this.distanceToSqr(target);
        if (plofCooldown == 0 && this.onGround() && d > 16 && d < 100) {
            Vec3 to = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(to.x * 0.9, 0.75, to.z * 0.9);
            this.hurtMarked = true;
            plofferen = true;
            plofCooldown = PLOF_COOLDOWN;
            this.playSound(ModSounds.MIKA_AMBIENT.get(), 2f, 0.6f);
        }
        if (plofferen && this.onGround() && this.getDeltaMovement().y <= 0.01) {
            buikplof();
        }
        float part = this.getHealth() / this.getMaxHealth();
        if ((hulpjes == 0 && part < 2f / 3f) || (hulpjes == 1 && part < 1f / 3f)) {
            hulpjes++;
            callHulpjes();
        }
    }

    @Nullable
    private Player nearestPlayer(double range) {
        Player p = this.level().getNearestPlayer(this, range);
        return p != null && !p.isCreative() && !p.isSpectator() ? p : null;
    }

    private void throwVetbal(LivingEntity target) {
        MikaVetbalEntity bal = new MikaVetbalEntity(this.level(), this);
        double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
        double dy = target.getY(0.3) - bal.getY();
        double flat = Math.sqrt(dx * dx + dz * dz);
        bal.shoot(dx, dy + flat * 0.12, dz, 1.1f + (float) Math.min(1.0, flat / 40), 2f);
        this.level().addFreshEntity(bal);
        this.playSound(SoundEvents.SNOWBALL_THROW, 1.5f, 0.6f);
        this.triggerAnim("action", "pounce");
    }

    /** A puddle of Mika's vet that makes you slow and slippery-sad. */
    public void vetplas(Vec3 at) {
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(3f);
        cloud.setRadiusPerTick(-0.01f);
        cloud.setDuration(120);
        cloud.setWaitTime(0);
        cloud.setParticle(new DustParticleOptions(new Vector3f(0.85f, 0.7f, 0.25f), 1.5f));
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        this.level().addFreshEntity(cloud);
    }

    private void buikplof() {
        plofferen = false;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 1.5, 0.2, 1.5, 0);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.2f, 0.45f), 2.5f), getX(), getY() + 0.3, getZ(), 40, 2.5, 0.2, 2.5, 0.1);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.4f);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4, 1.5, 4),
                e -> e != this && !(e instanceof MikaEntity) && !(e instanceof HongerigeEnderguhEntity))) {
            e.hurt(this.damageSources().mobAttack(this), 4f);
            Vec3 away = e.position().subtract(this.position()).normalize();
            e.knockback(1.4, -away.x, -away.z);
            e.hurtMarked = true;
        }
    }

    private void callHulpjes() {
        shout("gui.guhs.guheinde.opper.hulpjes");
        for (int i = 0; i < 2; i++) {
            MikaEntity mika = ModEntities.MIKA.get().create(this.level(), EntitySpawnReason.TRIGGERED);
            if (mika != null) {
                double a = this.random.nextDouble() * Math.PI * 2;
                mika.snapTo(getX() + Math.cos(a) * 3, getY() + 0.5, getZ() + Math.sin(a) * 3, this.random.nextFloat() * 360f, 0f);
                mika.setTarget(this.getTarget());
                this.level().addFreshEntity(mika);
            }
        }
    }

    /** His hits steal kaasknabbels: NJEG, those are his now (until you beat him). */
    @Override
    public boolean doHurtTarget(Entity target) {
        this.triggerAnim("action", "pounce");
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof ServerPlayer player) {
            steal(player);
        }
        return hurt;
    }

    /** Takes up to four kaasknabbels from a player. */
    public int steal(ServerPlayer player) {
        int stolen = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stolen < 4 && (stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()))) {
                int t = Math.min(4 - stolen, stack.getCount());
                stack.shrink(t);
                stolen += t;
            }
        }
        if (stolen > 0) {
            buit += stolen;
            player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.opper.roof", stolen).withStyle(ChatFormatting.DARK_PURPLE));
        }
        return stolen;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && !this.isRemoved() && !this.dead) {
            shout("gui.guhs.guheinde.opper.gevadst");
            dropBuit();
            GuheindeGevecht fight = GuheindeGevecht.of(this.level());
            if (fight != null) {
                fight.onOpperMikaKilled(this, source);
            }
        }
        super.die(source);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    public List<ServerPlayer> bossBarPlayers() {
        return List.copyOf(bossBar.getPlayers());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return vehicle instanceof HongerigeEnderguhEntity;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Buit", buit);
        tag.putInt("Hulpjes", hulpjes);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        buit = tag.getIntOr("Buit", 0);
        hulpjes = tag.getIntOr("Hulpjes", 0);
        bossBar.setName(this.getDisplayName());
    }

    // --- sounds: the Mika sounds, deeper ---

    @Override
    public float getVoicePitch() {
        return 0.55f + this.random.nextFloat() * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIKA_AMBIENT.get();
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
    public int getAmbientSoundInterval() {
        return 160;
    }

    // --- GeckoLib (the guh animations, like the Mika) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> state.setAndContinue(state.isMoving() && !isRiding() ? WALK : IDLE)));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP).triggerableAnim("pounce", POUNCE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Drops the knabbels he stole so far (the tests look at this). */
    public void dropBuit() {
        for (int left = buit; left > 0; left -= 16) {
            this.level().addFreshEntity(new ItemEntity(this.level(), getX(), getY(), getZ(), new ItemStack(ModItems.KAAS_KNABBELS.get(), Math.min(16, left))));
        }
        buit = 0;
    }

    static float lerpYaw(float from, float to, float max) {
        return from + Mth.clamp(Mth.wrapDegrees(to - from), -max, max);
    }
}
