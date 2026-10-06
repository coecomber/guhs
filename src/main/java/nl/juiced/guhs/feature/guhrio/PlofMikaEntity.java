package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Plof-Mika: a square stone Mika that hangs in the air on its spot, scowling. When a player of the level passes under
 * it, it trembles for a moment and drops ("plof!"), lies there, and floats back up. Touching it sends you back to your
 * flag (or costs your power-up) - it only shoves. You cannot land on it and nothing stops it. Never saved, never hurt.
 */
public class PlofMikaEntity extends LevelWezen {
    public static final int HANGT = 0, TRILT = 1, VALT = 2, LIGT = 3, STIJGT = 4;
    /** How near (along the lane) and how far below a player must be; ticks of trembling, lying, resting before the next drop. */
    public static final double DICHTBIJ = 1.5, DIEP = 12;
    public static final int TRIL_TICKS = 8, LIG_TICKS = 25, RUST_TICKS = 20;
    public static final double STIJG = 0.12, VAL_MAX = 1.1;
    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(PlofMikaEntity.class, EntityDataSerializers.INT);

    private int ticks;

    public PlofMikaEntity(EntityType<? extends PlofMikaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAND, HANGT);
    }

    public int stand() {
        return this.entityData.get(DATA_STAND);
    }

    private void zetStand(int stand) {
        this.entityData.set(DATA_STAND, stand);
        ticks = 0;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ticks++;
        double thuisY = thuis == null ? getY() : thuis.getY();
        switch (stand()) {
            case HANGT -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (ticks > RUST_TICKS && iemandEronder()) {
                    zetStand(TRILT);
                }
            }
            case TRILT -> {
                if (ticks > TRIL_TICKS) {
                    zetStand(VALT);
                }
            }
            case VALT -> {
                double vy = Math.max(-VAL_MAX, this.getDeltaMovement().y - 0.14);
                this.move(MoverType.SELF, new Vec3(0, vy, 0));
                this.setDeltaMovement(0, vy, 0);
                if (this.onGround() || this.verticalCollision || (baan != null && getY() < baan.onder - 1)) {
                    this.setDeltaMovement(Vec3.ZERO);
                    zetStand(LIGT);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5f, 0.6f);
                    level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 10, 0.6, 0.05, 0.6, 0.03);
                }
            }
            case LIGT -> {
                if (ticks > LIG_TICKS) {
                    zetStand(STIJGT);
                }
            }
            default -> {
                double y = Math.min(thuisY, getY() + STIJG);
                this.setPos(getX(), y, getZ());
                if (y >= thuisY) {
                    zetStand(HANGT);
                }
            }
        }
    }

    /** Is a player of the level under it (near along the lane, lower, not too far down)? */
    private boolean iemandEronder() {
        if (baan == null) {
            return false;
        }
        double hier = baan.plek(getX(), getZ()).s();
        for (ServerPlayer p : spelers()) {
            GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
            if (s == null || s.lane() != baan || p.getY() > getY() || p.getY() < getY() - DIEP) {
                continue;
            }
            if (Math.abs(baan.plek(p.getX(), p.getZ()).s() - hier) < DICHTBIJ) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void terugThuis() {
        super.terugThuis();
        zetStand(HANGT);
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return !weg();
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        raakt(player, sessie);
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        return false;                                         // (a shell bounces off it)
    }
}
