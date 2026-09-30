package nl.juiced.guhs.feature.landdiertjes;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.PiepMenu;
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
 * A little land critter of the Guhmensie (3.0, DESIGN_30 §6): the {@link PluisegeltjeEntity pluisegeltje}, the
 * {@link GuhKonijntjeEntity guh-konijntje}, the {@link PluiseekhoorntjeEntity pluiseekhoorntje} and {@link ShuckleEntity
 * Sjokkel}. Guh-inspired (big glossy guh eyes, blush, round ears), but no guhs. Always lief.
 * <p>
 * What they share (a piep-maatje, {@link PiepMaatje}): tame it with its favourite food ({@link #isVoer}, 1 in
 * {@link #temKans()}); tamed it follows you (menu setting "Volg mij"), sits when told ("Rondvadsen"), can be picked up (sneak +
 * empty hand, or "Oppakken") into its {@code <soort>_item} and moves into a Guhhuisje; the owner's empty-hand click pets it and
 * opens its little menu with its own big button ({@link #speciaal}). It runs to its food lying on the ground and nibbles it
 * ({@link LekkerGoal}). GeckoLib animations: idle, walk, zit (sitting and on a shoulder), blij, eet + the critter's own.
 */
public abstract class Landdiertje extends TamableAnimal implements GeoEntity, PiepMaatje {
    /** The menu settings that are off (PiepInstelling bits; saved as "PiepUit"). */
    private static final EntityDataAccessor<Integer> DATA_UIT = SynchedEntityData.defineId(Landdiertje.class, EntityDataSerializers.INT);

    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** (Client, shoulder copies) render it sitting. */
    public boolean opSchouder;
    private long laatstGeaaid = Long.MIN_VALUE / 2, volgendeHap;
    /** Game time of the next check of its huisje advancement. */
    private long volgendeHuisjeCheck;

    protected Landdiertje(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    // --- what each critter says about itself --------------------------------------------------------------------------------

    /** Its favourite food: tames it, heals it, lures it (and it nibbles it off the ground). */
    public abstract boolean isVoer(ItemStack stack);

    /** A favourite food tames a wild one 1 in this many times. */
    public abstract int temKans();

    /** Its own sound (also the pick-up sound). */
    protected abstract SoundEvent geluid();

    /** Can a player tame it right now (not while it is rolled up / in its shell)? The reason otherwise, else null. */
    @Nullable
    protected Component nietNu() {
        return null;
    }

    /** How fast it trots after its owner (1.0 = its walking speed). */
    protected double volgSnelheid() {
        return 1.2;
    }

    // --- synced data, save -------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UIT, 0);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PiepUit", uitVlaggen());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setUitVlaggen(tag.getIntOr("PiepUit", 0));
    }

    // --- goals -----------------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !isTame() && !isBezig() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new BlijfGoal(this));
        eigenDoelen();
        goalSelector.addGoal(4, new LekkerGoal());
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isVoer, false));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, volgSnelheid(), 5.0f, 2.0f) {
            @Override
            public boolean canUse() {
                return aan(PiepInstelling.VOLGEN) && !isBezig() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return aan(PiepInstelling.VOLGEN) && !isBezig() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** The critter's own goals (priority 2-3). */
    protected void eigenDoelen() {
    }

    // --- being lief ------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean voer = isVoer(stack);
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!voer && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (voer) {
            return voer(sp, stack);
        }
        if (player.isSecondaryUseActive() && isTame() && isOwnedBy(player)) {
            PiepDierItem.pakOp(this, sp);                          // sneak + empty hand (owner): picked up
            return InteractionResult.SUCCESS;
        }
        aaien(sp);
        if (isTame() && isOwnedBy(player) && !isBezig()) {
            PiepMenu.open(sp, this);                               // empty hand (owner): petted, and its menu
        }
        return InteractionResult.SUCCESS;
    }

    /** Its favourite food from a player's hand: taming (wild), or a happy snack (tamed). */
    protected InteractionResult voer(ServerPlayer player, ItemStack stack) {
        if (!isTame()) {
            Component waarom = nietNu();
            if (waarom != null) {
                player.sendOverlayMessage(waarom.copy().withStyle(ChatFormatting.GRAY));
                return InteractionResult.SUCCESS;
            }
            stack.consume(1, player);
            eetEffect(stack);
            if (random.nextInt(temKans()) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                temmen(player);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        stack.consume(1, player);
        eetEffect(stack);
        heal(4);
        gegeten(player, stack);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        return InteractionResult.SUCCESS;
    }

    /** (Tamed, after a snack from its owner) e.g. Sjokkel keeps the berries for his juice. */
    protected void gegeten(@Nullable ServerPlayer player, ItemStack snack) {
    }

    protected void eetEffect(ItemStack stack) {
        triggerAnim("actie", "eet");
        playSound(geluid(), 0.7f, 1.2f + random.nextFloat() * 0.2f);
        if (level() instanceof ServerLevel sl && !stack.isEmpty()) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1).getItem()), getX(), getY() + getBbHeight() * 0.6, getZ(),
                    6, 0.12, 0.05, 0.12, 0.03);
        }
    }

    /** Petting: hearts, a happy little animation (counts once every few seconds). */
    public void aaien(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        if (!isBezig()) {
            triggerAnim("actie", "blij");
        }
        playSound(geluid(), 0.7f, 1.1f + random.nextFloat() * 0.3f);
        if (level.getGameTime() - laatstGeaaid >= 40) {
            laatstGeaaid = level.getGameTime();
            GidsFeature.grant(player, "diertjes/landdiertjes_geaaid");
        }
    }

    /** Tamed by this player (hearts, a message, its advancement comes from minecraft:tame_animal). */
    public void temmen(ServerPlayer player) {
        tame(player);
        getNavigation().stop();
        setTarget(null);
        level().broadcastEntityEvent(this, (byte) 7);
        triggerAnim("actie", "blij");
        player.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.getemd." + soort(), getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isVoer(stack);
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return false;
    }

    /** Its name floats above it like a guh's (its own name, or its kind). */
    @Override
    public boolean shouldShowName() {
        return hasCustomName() && !isInvisible();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isTame() && level().getGameTime() >= volgendeHuisjeCheck) {
            volgendeHuisjeCheck = level().getGameTime() + 100 + random.nextInt(40);
            if (Huisjes.isBewoner(this) && getOwner() instanceof ServerPlayer owner) {
                GidsFeature.grant(owner, "diertjes/landdiertjes_huisje");
            }
        }
    }

    // --- sounds ------------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isBezig() ? null : geluid();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return geluid();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return geluid();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public SoundEvent oppakGeluid() {
        return geluid();
    }

    // --- the menu (PiepMaatje) ---------------------------------------------------------------------------------------------------

    @Override
    public TamableAnimal dier() {
        return this;
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return List.of(PiepInstelling.RONDVADSEN, PiepInstelling.VOLGEN);
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
    public void opSchouder(boolean ja) {
        this.opSchouder = ja;
    }

    /** The item of this kind (PiepDierItem.van keeps everything). */
    public ItemStack alsItem() {
        return PiepDierItem.van(this, oppakItem());
    }

    @Override
    public abstract Item oppakItem();

    // --- nibbling its food off the ground ------------------------------------------------------------------------------------------

    /** Its food lies on the ground nearby: trot there and nibble one off the stack (then it waits a while). */
    protected class LekkerGoal extends Goal {
        private ItemEntity doel;
        private int ticks;

        LekkerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (level().getGameTime() < volgendeHap || isBezig() || isOrderedToSit()) {
                return false;
            }
            volgendeHap = level().getGameTime() + 20;
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8, 3, 8),
                    i -> i.isAlive() && isVoer(i.getItem()));
            doel = items.isEmpty() ? null : items.stream().min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).get();
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && doel.isAlive() && ticks < 240 && !isBezig();
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(doel, 1.2);
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
            ticks++;
            getLookControl().setLookAt(doel);
            if (distanceToSqr(doel) < 1.0) {
                ItemStack s = doel.getItem().copy();
                ItemStack hap = s.split(1);
                if (s.isEmpty()) {
                    doel.discard();
                } else {
                    doel.setItem(s);
                }
                eetEffect(hap);
                heal(2);
                gevonden(hap);
                volgendeHap = level().getGameTime() + 200 + random.nextInt(200);
                doel = null;
            } else if (ticks % 15 == 0) {
                getNavigation().moveTo(doel, 1.2);
            }
        }

        @Override
        public void stop() {
            doel = null;
        }
    }

    /** (After nibbling something off the ground) e.g. the eekhoorntje stuffs it into its cheeks for a stash. */
    protected void gevonden(ItemStack hap) {
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------------------

    /** The looping animation right now (override for rolled up / in its shell). */
    protected RawAnimation beweging(com.geckolib.animation.state.AnimationTest<Landdiertje> state) {
        if (opSchouder || isInSittingPose()) {
            return ZIT;
        }
        return state.isMoving() ? WALK : IDLE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Landdiertje>("beweeg", 3, state -> state.setAndContinue(beweging(state))));
        AnimationController<Landdiertje> actie = new AnimationController<Landdiertje>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"));
        extraActies(actie);
        controllers.add(actie);
    }

    /** More one-shot animations of this critter (triggerableAnim on the "actie" controller). */
    protected void extraActies(AnimationController<Landdiertje> actie) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
