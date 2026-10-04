package nl.juiced.guhs.feature;

import java.util.function.Supplier;

import net.minecraft.world.level.Level;

/**
 * 1.2.7: block changes that are the mod's own work, done from inside {@code Item.useOn}.
 * <p>
 * NeoForge records every block set during {@code useOn} ({@code Level.captureBlockSnapshots}) and afterwards fires a
 * place event for each one; in a protected place (a Knuffeldal town, the Guhboerderij, the Knuffelbad...) the protection
 * cancels those and the blocks are put back, so the item seems to do nothing in survival. Watering a tuintje, laying a
 * roof tile on its ghost tile or building a sneeuwpopguh is not "the player placing blocks": run it through
 * {@link #doe} and it isn't recorded (first used by timmerguh.DakpluisjeItem, 1.2.0).
 */
public final class EigenWerk {
    private EigenWerk() {
    }

    /** Runs the work with block recording off (and puts the recording back as it was). */
    public static <T> T doe(Level level, Supplier<T> werk) {
        boolean capture = level.captureBlockSnapshots;
        level.captureBlockSnapshots = false;
        try {
            return werk.get();
        } finally {
            level.captureBlockSnapshots = capture;
        }
    }
}
