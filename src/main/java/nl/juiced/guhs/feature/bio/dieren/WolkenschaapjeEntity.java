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
import net.minecraft.world.entity.TamableAnimal;
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
import net.minecraft.world.item.Item;
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
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.PiepMenu;
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
 *   <li>1.8.0 (vadskracht, de Wolfabriek): a kept wolkenschaapje is a real maatje ({@link PiepMaatje}): its keeper is its
 *       owner, the empty hand pets it and opens its little menu (Rondvadsen + "Pluisknuffel"), sneak + empty hand picks it up
 *       ({@code guhs:wolkenschaapje_item}), it moves into a Guhhuisje and shows in "Mijn andere vadsjes". It also likes pluisgras
 *       and wolkensuikerspin (lure, keep, and two kept ones with wolkensuikerspin get a lammetje). It sometimes floats around
 *       De Zingende Weide too.</li>
 * </ul>
 */
public class WolkenschaapjeEntity extends TamableAnimal implements GeoEntity, IShearable, PiepMaatje {
    private static final EntityDataAccessor<Boolean> DATA_GESCHOREN = SynchedEntityData.defineId(WolkenschaapjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GEHOUDEN = SynchedEntityData.defineId(WolkenschaapjeEntity.class, EntityDataSerializers.BOOLEAN);
    /** 1.8.0: the menu settings that are off (PiepInstelling bits; saved as "PiepUit"). */
    private static final EntityDataAccessor<Integer> DATA_UIT = SynchedEntityData.defineId(WolkenschaapjeEntity.class, EntityDataSerializers.INT);
    /** 1.8.0: seconds between two Pluisknuffels (the menu's big button). */
    public static final int KNUFFEL_RUST = 60;
    /** 1.8.0: the item a picked-up wolkenschaapje becomes (set by FabriekFeature, which registers it). */
    public static java.util.function.Supplier<? extends Item> OPPAK_ITEM = () -> net.minecraft.world.item.Items.WHITE_WOOL;
    /** The fluff is back this many ticks after shearing (five minutes). */
    public static final int PLUIS_TERUG = 20 * 60 * 5;
    /** It never falls faster than this (blocks per tick): a cloud drifts down. */
    public static final double VALSNELHEID = 0.12;

    private static final RawAnimation ZWEEF = RawAnimation.begin().thenLoop("zweef");
    private static final RawAnimation DRIJF = RawAnimation.begin().thenLoop("drijf");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int pluisTerug;
    private long knuffelKlaar;

    public WolkenschaapjeEntity(EntityType<? extends TamableAnimal> type, Level level) {
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
        boolean inBiome = level.getBiome(pos).is(Bio.WOLKENWEIDE)
                || level.getBiome(pos).is(ZINGENDE_WEIDE) && random.nextInt(ZINGENDE_WEIDE_KANS) == 0;   // 1.8.0: now and then in the weide
        int wild = grond && licht && inBiome ? level.getEntitiesOfClass(WolkenschaapjeEntity.class,
                new AABB(pos).inflate(DierenRegels.SCHAAPJE_TEL_STRAAL), s -> !s.isGehouden()).size() : 0;
        return DierenRegels.schaapjeMag(reden, grond, licht, inBiome, wild);
    }

    /** 1.8.0: De Zingende Weide, where a wolkenschaapje floats by now and then (1 in {@link #ZINGENDE_WEIDE_KANS} spawn tries). */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> ZINGENDE_WEIDE = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.BIOME, nl.juiced.guhs.Guhs.id("zingende_weide"));
    public static final int ZINGENDE_WEIDE_KANS = 4;

    // --- state ------------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GESCHOREN, false);
        builder.define(DATA_GEHOUDEN, false);
        builder.define(DATA_UIT, 0);
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
        tag.putInt("PiepUit", uitVlaggen());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_GESCHOREN, tag.getBooleanOr("Geschoren", false));
        pluisTerug = tag.getIntOr("PluisTerug", 0);
        entityData.set(DATA_GEHOUDEN, tag.getBooleanOr("Gehouden", false));
        setUitVlaggen(tag.getIntOr("PiepUit", 0));
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
        if (speler != null && !isTame()) {
            tame(speler);                                   // 1.8.0: its keeper is its owner (menu, picking up, huisje)
        }
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
        if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND && isGehouden() && !isBaby()) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            ServerPlayer sp = (ServerPlayer) player;
            if (!isTame()) {
                tame(sp);                                   // (kept before 1.8.0: whoever clicks it first now is its owner)
            }
            if (isOwnedBy(player)) {
                if (player.isSecondaryUseActive()) {
                    PiepDierItem.pakOp(this, sp);           // sneak + empty hand: picked up
                } else {
                    ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
                    triggerAnim("actie", "blij");
                    playSound(DierenSlice.SCHAAPJE_BLEH.get(), 0.6f, getVoicePitch());
                    PiepMenu.open(sp, this);                // empty hand: petted, and its menu
                }
                return InteractionResult.SUCCESS;
            }
        }
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
        return stack.is(BoerderijFeature.KNABBELVOER.get()) || isLekkers(stack);
    }

    /** 1.8.0: pluisgras and wolkensuikerspin (lure, keep, and two kept ones get a lammetje with them). */
    public static boolean isLekkers(ItemStack stack) {
        net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return nl.juiced.guhs.Guhs.MODID.equals(id.getNamespace()) && (id.getPath().equals("pluisgras") || id.getPath().equals("wolkensuikerspin"));
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
            if (getOwnerReference() != null) {
                lammetje.setOwnerReference(getOwnerReference());   // 1.8.0: and it belongs to the same keeper
                lammetje.setTame(true, true);
            }
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
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(1, new PiepMaatje.BlijfGoal(this));
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

    // --- 1.8.0: a maatje (PiepMaatje) -------------------------------------------------------------------------------------

    @Override
    public TamableAnimal dier() {
        return this;
    }

    @Override
    public String soort() {
        return "wolkenschaapje";
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return List.of(PiepInstelling.RONDVADSEN);
    }

    @Override
    public Item oppakItem() {
        return OPPAK_ITEM.get();
    }

    @Override
    public int uitVlaggen() {
        return entityData.get(DATA_UIT);
    }

    @Override
    public void setUitVlaggen(int vlaggen) {
        entityData.set(DATA_UIT, vlaggen);
    }

    @Override
    public boolean isBezig() {
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent oppakGeluid() {
        return DierenSlice.SCHAAPJE_BLEH.get();
    }

    /** The big button "Pluisknuffel": you sink into its fluff: a moment of Slow Falling, like a cloud (once a minute). */
    @Override
    public void speciaal(ServerPlayer player) {
        if (rustSeconden() > 0) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.wolkenschaapje.knuffel_rust", rustSeconden()).withStyle(ChatFormatting.GRAY));
            return;
        }
        knuffelKlaar = level().getGameTime() + KNUFFEL_RUST * 20L;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 20 * 20, 0, false, true));
        triggerAnim("actie", "blij");
        ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 14, 0.4, 0.5, 0.4, 0.02);
        playSound(DierenSlice.SCHAAPJE_PLUIS.get(), 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.wolkenschaapje.knuffel", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public int rustSeconden() {
        long over = knuffelKlaar - level().getGameTime();
        return over > 0 ? (int) ((over + 19) / 20) : 0;
    }

    @Override
    public boolean wantsToAttack(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity owner) {
        return false;
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
