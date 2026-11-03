package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Getuige;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Kaart;

/**
 * The eleven cases of the Sherlock story (erfenis-voorstellen/sherlock.md, approved in round 16), linear: a case starts only
 * when the one before it is solved AND its fixed moment came. Clue spots are plot tiles (0..63); when something stands
 * there the clue moves to the nearest free grass ({@link Sporen#plek}). The texts: tools/features/erfenis_sherlock.py.
 */
public final class Zaken {
    // glitter colours of the clues
    private static final int ROOD = 0xE8404A, BRUIN = 0x8A5A32, WIT = 0xFFF4E8, GOUD = 0xF2C048, ROZE = 0xF48CB8, GRIJS = 0x9AA0AA,
            GROEN = 0x8CE860, BLAUW = 0x7EC8F0, PAARS = 0xB98CE8;

    private static Kaart spoor(String id, String icoon, int x, int z, int kleur) {
        return new Kaart(id, icoon, true, null, x, z, null, kleur);
    }

    private static Kaart haring(String id, String icoon, String wijstNaar, int x, int z, int kleur) {
        return new Kaart(id, icoon, false, wijstNaar, x, z, null, kleur);
    }

    private static Kaart getuigenis(String id, String getuige) {
        return new Kaart(id, "minecraft:writable_book", true, null, -1, -1, getuige, 0);
    }

    public static final Zaak EIEREN = new Zaak("eieren", 1, v -> v.gebouwd.contains("detectivebureau"),
            List.of(spoor("rood_veertje", "minecraft:red_dye", 12, 38, ROOD),
                    spoor("pootafdrukjes", "minecraft:brown_dye", 16, 44, BRUIN),
                    spoor("hartjesschaal", "minecraft:egg", 10, 46, ROZE),
                    haring("stagiairspasje", "minecraft:name_tag", "pim", 18, 36, GRIJS),
                    getuigenis("bonnie_zuchten", "bonnie")),
            List.of("pim", "eendje", "hendrik"), "hendrik",
            List.of(new Getuige("bonnie", Kind.BOERINNEGUH, 14, 49)), 150, List.of("hartjesnest"));

    public static final Zaak KRUIMELS = new Zaak("kruimels", 2, v -> v.gebouwd.contains("bakkerij"),
            List.of(spoor("kruimelspoor", "minecraft:cookie", 44, 40, GOUD),
                    spoor("handjes_in_meel", "minecraft:wheat", 50, 46, WIT),
                    spoor("theeservies", "minecraft:flower_pot", 54, 42, ROZE),
                    haring("meelzak", "minecraft:bundle", "meelmuts", 46, 50, WIT),
                    getuigenis("meelspoken", "meelmuts")),
            List.of("meelmuts", "pim", "babyguhs"), "babyguhs",
            List.of(new Getuige("meelmuts", Kind.BAKKERGUH, 50, 53)), 250, List.of("theekransje_tafeltje", "recept_theetaartje"));

    public static final Zaak SPOOK = new Zaak("spook", 3, v -> v.hoeve >= 4,
            List.of(spoor("uilenveertjes", "minecraft:feather", 28, 16, WIT),
                    spoor("grijs_dons", "minecraft:gray_wool", 32, 20, GRIJS),
                    spoor("luik_slotje", "minecraft:tripwire_hook", 26, 20, GOUD),
                    haring("schoenafdrukken", "minecraft:leather_boots", "morris", 34, 14, BRUIN),
                    getuigenis("hoe_hoe", "bouwer")),
            List.of("morris", "pim", "spook"), "spook",
            List.of(new Getuige("bouwer", Kind.BOUWVAKKERGUH, 30, 25)), 400, List.of("uilenhuisje"));

    public static final Zaak WOL = new Zaak("wol", 4, v -> v.gebouwd.contains("weverij") && v.bestellingen >= 10,
            List.of(spoor("wolspoor", "minecraft:white_wool", 48, 26, WIT),
                    spoor("breinaalden", "minecraft:stick", 54, 30, PAARS),
                    spoor("krullerig_regeltje", "minecraft:paper", 56, 24, BLAUW),
                    haring("schapenwol_hek", "minecraft:string", "schaapje", 50, 34, WIT),
                    getuigenis("welke_wol", "wolletje")),
            List.of("postguh", "wolletje", "schaapje", "oma"), "oma",
            List.of(new Getuige("wolletje", Kind.STALKNECHTGUH, 45, 30)), 600, List.of("reuzensjaal", "oma_bestelling"));

    public static final Zaak KELDER = new Zaak("kelder", 5, v -> v.hoeve >= 5,
            List.of(spoor("geknabbelde_randjes", "minecraft:paper", 30, 12, WIT),
                    spoor("angora_pluisjes", "minecraft:white_wool", 34, 16, ROZE),
                    spoor("papier_in_ren", "minecraft:book", 26, 16, GOUD),
                    haring("visitekaartje", "minecraft:gray_dye", "morris", 32, 9, GRIJS),
                    getuigenis("knabbel_knabbel", "oehoe")),
            List.of("morris", "pim", "konijntjes"), "konijntjes",
            List.of(new Getuige("oehoe", Kind.OEHOE, 28, 19)), 900, List.of("opas_kelder"));

    public static final Zaak KOEIENBEL = new Zaak("koeienbel", 6, v -> v.gebouwd.contains("muziektent"),
            List.of(spoor("hoefafdrukjes", "minecraft:brown_dye", 10, 22, BRUIN),
                    spoor("bladmuziek", "minecraft:paper", 14, 26, WIT),
                    spoor("emmer_podium", "minecraft:bucket", 8, 28, BLAUW),
                    haring("folders", "minecraft:map", "pim", 16, 20, GRIJS),
                    getuigenis("vals_met_gevoel", "koos")),
            List.of("bertha", "pim", "muzikanten"), "bertha",
            List.of(new Getuige("koos", Kind.KROEGBAASGUH, 12, 31)), 1200, List.of("koeienbel_lantaarn", "bertha_optreden"));

    public static final Zaak PAALTJES = new Zaak("paaltjes", 7, v -> v.hoeve >= 7,
            List.of(spoor("schouderafdrukje", "minecraft:oak_fence", 3, 20, BRUIN),
                    spoor("touw_strikje", "minecraft:lead", 60, 40, ROZE),
                    spoor("moestuinboekje", "minecraft:carrot", 40, 3, GROEN),
                    haring("plattegrond", "minecraft:map", "morris", 20, 60, GRIJS),
                    getuigenis("kleintje_huilde", "oehoe")),
            List.of("morris", "pim", "bouwer"), "pim",
            List.of(new Getuige("oehoe", Kind.OEHOE, 33, 37)), 1800, List.of("moestuintje_pim", "paaltjes_terug"));

    public static final Zaak LICHT = new Zaak("licht", 8, v -> v.gebouwd.contains("kaarsenmakerij"),
            List.of(spoor("glinsterstof", "minecraft:glowstone_dust", 26, 12, GROEN),
                    spoor("gesmolten_was", "minecraft:honeycomb", 30, 10, GOUD),
                    spoor("zoemende_honingpot", "minecraft:honey_bottle", 34, 12, GOUD),
                    haring("uilenveertje_trap", "minecraft:feather", "oehoe", 24, 16, WIT),
                    getuigenis("groene_lichtjes", "bonnie")),
            List.of("oehoe", "pim", "glimworm"), "glimworm",
            List.of(new Getuige("bonnie", Kind.BOERINNEGUH, 32, 19)), 2400, List.of("glimwormguhtjes"));

    public static final Zaak BLIKJE = new Zaak("blikje", 9, v -> v.hoeve >= 8,
            List.of(spoor("veertjes_boompje", "minecraft:feather", 54, 6, WIT),
                    spoor("nesttakje", "minecraft:stick", 58, 10, BRUIN),
                    spoor("ansichtkaart", "minecraft:paper", 52, 11, BLAUW),
                    haring("glimmend_knoopje", "minecraft:iron_nugget", "morris", 58, 4, GRIJS),
                    getuigenis("iets_wits", "pim")),
            List.of("morris", "pim", "oehoe", "makelaar"), "oehoe",
            List.of(new Getuige("pim", Kind.PIM_MIKA, 49, 8)), 3000, List.of("oudste_boompje", "testament"));

    public static final Zaak VOORBEREIDING = new Zaak("voorbereiding", 10, v -> true,
            List.of(getuigenis("oehoe_bewaakt", "oehoe"), getuigenis("oma_kende_opa", "oma"), getuigenis("pim_biecht", "pim"),
                    getuigenis("hendrik_komt_graag", "hendrik"), getuigenis("guhrrit_proefde", "guhrrit")),
            List.of(), null,
            List.of(new Getuige("oehoe", Kind.OEHOE, 28, 19), new Getuige("oma", Kind.DROOM_OMAGUH, 40, 37),
                    new Getuige("pim", Kind.PIM_MIKA, 24, 37), new Getuige("hendrik", null, 16, 45),
                    new Getuige("guhrrit", Kind.GUHRRIT_VAKANTIE, 32, 51)),
            3500, List.of("getuigenbank"));

    public static final Zaak RECHTSZAAK = new Zaak("rechtszaak", 11, v -> v.hoeve >= 9,
            List.of(), List.of(), null, List.of(), 6000,
            List.of("standbeeld_opa_knabbel", "knabbelparkinkje", "detective_pet", "bureau_eindlook"));

    public static final List<Zaak> ALLE = List.of(EIEREN, KRUIMELS, SPOOK, WOL, KELDER, KOEIENBEL, PAALTJES, LICHT, BLIKJE, VOORBEREIDING, RECHTSZAAK);

    /** The deco among the rewards (the rest are flags other slices can read with Sherlock.beloningen). */
    public static final List<String> DECO = List.of("hartjesnest", "theekransje_tafeltje", "uilenhuisje", "reuzensjaal", "koeienbel_lantaarn",
            "moestuintje_pim", "glimwormguhtjes", "oudste_boompje", "getuigenbank", "standbeeld_opa_knabbel", "knabbelparkinkje");

    /** The titles per case (feature/titels): earned when that case is solved. */
    public static final Map<String, String> TITELS = Map.of("eieren", "sherlock_speurneus", "spook", "sherlock_spokenjager", "kelder", "sherlock_archiefguh",
            "paaltjes", "sherlock_grenswachter", "licht", "sherlock_lichtpuntje", "blikje", "sherlock_schatbewaarder", "rechtszaak", "sherlock_meesterspeurneus");

    /** Every character that can be on the board or speak (lang sherlock.wie.<id>) and the item on their polaroid. */
    public static final Map<String, String> WIE = Map.ofEntries(Map.entry("pim", "minecraft:carrot"), Map.entry("eendje", "minecraft:lily_pad"),
            Map.entry("hendrik", "minecraft:chicken_spawn_egg"), Map.entry("meelmuts", "minecraft:wheat"), Map.entry("babyguhs", "minecraft:pink_dye"),
            Map.entry("morris", "minecraft:gray_concrete"), Map.entry("spook", "minecraft:cobweb"), Map.entry("postguh", "minecraft:chest_minecart"),
            Map.entry("wolletje", "minecraft:white_wool"), Map.entry("schaapje", "minecraft:sheep_spawn_egg"), Map.entry("oma", "minecraft:red_wool"),
            Map.entry("konijntjes", "minecraft:rabbit_foot"), Map.entry("bertha", "minecraft:cow_spawn_egg"), Map.entry("muzikanten", "minecraft:note_block"),
            Map.entry("bouwer", "minecraft:iron_shovel"), Map.entry("oehoe", "minecraft:feather"), Map.entry("glimworm", "minecraft:glow_berries"),
            Map.entry("makelaar", "minecraft:filled_map"));

    /** The courtroom: the evidence you can show (in this order) and the right answer for each of Morris' three claims. */
    public static final List<String> BEWIJS = List.of("testament", "laatste_bladzijde", "kaasknabbel", "rood_veertje", "glimmend_knoopje");
    public static final List<String> BEZWAAR = List.of("testament", "laatste_bladzijde", "kaasknabbel");

    private Zaken() {
    }

    @Nullable
    public static Zaak van(String id) {
        for (Zaak z : ALLE) {
            if (z.id().equals(id)) {
                return z;
            }
        }
        return null;
    }

    @Nullable
    public static Zaak vorige(Zaak z) {
        int i = ALLE.indexOf(z);
        return i <= 0 ? null : ALLE.get(i - 1);
    }

    @Nullable
    public static Zaak volgende(Zaak z) {
        int i = ALLE.indexOf(z);
        return i < 0 || i + 1 >= ALLE.size() ? null : ALLE.get(i + 1);
    }
}
