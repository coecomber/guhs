package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B, slice h4_vuur: a ball of fire in the finale that never burns anybody (DESIGN 0: Mika's only shove). It is
 * only light and sparks on the client; the minigame that made it decides what touching it does (a shove, never damage) and
 * what a click on it does ({@link #bijSlag}: hit it back, {@link #bijKlik}: douse it). Kinds ({@code Soort}):
 * <ul>
 *   <li>{@link #STOOT}: a blast of Mikai or of an airship, flying;</li>
 *   <li>{@link #VLAM}: a flame that stands on a pillar top (douse it with a right click: water);</li>
 *   <li>{@link #VONK}: a spark of the comet falling from the sky;</li>
 *   <li>{@link #TERUG}: a blast you hit back (blue: it is yours now).</li>
 * </ul>
 * Never saved ({@code noSave}): a server restart in the middle of the duel leaves nothing behind.
 */
public class VuurbalEntity extends Entity {
    public static final int STOOT = 0, VLAM = 1, VONK = 2, TERUG = 3;
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(VuurbalEntity.class, EntityDataSerializers.INT);

    /** Server, not saved: what a click (left: hit) and a right click on it do; and how long it lives. */
    @Nullable
    BiConsumer<VuurbalEntity, ServerPlayer> bijSlag, bijKlik;
    int leven = 200;
    /** (fix4-finale) Server: this blast was already counted by the duel (dodged, a hit, or hit back). */
    boolean geteld;

    public VuurbalEntity(EntityType<? extends VuurbalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_SOORT, STOOT);
    }

    public int soort() {
        return entityData.get(DATA_SOORT);
    }

    public void zetSoort(int s) {
        entityData.set(DATA_SOORT, s);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            vonken();                                    // (the server moves it; the client follows its position updates)
            return;
        }
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (--leven <= 0) {
            discard();
        }
    }

    /** Client: what it looks like (it has no model: only light). */
    private void vonken() {
        Level l = level();
        double x = getX(), y = getY() + 0.4, z = getZ();
        switch (soort()) {
            case VLAM -> {
                l.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y - 0.2, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.05, 0);
                if (random.nextInt(3) == 0) {
                    l.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0, 0.03, 0);
                }
            }
            case VONK -> {
                l.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                l.addParticle(ParticleTypes.FIREWORK, x, y + 0.3, z, 0, 0.02, 0);
            }
            case TERUG -> {
                for (int i = 0; i < 3; i++) {
                    l.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + (random.nextDouble() - 0.5) * 0.6, y + (random.nextDouble() - 0.5) * 0.6,
                            z + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                }
            }
            default -> {
                for (int i = 0; i < 3; i++) {
                    l.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.6, y + (random.nextDouble() - 0.5) * 0.6,
                            z + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                }
                l.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.01, 0);
            }
        }
    }

    @Override
    public boolean isPickable() {
        return soort() == STOOT || soort() == VLAM;
    }

    @Override
    public float getPickRadius() {
        return 0.5f;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    /** A left click: the minigame's {@link #bijSlag} (hitting a blast back), never damage. */
    @Override
    public boolean skipAttackInteraction(Entity bron) {
        if (!level().isClientSide() && bron instanceof ServerPlayer p && bijSlag != null) {
            bijSlag.accept(this, p);
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && player instanceof ServerPlayer p && bijKlik != null && hand == InteractionHand.MAIN_HAND) {
            bijKlik.accept(this, p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
