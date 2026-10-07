package nl.juiced.guhs.feature.guhriow3;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * bbq2 (guhrio-w3): the cake of Prinses Perzikguh on her serving cart, as a thing a cutscene can roll around: the prop of
 * the end scene of the duel ({@link GuhrioW3Feature#EINDE}). It does nothing: the scene moves it (it is one of the scene's
 * actors, only in the viewer's own game). {@code Stukjes} (0..3) is how many pieces are gone. One that somebody summons by
 * hand just stands there and is never saved. Drawn by client.GuhrioW3Client from the box model guhriow3_taart (the cart,
 * the plate, four quarters: tools/features/guhrio_w3_modellen.py).
 */
public class TaartEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_STUKJES = SynchedEntityData.defineId(TaartEntity.class, EntityDataSerializers.INT);
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    public TaartEntity(EntityType<? extends TaartEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STUKJES, 0);
    }

    /** How many pieces have been eaten (0..3). */
    public int stukjes() {
        return this.entityData.get(DATA_STUKJES);
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
        }
    }

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
        this.entityData.set(DATA_STUKJES, Math.max(0, Math.min(3, tag.getIntOr("Stukjes", 0))));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.putInt("Stukjes", stukjes());
    }
}
