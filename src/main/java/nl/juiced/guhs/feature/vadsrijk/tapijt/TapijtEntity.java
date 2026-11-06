package nl.juiced.guhs.feature.vadsrijk.tapijt;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;

/**
 * U-TAPIJT: the flying carpet (zone 6, Zijdeland). A mount like every other ({@link RijdierEntity}: called with the mount key,
 * levels from riding, poofs away when you get off), but it moves with its own floaty physics ({@link TapijtFysica}):
 * <ul>
 *   <li>it turns toward where you look, but its momentum keeps carrying it the old way and only slowly follows (drift);</li>
 *   <li>W builds up speed smoothly, letting go glides on for a long while, S brakes, A/D push it sideways, space = a small hop;</li>
 *   <li>it hovers ~1.25 blocks above ground and water (also over sand and lava), climbs steps smoothly (it looks ahead), floats down
 *       from ledges slowly and ducks under low ceilings; it can never fly higher than that.</li>
 * </ul>
 * The riding client moves it (prediction, like every mount: smooth in multiplayer); the server checks the speed and the height
 * ({@link TapijtFysica#teSnel}, {@link TapijtFysica#teHoog}) and puts it back when a client cheats. It counts as a "flying vehicle",
 * so the server never kicks the rider for floating. The looks (banking, nose dip, bob) are worked out on every client from what it
 * sees the carpet do.
 */
public class TapijtEntity extends RijdierEntity {
    /** How far down it looks for ground or water (blocks below the carpet); below that it just glides down. */
    public static final int DIEPTE = 40;
    /** Look ahead along the momentum this many ticks (steps and slopes: it rises before it gets there). */
    public static final double VOORUIT = 6;
    /** The footprint: corners this far from the middle (blocks). */
    public static final double HOEK = 0.75;

    /** The momentum (blocks a tick), kept by whoever moves the carpet (the rider's client). */
    private double vx, vz, vy;
    private int hopWacht;
    private boolean springLos = true;
    /** The last tick's hover numbers (client HUD / dev status). */
    private double laatsteHoogte = Double.NaN, laatsteDoel = 1.25;

    // client looks
    private float roll, rollO, pitch, pitchO;
    private double vorigeSnelheid;

    // server check
    private double checkX = Double.NaN, checkY, checkZ;
    private int straf;

    public TapijtEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public double snelheid() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double hoogte() {
        return laatsteHoogte;
    }

    public double doel() {
        return laatsteDoel;
    }

    /** The top speed (blocks a tick) at its level. */
    public double top() {
        return rijSnelheid() * TapijtBalans.get("topfactor");
    }

    public float roll(float pt) {
        return Mth.lerp(pt, rollO, roll);
    }

    public float pitch(float pt) {
        return Mth.lerp(pt, pitchO, pitch);
    }

    // ---- riding -----------------------------------------------------------------------------------------------------------------

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa, 0, player.zza);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        // (the turning is part of the physics in travel: the carpet follows your view smoothly, its momentum lags behind)
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;   // it hovers on purpose: the server never kicks the rider for "floating too long"
    }

    @Override
    public void travel(Vec3 input) {
        TapijtBalans.Knoppen k = TapijtBalans.nu();
        Player rider = getControllingPassenger() instanceof Player p ? p : null;
        double gas = rider == null ? 0 : input.z, zij = rider == null ? 0 : input.x;
        float kijk = rider == null ? getYRot() : rider.getYRot();
        TapijtFysica.Horizontaal h = TapijtFysica.stap(vx, vz, getYRot(), kijk, gas, zij, top(), k);
        vx = h.vx();
        vz = h.vz();
        setYRot(h.yaw());
        this.yBodyRot = this.yHeadRot = h.yaw();
        // the ground below: the highest of the footprint and a point ahead along the momentum
        double grond = grondVoetafdruk(getX(), getZ());
        double vooruit = grond(getX() + vx * VOORUIT, getZ() + vz * VOORUIT, getY() + 1.0);
        if (!Double.isNaN(vooruit) && (Double.isNaN(grond) || vooruit > grond)) {
            grond = vooruit;
        }
        double hoogte = Double.isNaN(grond) ? Double.POSITIVE_INFINITY : getY() - grond;
        double doel = Double.isNaN(grond) ? k.zweefhoogte() : TapijtFysica.doelHoogte(ruimte(grond, k), k);
        double hop = 0;
        if (hopWacht > 0) {
            hopWacht--;
        }
        boolean springt = rider != null && rider.isJumping();
        if (springt && springLos && hopWacht <= 0 && TapijtFysica.magHoppen(hoogte, doel)) {
            hop = rijSprong() * k.hopKracht();
            hopWacht = 12;
            springLos = false;
            if (level().isClientSide()) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.7f, 1.5f, false);
                for (int i = 0; i < 6; i++) {
                    level().addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 1.6, getY(), getZ() + (random.nextDouble() - 0.5) * 1.6,
                            0, -0.04, 0);
                }
            }
        }
        if (!springt) {
            springLos = true;
        }
        vy = TapijtFysica.verticaal(hoogte, vy, hop, doel, k);
        laatsteHoogte = hoogte;
        laatsteDoel = doel;
        Vec3 wil = new Vec3(vx, vy, vz);
        setDeltaMovement(wil);
        move(MoverType.SELF, wil);
        // bumped into something: that part of the momentum is gone (no bouncing, no sliding through)
        Vec3 echt = getDeltaMovement();
        if (horizontalCollision) {
            if (Math.abs(echt.x - vx) > 1e-4) {
                vx *= 0.2;
            }
            if (Math.abs(echt.z - vz) > 1e-4) {
                vz *= 0.2;
            }
        }
        if (verticalCollision) {
            vy = 0;
        }
        resetFallDistance();
    }

    /** The highest ground/water under the carpet's middle and four corners (NaN = nothing in reach). */
    public double grondVoetafdruk(double x, double z) {
        double y0 = getY() + 1.0, g = Double.NaN;
        double[][] punten = {{0, 0}, {HOEK, HOEK}, {-HOEK, HOEK}, {HOEK, -HOEK}, {-HOEK, -HOEK}};
        for (double[] p : punten) {
            double h = grond(x + p[0], z + p[1], y0);
            if (!Double.isNaN(h) && (Double.isNaN(g) || h > g)) {
                g = h;
            }
        }
        return g;
    }

    /** The top of the first ground or fluid surface at (x, z) from {@code vanY} down ({@link #DIEPTE} blocks), or NaN. */
    public double grond(double x, double z, double vanY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x), bz = Mth.floor(z), y0 = Mth.floor(vanY);
        for (int y = y0; y >= y0 - DIEPTE && y >= level().getMinY(); y--) {
            m.set(bx, y, bz);
            FluidState f = level().getFluidState(m);
            if (!f.isEmpty()) {
                return y + f.getHeight(level(), m);   // water and lava count as ground: it glides over them
            }
            BlockState s = level().getBlockState(m);
            VoxelShape v = s.getCollisionShape(level(), m);
            if (!v.isEmpty()) {
                return y + v.max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }

    /** Free room above the ground in the middle (blocks; +inf when open): under a low ceiling the carpet hovers lower. */
    private double ruimte(double grond, TapijtBalans.Knoppen k) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(getX()), bz = Mth.floor(getZ());
        int van = Mth.floor(grond + 0.5), tot = Mth.floor(grond + k.zweefhoogte() + TapijtFysica.RUITER + 1);
        for (int y = van; y <= tot; y++) {
            m.set(bx, y, bz);
            BlockState s = level().getBlockState(m);
            VoxelShape v = s.getCollisionShape(level(), m);
            if (!v.isEmpty()) {
                return y + v.min(Direction.Axis.Y) - grond;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        this.yBodyRot = getYRot();   // a carpet never turns its "body" toward where it drifts
    }

    @Override
    public float maxUpStep() {
        return 1.0f;   // (the hover already clears 1-block steps; this keeps it from catching on a 2-block edge)
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
        if (isRemoved()) {
            return;
        }
        if (level().isClientSide()) {
            kijkjes();
        } else if (getControllingPassenger() instanceof ServerPlayer p) {
            controle(p);
        } else {
            checkX = Double.NaN;
        }
    }

    /** Client: the banking / nose dip from what the carpet does (also for other players' carpets), and a few sparkles. */
    private void kijkjes() {
        rollO = roll;
        pitchO = pitch;
        double dx = getX() - xo, dz = getZ() - zo;
        double v = Math.sqrt(dx * dx + dz * dz);
        double[] f = TapijtFysica.richting(getYRot());
        double zijDrift = dx * f[1] - dz * f[0];   // the sideways part of the movement (left = +)
        float draai = Mth.wrapDegrees(getYRot() - yRotO);
        float[] t = TapijtFysica.kanteling(roll, pitch, draai, zijDrift, v - vorigeSnelheid, TapijtBalans.nu());
        roll = t[0];
        pitch = t[1];
        vorigeSnelheid = v;
        if (v > 0.18 && tickCount % 3 == 0) {
            double a = random.nextBoolean() ? 1 : -1;
            level().addParticle(ParticleTypes.END_ROD, getX() - f[0] * 1.3 + f[1] * a * 0.8, getY() + 0.1, getZ() - f[1] * 1.3 - f[0] * a * 0.8,
                    0, -0.01, 0);
        }
        if (v > 0.12 && tickCount % 4 == 0 && level().getFluidState(BlockPos.containing(getX(), getY() - 1.0, getZ())).isSource()) {
            level().addParticle(ParticleTypes.SPLASH, getX() + (random.nextDouble() - 0.5) * 2, getY() - laatsteDoel + 0.1,
                    getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
        }
    }

    /** Server: every 10 ticks, check the rider's carpet against the model (speed + height); a cheat is put back. */
    private void controle(ServerPlayer p) {
        if (Double.isNaN(checkX)) {
            checkX = getX();
            checkY = getY();
            checkZ = getZ();
            return;
        }
        if (tickCount % 10 != 0) {
            return;
        }
        TapijtBalans.Knoppen k = TapijtBalans.nu();
        double dx = getX() - checkX, dz = getZ() - checkZ;
        double grond = grondVoetafdruk(getX(), getZ());
        double hoogte = Double.isNaN(grond) ? Double.POSITIVE_INFINITY : getY() - grond;
        boolean snel = TapijtFysica.teSnel(Math.sqrt(dx * dx + dz * dz), 10, top(), k);
        boolean hoog = TapijtFysica.teHoog(hoogte, checkY - getY(), k);
        if (snel || hoog) {
            straf++;
            double ny = hoog ? grond + k.zweefhoogte() : snel ? checkY : getY();
            absSnapTo(snel ? checkX : getX(), ny, snel ? checkZ : getZ(), getYRot(), getXRot());
            p.connection.send(ClientboundMoveVehiclePacket.fromEntity(this));
            if (straf % 5 == 1) {
                com.mojang.logging.LogUtils.getLogger().warn("Vadsrijk tapijt: {} moved {} (snel {}, hoog {}), put back", p.getPlainTextName(),
                        String.format(java.util.Locale.ROOT, "%.2f,%.2f", dx, dz), snel, hoog);
            }
        }
        checkX = getX();
        checkY = getY();
        checkZ = getZ();
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return false;   // (water, walls, cactus, lava below: a carpet doesn't mind; only a real hit scares it, like every mount)
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        // the rider sits cross-legged in the middle, a little toward the back (+z = forward here)
        return new Vec3(0, soort().zit, -0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    // ---- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ZWEEF = RawAnimation.begin().thenLoop("zweef");
    private static final RawAnimation SNEL = RawAnimation.begin().thenLoop("snel");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TapijtEntity>("golf", 8, state -> {
            if (voorbeeld) {
                return state.setAndContinue(STIL);
            }
            double dx = getX() - xo, dz = getZ() - zo;
            double v = Math.sqrt(dx * dx + dz * dz);
            return state.setAndContinue(v > 0.4 ? SNEL : v > 0.05 ? ZWEEF : STIL);
        }));
    }
}
