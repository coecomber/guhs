package nl.juiced.guhs.feature.guhrio;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A knabbel thrown with the Vuurpeper: it flies along the lane, bounces over the ground a few times and is gone when it
 * hits a wall, a creature or after a couple of seconds. What it hits answers itself ({@link GuhrioWezen#knabbel}: a Guhmba
 * is flat, a Schild-Mika pulls into its shell, a Hapbloem ducks); a switch block it flies into is flipped for the thrower.
 * It never touches a player. Never saved.
 */
public class KnabbelEntity extends LevelWezen {
    public static final double SNELHEID = 0.5, ZWAARTE = 0.06, STUITER = 0.32;
    public static final int LEEFT = 50, STUITERS = 4;

    @Nullable
    private UUID gooier;
    private int teken = 1, ticks, stuiters;
    /** Client: its tumbling. */
    public float rol, rolO;

    public KnabbelEntity(EntityType<? extends KnabbelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Thrown by this player, {@code teken} +1 further along the lane / -1 back. */
    public void gooi(ServerPlayer door, int teken) {
        this.gooier = door.getUUID();
        this.teken = teken < 0 ? -1 : 1;
        this.setDeltaMovement(0, -0.12, 0);
    }

    @Override
    protected void clientTick() {
        rolO = rol;
        rol += 28f;
    }

    @Nullable
    private ServerPlayer gooier() {
        return gooier == null || !(level() instanceof ServerLevel level) ? null : level.getPlayerByUUID(gooier) instanceof ServerPlayer p ? p : null;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (++ticks > LEEFT || baan == null) {
            poef(level);
            return;
        }
        Direction d = baan.richting(stuk);
        double vy = this.getDeltaMovement().y - ZWAARTE;
        Vec3 stap = new Vec3(d.getStepX() * teken * SNELHEID, vy, d.getStepZ() * teken * SNELHEID);
        this.move(MoverType.SELF, stap);
        if (this.horizontalCollision) {
            ServerPlayer wie = gooier();
            BlockPos tegen = BlockPos.containing(getX() + d.getStepX() * teken * 0.5, getY() + 0.2, getZ() + d.getStepZ() * teken * 0.5);
            BlockState state = level.getBlockState(tegen);
            if (wie != null && state.getBlock() instanceof GuhrioStukken.SchakelaarBlok schakelaar) {
                GuhrioSpel.Sessie s = GuhrioSpel.sessie(wie);
                if (s != null) {
                    schakelaar.schakel(wie, s, tegen, state);
                }
            }
            poef(level);
            return;
        }
        if (this.onGround()) {
            if (++stuiters > STUITERS) {
                poef(level);
                return;
            }
            vy = STUITER;
        } else if (this.verticalCollision) {
            vy = -0.1;
        }
        this.setDeltaMovement(0, vy, 0);
        Baan.Stap op = baan.stap(stuk, getX(), getZ());
        stuk = op.stuk();
        this.setPos(op.x(), getY(), op.z());
        if (op.eind() || getY() < baan.onder - 2) {
            poef(level);
            return;
        }
        if (actief != null) {
            ServerPlayer wie = gooier();
            GuhrioSpel.Sessie s = wie == null ? null : GuhrioSpel.sessie(wie);
            for (Entity e : GuhrioSpel.wezens(actief)) {
                if (e != this && !(e instanceof KnabbelEntity) && e instanceof GuhrioWezen w && e.isAlive() && !e.isInvisible()
                        && w.raaktVak(this.getBoundingBox().inflate(0.15))) {
                    if (wie != null && s != null) {
                        w.knabbel(wie, s);
                    }
                    poef(level);
                    return;
                }
            }
        }
    }

    private void poef(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.4f, 1.6f);
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && !this.isRemoved() && gooier != null) {
            GuhrioSpel.knabbelWeg(gooier);
        }
        super.remove(reason);
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return false;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
    }
}
