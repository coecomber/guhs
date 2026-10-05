package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A Guhmba: a grumbling mini-Mika that walks up and down its lane. It turns at a wall, at a ledge and at the end of the
 * lane. Land on it and it is flat ("njeg!") and you bounce; a few seconds later it pops back up, giggling. Touch it from
 * the side and you are back at your flag (or you lose your power-up): it only shoves, nobody is ever hurt, and neither is
 * the Guhmba. It belongs to a {@link GuhrioBlocks.GuhmbaPlek} of a level somebody plays and is never saved.
 */
public class GuhmbaEntity extends Entity implements GuhrioWezen {
    /** Blocks per tick. */
    public static final double SNELHEID = 0.05;
    /** How long it stays flat. */
    public static final int PLAT_TICKS = 60;
    private static final EntityDataAccessor<Boolean> DATA_PLAT = SynchedEntityData.defineId(GuhmbaEntity.class, EntityDataSerializers.BOOLEAN);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    @Nullable
    private Baan baan;
    @Nullable
    private BlockPos thuis;
    private int stuk;
    /** +1 further along the lane, -1 back. */
    private int teken = -1;
    private int platTicks;
    /** Client: how flat it is drawn (0 round .. 1 flat) and its walk. */
    public float platheid, platheidO, loop, loopO;

    public GuhmbaEntity(EntityType<? extends GuhmbaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PLAT, false);
    }

    /** The lane it walks on and the spot it belongs to. */
    public void zetBaan(Baan baan, BlockPos thuis) {
        this.baan = baan;
        this.thuis = thuis;
        this.stuk = baan.plek(getX(), getZ()).stuk();
        draai();
    }

    public boolean plat() {
        return this.entityData.get(DATA_PLAT);
    }

    /** Which way it walks along its lane (+1 / -1). */
    public int teken() {
        return teken;
    }

    public void zetTeken(int teken) {
        this.teken = teken < 0 ? -1 : 1;
        draai();
    }

    private Vec3 vooruit() {
        Direction d = baan == null ? Direction.EAST : baan.richting(stuk);
        return new Vec3(d.getStepX() * teken, 0, d.getStepZ() * teken);
    }

    private void draai() {
        Vec3 v = vooruit();
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        this.setYRot(yaw);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            interpolation.interpolate();
            platheidO = platheid;
            platheid += ((plat() ? 1f : 0f) - platheid) * 0.5f;
            loopO = loop;
            Vec3 d = this.position().subtract(this.xo, this.yo, this.zo);
            loop += (float) Math.sqrt(d.x * d.x + d.z * d.z) * 6f;
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Vec3 v = this.getDeltaMovement();
        double val = this.onGround() ? -0.08 : v.y - 0.08;
        if (plat()) {
            if (--platTicks <= 0) {
                this.entityData.set(DATA_PLAT, false);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.5f, 1.9f);
            }
            this.move(MoverType.SELF, new Vec3(0, val, 0));
            this.setDeltaMovement(0, this.onGround() ? 0 : val * 0.98, 0);
            return;
        }
        Vec3 voor = vooruit();
        if (this.onGround() && afgrond(level, voor)) {
            teken = -teken;
            voor = vooruit();
        }
        this.move(MoverType.SELF, new Vec3(voor.x * SNELHEID, val, voor.z * SNELHEID));
        this.setDeltaMovement(voor.x * SNELHEID, this.onGround() ? 0 : val * 0.98, voor.z * SNELHEID);
        if (this.horizontalCollision) {
            teken = -teken;
        }
        if (baan != null) {
            Baan.Stap stap = baan.stap(stuk, getX(), getZ());
            stuk = stap.stuk();
            this.setPos(stap.x(), getY(), stap.z());
            if (stap.eind()) {
                teken = -teken;
            }
            if (getY() < baan.onder - 4) {
                this.discard();                       // (fell out of the level: its spot makes a new one)
                return;
            }
        } else if (getY() < level.getMinY() - 8) {
            this.discard();
            return;
        }
        draai();
        if (this.isInLava() || this.isInWater()) {
            this.clearFire();
        }
    }

    /** Is there nothing to stand on one step ahead? */
    private boolean afgrond(ServerLevel level, Vec3 voor) {
        BlockPos onder = BlockPos.containing(getX() + voor.x * 0.6, getY() - 0.3, getZ() + voor.z * 0.6);
        return level.getBlockState(onder).getCollisionShape(level, onder).isEmpty();
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return !plat();
    }

    @Override
    public boolean gevaarlijk() {
        return !plat();
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (plat()) {
            return;
        }
        maakPlat();
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.STUITER, this.blockPosition(), this.getId()));
    }

    /** Flat ("njeg!"); it pops back after {@link #PLAT_TICKS}. */
    public void maakPlat() {
        this.entityData.set(DATA_PLAT, true);
        platTicks = PLAT_TICKS;
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MIKA_HURT.get(), SoundSource.NEUTRAL, 0.6f, 1.7f);
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 6, 0.25, 0.05, 0.25, 0.02);
        }
    }

    // --- an entity that is only there for the game ----------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
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
}
