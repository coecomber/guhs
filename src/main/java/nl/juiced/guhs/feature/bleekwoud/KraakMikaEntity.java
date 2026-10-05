package nl.juiced.guhs.feature.bleekwoud;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.world.VoorIedereen;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

/**
 * De Kraak-Mika: the creature of a soured heart (verzuurd_guhhartje). A Mika of pale wood: like the Kraakguh it only moves
 * while nobody looks, and like every Mika it only shoves ({@link MikaEntity#doHurtTarget}: a push, never damage, NJEG).
 * <p>
 * It can't be hurt while its heart stands, so that a fight never drags on: after {@link #KLAPPEN_GENOEG} hits (yours, or
 * your aggressive guhs') it has had enough for tonight, crumbles back into its tree, and the heart only calls a new one the
 * next night. Break the heart and it is gone for good.
 */
public class KraakMikaEntity extends MikaEntity implements KraakWezen {
    private static final EntityDataAccessor<Boolean> MAG_BEWEGEN = SynchedEntityData.defineId(KraakMikaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WAKKER = SynchedEntityData.defineId(KraakMikaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HART = SynchedEntityData.defineId(KraakMikaEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    /** After this many hits it has had enough for tonight. */
    public static final int KLAPPEN_GENOEG = 8;
    /** Ticks between two shoves (vanilla's Creaking: 40). */
    public static final int DUW_PAUZE = 40;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kraak.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kraak.walk");
    private static final RawAnimation WIEBEL = RawAnimation.begin().thenPlay("animation.kraak.wiebel");
    private static final RawAnimation DUW = RawAnimation.begin().thenPlay("animation.kraak.knuffel");

    private int klappen;
    private int auTicks;
    private int duwPauze;
    private int vastTeller;

    public KraakMikaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.lookControl = new KraakWezen.Kijk(this);
        this.moveControl = new KraakWezen.Beweeg(this);
        this.jumpControl = new KraakWezen.Spring(this);
        this.xpReward = 0;
        this.setCustomName(Component.translatable("entity.guhs.kraak_mika"));
        setPathfindingMalus(PathType.DAMAGING, 8f);
        setPathfindingMalus(PathType.POWDER_SNOW, 8f);
        setPathfindingMalus(PathType.LAVA, 8f);
    }

    public static AttributeSupplier.Builder createKraakAttributes() {
        return MikaEntity.createAttributes().add(Attributes.STEP_HEIGHT, 1.0625).add(Attributes.FOLLOW_RANGE, 32.0);
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

    /** (not the Mika's goals: its melee goal would shove while it stands frozen) */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SluipDuwGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** (a Mika gets a random size when it spawns: this one keeps the size of its tree's wood) */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        this.getAttribute(Attributes.SCALE).setBaseValue(1.0);
        this.refreshDimensions();
        return result;
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

    /** Awake: it is after someone (its eyes glow). */
    public boolean wakker() {
        return this.entityData.get(WAKKER);
    }

    /** How many hits it took tonight. */
    public int klappen() {
        return klappen;
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
            if (auTicks > 0) {
                auTicks--;
            }
            if (duwPauze > 0) {
                duwPauze--;
            }
            boolean mocht = this.entityData.get(MAG_BEWEGEN);
            boolean mag = !KraakWezen.bekeken(this);
            if (mag != mocht) {
                if (mag) {
                    playSound(BleekwoudFeature.KRAAK.get(), 0.5f, 0.8f);
                } else {
                    stopInPlace();
                    playSound(BleekwoudFeature.KRAAK_BEVRIES.get(), 0.9f, 0.8f);
                    triggerAnim("action", "wiebel");
                }
                this.entityData.set(MAG_BEWEGEN, mag);
            }
            this.entityData.set(WAKKER, KraakWezen.isLevend(getTarget()));
        }
        super.aiStep();
    }

    @Override
    public void verkruimel() {
        KraakWezen.kruimels(this, true);
        playSound(BleekwoudFeature.KRAAK_VERKRUIMEL.get(), 1f, 0.8f);
        discard();
    }

    @Override
    public boolean spelerZitVast() {
        vastTeller = KraakWezen.spelerErin(this) ? vastTeller + 1 : 0;
        return vastTeller > 4;
    }

    // =================================================================================================================
    // sneaking up and shoving
    // =================================================================================================================

    /** Sneaks towards its target while nobody looks, and shoves when it gets there (never while it stands frozen). */
    private class SluipDuwGoal extends Goal {
        private int herpad;

        SluipDuwGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return KraakWezen.isLevend(getTarget());
        }

        @Override
        public void start() {
            herpad = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity doel = getTarget();
            if (doel == null) {
                return;
            }
            BlockPos hart = hart();
            if (hart != null && !doel.blockPosition().closerThan(hart, THUIS_STRAAL)) {
                setTarget(null);        // (it never follows anyone away from its heart)
                return;
            }
            getLookControl().setLookAt(doel, 30f, 30f);
            if (--herpad <= 0) {
                herpad = 10;
                getNavigation().moveTo(doel, 1.15);
            }
            if (magBewegen() && duwPauze <= 0 && isWithinMeleeAttackRange(doel) && getSensing().hasLineOfSight(doel)
                    && level() instanceof ServerLevel server) {
                duwPauze = DUW_PAUZE;
                doHurtTarget(server, doel);
            }
        }
    }

    /** The Mika's shove (a push and a little hop back, the Mika's NJEG; no damage), and the "shoved" advancement. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean result = super.doHurtTarget(level, target);
        playSound(BleekwoudFeature.KRAAK_BEVRIES.get(), 0.8f, 0.7f);
        if (target instanceof ServerPlayer player) {
            VoorIedereen.shown(player, "guhmension/bleekwoud_geduwd");
        }
        return result;
    }

    // =================================================================================================================
    // can't be hurt while its heart stands, but it gives up after enough hits
    // =================================================================================================================

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
        playSound(BleekwoudFeature.KRAAK_AU.get(), 1f, 0.8f);
        if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player p && p.isCreative())) {
            setLastHurtByMob(attacker);
        }
        GuhhartjeBlockEntity be = KraakWezen.hartVan(this, hart);
        if (be != null) {
            be.wezenGeraakt();
            if (++klappen >= KLAPPEN_GENOEG) {
                be.klaarVoorVannacht();     // (it crumbles; a new one next night)
            }
        }
        return true;
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
    // sounds: the Mika's own NJEG when it shoves (MikaEntity), wood for the rest
    // =================================================================================================================

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
    public float getVoicePitch() {
        return 0.75f + this.random.nextFloat() * 0.1f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.CREAKING_STEP, 0.12f, 1.0f);
    }

    // =================================================================================================================
    // saving
    // =================================================================================================================

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("hart", BlockPos.CODEC, hart());
        output.putInt("klappen", klappen);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("hart", BlockPos.CODEC).ifPresent(this::bind);
        klappen = input.getIntOr("klappen", 0);
    }

    // =================================================================================================================
    // animations (frozen: none; "pounce" is what MikaEntity triggers when it shoves)
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
                .triggerableAnim("wiebel", WIEBEL).triggerableAnim("pounce", DUW));
    }
}
