package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

/**
 * einde2, slice sponsguh_wezens: juf Pufguh, the puffer guh of the boating school. Her flag {@code Opgeblazen} (NBT,
 * synced): she blows up to twice her size with her spikes out (her hitbox grows along) and stays put until she lets the
 * air out again.
 * <p>
 * hook fase B: the boating exam blows her up with {@link #setOpgeblazen}.
 */
public class JufPufguhEntity extends SponsguhWezen {
    public JufPufguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public boolean isOpgeblazen() {
        return vlag();
    }

    public void setOpgeblazen(boolean opgeblazen) {
        if (opgeblazen == isOpgeblazen()) {
            return;
        }
        if (opgeblazen) {
            super.zetAnimatie("blaas_op");
            playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0f, 0.8f);
            getNavigation().stop();
        } else {
            playSound(SoundEvents.PUFFER_FISH_BLOW_OUT, 1.0f, 0.8f);
        }
        zetVlag(opgeblazen);
        refreshDimensions();
    }

    @Override
    protected boolean magLopen() {
        return !isOpgeblazen() && super.magLopen();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dims = super.getDefaultDimensions(pose);
        return isOpgeblazen() ? dims.scale(2.0f) : dims;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (isVlagData(key)) {
            refreshDimensions();                                             // (the client's hitbox follows the flag)
        }
    }
}
