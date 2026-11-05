package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;

/**
 * T-Z4BOOT: the Zeilbootje, the player's own guh sailboat (zone 4). It is a mount ({@link RijdierEntity}: called with the mount
 * key, levels from sailing, poofs away when you get off / leave / log out), but it moves on WATER with its own physics
 * ({@link BootFysica}): W = sail up (builds up speed), S = reef / back, A/D = turn the bow (your view turns along), no key = it
 * glides on. It floats on the water surface and bobs a little; on land it hardly moves (it scrapes). In De Kringstroom the current
 * pushes it back in. Shift next to a dock = moor: you step onto the dock's planks (that dock is remembered: logging out at sea
 * brings you back there). Looks: sail colour, pennant and figurehead ({@link BootVariant}), synced.
 */
public class BootEntity extends RijdierEntity {
    /** How deep the hull lies: the water surface is this far above the boat's feet. */
    public static final double DIEPGANG = 0.40;
    private static final EntityDataAccessor<Integer> DATA_ZEIL = SynchedEntityData.defineId(BootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WIMPEL = SynchedEntityData.defineId(BootEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOEG = SynchedEntityData.defineId(BootEntity.class, EntityDataSerializers.INT);

    /** The forward speed (blocks a tick), kept by whoever moves the boat (the rider's client). */
    private double vaart;
    /** The last tick's result (client: HUD + particles). */
    private double laatsteDiepte;
    private boolean opWater = true;

    public BootEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_ZEIL, 0);
        b.define(DATA_WIMPEL, 0);
        b.define(DATA_BOEG, 0);
    }

    public BootVariant variant(BootVariant.As as) {
        return BootVariant.van(as, entityData.get(switch (as) {
            case ZEIL -> DATA_ZEIL;
            case WIMPEL -> DATA_WIMPEL;
            case BOEG -> DATA_BOEG;
        }));
    }

    public void zetVariant(BootVariant v) {
        entityData.set(switch (v.as) {
            case ZEIL -> DATA_ZEIL;
            case WIMPEL -> DATA_WIMPEL;
            case BOEG -> DATA_BOEG;
        }, v.index());
    }

    public double vaart() {
        return vaart;
    }

    /** Depth in De Kringstroom's band last tick (0 = free water). */
    public double stroomDiepte() {
        return laatsteDiepte;
    }

    public boolean opWater() {
        return opWater;
    }

    /** The top speed (blocks a tick, before wind) at its level. */
    public double top() {
        return rijSnelheid() * BootFysica.TOP_FACTOR;
    }

    // ---- sailing ----------------------------------------------------------------------------------------------------------------

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa, 0, player.zza);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        // (the turning happens in travel: A/D turn the bow, not where you look)
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;   // (it bobs on the water: never "floating too long" for the server)
    }

    /** The water surface under the boat (y), or NaN when there is no water close below. */
    public double oppervlak() {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(getX()), bz = Mth.floor(getZ()), y0 = Mth.floor(getY());
        for (int y = y0 + 1; y >= y0 - 2; y--) {
            m.set(bx, y, bz);
            FluidState f = level().getFluidState(m);
            if (f.is(FluidTags.WATER)) {
                return y + f.getHeight(level(), m);
            }
        }
        return Double.NaN;
    }

    @Override
    public void travel(Vec3 input) {
        Player rider = getControllingPassenger() instanceof Player p ? p : null;
        double gas = rider == null ? 0 : input.z, stuur = rider == null ? 0 : input.x;
        double opp = oppervlak();
        opWater = !Double.isNaN(opp);
        long tijd = level().getGameTime();
        BootFysica.Stap s = BootFysica.stap(getX(), getZ(), getYRot(), vaart, gas, stuur, top(), tijd,
                BootZee.kring().r(getX(), getZ()) < BootZee.STROOM_TOT ? BootZee.kring() : null);
        laatsteDiepte = s.diepte();
        vaart = opWater ? s.vaart() : s.vaart() * 0.5;
        if (s.draai() != 0) {
            float yaw = getYRot() + s.draai();
            setYRot(yaw);
            if (rider != null && level().isClientSide()) {
                rider.setYRot(rider.getYRot() + s.draai());   // your view turns along with the boat
                rider.setYHeadRot(rider.getYHeadRot() + s.draai());
            }
        }
        this.yBodyRot = this.yHeadRot = getYRot();
        double vy;
        if (opWater) {
            double doel = opp - DIEPGANG + 0.035 * Math.sin(tickCount * 0.13);   // a gentle bob
            vy = Mth.clamp((doel - getY()) * 0.3, -0.25, 0.25);
        } else {
            vy = Math.max(-1.5, getDeltaMovement().y * 0.98 - 0.06);   // (on land / falling)
        }
        double f = opWater ? 1 : 0.2;   // on sand it scrapes: barely moves
        Vec3 beweeg = new Vec3(s.dx() * f, vy, s.dz() * f);
        setDeltaMovement(beweeg);
        move(MoverType.SELF, beweeg);
        if (horizontalCollision) {
            vaart *= 0.5;   // bonk
        }
        resetFallDistance();
        if (level().isClientSide()) {
            spetters();
        }
    }

    /** Client: spray at the bow and a wake when sailing fast, foam in the current. */
    private void spetters() {
        double v = Math.abs(vaart);
        if (!opWater) {
            return;
        }
        double rad = getYRot() * Mth.DEG_TO_RAD;
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double opp = getY() + DIEPGANG;
        if (v > 0.15 && random.nextFloat() < v * 1.6) {
            level().addParticle(ParticleTypes.SPLASH, getX() + fx * 2.4 + (random.nextDouble() - 0.5), opp + 0.1, getZ() + fz * 2.4 + (random.nextDouble() - 0.5),
                    0, 0.1, 0);
        }
        if (v > 0.1 && random.nextFloat() < v) {
            level().addParticle(ParticleTypes.BUBBLE_POP, getX() - fx * 2.4 + (random.nextDouble() - 0.5) * 1.5, opp + 0.05,
                    getZ() - fz * 2.4 + (random.nextDouble() - 0.5) * 1.5, 0, 0, 0);
        }
        if (laatsteDiepte > 0 && random.nextFloat() < 0.5) {
            double hoek = random.nextDouble() * Math.PI * 2;
            level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(hoek) * 3, opp + 0.1, getZ() + Math.sin(hoek) * 3, 0, 0.01, 0);
        }
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        this.yBodyRot = getYRot();   // a boat never turns its "body" toward where it drifts
    }

    @Override
    public float maxUpStep() {
        return 0f;   // no climbing onto the beach
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // ---- life -------------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && !isRemoved() && getFirstPassenger() instanceof ServerPlayer p && tickCount % 10 == 0) {
            Boot.opZeeTik(p, this);
        }
        if (level().isClientSide() && opWater && Math.abs(vaart) > 0.2 && tickCount % 24 == 0) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 0.5f, 0.9f + random.nextFloat() * 0.2f, false);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return false;   // (water, walls, cramming: a boat doesn't mind; only a real hit scares it, like every mount)
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        // the rider sits on the little bench at the stern, by the tiller
        return new Vec3(0, soort().zit, -1.35).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** Off the boat: next to a dock you step onto its planks (and it is remembered); elsewhere the normal way (into the water). */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BootZee.Steiger s = BootZee.dichtbij(getX(), getZ(), BootZee.AANLEG_AFSTAND);
        if (s != null) {
            if (passenger instanceof ServerPlayer sp) {
                Boot.aangemeerd(sp, s);
            }
            return Vec3.atBottomCenterOf(s.plek());
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ---- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation VAREN = RawAnimation.begin().thenLoop("varen");
    private static final RawAnimation HARD = RawAnimation.begin().thenLoop("hard");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BootEntity>("vaar", 6, state -> {
            if (voorbeeld) {
                return state.setAndContinue(STIL);
            }
            double dx = getX() - xo, dz = getZ() - zo;
            double v = Math.sqrt(dx * dx + dz * dz);
            boolean mee = BootFysica.windMee(getYRot(), level().getGameTime()) > 0.5;
            return state.setAndContinue(v > 0.35 && mee ? HARD : v > 0.04 ? VAREN : STIL);
        }));
    }
}
