package nl.juiced.guhs.feature.golf;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModBlocks;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The guh golf ball: a little pink guh rolled up into a ball. Simple physics, all on the server: it rolls with
 * friction, bounces off walls (and extra hard off pink slime bumpers), steps up slabs and stairs (losing speed) and rolls
 * down them by itself, gets launched by a slime block it rolls onto, and drops into a cup when it's slow enough (a bit
 * faster and it lips out, much faster and it just rolls over). It tells its {@link GolfGame} when it stops, drops in,
 * lands in the kaassaus or leaves the course. A ball without a game (tests) just rolls and remembers its last event.
 */
public class GolfBallEntity extends Entity {
    public static final float SIZE = 0.3f;
    public static final double GRAVITY = 0.06, FRICTION = 0.955, AIR = 0.99, SLOPE = 0.02, STOP = 0.012, MAX_SPEED = 1.6;
    public static final double WALL_BOUNCE = 0.62, BUMPER_BOUNCE = 1.08, STEP_LOSS = 0.8;
    /** A slime block under a ball rolling at least LAUNCH_MIN throws it up (slower, it just lies on it). */
    public static final double LAUNCH = 0.5, LAUNCH_MIN = 0.12;
    /** Into the cup: centre within CAPTURE_RADIUS of the middle, at most CAPTURE_SPEED; up to LIP_SPEED it may lip out. */
    public static final double CAPTURE_RADIUS = 0.36, CAPTURE_SPEED = 0.42, LIP_SPEED = 0.62;
    /** A ball that's still rolling after this long just stops (stuck against something on a slope). */
    public static final int MAX_ROLL_TICKS = 20 * 25;

    public enum BallEvent { STOPPED, HOLED, SAUS, OUT, LIP }

    /** The golfer (1.1.0: 26.1 has no OPTIONAL_UUID serializer; an entity reference holds the same UUID). */
    private static final EntityDataAccessor<Optional<net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.LivingEntity>>> OWNER =
            SynchedEntityData.defineId(GolfBallEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

    /** The Golfguh whose game this ball belongs to (null: a free ball, only in tests). */
    @Nullable
    private UUID npc;
    private boolean moving, sunk, frozen;
    private int movingTicks, lipCooldown;
    private double startY;
    @Nullable
    private BallEvent lastEvent;

    // client: smooth movement and rolling
    /** Client: smooth movement between the server's positions (1.0.0: its own lerpTo, 1/steps per tick). */
    private final net.minecraft.world.entity.InterpolationHandler interpolation = new net.minecraft.world.entity.InterpolationHandler(this);
    public float roll, oRoll, rollYaw;

    public GolfBallEntity(EntityType<? extends GolfBallEntity> type, Level level) {
        super(type, level);
    }

    public static GolfBallEntity create(ServerLevel level, Vec3 pos, @Nullable UUID owner, @Nullable UUID npc) {
        GolfBallEntity ball = new GolfBallEntity(GolfFeature.BALL.get(), level);
        ball.snapTo(pos.x, pos.y, pos.z, 0, 0);
        ball.entityData.set(OWNER, Optional.ofNullable(owner).map(net.minecraft.world.entity.EntityReference::of));
        ball.npc = npc;
        ball.startY = pos.y;
        level.addFreshEntity(ball);
        return ball;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
    }

    @Nullable
    public UUID getOwner() {
        return entityData.get(OWNER).map(net.minecraft.world.entity.EntityReference::getUUID).orElse(null);
    }

    @Nullable
    public UUID getNpc() {
        return npc;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isSunk() {
        return sunk;
    }

    @Nullable
    public BallEvent lastEvent() {
        return lastEvent;
    }

    /** A swing of the club. */
    public void hit(Vec3 velocity) {
        sunk = frozen = false;
        noPhysics = false;
        moving = true;
        movingTicks = 0;
        setDeltaMovement(velocity);
        needsSync = true;
    }

    /** Back on this spot, lying still (a new hole, or after the kaassaus / out of bounds). */
    public void resetTo(Vec3 pos) {
        sunk = frozen = moving = false;
        noPhysics = false;
        setDeltaMovement(Vec3.ZERO);
        startY = pos.y;
        teleportTo(pos.x, pos.y, pos.z);
    }

    /** Stays where it is (in the kaassaus...) until it's put back. */
    public void freeze() {
        frozen = true;
        moving = false;
        setDeltaMovement(Vec3.ZERO);
    }

    private void sink(BlockPos cup) {
        sunk = true;
        moving = false;
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(cup.getX() + 0.5, cup.getY() + 0.45, cup.getZ() + 0.5);
        level().playSound(null, cup, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.2f);
        level().playSound(null, cup, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    private void event(BallEvent e) {
        lastEvent = e;
        GolfGame.ballEvent(this, e);
    }

    // --- physics (server) ---------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientTick();
            return;
        }
        if (tickCount % 20 == 0 && !GolfGame.ballAlive(this)) {
            discard();
            return;
        }
        if (sunk || frozen) {
            return;
        }
        if (!moving) {
            move(MoverType.SELF, new Vec3(0, -0.04, 0));          // still lying on something?
            if (onGround()) {
                if (tickCount % 12 == 0 && level() instanceof ServerLevel server) {   // a little sparkle so you can find it
                    server.sendParticles(new DustParticleOptions(0xFF99D9 /* 1, 0.6, 0.85 */, 0.8f), getX(), getY() + 0.45, getZ(), 2, 0.08, 0.05, 0.08, 0);
                }
                return;
            }
            moving = true;
            movingTicks = 0;
        }
        physics();
    }

    private void physics() {
        movingTicks++;
        if (lipCooldown > 0) {
            lipCooldown--;
        }
        Vec3 from = position();
        Vec3 v = getDeltaMovement();
        BlockState under = level().getBlockState(below());
        boolean slope = false;
        if (onGround() && under.getBlock() instanceof StairBlock && under.getValue(StairBlock.HALF) == Half.BOTTOM) {
            Direction up = under.getValue(StairBlock.FACING);           // stairs go up towards their facing: roll the other way
            v = v.add(-up.getStepX() * SLOPE, 0, -up.getStepZ() * SLOPE);
            slope = true;
        }
        v = new Vec3(v.x, v.y - GRAVITY, v.z);
        // (2.10: no more wind on lastig; the far tees and the extra bumpers make it lastig now)
        double speed = v.horizontalDistance();
        if (speed > MAX_SPEED) {
            v = new Vec3(v.x * MAX_SPEED / speed, v.y, v.z * MAX_SPEED / speed);
            speed = MAX_SPEED;
        }
        move(MoverType.SELF, v);
        if (isRemoved()) {
            return;
        }
        Vec3 moved = position().subtract(from);
        double vx = v.x, vy = v.y, vz = v.z;
        // walls: bounce back (the pink slime bumpers give it an extra kick)
        if (horizontalCollision && Math.abs(moved.x - v.x) > 1e-5) {
            vx = -v.x * bounce(BlockPos.containing(getX() + Math.signum(v.x) * (SIZE / 2 + 0.1), getY() + 0.1, getZ()), Math.abs(v.x));
        }
        if (horizontalCollision && Math.abs(moved.z - v.z) > 1e-5) {
            vz = -v.z * bounce(BlockPos.containing(getX(), getY() + 0.1, getZ() + Math.signum(v.z) * (SIZE / 2 + 0.1)), Math.abs(v.z));
        }
        if (verticalCollisionBelow) {
            vy = v.y < -0.35 ? -v.y * 0.3 : 0;
        } else if (verticalCollision) {
            vy = 0;
        }
        boolean stepped = moved.y > 0.05 && v.y < 0.05;
        if (stepped) {
            vx *= STEP_LOSS;
            vz *= STEP_LOSS;
        }
        boolean grounded = onGround();
        BlockState ground = level().getBlockState(below());
        if (grounded && ground.is(ModBlocks.ROZE_SLIJMBLOK.get()) && Math.hypot(vx, vz) >= LAUNCH_MIN) {   // the launch pad of the Vahoegschans
            vy = LAUNCH;
            vx *= 1.1;
            vz *= 1.1;
            grounded = false;
            setOnGround(false);
            level().playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1f, 1.3f);
        }
        if (grounded) {
            vx *= FRICTION;
            vz *= FRICTION;
        } else {
            vx *= AIR;
            vz *= AIR;
        }
        setDeltaMovement(vx, vy, vz);

        if (checkCup(from, position(), speed)) {
            return;
        }
        // hazards: kaassaus (or any other liquid), off the course, fallen off the world. (Lying on the edge of the ditch
        // with its middle over the kaassaus counts as in it: it would tip in.)
        BlockPos underPos = below();
        boolean overNothing = ground.getCollisionShape(level(), underPos).isEmpty();
        if (!level().getFluidState(blockPosition()).isEmpty() || (grounded && overNothing && !level().getFluidState(underPos).isEmpty())) {
            level().playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 1.4f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.1, 0.3, 0.1);
            }
            freeze();
            event(BallEvent.SAUS);
            return;
        }
        if ((grounded && !overNothing && !isCourse(ground)) || getY() < startY - 8) {
            freeze();
            event(BallEvent.OUT);
            return;
        }
        double left = Math.hypot(vx, vz);
        if (grounded && overNothing && left < 0.03) {                    // balancing on an edge: over it goes
            Vec3 push = new Vec3(underPos.getX() + 0.5 - getX(), 0, underPos.getZ() + 0.5 - getZ());
            push = push.lengthSqr() < 1e-6 ? new Vec3(vx, 0, vz) : push;
            if (push.lengthSqr() > 1e-8) {
                push = push.normalize().scale(0.04);
                setDeltaMovement(push.x, vy, push.z);
                return;
            }
        }
        if ((grounded && !slope && left < STOP && Math.abs(vy) < 0.05) || (slope && horizontalCollision && left < 0.03)
                || movingTicks > MAX_ROLL_TICKS) {
            setDeltaMovement(Vec3.ZERO);
            moving = false;
            event(BallEvent.STOPPED);
        }
    }

    private BlockPos below() {
        return BlockPos.containing(getX(), getY() - 0.2, getZ());
    }

    /** How much speed a wall gives back; with a sound for a proper hit. */
    private double bounce(BlockPos wall, double speed) {
        boolean bumper = level().getBlockState(wall).is(ModBlocks.ROZE_SLIJMBLOK.get());
        if (speed > 0.04) {
            if (bumper) {
                level().playSound(null, wall, SoundEvents.SLIME_BLOCK_HIT, SoundSource.PLAYERS, 1f, 1.4f);
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.2, getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                }
            } else {
                level().playSound(null, wall, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, (float) Math.min(1, speed * 2), 1.7f);
            }
        }
        return bumper ? BUMPER_BOUNCE : WALL_BOUNCE;
    }

    /** Rolling over a cup: in it goes when slow enough, a bit faster it may lip out, much faster it rolls over. */
    private boolean checkCup(Vec3 from, Vec3 to, double speed) {
        for (Vec3 p : new Vec3[]{to, from.add(to).scale(0.5), from}) {
            BlockPos cup = BlockPos.containing(p.x, p.y - 0.2, p.z);
            if (!level().getBlockState(cup).is(GolfFeature.HOLE.get()) || Math.abs(to.y - (cup.getY() + 1)) > 0.3) {
                continue;
            }
            double dist = distanceToSegment(cup.getX() + 0.5, cup.getZ() + 0.5, from, to);
            if (dist > CAPTURE_RADIUS) {
                continue;
            }
            if (speed <= CAPTURE_SPEED) {
                sink(cup);
                event(BallEvent.HOLED);
                return true;
            }
            if (speed <= LIP_SPEED && dist < CAPTURE_RADIUS * 0.7 && lipCooldown == 0) {
                double angle = (random.nextBoolean() ? 1 : -1) * (0.35 + random.nextDouble() * 0.5);
                Vec3 v = getDeltaMovement();
                double c = Math.cos(angle), s = Math.sin(angle);
                setDeltaMovement((v.x * c - v.z * s) * 0.7, v.y, (v.x * s + v.z * c) * 0.7);
                lipCooldown = 10;
                level().playSound(null, cup, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1f, 0.8f);
                event(BallEvent.LIP);
            }
            return false;
        }
        return false;
    }

    static double distanceToSegment(double px, double pz, Vec3 a, Vec3 b) {
        double dx = b.x - a.x, dz = b.z - a.z;
        double len = dx * dx + dz * dz;
        double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - a.x) * dx + (pz - a.z) * dz) / len));
        return Math.hypot(a.x + dx * t - px, a.z + dz * t - pz);
    }

    /** What the ball may roll on: golf felt, tees and cups, ramps (stairs and slabs), launch pads and the guh's tongue. */
    public static boolean isCourse(BlockState state) {
        return state.is(GolfFeature.VILT.get()) || state.is(GolfFeature.AFSLAG.get()) || state.is(GolfFeature.HOLE.get())
                || state.is(ModBlocks.ROZE_SLIJMBLOK.get()) || state.is(ModBlocks.TONG.get())
                || state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock;
    }

    // --- client: smooth movement, and the rolling for the renderer ----------------------------------------------------

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return interpolation;
    }

    private void clientTick() {
        interpolation.interpolate();
        oRoll = roll;
        double dx = getX() - xo, dz = getZ() - zo;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 1e-4 && dist < 4) {
            rollYaw = (float) Math.atan2(dx, dz);
            roll += (float) (dist / (SIZE / 2));
        }
    }

    // --- not a normal entity: no hurting, pushing or saving ------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer player && player.getUUID().equals(getOwner())) {
            GolfGame.poked(player);
        }
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return InteractionResult.PASS;   // (so right-clicking the ball with the club starts the swing)
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 0.55f;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
