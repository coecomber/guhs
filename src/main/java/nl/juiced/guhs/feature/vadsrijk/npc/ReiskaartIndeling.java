package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;

/**
 * 1.10.3: THE layout of the travel map (Reiskaart), shared by the server ({@link Reiskaart}), the client
 * ({@code npc/client/ReiskaartScherm}) and the art script tools/features/vadsrijk_reiskaart_art.py, which reads the constants and
 * the {@code new Plek(...)} / {@code new Naam(...)} lines below with a regex (keep their one-line format). The map is a LINEAR
 * picture of the real world: image pixel = (world - (X0, Z0)) * S, 1 image pixel = 1 GUI pixel. Zone 5 (Hoogvads) floats above
 * the east of zone 4: it is drawn {@link #Z5_OPTIL} pixels higher, with its shadow on the real spot.
 */
public final class ReiskaartIndeling {
    private ReiskaartIndeling() {
    }

    // ---- the world -> image mapping (art script: keep "NAME = value;" on one line) --------------------------------------------
    public static final int W = 1256;
    public static final int H = 688;
    public static final int X0 = -720;
    public static final int Z0 = -1530;
    public static final float S = 0.25f;
    /** Zone 5 floats: drawn this many image pixels higher than its real x/z. */
    public static final int Z5_OPTIL = 34;

    /** Image x of a world x. */
    public static float px(double x) {
        return (float) ((x - X0) * S);
    }

    /** Image y of a world z (on the ground). */
    public static float py(double z) {
        return (float) ((z - Z0) * S);
    }

    /** Image y of a world z in this zone (zone 5 floats higher). */
    public static float py(double z, int zone) {
        return py(z) - (zone == 5 ? Z5_OPTIL : 0);
    }

    public static boolean inBeeld(float x, float y) {
        return x >= 0 && x < W && y >= 0 && y < H;
    }

    // ---- the destinations --------------------------------------------------------------------------------------------------------

    /** A destination: its badge spot (world x/z) and the land box (minX, minZ, maxX, maxZ) the grey mist covers when closed. */
    public record Plek(String id, int zone, int x, int z, int[] land) {
        public float bx() {
            return px(x);
        }

        public float by() {
            return py(z, zone);
        }
    }

    /** In map order (= {@link Reiskaart#PLEKKEN}). Zone 0 = the camp island (never closed). */
    public static final List<Plek> PLEKKEN = List.of(
            new Plek("knuffelkamp", 0, -384, 1010, VadsrijkZones.LAND_KAMP),
            new Plek("zwamzoet", 1, 0, 0, VadsrijkZones.LAND_Z1),
            new Plek("kandijkom", 2, 704, -1057, VadsrijkZones.LAND_Z2),
            new Plek("schemerwortel", 3, 1404, -576, VadsrijkZones.LAND_Z3),
            new Plek("lampionkust", 4, 1846, -470, VadsrijkZones.LAND_Z4),
            new Plek("hoogvads", 5, 2716, -520, VadsrijkZones.LAND_Z5),
            new Plek("zijdeland", 6, 3762, -800, VadsrijkZones.LAND_Z6));

    @Nullable
    public static Plek plek(String id) {
        for (Plek p : PLEKKEN) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** The zone of a destination (-1 = unknown). */
    public static int zone(String id) {
        Plek p = plek(id);
        return p == null ? -1 : p.zone();
    }

    /** The airship routes (dotted): line 0 camp-Haven, A Zwamzoet-Kandijkom, B -> zone 3, C -> zone 4, D -> Hoogvads, E -> Zijdeland. */
    public static final String[][] ROUTES = {{"knuffelkamp", "zwamzoet"}, {"zwamzoet", "kandijkom"}, {"kandijkom", "schemerwortel"},
            {"schemerwortel", "lampionkust"}, {"lampionkust", "hoogvads"}, {"hoogvads", "zijdeland"}};

    // ---- the area names (small labels, only for zones open for the player) -----------------------------------------------------

    /**
     * An area label: {@code gebied} = a {@link nl.juiced.guhs.feature.vadsrijk.kern.Gebieden} id (shown as
     * {@code vadsrijk.kern.gebied.<id>}), its zone and the world x/z of the label (the area's box centre, nudged so labels do not
     * overlap; zone 3 is layered, so its labels sit around the trunk). Only the main areas: the map stays readable.
     */
    public record Naam(String gebied, int zone, int x, int z) {
    }

    public static final List<Naam> NAMEN = List.of(
            new Naam("klodderweide", 1, 380, 50),
            new Naam("sporenwoud", 1, -380, 120),
            new Naam("druipgrotten", 1, -380, -380),
            new Naam("zwamtempel", 1, 380, -450),
            new Naam("z2_splinterkloof", 2, 600, -1250),
            new Naam("z2_spiegelburcht", 2, 870, -1380),
            new Naam("z2_galmhellingen", 2, 900, -940),
            new Naam("z2_knuffelmeer", 2, 520, -930),
            new Naam("z3_kruinzee", 3, 1340, -790),
            new Naam("z3_zeppelinwerf", 3, 1643, -827),
            new Naam("z3_honingtak", 3, 1250, -560),
            new Naam("z3_wortelvelden", 3, 1260, -380),
            new Naam("z3_takkenbanen", 3, 1600, -400),
            new Naam("z4_strandfeest", 4, 1995, -605),
            new Naam("z4_koraalbaai", 4, 2185, -565),
            new Naam("z4_vliegereiland", 4, 2300, -665),
            new Naam("z4_bloemeneiland", 4, 2085, -405),
            new Naam("z4_schildpad", 4, 2290, -420),
            new Naam("z4_knuffeltij", 4, 1935, -345),
            new Naam("z4_mysterie", 4, 2480, -330),
            new Naam("z5_ooggaarde", 5, 2551, -600),
            new Naam("z5_paleisheuvel", 5, 2818, -610),
            new Naam("z5_luchthaven", 5, 2630, -370),
            new Naam("z5_staartvlakte", 5, 2985, -520),
            new Naam("z6_palmenkloven", 6, 3500, -930),
            new Naam("z6_rozenkloof", 6, 3368, -726),
            new Naam("z6_roze_duinen", 6, 3530, -620),
            new Naam("z6_ruinedal", 6, 4015, -980),
            new Naam("z6_zoutvlakte", 6, 3946, -600),
            new Naam("z6_moederoase", 6, 4090, -760));
}
