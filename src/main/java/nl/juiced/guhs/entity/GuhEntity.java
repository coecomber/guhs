package nl.juiced.guhs.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
/**
 * De Guh: a chubby pink plush mouse.
 * <ul>
 *     <li>Spawns in random sizes (SCALE attribute), from ~half a block to ~3 blocks long.</li>
 *     <li>Tame / heal / breed with Kaas Knabbels. Wild: 25 HP, tamed: 1000 HP.</li>
 *     <li>Owner: tap right-click = ride (if big enough and saddled) or sit/stand; sneak + tap = pick it up, hold right-click = Guh menu (client side, see GuhInteractHandler).</li>
 *     <li>Menu toggles: teleport-to-owner, gravity (heavy + squished + no jumping), sit.</li>
 * </ul>
 */
public class GuhEntity extends TamableAnimal implements GeoEntity, Saddleable {
    public static final float WILD_HEALTH = 25f;
    public static final float TAMED_HEALTH = 1000f;
    /** Size range. At scale 1.0 a guh is ~1.45 blocks long (incl. paws, excl. tail). */
    public static final float MIN_SCALE = 0.35f;
    public static final float MAX_SCALE = 2.1f;
    /** Guhs at least this big can be ridden by their owner. */
    public static final float RIDEABLE_SCALE = 1.2f;
    /** Chance (1 in N) that a kaas knabbel tames a wild guh. */
    public static final int TAME_CHANCE = 3;
    /** The ender guh: 1 in N guhs born on the Guh Peaks; 1 in N kaas knabbels tames it (fried knabbels always do). */
    public static final int ENDER_CHANCE = 30, ENDER_TAME_CHANCE = 8;
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> GUH_PEAKS =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, nl.juiced.guhs.Guhs.id("guh_peaks"));
    /** Client: is the rider holding jump (set by GuhsClient; the rider's own client steers a flying guh). */
    public static java.util.function.BooleanSupplier riderJumping = () -> false;
    /** The Koningguh is always a giant (about 2.5 blocks long). */
    public static final float KONING_SCALE = 1.72f;
    /** HP healed per kaas knabbel when tamed. */
    public static final float HEAL_PER_KNABBEL = 100f;

    private static final EntityDataAccessor<Boolean> DATA_TELEPORT = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** Tamed guhs with "wander" off stay put (standing, not sitting): no strolling, no following. */
    private static final EntityDataAccessor<Boolean> DATA_WANDER = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GRAVITY = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SOUNDS = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SOUND_FREQUENCY = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BEHAVIOR = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_RADIUS = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SECRET_NOTE = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PERSONALITY = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    /** Hidden in the verstopguh house (no name above its head, so it can't give itself away through a wall). */
    private static final EntityDataAccessor<Boolean> DATA_HIDDEN = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** Worn clothes, one per GuhClothes.Slot (index into GuhClothes, -1 = nothing). */
    private static final java.util.List<EntityDataAccessor<Integer>> DATA_CLOTHES = java.util.List.of(
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT),    // (2.8: the sixth, HAAR)
            SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT));   // (2.9: the seventh, OREN)
    /** 2.8: the visual flags of the Knus features (feature.knus.GuhHooks: GLANZEND, PYJAMA, ...), saved as "KnusVlaggen". */
    private static final EntityDataAccessor<Integer> DATA_KNUS_VLAGGEN = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    /** 2.8: the hair colour (RGB) of its HAAR piece, -1 = natural (the kapper's dyes), saved as "Haarkleur". */
    private static final EntityDataAccessor<Integer> DATA_HAARKLEUR = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    /** The launch move of a tamed guh mount: 0 = none, 1 = sucking in air, 2 = flying, 3 = dropping down. */
    private static final EntityDataAccessor<Integer> DATA_LAUNCH = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    public static final int LAUNCH_NONE = 0, LAUNCH_CHARGING = 1, LAUNCH_FLYING = 2, LAUNCH_DROPPING = 3;
    /** Emotes (feature/emotes): the emote it is doing now (see GuhEmotes: 0 = none) and its favourite one (-1 = none). */
    private static final EntityDataAccessor<Integer> DATA_EMOTE = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FAVORITE_EMOTE = SynchedEntityData.defineId(GuhEntity.class, EntityDataSerializers.INT);
    /** Sucking in air takes 4 seconds; after a launch the guh needs 10 seconds to recover. */
    public static final int LAUNCH_CHARGE_TICKS = 80, LAUNCH_COOLDOWN = 200, LAUNCH_MAX_FLIGHT = 160;
    public static final double LAUNCH_SPEED = 1.95, LAUNCH_SINK = 0.06;
    public static final int BACKPACK_SIZE = 18;
    private static final net.minecraft.resources.Identifier PERSONALITY_SPEED = nl.juiced.guhs.Guhs.id("personality_speed");
    /** About 1 in this many spawned guhs carries a secret note for the first player it meets. */
    public static final int SECRET_NOTE_CHANCE = 200;
    /** About 1 in this many wild Guhmension guhs already wears an outfit. */
    public static final int OUTFIT_CHANCE = 25;

    /** How a tamed guh reacts to other mobs (set in the Guh menu). */
    public enum Behavior { PASSIVE_FLEE, PASSIVE, NEUTRAL, AGGRESSIVE }

    /** Ticks between ambient sounds per frequency setting (0 = very rare ... 4 = very often). */
    public static final int[] SOUND_INTERVALS = {800, 400, 200, 100, 50};
    public static final int DEFAULT_SOUND_FREQUENCY = 2;
    /** Wild guhs chatter ~20% less than the old 160 ticks. */
    public static final int WILD_SOUND_INTERVAL = 200;
    public static final int MIN_ATTACK_RADIUS = 1, MAX_ATTACK_RADIUS = 20;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.guh.sit");
    private static final RawAnimation HAPPY = RawAnimation.begin().thenPlay("animation.guh.happy");
    private static final RawAnimation SUCK = RawAnimation.begin().thenPlayAndHold("animation.guh.suck");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.guh.fly");
    private static final RawAnimation ENDER_FLAP = RawAnimation.begin().thenLoop("animation.guh.ender_flap");
    private static final RawAnimation ENDER_REST = RawAnimation.begin().thenLoop("animation.guh.ender_rest");
    private static final RawAnimation ZEEMEER_SWIM = RawAnimation.begin().thenLoop("animation.guh.zeemeer_swim");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** Its emotes: timers and what starts / stops them (feature/emotes). */
    public final nl.juiced.guhs.feature.emotes.GuhEmotes emotes = new nl.juiced.guhs.feature.emotes.GuhEmotes(this);

    /** The 18 slots of the backpack (only usable while it wears one; 2.9: the contents belong to this guh). */
    private final net.minecraft.world.SimpleContainer backpack = new net.minecraft.world.SimpleContainer(BACKPACK_SIZE);
    private int launchTicks;
    private int launchCooldown;
    private Vec3 launchDirection = Vec3.ZERO;

    /** Client-side 0..1 smoothing of the "gravity" squish so it plops down instead of snapping. */
    private float squish;
    private float squishO;

    public GuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, WILD_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 0.6)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    /** Spawn on any solid block in daylight-ish light, so guhs show up everywhere (deserts, snow, beaches...). */
    public static boolean checkGuhSpawnRules(EntityType<? extends Mob> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        boolean lightOk = EntitySpawnReason.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8;
        return lightOk && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TELEPORT, true);
        builder.define(DATA_WANDER, true);
        builder.define(DATA_GRAVITY, false);
        builder.define(DATA_SADDLED, false);
        builder.define(DATA_SOUNDS, true);
        builder.define(DATA_SOUND_FREQUENCY, DEFAULT_SOUND_FREQUENCY);
        builder.define(DATA_BEHAVIOR, Behavior.PASSIVE_FLEE.ordinal());
        builder.define(DATA_ATTACK_RADIUS, 8);
        builder.define(DATA_SECRET_NOTE, false);
        builder.define(DATA_VARIANT, GuhVariant.NORMAL.ordinal());
        builder.define(DATA_PERSONALITY, -1);
        builder.define(DATA_HIDDEN, false);
        DATA_CLOTHES.forEach(slot -> builder.define(slot, -1));
        builder.define(DATA_LAUNCH, LAUNCH_NONE);
        builder.define(DATA_EMOTE, 0);
        builder.define(DATA_FAVORITE_EMOTE, -1);
        builder.define(DATA_KNUS_VLAGGEN, 0);
        builder.define(DATA_HAARKLEUR, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return !isZeemeer() && super.canUse(); // (the Zeemeerguh dives instead of bobbing at the surface)
            }
        });
        // wild guhs panic when hurt; tamed ones follow their behaviour setting instead
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.4) {
            @Override
            public boolean canUse() {
                return !GuhEntity.this.isTame() && getPersonality() != GuhPersonality.BRAVE && super.canUse();
            }
        });
        // SHY wild guhs keep their distance from players (unless they bring kaas knabbels)
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, Player.class, 7f, 1.0, 1.3,
                player -> getPersonality() == GuhPersonality.SHY && !isTame() && !isOrderedToSit()
                        && !player.isHolding(ModItems.KAAS_KNABBELS.get()) && !player.isSpectator()
                        && !nl.juiced.guhs.feature.knus.GuhHooks.isBewoner(this)));   // (town residents are used to visitors)
        this.goalSelector.addGoal(1, new GuhFleeGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isTame() && (getBehavior() == Behavior.NEUTRAL || getBehavior() == Behavior.AGGRESSIVE) && super.canUse();
            }
        }.setAlertOthers(GuhEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> getBehavior() == Behavior.AGGRESSIVE && isTame() && wantsToFight(target)
                        && distanceTo(target) <= getAttackRadius()));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new nl.juiced.guhs.feature.emotes.EmoteGoal(this)); // stands still while doing an emote
        this.goalSelector.addGoal(2, new DeliverNoteGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new PersonalityGoals.EatDroppedSnacks(this));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.15, stack -> stack.is(ModItems.KAAS_KNABBELS.get()), false) {
            @Override
            public boolean canUse() {
                return !isOrderedToSit() && mayWander() && super.canUse(); // a sitting (or staying) guh stays put, snacks or not
            }
        });
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 8.0f, 3.0f) {
            @Override
            public boolean canUse() {
                return mayWander() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new PersonalityGoals.Zoomies(this));
        this.goalSelector.addGoal(6, new PersonalityGoals.Curious(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override
            public boolean canUse() {
                // LAZY guhs can't be bothered most of the time
                return !isEnder() && !(isZeemeer() && isInWater()) && mayWander()
                        && (getPersonality() != GuhPersonality.LAZY || GuhEntity.this.random.nextInt(4) == 0) && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, nl.juiced.guhs.feature.onderwater.Zeemeerguh.swimGoal(this));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return isEnder() && mayWander() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        nl.juiced.guhs.feature.knus.GuhHooks.runDoelen(this, this.goalSelector);   // 2.8: the goals of the Knus features
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (!hasPersonality()) {
            setPersonality(GuhPersonality.random(this.random));
        }
        // Guhs placed by a structure (e.g. the giant in the hamster house) keep the size saved in the structure.
        if (spawnType != EntitySpawnReason.STRUCTURE) {
            // Skewed random so most guhs are normal-sized and the huge ones are a nice surprise.
            float r = this.random.nextFloat();
            setGuhScale(MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (float) Math.pow(r, 1.8));
            // variants (and the note guh) only turn up in the Guhmension
            if (level.getLevel().dimension() == nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
                if (this.random.nextInt(SECRET_NOTE_CHANCE) == 0) {
                    setVariant(GuhVariant.BROCOCOLIEF);
                    wear(GuhClothes.PINK_ONESIE);
                    setSecretNote(true); // shhh...
                } else {
                    GuhVariant variant = GuhVariant.roll(this.random);
                    if (variant == GuhVariant.GHOST && level.getLevel().isDay()) {
                        variant = GuhVariant.NORMAL; // ghosts only come out at night
                    }
                    if (level.getBiome(this.blockPosition()).is(GUH_PEAKS) && this.random.nextInt(ENDER_CHANCE) == 0) {
                        variant = GuhVariant.ENDER; // big enough to ride
                        setGuhScale(Math.max(getGuhScale(), RIDEABLE_SCALE + 0.15f + this.random.nextFloat() * 0.4f));
                    }
                    setVariant(variant);
                    if (variant != GuhVariant.ENDER && this.random.nextInt(OUTFIT_CHANCE) == 0) {
                        GuhClothes.WILD_OUTFITS.get(this.random.nextInt(GuhClothes.WILD_OUTFITS.size())).forEach(this::wear);
                    }
                }
            }
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The secret note: a rare guh walks up to you with a paper in its mouth
    // ------------------------------------------------------------------------------------------------------------

    public boolean hasSecretNote() {
        return this.entityData.get(DATA_SECRET_NOTE);
    }

    public void setSecretNote(boolean note) {
        this.entityData.set(DATA_SECRET_NOTE, note);
        if (note) {
            this.setPersistenceRequired(); // don't despawn before delivering it
        }
    }

    /** Whispers to the player (only they see it) and hands over the note: a paper that says "bork". */
    public void deliverSecretNote(Player player) {
        if (!hasSecretNote() || this.level().isClientSide()) {
            return;
        }
        setSecretNote(false);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.type.text", this.getDisplayName(),
                net.minecraft.network.chat.Component.literal("shhh. niet doorvertellen. Lees dit...")));
        ItemStack note = new ItemStack(net.minecraft.world.item.Items.PAPER);
        note.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.translatable("item.guhs.secret_note")
                        .withStyle(s -> s.withItalic(false).withColor(net.minecraft.ChatFormatting.LIGHT_PURPLE)));
        note.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                net.minecraft.network.chat.Component.literal("bork").withStyle(s -> s.withItalic(false).withColor(net.minecraft.ChatFormatting.WHITE)))));
        if (!player.getInventory().add(note)) {
            player.drop(note, false);
        }
        this.playSound(ModSounds.GUH_HAPPY.get(), 1f, this.getVoicePitch());
    }

    public float getGuhScale() {
        return (float) this.getAttributeBaseValue(Attributes.SCALE);
    }

    public void setGuhScale(float scale) {
        // SCALE is a vanilla attribute: it scales the hitbox, the renderer, passengers and is saved with the entity.
        this.getAttribute(Attributes.SCALE).setBaseValue(Mth.clamp(scale, MIN_SCALE, MAX_SCALE));
        this.refreshDimensions(); // vanilla would only do this on the next tick
    }

    /** Big enough (and grown up and tamed) to wear a saddle and be ridden. */
    // --- behaviour ---

    public Behavior getBehavior() {
        int i = this.entityData.get(DATA_BEHAVIOR);
        return Behavior.values()[Math.floorMod(i, Behavior.values().length)];
    }

    public void setBehavior(Behavior behavior) {
        this.entityData.set(DATA_BEHAVIOR, behavior.ordinal());
        if (behavior == Behavior.PASSIVE || behavior == Behavior.PASSIVE_FLEE) {
            this.setTarget(null);
        }
    }

    public int getAttackRadius() {
        return this.entityData.get(DATA_ATTACK_RADIUS);
    }

    public void setAttackRadius(int radius) {
        this.entityData.set(DATA_ATTACK_RADIUS, Mth.clamp(radius, MIN_ATTACK_RADIUS, MAX_ATTACK_RADIUS));
    }

    /** Aggressive guhs attack other mobs, but never players, other guhs or their owner's pets. */
    private boolean wantsToFight(LivingEntity target) {
        if (target instanceof GuhEntity || target instanceof QuestGuhEntity || target instanceof Player) {
            return false;
        }
        return !(target instanceof TamableAnimal pet && pet.isTame() && pet.getOwnerUUID() != null
                && pet.getOwnerUUID().equals(this.getOwnerUUID()));
    }

    /** Bigger guhs hit harder (3 damage at normal size). */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * Mth.clamp(getGuhScale(), 0.5f, 4f)
                * (getPersonality() == GuhPersonality.BRAVE ? 1.5f : 1f);
        boolean hit = target.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            this.triggerAnim("action", "happy");
        }
        return hit;
    }

    // --- sounds ---

    public boolean areSoundsEnabled() {
        return this.entityData.get(DATA_SOUNDS);
    }

    public void setSoundsEnabled(boolean enabled) {
        this.entityData.set(DATA_SOUNDS, enabled);
    }

    public int getSoundFrequency() {
        return this.entityData.get(DATA_SOUND_FREQUENCY);
    }

    public void setSoundFrequency(int frequency) {
        this.entityData.set(DATA_SOUND_FREQUENCY, Mth.clamp(frequency, 0, SOUND_INTERVALS.length - 1));
    }

    @Override
    public void playAmbientSound() {
        if (hiddenBy != null) {
            // a hidden guh's giggle is only for the seekers inside (nobody outside the house hears it)
            if (!this.level().isClientSide() && getAmbientSound() != null) {
                nl.juiced.guhs.quest.VerstopGame.soundForSeekers(this, getAmbientSound(), getSoundVolume(), getVoicePitch());
            }
            return;
        }
        if (!this.isTame() || areSoundsEnabled()) {
            super.playAmbientSound();
        }
    }

    // --- armour (iron / diamond / netherite guh armour in the body slot) ---

    @Override
    public boolean isBodyArmorItem(ItemStack stack) {
        return stack.getItem() instanceof nl.juiced.guhs.item.GuhArmorItem;
    }

    @Nullable
    public nl.juiced.guhs.item.GuhArmorItem.Tier getArmorTier() {
        return this.getBodyArmorItem().getItem() instanceof nl.juiced.guhs.item.GuhArmorItem armor ? armor.getTier() : null;
    }

    public boolean isRideable() {
        return this.isTame() && !this.isBaby() && getGuhScale() >= RIDEABLE_SCALE;
    }

    // --- saddle (right-click a rideable guh with a vanilla saddle; riding needs one) ---

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && isRideable();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource soundSource) {
        this.entityData.set(DATA_SADDLED, true);
        if (soundSource != null) {
            this.level().playSound(null, this, SoundEvents.HORSE_SADDLE, soundSource, 0.6f, 1.2f);
        }
    }

    @Override
    public boolean isSaddled() {
        return this.entityData.get(DATA_SADDLED);
    }

    @Override
    protected void dropEquipment() {
        if (nl.juiced.guhs.feature.band.Band.isBandGuh(this)) {
            return;   // 3.0: a tamed guh takes its saddle, armour and backpack along to the wolkjes (band.Wolkjes: nothing is lost)
        }
        super.dropEquipment();
        if (this.isSaddled()) {
            this.spawnAtLocation(Items.SADDLE);
        }
        // 2.9: clothes are unlocks now, so a guh never drops them (wild, structure or tamed); only what is IN its backpack falls out
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            if (!backpack.getItem(i).isEmpty()) {
                this.spawnAtLocation(backpack.removeItemNoUpdate(i));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Menu toggles
    // ------------------------------------------------------------------------------------------------------------

    public boolean isWandering() {
        return this.entityData.get(DATA_WANDER);
    }

    public void setWandering(boolean wander) {
        this.entityData.set(DATA_WANDER, wander);
        if (!wander) {
            this.getNavigation().stop();
        }
    }

    /** Wild guhs always roam; tamed ones only if "wander" is on. */
    public boolean mayWander() {
        return !this.isTame() || isWandering();
    }

    public boolean isTeleportEnabled() {
        return this.entityData.get(DATA_TELEPORT);
    }

    public void setTeleportEnabled(boolean enabled) {
        this.entityData.set(DATA_TELEPORT, enabled);
    }

    /** "Gravity on": the guh is heavy - it can't jump, falls faster and flattens itself against the ground. */
    public boolean isGravityEnabled() {
        return this.entityData.get(DATA_GRAVITY);
    }

    public void setGravityEnabled(boolean enabled) {
        this.entityData.set(DATA_GRAVITY, enabled);
    }

    public void toggleSit() {
        boolean sit = !this.isOrderedToSit();
        this.setOrderedToSit(sit);
        this.setInSittingPose(sit);
        this.jumping = false;
        this.navigation.stop();
        this.setTarget(null);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_GRAVITY.equals(key)) {
            this.refreshDimensions();
        }
        if (DATA_VARIANT.equals(key)) {
            updateFlight();
            updateZeemeerSwimming();
        }
    }

    @Nullable
    private net.minecraft.world.entity.ai.control.MoveControl landControl;
    @Nullable
    private net.minecraft.world.entity.ai.navigation.PathNavigation landNavigation;

    /** The Zeemeerguh swims (on land it walks, in water it dives): see {@link nl.juiced.guhs.feature.onderwater.Zeemeerguh}. */
    private void updateZeemeerSwimming() {
        boolean swimming = this.navigation instanceof net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
        if (isZeemeer() && !swimming) {
            landControl = this.moveControl;
            landNavigation = this.navigation;
            this.moveControl = nl.juiced.guhs.feature.onderwater.Zeemeerguh.moveControl(this, landControl);
            this.navigation = nl.juiced.guhs.feature.onderwater.Zeemeerguh.navigation(this, this.level());
        } else if (!isZeemeer() && swimming && landControl != null) {
            this.moveControl = landControl;
            this.navigation = landNavigation;
        }
    }

    public boolean isZeemeer() {
        return getVariant() == GuhVariant.ZEEMEERGUH;
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return !(isZeemeer() && type == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) && super.canDrownInFluidType(type);
    }

    @Nullable
    private net.minecraft.world.entity.ai.control.MoveControl walkControl;
    @Nullable
    private net.minecraft.world.entity.ai.navigation.PathNavigation walkNavigation;

    /** The ender guh flies (and lands when it has nowhere to go, like a parrot); all other guhs walk. */
    private void updateFlight() {
        boolean flying = this.moveControl instanceof net.minecraft.world.entity.ai.control.FlyingMoveControl;
        if (isEnder() && !flying) {
            walkControl = this.moveControl;
            walkNavigation = this.navigation;
            this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 12, false);
            var nav = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, this.level());
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            this.navigation = nav;
        } else if (!isEnder() && flying && walkControl != null) {
            this.moveControl = walkControl;
            this.navigation = walkNavigation;
            this.setNoGravity(false);
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dims = super.getDefaultDimensions(pose);
        return isGravityEnabled() ? dims.scale(1.1f, 0.75f) : dims;
    }

    @Override
    protected float getJumpPower() {
        return isGravityEnabled() ? 0f : super.getJumpPower();
    }

    @Override
    public void jumpFromGround() {
        if (!isGravityEnabled()) {
            super.jumpFromGround();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return isGravityEnabled() ? super.getDefaultGravity() * 1.6 : super.getDefaultGravity();
    }

    @Override
    public boolean shouldTryTeleportToOwner() {
        // 2.10: a Guhhuisje resident stays at home (its huisje is its home base)
        return isTeleportEnabled() && !nl.juiced.guhs.feature.huisje.Huisjes.isBewoner(this) && super.shouldTryTeleportToOwner();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Taming, feeding, breeding, riding
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (nl.juiced.guhs.feature.verhaal.VerhaalGuhs.isKopieOveral(this)) {
            // 3.0: a story copy (Baltoguh in Nomguh...) is never tamed or fed: its story slice decides what a click does
            return nl.juiced.guhs.feature.verhaal.VerhaalGuhs.klik(this, player, hand);
        }
        if (hiddenBy == null) {
            InteractionResult hooked = nl.juiced.guhs.feature.knus.GuhHooks.runKlik(this, player, hand);   // 2.8: the Knus features first
            if (hooked != InteractionResult.PASS) {
                return hooked;
            }
        }
        if (hiddenBy != null) {
            if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof net.minecraft.server.level.ServerPlayer seeker) {
                nl.juiced.guhs.quest.VerstopGame.foundGuh(this, seeker);
            }
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) && isEnder() && !this.isTame()) {
            // the ender guh can't resist fried knabbels
            if (!this.level().isClientSide()) {
                stack.consume(1, player);
                this.playSound(ModSounds.GUH_EAT.get(), 1f, this.getVoicePitch());
                if (!net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    tamedBy(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) && this.isTame() && this.getHealth() < this.getMaxHealth()) {
            // fried knabbels: instantly back to full health
            if (!this.level().isClientSide()) {
                ItemStack eaten = stack.copyWithCount(1);
                stack.consume(1, player);
                this.setHealth(this.getMaxHealth());
                this.playSound(ModSounds.GUH_EAT.get(), 1f, this.getVoicePitch());
                this.level().broadcastEntityEvent(this, (byte) 7);
                this.triggerAnim("action", "happy");
                if (player instanceof net.minecraft.server.level.ServerPlayer sp && player.getUUID().equals(this.getOwnerUUID())) {
                    nl.juiced.guhs.feature.band.BandEvents.gevoerd(this, sp, eaten);   // 2.10: hearts for feeding
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof nl.juiced.guhs.item.GuhClothingItem clothing && this.isTame() && player.getUUID().equals(this.getOwnerUUID())
                && nl.juiced.guhs.feature.kleding.KledingUnlocks.heeft(player, clothing.getClothes())
                && clothing.getClothes().slot != GuhClothes.Slot.HAAR) {
            // 2.9: a piece you already unlocked goes straight on (the item stays yours); a piece you don't have yet is
            // unlocked by holding right-click (GuhClothingItem), so this passes
            if (!this.level().isClientSide()) {
                nl.juiced.guhs.feature.kleding.KledingKast.trekAan(this, player, clothing.getClothes());
            }
            return InteractionResult.SUCCESS;
        }
        if (isBodyArmorItem(stack) && this.isTame() && player.getUUID().equals(this.getOwnerUUID()) && this.getBodyArmorItem().isEmpty()) {
            if (!this.level().isClientSide()) {
                this.setBodyArmorItem(stack.copyWithCount(1));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!isFood(stack) && this.isTame() && player.getUUID().equals(this.getOwnerUUID()) && nl.juiced.guhs.feature.band.BandEvents.isSnack(stack)) {
            // 2.10: feeding your own guh a snack (#guhs:band/snacks): it munches it, hearts
            if (!this.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                nl.juiced.guhs.feature.band.BandEvents.voer(this, sp, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (!isFood(stack)) {
            // Empty-hand interaction by the owner is handled through the tap/hold packets (GuhActionPayload).
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide()) {
            return InteractionResult.CONSUME;
        }

        if (!this.isTame()) {
            stack.consume(1, player);
            this.playSound(ModSounds.GUH_EAT.get(), 1f, this.getVoicePitch());
            int chance = isEnder() ? ENDER_TAME_CHANCE : getPersonality().tameChance;
            if (this.random.nextInt(chance) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                tamedBy(player);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6); // smoke
            }
            return InteractionResult.SUCCESS;
        }

        if (this.getHealth() < this.getMaxHealth()) {
            ItemStack eaten = stack.copyWithCount(1);
            stack.consume(1, player);
            this.heal(HEAL_PER_KNABBEL);
            this.playSound(ModSounds.GUH_EAT.get(), 1f, this.getVoicePitch());
            this.triggerAnim("action", "happy");
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                nl.juiced.guhs.feature.band.BandEvents.gevoerd(this, sp, eaten);   // 2.10: hearts for feeding
            }
            return InteractionResult.SUCCESS;
        }

        // Full health: vanilla breeding (love mode) / baby growth
        ItemStack eaten = stack.copyWithCount(1);
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) {
            this.playSound(ModSounds.GUH_HAPPY.get(), 1f, this.getVoicePitch());
            this.triggerAnim("action", "happy");
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                nl.juiced.guhs.feature.band.BandEvents.gevoerd(this, sp, eaten);   // 2.10: hearts for feeding
            }
        }
        return result;
    }

    private void tamedBy(Player player) {
        this.tame(player);
        this.navigation.stop();
        this.setTarget(null);
        this.level().broadcastEntityEvent(this, (byte) 7); // hearts
        this.playSound(ModSounds.GUH_HAPPY.get(), 1f, this.getVoicePitch());
        this.triggerAnim("action", "happy");
    }

    /** Owner tapped (short right-click) with an empty-ish hand. Sneaking picks the guh up. */
    public void onOwnerTap(Player player) {
        if (player.isSecondaryUseActive()) {
            if (!this.isVehicle() && !this.isPassenger()) {
                this.playSound(ModSounds.GUH_HAPPY.get(), 1f, this.getVoicePitch());
                player.getInventory().placeItemBackInInventory(PickedUpGuhItem.pickUp(this));
            }
            return;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            nl.juiced.guhs.feature.band.BandEvents.aai(this, sp);   // 2.10: a tap is a little pet (hearts)
        }
        if (isRideable() && isSaddled() && !this.isVehicle()) {
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
            player.startRiding(this);
        } else {
            toggleSit();
        }
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        this.setNoAi(false); // (castle guards stand still until they're yours)
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            nl.juiced.guhs.quest.GuhDex.onTamed(serverPlayer, this);
            nl.juiced.guhs.feature.band.BandEvents.getemd(this, serverPlayer);   // 2.10: the hartjesmeter starts
        }
    }

    @Override
    protected void applyTamingSideEffects() {
        float maxHealth = this.isTame() ? TAMED_HEALTH : WILD_HEALTH;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        this.setHealth(maxHealth);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        GuhEntity baby = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baby != null) {
            float parentsScale = otherParent instanceof GuhEntity other ? (getGuhScale() + other.getGuhScale()) / 2f : getGuhScale();
            baby.setGuhScale(parentsScale + (this.random.nextFloat() - 0.5f) * 0.4f);
            GuhVariant inherited = otherParent instanceof GuhEntity other && this.random.nextBoolean() ? other.getVariant() : getVariant();
            // (3.0: the babies of a story guh are normal guhs: Baltoguh, Guhtwo and the 626-guh are one of a kind)
            baby.setVariant(inherited == GuhVariant.BROCOCOLIEF || inherited.isVerhaalGuh() ? GuhVariant.NORMAL : inherited);
            // mostly like a parent, sometimes a personality of its own
            int roll = this.random.nextInt(10);
            baby.setPersonality(roll < 4 ? getPersonality() : roll < 8 && otherParent instanceof GuhEntity other ? other.getPersonality()
                    : GuhPersonality.random(this.random));
            if (this.isTame() && this.getOwnerUUID() != null) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
                nl.juiced.guhs.feature.band.BandEvents.geboren(baby);   // 2.10: a tamed baby joins the band once it is in the world
            }
        }
        return baby;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (getLaunchState() != LAUNCH_NONE) {
            return null; // while launching the guh moves by itself (the server steers, so every client sees the same)
        }
        if (this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The launch: right-click while riding -> suck in air (4 s) -> shoot off, low and fast, until it hits something.
    // Right-click again while flying -> it stops and drops down. Hitting something = a harmless "plof".
    // ------------------------------------------------------------------------------------------------------------

    public int getLaunchState() {
        return this.entityData.get(DATA_LAUNCH);
    }

    private void setLaunchState(int state) {
        this.entityData.set(DATA_LAUNCH, state);
        this.launchTicks = 0;
    }

    public int getLaunchCooldown() {
        return launchCooldown;
    }

    /** The rider pressed right-click. */
    public void onLaunchPressed(Player rider) {
        if (this.level().isClientSide() || rider.getVehicle() != this || !this.isOwnedBy(rider) || isEnder()) {
            return;
        }
        switch (getLaunchState()) {
            case LAUNCH_NONE -> {
                if (launchCooldown > 0) {
                    rider.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.launch.cooldown",
                            (launchCooldown + 19) / 20));
                    return;
                }
                setLaunchState(LAUNCH_CHARGING);
                this.launchDirection = Vec3.directionFromRotation(0, rider.getYRot()).normalize();
                this.playSound(SoundEvents.BREEZE_INHALE, 1.5f, 0.8f);
            }
            case LAUNCH_FLYING -> {
                setLaunchState(LAUNCH_DROPPING);
                this.setDeltaMovement(0, -0.8, 0);
                this.setNoGravity(false);
            }
            default -> {
            }
        }
    }

    private void tickLaunch() {
        if (launchCooldown > 0) {
            launchCooldown--;
        }
        int state = getLaunchState();
        if (state == LAUNCH_NONE) {
            return;
        }
        if (!this.isVehicle() && state == LAUNCH_CHARGING) {
            setLaunchState(LAUNCH_NONE); // the rider hopped off
            return;
        }
        launchTicks++;
        ServerLevel server = (ServerLevel) this.level();
        switch (state) {
            case LAUNCH_CHARGING -> {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                // air (and bits of fluff) rushing into its mouth
                Vec3 mouth = this.position().add(launchDirection.scale(this.getBbWidth() * 0.6)).add(0, this.getBbHeight() * 0.5, 0);
                for (int i = 0; i < 4; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double dist = 3 + this.random.nextDouble() * 3;
                    Vec3 from = mouth.add(launchDirection.scale(dist)).add(Math.cos(angle) * 1.5, Math.sin(angle) * 1.2, Math.sin(angle) * 1.5);
                    Vec3 speed = mouth.subtract(from).scale(0.12);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, from.x, from.y, from.z, 0, speed.x, speed.y, speed.z, 1.0);
                }
                if (launchTicks % 20 == 0) {
                    this.playSound(SoundEvents.BREEZE_INHALE, 1.2f, 0.7f + launchTicks / 100f);
                }
                if (launchTicks >= LAUNCH_CHARGE_TICKS) {
                    setLaunchState(LAUNCH_FLYING);
                    this.setNoGravity(true);
                    this.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 2f, 0.7f);
                    this.playSound(ModSounds.GUH_HAPPY.get(), 1.5f, this.getVoicePitch());
                }
            }
            case LAUNCH_FLYING -> {
                if (launchTicks > 2 && (this.horizontalCollision || hitSomeone() || launchTicks > LAUNCH_MAX_FLIGHT || this.isInWater())) {
                    plof();
                    return;
                }
                this.setDeltaMovement(launchDirection.x * LAUNCH_SPEED, -LAUNCH_SINK, launchDirection.z * LAUNCH_SPEED);
                this.setYRot((float) (Math.atan2(-launchDirection.x, launchDirection.z) * 180 / Math.PI));
                this.yBodyRot = this.yHeadRot = this.getYRot();
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            }
            case LAUNCH_DROPPING -> {
                // right-click locked the distance: straight down from here, no more drifting forward
                this.setDeltaMovement(0, Math.min(this.getDeltaMovement().y, -0.5), 0);
                if (this.onGround() || this.isInWater() || launchTicks > 200) {
                    plof();
                }
            }
            default -> {
            }
        }
    }

    private boolean hitSomeone() {
        return !this.level().getEntities(this, this.getBoundingBox().inflate(0.4),
                e -> e instanceof LivingEntity && e.isAlive() && !this.hasPassenger(e) && e != this.getOwner()).isEmpty();
    }

    /** Stop with a puff of pink dust: no damage anywhere, but mobs close by get pushed away. */
    private void plof() {
        if (this.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer rider) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(rider, "launched");
        }
        ServerLevel server = (ServerLevel) this.level();
        setLaunchState(LAUNCH_NONE);
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.launchCooldown = LAUNCH_COOLDOWN;
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 40, 0.8, 0.6, 0.8, 0.08);
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, 0.55f, 0.75f), 2f),
                getX(), getY() + getBbHeight() / 2, getZ(), 30, 1, 0.6, 1, 0.1);
        this.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 1.5f, 1.2f);
        this.playSound(ModSounds.GUH_HURT.get(), 1f, this.getVoicePitch() * 1.2f);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3),
                e -> e != this && !this.hasPassenger(e) && e != this.getOwner())) {
            Vec3 away = e.position().subtract(this.position()).normalize();
            e.knockback(1.2, -away.x, -away.z);
            e.hurtMarked = true;
        }
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.5f);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (getLaunchState() != LAUNCH_NONE) {
            return Vec3.ZERO;
        }
        float forward = player.zza <= 0 ? player.zza * 0.5f : player.zza;
        return new Vec3(player.xxa * 0.5f, 0, forward);
    }

    /**
     * The ender guh with its owner on its back flies where the rider looks (W), sideways (A/D), up (jump) and hangs in
     * the air when you let go. This runs on the rider's own client, which tells the server where it went.
     */
    @Override
    public void travel(Vec3 input) {
        nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(this);
        if (gedrag != null && gedrag.travel(this, input)) {
            return; // 3.0: a story variant moves its own way (Guhtwo floats over gaps...)
        }
        if (isEnder() && getLaunchState() == LAUNCH_NONE && getControllingPassenger() instanceof Player rider) {
            Vec3 look = rider.getLookAngle();
            Vec3 left = new Vec3(look.z, 0, -look.x).normalize();
            double speed = rider.isSprinting() ? 0.9 : 0.6;
            Vec3 want = look.scale(Math.max(0, rider.zza) * speed).add(look.scale(Math.min(0, rider.zza) * speed * 0.4))
                    .add(left.scale(rider.xxa * speed * 0.5));
            if (riderJumping.getAsBoolean()) {
                want = want.add(0, 0.45, 0);
            }
            Vec3 motion = getDeltaMovement().scale(0.82).add(want.scale(0.18));
            setDeltaMovement(motion);
            move(net.minecraft.world.entity.MoverType.SELF, motion);
            calculateEntityAnimation(true);
            return;
        }
        if (nl.juiced.guhs.feature.onderwater.Zeemeerguh.travel(this, input)) {
            return; // the Zeemeerguh in water: fast swimming (and ridden: where the rider looks)
        }
        super.travel(input);
    }

    public boolean isEnder() {
        return getVariant() == GuhVariant.ENDER || getVariant() == GuhVariant.VAHOEGE_ENDER;
    }

    /** Getting off a flying ender guh up high: you float down instead of falling. */
    @Override
    protected void removePassenger(net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && isEnder() && passenger instanceof LivingEntity living && !this.onGround()) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(this);
        return gedrag == null ? speed : gedrag.riddenSpeed(this, speed);   // 3.0 (Baltoguh: fast in the snow)
    }

    /** 3.0: a story variant may climb (the 626-guh: walls and ceilings). */
    @Override
    public boolean onClimbable() {
        nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(this);
        return gedrag != null && gedrag.opKlimbaar(this) || super.onClimbable();
    }

    /** Wild guhs in the Guhmension come and go (there are LOTS of them); everywhere else they stay like normal animals. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && this.level().dimension() == ModDimensions.GUHMENSION
                && !nl.juiced.guhs.feature.verhaal.VerhaalGuhs.isKopie(this);   // (3.0: a story copy stays)
    }

    /** Named with a name tag -> the name is always shown above its head. */
    @Override
    public boolean shouldShowName() {
        return !hideName && !this.entityData.get(DATA_HIDDEN); // every guh shows its name: "Guh", its variant, or the name you gave it
    }

    // --- verstopguh: a guh hidden in the verstopguh house by Verstopguhtje ---------------------------------------------

    @Nullable
    private java.util.UUID hiddenBy;

    /** Hides this guh for Verstopguhtje's game: it stays put, can't be hurt and giggles (guh noises) now and then. */
    public void setHiddenBy(java.util.UUID npc, int soundFrequency) {
        this.hiddenBy = npc;
        this.entityData.set(DATA_HIDDEN, true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.setSoundFrequency(soundFrequency);
    }

    @Nullable
    public java.util.UUID getHiddenBy() {
        return hiddenBy;
    }

    @Override
    protected float getSoundVolume() {
        return hiddenBy != null ? 0.35f : super.getSoundVolume(); // soft: a hint, not a siren
    }

    /** Client-side stand-in guhs (e.g. the ones pulling a sled) don't show a name. */
    public boolean hideName;

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return getVariant().displayName();
    }

    public boolean hasPersonality() {
        return this.entityData.get(DATA_PERSONALITY) >= 0;
    }

    public GuhPersonality getPersonality() {
        int i = this.entityData.get(DATA_PERSONALITY);
        return i >= 0 && i < GuhPersonality.values().length ? GuhPersonality.values()[i] : GuhPersonality.PLAYFUL;
    }

    public void setPersonality(GuhPersonality personality) {
        this.entityData.set(DATA_PERSONALITY, personality.ordinal());
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(PERSONALITY_SPEED);
            if (personality.speedBonus != 0) {
                speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PERSONALITY_SPEED,
                        personality.speedBonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    @javax.annotation.Nullable
    public GuhClothes getClothes(GuhClothes.Slot slot) {
        return GuhClothes.byIndex(this.entityData.get(DATA_CLOTHES.get(slot.ordinal())));
    }

    /** Puts on a piece of clothing; returns what was worn in that slot before (or null). */
    @javax.annotation.Nullable
    public GuhClothes wear(GuhClothes clothes) {
        GuhClothes old = getClothes(clothes.slot);
        this.entityData.set(DATA_CLOTHES.get(clothes.slot.ordinal()), clothes.ordinal());
        return old;
    }

    /**
     * Takes off the piece in one slot and returns what it wore there (or null). 2.9: clothes are unlocks, so nothing
     * comes back as an item. The backpack's contents stay with the guh: they are there again when it wears the backpack
     * again (the wardrobe only lets you take an EMPTY backpack off).
     */
    @javax.annotation.Nullable
    public GuhClothes takeOff(GuhClothes.Slot slot) {
        GuhClothes worn = getClothes(slot);
        if (worn != null) {
            this.entityData.set(DATA_CLOTHES.get(slot.ordinal()), -1);
        }
        return worn;
    }

    /** Takes all clothes off; returns what it wore. (Not the hair: see GuhClothes.Slot.HAAR.) */
    public java.util.List<GuhClothes> takeOffClothes() {
        java.util.List<GuhClothes> worn = new java.util.ArrayList<>();
        for (GuhClothes.Slot slot : GuhClothes.Slot.kleding()) {
            GuhClothes off = takeOff(slot);
            if (off != null) {
                worn.add(off);
            }
        }
        return worn;
    }

    /** 2.9: is there anything in its backpack's 18 slots? */
    public boolean backpackIsEmpty() {
        return backpack.isEmpty();
    }

    public net.minecraft.world.SimpleContainer getBackpack() {
        return backpack;
    }

    public boolean hasBackpack() {
        return getClothes(GuhClothes.Slot.BACK) == GuhClothes.GUH_BACKPACK;
    }

    /** Opens the wardrobe (clothes + backpack) for its owner. */
    public void openWardrobe(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, p) -> new nl.juiced.guhs.menu.GuhWardrobeMenu(id, inventory, this),
                    net.minecraft.network.chat.Component.translatable("gui.guhs.wardrobe.title", this.getDisplayName())),
                    buf -> buf.writeVarInt(this.getId()));
        }
    }

    public boolean isWearingClothes() {
        return java.util.Arrays.stream(GuhClothes.Slot.values()).anyMatch(slot -> getClothes(slot) != null);
    }

    /** 2.8: the Knus flags (see feature.knus.GuhHooks). */
    public int getKnusVlaggen() {
        return this.entityData.get(DATA_KNUS_VLAGGEN);
    }

    public void setKnusVlaggen(int flags) {
        this.entityData.set(DATA_KNUS_VLAGGEN, flags);
    }

    /** 2.8: the colour (RGB) its hair piece (GuhClothes.Slot.HAAR) is dyed, -1 = natural. */
    public int getHaarkleur() {
        return this.entityData.get(DATA_HAARKLEUR);
    }

    public void setHaarkleur(int rgb) {
        this.entityData.set(DATA_HAARKLEUR, rgb < 0 ? -1 : rgb & 0xFFFFFF);
    }

    public GuhVariant getVariant() {
        int i = this.entityData.get(DATA_VARIANT);
        return i >= 0 && i < GuhVariant.values().length ? GuhVariant.values()[i] : GuhVariant.NORMAL;
    }

    public void setVariant(GuhVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false; // it's a plushie
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking & sounds
    // ------------------------------------------------------------------------------------------------------------

    /** A sitting guh doesn't move at all: no goal can make it walk, and nothing pushes it around. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || (this.isOrderedToSit() && !this.isVehicle() && !this.isPassenger());
    }

    @Override
    public boolean isPushable() {
        return !this.isOrderedToSit() && !nl.juiced.guhs.feature.verhaal.VerhaalGuhs.isKopieOveral(this) && super.isPushable();
    }

    /** 3.0: story copies never breed. */
    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return !nl.juiced.guhs.feature.verhaal.VerhaalGuhs.isKopie(this) && !nl.juiced.guhs.feature.verhaal.VerhaalGuhs.isKopie(other)
                && super.canMate(other);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            tickLaunch();
        }
        super.tick();
        if (hiddenBy != null && !this.level().isClientSide() && this.tickCount % 100 == 0
                && ((ServerLevel) this.level()).getEntity(hiddenBy) instanceof GuhNpcEntity npc && !npc.verstop.isHiding(this.getUUID())) {
            this.discard();
            return;
        }
        // wild ghost guhs fade away when the sun comes up
        if (!this.level().isClientSide() && getVariant() == GuhVariant.GHOST && !this.isTame() && this.tickCount % 100 == 0
                && this.level().isDay() && !this.isPersistenceRequired()) {
            ((ServerLevel) this.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            this.discard();
            return;
        }
        // CUDDLY: cuddles heal its owner a little (half a heart about every 30 seconds, when close)
        if (!this.level().isClientSide() && getPersonality() == GuhPersonality.CUDDLY && this.isTame() && this.tickCount % 600 == 0
                && this.getOwner() instanceof Player owner && owner.distanceTo(this) < 4 + this.getBbWidth()
                && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(1f);
            this.level().broadcastEntityEvent(this, (byte) 7); // hearts
        }
        if (isEnder()) {
            if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, this.getRandomX(0.6), this.getRandomY() - 0.2,
                        this.getRandomZ(0.6), (this.random.nextDouble() - 0.5) * 0.5, -this.random.nextDouble() * 0.3, (this.random.nextDouble() - 0.5) * 0.5);
            }
            if (!this.onGround() && this.tickCount % 16 == 0 && !this.isInSittingPose()) {
                this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.25f, 1.7f + this.random.nextFloat() * 0.2f);
            }
            if (this.isVehicle()) {
                this.getPassengers().forEach(p -> p.resetFallDistance());
            }
            if (!this.level().isClientSide() && this.isInSittingPose() && this.isNoGravity()) {
                this.setNoGravity(false); // sitting: come down and land
            }
        }
        emotes.tick();
        if (!this.level().isClientSide()) {
            nl.juiced.guhs.feature.knus.GuhHooks.runTick(this);   // 2.8: the Knus features
        }
        if (isZeemeer()) {
            nl.juiced.guhs.feature.onderwater.Zeemeerguh.tick(this);
        }
        nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(this);
        if (gedrag != null) {
            gedrag.tick(this);   // 3.0: a story variant's own behaviour (both sides)
        }
        if (this.level().isClientSide()) {
            this.squishO = this.squish;
            this.squish = Mth.approach(this.squish, isGravityEnabled() ? 1f : 0f, 0.15f);
        }
    }

    /** The synced emote state (GuhEmotes decodes it). */
    public int getEmoteData() {
        return this.entityData.get(DATA_EMOTE);
    }

    public void setEmoteData(int data) {
        this.entityData.set(DATA_EMOTE, data);
    }

    /** The favourite emote (index into Emote, -1 = none): a tamed guh does it by itself now and then. */
    public int getFavoriteEmote() {
        return this.entityData.get(DATA_FAVORITE_EMOTE);
    }

    public void setFavoriteEmote(int emote) {
        this.entityData.set(DATA_FAVORITE_EMOTE, emote);
    }

    public float getSquish(float partialTick) {
        return Mth.lerp(partialTick, this.squishO, this.squish);
    }

    @Override
    public float getVoicePitch() {
        // big guhs have deeper voices, small ones squeak
        float sizePitch = (float) Math.pow(1f / Math.max(0.3f, getGuhScale() * this.getAgeScale()), 0.35);
        return super.getVoicePitch() * Mth.clamp(sizePitch, 0.6f, 1.6f);
    }

    @Override
    public int getAmbientSoundInterval() {
        int interval = this.isTame() ? SOUND_INTERVALS[getSoundFrequency()] : WILD_SOUND_INTERVAL;
        return getPersonality() == GuhPersonality.CHATTY ? interval / 2 : interval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GUH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GUH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GUH_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOOL_STEP, 0.15f, 1.2f);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TeleportToOwner", isTeleportEnabled());
        tag.putBoolean("Wander", isWandering());
        tag.putBoolean("Gravity", isGravityEnabled());
        if (hiddenBy != null) {
            tag.store("VerstopNpc", UUIDUtil.CODEC, hiddenBy);
        }
        tag.putBoolean("Saddle", isSaddled());
        tag.putBoolean("AmbientSounds", areSoundsEnabled());
        tag.putInt("SoundFrequency", getSoundFrequency());
        tag.putString("Behavior", getBehavior().name());
        tag.putInt("AttackRadius", getAttackRadius());
        tag.putBoolean("SecretNote", hasSecretNote());
        tag.putString("Variant", getVariant().id());
        tag.putString("Personality", getPersonality().id());
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes worn = getClothes(slot);
            if (worn != null) {
                tag.putString("Clothes" + slot.name().charAt(0) + slot.name().substring(1).toLowerCase(java.util.Locale.ROOT), worn.id());
            }
        }
        if (!backpack.isEmpty()) {
            // with slot numbers, so everything comes back in the same place
            tag.put("Backpack", net.minecraft.world.ContainerHelper.saveAllItems(new CompoundTag(), backpack.getItems(), this.registryAccess()));
        }
        emotes.save(tag);
        if (getKnusVlaggen() != 0) {
            tag.putInt("KnusVlaggen", getKnusVlaggen());
        }
        if (getHaarkleur() >= 0) {
            tag.putInt("Haarkleur", getHaarkleur());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Wander")) {
            setWandering(tag.getBooleanOr("Wander", false));
        }
        if (tag.contains("TeleportToOwner")) {
            setTeleportEnabled(tag.getBooleanOr("TeleportToOwner", false));
        }
        setGravityEnabled(tag.getBooleanOr("Gravity", false));
        this.entityData.set(DATA_SADDLED, tag.getBooleanOr("Saddle", false));
        setSecretNote(tag.getBooleanOr("SecretNote", false));
        setVariant(GuhVariant.byId(tag.getStringOr("Variant", "")));
        if (tag.read("VerstopNpc", UUIDUtil.CODEC).isPresent()) {
            this.hiddenBy = tag.read("VerstopNpc", UUIDUtil.CODEC).orElseThrow();
            this.entityData.set(DATA_HIDDEN, true);
        }
        // guhs saved as one of the old outfit variants become a normal guh wearing that outfit
        java.util.List<GuhClothes> oldOutfit = switch (tag.getStringOr("Variant", "")) {
            case "sweater" -> GuhClothes.WILD_OUTFITS.get(0);
            case "rain" -> GuhClothes.WILD_OUTFITS.get(1);
            case "party" -> GuhClothes.WILD_OUTFITS.get(2);
            case "chef" -> GuhClothes.WILD_OUTFITS.get(3);
            default -> java.util.List.of();
        };
        GuhPersonality personality = GuhPersonality.byId(tag.getStringOr("Personality", ""));
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            String key = "Clothes" + slot.name().charAt(0) + slot.name().substring(1).toLowerCase(java.util.Locale.ROOT);
            GuhClothes worn = GuhClothes.byId(tag.getStringOr(key, ""));
            this.entityData.set(DATA_CLOTHES.get(slot.ordinal()), worn != null && worn.slot == slot ? worn.ordinal() : -1);
        }
        oldOutfit.forEach(this::wear);
        backpack.clearContent();
        if (tag.contains("Backpack")) {
            net.minecraft.world.ContainerHelper.loadAllItems(tag.getCompoundOrEmpty("Backpack"), backpack.getItems(), this.registryAccess());
        }
        // guhs from before personalities existed (or placed by structures) get one now
        setPersonality(personality != null ? personality : GuhPersonality.random(this.random));
        if (tag.contains("AmbientSounds")) {
            setSoundsEnabled(tag.getBooleanOr("AmbientSounds", false));
            setSoundFrequency(tag.getIntOr("SoundFrequency", 0));
            setAttackRadius(tag.getIntOr("AttackRadius", 0));
            try {
                setBehavior(Behavior.valueOf(tag.getStringOr("Behavior", "")));
            } catch (IllegalArgumentException ignored) {
                // unknown value: keep the default
            }
        }
        emotes.load(tag);
        setKnusVlaggen(tag.getIntOr("KnusVlaggen", 0));
        setHaarkleur(tag.contains("Haarkleur") ? tag.getIntOr("Haarkleur", 0) : -1);
    }

    // ------------------------------------------------------------------------------------------------------------
    // GeckoLib animations (animation names live in assets/guhs/animations/entity/guh.animation.json)
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnimation));
        controllers.add(new AnimationController<>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("happy", HAPPY));
        // the ender guh's wings: flapping in the air, folded on the ground
        controllers.add(new AnimationController<>(this, "wings", 4, state -> !isEnder() ? PlayState.STOP
                : state.setAndContinue(this.onGround() || this.isInSittingPose() ? ENDER_REST : ENDER_FLAP)));
        // the Zeemeerguh's fish tail swishes
        controllers.add(new AnimationController<>(this, "zeemeer", 4, state -> !isZeemeer() ? PlayState.STOP : state.setAndContinue(ZEEMEER_SWIM)));
        // 3.0: a story variant's own loop (the Guhtwo's hover...): feature.verhaal.VariantGedrag.animatie
        controllers.add(new AnimationController<>(this, "variant", 4, state -> {
            nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(this);
            RawAnimation anim = gedrag == null ? null : gedrag.animatie(this);
            return anim == null ? PlayState.STOP : state.setAndContinue(anim);
        }));
    }

    /** Client-only: this is the copy of a guh drawn inside a Guh Wheel, so it always runs. */
    private boolean runningInWheel;

    public void setRunningInWheel(boolean running) {
        this.runningInWheel = running;
    }

    private PlayState mainAnimation(AnimationTest<GuhEntity> state) {
        if (runningInWheel) {
            state.getController().setAnimationSpeed(1.6);
            return state.setAndContinue(WALK);
        }
        if (getLaunchState() == LAUNCH_CHARGING) {
            return state.setAndContinue(SUCK);
        }
        if (getLaunchState() == LAUNCH_FLYING || getLaunchState() == LAUNCH_DROPPING) {
            return state.setAndContinue(FLY);
        }
        RawAnimation emote = emotes.animation();
        if (emote != null) {
            return state.setAndContinue(emote);
        }
        if (this.isInSittingPose()) {
            return state.setAndContinue(SIT);
        }
        if (isEnder() && !this.onGround()) {
            return state.setAndContinue(IDLE); // flying: the wings do the work (see "wings")
        }
        if (state.isMoving()) {
            return state.setAndContinue(WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
