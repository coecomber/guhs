package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Where the zone-4 creatures live (De Lampionkust, x 1740..2540, z -716..-266; the sea surface is y 63).
 * <p><b>PLACEHOLDERS</b> until t-z4wereld's POI_Z4 exists: the spots below come from the concept renders
 * ({@code renders-lampionkust/_bron/kust.py}: the island centres + {@code kust.hoogte(x, z)} for the ground). When
 * {@code zone4.wereld.Z4WereldPlekken} is on the classpath, its {@code plek(String id)} is asked by REFLECTION (no compile
 * dependency) for every id here and wins (ids: {@code z4_mobs_<eiland>_<n>}; see NOTITIES_T_Z4WEZENS "POI-verzoeken").
 * <p>Light combat: few spots, small circles, and NO creatures on Lampionhaven, the Knuffeltij-strand, the Strandfeesteiland,
 * the Vuurtoreneiland (the old keeper), the Schildpadeiland or the Pierkop (all safe spots). The islands with creatures follow
 * t-z4wereld's MOBZONES: Koraalbaai, Vliegereiland, Bloemeneiland (outer fields only), Mysterie-eiland and the Wrak.
 */
public final class Z4WezensPlekken {
    public static final int MIN_X = 1740, MAX_X = 2540, MIN_Z = -716, MAX_Z = -266, ZEE = 63;

    /** A spawn region: id, centre (ground), radius, area (for the level band + mix), level band, cave window (MIN_VALUE = open air). */
    public record Regio(String id, BlockPos centrum, int straal, String eiland, int minLevel, int maxLevel, int minY, int maxY) {
        public boolean grot() {
            return minY != Integer.MIN_VALUE;
        }
    }

    /** A safe spot (village, party, the pier): x, z, radius. */
    public record Veilig(String naam, int x, int z, int straal) {
    }

    public static final List<Veilig> VEILIG = List.of(
            new Veilig("lampionhaven", 1850, -470, 88),
            new Veilig("knuffeltij_strand", 1935, -345, 66),
            new Veilig("strandfeesteiland", 1995, -605, 56),
            new Veilig("vuurtoreneiland", 2400, -500, 34),
            new Veilig("schildpadeiland", 2290, -420, 30),
            new Veilig("pierkop", 2470, -565, 30));

    /** id -> placeholder ground spot (x, y, z) + radius [+ cave window minY, maxY]. Matches t-z4wereld's MOBZONES (POI_Z4 v2). */
    private static final Map<String, int[]> STANDAARD = new LinkedHashMap<>();

    static {
        // De Koraalbaai: the atoll ring around the pink lagoon (mob zone z4_koraalbaai, 53-54)
        STANDAARD.put("z4_mobs_koraalbaai_1", new int[] {2185, 70, -618, 7});
        STANDAARD.put("z4_mobs_koraalbaai_2", new int[] {2238, 66, -570, 7});
        STANDAARD.put("z4_mobs_koraalbaai_3", new int[] {2132, 69, -560, 6});
        STANDAARD.put("z4_mobs_koraalbaai_4", new int[] {2222, 68, -608, 6});
        STANDAARD.put("z4_mobs_koraalbaai_5", new int[] {2222, 69, -525, 6});
        // Vliegereiland: the windy hill (z4_vliegereiland, 52-53)
        STANDAARD.put("z4_mobs_vliegereiland_1", new int[] {2300, 79, -650, 8});
        STANDAARD.put("z4_mobs_vliegereiland_2", new int[] {2325, 71, -670, 7});
        STANDAARD.put("z4_mobs_vliegereiland_3", new int[] {2275, 65, -660, 6});
        // Bloemeneiland: only the outer flower fields, away from the bee farm and the picnic (z4_bloemeneiland, 51-52)
        STANDAARD.put("z4_mobs_bloemeneiland_1", new int[] {2062, 70, -380, 6});
        STANDAARD.put("z4_mobs_bloemeneiland_2", new int[] {2112, 70, -425, 6});
        // Mysterie-eiland: the grey mist in the south-east (z4_mysterie, 55-56)
        STANDAARD.put("z4_mobs_mysterie_1", new int[] {2480, 78, -330, 8});
        STANDAARD.put("z4_mobs_mysterie_2", new int[] {2495, 77, -335, 6});
        STANDAARD.put("z4_mobs_mysterie_3", new int[] {2462, 76, -325, 6});
        // a few rocks and sandbanks along the route (50-52): one small group each
        STANDAARD.put("z4_mobs_rotsjes_1", new int[] {2350, 69, -585, 4});
        STANDAARD.put("z4_mobs_rotsjes_2", new int[] {2240, 63, -640, 5});
        STANDAARD.put("z4_mobs_rotsjes_3", new int[] {1790, 68, -610, 4});
        // Het Wrak van de Vliegervanger (z4_wrak, 54-55): the 3 rooms below deck (cave mode) and the deck before the fore deck
        STANDAARD.put("z4_mobs_wrak_1", new int[] {2175, 57, -700, 3, 54, 62});   // het ruim
        STANDAARD.put("z4_mobs_wrak_2", new int[] {2165, 57, -700, 3, 54, 62});   // de kombuis
        STANDAARD.put("z4_mobs_wrak_3", new int[] {2155, 57, -700, 2, 54, 62});   // bij de knuffelkooi
        STANDAARD.put("z4_mobs_wrak_4", new int[] {2172, 67, -701, 3, 65, 72});   // het dek
    }

    /** POI ids of t-z4wereld whose spot replaces a placeholder (the wreck rooms). */
    private static final Map<String, String> POI_ALIAS = Map.of("z4_mobs_wrak_1", "wrak_ruim", "z4_mobs_wrak_2", "wrak_kombuis", "z4_mobs_wrak_3",
            "wrak_knuffelkooi", "z4_mobs_wrak_4", "wrak_ingang");

    private static final Map<String, int[]> LEVELS = Map.of("koraalbaai", new int[] {53, 54}, "vliegereiland", new int[] {52, 53},
            "bloemeneiland", new int[] {51, 52}, "mysterie", new int[] {55, 56}, "rotsjes", new int[] {50, 52}, "wrak", new int[] {54, 55});

    private Z4WezensPlekken() {
    }

    /** True while the spots come from t-z4wereld's POI (reflection), false = placeholders. */
    public static boolean echt;

    @Nullable
    private static Method wereldPlek;
    private static boolean gezocht;

    /** t-z4wereld's live spot for an id (reflection on zone4.wereld.Z4WereldPlekken.plek(String)), or null. */
    @Nullable
    public static BlockPos wereld(String id) {
        if (!gezocht) {
            gezocht = true;
            try {
                Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken");
                wereldPlek = c.getMethod("plek", String.class);
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
            // fall back
        }
        return null;
    }

    /** All spawn regions (the live POI spot when there is one, else the placeholder). */
    public static List<Regio> regios() {
        List<Regio> l = new java.util.ArrayList<>();
        boolean allesEcht = true;
        for (var e : STANDAARD.entrySet()) {
            int[] s = e.getValue();
            BlockPos w = wereld(e.getKey());
            if (w == null && POI_ALIAS.containsKey(e.getKey())) {
                w = wereld(POI_ALIAS.get(e.getKey()));
            }
            allesEcht &= w != null || wereldPlek != null;
            BlockPos c = w != null ? w : new BlockPos(s[0], s[1], s[2]);
            String eiland = e.getKey().substring("z4_mobs_".length(), e.getKey().lastIndexOf('_'));
            int[] lv = LEVELS.get(eiland);
            l.add(new Regio(e.getKey(), c, s[3], eiland, lv[0], lv[1], s.length > 5 ? s[4] : Integer.MIN_VALUE, s.length > 5 ? s[5] : Integer.MAX_VALUE));
        }
        echt = allesEcht;
        return l;
    }

    /** t-z4wereld's campfires (Z4WereldPlekken.KAMPVUREN by reflection), or none. */
    @SuppressWarnings("unchecked")
    public static List<BlockPos> kampvuren() {
        try {
            Object o = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken").getField("KAMPVUREN").get(null);
            if (o instanceof List<?> l) {
                return (List<BlockPos>) l;
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            // not there yet
        }
        return List.of();
    }

    /** Is (x, z) in zone 4's box? */
    public static boolean inZone(double x, double z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    /** A safe spot (village, party island, the pier): creatures forget their fight there. */
    public static boolean veilig(double x, double y, double z) {
        if (!inZone(x, z)) {
            return false;
        }
        for (Veilig v : VEILIG) {
            double dx = x - v.x(), dz = z - v.z();
            if (dx * dx + dz * dz <= (double) v.straal() * v.straal()) {
                return true;
            }
        }
        return false;
    }

    /** The island name for a spot (dev command / tp), or null. */
    @Nullable
    public static String eilandBij(double x, double z) {
        String best = null;
        double bd = 70 * 70;
        for (Regio r : regios()) {
            double d = r.centrum().distToCenterSqr(x, r.centrum().getY(), z);
            if (d < bd) {
                bd = d;
                best = r.eiland();
            }
        }
        return best;
    }

    /** A tp spot per island for the dev command (the first region of it). */
    public static Map<String, BlockPos> eilanden() {
        Map<String, BlockPos> m = new LinkedHashMap<>();
        for (Regio r : regios()) {
            m.putIfAbsent(r.eiland(), r.centrum().above(2));
        }
        return m;
    }
}
