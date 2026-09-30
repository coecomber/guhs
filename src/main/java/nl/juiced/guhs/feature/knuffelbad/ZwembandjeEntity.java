package nl.juiced.guhs.feature.knuffelbad;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The zwembandje: the pink swim ring (with a little guh head) you ride a water slide in. It follows the slide's path
 * ({@link GlijPad}) at the ride's time tau (ticks since the start) and your lateral (-1 left .. 1 right, you steer it).
 * <p>
 * The server's ride ({@link GlijRit}) drives it for everybody. The rider's own game runs the ride itself as well (the
 * path and the speed are the same on both sides), so it is perfectly smooth: it follows its own clock, gently pulled
 * towards the server's, steers without any delay and tells the server where it is every tick ({@code knuffelbad_stuur}).
 * Never saved: a ride doesn't survive the server stopping.
 */
public class ZwembandjeEntity extends Entity implements GeoEntity {
    public static final int WACHT = 0, GLIJDT = 1, PLONS = 2;
    /** Where the rider sits above the surface, and how far along the surface's normal the eyes go (they lean with the ring). */
    public static final double ZIT = 0.22, OGEN = 1.02;

    private static final EntityDataAccessor<Byte> DATA_BAAN = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<BlockPos> DATA_START = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Byte> DATA_FACING = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_TAU = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAT = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_FASE = SynchedEntityData.defineId(ZwembandjeEntity.class, EntityDataSerializers.BYTE);

    // --- client hooks (set by KnuffelbadClient; null on a dedicated server) ---
    /** The local player. */
    @Nullable
    public static Supplier<Player> lokaal;
    /** The steering input of the local player (-1 left .. 1 right). */
    @Nullable
    public static DoubleSupplier stuur;
    /** Every client tick of a ring (sounds, splashes, the camera's bookkeeping). */
    @Nullable
    public static Consumer<ZwembandjeEntity> clientTick;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private GlijPad.Baan baan;
    private int baanSleutel = Integer.MIN_VALUE;

    /** The ride's time and lateral this tick and the one before (the rider's own ones on the rider's client). */
    public double tau, tauO, lat, latO;
    /** Client: this ring carries the local player, who runs the ride on its own clock. */
    public boolean eigen;
    private boolean gestart;

    public ZwembandjeEntity(EntityType<? extends ZwembandjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BAAN, (byte) 0);
        builder.define(DATA_START, BlockPos.ZERO);
        builder.define(DATA_FACING, (byte) Direction.NORTH.get3DDataValue());
        builder.define(DATA_TAU, 0f);
        builder.define(DATA_LAT, 0f);
        builder.define(DATA_FASE, (byte) WACHT);
    }

    // --- the slide ------------------------------------------------------------------------------------------------------

    public void zetBaan(Glijbaan glijbaan, BlockPos start, Direction facing) {
        entityData.set(DATA_BAAN, (byte) glijbaan.ordinal());
        entityData.set(DATA_START, start);
        entityData.set(DATA_FACING, (byte) facing.get3DDataValue());
        baan = null;
    }

    public Glijbaan glijbaan() {
        return Glijbaan.byIndex(entityData.get(DATA_BAAN));
    }

    public GlijPad.Baan baan() {
        int key = entityData.get(DATA_BAAN) * 31 + entityData.get(DATA_START).hashCode() * 7 + entityData.get(DATA_FACING);
        if (baan == null || key != baanSleutel) {
            Direction facing = Direction.from3DDataValue(entityData.get(DATA_FACING));
            baan = new GlijPad.Baan(glijbaan().pad(), entityData.get(DATA_START), facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
            baanSleutel = key;
        }
        return baan;
    }

    public int fase() {
        return entityData.get(DATA_FASE);
    }

    public double serverTau() {
        return entityData.get(DATA_TAU);
    }

    /** The ride's spot at time tau and lateral lat (world). */
    public GlijPad.Stand stand(double tau, double lat) {
        GlijPad.Baan b = baan();
        return b.stand(b.pad().sAt(tau), lat);
    }

    /** For drawing: the ride between two ticks. */
    public double tekenTau(float partialTick) {
        return Mth.lerp(partialTick, tauO, tau);
    }

    public double tekenLat(float partialTick) {
        return Mth.lerp(partialTick, latO, lat);
    }

    /** Server: where the ride is (from GlijRit) - moves the ring and tells everyone. */
    public void volg(int fase, double tau, double lat) {
        entityData.set(DATA_FASE, (byte) fase);
        entityData.set(DATA_TAU, (float) tau);
        entityData.set(DATA_LAT, (float) lat);
        this.tauO = this.tau;
        this.latO = this.lat;
        this.tau = tau;
        this.lat = lat;
        zetOp(stand(tau, lat));
    }

    private void zetOp(GlijPad.Stand st) {
        setPos(st.pos());
        Vec3 t = st.tangent();
        setYRot((float) Math.toDegrees(Math.atan2(-t.x, t.z)));
        setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(t.y, -1, 1))));
    }

    // --- ticking ----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (!GlijRit.tickVan(this) && tickCount > 5) {
                ejectPassengers();
                discard();                      // (no ride: a lost ring)
            }
            return;
        }
        Player me = lokaal == null ? null : lokaal.get();
        eigen = me != null && getPassengers().contains(me);
        tauO = tau;
        latO = lat;
        if (eigen) {
            voorspel();
        } else {
            tau = serverTau();
            lat = entityData.get(DATA_LAT);
            gestart = false;
        }
        if (clientTick != null) {
            clientTick.accept(this);
        }
    }

    /**
     * The rider's own ride: its clock runs by itself (pulled gently towards the server's, which is a little behind in
     * what it has told us), steering is immediate: the stick (A/D) plus the push outwards in the bends (you ride up the
     * wall of a funnel or a tube) and the slide's shape that brings you back to its middle.
     */
    private void voorspel() {
        GlijPad pad = baan().pad();
        int fase = fase();
        double server = serverTau();
        if (fase == WACHT) {
            tau = 0;
            gestart = false;
        } else if (!gestart) {
            tau = Math.max(0, server);
            gestart = true;
        } else if (tau < pad.duur()) {
            double err = server + 1 - tau;
            tau = Math.abs(err) > 12 ? server + 1 : tau + 1 + Mth.clamp(err * 0.08, -0.25, 0.25);
            tau = Math.min(tau, pad.duur());
        }
        double s = pad.sAt(tau);
        int prof = pad.profiel(s);
        if (fase == GLIJDT && prof != GlijPad.LUCHT && prof != GlijPad.WATER) {
            double v = pad.snelheid(tau);
            double kappa = pad.bocht(s);
            double doel = Mth.clamp(-kappa * v * v * 14.0, -0.9, 0.9);
            double in = stuur == null ? 0 : stuur.getAsDouble();
            lat += in * 0.085 + (doel - lat) * 0.05;
            lat = Mth.clamp(lat, -1, 1);
        } else if (prof == GlijPad.WATER) {
            lat *= 0.9;
        }
        zetOp(stand(tau, lat));
        if (fase == GLIJDT && tickCount % 1 == 0) {
            KnuffelbadPayloads.stuur(getId(), (float) tau, (float) lat);
        }
    }

    /** The rider's client moves the ring itself: the server's position packets would only make it shake. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!eigen) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    // --- riding ----------------------------------------------------------------------------------------------------------

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        GlijPad.Stand st = stand(tau, lat);
        Vec3 n = st.normal();
        // sitting in the ring, leaning with it: the eyes end up along the surface's normal
        return n.scale(ZIT).add(n.subtract(0, 1, 0).scale(OGEN));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(getYRot());
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
