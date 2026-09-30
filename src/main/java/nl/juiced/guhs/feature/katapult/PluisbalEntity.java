package nl.juiced.guhs.feature.katapult;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pluisbal: a big soft ball of pink guh fluff, shot by the Knabbelkatapult. It flies with gravity, a little air drag and
 * (on lastig) the wind; when it hits a block of the Mika fort it knocks blocks out ({@link KatapultGame#impact}), bounces
 * a bit and flies on, until it lies still, has bounced enough or is gone: then it goes POEF in a cloud of fluff and tells
 * its game. It never hurts anyone (a pluisbal is as soft as a guh).
 */
public class PluisbalEntity extends Entity {
    public static final float SIZE = 0.5f;
    public static final double GRAVITY = 0.05, DRAG = 0.99;
    /** Knock energy = speed² × ENERGY (a stone block costs 4, wood 2, glass/Mikas 1). */
    public static final double ENERGY = 6.0;
    public static final double BOUNCE = 0.35, DONE_SPEED = 0.12;
    public static final int MAX_BOUNCES = 3, MAX_AGE = 20 * 12;

    @Nullable
    private UUID npc;
    private Vec3 wind = Vec3.ZERO;
    private int bounces;
    private boolean done;
    private double lowest = -1000;

    // client: smooth movement and spin
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    public float spin, oSpin;

    public PluisbalEntity(EntityType<? extends PluisbalEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static PluisbalEntity create(ServerLevel level, Vec3 pos, Vec3 velocity, @Nullable UUID npc, Vec3 wind, double lowest) {
        PluisbalEntity ball = new PluisbalEntity(KatapultFeature.PLUISBAL.get(), level);
        ball.snapTo(pos.x, pos.y, pos.z, 0, 0);
        ball.setDeltaMovement(velocity);
        ball.npc = npc;
        ball.wind = wind;
        ball.lowest = lowest;
        level.addFreshEntity(ball);
        return ball;
    }

    @Nullable
    public UUID getNpc() {
        return npc;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientTick();
            return;
        }
        if (done) {
            return;
        }
        if (npc != null && tickCount % 10 == 0 && !KatapultGame.ballAlive(this)) {
            discard();
            return;
        }
        Vec3 v = getDeltaMovement().add(wind).add(0, -GRAVITY, 0).scale(DRAG);
        Vec3 from = position(), to = from.add(v);
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            Vec3 at = hit.getLocation();
            double speed = v.length();
            boolean fort = npc != null && KatapultGame.hitFort(this, pos, at, v, speed * speed * ENERGY);
            Direction face = hit.getDirection();
            Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            double vn = v.dot(n);
            Vec3 bounced = v.subtract(n.scale(2 * vn)).scale(fort ? BOUNCE * 0.8 : BOUNCE);
            setPos(at.x + n.x * 0.05, at.y + n.y * 0.05, at.z + n.z * 0.05);
            setDeltaMovement(bounced);
            bounces++;
            level().playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1f, 0.9f);
            level().playSound(null, pos, SoundEvents.SLIME_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 1.3f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1f, 0.7f, 0.85f), 1.4f), at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.05);
            }
            if (bounces >= MAX_BOUNCES || bounced.length() < DONE_SPEED) {
                poof();
            }
            return;
        }
        setPos(to.x, to.y, to.z);
        setDeltaMovement(v);
        if (level() instanceof ServerLevel server && tickCount % 2 == 0) {
            server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1f, 0.75f, 0.9f), 0.9f), getX(), getY() + SIZE / 2, getZ(), 1, 0.05, 0.05, 0.05, 0);
        }
        if (tickCount > MAX_AGE || getY() < lowest) {
            poof();
        }
    }

    /** POEF: a cloud of fluff, and the game hears that this ball is done. */
    public void poof() {
        if (done) {
            return;
        }
        done = true;
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1f, 0.72f, 0.88f), 1.6f), getX(), getY() + 0.25, getZ(), 24, 0.35, 0.3, 0.35, 0.02);
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.25, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            level().playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        KatapultGame.ballDone(this);
        discard();
    }

    // --- client -----------------------------------------------------------------------------------------------------------

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    private void clientTick() {
        if (lerpSteps > 0) {
            double d = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
            lerpSteps--;
        }
        oSpin = spin;
        double dx = getX() - xo, dy = getY() - yo, dz = getZ() - zo;
        spin += (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.2f;
    }

    // --- not a normal entity -------------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return false;
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
