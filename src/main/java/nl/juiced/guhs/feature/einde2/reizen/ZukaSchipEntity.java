package nl.juiced.guhs.feature.einde2.reizen;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2 phase B, slice reizen: Zuka's ship in the chase between the chapters: a grey Mika warship with a big smokestack
 * and red sails, flying through the void (model: einde2_reizen_modellen.py, drawn at {@link #SCHAAL}). It is a puppet: the
 * chase ({@link Jacht}) tells it where to be and what to look at every tick; without that for two seconds it is gone (a
 * restart, the player left). Only the hunted player and their guests get to know about it ({@link #broadcastToPlayer});
 * nothing can hit it, push it or ride it but Zuka (ZUKA_BALLING) at its bow. Never saved.
 */
public class ZukaSchipEntity extends Entity implements GeoEntity {
    /** How much bigger than its model it is drawn (E2ReizenClient). */
    public static final float SCHAAL = 4f;
    /** Blocks per tick it may move at most (Appa does 0.92 on a journey, 1.2 when he hurries: einde2 fix-reizen). */
    public static final double MAX_VAART = 1.6;
    /** Where Zuka stands: on the bow, in the ship's own frame (+z forward), blocks. */
    static final Vec3 BOEG = new Vec3(0, 2.1, 3.6);
    /** Where the fireballs leave from (the bow cannon), in the ship's own frame. */
    static final Vec3 KANON = new Vec3(0, 2.6, 5.2);

    private static final EntityDataAccessor<Boolean> DATA_SPUTTER = SynchedEntityData.defineId(ZukaSchipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation VAAR = RawAnimation.begin().thenLoop("vaar");
    private static final RawAnimation SPUTTER = RawAnimation.begin().thenLoop("sputter");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> kijkers = new HashSet<>();
    @Nullable
    private Vec3 doelPlek;
    @Nullable
    private Vec3 kijkNaar;
    private int levend = 40;
    private int lerpStappen;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;

    public ZukaSchipEntity(EntityType<? extends ZukaSchipEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPUTTER, false);
    }

    // --- the puppet strings ---------------------------------------------------------------------------------------------------

    /** Who may know about it (the hunted player and their guests). */
    void zetKijkers(Set<UUID> wie) {
        kijkers.clear();
        kijkers.addAll(wie);
    }

    /** This tick: be there, look at that (the chase calls this every tick; without it the ship goes). */
    void stuur(Vec3 plek, @Nullable Vec3 kijk) {
        doelPlek = plek;
        kijkNaar = kijk;
        levend = 40;
    }

    /** The engine gives up: black smoke, and it sinks away behind them. */
    void sputter() {
        entityData.set(DATA_SPUTTER, true);
    }

    public boolean sputtert() {
        return entityData.get(DATA_SPUTTER);
    }

    /** The bow cannon in the world. */
    Vec3 kanon() {
        return position().add(KANON.yRot(-getYRot() * Mth.DEG_TO_RAD));
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return kijkers.contains(player.getUUID());
    }

    // --- every tick -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (lerpStappen > 0) {
                double f = 1.0 / lerpStappen;
                setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYRot - getYRot()) * (float) f);
                lerpStappen--;
            }
            rook();
            return;
        }
        if (--levend < 0) {
            ejectPassengers();
            discard();
            return;
        }
        if (doelPlek != null) {
            Vec3 d = doelPlek.subtract(position());
            double lang = d.length();
            Vec3 stap = lang > MAX_VAART ? d.scale(MAX_VAART / lang) : d;
            setPos(getX() + stap.x, getY() + stap.y, getZ() + stap.z);
        }
        if (kijkNaar != null) {
            Vec3 k = kijkNaar.subtract(position());
            if (k.horizontalDistanceSqr() > 1e-4) {
                float yaw = (float) (Mth.atan2(k.z, k.x) * Mth.RAD_TO_DEG) - 90f;
                setYRot(Mth.approachDegrees(getYRot(), yaw, 4f));
            }
        }
    }

    /** (client) the smokestack: grey puffs while it hunts, thick black ones when the engine gives up. */
    private void rook() {
        if (tickCount % 2 != 0) {
            return;
        }
        Vec3 schoorsteen = position().add(new Vec3(0, 6.6, -1.2).yRot(-getYRot() * Mth.DEG_TO_RAD));
        if (sputtert()) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, schoorsteen.x, schoorsteen.y, schoorsteen.z, (random.nextDouble() - 0.5) * 0.1, 0.05, 0);
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + random.nextGaussian() * 2, getY() + 1.5, getZ() + random.nextGaussian() * 3, 0, -0.05, 0);
            }
        } else {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, schoorsteen.x, schoorsteen.y, schoorsteen.z, 0, 0.08, 0);
        }
    }

    private final InterpolationHandler glijden = new InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            lerpX = position.x;
            lerpY = position.y;
            lerpZ = position.z;
            lerpYRot = yRot;
            lerpStappen = 3;
        }

        @Override
        public boolean hasActiveInterpolation() {
            return lerpStappen > 0;
        }

        @Override
        public void interpolate() {
        }

        @Override
        public void cancel() {
            lerpStappen = 0;
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return glijden;
    }

    // --- Zuka at the bow ------------------------------------------------------------------------------------------------------

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return BOEG.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYRot(getYRot());
        passenger.setYHeadRot(getYRot());
    }

    // --- nothing touches it ---------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
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

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZukaSchipEntity>("vaar", 10, state -> state.setAndContinue(sputtert() ? SPUTTER : VAAR)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
