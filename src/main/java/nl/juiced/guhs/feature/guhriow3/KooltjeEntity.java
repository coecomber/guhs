package nl.juiced.guhs.feature.guhriow3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.LevelWezen;

/**
 * bbq2 (guhrio-w3): a glowing coal that the Grote Nether-Mika throws: it sinks to knee height and then floats slowly along
 * the lane, over the bridge and over the gap alike, until it reaches a wall or burns out. Jump over it: a touch sends you
 * back to your flag (or costs your power-up) - it only shoves. A thrown knabbel puts it out. Never saved, never hurt.
 * Drawn by client.GuhrioW3Client from the box model guhriow3_kooltje (tools/features/guhrio_w3_modellen.py).
 */
public class KooltjeEntity extends LevelWezen {
    /** Blocks per tick along the lane, how fast it sinks to its height, how long it glows. */
    public static final double SNELHEID = 0.11, ZAKT = 0.06;
    public static final int LEEFT = 220;
    /** How high above the ground it floats (its box is 0.6 high: a standing player can't step over it). */
    public static final double VLIEGHOOGTE = 0.3;
    /** How many of them there are at most at once. */
    public static final int TEGELIJK = 3;

    private int teken = -1, ticks;
    private double vliegY;
    /** Client: its tumbling. */
    public float rol, rolO;

    public KooltjeEntity(EntityType<? extends KooltjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Thrown {@code teken} +1 further along the lane / -1 back; it floats at height {@code vliegY}. */
    public void gooi(int teken, double vliegY) {
        this.teken = teken < 0 ? -1 : 1;
        this.vliegY = vliegY;
    }

    @Override
    protected void clientTick() {
        rolO = rol;
        rol += 9f;
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.5, getZ(), 0, 0.02, 0);
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (++ticks > LEEFT || baan == null) {
            uit(level);
            return;
        }
        Direction d = baan.richting(stuk);
        double y = Math.max(vliegY, getY() - ZAKT);
        Baan.Stap op = baan.stap(stuk, getX() + d.getStepX() * teken * SNELHEID, getZ() + d.getStepZ() * teken * SNELHEID);
        stuk = op.stuk();
        this.setPos(op.x(), y, op.z());
        this.setDeltaMovement(Vec3.ZERO);
        // a wall in its way (and the lane's two ends)
        BlockPos voor = BlockPos.containing(op.x() + d.getStepX() * teken * 0.35, y + 0.3, op.z() + d.getStepZ() * teken * 0.35);
        if (op.eind() || !level.getBlockState(voor).getCollisionShape(level, voor).isEmpty()) {
            uit(level);
        }
    }

    private void uit(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 6, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.4f, 1.4f);
        this.discard();
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return true;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        raakt(player, sessie);
    }

    @Override
    public boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        if (level() instanceof ServerLevel level) {
            uit(level);
        }
        return true;
    }
}
