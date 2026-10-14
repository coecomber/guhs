package nl.juiced.guhs.feature.einde2.castschurk;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * einde2, slice cast_schurk: a vosguh, one of the knowledge seekers of Wan Shi Guh's library (CONTRACT_E2 11.6). It carries a
 * scroll in its mouth when {@link #setDraagt} says so (NBT {@code Draagt}, synced; the renderer shows the bone
 * {@code vosguh_rol}). hook fase B: a vosguh that brings a scroll to its shelf plays {@code breng} and puts it down.
 */
public class VosguhEntity extends CastWezen {
    private static final EntityDataAccessor<Boolean> DATA_DRAAGT = SynchedEntityData.defineId(VosguhEntity.class, EntityDataSerializers.BOOLEAN);

    public VosguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DRAAGT, false);
    }

    public boolean draagt() {
        return entityData.get(DATA_DRAAGT);
    }

    public void setDraagt(boolean draagt) {
        entityData.set(DATA_DRAAGT, draagt);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Draagt", draagt());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setDraagt(tag.getBooleanOr("Draagt", false));
    }
}
