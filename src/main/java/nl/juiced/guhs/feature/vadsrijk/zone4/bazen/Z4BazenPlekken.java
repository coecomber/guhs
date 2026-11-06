package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensPlekken;

/**
 * The zone-4 boss spots. <b>PLACEHOLDERS</b> from the concept renders until t-z4wereld's POI_Z4 exists; every spot first asks
 * {@code Z4WereldPlekken.plek(id)} by reflection ({@link Z4WezensPlekken#wereld}). POI ids wanted (NOTITIES_T_Z4WEZENS):
 * {@code arena_zeeslurf} (the lagoon raft), {@code arena_zeeslurf_deur} (the dive jetty end), {@code arena_zeeslurf_bord},
 * {@code zeeslurf_grotje} (grotto 1); {@code arena_vliegervanger} (the fore deck), {@code arena_vliegervanger_deur},
 * {@code arena_vliegervanger_bord} (POI_Z4 v2 has all of these) and optionally {@code wrak_lier_1..3} (the three winches; without
 * them they stand at fixed offsets round the fore deck).
 */
public final class Z4BazenPlekken {
    private Z4BazenPlekken() {
    }

    private static BlockPos p(String id, int x, int y, int z) {
        BlockPos w = Z4WezensPlekken.wereld(id);
        return w != null ? w : new BlockPos(x, y, z);
    }

    // De Grauwe Zeeslurf: the pink lagoon of the Koraalbaai (top water block y 62; the fight centre is the raft on it), its grotto 1
    public static final int ZEE_Y = 62;
    public static BlockPos ZEESLURF_CENTRUM = p("arena_zeeslurf", 2185, 64, -565);
    public static BlockPos ZEESLURF_DEUR = p("arena_zeeslurf_deur", 2185, 65, -585);
    public static BlockPos ZEESLURF_GROTJE = p("zeeslurf_grotje", 2171, 47, -565);
    public static BlockPos ZEESLURF_BORD = p("arena_zeeslurf_bord", 2184, 65, -587);
    /** Radius 26 (the lagoon), height window 22 down (the grotto floor at -17) and 14 up. */
    public static final int ZEESLURF_STRAAL = 26, ZEESLURF_ONDER = 22, ZEESLURF_BOVEN = 14, ZEESLURF_RESPAWN = 20 * 60;

    // Het Wrak van de Vliegervanger: deck ramp -> hatch -> 3 rooms below deck (y 57) -> the boss on the fore deck (y 67)
    public static BlockPos WRAK_CENTRUM = p("arena_vliegervanger", 2158, 67, -700);
    public static BlockPos WRAK_DEUR = p("arena_vliegervanger_deur", 2165, 67, -700);
    public static BlockPos WRAK_BORD = p("arena_vliegervanger_bord", 2178, 67, -697);
    public static final int WRAK_STRAAL = 12, WRAK_ONDER = 5, WRAK_BOVEN = 30, WRAK_RESPAWN = 10 * 60;

    /** The three deck winches that hold the war kite (offsets from the deck centre when there is no POI). */
    public static BlockPos lier(int i) {
        int[][] o = {{-5, 0, -3}, {5, 0, -3}, {0, 0, 4}};
        BlockPos w = Z4WezensPlekken.wereld("wrak_lier_" + (i + 1));
        return w != null ? w : WRAK_CENTRUM.offset(o[i][0], o[i][1], o[i][2]);
    }

    /** Re-reads every spot (dev command "herlaad", after the POI arrived). */
    public static void herlaad() {
        ZEESLURF_CENTRUM = p("arena_zeeslurf", 2185, 64, -565);
        ZEESLURF_DEUR = p("arena_zeeslurf_deur", 2185, 65, -585);
        ZEESLURF_GROTJE = p("zeeslurf_grotje", 2171, 47, -565);
        ZEESLURF_BORD = p("arena_zeeslurf_bord", 2184, 65, -587);
        WRAK_CENTRUM = p("arena_vliegervanger", 2158, 67, -700);
        WRAK_DEUR = p("arena_vliegervanger_deur", 2165, 67, -700);
        WRAK_BORD = p("arena_vliegervanger_bord", 2178, 67, -697);
    }
}
