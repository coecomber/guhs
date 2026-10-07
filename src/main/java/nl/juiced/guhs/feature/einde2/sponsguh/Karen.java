package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * einde2, slice sponsguh_wezens: Karen, Plankmika's computer wife (block {@code guhs:karen}, a monitor on a wheeled stand
 * with a face of lines: {@link SponsguhBlokken.KarenBlock}).
 * <p>
 * hook fase B: the scenes in De Vetemmer change her face.
 */
public final class Karen {
    public static final int NEUTRAAL = 0, BLIJ = 1, BOOS = 2, ZUCHT = 3;

    /** Sets the face of the Karen block at this spot (block state {@code gezicht} 0-3; nothing happens when no Karen stands there). */
    public static void zetGezicht(Level level, BlockPos pos, int gezicht) {
        BlockState state = level.getBlockState(pos);
        if (!level.isClientSide() && state.is(E2SponsguhFeature.KAREN.get())) {
            level.setBlock(pos, state.setValue(SponsguhBlokken.KarenBlock.GEZICHT, Mth.clamp(gezicht, NEUTRAAL, ZUCHT)), 3);
        }
    }

    /** The face of the Karen block at this spot (-1: no Karen stands there). */
    public static int gezicht(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(E2SponsguhFeature.KAREN.get()) ? state.getValue(SponsguhBlokken.KarenBlock.GEZICHT) : -1;
    }

    private Karen() {
    }
}
