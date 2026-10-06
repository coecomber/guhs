package nl.juiced.guhs.feature.bestaand;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * bbq2 (bestaand): the spots of this slice in the two existing buildings, in the coordinates of the whole build (what
 * Bezetting and Kopieen want for a guhs:burcht). The single source is tools/features/bestaand_bouw.py (PLEKKEN): its check
 * rebuilds both buildings, makes sure every spot is free there, reads THIS file and fails the generator run when a number
 * differs. So: change a number in both places, in exactly this shape ({@code NAME = new BlockPos(x, y, z);} or
 * {@code NAME = List.of(new BlockPos(x, y, z), ...);}).
 */
public final class Plekken {
    // --- Spiesburcht (middle 60, deck 30) ------------------------------------------------------------------------------
    /** The corner of the wachthokje (template bestaand_wachthokje, 3 x 6 x 3, open to the north), next to the statue. */
    public static final BlockPos WACHTHOKJE = new BlockPos(64, 31, 56);
    /** The Wachter-guh stands in it, looking north like the statue. */
    public static final BlockPos WACHTER = new BlockPos(65, 31, 57);
    /** Where the fire of the east bridge may stand (template bestaand_brugvuur_0), in order of preference. */
    public static final List<BlockPos> BRUGVUUR_0 = List.of(new BlockPos(95, 31, 61), new BlockPos(89, 31, 61), new BlockPos(101, 31, 61),
            new BlockPos(95, 31, 59), new BlockPos(89, 31, 59), new BlockPos(101, 31, 59));
    /** The south bridge. */
    public static final List<BlockPos> BRUGVUUR_1 = List.of(new BlockPos(59, 31, 95), new BlockPos(59, 31, 89), new BlockPos(59, 31, 101),
            new BlockPos(61, 31, 95), new BlockPos(61, 31, 89), new BlockPos(61, 31, 101));
    /** The west bridge. */
    public static final List<BlockPos> BRUGVUUR_2 = List.of(new BlockPos(25, 31, 59), new BlockPos(31, 31, 59), new BlockPos(19, 31, 59),
            new BlockPos(25, 31, 61), new BlockPos(31, 31, 61), new BlockPos(19, 31, 61));
    /** The north bridge. */
    public static final List<BlockPos> BRUGVUUR_3 = List.of(new BlockPos(61, 31, 25), new BlockPos(61, 31, 31), new BlockPos(61, 31, 19),
            new BlockPos(59, 31, 25), new BlockPos(59, 31, 31), new BlockPos(59, 31, 19));
    /** The six tufts of Mikakruid in the pindasaus-tuintje (on its paths, never in a bed). */
    public static final List<BlockPos> MIKAKRUID = List.of(new BlockPos(56, 40, 58), new BlockPos(65, 40, 61), new BlockPos(60, 40, 55),
            new BlockPos(60, 40, 64), new BlockPos(53, 40, 62), new BlockPos(67, 40, 56));
    /** The middle of the keep at deck height (for "which bridge is this" in a turned copy). */
    public static final BlockPos BURCHT_MIDDEN = new BlockPos(60, 31, 60);

    // --- Mika-grillpaleis (middle 36, first floor: you stand at y 22) --------------------------------------------------------
    /** The corner of the naaihoek (template bestaand_naaihoek, 5 x 4 x 6: its sign row first, the door in the north side). */
    public static final BlockPos NAAIHOEK = new BlockPos(41, 22, 40);
    /** The Knuffelmaker-guh sits in it, looking at his door. */
    public static final BlockPos KNUFFELMAKER = new BlockPos(43, 22, 43);
    /** The middles of the three cages with stolen plush guhs (5 x 5, the plush sits on this block). */
    public static final List<BlockPos> KOOIEN = List.of(new BlockPos(29, 22, 29), new BlockPos(43, 22, 29), new BlockPos(29, 22, 43));
    /** Where a freed plush sits in the naaihoek, for the player who freed it (cage i -> spot i). */
    public static final List<BlockPos> KNUFFELPLEKKEN = List.of(new BlockPos(42, 22, 42), new BlockPos(44, 22, 42), new BlockPos(44, 22, 43));

    /** The fire spots per bridge (the block state's nr: 0 east, 1 south, 2 west, 3 north in the template). */
    public static final List<List<BlockPos>> BRUGVUREN = List.of(BRUGVUUR_0, BRUGVUUR_1, BRUGVUUR_2, BRUGVUUR_3);

    private Plekken() {
    }
}
