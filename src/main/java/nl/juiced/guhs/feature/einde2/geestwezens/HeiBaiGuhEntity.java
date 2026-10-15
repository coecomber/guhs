package nl.juiced.guhs.feature.einde2.geestwezens;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

/**
 * einde2, slice geestmodellen: Hei Bai-guh, the spirit of the Gefluisterwoud (DESIGN_EINDE2 5). Two forms in one model, picked
 * by {@code Soort} (the renderer shows one): {@link #KLEIN} the little black-and-white panda spirit guh with his white face
 * mask, who wanders about his wood; {@link #BOOS} the angry giant of about five blocks, black, with many arms and the white
 * mask face, who stands still. The hitbox follows the form ({@link #GROOT}).
 * <p>
 * Animations: {@code idle}, {@code praat}, {@code groei} (the giant swells up out of nothing: play it right after
 * {@link #wordBoos}), {@code kalm} (the giant shrinks away: {@link #kalmeer} turns him back into the little one at its end).
 * <p>
 * hook fase B: his side quest (his wood burnt down in the real world; a knabbelkoor acorn calms him and the wood blooms) uses
 * {@link #wordBoos} / {@link #kalmeer}; nothing of it lives here. He never hurts anyone, also not as a giant.
 */
public class HeiBaiGuhEntity extends GeestWezen {
    public static final int KLEIN = 0, BOOS = 1;
    /** The giant's hitbox. */
    public static final EntityDimensions GROOT = EntityDimensions.scalable(3.0f, 5.0f).withEyeHeight(4.4f);

    /** Server: the game time at which "kalm" is over and he is the little one again (-1: not calming down). */
    private long kleinOp = -1;

    public HeiBaiGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public boolean isBoos() {
        return soort() == BOOS;
    }

    /** He becomes the angry giant (and swells up). */
    public void wordBoos() {
        kleinOp = -1;
        zetSoort(BOOS);
        zetAnimatie("groei");
    }

    /** The giant calms down: he shrinks away and is the little panda again when "kalm" ends. */
    public void kalmeer() {
        if (!isBoos()) {
            zetAnimatie("kalm");
            return;
        }
        zetAnimatie("kalm");
        kleinOp = level().getGameTime() + wezen.duur("kalm");
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && kleinOp >= 0 && level().getGameTime() >= kleinOp) {
            kleinOp = -1;
            zetSoort(KLEIN);
        }
    }

    /** A new Hei Bai-guh is the little one. */
    @Override
    protected boolean soortGezet() {
        if (!super.soortGezet() && !level().isClientSide()) {
            super.zetSoort(KLEIN);
        }
        return true;
    }

    @Override
    public void zetSoort(int soort) {
        super.zetSoort(soort);
        refreshDimensions();
        if (isBoos()) {
            getNavigation().stop();
        }
    }

    @Override
    protected boolean magLopen() {
        return !isBoos() && super.magLopen();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return isBoos() ? GROOT : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (isSoortData(key)) {
            refreshDimensions();                                    // (the client's hitbox follows the form)
        }
    }
}
