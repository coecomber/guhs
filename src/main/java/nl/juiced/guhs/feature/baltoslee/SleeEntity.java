package nl.juiced.guhs.feature.baltoslee;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * The Nomguh sled (guhs:baltoslee_slee): a wooden sled with a red blanket, the handlebar with sled bells, a lantern, and -
 * after the berghut - the medicine chest. You stand on its runners and steer it over the marked route through the storm;
 * in front run the guh-sledehondjes (and on the medicine ride Baltoguh leads them). The dogs are drawn by the client, they
 * are no real entities. Steele-Mika's sled in the sledesprint is the same entity ({@link #STEELE}), driven by the server.
 * <p>
 * Like the Knuffelbad's zwembandje it is no living entity and it never collides: its place is always "leg {@code been} of
 * the ride's route ({@link RitRoute}, in the synced data), {@code s} blocks along, {@code lat} to the side". The rider's own
 * game drives it (it follows the rider's keys at once, {@link SleeRijden}) and tells the server where it is every tick
 * ({@code baltoslee_stuur}); the server checks that and does the rest ({@link SleeRit}). When the server puts the sled back
 * (buried by an avalanche, fallen off an ice bridge) it counts up {@link #gen()}: the rider's game then takes the server's
 * spot and the server ignores what the rider's game still says about the old one (the 2.10 lesson: a client-driven vehicle
 * must never "win" against a server reset). Never saved.
 */
public class SleeEntity extends Entity implements GeoEntity {
    /** What kind of sled: the medicine ride (Baltoguh leads), the sledesprint (your own team), Steele-Mika's. */
    public static final int TOCHT = 0, SPRINT = 1, STEELE = 2;
    /** Its phase: countdown, riding, pausing (berghut, dieptepunt, a rest), stuck (digging out of the snow), done. */
    public static final int WACHT = 0, RIJDT = 1, PAUZE = 2, VAST = 3, KLAAR = 4;
    /** Why it pauses. */
    public static final int GEEN = 0, BERGHUT = 1, DIEPTEPUNT = 2, RUST = 3;
    /** Entity events for the games that see it (effects). */
    /** (100+: 60-65 collided with vanilla, 63 is the Sniffer id the client casts; see entity.EntiteitEvents.) */
    public static final byte EV_BEDOLVEN = 100, EV_ONTWEKEN = 101, EV_PLOF = 102, EV_RUST = 103, EV_KEER = 104;
    /** Where the musher stands on the runners (behind the middle) and how high. */
    public static final double ACHTER = 1.2, OP_DE_LATTEN = 0.22;

    private static final EntityDataAccessor<CompoundTag> DATA_ROUTE = SynchedEntityData.defineId(SleeEntity.class, BaltoSleeFeature.COMPOUND_TAG);
    private static final EntityDataAccessor<Byte> DATA_SOORT = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_FASE = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_PAUZE = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_BEEN = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_NIVEAU = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_S = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAT = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_V = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LATV = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GEN = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_STORM = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WARMTE = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_KIST = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SEED = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.INT);
    /** The clock for the panel: the race time, or the time left for the way back (ticks; -1 none). */
    private static final EntityDataAccessor<Integer> DATA_TIJD = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.INT);
    /** Steele-Mika's progress in the race (0 .. 2 legs; -1 none), and the ticks left of a pause / countdown. */
    private static final EntityDataAccessor<Float> DATA_TEGEN = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WACHT = SynchedEntityData.defineId(SleeEntity.class, EntityDataSerializers.INT);

    // --- client hooks (set by BaltoSleeClient; null on a dedicated server) ---
    @Nullable
    public static Supplier<Player> lokaal;
    @Nullable
    public static Supplier<SleeRijden.Invoer> invoer;
    @Nullable
    public static Consumer<SleeEntity> clientTick;
    @Nullable
    public static Consumer<SleeEntity> clientEvent;
    /** The last entity event (for clientEvent). */
    public byte laatsteEvent;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private RitRoute route;
    @Nullable
    private CompoundTag routeTag;

    /** Where it is this tick and the one before (the rider's own on the rider's game). */
    public double s, sO, lat, latO, v, latV;
    public int been, beenO;
    /** Client: the local player rides this sled (and drives it). */
    public boolean eigen;
    private int mijnGen = Integer.MIN_VALUE, mijnBeen = -1;

    // --- client only: for drawing (the dogs' eased spots, the sled's eased turn and lean) ---
    public final Vec3[] honden = new Vec3[5];
    public final float[] hondYaw = new float[5];
    public float tekenYaw = Float.NaN, lean, leanO;
    public int loopTik;

    public SleeEntity(EntityType<? extends SleeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_ROUTE, new CompoundTag());
        b.define(DATA_SOORT, (byte) TOCHT);
        b.define(DATA_FASE, (byte) WACHT);
        b.define(DATA_PAUZE, (byte) GEEN);
        b.define(DATA_BEEN, (byte) 0);
        b.define(DATA_NIVEAU, (byte) 0);
        b.define(DATA_S, 0f);
        b.define(DATA_LAT, 0f);
        b.define(DATA_V, 0f);
        b.define(DATA_LATV, 0f);
        b.define(DATA_GEN, 0);
        b.define(DATA_STORM, 0f);
        b.define(DATA_WARMTE, 100f);
        b.define(DATA_KIST, false);
        b.define(DATA_SEED, 0);
        b.define(DATA_TIJD, -1);
        b.define(DATA_TEGEN, -1f);
        b.define(DATA_WACHT, 0);
    }

    // --- the ride's data ----------------------------------------------------------------------------------------------------

    /** Server: sets up a new sled of this kind on this route. */
    public void zet(RitRoute route, int soort, int niveau, int seed) {
        entityData.set(DATA_ROUTE, route.tag());
        entityData.set(DATA_SOORT, (byte) soort);
        entityData.set(DATA_NIVEAU, (byte) niveau);
        entityData.set(DATA_SEED, seed);
        this.route = route;
        this.routeTag = route.tag();
    }

    @Nullable
    public RitRoute route() {
        CompoundTag tag = entityData.get(DATA_ROUTE);
        if (tag.isEmpty()) {
            return null;
        }
        if (route == null || tag != routeTag) {
            route = RitRoute.lees(tag);
            routeTag = tag;
        }
        return route;
    }

    public int soort() {
        return entityData.get(DATA_SOORT);
    }

    public int fase() {
        return entityData.get(DATA_FASE);
    }

    public int pauze() {
        return entityData.get(DATA_PAUZE);
    }

    public int niveau() {
        return entityData.get(DATA_NIVEAU);
    }

    public int gen() {
        return entityData.get(DATA_GEN);
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public float storm() {
        return entityData.get(DATA_STORM);
    }

    public float warmte() {
        return entityData.get(DATA_WARMTE);
    }

    public boolean kist() {
        return entityData.get(DATA_KIST);
    }

    public int tijd() {
        return entityData.get(DATA_TIJD);
    }

    public float tegen() {
        return entityData.get(DATA_TEGEN);
    }

    public int wacht() {
        return entityData.get(DATA_WACHT);
    }

    public int serverBeen() {
        return entityData.get(DATA_BEEN);
    }

    public double serverS() {
        return entityData.get(DATA_S);
    }

    // --- server: the ride (SleeRit) tells where it is --------------------------------------------------------------------

    public void volg(int fase, int pauze, int been, SleeRijden.Stand st) {
        entityData.set(DATA_FASE, (byte) fase);
        entityData.set(DATA_PAUZE, (byte) pauze);
        entityData.set(DATA_BEEN, (byte) been);
        entityData.set(DATA_S, (float) st.s);
        entityData.set(DATA_LAT, (float) st.lat);
        entityData.set(DATA_V, (float) st.v);
        entityData.set(DATA_LATV, (float) st.latV);
        this.sO = this.s;
        this.latO = this.lat;
        this.beenO = this.been;
        this.s = st.s;
        this.lat = st.lat;
        this.v = st.v;
        this.latV = st.latV;
        this.been = been;
        zetOp();
    }

    public void zetStaat(float storm, float warmte, boolean kist, int tijd, float tegen, int wacht) {
        entityData.set(DATA_STORM, storm);
        entityData.set(DATA_WARMTE, warmte);
        entityData.set(DATA_KIST, kist);
        entityData.set(DATA_TIJD, tijd);
        entityData.set(DATA_TEGEN, tegen);
        entityData.set(DATA_WACHT, wacht);
    }

    /** Server: the sled is put back somewhere else (the rider's game must take this spot). */
    public void nieuweGen() {
        entityData.set(DATA_GEN, gen() + 1);
    }

    private void zetOp() {
        RitRoute r = route();
        if (r == null) {
            return;
        }
        SleeBaan b = r.baan(been);
        Vec3 p = b.op(s, lat);
        setPos(p.x, p.y, p.z);
        Vec3 t = b.richting(s);
        setYRot((float) Math.toDegrees(Math.atan2(-t.x, t.z)));
        setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(t.y, -1, 1))));
    }

    /** The musher's standing spot and the sled's frame, for anything that needs where the sled "is" (world). */
    public Vec3 richting() {
        RitRoute r = route();
        return r == null ? Vec3.directionFromRotation(0, getYRot()) : r.baan(been).richting(s);
    }

    // --- ticking -------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (!SleeRit.tickVan(this) && tickCount > 5) {
                ejectPassengers();
                discard();
            }
            return;
        }
        Player me = lokaal == null ? null : lokaal.get();
        eigen = me != null && getPassengers().contains(me);
        sO = s;
        latO = lat;
        beenO = been;
        RitRoute r = route();
        if (r == null) {
            return;
        }
        if (eigen && fase() == RIJDT) {
            voorspel(r);
        } else {
            neemServer();
        }
        zetOp();
        loopTik++;
        if (clientTick != null) {
            clientTick.accept(this);
        }
    }

    private void neemServer() {
        s = entityData.get(DATA_S);
        lat = entityData.get(DATA_LAT);
        v = entityData.get(DATA_V);
        latV = entityData.get(DATA_LATV);
        been = entityData.get(DATA_BEEN);
        mijnGen = gen();
        mijnBeen = been;
    }

    /** The rider's own ride: straight from the keys, told to the server every tick. */
    private void voorspel(RitRoute r) {
        int gen = gen(), sb = serverBeen();
        if (gen != mijnGen || sb != mijnBeen || Math.abs(serverS() - s) > 14) {
            neemServer();                                  // (a server reset, a new leg, or we got lost: its spot wins)
            sO = s;
            latO = lat;
        }
        SleeRijden.Stand st = new SleeRijden.Stand(s, lat, v, latV);
        SleeRijden.stap(st, invoer == null ? SleeRijden.Invoer.NIKS : invoer.get(), r, been, storm(), warmte(), seed());
        s = st.s;
        lat = st.lat;
        v = st.v;
        latV = st.latV;
        BaltoSleePayloads.stuur(getId(), mijnGen, been, (float) s, (float) lat, (float) v, (float) latV);
    }

    /**
     * The rider's game moves the sled itself: the server's position packets would only make it shake. 26.1: server
     * positions arrive through the interpolation handler (was lerpTo), which ignores them.
     */
    private final net.minecraft.world.entity.InterpolationHandler negeer = new net.minecraft.world.entity.InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
        }
    };

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return negeer;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EV_BEDOLVEN && id <= EV_KEER) {
            laatsteEvent = id;
            if (clientEvent != null) {
                clientEvent.accept(this);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    /** Server: a burst of snow at the sled (buried, a plof), for everybody. */
    public void sneeuwwolk(int n) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), n, 1.2, 0.6, 1.2, 0.08);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), n / 3, 1.0, 0.4, 1.0, 0.02);
        }
    }

    // --- riding ----------------------------------------------------------------------------------------------------------------

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 t = richting();
        Vec3 flat = new Vec3(t.x, 0, t.z);
        flat = flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
        return flat.scale(-ACHTER).add(0, OP_DE_LATTEN - t.y * ACHTER, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(getYRot());
            // (3.0 QA) the musher's view turns along with the sled in the bends (like a boat), so the team stays in front
            float draai = Mth.wrapDegrees(getYRot() - yRotO);
            if (draai != 0 && Math.abs(draai) < 45) {
                passenger.setYRot(passenger.getYRot() + draai);
                living.setYHeadRot(living.getYHeadRot() + draai);
            }
        }
    }

    /** The musher stands on the runners. */
    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player && soort() != STEELE;
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
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 160 * 160;
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
