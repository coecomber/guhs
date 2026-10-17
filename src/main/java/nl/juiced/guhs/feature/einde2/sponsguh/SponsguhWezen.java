package nl.juiced.guhs.feature.einde2.sponsguh;

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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice sponsguh_wezens: the base class of the whole cast of the Sponsguh story (DESIGN 5b). Every character is its
 * own entity type on this one class (the table: {@link Cast}; the ones with something of their own extend it): lief, can't
 * be hurt or pushed away, looks at players, strolls a little and keeps to its Honk (the blocks around the spot a building
 * put it), no natural spawn. Phase A only makes them EXIST: who says what and when is the story's.
 * <p>
 * hook fase B: every dialogue and scene plays an animation with {@link #zetAnimatie} and answers a click through
 * {@link #BIJ_KLIK}; {@link #zetStil} keeps a character on its mark. Without a listener a click plays {@code praat} and
 * says the one fixed line {@code gui.guhs.einde2_sponsguh_wezens.praatje.<id>}.
 * <p>
 * Models, animations and textures: tools/features/einde2_sponsguh_wezens_modellen.py. A flag or a look shows and hides
 * bones through the looping animations vlag_aan / vlag_uit and soort_&lt;n&gt; (controller "vlag").
 */
public class SponsguhWezen extends PathfinderMob implements GeoEntity {
    /** Asked in mobInteract, in the order they were added: the first that answers true took the click. */
    public static final List<BiFunction<SponsguhWezen, ServerPlayer, Boolean>> BIJ_KLIK = new CopyOnWriteArrayList<>();

    private static final EntityDataAccessor<String> DATA_ANIMATIE = SynchedEntityData.defineId(SponsguhWezen.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_VLAG = SynchedEntityData.defineId(SponsguhWezen.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(SponsguhWezen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(SponsguhWezen.class, EntityDataSerializers.BOOLEAN);
    /** sp_cast: the story looks that are on (bit i = {@link Cast#LOOKS}.get(i)); saved as "Looks" (names, comma separated). */
    private static final EntityDataAccessor<Integer> DATA_LOOKS = SynchedEntityData.defineId(SponsguhWezen.class, EntityDataSerializers.INT);
    /** sp_cast: the persistent data key of a look in SpCast's stand-in (a scene's actor NBT may still carry it). */
    public static final String STANDIN_LOOK = "guhs_einde2_sp_cast_look_";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Never null: an entity type that is not in the table behaves like a plain citizen. */
    protected final Cast cast;
    /** The radius of its Honk (template NBT, blocks; 0: none) and whether its home was taken from where it stands. */
    private int honk;
    private boolean honkGezet;
    /** Server: the one-shot animation that plays now and the game time it ends. */
    private String eenmalig = "";
    private long eenmaligTot;
    private long laatsteKlik = Long.MIN_VALUE / 2;
    /** sp_cast: the stand-in's looks (persistent data) were taken over (once). */
    private boolean standInGelezen;
    /** Client (sp_cast): the scene animation the controller saw last, its age, and whether a new one began. */
    private String sceneGezien = "";
    private int sceneTicks;
    private boolean sceneOpnieuw;
    /** Client (sp_cast): the game time a scene's "zwaai" (the engine's own: a swing) reached this actor. */
    private long zwaaiSinds = Long.MIN_VALUE / 2;

    public SponsguhWezen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        Cast c = Cast.van(type);
        this.cast = c == null ? Cast.BIKINIBODEM_GUH : c;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public Cast cast() {
        return cast;
    }

    // --- synced data, save -------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIMATIE, "idle");
        builder.define(DATA_VLAG, false);
        builder.define(DATA_SOORT, -1);
        builder.define(DATA_STIL, false);
        builder.define(DATA_LOOKS, 0);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Animatie", entityData.get(DATA_ANIMATIE));
        tag.putBoolean("Stil", stil());
        tag.putInt("Honk", honk);
        tag.putBoolean("HonkGezet", honkGezet);
        if (cast.vlag != null) {
            tag.putBoolean(cast.vlag, vlag());
        }
        if (cast.soorten > 0) {
            tag.putInt("Soort", soort());
        }
        if (looks() != 0) {
            tag.putString("Looks", String.join(",", lookNamen()));
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String naam = tag.getStringOr("Animatie", "idle");
        entityData.set(DATA_ANIMATIE, cast.heeft(naam) && Cast.DOORLOPEND.contains(naam) ? naam : "idle");
        entityData.set(DATA_STIL, tag.getBooleanOr("Stil", false));
        honk = Math.max(0, tag.getIntOr("Honk", 0));
        honkGezet = tag.getBooleanOr("HonkGezet", false);
        if (cast.vlag != null) {
            zetVlag(tag.getBooleanOr(cast.vlag, standaardVlag()));
        }
        if (cast.soorten > 0) {
            entityData.set(DATA_SOORT, tag.getIntOr("Soort", -1));
        }
        int mask = 0;
        for (String look : tag.getStringOr("Looks", "").split(",")) {
            int bit = Cast.LOOKS.indexOf(look.trim());
            if (bit >= 0 && cast.kanLook(look.trim())) {
                mask |= 1 << bit;
            }
        }
        entityData.set(DATA_LOOKS, mask);
        leesStandIn();                                                   // (NeoForgeData is read before this)
    }

    // --- sp_cast: the story looks (CONTRACT_E2_C 7.2; the API is SpCast) -----------------------------------------------------

    /** The looks that are on, as a mask over {@link Cast#LOOKS}. */
    public int looks() {
        return entityData.get(DATA_LOOKS);
    }

    /** The names of the looks that are on. */
    public java.util.List<String> lookNamen() {
        java.util.List<String> uit = new java.util.ArrayList<>();
        int mask = looks();
        for (int i = 0; i < Cast.LOOKS.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                uit.add(Cast.LOOKS.get(i));
            }
        }
        return uit;
    }

    public boolean heeftLook(String look) {
        int bit = Cast.LOOKS.indexOf(look);
        return bit >= 0 && (looks() & (1 << bit)) != 0;
    }

    /** Switches a look on or off; a look this character's model does not have does nothing. */
    public void zetLook(String look, boolean aan) {
        int bit = Cast.LOOKS.indexOf(look);
        if (bit < 0 || !cast.kanLook(look)) {
            return;
        }
        int mask = looks();
        entityData.set(DATA_LOOKS, aan ? mask | (1 << bit) : mask & ~(1 << bit));
    }

    /** The looks a scene actor carried in SpCast's stand-in form (persistent data {@value #STANDIN_LOOK}&lt;look&gt;). */
    private void leesStandIn() {
        standInGelezen = true;
        net.minecraft.nbt.CompoundTag data = getPersistentData();
        for (String look : Cast.LOOKS) {
            if (data.getBooleanOr(STANDIN_LOOK + look, false)) {
                zetLook(look, true);
                data.remove(STANDIN_LOOK + look);
            }
        }
    }

    // --- sp_cast: the scene animations (a cutscene actor, client) ------------------------------------------------------------

    /**
     * Client: the scene animation this actor plays ({@code Cutscenes.animatie}, set by {@code Cutscene.Builder.animatie}),
     * held while the scene waits for a click, like {@code GuhNpcEntity.sceneAnimatie} (1.4.4). The engine plays "zwaai"
     * itself as a swing: a character with a "zwaai" of its own plays that instead. "" = none.
     */
    public String sceneAnimatie() {
        if (nl.juiced.guhs.feature.verhaal.Cutscenes.gepauzeerd()) {
            return sceneGezien;
        }
        String scene = nl.juiced.guhs.feature.verhaal.Cutscenes.animatie(this);
        int ticks = nl.juiced.guhs.feature.verhaal.Cutscenes.animatieTicks(this);
        if (scene.isEmpty() && cast.heeft("zwaai") && level().getGameTime() - zwaaiSinds < Cast.duur("zwaai")) {
            scene = "zwaai";
            ticks = (int) (level().getGameTime() - zwaaiSinds);
        }
        if (!scene.equals(sceneGezien) || (!scene.isEmpty() && ticks < sceneTicks)) {
            sceneGezien = scene;
            sceneOpnieuw = true;
        }
        sceneTicks = ticks;
        return scene;
    }

    /** Client: did a new scene animation begin since the last ask (once)? */
    public boolean sceneBegonnen() {
        boolean b = sceneOpnieuw;
        sceneOpnieuw = false;
        return b;
    }

    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        super.swing(hand, updateSelf);
        if (level().isClientSide()) {
            zwaaiSinds = level().getGameTime();                         // (a scene's "zwaai": the engine swings the actor)
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        if (cast.vlag != null) {
            zetVlag(standaardVlag());
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // --- the animation phase B sets ----------------------------------------------------------------------------------------

    /**
     * Plays this animation of the model: "idle", "loop", "praat", "lach", "schrik", "juich", or one of the character's own
     * ({@link Cast#eigen}). A pose or a loop ({@link Cast#DOORLOPEND}) stays until another one is set; anything else plays
     * once on top of it. A name this character does not have does nothing.
     */
    public void zetAnimatie(String naam) {
        if (level().isClientSide() || !cast.heeft(naam)) {
            return;
        }
        if (Cast.DOORLOPEND.contains(naam)) {
            entityData.set(DATA_ANIMATIE, naam);
            eenmalig = "";
            return;
        }
        eenmalig = naam;
        eenmaligTot = level().getGameTime() + Cast.duur(naam);
        triggerAnim("actie", naam);
        animatieGeluid(naam);
    }

    /** The sound that goes with an animation of its own (the roar, the puff of the trident...). */
    private void animatieGeluid(String naam) {
        switch (naam) {
            case "brul" -> playSound(E2SponsguhFeature.MONSTER_BRUL.get(), 1.2f, 0.7f + random.nextFloat() * 0.2f);
            case "schrik" -> {
                SoundEvent geluid = geluid();
                if (geluid != null) {
                    playSound(geluid, 0.8f, stem() + 0.4f);
                }
            }
            case "bevries" -> playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK, 0.9f, 0.6f);
            case "zucht" -> playSound(E2SponsguhFeature.MIKA_BROM.get(), 0.7f, 0.6f);
            default -> {
            }
        }
    }

    /** The animation that plays now: the one-shot while it lasts (server), else the lasting one. */
    public String animatie() {
        if (!eenmalig.isEmpty() && level().getGameTime() < eenmaligTot) {
            return eenmalig;
        }
        return entityData.get(DATA_ANIMATIE);
    }

    /** The lasting animation ("idle" when nothing was set). */
    public String houding() {
        return entityData.get(DATA_ANIMATIE);
    }

    /** hook fase B: a character that stands still on its mark (no strolling, saved); it still turns its head to players. */
    public void zetStil(boolean stil) {
        entityData.set(DATA_STIL, stil);
        if (stil) {
            getNavigation().stop();
        }
    }

    public boolean stil() {
        return entityData.get(DATA_STIL);
    }

    // --- the one flag and the looks ------------------------------------------------------------------------------------------

    /** The character's one synced flag (Bevroren, Opgeblazen, Kroon, OpMotor: the named setters are on its own class). */
    public boolean vlag() {
        return entityData.get(DATA_VLAG);
    }

    protected void zetVlag(boolean aan) {
        entityData.set(DATA_VLAG, aan);
    }

    /** Is this synced value the flag (for a subclass whose hitbox follows it)? */
    protected static boolean isVlagData(EntityDataAccessor<?> key) {
        return DATA_VLAG.equals(key);
    }

    /** What the flag is when nothing says otherwise (Neptuguh wears his crown). */
    protected boolean standaardVlag() {
        return false;
    }

    /** Its look, 0 .. {@link Cast#soorten} - 1 (always 0 for a character with one look). */
    public int soort() {
        return cast.soorten <= 0 ? 0 : Math.floorMod(Math.max(0, entityData.get(DATA_SOORT)), cast.soorten);
    }

    public void zetSoort(int soort) {
        if (cast.soorten > 0) {
            entityData.set(DATA_SOORT, Math.floorMod(soort, cast.soorten));
        }
    }

    // --- being lief --------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        eigenDoelen();
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
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** The character's own goals (priority 1-4). */
    protected void eigenDoelen() {
    }

    /** May it stroll right now (not on its mark, not in a pose, not frozen...)? */
    protected boolean magLopen() {
        String houding = houding();
        return !stil() && !isPassenger() && (houding.equals("idle") || houding.equals("loop"));
    }

    @Override
    public void tick() {
        super.tick();
        if (!standInGelezen) {
            leesStandIn();
        }
        if (level().isClientSide()) {
            return;
        }
        if (cast.soorten > 0 && entityData.get(DATA_SOORT) < 0) {
            entityData.set(DATA_SOORT, random.nextInt(cast.soorten));      // (a look of its own the first time it ticks)
        }
        if (honk > 0 && !honkGezet) {
            honkGezet = true;
            setHomeTo(blockPosition(), honk);                               // (its Honk: around where the building put it)
        }
        if (!magLopen() && !getNavigation().isDone()) {
            getNavigation().stop();
        }
        // a lasting pose with a sound of its own: the clarinet (off key), the monsters' song
        String houding = houding();
        if (houding.equals("speel") && tickCount % 44 == 0) {
            playSound(E2SponsguhFeature.OCTO_KLARINET.get(), 0.9f, 0.7f + random.nextFloat() * 0.8f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.2, 0, 0.2, 0);
        } else if (houding.equals("zing") && tickCount % 40 == 0) {
            playSound(E2SponsguhFeature.MONSTER_ZING.get(), 1.0f, 0.6f + random.nextFloat() * 0.3f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.2, getZ(), 2, 0.4, 0, 0.4, 0);
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
        for (BiFunction<SponsguhWezen, ServerPlayer, Boolean> klik : BIJ_KLIK) {
            if (Boolean.TRUE.equals(klik.apply(this, sp))) {
                return InteractionResult.SUCCESS;                           // hook fase B: the story took the click
            }
        }
        if (!eigenKlik(sp)) {
            praatje(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** A click nobody of {@link #BIJ_KLIK} took: something of the character's own (true), or the fixed line. */
    protected boolean eigenKlik(ServerPlayer player) {
        return false;
    }

    /** The one fixed line of phase A, with the talking animation and the character's sound. */
    protected void praatje(ServerPlayer player) {
        getLookControl().setLookAt(player);
        zetAnimatie("praat");
        SoundEvent geluid = geluid();
        if (geluid != null) {
            playSound(geluid, 0.8f, stem());
        }
        GuhQuests.say(player, this, "gui.guhs.einde2_sponsguh_wezens.praatje." + cast.id());
    }

    /** Its voice; null: silent. */
    @Nullable
    protected SoundEvent geluid() {
        return isMika() ? E2SponsguhFeature.MIKA_BROM.get() : E2SponsguhFeature.GUH_BABBEL.get();
    }

    /** The pitch of its voice: big characters low, small ones high. */
    protected float stem() {
        return net.minecraft.util.Mth.clamp(1.7f - cast.hoog * 0.35f, 0.55f, 1.6f) + random.nextFloat() * 0.1f;
    }

    public boolean isMika() {
        return cast.id().endsWith("_mika") || cast == Cast.OCTOLIAM || cast == Cast.PLANKMIKA;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;                                                         // (they only speak when spoken to)
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);                 // (/kill and the void still tidy one up)
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            discard();                                                       // (creative: a sneak-hit takes it away)
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

    /** (The radius of its Honk, for the tests.) */
    public int honk() {
        return honk;
    }

    @Nullable
    public BlockPos thuisplek() {
        return hasHome() ? getHomePosition() : null;
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    /** The looping animation right now: its lasting pose, else walking or standing. */
    protected String beweging(boolean beweegt) {
        String houding = houding();
        if (!houding.equals("idle") && !houding.equals("loop")) {
            return houding;
        }
        return beweegt || houding.equals("loop") ? "loop" : "idle";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SponsguhWezen>("beweeg", 4,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop(beweging(state.isMoving())))));
        if (cast.vlag != null || cast.soorten > 0) {
            controllers.add(new AnimationController<SponsguhWezen>("vlag", 3, state -> state.setAndContinue(RawAnimation.begin().thenLoop(
                    cast.vlag != null ? (vlag() ? "vlag_aan" : "vlag_uit") : "soort_" + soort()))));
        }
        AnimationController<SponsguhWezen> actie = new AnimationController<SponsguhWezen>("actie", 2, state -> PlayState.STOP);
        for (String naam : cast.animaties()) {
            if (!Cast.DOORLOPEND.contains(naam) && !naam.startsWith("vlag_") && !naam.startsWith("soort_")) {
                actie.triggerableAnim(naam, RawAnimation.begin().thenPlay(naam));
            }
        }
        controllers.add(actie);
        // sp_cast: a cutscene's animation (Cutscene.Builder.animatie) on an actor, over everything else while it plays
        controllers.add(new AnimationController<SponsguhWezen>("scene", 2, state -> {
            String naam = sceneAnimatie();
            boolean doorlopend = Cast.DOORLOPEND.contains(naam);
            if (naam.isEmpty() || !cast.heeft(naam) || (!doorlopend && !sceneOpnieuw && sceneTicks > Cast.duur(naam) + 2)) {
                return PlayState.STOP;                                  // (a one-shot that has played: back to the pose below)
            }
            if (sceneBegonnen()) {
                state.controller().reset();
            }
            return state.setAndContinue(doorlopend ? RawAnimation.begin().thenLoop(naam) : RawAnimation.begin().thenPlay(naam));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
