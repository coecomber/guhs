package nl.juiced.guhs.feature.park.plek;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * 1.4.2 (Het Knabbelpark, plekjes A): the spots in the templates of the five plekjes of this part (template coordinates: x
 * east, z south, the ground layer at y = 1, you walk at y = 2; the entrance is on the south edge) that the code needs: who
 * lives where (Bezetting repairs them at every copy), where the rowing boat lies. tools/features/park_plek_a.py builds the
 * templates around exactly these numbers and reads them from this file (the lines "NAAM = new BlockPos(x, y, z)" and
 * "NAAM_YAW = f").
 */
public final class PlekAPlekken {
    /** The template names (each one unique as a part of a name: Bezetting and Kopieen find a piece by a part). */
    public static final String EENDENVIJVER = "knabbelpark/eendenvijver", SPEELTUIN = "knabbelpark/speeltuin",
            PICKNICKWEIDE = "knabbelpark/picknickweide", MUZIEKTUIN = "knabbelpark/muziektuin", JAPANSE_TUIN = "knabbelpark/japanse_tuin";
    public static final List<String> ALLE = List.of(EENDENVIJVER, SPEELTUIN, PICKNICKWEIDE, MUZIEKTUIN, JAPANSE_TUIN);

    // --- de eendenvijver ------------------------------------------------------------------------------------------------
    /** The berth of the rowing boat: a water block beside the end of the jetty. */
    public static final BlockPos LIGPLAATS = new BlockPos(6, 1, 7);
    /** The two guh-eendje mamas (each with her rijtje kuikentjes), on the water. */
    public static final BlockPos EEND_1 = new BlockPos(12, 1, 4);
    public static final BlockPos EEND_2 = new BlockPos(5, 1, 3);
    public static final List<BlockPos> EENDEN = List.of(EEND_1, EEND_2);

    // --- de picknickweide -----------------------------------------------------------------------------------------------
    /** The seller-guh behind the counter of the kraampje, looking south (out over the meadow). */
    public static final BlockPos KRAAM = new BlockPos(7, 2, 2);
    public static final float KRAAM_YAW = 0f;

    // --- de muziektuin --------------------------------------------------------------------------------------------------
    /** The jukebox in the middle of the muziektent (on its raised floor). */
    public static final BlockPos JUKEBOX = new BlockPos(8, 3, 9);
    /** The three dancing guhs on the floor of the muziektent. */
    public static final BlockPos DANSER_1 = new BlockPos(6, 3, 9);
    public static final BlockPos DANSER_2 = new BlockPos(10, 3, 9);
    public static final BlockPos DANSER_3 = new BlockPos(8, 3, 7);
    public static final List<BlockPos> DANSERS = List.of(DANSER_1, DANSER_2, DANSER_3);

    // --- de japanse tuin ------------------------------------------------------------------------------------------------
    /** The four Knabbelkoi's, deep in the koi pond. */
    public static final BlockPos KOI_1 = new BlockPos(11, 0, 5);
    public static final BlockPos KOI_2 = new BlockPos(13, 0, 7);
    public static final BlockPos KOI_3 = new BlockPos(12, 0, 3);
    public static final BlockPos KOI_4 = new BlockPos(10, 0, 7);
    public static final List<BlockPos> KOIEN = List.of(KOI_1, KOI_2, KOI_3, KOI_4);

    private PlekAPlekken() {
    }
}
