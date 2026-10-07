package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2, slice vetbootje: the small personal heteluchtballon (guhs:heteluchtballon), unfolded from its item
 * ({@link Heteluchtballon}). You do not steer it: the wind does. Hold jump = the burner roars and it rises; let go = it
 * sinks slowly. The wind of a dimension is slow and blows another way at every height ({@link #wind}): pick your height
 * and you pick your way (the little vane on the basket shows it). On the ground it stands still; with nobody in the
 * basket it sinks straight down and waits. It never leaves the world, below or above. One rider and one more (a friend,
 * or your own guh with a kaasknabbel). Sneak + click (the owner) folds it up again.
 * <p>
 * Driven like a boat: the rider's game moves it (vanilla's vehicle packets) and tells the server when the burner goes on
 * or off (payload einde2_vetbootje_brander), so everybody sees the flame. Model: tools/features/einde2_vetbootje_modellen.py.
 */
public class HeteluchtballonEntity extends Entity implements GeoEntity {
    /** Blocks per tick: rising with the burner on, sinking with it off, and the strongest wind. Gentle, all of it. */
    public static final double STIJG = 0.12, ZAK = 0.06, WIND = 0.07;
    /** The wind turns a full circle over this many blocks of height. */
    public static final double WINDTRAP = 72;

    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_EIGENAAR =
            SynchedEntityData.defineId(HeteluchtballonEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final EntityDataAccessor<Boolean> DATA_BRANDER = SynchedEntityData.defineId(HeteluchtballonEntity.class, EntityDataSerializers.BOOLEAN);

    /** The rider's burner key (set by the client: the jump key). */
    @Nullable
    public static BooleanSupplier branderToets;
    /** The rider's game tells the server that its burner went on or off (set by the client). */
    @Nullable
    public static BiConsumer<Integer, Boolean> meld;
    /** Every client tick (the flame's sparks). */
    @Nullable
    public static Consumer<HeteluchtballonEntity> clientTick;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Server: which copy of its owner's balloon this is ({@link Heteluchtballon#opnieuw}). */
    int nr;
    /** Upward speed (blocks per tick). */
    public double vy;
    private boolean lokaleBrander, gekeken;
    private int lerpSteps, windRichting = -1;
    private double lerpX, lerpY, lerpZ, lerpYRot;

    public HeteluchtballonEntity(EntityType<? extends HeteluchtballonEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_EIGENAAR, Optional.empty());
        b.define(DATA_BRANDER, false);
    }

    public void zetEigenaar(@Nullable UUID id) {
        entityData.set(DATA_EIGENAAR, Optional.ofNullable(id).map(EntityReference::of));
    }

    @Nullable
    public UUID eigenaar() {
        return entityData.get(DATA_EIGENAAR).map(EntityReference::getUUID).orElse(null);
    }

    /** May this player take the burner? The owner, or anybody in a balloon that belongs to nobody. */
    public boolean magSturen(Player player) {
        UUID e = eigenaar();
        return e == null || e.equals(player.getUUID());
    }

    /** Is the burner on (as everybody sees it)? */
    public boolean brander() {
        return entityData.get(DATA_BRANDER) || lokaleBrander;
    }

    public void zetBrander(boolean aan) {
        entityData.set(DATA_BRANDER, aan);
    }

    // --- the wind -----------------------------------------------------------------------------------------------------------

    /**
     * The wind at this height (blocks per tick, flat): every dimension has its own, it turns with the height (a full circle
     * every {@value #WINDTRAP} blocks) and wanders a little with the time. The same on the server and in every game.
     */
    public static Vec3 wind(Level level, double y, long tijd) {
        int zaad = level.dimension().identifier().hashCode();
        double graden = Math.floorMod(zaad, 360) + y * (360.0 / WINDTRAP) + Math.sin(tijd / 2400.0 + zaad % 17) * 35.0;
        double kracht = WIND * (0.75 + 0.25 * Math.sin(tijd / 1700.0 + zaad % 13));
        double hoek = Math.toRadians(graden);
        return new Vec3(Math.sin(hoek) * kracht, 0, Math.cos(hoek) * kracht);
    }

    /** Where the wind blows to, as one of eight compass points (0 = zuid .. the order of gui.guhs.einde2_vetbootje.wind.N). */
    public static int windstreek(Vec3 wind) {
        double graden = Math.toDegrees(Math.atan2(-wind.x, wind.z));       // (a yaw: 0 = south, 90 = west)
        return Math.floorMod((int) Math.round(graden / 45.0), 8);
    }

    // --- floating -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (lerpSteps > 0 && !bestuurdHier()) {
            double f = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) * (float) f);
            lerpSteps--;
        }
        if (bestuurdHier()) {
            boolean rijder = getControllingPassenger() != null;
            boolean aan = false;
            if (rijder && level().isClientSide() && branderToets != null) {
                aan = branderToets.getAsBoolean();
                if (aan != lokaleBrander) {
                    lokaleBrander = aan;
                    if (meld != null) {
                        meld.accept(getId(), aan);
                    }
                }
            }
            zweef(aan, rijder);
        } else {
            lokaleBrander = false;
            if (level().isClientSide()) {
                setDeltaMovement(Vec3.ZERO);
            }
        }
        if (!level().isClientSide()) {
            serverTick();
        } else if (clientTick != null) {
            clientTick.accept(this);
        }
    }

    /**
     * One tick of floating: the burner on = up, off = slowly down; in the air and manned the wind takes it along, on the
     * ground it stands still. (The rider's game calls it every tick; tests call it on the server.)
     */
    public void zweef(boolean brander, boolean metWind) {
        double doel = brander ? STIJG : -ZAK;
        if (brander && getY() >= level().getMaxY() + 24) {
            doel = 0;                                         // (thin air up here)
        }
        if (getY() <= level().getMinY() + 3) {
            doel = Math.max(doel, 0.03);                      // it never sinks out of the world
        }
        vy += Mth.clamp(doel - vy, -0.004, 0.006);
        Vec3 nu = getDeltaMovement();
        double hx = nu.x, hz = nu.z;
        boolean staat = onGround() && vy <= 0;
        if (staat) {
            vy = -0.02;                                       // (resting: it keeps feeling for the ground)
            hx = hz = 0;
        } else if (metWind) {
            Vec3 w = wind(level(), getY(), level().getGameTime());
            hx += (w.x - hx) * 0.04;
            hz += (w.z - hz) * 0.04;
            float neus = (float) Math.toDegrees(Math.atan2(-w.x, w.z));
            setYRot(Mth.approachDegrees(getYRot(), neus, 0.5f));    // its snoet turns slowly into the way it goes
        } else {
            hx *= 0.9;
            hz *= 0.9;
        }
        setDeltaMovement(hx, vy, hz);
        move(MoverType.SELF, getDeltaMovement());
        if (verticalCollision && vy > 0) {
            vy = 0;
        }
        if (horizontalCollision) {
            setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
        }
    }

    private void serverTick() {
        if (!gekeken || tickCount % 20 == 0) {
            gekeken = true;
            UUID e = eigenaar();
            if (e != null && !Heteluchtballon.geldig(((ServerLevel) level()).getServer(), e, nr)) {
                weg();                                        // (an old copy: lek)
                return;
            }
        }
        LivingEntity rijder = getControllingPassenger();
        if (rijder == null && entityData.get(DATA_BRANDER)) {
            zetBrander(false);
        }
        if (entityData.get(DATA_BRANDER) && tickCount % 12 == 0) {
            level().playSound(null, this, E2VetbootjeFeature.BRANDER.get(), SoundSource.NEUTRAL, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        }
        if (rijder instanceof ServerPlayer sp && tickCount % 20 == 0 && !onGround()) {
            int streek = windstreek(wind(level(), getY(), level().getGameTime()));
            if (streek != windRichting) {                     // the wind turned (or you rose into another wind)
                windRichting = streek;
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.wind",
                        Component.translatable("gui.guhs.einde2_vetbootje.wind." + streek)).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private void weg() {
        ejectPassengers();
        ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 16, 0.6, 1.0, 0.6, 0.02);
        discard();
    }

    /** 1.21.1 isControlledByLocalInstance (26.1 made its replacement final): the rider's own game, or the server without rider. */
    public boolean bestuurdHier() {
        return getControllingPassenger() instanceof Player p ? p.isLocalPlayer() : !level().isClientSide();
    }

    private final InterpolationHandler glijden = new InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            lerpX = position.x;
            lerpY = position.y;
            lerpZ = position.z;
            lerpYRot = yRot;
            lerpSteps = 10;
        }

        @Override
        public boolean hasActiveInterpolation() {
            return lerpSteps > 0;
        }

        @Override
        public void interpolate() {
        }

        @Override
        public void cancel() {
            lerpSteps = 0;
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return glijden;
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    // --- riding -------------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) {
            if (e instanceof Player p && magSturen(p)) {
                return p;
            }
        }
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof Player p && magSturen(p) && getControllingPassenger() == null) {
            return true;
        }
        return getPassengers().size() - (getControllingPassenger() == null ? 0 : 1) < 1;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 voor = Vec3.directionFromRotation(0, getYRot());
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        double kant = passenger == getControllingPassenger() ? 0.2 : -0.22;
        return opzij.scale(kant).add(voor.scale(passenger == getControllingPassenger() ? -0.1 : 0.12)).add(0, 0.1, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            float draai = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(draai) < 45) {
                passenger.setYRot(passenger.getYRot() + draai);
                passenger.setYHeadRot(passenger.getYHeadRot() + draai);
            }
        }
    }

    /** Out of the basket high in the air: you float down instead of falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        Meevaren.zachtLanden(this, passenger);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.KAAS_KNABBELS.get())) {
            if (magSturen(player)) {
                Meevaren.wissel(this, sp, 1 - (getPassengers().size() - (getControllingPassenger() == null ? 0 : 1)));
            } else {
                nietVanJou(sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (magSturen(player)) {
                vouwOp(sp);
            } else {
                nietVanJou(sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isPassenger() && player.startRiding(this)) {
            if (getControllingPassenger() == player && !GuhQuests.saved(sp).getBooleanOr("guhs_einde2_vetbootje_ballon_uitleg", false)) {
                GuhQuests.saved(sp).putBoolean("guhs_einde2_vetbootje_ballon_uitleg", true);
                sp.sendSystemMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.uitleg", Component.keybind("key.jump")).withStyle(ChatFormatting.AQUA));
            }
            windRichting = -1;
        } else if (!player.isPassenger()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    private void nietVanJou(ServerPlayer sp) {
        Vloot.Boot b = Vloot.van(sp.level().getServer()).boot(eigenaar());
        sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.niet_van_jou", b.eigenaarNaam).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Server: folds the balloon up: everybody out, the item back to this player, the balloon gone. */
    public void vouwOp(ServerPlayer player) {
        ejectPassengers();
        Minigames.give(player, Heteluchtballon.stapel(eigenaar(), nr));
        level().playSound(null, blockPosition(), E2VetbootjeFeature.VOUW.get(), SoundSource.NEUTRAL, 0.9f, 1.15f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.opgevouwen").withStyle(ChatFormatting.GOLD));
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(E2VetbootjeFeature.HETELUCHTBALLON_ITEM.get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    // --- saving -------------------------------------------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        tag.read("Eigenaar", UUIDUtil.CODEC).ifPresent(this::zetEigenaar);
        nr = tag.getIntOr("Nr", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        UUID e = eigenaar();
        if (e != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, e);
        }
        tag.putInt("Nr", nr);
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.heteluchtballon.zweven");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
