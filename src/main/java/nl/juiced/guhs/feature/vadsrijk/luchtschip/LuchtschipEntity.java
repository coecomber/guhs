package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * U-LUCHTSCHIP: the walkable deck in the ship frame (blocks; a player's middle stays this far inside the railing, in front of
     * the stern cabin and behind the bow railing).
     */
    public static final double DEK_X = 2.95, DEK_ACHTER = -4.2, DEK_VOOR = 8.6;
    /** U-LUCHTSCHIP: walking speed on the deck (blocks per tick; sprinting a bit faster), the same as on the ground. */
    public static final double DEK_LOOP = 0.2, DEK_REN = 0.28;

    /**
     * U-LUCHTSCHIP: the click boxes (ship frame z of the middle, y of the bottom, width, height), following the model at scale 4:
     * four along the hull (incl. the railing), the bow, the cabin, three along the balloon, its nose with the face, the tail fins
     * and the ears.
     */
    static final double[][] DELEN = {
            {-6.5, -3.25, 6.5, 4.25}, {-2.2, -3.25, 6.5, 4.25}, {2.2, -3.25, 6.5, 4.25}, {6.5, -3.25, 6.5, 4.25},
            {10.5, -2.5, 4, 3},
            {-6.5, 0, 5.5, 4.25},
            {-6, 8.5, 9, 9}, {0, 8.5, 9, 9}, {6, 8.5, 9, 9},
            {11.5, 10, 6, 6.5},
            {-12.5, 11, 5, 5},
            {6.75, 17.5, 7, 2.5},
    };

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
    /** U-LUCHTSCHIP: the click boxes ({@link #DELEN}). */
    private final SchipDeel[] delen;
    /** U-LUCHTSCHIP: where each passenger stands on the deck (entity id -&gt; ship frame x, 0, z); both sides. */
    final Map<Integer, Vec3> dek = new HashMap<>();
    /** (client) the synced places of the other passengers, walked to softly; (server) unused. */
    final Map<Integer, Vec3> dekDoel = new HashMap<>();
    /** (client) last tick's places, for the walk animation. */
    private final Map<Integer, Vec3> dekVorig = new HashMap<>();
    /** (server) a place changed: {@link Luchtschepen} sends the deck to the clients. */
    boolean dekVuil;

    public LuchtschipEntity(EntityType<? extends LuchtschipEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        addTag(TAG_VRIJ);
        delen = new SchipDeel[DELEN.length];
        for (int i = 0; i < DELEN.length; i++) {
            double[] d = DELEN[i];
            delen[i] = new SchipDeel(this, new Vec3(0, d[1], d[0]), (float) d[2], (float) d[3]);
        }
        setId(ENTITY_COUNTER.getAndAdd(delen.length + 1) + 1);   // (the parts' ids follow the ship's, as the dragon's do)
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (delen != null) {
            for (int i = 0; i < delen.length; i++) {
                delen[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return delen;
    }

    /** (tests/dev) the click boxes. */
    public SchipDeel[] delen() {
        return delen;
    }

    /** Move the click boxes with the ship (both sides). */
    private void zetDelen() {
        float r = -getYRot() * Mth.DEG_TO_RAD;
        for (SchipDeel d : delen) {
            Vec3 p = position().add(d.plek.yRot(r));
            d.setOldPosAndRot();
            d.setPos(p);
        }
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
            dekVolgen();
        }
    }

    // --- U-LUCHTSCHIP: the walkable deck ----------------------------------------------------------------------------------------

    /** Keep a place on the deck (ship frame) inside the railing and in front of the cabin. */
    public static Vec3 klem(double x, double z) {
        return new Vec3(Mth.clamp(x, -DEK_X, DEK_X), 0, Mth.clamp(z, DEK_ACHTER, DEK_VOOR));
    }

    /** Where this passenger stands on the deck (ship frame): its walked place, else its fixed starting place. */
    public Vec3 dekPlek(Entity passenger) {
        Vec3 v = dek.get(passenger.getId());
        if (v != null) {
            return v;
        }
        return PLEKKEN[Math.max(0, getPassengers().indexOf(passenger)) % PLEKKEN.length];
    }

    /** (server) a free starting place for a new passenger: the first of {@link #PLEKKEN} with nobody within a block. */
    Vec3 vrijePlek(Entity nieuw) {
        for (Vec3 v : PLEKKEN) {
            boolean vrij = true;
            for (Entity q : getPassengers()) {
                if (q != nieuw && dekPlek(q).distanceToSqr(v) < 1) {
                    vrij = false;
                    break;
                }
            }
            if (vrij) {
                return v;
            }
        }
        return PLEKKEN[getPassengers().size() % PLEKKEN.length];
    }

    /** (server) put a passenger on a deck place (clamped) and let the clients know. */
    void zetDek(Entity passenger, Vec3 plek) {
        dek.put(passenger.getId(), klem(plek.x, plek.z));
        dekVuil = true;
    }

    /** (client) the synced places of the other passengers: walk there softly; and the walk animation of everybody on board. */
    private void dekVolgen() {
        int eigen = LuchtschipDek.eigenId();
        for (Map.Entry<Integer, Vec3> d : dekDoel.entrySet()) {
            if (d.getKey() == eigen) {
                continue;
            }
            Vec3 nu = dek.get(d.getKey());
            Vec3 doel = d.getValue();
            if (nu == null || nu.distanceToSqr(doel) > 36) {
                dek.put(d.getKey(), doel);
                continue;
            }
            double afstand = nu.distanceTo(doel);
            double stap = Math.min(afstand, Math.max(0.3, afstand * 0.5));
            dek.put(d.getKey(), afstand < 1e-4 ? doel : nu.add(doel.subtract(nu).scale(stap / afstand)));
        }
        for (Entity q : getPassengers()) {
            Vec3 nu = dekPlek(q);
            Vec3 oud = dekVorig.put(q.getId(), nu);
            LuchtschipDek.loopAnimatie(q, oud == null ? 0 : (float) nu.distanceTo(oud));
        }
        dekVorig.keySet().removeIf(id -> getPassengers().stream().noneMatch(q -> q.getId() == id));
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
        zetDelen();
        if (!level().isClientSide() && laatsteFase != s.fase()) {
            Dienstregeling.Fase oud = laatsteFase;
            laatsteFase = s.fase();
            if (oud != null) {
                Luchtschepen.faseWissel(this, l, oud, s.fase());
            }
        }
    }

    /**
     * (server) The level does not tick it (its chunk is outside every simulation distance, e.g. it flew past the edge): keep it
     * on time and carry the passengers along anyway.
     */
    void volgExtern() {
        setOldPosAndRot();
        volg();
        for (Entity q : getPassengers()) {
            positionRider(q, Entity::setPos);
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
        if (player.getVehicle() == this) {
            return InteractionResult.PASS;   // (U-LUCHTSCHIP: on board a click on the deck is just a click: eating, items)
        }
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
        return dekPlek(passenger).add(0, 0.6, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
        if (level().isClientSide()) {
            LuchtschipDek.zetLoop(passenger);   // U-LUCHTSCHIP: legs swing while walking on the deck
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        passenger.removeTag(TAG_VRIJ);
        dek.remove(passenger.getId());
        dekDoel.remove(passenger.getId());
        dekVorig.remove(passenger.getId());
        dekVuil = true;
        if (!level().isClientSide()) {
            Luchtschepen.uitgestapt(this, passenger);
        }
    }

    // --- nothing touches it ---------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    /** U-LUCHTSCHIP: the click boxes ({@link SchipDeel}) take the clicks; the ship's own small box would only be in the way. */
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
