package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * T-LUCHTSCHIP: one big guh airship of a scheduled line (model {@code vr_luchtschip}: a wooden deck with a cabin and railing
 * under a pink guh-faced balloon with ears, two propellers; drawn at {@link #SCHAAL}).
 * <p>
 * It is a timetable puppet: server AND client compute where it is from the level's game time ({@link Dienstregeling#stand}), so
 * the flight is smooth without position packets; the client runs its own soft clock that follows the server's game time.
 * Passengers stand on the deck in {@link #PLEKKEN} places and can look around; getting off is only possible while it lies at
 * a pier (sneak), never in flight ({@link Luchtschepen}). Never saved: {@link Luchtschepen} spawns it when someone is near
 * and removes it when nobody is (after a restart it is simply there again, at the right place in the timetable).
 */
public class LuchtschipEntity extends Entity implements GeoEntity {
    /** How much bigger than the model it is drawn. */
    public static final float SCHAAL = 4f;
    /** Places on the deck (ship frame: +x left/right, +z forward, y = deck), 16 passengers. */
    static final Vec3[] PLEKKEN = new Vec3[16];

    static {
        double[] xs = {-2.1, -0.7, 0.7, 2.1};
        double[] zs = {1.6, 0.0, -1.6, -3.2};
        int i = 0;
        for (double z : zs) {
            for (double x : xs) {
                PLEKKEN[i++] = new Vec3(x, 0, z);
            }
        }
    }

    private static final EntityDataAccessor<String> DATA_LIJN = SynchedEntityData.defineId(LuchtschipEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SCHIP = SynchedEntityData.defineId(LuchtschipEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation VAAR = RawAnimation.begin().thenLoop("vaar");
    private static final RawAnimation LIGT = RawAnimation.begin().thenLoop("ligt");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: own clock (follows the level's game time softly). */
    private double klok = Double.NaN;
    @Nullable
    private Dienstregeling.Fase laatsteFase;
    /** Server: only while true may passengers get off (landing / at the pier). */
    boolean uitstappen;

    public LuchtschipEntity(EntityType<? extends LuchtschipEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        addTag(TAG_VRIJ);
    }

    /** t-wereld's barrier push-back (Barrieres.TAG_VRIJ) leaves this vehicle and its passengers alone. */
    public static final String TAG_VRIJ = "guhs_luchtschip";

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIJN, "");
        builder.define(DATA_SCHIP, 0);
    }

    void zet(String lijn, int schip) {
        entityData.set(DATA_LIJN, lijn);
        entityData.set(DATA_SCHIP, schip);
    }

    @Nullable
    public Lijnen.Lijn lijn() {
        return Lijnen.vanId(entityData.get(DATA_LIJN));
    }

    public int schip() {
        return entityData.get(DATA_SCHIP);
    }

    /** The timetable time of this ship (server: game time; client: the soft clock). */
    double tijd(float partial) {
        Lijnen.Lijn l = lijn();
        double basis = level().isClientSide() ? (Double.isNaN(klok) ? level().getGameTime() : klok)
                : l != null ? Tijd.basis((ServerLevel) level(), l) : level().getGameTime();
        return basis + partial + Tijd.verschuiving(l);
    }

    @Nullable
    public Dienstregeling.Stand stand() {
        Lijnen.Lijn l = lijn();
        return l == null ? null : Dienstregeling.stand(l, schip(), tijd(0));
    }

    public boolean vliegt() {
        Dienstregeling.Stand s = stand();
        return s != null && s.fase().vliegt();
    }

    // --- every tick -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            double server = level().getGameTime();
            if (Double.isNaN(klok) || Math.abs(server - klok) > 40) {
                klok = server;
            } else {
                klok += 1 + (server - klok) * 0.05;
            }
        }
        volg();
        if (level().isClientSide()) {
            effecten();
        }
    }

    /** Put the ship where the timetable says (both sides); the server reports phase changes to {@link Luchtschepen}. */
    void volg() {
        Lijnen.Lijn l = lijn();
        if (l == null) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        Dienstregeling.Stand s = Dienstregeling.stand(l, schip(), tijd(0));
        setPos(s.pos());
        if (Math.abs(Mth.wrapDegrees(s.yaw() - getYRot())) > 90 && tickCount < 3) {
            yRotO = s.yaw();
        }
        setYRot(s.yaw());
        if (!level().isClientSide() && laatsteFase != s.fase()) {
            Dienstregeling.Fase oud = laatsteFase;
            laatsteFase = s.fase();
            if (oud != null) {
                Luchtschepen.faseWissel(this, l, oud, s.fase());
            }
        }
    }

    /** (client) smoke from the propellers in flight, a puff of steam now and then at the pier. */
    private void effecten() {
        boolean vliegt = vliegt();
        if (tickCount % (vliegt ? 3 : 14) != 0) {
            return;
        }
        float r = -getYRot() * Mth.DEG_TO_RAD;
        for (int kant = -1; kant <= 1; kant += 2) {
            Vec3 p = position().add(new Vec3(kant * 4.4, 1.6, -7.2).yRot(r));
            level().addParticle(vliegt ? ParticleTypes.CLOUD : ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z, 0, vliegt ? 0.01 : 0.04, 0);
        }
    }

    /** The client ignores the server's position packets: it flies the timetable itself. */
    private final InterpolationHandler glijden = new InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (lijn() == null) {
                setPos(position);
                setYRot(yRot);
            }
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return glijden;
    }

    // --- passengers -----------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            Luchtschepen.instappen(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < PLEKKEN.length;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = Math.max(0, getPassengers().indexOf(passenger)) % PLEKKEN.length;
        return PLEKKEN[i].add(0, 0.6, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            // the passengers turn with the ship (small turns only, so a fresh look round is not disturbed)
            float turn = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(turn) < 30) {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        }
        passenger.resetFallDistance();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        passenger.removeTag(TAG_VRIJ);
        if (!level().isClientSide()) {
            Luchtschepen.uitgestapt(this, passenger);
        }
    }

    // --- nothing touches it ---------------------------------------------------------------------------------------------------

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

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    /** (renderer) the balloon is far above and in front of the deck. */
    public AABB cullBox() {
        return getBoundingBox().inflate(10, 2, 10).expandTowards(0, 16, 0).expandTowards(0, -4, 0);
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
        controllers.add(new AnimationController<LuchtschipEntity>("vaar", 20, state -> state.setAndContinue(vliegt() ? VAAR : LIGT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
