package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;

/**
 * T-RIJDIER (roadmap §4.1): the looks of the land mounts, separate from their stats (a variant never changes speed or jump). Three
 * axes ("as"):
 * <ul>
 *   <li>{@link As#VACHT}: the coat colour of one species (a texture per colour, {@code textures/entity/vr_eco_<soort>_<vacht>.png};
 *       "naturel" = the normal texture). The Pluimguh keeps its own colour enum ({@code RijdierSoort.PluimKleur}), so it has no
 *       vacht here;</li>
 *   <li>{@link As#PATROON}: a pattern drawn over the coat by a second render pass (spots, stripes, stars, hearts, glitter;
 *       {@code textures/entity/vr_rijpatroon_<soort>_<patroon>.png}, generated per species). For every land mount;</li>
 *   <li>{@link As#TUIG}: tack: small 3D bits on the head, the neck or the back (a blanket, a flower crown, bells, a hat, lanterns that
 *       glow, a crown), drawn as item models on the mount's bones ({@code client.UiterlijkLaag}). For every land mount.</li>
 * </ul>
 * Patterns and tack are a wardrobe for ALL your mounts (unlock once, wear on any); coats belong to one species. Every variant has a
 * source ({@link Bron}); {@code Aankleden} keeps who owns what and checks the achievement/boss/secret sources.
 * <p>Common code (the client screen reads names and swatches from here). Other slices add variants with {@link #registreer}.
 */
public final class Uiterlijk {
    public enum As {
        VACHT, PATROON, TUIG;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Where a variant comes from.
     *
     * @param soort   "start" (owned with the mount), "stal" (buy at the stal for {@code waarde} brons), "veren" (trade {@code waarde}
     *                Gouden Veren, in the mount screen), "prestatie" (an achievement {@code sleutel}, see {@link Prestaties}),
     *                "baas" (the first kill of entity type {@code sleutel}), "geheim" (secrets found, see {@link Prestaties}),
     *                "api" (given by another slice's code, e.g. a zone-4 quest)
     */
    public record Bron(String soort, long waarde, String sleutel) {
        public static final Bron START = new Bron("start", 0, "");

        public static Bron stal(long brons) {
            return new Bron("stal", brons, "");
        }

        public static Bron veren(int n) {
            return new Bron("veren", n, "");
        }

        public static Bron prestatie(String id) {
            return new Bron("prestatie", 0, id);
        }

        public static Bron baas(String type) {
            return new Bron("baas", 0, type);
        }

        public static Bron geheim(String id) {
            return new Bron("geheim", 0, id);
        }

        public static Bron api(String id) {
            return new Bron("api", 0, id);
        }

        /** Checked by {@code Aankleden.inhalen} (achievements, bosses, secrets). */
        public boolean vanzelf() {
            return soort.equals("prestatie") || soort.equals("baas") || soort.equals("geheim");
        }

        /** The lang key of the "how to get it" line; {@code vadsrijk.rijdier.bron.<soort>[.<sleutel>]}. */
        public String tekstKey() {
            return switch (soort) {
                case "prestatie", "geheim", "api" -> "vadsrijk.rijdier.bron." + soort + "." + sleutel;
                case "baas" -> "vadsrijk.rijdier.bron.baas";
                default -> "vadsrijk.rijdier.bron." + soort;
            };
        }
    }

    /**
     * One variant.
     *
     * @param soort the species (VACHT only; null for patterns and tack: every land mount)
     * @param kleur the swatch in the screen (ARGB); for a pattern also its tint basis
     */
    public record Variant(As as, String id, @Nullable RijdierSoort soort, int kleur, Bron bron) {
        /** The id stored per player: "vacht:knuffelhert:sneeuw", "patroon:sterretjes", "tuig:hoedje". */
        public String sleutel() {
            return as.id() + ":" + (soort == null ? "" : soort.id() + ":") + id;
        }

        public String naamKey() {
            return soort == null ? "vadsrijk.rijdier." + as.id() + "." + id : "vadsrijk.rijdier.vacht." + soort.id() + "." + id;
        }

        public boolean voor(RijdierSoort s) {
            return soort == null ? !ZONDER_GARDEROBE.contains(s) : soort == s;
        }
    }

    public static final String NATUREL = "naturel";
    /**
     * Species that don't wear patterns and tack (e.g. a water mount such as t-z4boot's sailboat: call {@link #zonderGarderobe} from
     * its registry). Their own coats ({@link As#VACHT} with that species) still work.
     */
    private static final java.util.Set<RijdierSoort> ZONDER_GARDEROBE = java.util.Collections.synchronizedSet(java.util.EnumSet.noneOf(RijdierSoort.class));

    public static void zonderGarderobe(RijdierSoort s) {
        ZONDER_GARDEROBE.add(s);
    }
    private static final List<Variant> ALLE = Collections.synchronizedList(new ArrayList<>());

    // ---- patterns (every land mount) ------------------------------------------------------------------------------------------------
    public static final Variant VLEKJES = registreer(new Variant(As.PATROON, "vlekjes", null, 0xFFB08A70, Bron.stal(600)));
    public static final Variant STREEPJES = registreer(new Variant(As.PATROON, "streepjes", null, 0xFFF4E6FF, Bron.stal(600)));
    public static final Variant HARTJES = registreer(new Variant(As.PATROON, "hartjes", null, 0xFFFF6FA8, Bron.veren(5)));
    public static final Variant STERRETJES = registreer(new Variant(As.PATROON, "sterretjes", null, 0xFFFFE070, Bron.prestatie("huisdieren_4")));
    public static final Variant GLITTER = registreer(new Variant(As.PATROON, "glitter", null, 0xFFFFFFFF, Bron.baas("guhs:vr_z3_baas_vahoegnir")));

    // ---- tack (every land mount) ----------------------------------------------------------------------------------------------------
    public static final Variant DEKJE = registreer(new Variant(As.TUIG, "dekje", null, 0xFFFF9CC8, Bron.START));
    public static final Variant BLOEMENKRANS = registreer(new Variant(As.TUIG, "bloemenkrans", null, 0xFFFFD0E6, Bron.stal(400)));
    public static final Variant BELLETJES = registreer(new Variant(As.TUIG, "belletjes", null, 0xFFFFD040, Bron.stal(500)));
    public static final Variant HOEDJE = registreer(new Variant(As.TUIG, "hoedje", null, 0xFF5A3A78, Bron.geheim("schatten_8")));
    public static final Variant LAMPIONNETJES = registreer(new Variant(As.TUIG, "lampionnetjes", null, 0xFFFF8A3A, Bron.veren(6)));
    public static final Variant KROONTJE = registreer(new Variant(As.TUIG, "kroontje", null, 0xFFFFC830, Bron.prestatie("rijdiermeester")));

    static {
        // ---- coats per species ("naturel" = the normal texture, always yours with the mount) ---------------------------------------
        vacht(RijdierSoort.KNUFFELHERT, 0xFFD8A882, new Object[][] {
                {"sneeuw", 0xFFF2F0F8, Bron.stal(1500)}, {"roze", 0xFFF4AACA, Bron.veren(6)},
                {"honing", 0xFFF0BE5A, Bron.prestatie("lv10_knuffelhert")}, {"nacht", 0xFF5A5096, Bron.geheim("knuffels_20")}});
        vacht(RijdierSoort.GUHWOLF, 0xFFE6E0F0, new Object[][] {
                {"zilver", 0xFFAEB4C2, Bron.stal(1500)}, {"zon", 0xFFF2C878, Bron.veren(6)},
                {"middernacht", 0xFF46467A, Bron.baas("guhs:vr_z2_baas_spiegelzucht")}, {"roze", 0xFFF6B4D2, Bron.prestatie("verkenner_zone2_75")}});
        vacht(RijdierSoort.WOLKENSCHAAPJE, 0xFFF6F2FF, new Object[][] {
                {"suikerspin", 0xFFFFB8DA, Bron.stal(1200)}, {"lavendel", 0xFFC8B4F4, Bron.stal(1200)},
                {"onweer", 0xFF8A8AA6, Bron.baas("guhs:vr_z3_baas_kruinroofvogel")}, {"karamel", 0xFFE6B07A, Bron.veren(5)}});
        vacht(RijdierSoort.SLAKKIE, 0xFFF8AE78, new Object[][] {
                {"mint", 0xFF8CE0BE, Bron.stal(1500)}, {"lavendel", 0xFFB8A0F0, Bron.stal(1500)},
                {"honing", 0xFFF0C060, Bron.prestatie("lv10_slakkie")}, {"nacht", 0xFF4A4A8A, Bron.geheim("schatten_15")}});
        vacht(RijdierSoort.DUINHUPPER, 0xFFE8CC96, new Object[][] {
                {"sneeuw", 0xFFF4F2FA, Bron.stal(1500)}, {"roze", 0xFFF6B0CE, Bron.veren(6)},
                {"nacht", 0xFF52508E, Bron.prestatie("lv10_duinhupper")}});
        vacht(RijdierSoort.WOLKWALKALFJE, 0xFFAED8F6, new Object[][] {
                {"zonsopkomst", 0xFFFFC0A0, Bron.stal(2000)}, {"sterrenhemel", 0xFF3E3C80, Bron.prestatie("huisdieren_6")},
                {"goud", 0xFFFFD86A, Bron.veren(10)}});
        vacht(RijdierSoort.ZWAMZWIJNTJE, 0xFFF6B6C4, new Object[][] {
                {"blauwzwam", 0xFF8CB4F0, Bron.stal(1500)}, {"dooier", 0xFFF6D66A, Bron.prestatie("verkenner_zone1_75")},
                {"glimzwam", 0xFF6A4AA6, Bron.geheim("aanwijzingen_10")}});
        nl.juiced.guhs.feature.vadsrijk.tapijt.Tapijt.registreerUiterlijk();   // U-TAPIJT: the flying carpet's weave / tassels / cushion
    }

    private Uiterlijk() {
    }

    private static void vacht(RijdierSoort s, int naturel, Object[][] rest) {
        registreer(new Variant(As.VACHT, NATUREL, s, naturel, Bron.START));
        for (Object[] r : rest) {
            registreer(new Variant(As.VACHT, (String) r[0], s, (Integer) r[1], (Bron) r[2]));
        }
    }

    /** Adds a variant (other slices: e.g. a zone-4 coat with {@code Bron.api("lampionkust")}). Returns it. */
    public static Variant registreer(Variant v) {
        ALLE.add(v);
        return v;
    }

    public static List<Variant> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE);
        }
    }

    /** The variants of one axis that fit this species, in registration order. */
    public static List<Variant> van(RijdierSoort s, As as) {
        List<Variant> l = new ArrayList<>();
        for (Variant v : alle()) {
            if (v.as() == as && v.voor(s)) {
                l.add(v);
            }
        }
        return l;
    }

    /** A variant by its stored key ("vacht:knuffelhert:sneeuw", "patroon:hartjes", "tuig:hoedje"). */
    @Nullable
    public static Variant vanSleutel(String sleutel) {
        for (Variant v : alle()) {
            if (v.sleutel().equals(sleutel)) {
                return v;
            }
        }
        return null;
    }

    @Nullable
    public static Variant van(RijdierSoort s, As as, String id) {
        for (Variant v : van(s, as)) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return null;
    }

    /** Has the species any looks to change (the land mounts: true; a water mount without variants: false). */
    public static boolean heeftVarianten(RijdierSoort s) {
        return !van(s, As.PATROON).isEmpty() || !van(s, As.VACHT).isEmpty();
    }

    // ---- the synced look string -----------------------------------------------------------------------------------------------------

    /** "vacht|patroon|tuig" (empty parts = plain). */
    public static String maak(String vacht, String patroon, String tuig) {
        return vacht + "|" + patroon + "|" + tuig;
    }

    /** Splits a look string into {vacht, patroon, tuig} (always 3 parts). */
    public static String[] delen(String u) {
        String[] d = (u == null ? "" : u).split("\\|", -1);
        return new String[] {d.length > 0 ? d[0] : "", d.length > 1 ? d[1] : "", d.length > 2 ? d[2] : ""};
    }

    /** The swatch colour of the coat (for tinting a pattern): the vacht's colour, or the species' naturel colour. */
    public static int vachtKleur(RijdierSoort s, String vacht) {
        Variant v = van(s, As.VACHT, vacht.isEmpty() ? NATUREL : vacht);
        if (v == null) {
            v = van(s, As.VACHT, NATUREL);
        }
        return v == null ? 0xFFE0D0E0 : v.kleur();
    }
}
