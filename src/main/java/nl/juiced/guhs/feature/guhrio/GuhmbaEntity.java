package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A Guhmba: a grumbling mini-Mika that walks up and down its lane. It turns at a wall, at a ledge and at the end of the
 * lane. Land on it and it is flat ("njeg!") and you bounce; a few seconds later it pops back up, giggling. Touch it from
 * the side and you are back at your flag (or you lose your power-up): it only shoves, nobody is ever hurt, and neither is
 * the Guhmba. A thrown knabbel or a sliding shell squashes it too; Guhshi's tongue eats it (it is back a little later).
 * It belongs to a {@link GuhrioBlocks.GuhmbaPlek} of a level somebody plays and is never saved.
 */
public class GuhmbaEntity extends LoopWezen {
    /** Blocks per tick. */
    public static final double SNELHEID = 0.05;
    /** How long it stays flat; how long it is gone after Guhshi ate it. */
    public static final int PLAT_TICKS = 60, WEG_TICKS = 100;
    private static final EntityDataAccessor<Boolean> DATA_PLAT = SynchedEntityData.defineId(GuhmbaEntity.class, EntityDataSerializers.BOOLEAN);

    private int platTicks;
    /** Client: how flat it is drawn (0 round .. 1 flat). */
    public float platheid, platheidO;

    public GuhmbaEntity(EntityType<? extends GuhmbaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PLAT, false);
    }

    public boolean plat() {
        return this.entityData.get(DATA_PLAT);
    }

    @Override
    protected void clientTick() {
        super.clientTick();
        platheidO = platheid;
        platheid += ((plat() ? 1f : 0f) - platheid) * 0.5f;
    }

    @Override
    protected double snelheid() {
        return plat() ? 0 : SNELHEID;
    }

    @Override
    protected void naStap(ServerLevel level) {
        if (plat() && --platTicks <= 0) {
            this.entityData.set(DATA_PLAT, false);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.5f, 1.9f);
        }
    }

    @Override
    protected void terugThuis() {
        super.terugThuis();
        this.entityData.set(DATA_PLAT, false);
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return !plat() && !weg();
    }

    @Override
    public boolean gevaarlijk() {
        return !plat() && !weg();
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        if (plat() || weg()) {
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

    @Override
    public boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        if (plat() || weg()) {
            return false;
        }
        maakPlat();
        return true;
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        if (!plat() && !weg()) {
            maakPlat();
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
