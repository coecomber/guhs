package nl.juiced.guhs.feature.einde2.castschurk;

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
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
 * einde2, slice cast_schurk: the base class of the creatures of the Guhvatar story (CONTRACT_E2 11.6; the table:
 * {@link Wezens}): the draakguhs, beer Bosco, Wan Shi Guh and his vosguhs, the face of the Leeuwschildpad-guh, Flopsie, the
 * luchtbizon-guhs and the two fish. Lief: it can't be hurt, pushed away or leashed, never despawns, looks at players and
 * keeps to its Honk (the blocks around the spot a building put it, NBT {@code Honk}); no natural spawn, no loot.
 * <p>
 * hook fase B: {@link #BIJ_KLIK} takes a click, {@link #zetAnimatie} plays an animation of the model (a one-shot plays on
 * top of the lasting pose), {@link #zetStil} keeps it on its mark (a resident), {@link #zetSoort} picks its look. Without a
 * listener a click plays {@code praat} and says one of {@code quest.guhs.einde2_cast_schurk.praatje.<id>.0} / {@code .1}.
 * NBT: {@code Soort}, {@code Stil}, {@code Honk}, {@code Houding}. Models: tools/features/einde2_cast_schurk_wezens.py.
 */
public class CastWezen extends PathfinderMob implements GeoEntity {
    /** Asked in mobInteract, in the order they were added: the first that answers true took the click. */
    public static final List<BiFunction<CastWezen, ServerPlayer, Boolean>> BIJ_KLIK = new CopyOnWriteArrayList<>();
    /** The vanilla scale of each look of the luchtbizon-guh: cream, sandy, grey, dark brown, the calf. */
    public static final float[] BIZON_SCHAAL = {1.0f, 0.9f, 1.1f, 0.8f, 0.55f};
    /** How long a click's talk lasts when "praat" is a pose of its own (the Leeuwschildpad-guh talks slowly). */
    static final int PRAAT_HOUDING = 60;

    private static final EntityDataAccessor<String> DATA_HOUDING = SynchedEntityData.defineId(CastWezen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(CastWezen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(CastWezen.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Never null: an entity type that is not in the table behaves like Bosco. */
    protected final Wezens wezen;
    private int honk;
    private boolean honkGezet;
    /** Server: the one-shot that plays now and the game time it ends; a talking pose a click set and when it stops. */
    private String eenmalig = "";
    private long eenmaligTot;
    private long praatTot = -1;
    private long laatsteKlik = Long.MIN_VALUE / 2;

    public CastWezen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        Wezens w = Wezens.van(type);
        this.wezen = w == null ? Wezens.BOSCO : w;
        setPersistenceRequired();
        if (wezen == Wezens.LEEUWSCHILDPAD_GUH) {
            noPhysics = true;                                   // (laid over a head of blocks: no collision, no gravity)
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public Wezens wezens() {
        return wezen;
    }

    /** Its entity id ("draakguh", "vosguh" ...). */
    public String wie() {
        return wezen.id();
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
        if (wezen == Wezens.LEEUWSCHILDPAD_GUH) {
            noPhysics = true;                                   // (Entity.load has just read NoGravity: false)
            setNoGravity(true);
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
     * Plays this animation of the model: "idle", "praat" or one of its row in {@link Wezens}. A pose or a loop (0 ticks in
     * the table) stays until another one is set; anything else plays once on top of it. A name it does not have does nothing.
     */
    public void zetAnimatie(String naam) {
        if (level().isClientSide() || !wezen.heeft(naam)) {
            return;
        }
        praatTot = -1;
        if (wezen.blijft(naam)) {
            entityData.set(DATA_HOUDING, naam);
            eenmalig = "";
            if (naam.equals("vlieg") || houdingVliegt()) {
                getNavigation().stop();
            }
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

    /** A creature that stands still on its mark (no strolling, saved); it still turns its head to players. */
    public void zetStil(boolean stil) {
        entityData.set(DATA_STIL, stil);
        if (stil) {
            getNavigation().stop();
        }
    }

    public boolean stil() {
        return entityData.get(DATA_STIL);
    }

    /** Its look, 0 .. {@link Wezens#soorten} - 1 (always 0 for a creature with one look). */
    public int soort() {
        return wezen.soorten <= 0 ? 0 : Math.floorMod(Math.max(0, entityData.get(DATA_SOORT)), wezen.soorten);
    }

    public void zetSoort(int soort) {
        if (wezen.soorten <= 0) {
            return;
        }
        int s = Math.floorMod(soort, wezen.soorten);
        entityData.set(DATA_SOORT, s);
        if (wezen == Wezens.LUCHTBIZON_GUH) {
            AttributeInstance schaal = getAttribute(Attributes.SCALE);
            if (schaal != null) {
                schaal.setBaseValue(BIZON_SCHAAL[s]);
                refreshDimensions();
            }
        }
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

    /** The two draakguhs have names of their own (Ran and Shaw, by look). */
    @Override
    protected Component getTypeName() {
        if (wezen == Wezens.DRAAKGUH) {
            return Component.translatable("entity.guhs.draakguh." + soort());
        }
        return super.getTypeName();
    }

    // --- being lief --------------------------------------------------------------------------------------------------------

    /** Does it walk around (bear, fox, gorilla goat, bison) or stay where it is (dragon, owl, the face, the fish swim on their own)? */
    protected boolean loopt() {
        return wezen == Wezens.BOSCO || wezen == Wezens.VOSGUH || wezen == Wezens.FLOPSIE || wezen == Wezens.LUCHTBIZON_GUH;
    }

    @Override
    protected void registerGoals() {
        if (wezen == Wezens.LEEUWSCHILDPAD_GUH || wezen == Wezens.MAANVIS || wezen == Wezens.OCEAANVIS) {
            return;                                                 // (a face of blocks, and fish that swim their own ring)
        }
        goalSelector.addGoal(0, new FloatGoal(this));
        if (loopt()) {
            goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9) {
                @Override
                public boolean canUse() {
                    return magLopen() && super.canUse();
                }
            });
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.002f) {
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
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** May it stroll right now (not on its mark, not in a pose)? */
    protected boolean magLopen() {
        String houding = houding();
        return !stil() && !isPassenger() && (houding.equals("idle") || houding.equals(wezen.beweegLoop()));
    }

    /** The bison hovers at its spot while it "flies". */
    private boolean houdingVliegt() {
        return wezen == Wezens.LUCHTBIZON_GUH && houding().equals("vlieg");
    }

    @Override
    public void tick() {
        if (wezen == Wezens.LEEUWSCHILDPAD_GUH) {
            noPhysics = true;
            setNoGravity(true);
        }
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (wezen.textuurPerSoort() && entityData.get(DATA_SOORT) < 0) {
            zetSoort(random.nextInt(wezen.soorten));              // (a look of its own the first time it ticks)
        }
        if (honk > 0 && !honkGezet) {
            honkGezet = true;
            setHomeTo(blockPosition(), honk);                       // (its Honk: around where the building put it)
        }
        if (!magLopen() && !getNavigation().isDone()) {
            getNavigation().stop();
        }
        if (wezen == Wezens.LUCHTBIZON_GUH) {
            boolean vliegt = houdingVliegt();
            if (isNoGravity() != vliegt) {
                setNoGravity(vliegt);
            }
            if (vliegt) {
                setDeltaMovement(getDeltaMovement().multiply(0.5, 0.0, 0.5));
            }
        }
        if (praatTot >= 0 && level().getGameTime() >= praatTot) {
            praatTot = -1;
            if (houding().equals("praat")) {
                entityData.set(DATA_HOUDING, "idle");
            }
        }
        if (wezen == Wezens.DRAAKGUH && "vuur".equals(eenmalig) && level().getGameTime() < eenmaligTot && tickCount % 2 == 0) {
            vuurAdem((ServerLevel) level());
        }
    }

    /** The draakguh's breath of fire in every colour: particles only, lukewarm (it hurts nobody). */
    private void vuurAdem(ServerLevel level) {
        Vec3 kijk = Vec3.directionFromRotation(0, getYHeadRot()).scale(2.5);
        double x = getX() + kijk.x, y = getY() + 3.4, z = getZ() + kijk.z;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.5, 0.3, 0.5, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 4, 0.5, 0.3, 0.5, 0.05);
        level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 4, 0.6, 0.4, 0.6, 0.2);
        level.sendParticles(ParticleTypes.GLOW, x, y, z, 3, 0.6, 0.4, 0.6, 0.1);
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
        for (BiFunction<CastWezen, ServerPlayer, Boolean> klik : BIJ_KLIK) {
            if (Boolean.TRUE.equals(klik.apply(this, sp))) {
                return InteractionResult.SUCCESS;                   // hook fase B: the story took the click
            }
        }
        praatje(sp);
        return InteractionResult.SUCCESS;
    }

    /** The small talk of phase A, with the talking animation. */
    protected void praatje(ServerPlayer player) {
        if (wezen != Wezens.LEEUWSCHILDPAD_GUH && wezen != Wezens.MAANVIS && wezen != Wezens.OCEAANVIS) {
            getLookControl().setLookAt(player);
        }
        if (wezen.blijft("praat")) {
            if (houding().equals("idle")) {
                zetAnimatie("praat");
                praatTot = level().getGameTime() + PRAAT_HOUDING;
            }
        } else {
            zetAnimatie("praat");
        }
        GuhQuests.say(player, this, CastSchurkRol.PRAATJE + wie() + "." + random.nextInt(2));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;                                                 // (they only speak when spoken to)
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
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
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

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    /** The looping animation right now: its lasting pose, else walking (or swimming) or standing. */
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
        controllers.add(new AnimationController<CastWezen>("beweeg", 4,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop(beweging(state.isMoving() || isBezig())))));
        if (wezen.soorten > 0 && !wezen.textuurPerSoort()) {
            controllers.add(new AnimationController<CastWezen>("soort", 0,
                    state -> state.setAndContinue(RawAnimation.begin().thenLoop("soort_" + soort()))));
        }
        AnimationController<CastWezen> actie = new AnimationController<CastWezen>("actie", 2, state -> PlayState.STOP);
        for (String naam : wezen.animaties()) {
            if (!wezen.blijft(naam)) {
                actie.triggerableAnim(naam, RawAnimation.begin().thenPlay(naam));
            }
        }
        controllers.add(actie);
    }

    /** Moving on its own (the fish swim without a path). */
    protected boolean isBezig() {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** (A wrapped angle in degrees, for the fish.) */
    static float hoek(double dx, double dz) {
        return Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f);
    }
}
