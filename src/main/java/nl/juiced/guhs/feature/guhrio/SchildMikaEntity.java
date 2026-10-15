package nl.juiced.guhs.feature.guhrio;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A Schild-Mika: a Mika with a turtle shell on its back that walks its lane like a Guhmba. Land on it and it pulls into
 * its shell. Touch the shell (or land on it again) and it slides away from you, fast: it squashes every Guhmba in its way,
 * flips the switches it bumps into (for whoever kicked it), bounces off walls and drops off ledges. A sliding shell shoves
 * like any creature (back to your flag) - also the one who kicked it, so jump over it or land on it to stop it. Left
 * alone, the Mika peeks out again after a while. Never saved, never hurt.
 */
public class SchildMikaEntity extends LoopWezen {
    public static final int LOOPT = 0, SCHILD = 1, GLIJDT = 2;
    public static final double SNELHEID = 0.045, GLIJ = 0.38;
    /** A still shell opens again after this; a sliding one stops after {@link #GLIJ_TICKS}. */
    public static final int SCHILD_TICKS = 160, GLIJ_TICKS = 400, VEILIG = 8, WEG_TICKS = 100;
    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(SchildMikaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_VEILIG = SynchedEntityData.defineId(SchildMikaEntity.class, EntityDataSerializers.BOOLEAN);

    private int ticks, veilig;
    @Nullable
    private UUID schopper;
    /** Client: how far it is pulled in (0 walking .. 1 shell) and the shell's spin. */
    public float ingetrokken, ingetrokkenO, tol, tolO;

    public SchildMikaEntity(EntityType<? extends SchildMikaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAND, LOOPT);
        builder.define(DATA_VEILIG, false);
    }

    public int stand() {
        return this.entityData.get(DATA_STAND);
    }

    private void zetStand(int stand) {
        this.entityData.set(DATA_STAND, stand);
        ticks = 0;
    }

    @Override
    protected void clientTick() {
        super.clientTick();
        ingetrokkenO = ingetrokken;
        ingetrokken += ((stand() == LOOPT ? 0f : 1f) - ingetrokken) * 0.4f;
        tolO = tol;
        if (stand() == GLIJDT) {
            tol += 40f;
        }
    }

    @Override
    protected double snelheid() {
        return switch (stand()) {
            case LOOPT -> SNELHEID;
            case GLIJDT -> GLIJ;
            default -> 0;
        };
    }

    @Override
    protected boolean draaitBijAfgrond() {
        return stand() == LOOPT;
    }

    @Override
    protected void botst(ServerLevel level, BlockPos tegen) {
        if (stand() != GLIJDT) {
            return;
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.NEUTRAL, 0.7f, 1.2f);
        ServerPlayer wie = schopper();
        for (BlockPos pos : new BlockPos[]{tegen, tegen.above()}) {
            BlockState state = level.getBlockState(pos);
            if (wie != null && state.getBlock() instanceof GuhrioStukken.SchakelaarBlok schakelaar) {
                GuhrioSpel.Sessie s = GuhrioSpel.sessie(wie);
                if (s != null && s.actief == actief) {
                    schakelaar.schakel(wie, s, pos, state);
                }
                return;
            }
        }
    }

    @Nullable
    private ServerPlayer schopper() {
        return schopper == null || !(level() instanceof ServerLevel level) ? null : level.getPlayerByUUID(schopper) instanceof ServerPlayer p ? p : null;
    }

    @Override
    protected void naStap(ServerLevel level) {
        ticks++;
        if (veilig > 0 && --veilig == 0) {
            this.entityData.set(DATA_VEILIG, false);
        }
        int stand = stand();
        if (stand == SCHILD && ticks > SCHILD_TICKS) {
            zetStand(LOOPT);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.5f, 1.5f);
        } else if (stand == GLIJDT) {
            if (ticks > GLIJ_TICKS) {
                zetStand(SCHILD);
                return;
            }
            if (actief == null) {
                return;
            }
            ServerPlayer wie = schopper();
            for (Entity e : GuhrioSpel.wezens(actief)) {
                if (e != this && e instanceof GuhrioWezen w && e.isAlive() && e.getBoundingBox().intersects(this.getBoundingBox())) {
                    if (!w.schild(wie)) {
                        teken = -teken;
                        draai();
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void terugThuis() {
        super.terugThuis();
        zetStand(LOOPT);
        schopper = null;
    }

    /** The shell slides off, away from {@code van}. */
    public void schop(@Nullable ServerPlayer door, double vanX, double vanZ) {
        if (baan != null) {
            double hier = baan.plek(getX(), getZ()).s(), daar = baan.plek(vanX, vanZ).s();
            teken = hier >= daar ? 1 : -1;
        }
        schopper = door == null ? null : door.getUUID();
        zetStand(GLIJDT);
        veilig = VEILIG;
        this.entityData.set(DATA_VEILIG, true);
        draai();
        if (level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.TURTLE_SHAMBLE, SoundSource.NEUTRAL, 0.9f, 1.4f);
        }
    }

    /** Into its shell (it stands still). */
    public void inSchild() {
        zetStand(SCHILD);
        if (level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MIKA_HURT.get(), SoundSource.NEUTRAL, 0.6f, 1.4f);
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 5, 0.2, 0.1, 0.2, 0.02);
        }
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return !weg();
    }

    @Override
    public boolean gevaarlijk() {
        return !weg() && stand() != SCHILD && !this.entityData.get(DATA_VEILIG);
    }

    @Override
    public boolean aanraakbaar() {
        return !weg();
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (weg()) {
            return;
        }
        switch (stand()) {
            case LOOPT, GLIJDT -> inSchild();
            default -> schop(player, player.getX(), player.getZ());
        }
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.STUITER, this.blockPosition(), this.getId()));
    }

    @Override
    public void raakt(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (weg()) {
            return;
        }
        if (stand() == SCHILD) {
            schop(player, player.getX(), player.getZ());
        } else if (gevaarlijk()) {
            GuhrioSpel.geraakt(player, sessie);
        }
    }

    @Override
    public boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        if (weg() || stand() != LOOPT) {
            return false;
        }
        inSchild();
        return true;
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        if (!weg() && stand() == LOOPT) {
            inSchild();
        }
        return true;
    }

    @Override
    public boolean tong(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (weg()) {
            return false;
        }
        wegVoor(WEG_TICKS);
        return true;
    }
}
