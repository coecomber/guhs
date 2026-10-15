package nl.juiced.guhs.feature.bio.dieren;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IShearable;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.world.WildeDieren;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * Het wolkenschaapje (biomes3): a little sheep that is mostly cloud. A round ball of white fluff with a guh face, guh
 * ears and four dangling legs that never touch the ground: it floats a hand above the meadow of the Wolkenweide (the
 * model hangs in the air, its shadow under it) and drifts along slowly. It falls like a cloud too: gently, and it never
 * hurts itself.
 * <ul>
 *   <li><b>Shears</b> give wolkenpluis (the wolkenblok recipe needs it); it is a small bare lamb for a while and then
 *       its fluff has grown back ({@link #PLUIS_TERUG}).</li>
 *   <li><b>Keeping one</b>, like the animals of the Guhboerderij: it follows knabbelvoer, and the first bite (or a lead)
 *       makes it yours: it stays for good ({@link #houd}). A wild one a spawner brought comes and goes. Two of yours with
 *       knabbelvoer get a lammetje, in a herd of at most {@link DierenRegels#SCHAAPJE_MAX_KUDDE}.</li>
 * </ul>
 */
public class WolkenschaapjeEntity extends Animal implements GeoEntity, IShearable {
    private static final EntityDataAccessor<Boolean> DATA_GESCHOREN = SynchedEntityData.defineId(WolkenschaapjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GEHOUDEN = SynchedEntityData.defineId(WolkenschaapjeEntity.class, EntityDataSerializers.BOOLEAN);
    /** The fluff is back this many ticks after shearing (five minutes). */
    public static final int PLUIS_TERUG = 20 * 60 * 5;
    /** It never falls faster than this (blocks per tick): a cloud drifts down. */
    public static final double VALSNELHEID = 0.12;

    private static final RawAnimation ZWEEF = RawAnimation.begin().thenLoop("zweef");
    private static final RawAnimation DRIJF = RawAnimation.begin().thenLoop("drijf");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int pluisTerug;

    public WolkenschaapjeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.16);
    }

    /** Ground a wolkenschaapje may appear above: anything sturdy, and the soft cloud blocks of the Wolkenweide. */
    public static boolean isGrond(LevelAccessor level, BlockPos onder) {
        BlockState state = level.getBlockState(onder);
        return state.isFaceSturdy(level, onder, Direction.UP) && !state.is(BlockTags.LEAVES)
                || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("wolkenblok");
    }

    /** By itself only in the Wolkenweide, on ground, in the light, and never more wild ones together than the cap. */
    public static boolean checkSpawn(EntityType<WolkenschaapjeEntity> type, LevelAccessor level, EntitySpawnReason reden, BlockPos pos, RandomSource random) {
        if (!DierenRegels.natuurlijk(reden)) {
            return true;
        }
        boolean grond = isGrond(level, pos.below()) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
        boolean licht = level.getRawBrightness(pos, 0) > 8;
        boolean inBiome = level.getBiome(pos).is(Bio.WOLKENWEIDE);
        int wild = grond && licht && inBiome ? level.getEntitiesOfClass(WolkenschaapjeEntity.class,
                new AABB(pos).inflate(DierenRegels.SCHAAPJE_TEL_STRAAL), s -> !s.isGehouden()).size() : 0;
        return DierenRegels.schaapjeMag(reden, grond, licht, inBiome, wild);
    }

    // --- state ------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GESCHOREN, false);
        builder.define(DATA_GEHOUDEN, false);
    }

    public boolean isGeschoren() {
        return entityData.get(DATA_GESCHOREN);
    }

    public void setGeschoren(boolean geschoren) {
        entityData.set(DATA_GESCHOREN, geschoren);
        pluisTerug = geschoren ? PLUIS_TERUG : 0;
    }

    /** (Tests) ticks until the fluff is back. */
    public void setPluisTerug(int ticks) {
        pluisTerug = ticks;
    }

    /** Somebody's: it stays for good. */
    public boolean isGehouden() {
        return entityData.get(DATA_GEHOUDEN);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Geschoren", isGeschoren());
        tag.putInt("PluisTerug", pluisTerug);
        tag.putBoolean("Gehouden", isGehouden());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_GESCHOREN, tag.getBooleanOr("Geschoren", false));
        pluisTerug = tag.getIntOr("PluisTerug", 0);
        entityData.set(DATA_GEHOUDEN, tag.getBooleanOr("Gehouden", false));
    }

    // --- keeping it -------------------------------------------------------------------------------------------------------

    /**
     * This wolkenschaapje is somebody's now (knabbelvoer, a lead, born from kept parents): never a come-and-go animal
     * again. Returns false when it already was.
     */
    public boolean houd(@Nullable ServerPlayer speler) {
        if (isGehouden()) {
            return false;
        }
        entityData.set(DATA_GEHOUDEN, true);
        setPersistenceRequired();
        removeTag(WildeDieren.KOM_EN_GA);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 5, 0.35, 0.2, 0.35, 0.0);
        }
        if (speler != null) {
            GuhAdvancements.grant(speler, "wolkenschaapje_gehouden");
            speler.sendOverlayMessage(Component.translatable("gui.guhs.wolkenschaapje.gehouden").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isFood(stack) && player instanceof ServerPlayer sp && !isGehouden()) {
            houd(sp);
            if (isBaby() || !canFallInLove()) {
                // (the first bite is the "you are mine now" one, also when it can't fall in love right now)
                stack.consume(1, player);
                triggerAnim("actie", "blij");
                playSound(DierenSlice.SCHAAPJE_BLEH.get(), 0.7f, getVoicePitch());
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(BoerderijFeature.KNABBELVOER.get());
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && level().getEntitiesOfClass(WolkenschaapjeEntity.class,
                getBoundingBox().inflate(DierenRegels.SCHAAPJE_KUDDE_STRAAL)).size() < DierenRegels.SCHAAPJE_MAX_KUDDE;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return DierenSlice.WOLKENSCHAAPJE.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public void finalizeSpawnChildFromBreeding(ServerLevel level, Animal partner, @Nullable AgeableMob kind) {
        super.finalizeSpawnChildFromBreeding(level, partner, kind);
        if (kind instanceof WolkenschaapjeEntity lammetje) {
            lammetje.houd(null);   // (its parents were fed by a player: they are kept, and so is their lammetje)
        }
    }

    // --- shearing ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isShearable(@Nullable Player player, ItemStack item, Level level, BlockPos pos) {
        return isAlive() && !isGeschoren() && !isBaby();
    }

    @Override
    public List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return List.of();
        }
        return List.of(scheer(player instanceof ServerPlayer sp ? sp : null, server));
    }

    /** Snip: the fluff comes off (it grows back in {@link #PLUIS_TERUG}). Returns the wolkenpluis. */
    public ItemStack scheer(@Nullable ServerPlayer speler, ServerLevel level) {
        setGeschoren(true);
        level.playSound(null, this, DierenSlice.SCHAAPJE_PLUIS.get(), SoundSource.NEUTRAL, 0.8f, 1.0f + (random.nextFloat() - 0.5f) * 0.2f);
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.5f, 1.4f);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 10, 0.3, 0.25, 0.3, 0.02);
        triggerAnim("actie", "pluis");
        if (speler != null) {
            GuhAdvancements.grant(speler, "wolkenschaapje_geschoren");
        }
        return new ItemStack(Bio.item("wolkenpluis", Items.WHITE_WOOL), 1 + random.nextInt(3));
    }

    // --- a cloud on legs --------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < -VALSNELHEID) {
            setDeltaMovement(v.x, -VALSNELHEID, v.z);   // it drifts down like a cloud
        }
        resetFallDistance();
        if (level().isClientSide()) {
            if (!isGeschoren() && random.nextInt(24) == 0) {
                level().addParticle(ParticleTypes.CLOUD, getRandomX(0.4), getY() + 0.25, getRandomZ(0.4), 0, 0.005, 0);   // a wisp under it
            }
            return;
        }
        if (isGeschoren() && --pluisTerug <= 0) {
            setGeschoren(false);
            ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.25, 0.3, 0.01);
        }
        if ((tickCount + getId()) % 20 == 0 && isLeashed() && !isGehouden()) {
            houd(getLeashHolder() instanceof ServerPlayer sp ? sp : null);   // on a lead: it is yours
        }
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
        return 0;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // (it floats: no steps)
    }

    // --- sounds -----------------------------------------------------------------------------------------------------------

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("actie", "geluid");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DierenSlice.SCHAAPJE_BLEH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.75f : 1.4f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 6, state -> state.setAndContinue(state.isMoving() ? DRIJF : ZWEEF)));
        controllers.add(new AnimationController<>("actie", 2, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("pluis", RawAnimation.begin().thenPlay("pluis"))
                .triggerableAnim("geluid", RawAnimation.begin().thenPlay("geluid")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
