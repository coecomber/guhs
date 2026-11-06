package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * T-KLEDING: the catalogue of everything in the Kledingkast (VOORSTEL_ROADMAP §4.2-4.4, R3/R4/R5). Every cosmetic has an id
 * {@code <soort>:<naam>} (that is also its unlock key in the account store), a kind, a source ({@link #bron}, see {@link Bronnen})
 * and, for wings, its one MOVEMENT trick ({@link Kracht}; never combat stats, never rising).
 * <p>Lang: {@code vadsrijk.kleding.ding.<soort>.<naam>} (name) + {@code .bron} (how to earn it). Models/textures:
 * tools/features/vadsrijk_t_kleding.py (same ids).
 */
public final class Cosmetica {
    /** The kinds; each kind has ONE active choice (wings and capes share the back slot). */
    public enum Soort {
        VLEUGEL("vleugel"), KAAP("kaap"), ACCESSOIRE("accessoire"), NAAMKLEUR("naamkleur"), VERF("verf"), PLOF("plof"), SPOOR("spoor"),
        AURA("aura");

        public final String id;

        Soort(String id) {
            this.id = id;
        }

        /** The choice key in the account store ("rug" for wings + capes). */
        public String keuze() {
            return this == VLEUGEL || this == KAAP ? "rug" : this == ACCESSOIRE ? "hoofd" : id;
        }

        @Nullable
        public static Soort van(String id) {
            for (Soort s : values()) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
            return null;
        }
    }

    /** The one small MOVEMENT effect of a wing (R4). Every wing glides once gliding is unlocked (the Vahoegveertjes). */
    public enum Kracht {
        /** Just gliding: hold space in the air, you sink slowly. */
        ZWEVEN,
        /** Tap space in the air: one extra little jump per flight. */
        DUBBELE_SPRONG,
        /** Glide a bit faster forward. */
        SNELLER_ZWEVEN,
        /** Sink even slower while gliding. */
        LANGER_ZWEVEN,
        /** Tap space in the air: one short forward dash per flight. */
        LUCHTDASH,
        /** Never fall damage with these on, and a pink poof when you land. */
        ZACHTE_LANDING
    }

    /**
     * One cosmetic.
     *
     * @param id     "soort:naam"
     * @param bron   the source spec (Bronnen): "start", "binnen", "verhaal:vahoegnir", "vlag:zone1_klaar", "boek:zone2:100", "level:20",
     *               "veren:25", "kills:100", "vahoeg", "vahoegkill", "geheim:&lt;id&gt;", "alleboeken", "later:&lt;wat&gt;"
     * @param kracht wings only
     * @param kleur  name colours / dyes: the RGB; else 0
     * @param zone   the zone it belongs to for sorting (0 = general)
     */
    public record Ding(String id, Soort soort, String bron, @Nullable Kracht kracht, int kleur, int zone) {
        public String naam() {
            return id.substring(id.indexOf(':') + 1);
        }

        public String naamKey() {
            return "vadsrijk.kleding.ding." + soort.id + "." + naam();
        }

        public String bronKey() {
            return naamKey() + ".bron";
        }

        /** Not obtainable yet (a later zone). */
        public boolean later() {
            return bron.startsWith("later:");
        }

        public boolean geheim() {
            return bron.startsWith("geheim:") || bron.startsWith("puzzel:");
        }
    }

    private static final Map<String, Ding> ALLE = Collections.synchronizedMap(new LinkedHashMap<>());

    // ---- wings (rug) -------------------------------------------------------------------------------------------------------------
    public static final Ding VAHOEGVEERTJES = vleugel("vahoegveertjes", "verhaal:vahoegnir", Kracht.ZWEVEN, 3);
    public static final Ding KRISTAL = vleugel("kristal", "boek:zone2:100", Kracht.DUBBELE_SPRONG, 2);
    public static final Ding HONINGBIJ = vleugel("honingbij", "boek:zone3:100", Kracht.SNELLER_ZWEVEN, 3);
    public static final Ding GOUDVEER = vleugel("goudveer", "veren:25", Kracht.LANGER_ZWEVEN, 3);
    public static final Ding SCHEMER = vleugel("schemer", "vahoeg", Kracht.LUCHTDASH, 3);
    public static final Ding PLUIS = vleugel("pluis", "geheim:zweefvlucht", Kracht.ZACHTE_LANDING, 0);
    public static final Ding LAMPION = vleugel("lampion", "boek:zone4:100", Kracht.LANGER_ZWEVEN, 4);   // (T-INT: the zone-4 Verkenningsboek of t-z4verhaal)
    // (t-puzzels: puzzle rewards; source "puzzel:<t-puzzels unlock id>", see Bronnen + KledingRegistry.puzzels)
    public static final Ding STERRENSTOF = vleugel("sterrenstof", "puzzel:pz_vleugels_sterrenstof", Kracht.DUBBELE_SPRONG, 0);
    public static final Ding POOLLICHT = vleugel("poollicht", "puzzel:pz_vleugels_poollicht", Kracht.LANGER_ZWEVEN, 0);
    public static final Ding LANTAARNMOT = vleugel("lantaarnmot", "puzzel:pz_vleugels_lantaarnmot", Kracht.ZACHTE_LANDING, 0);

    // ---- cosmetic capes (rug, over the gear cape of t-items) ------------------------------------------------------------------
    public static final Ding KAAP_KNUFFELORDE = ding("kaap:knuffelorde", Soort.KAAP, "vlag:zone1_klaar", 0, 1);
    public static final Ding KAAP_ROZE = ding("kaap:roze", Soort.KAAP, "boek:zone1:50", 0, 1);
    public static final Ding KAAP_STERREN = ding("kaap:sterren", Soort.KAAP, "boek:zone2:50", 0, 2);
    public static final Ding KAAP_BLOESEM = ding("kaap:bloesem", Soort.KAAP, "boek:zone3:50", 0, 3);
    public static final Ding KAAP_REIS = ding("kaap:reis", Soort.KAAP, "geheim:kleermakers", 0, 0);
    public static final Ding KAAP_SNEEUWVLOK = ding("kaap:sneeuwvlok", Soort.KAAP, "puzzel:pz_cape_sneeuwvlok", 0, 0);
    public static final Ding KAAP_HONINGRAAT = ding("kaap:honingraat", Soort.KAAP, "puzzel:pz_cape_honingraat", 0, 0);
    public static final Ding KAAP_MOS = ding("kaap:mos", Soort.KAAP, "puzzel:pz_cape_mos", 0, 0);

    // ---- head accessories (on top of / instead of nothing; the helmet can be hidden separately) ---------------------------------
    public static final Ding OORTJES = ding("accessoire:oortjes", Soort.ACCESSOIRE, "binnen", 0, 0);
    public static final Ding STRIKJE = ding("accessoire:strikje", Soort.ACCESSOIRE, "boek:zone1:25", 0, 1);
    public static final Ding PADDENHOEDJE = ding("accessoire:paddenhoedje", Soort.ACCESSOIRE, "boek:zone1:75", 0, 1);
    public static final Ding KRISTALTIARA = ding("accessoire:kristaltiara", Soort.ACCESSOIRE, "vlag:zone2_klaar", 0, 2);
    public static final Ding HONINGKROON = ding("accessoire:honingkroon", Soort.ACCESSOIRE, "boek:zone3:25", 0, 3);
    public static final Ding FEESTMUTS = ding("accessoire:feestmuts", Soort.ACCESSOIRE, "level:20", 0, 0);
    public static final Ding LAMPIONSPELD = ding("accessoire:lampionspeld", Soort.ACCESSOIRE, "boek:zone4:50", 0, 4);   // (T-INT: the zone-4 Verkenningsboek)

    // ---- name colours ------------------------------------------------------------------------------------------------------------
    public static final Ding NAAM_ROZE = ding("naamkleur:roze", Soort.NAAMKLEUR, "vlag:zone1_klaar", 0xFF8CC8, 1);
    public static final Ding NAAM_IJS = ding("naamkleur:ijsblauw", Soort.NAAMKLEUR, "vlag:zone2_klaar", 0x8CDCFF, 2);
    public static final Ding NAAM_HONING = ding("naamkleur:honing", Soort.NAAMKLEUR, "vlag:zone3_klaar", 0xFFC84A, 3);
    public static final Ding NAAM_PAARS = ding("naamkleur:paars", Soort.NAAMKLEUR, "kills:500", 0xC88CFF, 0);
    public static final Ding NAAM_GOUD = ding("naamkleur:goud", Soort.NAAMKLEUR, "alleboeken", 0xFFD700, 0);

    // ---- dye (tints wings + head accessory; capes are painted) -------------------------------------------------------------------
    public static final Ding VERF_ROZE = ding("verf:roze", Soort.VERF, "start", 0xFF9FD0, 0);
    public static final Ding VERF_WIT = ding("verf:wit", Soort.VERF, "start", 0xF4F4F4, 0);
    public static final Ding VERF_GEEL = ding("verf:zachtgeel", Soort.VERF, "boek:zone1:25", 0xFFF0A0, 1);
    public static final Ding VERF_MINT = ding("verf:mint", Soort.VERF, "boek:zone1:75", 0xA8F0C8, 1);
    public static final Ding VERF_IJS = ding("verf:ijsblauw", Soort.VERF, "boek:zone2:25", 0xA8DCFF, 2);
    public static final Ding VERF_LILA = ding("verf:lila", Soort.VERF, "boek:zone2:75", 0xD0B0FF, 2);
    public static final Ding VERF_HONING = ding("verf:honing", Soort.VERF, "boek:zone3:25", 0xFFC860, 3);
    public static final Ding VERF_BLOESEM = ding("verf:bloesem", Soort.VERF, "boek:zone3:75", 0xFFB8C8, 3);

    // ---- cheer-up (kill) effects -------------------------------------------------------------------------------------------------
    public static final Ding PLOF_CONFETTI = ding("plof:confetti", Soort.PLOF, "start", 0, 0);
    public static final Ding PLOF_REGENBOOG = ding("plof:regenboog", Soort.PLOF, "boek:zone1:100", 0, 1);
    public static final Ding PLOF_HARTJES = ding("plof:hartjes", Soort.PLOF, "boek:zone2:75", 0, 2);
    public static final Ding PLOF_BLOEMEN = ding("plof:bloemen", Soort.PLOF, "boek:zone3:25", 0, 3);
    public static final Ding PLOF_KNUFFELS = ding("plof:knuffels", Soort.PLOF, "vahoegkill", 0, 3);
    public static final Ding PLOF_GIECHEL = ding("plof:giechel", Soort.PLOF, "kills:100", 0, 0);
    public static final Ding PLOF_NJEG = ding("plof:njeg", Soort.PLOF, "kills:1000", 0, 0);

    // ---- trails ------------------------------------------------------------------------------------------------------------------
    public static final Ding SPOOR_BLAADJES = ding("spoor:bloemblaadjes", Soort.SPOOR, "boek:zone1:50", 0, 1);
    public static final Ding SPOOR_SNEEUW = ding("spoor:sneeuwvlokjes", Soort.SPOOR, "boek:zone2:50", 0, 2);
    public static final Ding SPOOR_STERREN = ding("spoor:sterretjes", Soort.SPOOR, "boek:zone2:100", 0, 2);
    public static final Ding SPOOR_HONING = ding("spoor:honingdruppels", Soort.SPOOR, "boek:zone3:50", 0, 3);
    public static final Ding SPOOR_NOTEN = ding("spoor:muzieknootjes", Soort.SPOOR, "level:30", 0, 0);

    // ---- auras (standing still > 3 s) --------------------------------------------------------------------------------------------
    public static final Ding AURA_GLOED = ding("aura:rozegloed", Soort.AURA, "boek:zone1:75", 0, 1);
    public static final Ding AURA_PLUISJES = ding("aura:pluisjes", Soort.AURA, "boek:zone2:25", 0, 2);
    public static final Ding AURA_VUURVLIEGJES = ding("aura:vuurvliegjes", Soort.AURA, "boek:zone3:75", 0, 3);

    private Cosmetica() {
    }

    private static Ding vleugel(String naam, String bron, Kracht k, int zone) {
        return registreer(new Ding("vleugel:" + naam, Soort.VLEUGEL, bron, k, 0, zone));
    }

    private static Ding ding(String id, Soort s, String bron, int kleur, int zone) {
        return registreer(new Ding(id, s, bron, null, kleur, zone));
    }

    /** Another slice may add a cosmetic (its lang + model are its own). */
    public static Ding registreer(Ding d) {
        ALLE.put(d.id(), d);
        return d;
    }

    @Nullable
    public static Ding van(@Nullable String id) {
        return id == null ? null : ALLE.get(id);
    }

    public static List<Ding> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.values());
        }
    }

    public static List<Ding> van(Soort s) {
        List<Ding> l = new ArrayList<>();
        for (Ding d : alle()) {
            if (d.soort() == s) {
                l.add(d);
            }
        }
        return l;
    }

    /** The wing's trick, or null for no wing / not a wing. */
    @Nullable
    public static Kracht kracht(@Nullable String rug) {
        Ding d = van(rug);
        return d == null ? null : d.kracht();
    }
}
