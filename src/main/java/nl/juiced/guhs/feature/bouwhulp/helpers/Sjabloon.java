package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An adaptive template (design 2.8): it looks at the real terrain and works out a fitting ghost. {@link #zoek} is "Zoek
 * plek" (the spot(s) the family picks itself near the board), {@link #maak} the blocks for given spots (world positions; an
 * AIR state digs a spot out). Both throw {@link Klus.Nee} when it cannot ("Hier stroomt niks, njeg").
 */
public interface Sjabloon {
    /** What kind of job: a ghost to build, or a running job without blocks (the kangaroo's pouch, the elephant carrying). */
    enum Soort { SPOOK, TRANSPORT, SJOUWEN }

    /** The spot(s) found by the family itself ({@link #punten} of them). */
    List<BlockPos> zoek(Klus k);

    /** The blocks for these spots (clicked blocks or found spots). */
    Map<BlockPos, BlockState> maak(Klus k, List<BlockPos> punten);

    /** How many spots the job needs (a bridge between two trees, a garland, the pouch transport: 2). */
    default int punten() {
        return 1;
    }

    /** The variants (the colour theme, the room style...): 1 = none to choose. */
    default int varianten() {
        return 1;
    }

    default Soort soort() {
        return Soort.SPOOK;
    }
}
