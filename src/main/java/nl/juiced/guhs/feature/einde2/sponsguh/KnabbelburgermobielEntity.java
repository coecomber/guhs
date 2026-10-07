package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.RijGuard;

/**
 * einde2, slice sponsguh_wezens: the Knabbelburgermobiel, a car shaped like a Knabbelburger (the bottom bun on four pickle
 * wheels, the patty, cheese and lettuce, the sesame top bun as a roof) for a driver and two passengers. Not one of the cast:
 * a vehicle, and a story vehicle only: no item, no recipe, only {@link Knabbelburgermobiel#zet} makes one.
 * <p>
 * It drives on the ground: slow, it climbs a whole block, and it never drives into the void: at an edge it stops
 * ({@link #heeftGrond}). The SERVER drives it from the keys the driver holds (the game sends those; a slow story car needs
 * no client prediction), so everybody sees the same ride. {@code voor}: only that player may drive (anybody may ride
 * along); null: anybody may drive.
 * <p>
 * hook fase B: the trip of the second part of the Sponsguh story (its cast rides along through {@link #zetMee}).
 */
public class KnabbelburgermobielEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_RIJDT = SynchedEntityData.defineId(KnabbelburgermobielEntity.class, EntityDataSerializers.BOOLEAN);
    /** Blocks per tick: forwards, and backwards; degrees per tick it turns at speed. */
    public static final double TOP = 0.22, ACHTERUIT = 0.1;
    public static final float DRAAI = 3.2f;
    public static final int PLAATSEN = 3;
    /** How far ahead it looks for ground, and how deep a drop it still takes. */
    public static final double KIJK_VOORUIT = 1.5;
    public static final int MAX_AFSTAP = 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Only this player may drive (null: anybody). */
    @Nullable
    private UUID voor;
    private double snelheid;
    private int randBericht;
    /** Tests (a mock player holds no keys): {vooruit, stuur} instead of the driver's keys, while somebody is at the wheel. */
    @Nullable
    float[] proefInvoer;

    public KnabbelburgermobielEntity(EntityType<? extends KnabbelburgermobielEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RIJDT, false);
    }

    @Nullable
    public UUID voor() {
        return voor;
    }

    void zetVoor(@Nullable UUID voor) {
        this.voor = voor;
    }

    /** May this player take the wheel? */
    public boolean magRijden(Player player) {
        return voor == null || voor.equals(player.getUUID());
    }

    public boolean rijdt() {
        return entityData.get(DATA_RIJDT);
    }

    public double snelheid() {
        return snelheid;
    }

    /** The driver: the first rider, when that is a player who may drive it. */
    @Nullable
    public Player bestuurder() {
        return getFirstPassenger() instanceof Player p && magRijden(p) ? p : null;
    }

    // --- driving ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // (26.1: the server's position updates only set a target in the handler; a plain Entity must take the steps itself,
            // as a boat and a living entity do. Without this the car stays where it appeared on every client)
            interpolation.interpolate();
            return;
        }
        float vooruit = 0, stuur = 0;
        if (proefInvoer != null && bestuurder() != null) {
            vooruit = proefInvoer[0];
            stuur = proefInvoer[1];
        } else if (bestuurder() instanceof ServerPlayer rijder) {
            Input in = rijder.getLastClientInput();
            vooruit = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0);
            stuur = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
        }
        rijd(vooruit, stuur);
        if (voor != null && tickCount % 20 == 0) {
            ServerPlayer baas = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(voor);
            if (baas != null && !Knabbelburgermobiel.isDeEchte(baas, getUUID())) {
                opgeruimd();                                                 // (tidied up, or its player got a new one)
            }
        }
        if (getY() < level().getMinY() - 16) {
            discard();
        }
    }

    /**
     * One tick of driving: vooruit 1 = forwards, -1 = backwards, 0 = rolling out; stuur 1 = to the right, -1 = to the left.
     * (The tick calls it with the driver's keys; tests call it themselves.)
     */
    public void rijd(float vooruit, float stuur) {
        if (vooruit > 0.1f) {
            snelheid = Math.min(TOP, snelheid + 0.012);
        } else if (vooruit < -0.1f) {
            snelheid = Math.max(-ACHTERUIT, snelheid - 0.012);
        } else {
            snelheid *= 0.8;
            if (Math.abs(snelheid) < 0.004) {
                snelheid = 0;
            }
        }
        if (Math.abs(snelheid) > 0.01) {
            setYRot(getYRot() + stuur * DRAAI * (snelheid < 0 ? -1 : 1));
        }
        Vec3 richting = Vec3.directionFromRotation(0, getYRot());
        // never into the void: no ground ahead = it stops
        if (snelheid != 0 && onGround()) {
            Vec3 voorruit = position().add(richting.scale(Math.signum(snelheid) * KIJK_VOORUIT));
            if (!heeftGrond(level(), voorruit)) {
                snelheid = 0;
                if (randBericht <= 0 && bestuurder() instanceof ServerPlayer rijder) {
                    randBericht = 60;
                    rijder.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.mobiel.rand").withStyle(ChatFormatting.GOLD));
                    level().playSound(null, blockPosition(), E2SponsguhFeature.MOBIEL_TOET.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
                }
            }
        }
        if (randBericht > 0) {
            randBericht--;
        }
        double vy = onGround() ? -0.08 : Math.max(-1.2, getDeltaMovement().y - 0.08);
        setDeltaMovement(richting.x * snelheid, vy, richting.z * snelheid);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            snelheid *= 0.3;
        }
        boolean rijdt = Math.abs(snelheid) > 0.02;
        if (rijdt != rijdt()) {
            entityData.set(DATA_RIJDT, rijdt);
        }
        if (rijdt && tickCount % 30 == 0) {
            level().playSound(null, blockPosition(), E2SponsguhFeature.MOBIEL_MOTOR.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + (float) Math.abs(snelheid));
        }
    }

    /** Is there something to drive on under this spot: a block with a collision shape at most {@value #MAX_AFSTAP} blocks below it? */
    public static boolean heeftGrond(Level level, Vec3 plek) {
        BlockPos p = BlockPos.containing(plek.x, plek.y + 0.1, plek.z);
        for (int dy = 0; dy <= MAX_AFSTAP + 1; dy++) {
            BlockPos onder = p.below(dy);
            if (!level.getBlockState(onder).getCollisionShape(level, onder).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    private final net.minecraft.world.entity.InterpolationHandler interpolation = new net.minecraft.world.entity.InterpolationHandler(this);

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // --- riding -------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() || player.getVehicle() == this) {
            return InteractionResult.PASS;
        }
        stapIn(player);
        return InteractionResult.SUCCESS;
    }

    /**
     * This player gets in: behind the wheel when the car is empty and it is theirs to drive, else on a passenger seat next
     * to a driver. False: full, or an empty car that is somebody else's (nobody could drive it).
     */
    public boolean stapIn(Player player) {
        if (getPassengers().size() >= PLAATSEN || !RijGuard.mag(player, this)) {
            return false;
        }
        if (getPassengers().isEmpty() && !magRijden(player)) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.mobiel.niet_van_jou").withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        return player.startRiding(this);
    }

    /** hook fase B: a passenger that is not a player (Sponsguh and Patguh ride along). False: full, or the car is empty (the driver's seat stays free). */
    public boolean zetMee(Entity e) {
        if (level().isClientSide() || getPassengers().isEmpty() || getPassengers().size() >= PLAATSEN || !RijGuard.mag(e, this)) {
            return false;
        }
        return e.startRiding(this, true, true);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < PLAATSEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        // the driver sits at the wheel (right), the two passengers a little behind, left and right
        Vec3 plek = index == 0 ? new Vec3(-0.28, 0.8, 0.12) : new Vec3(index == 1 ? 0.34 : -0.34, 0.8, -0.3);
        return plek.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            float turn = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(turn) < 45) {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        } else {
            passenger.setYRot(getYRot());
            passenger.setYBodyRot(getYRot());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()) {
            opgeruimd();                                                     // (creative: a sneak-hit takes it away)
            return true;
        }
        return false;
    }

    /** Gone in a puff of crumbs (riders step out first). */
    void opgeruimd() {
        if (level() instanceof ServerLevel level) {
            ejectPassengers();
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 16, 0.6, 0.4, 0.8, 0.02);
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

    // --- saving -------------------------------------------------------------------------------------------------------------

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

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RIJ = RawAnimation.begin().thenLoop("rij");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KnabbelburgermobielEntity>("rij", 4, state -> state.setAndContinue(rijdt() ? RIJ : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
