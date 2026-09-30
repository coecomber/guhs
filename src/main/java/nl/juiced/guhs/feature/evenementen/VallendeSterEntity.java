package nl.juiced.guhs.feature.evenementen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A falling star of the sterrenregen: it shoots down in a straight line (with a sparkly tail) to the spot it was aimed
 * at, and then lies there glowing as a bit of sterrenstof you can pick up by walking into it (see {@link Sterrenregen}).
 * Never saved, never breaks anything.
 */
public class VallendeSterEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(VallendeSterEntity.class, EntityDataSerializers.VECTOR3);
    /** Blocks per tick. */
    public static final double SPEED = 1.1;

    /** Server: ticks since it landed (-1: still falling). */
    int landedTicks = -1;
    /** Server: the exact spot it's aimed at (the synced copy is only floats: not precise far from 0,0). */
    private Vec3 serverTarget = Vec3.ZERO;

    public VallendeSterEntity(EntityType<? extends VallendeSterEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, new Vector3f());
    }

    public void aimAt(Vec3 target) {
        this.serverTarget = target;
        this.entityData.set(DATA_TARGET, new Vector3f((float) target.x, (float) target.y, (float) target.z));
    }

    public Vec3 target() {
        if (!this.level().isClientSide) {
            return serverTarget;
        }
        Vector3f t = this.entityData.get(DATA_TARGET);
        return new Vec3(t.x, t.y, t.z);
    }

    public boolean landed() {
        return this.position().distanceToSqr(target()) < 1.0e-4;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(EvenementenFeature.STERRENSTOF.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 20 == 0 && !Evenementen.owns(this)) {
            this.discard(); // a leftover of an event that's over
            return;
        }
        if (!this.level().isClientSide) {
            return; // on the server its event moves it (see serverStep): also where its chunk doesn't tick entities
        }
        Vec3 step = step();
        if (step != null) {
            for (int i = 0; i < 3; i++) { // the tail
                double f = this.random.nextDouble();
                this.level().addParticle(i == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD, this.getX() - step.x * f,
                        this.getY() - step.y * f + 0.2, this.getZ() - step.z * f, 0, 0, 0);
            }
        } else if (this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX() + (this.random.nextDouble() - 0.5) * 0.6, this.getY() + 0.3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.6, 0, 0.03, 0);
        }
    }

    /** One step towards the target (null: it's there already). */
    private Vec3 step() {
        Vec3 to = target().subtract(this.position());
        double dist = to.length();
        if (dist <= 1.0e-6) {
            return null;
        }
        Vec3 step = dist <= SPEED ? to : to.scale(SPEED / dist);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        return step;
    }

    /**
     * Server: moved by its {@link Sterrenregen} every server tick, whether or not its chunk ticks entities (a star comes
     * from far away and high up, often from a chunk outside the simulation distance, where it would hang still).
     */
    void serverStep() {
        step();
        if (landed()) {
            landedTicks++;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160; // you want to see it coming
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
