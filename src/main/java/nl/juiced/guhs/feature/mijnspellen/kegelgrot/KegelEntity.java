package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A thing on a lane of the Kegelgrot: a plush guh pin ({@link KegelgrotFeature#KEGEL}), the pink wool ball
 * ({@link KegelgrotFeature#BAL}) or the sliding Mika-blokje ({@link KegelgrotFeature#SCHUIF}). No physics of its own: the
 * {@link KegelGame} moves it to where {@link KegelBaan} says, every tick; the client glides along. A pin knows whether it
 * lies (OM, and its yRot is the way it fell), the ball how far it has rolled (ROL, for the renderer). One that no game
 * owns any more disappears by itself; they are never saved.
 */
public class KegelEntity extends Entity {
    private static final EntityDataAccessor<Boolean> OM = SynchedEntityData.defineId(KegelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ROL = SynchedEntityData.defineId(KegelEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    KegelGame game;

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;

    public KegelEntity(EntityType<? extends KegelEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    static KegelEntity create(ServerLevel level, EntityType<KegelEntity> type, KegelGame game, Vec3 pos, float yRot) {
        KegelEntity e = new KegelEntity(type, level);
        e.game = game;
        e.snapTo(pos.x, pos.y, pos.z, yRot, 0);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OM, false);
        builder.define(ROL, 0f);
    }

    public boolean om() {
        return entityData.get(OM);
    }

    void om(boolean om) {
        if (om() != om) {
            entityData.set(OM, om);
        }
    }

    public float rol() {
        return entityData.get(ROL);
    }

    void rol(float rol) {
        entityData.set(ROL, rol);
    }

    /** Which block model the renderer draws. */
    public BlockState model() {
        if (getType() == KegelgrotFeature.BAL.get()) {
            return KegelgrotFeature.WOLBAL_BLOK.get().defaultBlockState();
        }
        if (getType() == KegelgrotFeature.SCHUIF.get()) {
            return KegelgrotFeature.MIKABLOKJE.get().defaultBlockState();
        }
        return KegelgrotFeature.KEGELTJE.get().defaultBlockState();
    }

    void zet(Vec3 pos, float yRot) {
        setPos(pos.x, pos.y, pos.z);
        setYRot(yRot);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (lerpSteps > 0) {
                double d = 1.0 / lerpSteps;
                setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
                setYRot(getYRot() + (float) net.minecraft.util.Mth.wrapDegrees(lerpYRot - getYRot()) * (float) d);
                lerpSteps--;
            }
            return;
        }
        if (tickCount % 20 == 0 && (game == null || !game.owns(this))) {
            discard();
        }
    }

    private final InterpolationHandler interpolation = new InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            lerpX = position.x;
            lerpY = position.y;
            lerpZ = position.z;
            lerpYRot = yRot;
            lerpSteps = 2;
        }

        @Override
        public Vec3 position() {
            return lerpSteps > 0 ? new Vec3(lerpX, lerpY, lerpZ) : KegelEntity.this.position();
        }

        @Override
        public boolean hasActiveInterpolation() {
            return lerpSteps > 0;
        }

        @Override
        public void interpolate() {
        }

        @Override
        public void cancel() {
            lerpSteps = 0;
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }
}
