package nl.juiced.guhs.feature.ringh4;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (ring-h4): the elf boat of Guhlórien ({@code guhs:ringh4_elfenbootje}): a slender grey boat with a curled prow in the
 * shape of a guh's head. Model, texture, animation: tools/features/ring_h4_modellen.py; the renderer:
 * client.RingH4Client.
 * <ul>
 *   <li><b>Moored boats</b> come with the template (and are kept there by {@code Bezetting}): the guide boat at the jetty of
 *       the glade ({@link #GIDS}), the boat back at the landing ({@link #TERUG}) and two that are only there to look at
 *       ({@link #DECO}). They never move. A click is "may I get in?" ({@link Vaart#stapIn}).</li>
 *   <li><b>A trip</b> ({@link #RIT}) is a boat of its own that exists for one crossing: it waits a few seconds at the jetty
 *       (a friend can still get in), follows its path down the middle of the Guhduin by itself (no steering, a calm stretch),
 *       puts its passengers ashore and is gone. Meanwhile its moored boat is hidden ({@link #isWeg}). A trip is never saved:
 *       after a restart the moored boat simply lies at its jetty again.</li>
 * </ul>
 * Both sides move a sailing boat along the same path (the path and how far it is are synced), like the Luchtballon, so the
 * trip is smooth. Nobody gets out half-way ({@link RingH4Events}): the sauce is harmless, but a ring bearer in the river is
 * not how the story goes.
 */
public class ElfenbootjeEntity extends Entity implements GeoEntity {
    public static final int GIDS = 0, TERUG = 1, DECO = 2, RIT = 3;
    /** Blocks per tick at full speed (about 2.4 blocks a second: a calm stretch). */
    public static final double SNELHEID = 0.12;
    /** How long a trip waits at the jetty before it leaves (ticks). */
    public static final int INSTAPPEN = 60;
    /** Seats for players. */
    public static final int PLAATSEN = 2;

    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WEG = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_VAART = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BEMAND = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_AFSTAND = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.FLOAT);
    /** The path of a trip: "x,y,z;x,y,z;..." (26.1 has no CompoundTag serializer; the Luchtballon does the same). */
    private static final EntityDataAccessor<String> DATA_PAD = SynchedEntityData.defineId(ElfenbootjeEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private double afstand;
    @Nullable
    private List<Vec3> pad;
    private double[] lengtes = new double[0];
    /** Server, a trip: the moored boat it stands in for, where it puts its passengers, whether it sails back up, what was said. */
    @Nullable
    UUID thuis;
    @Nullable
    Vec3 uitstap;
    boolean terug;
    int fase;
    private int wacht;
    /** Server: true while the passengers are put ashore (only then may they get off a sailing boat). */
    boolean uitstappen;
    /** Server: read from a save (a trip is never saved; one that is found in a save goes at its first tick). */
    private boolean uitOpslag;

    public ElfenbootjeEntity(EntityType<? extends ElfenbootjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SOORT, DECO);
        builder.define(DATA_WEG, false);
        builder.define(DATA_VAART, false);
        builder.define(DATA_BEMAND, false);
        builder.define(DATA_AFSTAND, 0f);
        builder.define(DATA_PAD, "");
    }

    // --- state ------------------------------------------------------------------------------------------------------------

    public int soort() {
        return Mth.clamp(entityData.get(DATA_SOORT), GIDS, RIT);
    }

    public void setSoort(int soort) {
        entityData.set(DATA_SOORT, soort);
    }

    /** A moored boat whose trip is under way: not drawn, not to be clicked. */
    public boolean isWeg() {
        return entityData.get(DATA_WEG);
    }

    public void setWeg(boolean weg) {
        entityData.set(DATA_WEG, weg);
    }

    /** Is this trip sailing (false: moored, or still waiting at the jetty)? */
    public boolean vaart() {
        return entityData.get(DATA_VAART);
    }

    /** Leguhlas at the stern and Gimguh at the prow (drawn by the renderer; the trip down the river). */
    public boolean bemand() {
        return entityData.get(DATA_BEMAND);
    }

    public double afstand() {
        return afstand;
    }

    /** How far the trip is, 0..1. */
    public double fractie() {
        double l = lengte();
        return l <= 0 ? 0 : Mth.clamp(afstand / l, 0, 1);
    }

    @Nullable
    public List<Vec3> pad() {
        if (pad == null) {
            String s = entityData.get(DATA_PAD);
            if (s.isEmpty()) {
                return null;
            }
            List<Vec3> punten = new ArrayList<>();
            try {
                for (String punt : s.split(";")) {
                    String[] c = punt.split(",");
                    punten.add(new Vec3(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2])));
                }
            } catch (RuntimeException e) {
                return null;
            }
            if (punten.size() < 2) {
                return null;
            }
            pad = punten;
            lengtes = new double[punten.size()];
            for (int i = 1; i < punten.size(); i++) {
                lengtes[i] = lengtes[i - 1] + punten.get(i).distanceTo(punten.get(i - 1));
            }
        }
        return pad;
    }

    public double lengte() {
        return pad() == null ? 0 : lengtes[lengtes.length - 1];
    }

    /** The point of the path this far along it. */
    public Vec3 op(double d) {
        List<Vec3> p = pad();
        if (p == null) {
            return position();
        }
        d = Mth.clamp(d, 0, lengte());
        for (int i = 1; i < p.size(); i++) {
            if (d <= lengtes[i] || i == p.size() - 1) {
                double deel = lengtes[i] - lengtes[i - 1];
                return p.get(i - 1).lerp(p.get(i), deel <= 1e-6 ? 0 : (d - lengtes[i - 1]) / deel);
            }
        }
        return p.get(p.size() - 1);
    }

    /** The yaw of the path this far along it (looking two blocks ahead, so the bends are soft). */
    public float richting(double d) {
        Vec3 a = op(Math.max(0, d - 0.5)), b = op(Math.min(lengte(), d + 2.0));
        Vec3 r = b.subtract(a);
        return r.horizontalDistanceSqr() < 1e-6 ? getYRot() : (float) Math.toDegrees(Math.atan2(-r.x, r.z));
    }

    /** Slow away from the jetty, slow up to the other one. */
    private double snelheid() {
        double l = lengte();
        return SNELHEID * Mth.clamp(Math.min(0.2 + afstand / 5.0, 0.15 + (l - afstand) / 6.0), 0.15, 1.0);
    }

    // --- a trip -------------------------------------------------------------------------------------------------------------

    /**
     * Server: this boat becomes a trip along {@code route} (world positions): it waits {@link #INSTAPPEN} ticks where it
     * lies, sails, and puts its passengers at {@code uitstap}.
     */
    public void maakRit(List<Vec3> route, Vec3 uitstap, @Nullable UUID thuis, boolean terug, boolean bemand) {
        StringBuilder s = new StringBuilder();
        for (Vec3 p : route) {
            s.append(s.length() == 0 ? "" : ";").append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f", p.x, p.y, p.z));
        }
        setSoort(RIT);
        pad = null;
        entityData.set(DATA_PAD, s.toString());
        entityData.set(DATA_BEMAND, bemand);
        entityData.set(DATA_AFSTAND, 0f);
        this.afstand = 0;
        this.uitstap = uitstap;
        this.thuis = thuis;
        this.terug = terug;
        this.fase = 0;
        this.wacht = INSTAPPEN;
        Vec3 begin = route.get(0);
        setPos(begin);
        float yaw = richting(0);
        setYRot(yaw);
        yRotO = yaw;
    }

    /** Server: the trip is over (arrived, or broken off): everybody ashore, the moored boat is back, this one is gone. */
    public void eindig(boolean aangekomen) {
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        List<Entity> passagiers = List.copyOf(getPassengers());
        entityData.set(DATA_VAART, false);
        uitstappen = true;
        try {
            ejectPassengers();
        } finally {
            uitstappen = false;
        }
        Vaart.voorbij(this, passagiers, aangekomen);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 12, 0.6, 0.2, 0.6, 0.02);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (soort() != RIT) {
            if (!level().isClientSide() && soort() == GIDS && !isWeg() && tickCount % 40 == 7) {
                Vaart.gidsenTerug(this);       // (self-healing: the guides stand on the quay whenever their boat lies there)
            }
            return;
        }
        if (!level().isClientSide()) {
            if (uitOpslag) {
                discard();
                return;
            }
            if (!vaart()) {
                if (getPassengers().isEmpty()) {
                    eindig(false);             // (everybody got out again before it left)
                } else if (--wacht <= 0) {
                    entityData.set(DATA_VAART, true);
                    level().playSound(null, this, SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 0.9f, 0.8f);
                    Vaart.vertrokken(this);
                }
                return;
            }
        }
        if (!vaart() || pad() == null) {
            return;
        }
        afstand = Math.min(lengte(), afstand + snelheid());
        setPos(op(afstand));
        setYRot(Mth.approachDegrees(getYRot(), richting(afstand), 2.5f));
        if (level().isClientSide()) {
            if (random.nextInt(4) == 0) {
                Vec3 achter = Vec3.directionFromRotation(0, getYRot()).scale(-1.6);
                level().addParticle(ParticleTypes.SPLASH, getX() + achter.x + (random.nextDouble() - 0.5) * 0.8, getY() + 0.15,
                        getZ() + achter.z + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
            }
            return;
        }
        if (tickCount % 20 == 0) {
            entityData.set(DATA_AFSTAND, (float) afstand);
        }
        if (tickCount % 34 == 0) {
            level().playSound(null, this, SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 0.6f, 0.7f + random.nextFloat() * 0.3f);
        }
        fase = Vaart.onderweg(this, fase);
        if (getPassengers().isEmpty() && afstand > 2) {
            eindig(false);
        } else if (afstand >= lengte() - 1e-6) {
            eindig(true);
        }
    }

    /** The client follows the path itself; it only takes the server's distance when it is far off. */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == DATA_PAD) {
            pad = null;
        }
        if (!level().isClientSide()) {
            return;
        }
        if (key == DATA_VAART && vaart()) {
            afstand = entityData.get(DATA_AFSTAND);
        } else if (key == DATA_AFSTAND && vaart()) {
            double server = entityData.get(DATA_AFSTAND);
            if (Math.abs(server - afstand) > 2.5) {
                afstand = server;
            }
        }
    }

    /** While it sails the client ignores the server's positions; otherwise it snaps like a plain entity. */
    private final net.minecraft.world.entity.InterpolationHandler interpolation = new net.minecraft.world.entity.InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (!vaart()) {
                setPos(position);
                setYRot(yRot % 360.0F);
                setXRot(xRot % 360.0F);
            }
        }
    };

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // --- riding ---------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            Vaart.stapIn(p, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown() && soort() != RIT) {
            discard();                         // (creative: a sneak-hit removes a moored boat)
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved() && !isWeg();
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
    public boolean isInvisible() {
        return isWeg() || super.isInvisible();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return soort() == RIT && getPassengers().size() < PLAATSEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        return new Vec3(0, 0.22, index == 0 ? 0.25 : -0.55).yRot(-getYRot() * Mth.DEG_TO_RAD);
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

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    // --- saving ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.putInt("Soort", soort());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        int soort = tag.getIntOr("Soort", DECO);
        setSoort(soort);
        uitOpslag = soort == RIT;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation DOBBER = RawAnimation.begin().thenLoop("animation.ringh4_elfenbootje.dobber");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("dobber", 0, state -> state.setAndContinue(DOBBER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
