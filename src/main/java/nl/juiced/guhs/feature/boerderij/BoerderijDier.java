package nl.juiced.guhs.feature.boerderij;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A farm animal of the Guhboerderij: always lief and passive (it never attacks, it only runs off a bit when hurt).
 * <p>
 * <b>Care</b>, once a day each (the Guhmensie day, {@link Seizoen#dag}): {@link Zorg#AAIEN} (right-click with an empty
 * hand), {@link Zorg#BORSTELEN} (the guhborstel) and {@link Zorg#VOEREN} (knabbelvoer, or by itself at a filled
 * guh_voerbak). Two of the three on one day make it <b>content</b> ({@link #isBlij}): then it gives its product once that
 * day ({@link #geefProduct}; the koe with an empty bottle, see {@link GuhkoeEntity}). Every care step counts on the Knus tab
 * and for Boerin Hooibaal's chores ({@link Hooibaal#gedaan}).
 */
public abstract class BoerderijDier extends Animal implements GeoEntity {
    /** How many of the three kinds of care make an animal content. */
    public static final int BLIJ_BIJ = 2;
    /** How far an animal walks to a filled voerbak, and how far a kippetje looks for a nest. */
    public static final int VOERBAK_AFSTAND = 10;

    public enum Zorg {
        AAIEN, BORSTELEN, VOEREN;

        public int bit() {
            return 1 << ordinal();
        }
    }

    private static final EntityDataAccessor<Integer> DATA_ZORG = SynchedEntityData.defineId(BoerderijDier.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GEGEVEN = SynchedEntityData.defineId(BoerderijDier.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** The day (Seizoen.dag) the care bits are about. */
    private long zorgDag = -1;

    protected BoerderijDier(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    /** The farm animals spawn on grassy or fluffy ground in the light (and anywhere a structure or egg puts them). */
    public static boolean checkDierSpawnRules(LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.DIRT) || below.is(BlockTags.WOOL)
                || below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) && !below.is(BlockTags.LEAVES);
        return ground && (EntitySpawnReason.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8);
    }

    // --- the animal's own parts --------------------------------------------------------------------------------------------

    /** Its short id for the animations and texts ("guhschaapje"...). */
    public abstract String soort();

    /** Gives its product for today (it is content and hasn't given it yet); the player whose care did it, or null. */
    protected abstract void geefProduct(@Nullable ServerPlayer player);

    /** Its product needs the player to come and get it (the koe's kaasmelk: with a bottle). */
    protected boolean productOpAfroep() {
        return false;
    }

    /** Particles of a brush stroke. */
    protected ParticleOptions borstelDeeltje() {
        return BoerderijFeature.WOLPLUKJE.get();
    }

    protected abstract void speelGeluid();

    // --- synced state ----------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ZORG, 0);
        builder.define(DATA_GEGEVEN, false);
    }

    public int zorg() {
        return entityData.get(DATA_ZORG);
    }

    public boolean heeftZorg(Zorg z) {
        return (zorg() & z.bit()) != 0;
    }

    /** Cared for in at least two ways today. */
    public boolean isBlij() {
        return Integer.bitCount(zorg()) >= BLIJ_BIJ;
    }

    /** Has it given its product today? (The schaapje is shorn until the next day.) */
    public boolean productGegeven() {
        return entityData.get(DATA_GEGEVEN);
    }

    protected void setProductGegeven(boolean gegeven) {
        entityData.set(DATA_GEGEVEN, gegeven);
    }

    /** (Tests) forget today's care. */
    public void vergeetZorg() {
        entityData.set(DATA_ZORG, 0);
        setProductGegeven(false);
        zorgDag = Seizoen.dag(level());
    }

    /** A new day: new care, a new product. */
    protected void nieuweDag() {
        long dag = Seizoen.dag(level());
        if (dag != zorgDag) {
            zorgDag = dag;
            entityData.set(DATA_ZORG, 0);
            setProductGegeven(false);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Zorg", zorg());
        tag.putLong("ZorgDag", zorgDag);
        tag.putBoolean("ProductGegeven", productGegeven());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ZORG, tag.getIntOr("Zorg", 0));
        zorgDag = tag.getLong("ZorgDag").orElse(-1L);
        setProductGegeven(tag.getBooleanOr("ProductGegeven", false));
    }

    // --- care ------------------------------------------------------------------------------------------------------------

    /**
     * One kind of care (by a player, or null for the voerbak). Returns false when it already had this care today (it still
     * enjoys it: hearts). When it becomes content its product comes (unless the player must fetch it).
     */
    public boolean verzorg(Zorg z, @Nullable ServerPlayer player) {
        nieuweDag();
        ServerLevel level = (ServerLevel) level();
        boolean nieuw = !heeftZorg(z);
        boolean wasBlij = isBlij();
        if (nieuw) {
            entityData.set(DATA_ZORG, zorg() | z.bit());
        }
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), nieuw ? 3 : 1, 0.3, 0.2, 0.3, 0.0);
        if (z == Zorg.BORSTELEN) {
            level.sendParticles(borstelDeeltje(), getX(), getY() + getBbHeight() * 0.6, getZ(), 8, 0.35, 0.25, 0.35, 0.02);
        }
        triggerAnim("actie", z == Zorg.VOEREN ? "eet" : "blij");
        speelGeluid();
        if (player != null && nieuw) {
            KnusVoortgang.tel(player, switch (z) {
                case AAIEN -> BoerderijVoortgang.AAIEN;
                case BORSTELEN -> BoerderijVoortgang.BORSTELEN;
                case VOEREN -> BoerderijVoortgang.VOEREN;
            }, 1);
            Hooibaal.gedaan(player, z, this);
        }
        if (!wasBlij && isBlij()) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 10, 0.4, 0.3, 0.4, 0.0);
            if (player != null) {
                BoerderijVoortgang.blij(player);
                player.sendOverlayMessage(Component.translatable("gui.guhs.boerderij.blij." + soort()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (isBlij() && !productGegeven() && !productOpAfroep() && !isBaby()) {
            setProductGegeven(true);
            geefProduct(player);
        }
        return nieuw;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean leeg = stack.isEmpty();
        boolean borstel = stack.is(BoerderijFeature.GUHBORSTEL.get());
        boolean voer = stack.is(BoerderijFeature.KNABBELVOER.get());
        if (level().isClientSide()) {
            return leeg || borstel || voer && !heeftZorg(Zorg.VOEREN) ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (leeg && hand == InteractionHand.MAIN_HAND) {
            verzorg(Zorg.AAIEN, sp);
            return InteractionResult.SUCCESS;
        }
        if (borstel) {
            verzorg(Zorg.BORSTELEN, sp);
            stack.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            level().playSound(null, this, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1f, 1.2f);
            return InteractionResult.SUCCESS;
        }
        nieuweDag();
        if (voer && !heeftZorg(Zorg.VOEREN)) {
            stack.consume(1, player);
            verzorg(Zorg.VOEREN, sp);
            if (!isBaby() && canFallInLove()) {
                setInLove(player);
            } else if (isBaby()) {
                ageUp(getSpeedUpSecondsWhenFeeding(-getAge()), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(BoerderijFeature.KNABBELVOER.get());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (tickCount + getId()) % 40 == 0) {
            nieuweDag();
        }
    }

    // --- goals -------------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new VoerbakGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Hungry today (not fed yet) and a filled voerbak nearby: walk there and eat a bit of it. */
    class VoerbakGoal extends Goal {
        private BlockPos bak;
        private long volgendeKeer;
        private int ticks;

        VoerbakGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long now = level().getGameTime();
            if (now < volgendeKeer || isBaby() && getRandom().nextInt(4) != 0) {
                return false;
            }
            volgendeKeer = now + 60 + getRandom().nextInt(40);
            nieuweDag();
            if (heeftZorg(Zorg.VOEREN)) {
                return false;
            }
            bak = GuhVoerbakBlock.vindGevuld(level(), blockPosition(), VOERBAK_AFSTAND);
            return bak != null;
        }

        @Override
        public boolean canContinueToUse() {
            return bak != null && ticks < 200 && !heeftZorg(Zorg.VOEREN) && GuhVoerbakBlock.voer(level().getBlockState(bak)) > 0;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(bak.getX() + 0.5, bak.getY(), bak.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (bak == null) {
                return;                                        // (done: running goals tick once more before they stop)
            }
            ticks++;
            getLookControl().setLookAt(bak.getX() + 0.5, bak.getY() + 0.5, bak.getZ() + 0.5);
            if (distanceToSqr(bak.getX() + 0.5, bak.getY() + 0.5, bak.getZ() + 0.5) <= 2.6 * 2.6) {
                if (GuhVoerbakBlock.eet(level(), bak)) {
                    verzorg(Zorg.VOEREN, null);
                }
                bak = null;
            } else if (ticks % 20 == 0) {
                getNavigation().moveTo(bak.getX() + 0.5, bak.getY(), bak.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void stop() {
            bak = null;
            getNavigation().stop();
        }
    }

    // --- a gentle creature -----------------------------------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Nullable
    @Override
    public abstract AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other);

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>("actie", 2, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"))
                .triggerableAnim("geluid", RawAnimation.begin().thenPlay("geluid")));
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("actie", "geluid");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
