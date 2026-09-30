package nl.juiced.guhs.feature.guhwaiispellen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.spelen.Niveau;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

/**
 * A surfplankje on the waves of Guhwai'i (3.0): Lilo-guh's loaned board that you stand on (or Lilo-guh's own, with her
 * standing on it: {@link #isLilo}). Not a living thing and never saved: the server's {@link SurfSpel} puts it where the
 * ride ({@link SurfSim}) is every tick; the surfer's own game runs the ride itself and puts its board there at once
 * ({@link #eigen}: then the server's positions are ignored), so riding is smooth. The waves are drawn from the synced
 * surf spot (origin, direction, level, seed) and step.
 */
public class SurfPlankEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<BlockPos> DATA_ORIGIN = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> DATA_HOEK = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_NIVEAU = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_SEED = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAP = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LILO = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FASE = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_RICHTING = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HELLING = SynchedEntityData.defineId(SurfPlankEntity.class, EntityDataSerializers.FLOAT);

    /** How high the rider stands above the board's spot. */
    public static final double STAAN = 0.16;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: this board carries the local player, whose own game runs the ride (server positions ignored). */
    public boolean eigen;
    /** Client (eigen): the board's yaw and pitch from the own ride, this tick and the one before. */
    public float eigenYaw, eigenYawO, eigenHelling, eigenHellingO;
    /** Server: ticks without a game that wants it (then it goes). */
    private int wees;

    public SurfPlankEntity(EntityType<? extends SurfPlankEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ORIGIN, BlockPos.ZERO);
        builder.define(DATA_HOEK, 0f);
        builder.define(DATA_NIVEAU, (byte) Niveau.MEDIUM.ordinal());
        builder.define(DATA_SEED, 0);
        builder.define(DATA_STAP, 0);
        builder.define(DATA_LILO, false);
        builder.define(DATA_FASE, (byte) 0);
        builder.define(DATA_RICHTING, 0f);
        builder.define(DATA_HELLING, 0f);
    }

    // --- the surf spot and the ride (synced) ----------------------------------------------------------------------------

    public void zetSpot(Surfplek.Spot spot, Niveau niveau, int seed, boolean lilo) {
        entityData.set(DATA_ORIGIN, spot.origin());
        entityData.set(DATA_HOEK, (float) spot.hoek());
        entityData.set(DATA_NIVEAU, (byte) niveau.ordinal());
        entityData.set(DATA_SEED, seed);
        entityData.set(DATA_LILO, lilo);
    }

    public Surfplek.Spot spot() {
        return new Surfplek.Spot(entityData.get(DATA_ORIGIN), entityData.get(DATA_HOEK));
    }

    public Niveau niveau() {
        return Niveau.of(entityData.get(DATA_NIVEAU));
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public int stap() {
        return entityData.get(DATA_STAP);
    }

    public boolean isLilo() {
        return entityData.get(DATA_LILO);
    }

    public SurfSim.Fase fase() {
        return SurfSim.Fase.values()[Math.max(0, Math.min(SurfSim.Fase.values().length - 1, entityData.get(DATA_FASE)))];
    }

    /** The board's yaw (degrees, Minecraft style) as the server's ride has it. */
    public float richting() {
        return entityData.get(DATA_RICHTING);
    }

    public float helling() {
        return entityData.get(DATA_HELLING);
    }

    /** Server: puts the board where the ride is (every tick). */
    public void volg(SurfSim sim) {
        Surfplek.Spot spot = spot();
        Vec3 p = spot.wereld(sim.u(), sim.v(), sim.hoogte());
        setPos(p.x, p.y, p.z);
        entityData.set(DATA_STAP, sim.step());
        entityData.set(DATA_FASE, (byte) sim.fase().ordinal());
        float yaw = spot.yaw(sim);
        float helling = (float) Math.toDegrees(sim.helling());
        entityData.set(DATA_RICHTING, yaw);
        entityData.set(DATA_HELLING, helling);
        setYRot(yRotO + net.minecraft.util.Mth.wrapDegrees(yaw - yRotO));
        setXRot(net.minecraft.util.Mth.clamp(helling, -80f, 80f));
        wees = 0;
    }

    // --- entity -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ++wees > 60) {
            discard();                                   // (its game is over or gone)
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!eigen) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player && !isLilo();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, STAAN, 0);
    }

    @Nullable
    public Player rijder() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public boolean isPickable() {
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
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
