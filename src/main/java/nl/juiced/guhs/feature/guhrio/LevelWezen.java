package nl.juiced.guhs.feature.guhrio;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The shared part of every creature and moving thing of a Super Guhrio level: it belongs to a level somebody plays
 * ({@link #actief}; gone with it), knows its lane and the spot it came from, is never saved, never hurt and never hurts
 * anybody. A level slice's own creature (the boss) may extend this.
 * <ul>
 *     <li>{@link #serverTick}: its life on the server; {@link #clientTick}: what only the look needs.</li>
 *     <li>{@link #wegVoor}: "gone for a while" (eaten by Guhshi): invisible, harmless, and back on its spot afterwards.</li>
 * </ul>
 */
public abstract class LevelWezen extends Entity implements GuhrioWezen {
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    @Nullable
    protected GuhrioSpel.Actief actief;
    @Nullable
    protected Baan baan;
    @Nullable
    protected BlockPos thuis;
    /** The piece of the lane it is on. */
    protected int stuk;
    private int wegTicks;

    protected LevelWezen(EntityType<? extends LevelWezen> type, Level level) {
        super(type, level);
    }

    /** The level it belongs to, the lane it is on and the spot it came from (null: a loose one). */
    public void zetBaan(@Nullable GuhrioSpel.Actief actief, Baan baan, @Nullable BlockPos thuis) {
        this.actief = actief;
        this.baan = baan;
        this.thuis = thuis;
        this.stuk = baan.plek(getX(), getZ()).stuk();
    }

    /** (tests, and creatures made outside a level in use) */
    public void zetBaan(Baan baan, @Nullable BlockPos thuis) {
        zetBaan(null, baan, thuis);
    }

    @Nullable
    public Baan baan() {
        return baan;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (getInterpolation() != null) {
                getInterpolation().interpolate();
            }
            clientTick();
            return;
        }
        if (actief != null && !GuhrioSpel.leeft(actief)) {
            this.discard();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (wegTicks > 0) {
            if (--wegTicks == 0) {
                this.setInvisible(false);
                terugThuis();
            }
            return;
        }
        serverTick(level);
    }

    protected abstract void serverTick(ServerLevel level);

    protected void clientTick() {
    }

    /** Everybody who plays its level. */
    protected List<ServerPlayer> spelers() {
        return actief == null || !(level() instanceof ServerLevel level) ? List.of() : GuhrioSpel.spelers(level, actief);
    }

    /** Gone for {@code ticks} (invisible, harmless), then back on its spot. */
    public void wegVoor(int ticks) {
        wegTicks = Math.max(1, ticks);
        this.setInvisible(true);
        this.setDeltaMovement(0, 0, 0);
    }

    /** Is it gone for a while (both sides)? */
    public boolean weg() {
        return this.isInvisible();
    }

    /** Back on its spot, as new. */
    protected void terugThuis() {
        if (thuis != null) {
            this.setPos(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5);
            if (baan != null) {
                stuk = baan.plek(getX(), getZ()).stuk();
            }
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
