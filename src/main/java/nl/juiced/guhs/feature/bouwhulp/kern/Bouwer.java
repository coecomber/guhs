package nl.juiced.guhs.feature.bouwhulp.kern;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who builds a ghost that is linked to a block (the Bouwkeet, later maybe a helper's board): it says how fast, where the
 * blocks come from, and hears what happens. A ghost without a bouwer builds at its own {@link Spook#tempo} from the
 * builder's inventory and the ghost's own bank ({@link Spook#bank}).
 */
public interface Bouwer {
    /** Blocks per second now (0: wait, e.g. the keet is switched off). */
    double tempo(ServerLevel level, Spook spook);

    /** Where the blocks come from ({@code bouwer}: the player who pressed "Bouw!", maybe offline: null). */
    Materiaal.Bronnen bronnen(ServerLevel level, Spook spook, @Nullable ServerPlayer bouwer);

    /** May the whole thing appear at once (Pof!, level 4)? Asked once when building starts with everything there. */
    default boolean pof(ServerLevel level, Spook spook) {
        return false;
    }

    /**
     * Test round 5: how far (horizontally, in x and in z) from the builder's block it builds; spots further away stay a ghost
     * (drawn red). Unlimited by default.
     */
    default int bereik(ServerLevel level) {
        return Integer.MAX_VALUE;
    }

    /** A block was placed (for sounds, the crane, the bouwguhs). */
    default void geplaatst(ServerLevel level, Spook spook, BlockPos pos) {
    }

    /** Building stopped: done ({@code klaar}) or out of material (the rest stays a ghost). */
    default void gestopt(ServerLevel level, Spook spook, boolean klaar) {
    }
}
