package nl.juiced.guhs.feature.bleekwoud;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.VoorIedereen;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * De Kraakguh: a guh of pale wood, called by a Krakend Guhhartje at night. It only moves while nobody looks at it (it
 * freezes the instant it is seen, with a creak and a little wobble), and it sneaks up on you... to give you a wooden hug:
 * a short Slowness, hearts, a squeak and a creak. Then it stands still for a while before it sneaks again.
 * <p>
 * It is a guh, so it is friendly: it never hurts anyone. It can't be hurt either while its heart stands (a hit only makes
 * kaashars drip on the trunk); it crumbles at dawn, or when its heart is broken. Not tameable, no leash, no name tag.
 */
public class KraakguhEntity extends PathfinderMob implements GeoEntity, KraakWezen {
    private static final EntityDataAccessor<Boolean> MAG_BEWEGEN = SynchedEntityData.defineId(KraakguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WAKKER = SynchedEntityData.defineId(KraakguhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HART = SynchedEntityData.defineId(KraakguhEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    /** The Slowness of a wooden hug (3 seconds), how long it rests afterwards, and how close it has to get. */
    public static final int KNUFFEL_TRAAGHEID = 60, KNUFFEL_PAUZE = 200;
    public static final double KNUFFEL_AFSTAND = 1.7, ZOEK_STRAAL = 24.0;
    /** Player data: got the "wooden hug" line once. */
    public static final String EERSTE_KNUFFEL = "guhs_bleekwoud_knuffel";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kraak.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kraak.walk");
    private static final RawAnimation WIEBEL = RawAnimation.begin().thenPlay("animation.kraak.wiebel");
    private static final RawAnimation KNUFFEL = RawAnimation.begin().thenPlay("animation.kraak.knuffel");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int knuffelPauze;
    private int auTicks;
    private int vastTeller;

    public KraakguhEntity(EntityType<? extends KraakguhEntity> type, Level level) {
        super(type, level);
        this.lookControl = new KraakWezen.Kijk(this);
        this.moveControl = new KraakWezen.Beweeg(this);
        this.jumpControl = new KraakWezen.Spring(this);
        this.xpReward = 0;
        setPathfindingMalus(PathType.DAMAGING, 8f);
        setPathfindingMalus(PathType.POWDER_SNOW, 8f);
        setPathfindingMalus(PathType.LAVA, 8f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0625);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAG_BEWEGEN, true);
        builder.define(WAKKER, false);
        builder.define(HART, Optional.empty());
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new KraakWezen.Lijf(this);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new KraakWezen.Navigatie(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SluipGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
    }

    // =================================================================================================================
    // the heart
    // =================================================================================================================

    @Nullable
    @Override
    public BlockPos hart() {
        return this.entityData.get(HART).orElse(null);
    }

    @Override
    public void bind(BlockPos hart) {
        this.entityData.set(HART, Optional.of(hart));
    }

    @Override
    public boolean magBewegen() {
        return this.entityData.get(MAG_BEWEGEN);
    }

    /** Awake: it has noticed someone and sneaks up on them (its eyes glow orange). */
    public boolean wakker() {
        return this.entityData.get(WAKKER);
    }

    /** Still resting after a hug (ticks). */
    public int knuffelPauze() {
        return knuffelPauze;
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            BlockPos hart = hart();
            if (hart != null && !KraakWezen.heeftHart(this, hart)) {
                verkruimel();
                return;
            }
        }
        super.tick();
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) {
            if (knuffelPauze > 0) {
                knuffelPauze--;
            }
            if (auTicks > 0) {
                auTicks--;
            }
            boolean mocht = this.entityData.get(MAG_BEWEGEN);
            boolean mag = knuffelPauze <= 0 && !KraakWezen.bekeken(this);
            if (mag != mocht) {
                if (mag) {
                    playSound(BleekwoudFeature.KRAAK.get(), 0.5f, 1.2f);
                } else {
                    stopInPlace();
                    if (knuffelPauze <= 0) {       // (seen: a creak and a little wobble; after a hug it just rests)
                        playSound(BleekwoudFeature.KRAAK_BEVRIES.get(), 0.9f, 1f);
                        triggerAnim("action", "wiebel");
                    }
                }
                this.entityData.set(MAG_BEWEGEN, mag);
            }
        }
        super.aiStep();
    }

    @Override
    public void verkruimel() {
        KraakWezen.kruimels(this, false);
        playSound(BleekwoudFeature.KRAAK_VERKRUIMEL.get(), 1f, 1f);
        discard();
    }

    @Override
    public boolean spelerZitVast() {
        vastTeller = KraakWezen.spelerErin(this) ? vastTeller + 1 : 0;
        return vastTeller > 4;
    }

    // =================================================================================================================
    // sneaking up on a player, and the wooden hug
    // =================================================================================================================

    /** Sneaks towards the nearest player (the controls only work while nobody looks) and hugs them when it gets there. */
    private class SluipGoal extends Goal {
        @Nullable
        private Player doel;
        private int herpad;

        SluipGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (knuffelPauze > 0) {
                return false;
            }
            doel = KraakWezen.doelwit(KraakguhEntity.this, hart(), ZOEK_STRAAL);
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return knuffelPauze <= 0 && KraakWezen.isLevend(doel) && !doel.isCreative() && !doel.isSpectator()
                    && doel.distanceToSqr(KraakguhEntity.this) < (ZOEK_STRAAL + 6) * (ZOEK_STRAAL + 6)
                    && (hart() == null || doel.blockPosition().closerThan(hart(), THUIS_STRAAL));
        }

        @Override
        public void start() {
            entityData.set(WAKKER, true);
            herpad = 0;
        }

        @Override
        public void stop() {
            entityData.set(WAKKER, false);
            getNavigation().stop();
            doel = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (doel == null) {
                return;
            }
            getLookControl().setLookAt(doel, 30f, 30f);
            if (--herpad <= 0) {
                herpad = 10;
                getNavigation().moveTo(doel, 1.1);
            }
            if (magBewegen() && distanceToSqr(doel) < KNUFFEL_AFSTAND * KNUFFEL_AFSTAND && getSensing().hasLineOfSight(doel)
                    && doel instanceof ServerPlayer player) {
                knuffel(player);
            }
        }
    }

    /**
     * The wooden hug: a short Slowness (no damage, ever), hearts, a squeak and a creak; the first time a line on the action
     * bar. Then it rests for {@link #KNUFFEL_PAUZE} ticks (frozen, eyes closed) before it sneaks again.
     */
    public void knuffel(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KNUFFEL_TRAAGHEID, 1, false, true, true), this);
        playSound(BleekwoudFeature.KRAAK_KNUFFEL.get(), 1f, 1f);      // (the squeak)
        playSound(BleekwoudFeature.KRAAK_BEVRIES.get(), 0.8f, 1.1f);  // (and the creak)
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, (getX() + player.getX()) / 2, getY() + 1.0, (getZ() + player.getZ()) / 2, 7, 0.4, 0.3, 0.4, 0.02);
        }
        triggerAnim("action", "knuffel");
        if (!GuhQuests.saved(player).getBooleanOr(EERSTE_KNUFFEL, false)) {
            GuhQuests.saved(player).putBoolean(EERSTE_KNUFFEL, true);
            player.sendOverlayMessage(Component.translatable("gui.guhs.bleekwoud.knuffel").withStyle(ChatFormatting.GOLD));
        }
        VoorIedereen.shown(player, "guhmension/bleekwoud_knuffel");
        knuffelPauze = KNUFFEL_PAUZE;
        stopInPlace();
        this.entityData.set(MAG_BEWEGEN, false);
        this.entityData.set(WAKKER, false);
    }

    // =================================================================================================================
    // never hurt, never hurting, never taken away
    // =================================================================================================================

    /**
     * While its heart stands a hit does nothing to it: it creaks, wobbles, and kaashars drips on the trunk of its tree. (A
     * free one from a spawn egg is an ordinary mob; /kill and the void always work.)
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        BlockPos hart = hart();
        if (hart == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (auTicks > 0 || isDeadOrDying() || isRemoved()) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity) && !(direct instanceof Projectile) && !(source.getEntity() instanceof Player)) {
            return false;
        }
        auTicks = 8;
        triggerAnim("action", "wiebel");
        playSound(BleekwoudFeature.KRAAK_AU.get(), 1f, 1f);
        GuhhartjeBlockEntity be = KraakWezen.hartVan(this, hart);
        if (be != null) {
            be.wezenGeraakt();
        }
        return true;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;       // (your aggressive guhs, golems... leave it alone: it is a guh)
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return hart() == null && super.canRide(vehicle);
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return hart() == null && super.canUsePortal(allowPassengers);
    }

    @Override
    public boolean fireImmune() {
        return hart() != null || super.fireImmune();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && magBewegen();
    }

    @Override
    public void push(double x, double y, double z) {
        if (magBewegen()) {
            super.push(x, y, z);
        }
    }

    @Override
    public void knockback(double power, double x, double z) {
        if (magBewegen()) {
            super.knockback(power, x, z);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hart() != null || super.requiresCustomPersistence();
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0f;
    }

    // =================================================================================================================
    // sounds
    // =================================================================================================================

    /** A soft creak now and then, but only while nobody looks (was that behind you?). */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return magBewegen() ? BleekwoudFeature.KRAAK.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BleekwoudFeature.KRAAK_AU.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BleekwoudFeature.KRAAK_VERKRUIMEL.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.CREAKING_STEP, 0.12f, 1.3f);
    }

    // =================================================================================================================
    // saving
    // =================================================================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("hart", BlockPos.CODEC, hart());
        output.putInt("knuffel_pauze", knuffelPauze);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("hart", BlockPos.CODEC).ifPresent(this::bind);
        knuffelPauze = input.getIntOr("knuffel_pauze", 0);
    }

    // =================================================================================================================
    // animations (frozen: no animation at all, it stands like a statue)
    // =================================================================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 3, state -> {
            if (!magBewegen()) {
                return PlayState.STOP;
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP)
                .triggerableAnim("wiebel", WIEBEL).triggerableAnim("knuffel", KNUFFEL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
