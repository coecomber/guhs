package nl.juiced.guhs.feature.einde2.geestwezens;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2 phase B, slice geest_modellen: the Oude Reigerguh (DESIGN_EINDE2 5 "De Witte Lotus", the epic ending of its
 * trials): a huge see-through crane spirit with guh ears and a red crown that has slept for ages coiled round the closed stone
 * lotus. It never moves from its spot. Poses: {@code slaap} (coiled, breathing slowly; it does not look at anybody),
 * {@code spreek} (standing, its beak moving: the history of the order), and {@code idle} = awake and standing; the
 * one-shot {@code ontwaak} unfolds it from the coil, wings spread wide, and folds them again.
 * <p>
 * hook fase B: geest_spel wakes it ({@link #ontwaak}) when all tiles lie, in its scene {@code reiger} (an actor with
 * {@code Houding:"slaap"}), and lets it talk ({@link #spreek}). A new one sleeps ({@code Houding} "slaap" from the building).
 */
public class OudeReigerguhEntity extends GeestWezen {
    public OudeReigerguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            zetAnimatie("slaap");                                   // (a new one sleeps; a saved one gets its Houding back)
        }
    }

    /** Is it asleep (the coil)? */
    public boolean slaapt() {
        return houding().equals("slaap");
    }

    /** Back to sleep, coiled round the stone lotus. */
    public void slaap() {
        zetAnimatie("slaap");
    }

    /** Wakes up: the unfolding (4 s) on top of the standing pose it keeps afterwards. */
    public void ontwaak() {
        zetAnimatie("idle");
        zetAnimatie("ontwaak");
    }

    /** Talks (a pose that lasts: the beak moves) or stops talking (stands awake). */
    public void spreek(boolean ja) {
        zetAnimatie(ja ? "spreek" : "idle");
    }

    @Override
    protected boolean kijktNaarSpelers() {
        return !slaapt();
    }
}
