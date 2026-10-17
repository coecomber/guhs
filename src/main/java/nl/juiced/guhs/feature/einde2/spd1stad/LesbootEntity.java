package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.RijGuard;

/**
 * einde2 "Sponsguh", slice sp_d1_stad (CONTRACT_E2_C 7.4): juf Pufguh's lesbootje for the boating exam. A vetbootje of the
 * boating school (it wears the vetbootje's model, read-only: client/E2SpD1StadClient) that sails ON the road of the course, as
 * every boat of Bikinibodem does. Driven by the SERVER from the keys of its one player (as the Knabbelburgermobiel): W/S, A/D;
 * it climbs one block (the bridge's steps), it is never anybody else's ({@link Examen} puts it down with
 * {@code Zicht.alleenVoor}), it is a story vehicle only (no item). A knock against something at speed is a bump
 * ({@link #BOTSING}): the exam counts it; the boat is never hurt and nobody else is either.
 */
public class LesbootEntity extends Entity implements GeoEntity {
    /** Blocks per tick forwards / backwards; degrees per tick it turns at speed (also a little on the spot). */
    public static final double TOP = 0.26, ACHTERUIT = 0.1;
    public static final float DRAAI = 4.0f;
    /** A knock at more than this speed is a bump; then this many ticks nothing counts. */
    public static final double BOTS_SNELHEID = 0.06;
    public static final int BOTS_RUST = 16;
    /** Who is told of a bump (the exam). */
    public static volatile Consumer<LesbootEntity> BOTSING = b -> {
    };

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    /** Only this player may sail it (null: anybody, a test). */
    @Nullable
    private UUID voor;
    private double snelheid;
    private int botsRust;
    /** Tests (a mock player holds no keys): {vooruit, stuur} instead of the driver's keys. */
    @Nullable
    float[] proefInvoer;

    public LesbootEntity(EntityType<? extends LesbootEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Nullable
    public UUID voor() {
        return voor;
    }

    void zetVoor(@Nullable UUID voor) {
        this.voor = voor;
    }

    public boolean magVaren(Player player) {
        return voor == null || voor.equals(player.getUUID());
    }

    /** The driver: the first rider, when that is the boat's own player. */
    @Nullable
    public Player stuurman() {
        return getFirstPassenger() instanceof Player p && magVaren(p) ? p : null;
    }

    public double snelheid() {
        return snelheid;
    }

    /** Stops dead (a scene takes over, the exam starts again). */
    public void stop() {
        snelheid = 0;
        setDeltaMovement(Vec3.ZERO);
    }

    // --- sailing ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            if (Math.abs(snelheidClient()) > 0.05 && random.nextInt(3) == 0) {
                Vec3 achter = position().add(Vec3.directionFromRotation(0, getYRot()).scale(-0.9));
                level().addParticle(ParticleTypes.BUBBLE_POP, achter.x, achter.y + 0.2, achter.z, 0, 0.02, 0);
            }
            return;
        }
        float vooruit = 0, stuur = 0;
        if (proefInvoer != null) {
            vooruit = proefInvoer[0];
            stuur = proefInvoer[1];
        } else if (stuurman() instanceof ServerPlayer rijder) {
            Input in = rijder.getLastClientInput();
            vooruit = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0);
            stuur = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
        }
        vaar(vooruit, stuur);
        if (getY() < level().getMinY() - 16) {
            discard();
        }
    }

    private double snelheidClient() {
        return getDeltaMovement().horizontalDistance();
    }

    /** One tick: vooruit 1 / -1 / 0, stuur 1 = right, -1 = left. (The tick calls it with the driver's keys; tests too.) */
    public void vaar(float vooruit, float stuur) {
        if (vooruit > 0.1f) {
            snelheid = Math.min(TOP, snelheid + 0.014);
        } else if (vooruit < -0.1f) {
            snelheid = Math.max(-ACHTERUIT, snelheid - 0.014);
        } else {
            snelheid *= 0.82;
            if (Math.abs(snelheid) < 0.004) {
                snelheid = 0;
            }
        }
        // it turns at speed, and slowly on the spot (so a nose in a corner always comes free)
        float draai = Math.abs(snelheid) > 0.01 ? DRAAI * (snelheid < 0 ? -1 : 1) : DRAAI * 0.5f;
        if (stuur != 0) {
            setYRot(getYRot() + stuur * draai);
        }
        Vec3 richting = Vec3.directionFromRotation(0, getYRot());
        double vy = onGround() ? -0.08 : Math.max(-1.2, getDeltaMovement().y - 0.08);
        setDeltaMovement(richting.x * snelheid, vy, richting.z * snelheid);
        double voor = Math.abs(snelheid);
        move(MoverType.SELF, getDeltaMovement());
        if (botsRust > 0) {
            botsRust--;
        }
        if (horizontalCollision) {
            if (voor > BOTS_SNELHEID && botsRust == 0 && stuurman() != null) {
                botsRust = BOTS_RUST;
                level().playSound(null, blockPosition(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.9f, 0.8f);
                if (level() instanceof ServerLevel s) {
                    s.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.5, getZ(), 12, 0.6, 0.3, 0.6, 0.05);
                }
                BOTSING.accept(this);
            }
            snelheid *= 0.3;
        }
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // --- riding ----------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.getVehicle() == this || !magVaren(player)) {
            return InteractionResult.PASS;
        }
        if (getPassengers().stream().noneMatch(e -> e instanceof Player) && RijGuard.mag(player, this)) {
            // the player takes the helm: whoever sat down first (Sponsguh) moves to the front seat
            var andere = java.util.List.copyOf(getPassengers());
            andere.forEach(Entity::stopRiding);
            player.startRiding(this);
            andere.forEach(e -> e.startRiding(this, true, true));
        }
        return InteractionResult.SUCCESS;
    }

    /** Sponsguh sits down in the front seat (only next to the player; false: full or no player aboard). */
    public boolean zetMee(Entity e) {
        if (level().isClientSide() || getPassengers().size() >= 2 || !RijGuard.mag(e, this)) {
            return false;
        }
        return e.startRiding(this, true, true);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;   // (the server steers from the keys; no client prediction)
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 voor = Vec3.directionFromRotation(0, getYRot());
        boolean stuurman = passenger instanceof Player;
        return voor.scale(stuurman ? -0.42 : 0.55).add(0, stuurman ? 0.55 : 0.6, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        float draai = Mth.wrapDegrees(getYRot() - yRotO);
        if (passenger instanceof Player) {
            if (Math.abs(draai) < 45) {
                passenger.setYRot(passenger.getYRot() + draai);
                passenger.setYHeadRot(passenger.getYHeadRot() + draai);
            }
        } else {
            passenger.setYRot(getYRot());
            if (passenger instanceof LivingEntity l) {
                l.setYBodyRot(getYRot());
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()) {
            weg();
            return true;
        }
        return false;
    }

    /** Gone in a puff of bubbles (riders step out first). */
    public void weg() {
        if (level() instanceof ServerLevel level) {
            ejectPassengers();
            level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.6, getZ(), 20, 0.7, 0.4, 0.7, 0.02);
        }
        discard();
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
    public boolean shouldRiderSit() {
        return true;
    }

    // --- saving (never kept: Examen throws a loaded one away; the owner is saved for a test's sake) ------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        if (voor != null) {
            tag.store("Voor", UUIDUtil.CODEC, voor);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        voor = tag.read("Voor", UUIDUtil.CODEC).orElse(null);
    }

    // --- GeckoLib (the vetbootje's own floating loop) ------------------------------------------------------------------------

    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.vetbootje.zweven");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LesbootEntity>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
