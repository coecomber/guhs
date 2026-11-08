package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * EVERY coordinate of the zone-6 combat slice (U-Z6WEZENS), Het Zijdeland (ZONE6_PLAN §1-§2: x 3280..4180, z -1060..-460, lv 60-70).
 * <p><b>PLACEHOLDERS.</b> The map is u-z6wereld's. Every spot is {@code w("<poi id>", x, y, z)}: the generated
 * {@code zone6.wereld.Z6WereldPlekken.plek(String)} wins when that class exists (reflection, so this compiles without it), else
 * the number here (from the plan's coordinates; heights are guesses: open-air spawners search the ground themselves). The list of
 * ids is in NOTITIES_U_Z6WEZENS ("POI-verzoeken") and {@code /guhs vadsrijk z6wezens plekken}.
 * <ul>
 *   <li>creatures: 5 areas (De Roze Duinen 60-62, De Palmenkloven 62-64, De Zoutvlakte 64-66, Het Ruïnedal 66-68, De Moederoase
 *       68-70), 3-4 small spawn spots each ({@code z6w_mobs_<gebied>_<n>}), a night spot on the salt flat (Zandgeestjes), and one
 *       named area leader per area ({@code z6w_leider_<naam>});</li>
 *   <li>NO creatures in the cities (Rozenkloof, Saffraanpoort), the caravan camp, or at the foot of the raid tower;</li>
 *   <li>field bosses: Koningin Wormelien (the dunes, by day) and De Zoutkolos (the salt flat, at night);</li>
 *   <li>the dungeon De Verzonken Sterrenwacht: this slice's OWN rooms under the Ruïnedal ({@link Z6Bouw}), in through a hatch at
 *       {@code z6w_sterrenwacht_ingang};</li>
 *   <li>the cave elite De Zoutslang in De Zingende Zoutgrot ({@code z6w_zoutgrot_elite}).</li>
 * </ul>
 */
public final class Z6WezensPlekken {
    private Z6WezensPlekken() {
    }

    // ---- u-z6wereld's generated spots (reflection) ------------------------------------------------------------------------------
    private static Method wereldPlek;
    private static boolean gezocht;

    @Nullable
    public static synchronized BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                wereldPlek = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken").getMethod("plek", String.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                wereldPlek = null;
            }
        }
        if (wereldPlek == null) {
            return null;
        }
        try {
            Object o = wereldPlek.invoke(null, id);
            if (o instanceof BlockPos b) {
                return b;
            }
            if (o instanceof int[] a && a.length >= 3) {
                return new BlockPos(a[0], a[1], a[2]);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // fall back to the placeholder
        }
        return null;
    }

    /** True when u-z6wereld's spot class is on the classpath (real POI), false = placeholders. */
    public static boolean echt() {
        wereld("z6w_test");
        return wereldPlek != null;
    }

    /** All POI ids of this slice with their (placeholder or real) spot, in a stable order. */
    public static final Map<String, BlockPos> POI = new LinkedHashMap<>();

    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = wereld(id);
        BlockPos r = b != null ? b : new BlockPos(x, y, z);
        POI.put(id, r);
        return r;
    }

    /** The zone-6 box (ZONE6_PLAN §1.1). */
    public static final int MIN_X = 3280, MAX_X = 4180, MIN_Z = -1060, MAX_Z = -460;

    public static boolean inZone(double x, double z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    // ---- the areas -------------------------------------------------------------------------------------------------------------

    /** An area with creatures: id, level band. */
    public record Gebied(String id, int minLevel, int maxLevel) {
    }

    public static final List<Gebied> GEBIEDEN = List.of(new Gebied("duinen", 60, 62), new Gebied("palmenkloven", 62, 64),
            new Gebied("zoutvlakte", 64, 66), new Gebied("ruinedal", 66, 68), new Gebied("moederoase", 68, 70));

    @Nullable
    public static Gebied gebied(String id) {
        for (Gebied g : GEBIEDEN) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** A spawn spot: POI id, area, centre, radius. */
    public record Regio(String id, String gebied, BlockPos centrum, int straal) {
    }

    public static final List<Regio> REGIOS = new ArrayList<>();

    private static void r(String gebied, int n, int x, int y, int z, int straal) {
        String id = "z6w_mobs_" + gebied + "_" + n;
        REGIOS.add(new Regio(id, gebied, w(id, x, y, z), straal));
    }

    /** The night spot on the salt flat (only Zandgeestjes, only at night). */
    public static BlockPos ZOUT_NACHT;
    public static final int ZOUT_NACHT_STRAAL = 14;

    /** The named area leaders: name -> spot. */
    public static final Map<String, BlockPos> LEIDERS = new LinkedHashMap<>();

    /** A safe spot (city, camp): creatures forget their fight there and never spawn. */
    public record Veilig(String naam, BlockPos centrum, int straal) {
    }

    public static final List<Veilig> VEILIG = new ArrayList<>();

    // ---- bosses, dungeon, cave ------------------------------------------------------------------------------------------------
    public static BlockPos WORMELIEN, WORMELIEN_DEUR, WORMELIEN_BORD;
    public static final int WORMELIEN_STRAAL = 26;
    public static BlockPos ZOUTKOLOS, ZOUTKOLOS_DEUR, ZOUTKOLOS_BORD;
    public static final int ZOUTKOLOS_STRAAL = 22;

    /** The dungeon: the hatch on the surface (the ruin dome) and the rooms below (this slice builds them, {@link Z6Bouw}). */
    public static BlockPos STERRENWACHT_INGANG;
    /** Floor height of the dungeon rooms. */
    public static final int KERKER_Y = 30;
    public static final BlockPos KERKER_AANKOMST = new BlockPos(3943, KERKER_Y, -971);
    public static final BlockPos KERKER_UITGANG = new BlockPos(4061, KERKER_Y, -971);
    public static final BlockPos INGANGSHAL = new BlockPos(3947, KERKER_Y, -971);
    public static final BlockPos RINGENZAAL = new BlockPos(3972, KERKER_Y, -971);
    public static final int RINGEN_STRAAL = 9;
    public static final BlockPos KAARTENZAAL = new BlockPos(3998, KERKER_Y, -971);
    public static final BlockPos VUURSCHALENZAAL = new BlockPos(4022, KERKER_Y, -971);
    public static final int VUUR_HALF = 9;
    public static final BlockPos KOEPEL = new BlockPos(4049, KERKER_Y, -971);
    public static final int KOEPEL_STRAAL = 11;

    /** De Zingende Zoutgrot: where the elite lives (the cave itself is the map's). */
    public static BlockPos ZOUTGROT_ELITE;

    static {
        // De Roze Duinen (south-west, 60-62)
        r("duinen", 1, 3480, 78, -560, 10);
        r("duinen", 2, 3560, 80, -600, 10);
        r("duinen", 3, 3420, 76, -520, 9);
        r("duinen", 4, 3610, 78, -520, 10);
        // De Palmenkloven (the river gorge to the sea, north-west, 62-64)
        r("palmenkloven", 1, 3470, 72, -905, 9);
        r("palmenkloven", 2, 3540, 74, -930, 9);
        r("palmenkloven", 3, 3605, 78, -885, 9);
        // De Zoutvlakte (south, 64-66)
        r("zoutvlakte", 1, 3760, 70, -560, 11);
        r("zoutvlakte", 2, 3845, 70, -580, 11);
        r("zoutvlakte", 3, 3800, 70, -510, 10);
        ZOUT_NACHT = w("z6w_zout_nacht", 3820, 70, -615);
        // Het Ruïnedal (north-east, 66-68)
        r("ruinedal", 1, 3940, 86, -925, 10);
        r("ruinedal", 2, 3990, 84, -900, 10);
        r("ruinedal", 3, 4035, 86, -945, 9);
        // De Moederoase (east, around the lake, 68-70)
        r("moederoase", 1, 4055, 66, -800, 9);
        r("moederoase", 2, 4125, 66, -770, 8);
        r("moederoase", 3, 4075, 66, -715, 9);
        // the leaders (one per area)
        LEIDERS.put("oorschelp", w("z6w_leider_oorschelp", 3590, 80, -560));
        LEIDERS.put("pitje", w("z6w_leider_pitje", 3575, 76, -905));
        LEIDERS.put("korstje", w("z6w_leider_korstje", 3790, 70, -585));
        LEIDERS.put("oudstewachter", w("z6w_leider_oudstewachter", 3965, 86, -945));
        LEIDERS.put("brie", w("z6w_leider_brie", 4095, 66, -735));
        // safe: the two cities, the caravan camp, the foot of the raid tower
        VEILIG.add(new Veilig("rozenkloof", w("z6w_veilig_rozenkloof", 3357, 100, -740), 128));
        VEILIG.add(new Veilig("saffraanpoort", w("z6w_veilig_saffraanpoort", 3762, 70, -846), 78));
        VEILIG.add(new Veilig("karavaankamp", w("z6w_veilig_karavaankamp", 3720, 70, -620), 20));
        VEILIG.add(new Veilig("toren", w("z6w_veilig_toren", 4140, 70, -688), 22));
        // the field bosses
        WORMELIEN = w("z6w_wormelien", 3520, 78, -545);
        WORMELIEN_DEUR = w("z6w_wormelien_deur", 3520, 78, -575);
        WORMELIEN_BORD = w("z6w_wormelien_bord", 3518, 78, -577);
        ZOUTKOLOS = w("z6w_zoutkolos", 3880, 70, -560);
        ZOUTKOLOS_DEUR = w("z6w_zoutkolos_deur", 3880, 70, -586);
        ZOUTKOLOS_BORD = w("z6w_zoutkolos_bord", 3878, 70, -588);
        // the dungeon and the cave
        STERRENWACHT_INGANG = w("z6w_sterrenwacht_ingang", 3985, 84, -962);
        ZOUTGROT_ELITE = w("z6w_zoutgrot_elite", 3874, 40, -505);
    }

    /** A safe spot (city, camp, tower foot)? */
    public static boolean veilig(double x, double z) {
        if (!inZone(x, z)) {
            return false;
        }
        for (Veilig v : VEILIG) {
            double dx = x - v.centrum().getX(), dz = z - v.centrum().getZ();
            if (dx * dx + dz * dz <= (double) v.straal() * v.straal()) {
                return true;
            }
        }
        return false;
    }

    /** Is (x, y, z) inside the dungeon rooms? */
    public static boolean inKerker(double x, double y, double z) {
        return x >= 3936 && x <= 4064 && z >= -986 && z <= -956 && y >= KERKER_Y - 2 && y <= KERKER_Y + 16;
    }

    /** A tp spot per area (dev command): the first spawn spot of it, plus the special places. */
    public static Map<String, BlockPos> tpPlekken() {
        Map<String, BlockPos> m = new LinkedHashMap<>();
        for (Regio r : REGIOS) {
            m.putIfAbsent(r.gebied(), r.centrum().above(2));
        }
        m.put("zoutnacht", ZOUT_NACHT.above(2));
        m.put("wormelien", WORMELIEN_DEUR.above(1));
        m.put("zoutkolos", ZOUTKOLOS_DEUR.above(1));
        m.put("sterrenwacht", STERRENWACHT_INGANG.above(1));
        m.put("kerker", KERKER_AANKOMST.above(0));
        m.put("ringenzaal", RINGENZAAL.offset(-7, 0, 0));
        m.put("vuurschalenzaal", VUURSCHALENZAAL.offset(-7, 0, 0));
        m.put("koepel", KOEPEL.offset(-9, 0, 0));
        m.put("zoutgrot", ZOUTGROT_ELITE.offset(6, 1, 0));
        return m;
    }
}
