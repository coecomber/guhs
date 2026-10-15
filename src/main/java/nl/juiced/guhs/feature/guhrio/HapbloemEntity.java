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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Hapbloem: a flower with a big kissing mouth that lives in a green pipe (its spot, {@link GuhrioStukken.HapbloemPlek},
 * is the cell above the pipe's mouth). It comes up, looks around, and goes down again; it stays down while a player stands
 * on or right next to its pipe. Touch it while it is out and you get a wet kiss that sends you back to your flag (or costs
 * your power-up). The pipe can only be used while the flower is down. A knabbel, a shell or Guhshi's tongue sends it down
 * for a while. Never saved, never hurt.
 */
public class HapbloemEntity extends LevelWezen {
    /** Ticks: down, rising / sinking, out; and down after being scared off. */
    public static final int BINNEN = 50, BEWEEG = 12, BUITEN = 45, GESCHROKKEN = 100;
    /** How far it sinks into its pipe (it is this tall). */
    public static final double HOOG = 1.5;
    /** It stays down while a player is this near along the lane. */
    public static final double DICHTBIJ = 1.6;
    private static final EntityDataAccessor<Float> DATA_UIT = SynchedEntityData.defineId(HapbloemEntity.class, EntityDataSerializers.FLOAT);

    private int ticks = BINNEN / 2;
    /** Client: its chewing. */
    public float hap, hapO;

    public HapbloemEntity(EntityType<? extends HapbloemEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_UIT, 0f);
    }

    /** 0 down in its pipe .. 1 all the way out. */
    public float uit() {
        return this.entityData.get(DATA_UIT);
    }

    /** Is it (partly) out of its pipe? */
    public boolean buiten() {
        return uit() > 0.1f;
    }

    @Override
    protected void clientTick() {
        hapO = hap;
        hap += 0.35f;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ticks++;
        int ronde = BINNEN + BEWEEG + BUITEN + BEWEEG;
        if (ticks >= ronde) {
            ticks = 0;
        }
        if (ticks == BINNEN && iemandDichtbij()) {
            ticks = BINNEN - 10;                               // (not while somebody is on the pipe)
        }
        float uit = ticks < BINNEN ? 0f : ticks < BINNEN + BEWEEG ? (ticks - BINNEN) / (float) BEWEEG
                : ticks < BINNEN + BEWEEG + BUITEN ? 1f : 1f - (ticks - BINNEN - BEWEEG - BUITEN) / (float) BEWEEG;
        if (Math.abs(uit - uit()) > 1e-4) {
            this.entityData.set(DATA_UIT, uit);
        }
        if (thuis != null) {
            this.setPos(thuis.getX() + 0.5, thuis.getY() - HOOG * (1 - uit), thuis.getZ() + 0.5);
        }
        this.setDeltaMovement(Vec3.ZERO);
    }

    private boolean iemandDichtbij() {
        if (baan == null || thuis == null) {
            return false;
        }
        double hier = baan.plek(thuis.getX() + 0.5, thuis.getZ() + 0.5).s();
        for (ServerPlayer p : spelers()) {
            if (Math.abs(p.getY() - thuis.getY()) < 2.5 && Math.abs(baan.plek(p.getX(), p.getZ()).s() - hier) < DICHTBIJ) {
                return true;
            }
        }
        return false;
    }

    /** Down, now, and it stays there for a while. */
    public void schrik() {
        ticks = -GESCHROKKEN;
        this.entityData.set(DATA_UIT, 0f);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 5, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return uit() > 0.25f;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        raakt(player, sessie);
    }

    @Override
    public void raakt(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (!gevaarlijk() || !(level() instanceof ServerLevel level)) {
            return;
        }
        // a big wet kiss
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_ATTACK, SoundSource.NEUTRAL, 0.8f, 1.7f);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.6, player.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
        GuhrioSpel.geraakt(player, sessie);
    }

    @Override
    public boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        if (!buiten()) {
            return false;
        }
        schrik();
        return true;
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        if (buiten()) {
            schrik();
        }
        return true;
    }

    @Override
    public boolean tong(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (!buiten()) {
            return false;
        }
        schrik();
        return true;
    }
}
