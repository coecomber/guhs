package nl.juiced.guhs.feature.circuit;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.race.RaceGuhEntity;
import nl.juiced.guhs.registry.ModSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Mika-pikker of the Guh-Circuit: a little Mika in a racing bandana that waits beside the track. When a race guh runs
 * past too close, it pinches the guh's VAHOEG (the boost, and a bit of speed: RaceGuhEntity.SCHOK_PIK), giggles and hops
 * off, and a moment later it's back on its spot (poof). It never hurts anyone: it only pinches vaart.
 * <p>
 * A "duwer" (pusher) stands at the top of the Kaasberg's Knabbelhelling and pushes the rolling kaasknabbels down it.
 * Both are put out by the race (CircuitExtra) and go away with it; they're never saved.
 */
public class MikaPikkerEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_DUWER = SynchedEntityData.defineId(MikaPikkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation GIECHEL = RawAnimation.begin().thenPlay("animation.guh.happy");
    /** How long it's off giggling before it pops back to its spot, and how long before it can pinch again. */
    public static final int GIECHEL_TICKS = 30, WACHT_TICKS = 90;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private Vec3 home;
    private float homeYaw;
    private int giechel, cooldown;
    private int pinches;

    public MikaPikkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DUWER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public boolean isDuwer() {
        return this.entityData.get(DATA_DUWER);
    }

    public void setDuwer(boolean duwer) {
        this.entityData.set(DATA_DUWER, duwer);
    }

    /** Its spot beside the track (it looks at the track from there). */
    public void setHome(Vec3 pos, float yaw) {
        this.home = pos;
        this.homeYaw = yaw;
        this.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    @Nullable
    public Vec3 home() {
        return home;
    }

    /** Ready to pinch (on its spot, not giggling, not waiting)? */
    public boolean kanPikken() {
        return !isDuwer() && giechel == 0 && cooldown == 0 && isAlive();
    }

    public int pinches() {
        return pinches;
    }

    /** Pinches a passing race guh's VAHOEG: the guh slows down, the Mika giggles and hops off (nobody gets hurt). */
    public void pik(RaceGuhEntity mount, @Nullable ServerPlayer racer) {
        pinches++;
        mount.schok(RaceGuhEntity.SCHOK_PIK);
        giechel = GIECHEL_TICKS;
        cooldown = WACHT_TICKS;
        Vec3 away = this.position().subtract(mount.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, homeYaw + 180) : away.normalize();
        this.setDeltaMovement(away.scale(0.35).add(0, 0.42, 0));
        this.hasImpulse = true;
        this.triggerAnim("action", "giechel");
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.7f);
            level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1, getZ(), 3, 0.3, 0.2, 0.3, 1);
            level.sendParticles(ParticleTypes.WAX_OFF, mount.getX(), mount.getY() + 0.8, mount.getZ(), 12, 0.5, 0.4, 0.5, 0.1);
        }
        if (racer != null) {
            racer.displayClientMessage(Component.translatable("quest.guhs.circuit.gepikt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** A pusher at the Knabbelhelling gives a kaasknabbel a push (a giggle and a happy wiggle). */
    public void duw() {
        this.triggerAnim("action", "giechel");
        this.level().playSound(null, this, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 1.9f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > 40 && !CircuitExtra.isLive(this)) {
            this.discard();       // its race is over (or it was left behind): poof
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (giechel > 0) {
            if (--giechel == 0 && home != null) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                this.setDeltaMovement(Vec3.ZERO);
                this.teleportTo(home.x, home.y, home.z);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, home.x, home.y + 0.5, home.z, 10, 0.3, 0.3, 0.3, 0.02);
            }
            return;
        }
        if (home != null && this.position().distanceToSqr(home) > 2.25) {
            this.setDeltaMovement(Vec3.ZERO);
            this.teleportTo(home.x, home.y, home.z);
        }
        // on its spot: it keeps an eye on the race guhs coming by (or, a pusher, on the track down the hill)
        RaceGuhEntity near = this.level().getNearestEntity(RaceGuhEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                this, getX(), getY(), getZ(), getBoundingBox().inflate(14, 6, 14));
        if (near != null && !isDuwer()) {
            this.getLookControl().setLookAt(near, 30f, 30f);
        } else {
            this.setYRot(homeYaw);
            this.setYHeadRot(homeYaw);
            this.yBodyRot = homeYaw;
        }
    }

    // --- never hurt, never hurting, never saved -------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            this.level().playSound(null, this, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.8f);
            player.displayClientMessage(Component.translatable(isDuwer() ? "quest.guhs.circuit.duwer.hallo" : "quest.guhs.circuit.pikker.hallo")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIKA_AMBIENT.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.6f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Duwer", isDuwer());
    }

    // --- GeckoLib (the Mika model with the guh animations) ------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP).triggerableAnim("giechel", GIECHEL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
