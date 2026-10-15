package nl.juiced.guhs.feature.einde2.geestwezens;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: Fang, the spirit dragon of Roku-guh (DESIGN_EINDE2 5, Fang's Hemelpad): a long red dragon, see-
 * through and glowing, with the guh's face and ears; he hovers where he is. On his neck is one seat (the model's bone
 * {@code zadel}; {@link #ZADEL} is its spot): whoever phase B puts on him ({@code startRiding}) sits there.
 * <p>
 * Animations: {@code idle} (he hovers and his body winds), {@code praat}, {@code vlieg} (a pose: the body streams out, the
 * legs tucked), {@code buig} (he lowers his head: climb on).
 * <p>
 * hook fase B: the short flight to the Top der Voorgangers in chapter 4 (who rides, where he flies) is the story's; a click
 * of a player never puts anyone on him here.
 */
public class FangEntity extends GeestWezen {
    /**
     * The seat on his neck in blocks, relative to his feet, +z = forwards (the model's bone {@code zadel}: x 0, y 34, z -40 in
     * model pixels; tools/features/einde2_geestmodellen_wezens.py FANG_ZADEL).
     */
    public static final Vec3 ZADEL = new Vec3(0.0, 34.0 / 16.0, 40.0 / 16.0);

    public FangEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return ZADEL.scale(scale).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** Whoever rides him turns along with him. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYBodyRot(getYRot());
    }
}
