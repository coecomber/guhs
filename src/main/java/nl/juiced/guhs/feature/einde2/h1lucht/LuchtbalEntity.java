package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
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
 * einde2 phase B, slice h1_lucht: a swirl of air with two jobs.
 * <ul>
 *   <li><b>The ball of the luchtbal match</b> ({@link Luchtbal}). fix round 1 (playtest: "the ball keeps falling and
 *       resetting"): in a match it never falls. It floats at a fixed height just above the pole tops ({@link #zweef}), at
 *       the eye height of a player on a pole, and when it slows down it drifts to the nearest pole and balances above it,
 *       so you can always reach it by hopping to a pole next to it. A player hits it (left click) and it flies where they
 *       look; Aang-guh blows it back; the power luchtbal blows it hard.</li>
 *   <li><b>The luchtscooter</b> of the track ({@link Training}, {@link #scooter}): the player stands on the ball and rides
 *       it. The SERVER drives it (as the Knabbelburgermobiel): it goes where the rider looks, W is faster, S brakes, space
 *       hops. A gate gives a short boost ({@link #boost}).</li>
 * </ul>
 * One player's ball, never saved. Model: geckolib/models/entity/einde2_h1_lucht_luchtbal.geo.json.
 */
public class LuchtbalEntity extends Entity implements GeoEntity {
    private static final RawAnimation DRAAI = RawAnimation.begin().thenLoop("animation.einde2_h1_lucht_luchtbal.draai");
    private static final double ZWAARTE = 0.012, REMMING = 0.975;
    /** In a match: the horizontal drag, how hard the ball is pulled back to its height, and to the pole it settles on. */
    private static final double REMMING_ZWEEF = 0.98, VEER = 0.03, NAAR_PAAL = 0.012;
    /** Below this horizontal speed (blocks per tick) a floating ball looks for a pole to balance on. */
    private static final double RUSTIG = 0.07;
    /** The scooter: cruising, with W, with S (blocks per tick); a boost on top; the hop. */
    static final double KRUIS = 0.5, VOL = 0.72, REM = 0.15, BOOST = 0.4, HUP = 0.55;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    /** In a match: the height the ball floats at (NaN: it falls as a plain ball), and the pole tops it may settle on. */
    private double zweefY = Double.NaN;
    private List<Vec3> palen = List.of();
    /** A luchtscooter (ridden) instead of a ball; held still for this many ticks more (the countdown). */
    private boolean scooter;
    private int vast, boost;
    private double snel;

    public LuchtbalEntity(EntityType<? extends LuchtbalEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    /** A match ball: float at this height (the feet of the ball) and settle above one of these pole tops (block positions). */
    public void zweef(double hoogte, List<Vec3> paalToppen) {
        this.zweefY = hoogte;
        this.palen = List.copyOf(paalToppen);
    }

    /** A luchtscooter: ridden, held still for {@code vastTicks} ticks (the countdown) before it may go. */
    public void scooter(int vastTicks) {
        this.scooter = true;
        this.vast = vastTicks;
        this.snel = 0;
    }

    public boolean isScooter() {
        return scooter;
    }

    /** The countdown is over: it may go. */
    public boolean los() {
        return vast <= 0;
    }

    /** A short burst of speed (a gate of the track). */
    public void boost(int ticks) {
        this.boost = Math.max(boost, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // (26.1: the server's position updates only set a target in the handler; a plain Entity must take the steps
            // itself, as the Knabbelburgermobiel does. Without this the ball stays where it appeared on the client)
            interpolation.interpolate();
            return;
        }
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (scooter) {
            rijTick(server);
            return;
        }
        Vec3 v = getDeltaMovement();
        if (!Double.isNaN(zweefY)) {
            v = zweefBeweging(v);
        } else {
            v = v.add(0, -ZWAARTE, 0).scale(REMMING);
        }
        Vec3 voor = position();
        move(MoverType.SELF, v);
        Vec3 echt = position().subtract(voor);
        // a bounce on every side that stopped it
        double vx = Math.abs(echt.x - v.x) > 1e-4 ? -v.x * 0.7 : v.x;
        double vy = Math.abs(echt.y - v.y) > 1e-4 ? -v.y * 0.6 : v.y;
        double vz = Math.abs(echt.z - v.z) > 1e-4 ? -v.z * 0.7 : v.z;
        setDeltaMovement(vx, vy, vz);
        if (tickCount % 3 == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** A match ball: a soft spring to its height, little drag, and when slow a drift to the nearest pole (it balances there). */
    Vec3 zweefBeweging(Vec3 v) {
        double vy = v.y * 0.88 + (zweefY - getY()) * VEER;
        double vx = v.x * REMMING_ZWEEF, vz = v.z * REMMING_ZWEEF;
        if (Math.hypot(vx, vz) < RUSTIG) {
            Vec3 paal = dichtstePaal();
            if (paal != null) {
                double dx = paal.x - getX(), dz = paal.z - getZ();
                vx = (vx + dx * NAAR_PAAL) * 0.9;
                vz = (vz + dz * NAAR_PAAL) * 0.9;
            }
        }
        return new Vec3(vx, vy, vz);
    }

    /** The middle of the top of the pole nearest to the ball (horizontally), or null. */
    @Nullable
    Vec3 dichtstePaal() {
        Vec3 best = null;
        double d = Double.MAX_VALUE;
        for (Vec3 p : palen) {
            double e = Mth.square(p.x + 0.5 - getX()) + Mth.square(p.z + 0.5 - getZ());
            if (e < d) {
                d = e;
                best = new Vec3(p.x + 0.5, p.y, p.z + 0.5);
            }
        }
        return best;
    }

    // --- the luchtscooter -----------------------------------------------------------------------------------------------

    private void rijTick(ServerLevel server) {
        Vec3 v = getDeltaMovement();
        double vy = onGround() ? -0.08 : Math.max(-1.0, v.y - 0.08);
        if (getFirstPassenger() instanceof ServerPlayer rijder) {
            setYRot(rijder.getYRot());
            Input in = rijder.getLastClientInput();
            double doel;
            if (vast > 0) {
                vast--;
                doel = 0;
            } else {
                doel = in.backward() ? REM : in.forward() ? VOL : KRUIS;
                if (boost > 0) {
                    doel += BOOST * Math.min(1.0, boost / 10.0);
                    boost--;
                }
                if (in.jump() && onGround()) {
                    vy = HUP;
                    server.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.8f, 1.8f);
                }
            }
            snel += (doel - snel) * (doel > snel ? 0.12 : 0.25);
        } else {
            snel *= 0.8;
        }
        Vec3 r = Vec3.directionFromRotation(0, getYRot());
        double vx = Mth.lerp(0.35, v.x, r.x * snel), vz = Mth.lerp(0.35, v.z, r.z * snel);
        setDeltaMovement(vx, vy, vz);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            snel *= 0.6;
        }
        // the swirl: a ring of cloud round the ball, more of it when fast
        int n = 2 + (int) (snel * 6);
        for (int i = 0; i < n; i++) {
            double h = (tickCount * 0.6 + i * Math.PI * 2 / n);
            server.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(h) * 0.55, getY() + 0.25, getZ() + Math.sin(h) * 0.55, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (boost > 0 && tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
        }
        if (snel > 0.2 && tickCount % 16 == 0) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.15f, 1.6f);
        }
    }

    @Override
    public float maxUpStep() {
        return scooter ? 1.0f : 0.0f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return scooter && getPassengers().isEmpty();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;                                         // (you STAND on an air scooter)
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // --- the ball ---------------------------------------------------------------------------------------------------------

    /** A push in this direction (strength in blocks per tick). */
    public void duw(Vec3 richting, double kracht) {
        Vec3 r = richting.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : richting.normalize();
        setDeltaMovement(r.scale(kracht).add(0, 0.12, 0));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!scooter && attacker instanceof ServerPlayer p) {
            if (!Luchtbal.geslagen(p, this)) {
                duw(p.getLookAngle(), 0.85);
            }
        }
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved() && !scooter;
    }

    @Override
    public boolean isPushable() {
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
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(DRAAI)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
