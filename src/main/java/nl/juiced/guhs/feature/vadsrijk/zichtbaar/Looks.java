package nl.juiced.guhs.feature.vadsrijk.zichtbaar;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * U-ZICHTBAAR, common (no client classes, so the gametests can check it): which look a worn piece gets, which pieces are drawn at
 * which distance, and when the stat cape gives way to the wardrobe.
 * <p>The ids of the pieces with their OWN look match {@code tools/features/vadsrijk_u_zichtbaar.py} (KAPEN / HANDSCHOENEN /
 * ARMBANDEN); every other piece of that slot gets the "standaard" look. Rings, necklaces and earrings are one generic model each
 * (gold + a gem); the gem colour comes from {@link #kleur} (keywords in the item id).
 */
public final class Looks {
    /** The order of the synced list = {@link Slot#SIERADEN} (RING, RING2, KETTING, OORBEL, ARMBAND, HANDSCHOENEN, CAPE). */
    public static final List<Slot> VOLGORDE = List.of(Slot.SIERADEN);

    public static final Set<String> KAPEN = Set.of("knuffelcapeje", "sporencape", "galmcape", "buitcape", "wortelcape", "schemercape",
            "lampioncape", "vliegercape", "wolkwachtcape");
    public static final Set<String> HANDSCHOENEN = Set.of("pluiswantjes", "kloddergrijpers", "ijswantjes", "kristalhandschoenen",
            "bloesemhandjes", "schorshandschoenen", "schelpenwanten", "koraalwantjes", "wolkwachthandschoenen");
    public static final Set<String> ARMBANDEN = Set.of("pluisarmband", "rozenarmbandje", "kristalarmband", "honingarmband", "harsarmband",
            "zeeglasarmband", "stormarmband");

    /** The drawn parts (the cape goes through the vanilla cape layer, the rest through ZichtbaarLaag). */
    public enum Deel {
        CAPE, HANDSCHOENEN, ARMBAND, KETTING, RING, OORBEL
    }

    /** LOD (blocks): up to DICHT everything, up to MIDDEN no rings/earrings, up to VER only the gloves; the cape always. */
    public static final double DICHT = 16, MIDDEN = 32, VER = 48;
    /** FPS budget: at most this many OTHER players per frame get the small parts (gloves/jewellery); you always do. */
    public static final int BUDGET = 12;

    private static final String[][] KLEUREN = {
            {"eierwarmer", "FFE070"}, {"roze", "FF5FA8"}, {"knuffel", "FF7FC0"}, {"pluis", "F4F0FF"}, {"kristal", "6AD8FF"},
            {"spiegel", "D8E8F8"}, {"galm", "9A6AE8"}, {"honing", "FFB020"}, {"hars", "E88A2A"}, {"sporen", "7ACA4A"},
            {"parel", "FFF0F4"}, {"schelp", "FFC8A8"}, {"lagune", "3AD8C8"}, {"lampion", "FF8A3A"}, {"regendruppel", "5AB8FF"},
            {"walvis", "4A7ADC"}, {"wolk", "C8E8FF"}, {"storm", "FFE04A"}, {"zeeglas", "6ADCC0"}, {"bloesem", "FF9AD0"},
            {"schors", "8A6A40"}, {"wortel", "6ABA4A"}, {"schemer", "7A64C8"}};
    /** A ruby, for a piece without a known keyword. */
    public static final int STANDAARD_KLEUR = 0xE8507A;

    private Looks() {
    }

    /** "guhs:vr_t_sporencape" -> "sporencape"; also strips vr_z4_mob_ / vr_z3_baas_ / vr_ and the like. */
    public static String sleutel(String itemId) {
        String s = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        int c = s.indexOf(':');
        if (c >= 0) {
            s = s.substring(c + 1);
        }
        return s.replaceFirst("^vr_(t_|z\\d+_(mob_|baas_)?)?", "");
    }

    /** The cape texture (path under assets/guhs/) for a worn cape item. */
    public static String kaapTextuur(String itemId) {
        String k = sleutel(itemId);
        return "textures/vadsrijk/zichtbaar/cape_" + (KAPEN.contains(k) ? k : "standaard") + ".png";
    }

    /** The item model (guhs:...) for a glove / armband / the generic jewellery of slot {@code s}. */
    public static String model(Slot s, String itemId) {
        String k = sleutel(itemId);
        return switch (s) {
            case HANDSCHOENEN -> "vr_u_zichtbaar_handschoen_" + (HANDSCHOENEN.contains(k) ? k : "standaard");
            case ARMBAND -> "vr_u_zichtbaar_armband_" + (ARMBANDEN.contains(k) ? k : "standaard");
            case KETTING -> "vr_u_zichtbaar_ketting_standaard";
            case OORBEL -> "vr_u_zichtbaar_oorbel_standaard";
            case RING, RING2 -> "vr_u_zichtbaar_ring_standaard";
            default -> "";
        };
    }

    /** The gem colour (0xRRGGBB) of a piece of jewellery: the first keyword in its id, else a ruby. */
    public static int kleur(String itemId) {
        String k = sleutel(itemId);
        for (String[] e : KLEUREN) {
            if (k.contains(e[0])) {
                return Integer.parseInt(e[1], 16);
            }
        }
        return STANDAARD_KLEUR;
    }

    /**
     * T-KLEDING's rule (NOTITIES_T_KLEDING "For integration"): the wardrobe's cosmetic cape replaces the drawn cape and wings
     * hide it (wings and capes share the back). So the stat cape shows only when it is worn and neither is on.
     */
    public static boolean statCapeTonen(boolean statCape, boolean kosmetischeKaap, boolean vleugels) {
        return statCape && !kosmetischeKaap && !vleugels;
    }

    /** May this part be drawn at this squared distance from the camera (LOD)? */
    public static boolean teken(Deel d, double afstandKwadraat) {
        double max = switch (d) {
            case CAPE -> Double.MAX_VALUE;
            case HANDSCHOENEN -> VER;
            case ARMBAND, KETTING -> MIDDEN;
            case RING, OORBEL -> DICHT;
        };
        return max == Double.MAX_VALUE || afstandKwadraat <= max * max;
    }

    /** The index of a slot in the synced list. */
    public static int index(Slot s) {
        return VOLGORDE.indexOf(s);
    }

    /** The worn item id of slot {@code s} in a synced list ("" = nothing / not in the list). */
    public static String van(List<String> looks, Slot s) {
        int i = index(s);
        return looks == null || i < 0 || i >= looks.size() ? "" : looks.get(i);
    }
}
