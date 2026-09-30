package nl.juiced.guhs.feature.piep;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;
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
 * The pieppiepmuisje: a tiny plush mouse (dark purple-black, a fluffy cream band, a pink nose). Always lief.
 * <ul>
 *   <li>It scurries around in quick little trots and peeps (piep_ambient1..5), and runs to kaasknabbels lying on the ground
 *       to nibble them ({@link KnabbelGoal}).</li>
 *   <li>Right-click with an empty hand: aaien (hearts, a happy spin). A kaasknabbel tames it (1 in {@link #TAME_CHANCE};
 *       always with "Lief kijken"). Tamed it follows you. Sneak + right-click (owner): pick it up as the
 *       {@link MuisjeItem pieppiepmuisje_item} (name and all); use that item on a block to put it down, in the air to put it
 *       on your shoulder ({@link Schouder}).</li>
 *   <li>Verstoppertje ({@link Verstoppertje}): now and then a tamed muisje hides in a pot, basket or flower pot nearby (or
 *       in the grass) and peeps until you find it by right-clicking there.</li>
 * </ul>
 * GeckoLib bones/animations: see blockbench/pieppiepmuisje.bbmodel and PIEP_NOTES.md (idle, walk, blij, eet, piep, zit).
 */
public class PieppiepmuisjeEntity extends TamableAnimal implements GeoEntity, PiepMaatje {
    /** A kaasknabbel tames a wild muisje 1 in this many times. */
    public static final int TAME_CHANCE = 3;
    private static final EntityDataAccessor<Boolean> DATA_VERSTOPT = SynchedEntityData.defineId(PieppiepmuisjeEntity.class, EntityDataSerializers.BOOLEAN);
    /** The menu settings that are off (PiepInstelling bits; saved as "PiepUit"). */
    private static final EntityDataAccessor<Integer> DATA_UIT = SynchedEntityData.defineId(PieppiepmuisjeEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** When it may nibble the next dropped knabbel, and when it was last petted (no spam counting). */
    private long volgendeHap, laatstGeaaid = Long.MIN_VALUE / 2;   // (the first pet always counts, also in a brand-new world)
    /** (Client, shoulder copies) render it sitting. */
    public boolean opSchouder;
    /** (Made on first use: registerGoals runs from the Mob constructor, before the fields are set.) */
    private Verstoppertje verstop;

    public PieppiepmuisjeEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VERSTOPT, false);
        builder.define(DATA_UIT, 0);
    }

    Verstoppertje verstop() {
        if (verstop == null) {
            verstop = new Verstoppertje(this);
        }
        return verstop;
    }

    public boolean isVerstopt() {
        return entityData.get(DATA_VERSTOPT);
    }

    /** 2.10: its name floats above its head, like a guh's (its own name or "Pieppiepmuisje"), but not while it is hidden. */
    @Override
    public boolean shouldShowName() {
        return !isVerstopt() && !isInvisible();
    }

    void setVerstopt(boolean verstopt) {
        entityData.set(DATA_VERSTOPT, verstopt);
        setInvisible(verstopt);
        setNoGravity(verstopt);
        noPhysics = verstopt;
        if (verstopt) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new BlijfGoal(this));             // (rondvadsen uit: it stays put, sitting)
        goalSelector.addGoal(2, verstop().goal());
        goalSelector.addGoal(3, new KnabbelGoal());
        goalSelector.addGoal(4, new TemptGoal(this, 1.2, s -> s.is(ModItems.KAAS_KNABBELS.get()), false));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.25, 5.0f, 2.0f) {
            @Override
            public boolean canUse() {
                return aan(PiepInstelling.VOLGEN) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return aan(PiepInstelling.VOLGEN) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // --- being lief ----------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean knabbel = stack.is(ModItems.KAAS_KNABBELS.get());
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!knabbel && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (isVerstopt()) {
            verstop().gevonden(sp);
            return InteractionResult.SUCCESS;
        }
        if (knabbel) {
            stack.consume(1, player);
            triggerAnim("actie", "eet");
            playSound(PiepFeature.PIEP.get(), 0.8f, 1.2f);
            if (!isTame()) {
                boolean lief = player.hasEffect(PiepFeature.LIEF_KIJKEN);
                if ((lief || random.nextInt(TAME_CHANCE) == 0) && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    temmen(sp);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            } else {
                heal(4);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && isOwnedBy(player)) {
            MuisjeItem.pakOp(this, sp);                          // sneak + empty hand (owner): picked up
            return InteractionResult.SUCCESS;
        }
        aaien(sp);
        if (isTame() && isOwnedBy(player)) {
            PiepMenu.open(sp, this);                             // empty hand (owner): petted, and its menu
        }
        return InteractionResult.SUCCESS;
    }

    /** Petting: hearts, a happy spin and a piep (counts once every few seconds). */
    public void aaien(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.45, getZ(), 2, 0.2, 0.1, 0.2, 0);
        triggerAnim("actie", "blij");
        playSound(PiepFeature.PIEP.get(), 0.8f, 1.1f + random.nextFloat() * 0.3f);
        if (level.getGameTime() - laatstGeaaid >= 40) {
            laatstGeaaid = level.getGameTime();
            PiepVoortgang.tel(player, PiepVoortgang.GEAAID, 1, "piep_muisje_geaaid");
            PiepVoortgang.pagina(player, "pieppiepmuisje");
        }
    }

    /** Tamed by this player (hearts, a message, the counters). */
    public void temmen(ServerPlayer player) {
        tame(player);
        getNavigation().stop();
        level().broadcastEntityEvent(this, (byte) 7);
        triggerAnim("actie", "blij");
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep.muisje_getamed").withStyle(ChatFormatting.LIGHT_PURPLE));
        PiepVoortgang.tel(player, PiepVoortgang.GETAMED, 1, "piep_muisje_getamed");
        PiepVoortgang.pagina(player, "pieppiepmuisje");
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get());
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
    public boolean isPushable() {
        return !isVerstopt() && super.isPushable();
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (isVerstopt() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            verstop().tick();
        }
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return false;
    }

    // --- sounds --------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isVerstopt() || !aan(PiepInstelling.PIEPJES) ? null : PiepFeature.PIEP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PiepFeature.PIEP_AU.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PiepFeature.PIEP_AU.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 1.0f + (random.nextFloat() - 0.5f) * 0.25f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) {
            triggerAnim("actie", "piep");
        }
    }

    // --- save ----------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag verstopTag = new CompoundTag();
        verstop().save(verstopTag);
        tag.store(verstopTag);                                   // (1.1.0: the old keys, at the top level like 1.0.0)
        tag.putInt("PiepUit", uitVlaggen());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        verstop().load(nl.juiced.guhs.storage.Nbt.toTag(tag));
        setUitVlaggen(tag.getIntOr("PiepUit", 0));
    }

    // --- the menu (PiepMaatje) ----------------------------------------------------------------------------------------------

    @Override
    public TamableAnimal dier() {
        return this;
    }

    @Override
    public String soort() {
        return "pieppiepmuisje";
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return PiepInstelling.MUISJE;
    }

    @Override
    public net.minecraft.world.item.Item oppakItem() {
        return PiepFeature.PIEPPIEPMUISJE_ITEM.get();
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
        return isVerstopt();
    }

    @Override
    public boolean kanOpSchouder() {
        return true;
    }

    @Override
    public void opSchouder(boolean ja) {
        this.opSchouder = ja;
    }

    /** "Op mijn schouder!": it climbs onto your shoulder (if there is room). */
    @Override
    public void speciaal(ServerPlayer player) {
        if (Schouder.heeft(player)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep.schouder_vol").withStyle(ChatFormatting.GRAY));
            return;
        }
        Schouder.zet(player, this);
    }

    // --- nibbling dropped kaasknabbels ---------------------------------------------------------------------------------------

    /** A kaasknabbel lies on the ground nearby: scurry there and nibble it (one knabbel off the stack). */
    class KnabbelGoal extends Goal {
        private ItemEntity doel;
        private int ticks;

        KnabbelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (level().getGameTime() < volgendeHap || isVerstopt() || isOrderedToSit()) {
                return false;
            }
            volgendeHap = level().getGameTime() + 20;
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8, 3, 8),
                    i -> i.isAlive() && i.getItem().is(ModItems.KAAS_KNABBELS.get()));
            doel = items.isEmpty() ? null : items.stream().min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).get();
            return doel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && doel.isAlive() && ticks < 200;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(doel, 1.35);
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
            if (distanceToSqr(doel) < 0.8 * 0.8 + 0.3) {
                ItemStack s = doel.getItem().copy();
                s.shrink(1);
                if (s.isEmpty()) {
                    doel.discard();
                } else {
                    doel.setItem(s);
                }
                triggerAnim("actie", "eet");
                playSound(PiepFeature.PIEP.get(), 0.7f, 1.3f);
                heal(2);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get().asItem()),
                            getX(), getY() + 0.2, getZ(), 6, 0.1, 0.05, 0.1, 0.03);
                }
                volgendeHap = level().getGameTime() + 200 + random.nextInt(200);
                doel = null;
            } else if (ticks % 15 == 0) {
                getNavigation().moveTo(doel, 1.35);
            }
        }

        @Override
        public void stop() {
            doel = null;
        }
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(
                opSchouder || isInSittingPose() ? ZIT : state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"))
                .triggerableAnim("piep", RawAnimation.begin().thenPlay("piep")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
