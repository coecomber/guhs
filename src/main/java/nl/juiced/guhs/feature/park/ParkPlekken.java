package nl.juiced.guhs.feature.park;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * 1.4.2 (Het Knabbelpark): the spots in the templates of the required pieces (template coordinates: x east, z south, the
 * ground layer at y = 1, you walk at y = 2) that the code needs: who stands where (Bezetting repairs them at every copy),
 * the three plaques. tools/features/park_bouw.py builds the templates around exactly these numbers and its self-check
 * reads them from this file (the lines "NAAM = new BlockPos(x, y, z)" and "NAAM_YAW = f").
 */
public final class ParkPlekken {
    /** The template names of the pieces (each one is unique as a part of a name: Bezetting finds a piece by a part). */
    public static final String PLEIN = "knabbelpark/plein", BEELDENTUIN = "knabbelpark/beeldentuin", HERTENKOOI = "knabbelpark/hertenkooi",
            PARKWACHTER = "knabbelpark/parkwachter";

    /** The one Reisguh "Knabbelpark", in the very middle of the fountain plaza. */
    public static final BlockPos REISGUH = new BlockPos(16, 2, 16);
    public static final float REISGUH_YAW = 0f;
    /** The parkwachter-guh behind the counter of his hut, looking east (out of the front). */
    public static final BlockPos WACHTER = new BlockPos(8, 2, 7);
    public static final float WACHTER_YAW = -90f;
    /** The kooi-verzorger-guh by her hay bales, west of the paddock, looking north to the path. */
    public static final BlockPos VERZORGER = new BlockPos(3, 2, 15);
    public static final float VERZORGER_YAW = 180f;
    /** The three Hertguhtjes in the paddock. */
    public static final BlockPos HERT_1 = new BlockPos(10, 2, 10);
    public static final BlockPos HERT_2 = new BlockPos(13, 2, 14);
    public static final BlockPos HERT_3 = new BlockPos(15, 2, 9);
    public static final List<BlockPos> HERTEN = List.of(HERT_1, HERT_2, HERT_3);
    /** The paddock (inside the fence): the Hertguhtjes are looked for in here. */
    public static final BlockPos KOOI_MIN = new BlockPos(7, 2, 5), KOOI_MAX = new BlockPos(17, 4, 17);

    private ParkPlekken() {
    }
}
