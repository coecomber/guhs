package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;

/**
 * EVERY coordinate of the zone-5 combat slice (T-Z5WEZENS): where you fight in Hoogvads, the safe capital on the back of Moeder
 * Wolkwal. The city itself is safe; combat lives in four contained places (decided by this slice, see NOTITIES_T_Z5WEZENS):
 * <ol>
 *   <li><b>De Proefarena van de Wolkwacht</b> (the Academie's trials ground, south): possessed training dummies and sparring with
 *       knight guhs (non-lethal, the guhs never hurt);</li>
 *   <li><b>De Buikgangen</b> (the maintenance tunnels inside the whale, under the market; in through a hatch in the cellar of the
 *       Grote Soepkeuken): Mika saboteurs and grey fluff mites, and the small dungeon with <b>Opzichter Gorgonzola</b> in the
 *       Ketelkamer (the whale's warm-air valves);</li>
 *   <li><b>De Wolkrand</b> (the two pectoral fins and the tail fluke, below the city rim): grey cloud-imps, mist wisps and Mika
 *       harpooners attack the whale; world boss <b>Donderbuik</b> on the tail fluke;</li>
 *   <li><b>Het Schuilnest</b> (a secret Mika hideout inside the whale under the Luchthaven, in through a loose plank behind the
 *       crates of the station): disguised Mika's.</li>
 * </ol>
 * The spy finale (Kootje Zilverstaart) fights in t-z5verhaal's Wolkwaldroom.
 * <p><b>PLACEHOLDERS.</b> The city map is t-z5wereld's (later). Every spot is {@code w("<poi id>", x, y, z)}: the generated
 * {@code zone5.wereld.Z5WereldPlekken.plek(id)} wins when that class exists (reflection, so this compiles without it), else the
 * number here. Numbers from renders-hoogvads/_bron (wolkwal.py: back y ~230-243, belly y ~148-165 between x 2580 and 2800; the
 * pectoral fins at x 2590..2662, z -620..-712 / -420..-328, top y 182..151; the tail fluke x ~2938..3012, z -648..-392, y ~222-235)
 * through export.py's placement {@code gx = 2702 - x, gz = -520 - z}. The two indoor places (Buikgangen, Schuilnest) are this
 * slice's OWN rooms inside the hollow whale ({@link Z5Bouw}), clearly apart from the city and from the Wolkwaldroom box
 * (x 2540..2700, z -600..-440, y 150..225).
 */
public final class Z5WezensPlekken {
    private Z5WezensPlekken() {
    }

    // ---- t-z5wereld's generated spots (reflection) -----------------------------------------------------------------------------
    private static Method wereldPlek;
    private static boolean gezocht;

    @Nullable
    public static synchronized BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                wereldPlek = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken").getMethod("plek", String.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                wereldPlek = null;
            }
        }
        if (wereldPlek == null) {
            return null;
        }
        try {
            return wereldPlek.invoke(null, id) instanceof BlockPos b ? b : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** All POI ids of this slice with their (placeholder) spot, in a stable order (dev command "plekken", the POI request list). */
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        BlockPos r = b != null ? b : new BlockPos(x, y, z);
        POI.put(id, r);
        return r;
    }

    /** The zone-5 box (T-WERELD's tile box; zone 5 from y 150 up: below is zone 4's sea). */
    public static final int MIN_X = 2432, MAX_X = 3199, MIN_Z = -768, MAX_Z = -257, VANAF_Y = 150;

    public static boolean inZone(double x, double y, double z) {
        return y >= VANAF_Y && x >= MIN_X && x < MAX_X + 1 && z >= MIN_Z && z < MAX_Z + 1;
    }

    // ---- 1. De Proefarena van de Wolkwacht (Academie, south-east of the oefenplein) --------------------------------------------
    public static final BlockPos PROEFARENA = w("z5w_proefarena", 2760, 235, -592);
    public static final int PROEF_STRAAL = 11;

    // ---- 2. De Buikgangen (own rooms, floor y 186; x 2706..2806 under the market) ----------------------------------------------
    public static final int BUIK_Y = 166;   // (T-INT: t-z5wereld hangs the whale 20 lower: was 186)
    /** The hatch in the cellar of the Grote Soepkeuken (right-click: down) and where you land inside. */
    public static final BlockPos BUIK_LUIK = w("z5w_buik_luik", 2716, 239, -540);
    public static final BlockPos BUIK_AANKOMST = w("z5w_buik_aankomst", 2711, BUIK_Y, -520);
    /** The ladder hatch inside (right-click: back up) and where you come out. */
    public static final BlockPos BUIK_UITLUIK = w("z5w_buik_uitluik", 2708, BUIK_Y, -524);
    public static final BlockPos BUIK_UIT = w("z5w_buik_uit", 2716, 240, -537);
    public static final BlockPos RIBBENGANG = w("z5w_ribbengang", 2735, BUIK_Y, -520);
    public static final BlockPos KLEPPENZAAL = w("z5w_kleppenzaal", 2761, BUIK_Y, -520);
    public static final BlockPos KETELKAMER = w("z5w_ketelkamer", 2792, BUIK_Y, -520);
    public static final BlockPos KETEL_DEUR = w("z5w_ketelkamer_deur", 2780, BUIK_Y, -520);
    public static final int KETEL_STRAAL = 13;

    // ---- 3. De Wolkrand (fins + tail fluke) --------------------------------------------------------------------------------------
    public static final BlockPos ZUIDVIN = w("z5w_zuidvin", 2605, 176, -650);
    public static final BlockPos NOORDVIN = w("z5w_noordvin", 2605, 176, -390);
    /** Cloud lifts from the city rim down to the fins and the tail (right-click), placeholders for t-z5wereld's stairs. */
    public static final BlockPos LIFT_ZUID_BOVEN = w("z5w_lift_zuidvin_boven", 2610, 232, -615);
    public static final BlockPos LIFT_ZUID_ONDER = w("z5w_lift_zuidvin_onder", 2600, 178, -640);
    public static final BlockPos LIFT_NOORD_BOVEN = w("z5w_lift_noordvin_boven", 2610, 232, -425);
    public static final BlockPos LIFT_NOORD_ONDER = w("z5w_lift_noordvin_onder", 2600, 178, -400);
    public static final BlockPos LIFT_STAART_BOVEN = w("z5w_lift_staart_boven", 2900, 236, -545);
    public static final BlockPos LIFT_STAART_ONDER = w("z5w_lift_staart_onder", 2948, 229, -560);
    public static final BlockPos STAARTVLAKTE = w("z5w_staartvlakte_noord", 2975, 228, -455);
    /** World boss Donderbuik on the south lobe of the tail fluke. */
    public static final BlockPos DONDERBUIK = w("z5w_donderbuik_arena", 2975, 228, -585);
    public static final BlockPos DONDERBUIK_DEUR = w("z5w_donderbuik_deur", 2952, 228, -575);
    public static final int DONDER_STRAAL = 20;

    // ---- 4. Het Schuilnest (own room under the Luchthaven, floor y 208) -------------------------------------------------------------
    public static final int NEST_Y = 188;   // (T-INT: the whale is 20 lower: was 208)
    public static final BlockPos NEST_LUIK = w("z5w_schuilnest_luik", 2612, 237, -446);
    public static final BlockPos NEST_AANKOMST = w("z5w_schuilnest_aankomst", 2594, NEST_Y, -427);
    public static final BlockPos NEST_UITLUIK = w("z5w_schuilnest_uitluik", 2591, NEST_Y, -430);
    public static final BlockPos NEST_UIT = w("z5w_schuilnest_uit", 2610, 238, -444);
    public static final BlockPos SCHUILNEST = w("z5w_schuilnest", 2606, NEST_Y, -427);

    // ---- the spy finale (t-z5verhaal's Wolkwaldroom: Z5VerhaalPlekken.WOLKWALDROOM, entrance SPUITGAT) ------------------------------
    public static final BlockPos WOLKWALDROOM = w("wolkwaldroom_arena", 2600, 200, -520);
    public static final BlockPos WOLKWALDROOM_DEUR = w("z5w_wolkwaldroom_deur", 2585, 200, -520);
    public static final int DROOM_STRAAL = 16;

    // ---- the named area leaders -------------------------------------------------------------------------------------------------
    public static final Map<String, BlockPos> LEIDERS = new LinkedHashMap<>();

    static {
        LEIDERS.put("strobaal", w("z5w_leider_strobaal", 2766, 235, -596));
        LEIDERS.put("mistmoeder", w("z5w_leider_mistmoeder", 2980, 228, -450));
        LEIDERS.put("meestervermommer", w("z5w_leider_meestervermommer", 2612, NEST_Y, -425));
    }

    // ---- the hatches and lifts (Z5Luiken) ----------------------------------------------------------------------------------------
    /** A spot you right-click (a trapdoor / the lift block) that takes you to {@code naar}. */
    public record Luik(String id, BlockPos klik, BlockPos naar) {
    }

    public static final List<Luik> LUIKEN = new ArrayList<>(List.of(
            new Luik("buik_in", BUIK_LUIK, BUIK_AANKOMST), new Luik("buik_uit", BUIK_UITLUIK, BUIK_UIT),
            new Luik("nest_in", NEST_LUIK, NEST_AANKOMST), new Luik("nest_uit", NEST_UITLUIK, NEST_UIT),
            new Luik("zuidvin_af", LIFT_ZUID_BOVEN, LIFT_ZUID_ONDER), new Luik("zuidvin_op", LIFT_ZUID_ONDER.offset(2, 0, 0), LIFT_ZUID_BOVEN.above()),
            new Luik("noordvin_af", LIFT_NOORD_BOVEN, LIFT_NOORD_ONDER), new Luik("noordvin_op", LIFT_NOORD_ONDER.offset(2, 0, 0), LIFT_NOORD_BOVEN.above()),
            new Luik("staart_af", LIFT_STAART_BOVEN, LIFT_STAART_ONDER), new Luik("staart_op", LIFT_STAART_ONDER.offset(2, 0, 0), LIFT_STAART_BOVEN.above())));

    // ---- the combat areas (kern areas, put in FRONT of t-z5verhaal's town boxes so the spawners may work here) --------------------
    /** id, box (x/z), y-range, levels. NOT towns. */
    public static final List<Gebieden.Gebied> GEBIEDEN = List.of(
            new Gebieden.Gebied("z5_proefarena", PROEFARENA.getX() - PROEF_STRAAL - 2, PROEFARENA.getZ() - PROEF_STRAAL - 2, PROEFARENA.getX() + PROEF_STRAAL + 2,
                    PROEFARENA.getZ() + PROEF_STRAAL + 2, 56, 57, false).metY(PROEFARENA.getY() - 6, PROEFARENA.getY() + 10),
            new Gebieden.Gebied("z5_buikgangen", 2700, -545, 2810, -495, 57, 59, false).metY(BUIK_Y - 6, BUIK_Y + 14),
            new Gebieden.Gebied("z5_schuilnest", 2586, -440, 2624, -414, 58, 60, false).metY(NEST_Y - 4, NEST_Y + 10),
            new Gebieden.Gebied("z5_wolkrand_zuid", 2560, -700, 2670, -615, 58, 60, false).metY(125, 185),   // (T-INT: the whale is 20 lower: was 145..205)
            new Gebieden.Gebied("z5_wolkrand_noord", 2560, -425, 2670, -340, 58, 60, false).metY(125, 185),   // (T-INT: the whale is 20 lower: was 145..205)
            new Gebieden.Gebied("z5_staartvlakte", 2932, -655, 3025, -385, 58, 60, false).metY(185, 230));   // (T-INT: the whale is 20 lower: was 205..250)

    /** The Wolkrand areas with the spot a falling player is caught back to (Z5Vangnet). */
    public static final Map<String, BlockPos> VANGNET = new LinkedHashMap<>();

    static {
        VANGNET.put("z5_wolkrand_zuid", LIFT_ZUID_ONDER);
        VANGNET.put("z5_wolkrand_noord", LIFT_NOORD_ONDER);
        VANGNET.put("z5_staartvlakte", LIFT_STAART_ONDER);
    }

    @Nullable
    public static Gebieden.Gebied gebied(String id) {
        for (Gebieden.Gebied g : GEBIEDEN) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** Is this point inside one of this slice's combat areas? Which one (or null). */
    @Nullable
    public static String gebiedOp(double x, double y, double z) {
        for (Gebieden.Gebied g : GEBIEDEN) {
            if (g.bevat(x, y, z)) {
                return g.id();
            }
        }
        return null;
    }
}
