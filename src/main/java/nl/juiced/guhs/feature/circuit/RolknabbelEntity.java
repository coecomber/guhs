package nl.juiced.guhs.feature.circuit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.race.RaceGuhEntity;

/**
 * A big rolling kaasknabbel on the Kaasberg's Knabbelhelling: the Mika's push them down the hill (CircuitExtra), they roll
 * down faster and faster, and a race guh that runs into one gets a bump (it stops and hops: RaceGuhEntity.SCHOK_BOTS).
 * Nobody gets hurt: the knabbel crumbles to kaaskruimels and the Mika's giggle. It crumbles at the bottom too, and when it
 * bumps into a wall. Never saved.
 */
public class RolknabbelEntity extends Entity {
    /** How fast it starts, how much faster it gets per tick, how fast it gets at most (blocks a tick). */
    public static final double START = 0.12, ERBIJ = 0.012, MAX = 0.5;
    public static final int LEVEN = 20 * 12;
    private Vec3 richting = new Vec3(0, 0, 1);
    private double snelheid = START;
    private int leven;
    /** Client: how far it has rolled (for the turning). */
    public float rol, rolO;
    private boolean bumped;

    public RolknabbelEntity(EntityType<? extends RolknabbelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Sends it rolling this way (horizontal). */
    public void rol(Vec3 dir) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        this.richting = flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
        float yaw = (float) (Math.atan2(-richting.x, richting.z) * 180 / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    public boolean bumped() {
        return bumped;
    }

    @Override
    public void tick() {
        super.tick();
        rolO = rol;
        if (this.level().isClientSide()) {
            Vec3 d = this.position().subtract(this.xo, this.yo, this.zo);
            rol += (float) (Math.sqrt(d.x * d.x + d.z * d.z) / 0.7);
            return;
        }
        if (++leven > LEVEN || this.tickCount > 40 && !CircuitExtra.isLive(this)) {
            crumble(false);
            return;
        }
        snelheid = Math.min(MAX, snelheid + ERBIJ);
        Vec3 v = this.getDeltaMovement();
        Vec3 move = new Vec3(richting.x * snelheid, this.onGround() ? -0.08 : v.y - 0.08, richting.z * snelheid);
        this.move(MoverType.SELF, move);
        this.setDeltaMovement(move.x, this.onGround() ? 0 : move.y * 0.98, move.z);
        if (this.horizontalCollision) {
            crumble(false);
            return;
        }
        if (this.isInWater() || this.isInLava() || this.getY() < this.level().getMinY()) {
            crumble(false);
            return;
        }
        for (RaceGuhEntity guh : this.level().getEntitiesOfClass(RaceGuhEntity.class, this.getBoundingBox().inflate(0.15))) {
            if (!guh.inRit()) {
                hit(guh);
                return;
            }
        }
    }

    /** Boem: a race guh ran into it. */
    public void hit(RaceGuhEntity guh) {
        bumped = true;
        guh.schok(RaceGuhEntity.SCHOK_BOTS);
        if (guh.getFirstPassenger() instanceof ServerPlayer racer) {
            racer.sendOverlayMessage(Component.translatable("quest.guhs.circuit.rolknabbel").withStyle(ChatFormatting.GOLD));
        }
        crumble(true);
    }

    /** Crumbles to kaaskruimels (and a giggle from up the hill when it got someone). */
    private void crumble(boolean got) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState()),
                    getX(), getY() + 0.6, getZ(), 30, 0.5, 0.4, 0.5, 0.1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1f, 0.8f);
            if (got) {
                level.playSound(null, getX(), getY(), getZ(), nl.juiced.guhs.registry.ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.7f, 1.9f);
            }
        }
        this.discard();
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
