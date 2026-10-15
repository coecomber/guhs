package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice geestmodellen: the base class of the creatures of the Geestenwereld (DESIGN_EINDE2 5; the table:
 * {@link GeestWezens}): Koh, Hei Bai-guh, Fang, the spirit koi Tui and La, the libelkonijnguhs, the lantaarngeesten and the
 * schaduwgeesten. Mysterious, never mean: it can't be hurt, pushed away or leashed, never despawns, looks at players and keeps
 * to its Honk (the blocks around the spot it was put, NBT {@code Honk}); no natural spawn, no loot. Only a schaduwgeest ever
 * shoves (and then only that: {@link SchaduwgeestEntity}).
 * <p>
 * hook fase B: {@link #BIJ_KLIK} takes a click, {@link #zetAnimatie} plays an animation of the model (a one-shot plays on
 * top of the lasting pose), {@link #zetStil} keeps it on its mark (a resident), {@link #zetSoort} picks its look. Without a
 * listener a click plays {@code praat} and says one of {@code quest.guhs.einde2_geestmodellen.praatje.<id>.0} / {@code .1}.
 * NBT: {@code Soort}, {@code Stil}, {@code Honk}, {@code Houding}. Models: tools/features/einde2_geestmodellen_wezens.py.
 */
public class GeestWezen extends PathfinderMob implements GeoEntity {
    /** Asked in mobInteract, in the order they were added: the first that answers true took the click. */
    public static final List<BiFunction<GeestWezen, ServerPlayer, Boolean>> BIJ_KLIK = new CopyOnWriteArrayList<>();
    public static final String PRAATJE = "quest.guhs.einde2_geestmodellen.praatje.";

    private static final EntityDataAccessor<String> DATA_HOUDING = SynchedEntityData.defineId(GeestWezen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(GeestWezen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(GeestWezen.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Never null: an entity type that is not in the table behaves like a lantaarngeest. */
    protected final GeestWezens wezen;
    private int honk;
    private boolean honkGezet;
    /** Server: the one-shot that plays now and the game time it ends. */
    private String eenmalig = "";
    private long eenmaligTot;
    private long laatsteKlik = Long.MIN_VALUE / 2;

    public GeestWezen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        GeestWezens w = GeestWezens.van(type);
        this.wezen = w == null ? GeestWezens.LANTAARNGEEST : w;
        setPersistenceRequired();
        if (zweeft()) {
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public GeestWezens wezens() {
        return wezen;
    }

    /** Its entity id ("koh", "fang" ...). */
    public String wie() {
        return wezen.id();
    }

    /** Does it float (no gravity: Fang, the koi, the libelkonijnguh, the lantern)? */
    public boolean zweeft() {
        return wezen == GeestWezens.FANG || wezen == GeestWezens.GEEST_KOI || wezen == GeestWezens.LIBELKONIJNGUH
                || wezen == GeestWezens.LANTAARNGEEST;
    }

    // --- synced data, save -------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOUDING, "idle");
        builder.define(DATA_SOORT, -1);
        builder.define(DATA_STIL, false);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Houding", houding());
        tag.putBoolean("Stil", stil());
        tag.putInt("Honk", honk);
        tag.putBoolean("HonkGezet", honkGezet);
        if (wezen.soorten > 0) {
            tag.putInt("Soort", soort());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (zweeft()) {
            setNoGravity(true);                                     // (Entity.load has just read NoGravity: false)
        }
        String houding = tag.getStringOr("Houding", "idle");
        entityData.set(DATA_HOUDING, wezen.blijft(houding) ? houding : "idle");
        entityData.set(DATA_STIL, tag.getBooleanOr("Stil", false));
        honk = Math.max(0, tag.getIntOr("Honk", 0));
        honkGezet = tag.getBooleanOr("HonkGezet", false);
        if (wezen.soorten > 0) {
            int soort = tag.getIntOr("Soort", -1);
            if (soort >= 0) {
                zetSoort(soort);
            }
        }
    }

    // --- what phase B sets -------------------------------------------------------------------------------------------------

    /**
     * Plays this animation of the model: "idle", "praat" or one of its row in {@link GeestWezens}. A pose or a loop (0 ticks
     * in the table) stays until another one is set; anything else plays once on top of it. A name it does not have does nothing.
     */
    public void zetAnimatie(String naam) {
        if (level().isClientSide() || !wezen.heeft(naam)) {
            return;
        }
        if (wezen.blijft(naam)) {
            entityData.set(DATA_HOUDING, naam);
            eenmalig = "";
            getNavigation().stop();
            return;
        }
        eenmalig = naam;
        eenmaligTot = level().getGameTime() + wezen.duur(naam);
        triggerAnim("actie", naam);
    }

    /** The animation that plays now: the one-shot while it lasts (server), else the lasting one. */
    public String animatie() {
        if (!eenmalig.isEmpty() && level().getGameTime() < eenmaligTot) {
            return eenmalig;
        }
        return houding();
    }

    /** The lasting animation ("idle" when nothing was set). */
    public String houding() {
        return entityData.get(DATA_HOUDING);
    }

    /** A creature that stands still on its mark (no strolling or drifting, saved); it still turns its head to players. */
    public void zetStil(boolean stil) {
        entityData.set(DATA_STIL, stil);
        if (stil) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
        }
    }

    public boolean stil() {
        return entityData.get(DATA_STIL);
    }

    /** Its look, 0 .. {@link GeestWezens#soorten} - 1 (always 0 for a creature with one look). */
    public int soort() {
        return wezen.soorten <= 0 ? 0 : Math.floorMod(Math.max(0, entityData.get(DATA_SOORT)), wezen.soorten);
    }

    public void zetSoort(int soort) {
        if (wezen.soorten <= 0) {
            return;
        }
        entityData.set(DATA_SOORT, Math.floorMod(soort, wezen.soorten));
    }

    /** Was a look ever set (by NBT, a command or phase B)? */
    protected boolean soortGezet() {
        return entityData.get(DATA_SOORT) >= 0;
    }

    protected boolean isSoortData(EntityDataAccessor<?> key) {
        return DATA_SOORT.equals(key);
    }

    /** How far it strays from where it was put (0: no home). */
    public void zetHonk(int blokken) {
        honk = Math.max(0, blokken);
        honkGezet = true;
        if (honk > 0) {
            setHomeTo(blockPosition(), honk);
        } else {
            clearHome();
        }
    }

    public int honk() {
        return honk;
    }

    @Nullable
    public BlockPos thuisplek() {
        return hasHome() ? getHomePosition() : null;
    }

    // --- being mysterious and lief -----------------------------------------------------------------------------------------

    /** Does it stroll around on the ground (the little Hei Bai-guh, a schaduwgeest)? */
    protected boolean loopt() {
        return wezen == GeestWezens.SCHADUWGEEST || wezen == GeestWezens.HEI_BAI_GUH;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (loopt()) {
            goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9) {
                @Override
                public boolean canUse() {
                    return magLopen() && super.canUse();
                }
            });
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.002f) {
                @Override
                public boolean canUse() {
                    return magLopen() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return magLopen() && super.canContinueToUse();
                }
            });
        }
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, wezen == GeestWezens.KOH ? 16.0f : 10.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** May it stroll right now (not on its mark, not in a pose)? */
    protected boolean magLopen() {
        return !stil() && !isPassenger() && houding().equals("idle");
    }

    @Override
    public void tick() {
        if (zweeft() && !isNoGravity()) {
            setNoGravity(true);
        }
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (wezen.textuurPerSoort && !soortGezet()) {
            zetSoort(random.nextInt(wezen.soorten));              // (a look of its own the first time it ticks)
        }
        if (honk > 0 && !honkGezet) {
            honkGezet = true;
            setHomeTo(blockPosition(), honk);                       // (its Honk: around where the building put it)
        }
        if (!magLopen() && !getNavigation().isDone()) {
            getNavigation().stop();
        }
        if (zweeft() && !isVehicle()) {
            setDeltaMovement(getDeltaMovement().scale(0.6));        // (a floating spirit drifts to a stop)
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (level().getGameTime() - laatsteKlik < 5) {
            return InteractionResult.SUCCESS;
        }
        laatsteKlik = level().getGameTime();
        for (BiFunction<GeestWezen, ServerPlayer, Boolean> klik : BIJ_KLIK) {
            if (Boolean.TRUE.equals(klik.apply(this, sp))) {
                return InteractionResult.SUCCESS;                   // hook fase B: the story took the click
            }
        }
        praatje(sp);
        return InteractionResult.SUCCESS;
    }

    /** The small talk of phase A, with the talking animation. */
    protected void praatje(ServerPlayer player) {
        getLookControl().setLookAt(player);
        zetAnimatie("praat");
        GuhQuests.say(player, this, PRAATJE + wie() + "." + random.nextInt(2));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;                                                 // (spirits are silent until spoken to)
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);         // (/kill and the void still tidy one up)
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            discard();                                               // (creative: a sneak-hit takes it away)
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    // --- drawing far away ----------------------------------------------------------------------------------------------------

    /** Big spirits are drawn from further away than their hitbox alone would allow. */
    @Override
    public boolean shouldRenderAtSqrDistance(double afstand) {
        double ver = switch (wezen) {
            case KOH, FANG, GEEST_KOI -> 160.0;
            case HEI_BAI_GUH -> 96.0;
            default -> 64.0;
        };
        ver *= getScale();
        return afstand < ver * ver;
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    /** The looping animation right now: its lasting pose, else flying (or swimming) when it moves, or its idle. */
    protected String beweging(boolean beweegt) {
        String houding = houding();
        if (!houding.equals("idle")) {
            return houding;
        }
        String loop = wezen.beweegLoop();
        return loop != null && beweegt ? loop : "idle";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeestWezen>("beweeg", 4,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop(beweging(state.isMoving() || isBezig())))));
        AnimationController<GeestWezen> actie = new AnimationController<GeestWezen>("actie", 2, state -> PlayState.STOP);
        for (String naam : wezen.animaties()) {
            if (!wezen.blijft(naam)) {
                actie.triggerableAnim(naam, RawAnimation.begin().thenPlay(naam));
            }
        }
        controllers.add(actie);
    }

    /** Moving on its own, without a path (the koi swim, the libelkonijnguh flits). */
    protected boolean isBezig() {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** (A wrapped yaw in degrees for a step dx, dz.) */
    static float hoek(double dx, double dz) {
        return Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f);
    }

    /** Turns body and head along a step (the creatures that move without a path). */
    protected void kijkLangs(Vec3 stap) {
        if (stap.horizontalDistanceSqr() < 1e-6) {
            return;
        }
        float yaw = hoek(stap.x, stap.z);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }
}
