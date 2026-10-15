package nl.juiced.guhs.feature.techbezorg;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Bezorgguhtje (bbq2): a mini-guh on a step with a far too big backpack. It lives in a {@link StepstationBlockEntity
 * Stepstation} and rides along that station's Haltepaaltjes. It is only the legs of the delivery service: the station decides
 * where it goes ({@link #rijdNaar}, {@link #sta}, {@link #hop}) and keeps what is in the backpack, so nothing is ever lost
 * with the guhtje. What it does itself:
 * <ul>
 *   <li>It cannot be hurt (only {@code /kill} and the void get it; its station then simply makes a new one), hurts nobody,
 *       cannot be leashed or pushed around, never despawns.</li>
 *   <li>A guhtje whose station is gone, or whose station no longer knows it, poofs away the moment it notices
 *       ({@link #controleer}); while its station is not loaded it waits where it stands.</li>
 *   <li>A click: it waves and says what it is doing; a kaasknabbel makes it step extra hard for a minute.</li>
 *   <li>One without a station (spawn egg) just rides around.</li>
 * </ul>
 * GeckoLib animations (tools/features/tech_bezorg_modellen.py): idle / rijd / slaap, vol / leeg (the backpack), and the
 * one-shots laad, zwaai, blij, hop. Texture bezorgguhtje.png, asleep bezorgguhtje_slaapt.png.
 */
public class BezorgguhtjeEntity extends PathfinderMob implements GeoEntity {
    /** How close (blocks, on the ground) counts as "at" a stop, and how much higher or lower it may stand. */
    public static final double BIJ = 1.6, BIJ_HOOGTE = 1.8;
    /** Ticks a kaasknabbel makes it step harder, and how much. */
    public static final int VAHOEG_TICKS = 20 * 60;
    public static final double VAHOEG = 1.3;
    /** Without an order of its station for this many ticks it stands still (the station does not tick: not loaded far enough). */
    private static final int ZONDER_OPDRACHT = 40;

    private static final EntityDataAccessor<Integer> DATA_LADING = SynchedEntityData.defineId(BezorgguhtjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SLAAPT = SynchedEntityData.defineId(BezorgguhtjeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RIJD = RawAnimation.begin().thenLoop("rijd");
    private static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("slaap");
    private static final RawAnimation VOL = RawAnimation.begin().thenLoop("vol");
    private static final RawAnimation LEEG = RawAnimation.begin().thenLoop("leeg");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private BlockPos station;
    @Nullable
    private BlockPos navDoel;
    private int navTik;
    private long laatsteOpdracht;
    private long vahoegTot;

    public BezorgguhtjeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LADING, 0);
        builder.define(DATA_SLAAPT, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // (only one without a station decides for itself where it rides)
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return station == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return station == null && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f) {
            @Override
            public boolean canUse() {
                return !slaapt() && getNavigation().isDone() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !slaapt() && getNavigation().isDone() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return station == null && super.canUse();
            }
        });
    }

    // =====================================================================================================================
    // what its station tells it
    // =====================================================================================================================

    /** The Stepstation it belongs to (null: none, a spawn egg guhtje). */
    @Nullable
    public BlockPos station() {
        return station;
    }

    public void zetStation(@Nullable BlockPos pos) {
        station = pos == null ? null : pos.immutable();
    }

    /** Ride to this block (call it every tick while it should go there: the path is refreshed now and then). */
    public void rijdNaar(BlockPos naar) {
        laatsteOpdracht = level().getGameTime();
        boolean anders = !naar.equals(navDoel);
        if (anders || tickCount - navTik >= 40 || (getNavigation().isDone() && tickCount - navTik >= 10)) {
            navDoel = naar.immutable();
            navTik = tickCount;
            getNavigation().moveTo(naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5, snelheid());
        }
    }

    /** Stand still here. */
    public void sta() {
        laatsteOpdracht = level().getGameTime();
        if (navDoel != null || !getNavigation().isDone()) {
            navDoel = null;
            getNavigation().stop();
        }
    }

    /** Is it at this block (close enough to reach into the chest next to the pole)? */
    public boolean isBij(BlockPos pos) {
        double dx = pos.getX() + 0.5 - getX(), dz = pos.getZ() + 0.5 - getZ(), dy = pos.getY() - getY();
        return dx * dx + dz * dz <= BIJ * BIJ && Math.abs(dy) <= BIJ_HOOGTE;
    }

    /** How far it is from the middle of this block. */
    public double afstand(BlockPos pos) {
        return Math.sqrt(distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** The road is blocked (or far too long): it hops there with a poof. Vahoeg! */
    public void hop(BlockPos naar) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        stopRiding();
        poef(sl);
        teleportTo(naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5);
        setDeltaMovement(0, 0, 0);
        fallDistance = 0;
        navDoel = null;
        getNavigation().stop();
        laatsteOpdracht = sl.getGameTime();
        poef(sl);
        playSound(TechbezorgFeature.HOP.get(), 0.8f, 1f);
        triggerAnim("actie", "hop");
    }

    /** A one-shot animation: laad, zwaai, blij or hop. */
    public void speel(String animatie) {
        triggerAnim("actie", animatie);
    }

    /** Tuut tuut. */
    public void bel() {
        playSound(TechbezorgFeature.BEL.get(), 0.6f, 0.95f + random.nextFloat() * 0.1f);
    }

    /** How many stacks are in the backpack (the station tells; the backpack bulges and its lid stands open). */
    public int lading() {
        return entityData.get(DATA_LADING);
    }

    public void zetLading(int stapels) {
        if (lading() != stapels) {
            entityData.set(DATA_LADING, stapels);
        }
    }

    public boolean slaapt() {
        return entityData.get(DATA_SLAAPT);
    }

    public void zetSlaapt(boolean slaapt) {
        if (slaapt() != slaapt) {
            entityData.set(DATA_SLAAPT, slaapt);
        }
    }

    /** Poof: gone (its station was removed, or does not know it any more). */
    public void verdwijn() {
        if (level() instanceof ServerLevel sl) {
            poef(sl);
        }
        discard();
    }

    private void poef(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 8, 0.2, 0.25, 0.2, 0.02);
    }

    private double snelheid() {
        return level().getGameTime() < vahoegTot ? VAHOEG : 1.0;
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && station != null) {
            if ((tickCount + getId()) % 20 == 0) {
                controleer(sl);
            }
            if (!isRemoved() && sl.getGameTime() - laatsteOpdracht > ZONDER_OPDRACHT && !getNavigation().isDone()) {
                navDoel = null;
                getNavigation().stop();   // (nobody steers: the station is not ticking)
            }
        }
    }

    /**
     * Does its station still exist and still know it? While the station's chunk is not loaded nothing is decided (it waits);
     * a guhtje of a station that was removed, or that got a new guhtje in the meantime, poofs away.
     */
    void controleer(ServerLevel sl) {
        if (station == null || !sl.isLoaded(station)) {
            return;
        }
        if (!(sl.getBlockEntity(station) instanceof StepstationBlockEntity thuis) || !thuis.isKoerier(this)) {
            verdwijn();
        }
    }

    // =====================================================================================================================
    // being lief
    // =====================================================================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean knabbel = stack.is(ModItems.KAAS_KNABBELS.get());
        if (!knabbel && hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (knabbel) {
            stack.consume(1, player);
            vahoegTot = level().getGameTime() + VAHOEG_TICKS;
            speel("blij");
            playSound(TechbezorgFeature.GUHTJE.get(), 0.8f, 1.2f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 4, 0.25, 0.2, 0.25, 0);
            }
            sp.sendOverlayMessage(Component.translatable("gui.guhs.techbezorg.guhtje.lekker").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }
        if (!slaapt()) {
            speel("zwaai");
            bel();
        }
        Component stand = Component.translatable("gui.guhs.techbezorg.guhtje.dakloos");
        if (station != null && level().isLoaded(station) && level().getBlockEntity(station) instanceof StepstationBlockEntity thuis) {
            stand = thuis.standTekst();
        }
        sp.sendOverlayMessage(stand.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return slaapt() ? null : TechbezorgFeature.GUHTJE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 20;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    // =====================================================================================================================
    // save
    // =====================================================================================================================

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.storeNullable("Station", BlockPos.CODEC, station);
        tag.putBoolean("Slaapt", slaapt());
        tag.putInt("Lading", lading());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        station = tag.read("Station", BlockPos.CODEC).orElse(null);
        zetSlaapt(tag.getBooleanOr("Slaapt", false));
        zetLading(tag.getIntOr("Lading", 0));
    }

    // =====================================================================================================================
    // GeckoLib
    // =====================================================================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BezorgguhtjeEntity>("beweeg", 3,
                state -> state.setAndContinue(slaapt() ? SLAAP : state.isMoving() ? RIJD : IDLE)));
        controllers.add(new AnimationController<BezorgguhtjeEntity>("rugzak", 4, state -> state.setAndContinue(lading() > 0 ? VOL : LEEG)));
        controllers.add(new AnimationController<BezorgguhtjeEntity>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("laad", RawAnimation.begin().thenPlay("laad"))
                .triggerableAnim("zwaai", RawAnimation.begin().thenPlay("zwaai"))
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("hop", RawAnimation.begin().thenPlay("hop")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
