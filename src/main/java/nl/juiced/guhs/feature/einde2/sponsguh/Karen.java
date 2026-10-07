package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: Karen, Plankmika's computer wife (block {@code guhs:karen}, a monitor with a face of lines).
 * Skeleton stub: the stand-in block has no face until the sponsguh_wezens slice fills it in.
 * <p>
 * hook fase B: the scenes in De Vetemmer change her face.
 */
public final class Karen {
    public static final int NEUTRAAL = 0, BLIJ = 1, BOOS = 2, ZUCHT = 3;

    /** Sets the face of the Karen block at this spot (block state {@code gezicht} 0-3; nothing happens when no Karen stands there). */
    public static void zetGezicht(Level level, BlockPos pos, int gezicht) {
    }

    private Karen() {
    }
}
