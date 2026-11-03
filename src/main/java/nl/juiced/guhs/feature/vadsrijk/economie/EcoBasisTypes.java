package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;

import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;

/**
 * The economie slice's loot tables over the zone-1 base types of the other owners (integration 1.10.0: economie no longer has
 * base types of its own; one owner per concept):
 * <ul>
 * <li>kern: kussenzwaard, knuffelknots, pluishamer, pluishelm, knuffelharnas (+ the jewellery);</li>
 * <li>klassen ({@code klassen.Wapens}): the 9 class weapons (klodderkling, pluisboog, druppelboog, snoepkruisboog, eikelkatapult,
 * rozenstaf, vlammenstaf, zwamstokje, slijmboek);</li>
 * <li>wezens ({@code wezens.loot.WezensLoot}): the 14 armour pieces incl. the Klodderset and the Zwamset;</li>
 * <li>bazen ({@code bazen.BazenLoot}): the named Legendary/Guhddelijk items (Klodderkroontje, Neppe Snor...).</li>
 * </ul>
 * Tables: {@link #TABEL_KLODDER}, {@link #TABEL_ZWAM}, {@link #TABEL_ALLES}, {@link #TABEL_ZONE1} and {@code economie:baas_<baas>}
 * (the random part of the bazen chests).
 */
public final class EcoBasisTypes {
    public static final String TABEL_KLODDER = "economie:klodder", TABEL_ZWAM = "economie:zwam", TABEL_ALLES = "economie:alles",
            TABEL_ZONE1 = "economie:zone1";

    /** The base types the shops and the "alles" tables use (no named boss items). */
    public static final List<String> WAPENS = List.of("kussenzwaard", "knuffelknots", "pluishamer", "klodderkling", "pluisboog", "druppelboog",
            "snoepkruisboog", "eikelkatapult", "rozenstaf", "vlammenstaf", "zwamstokje", "slijmboek");
    public static final List<String> HARNAS = List.of("pluishelm", "knuffelharnas", "slijmmutsje", "bubbelvest", "plakpantoffels", "steelbroek",
            "knaagvachtje", "kristalhelmpje", "klodderkapje", "klodderhesje", "klodderbroek", "klodderstuiters", "zwamhoed", "sporenmantel",
            "zwambroek", "mossloffen");

    private EcoBasisTypes() {
    }

    /** Registers the loot tables (from EconomieRegistry.register). */
    static void registreer() {
        LootTabellen.registreer(TABEL_KLODDER, new LootTabel(List.of(r("klodderkling", 2), r("druppelboog", 2), r("slijmboek", 2),
                r("klodderkapje", 3), r("klodderhesje", 3), r("klodderbroek", 3), r("klodderstuiters", 3), r("slijmmutsje", 1),
                r("plakpantoffels", 1)), 1, 0.2, 1.0, 3, 12));
        LootTabellen.registreer(TABEL_ZWAM, new LootTabel(List.of(r("zwamstokje", 2), r("eikelkatapult", 2), r("knuffelknots", 2),
                r("zwamhoed", 3), r("sporenmantel", 3), r("zwambroek", 3), r("mossloffen", 3), r("steelbroek", 1)), 1, 0.2, 1.0, 5, 16));
        LootTabellen.registreer(TABEL_ALLES, new LootTabel(alles(2, 2), 1, 0.25, 1.2, 4, 14));
        LootTabellen.registreer(TABEL_ZONE1, new LootTabel(alles(3, 2), 2, 1.0, 2.0, 80, 220));
        // the bazen chests (bazen.BazenLoot#tabel rolls economie:baas_<baas> first): a themed mix, 2-3 pieces with good luck
        LootTabellen.registreer("economie:baas_koning_klodder", new LootTabel(metSieraden(List.of(r("klodderkling", 3), r("druppelboog", 3),
                r("slijmboek", 3), r("knuffelknots", 2), r("klodderkapje", 3), r("klodderhesje", 3), r("klodderbroek", 3),
                r("klodderstuiters", 3), r("bubbelvest", 1), r("slijmmutsje", 1))), 2, 1.0, 2.5, 150, 400));
        LootTabellen.registreer("economie:baas_opperzwam", new LootTabel(metSieraden(List.of(r("vlammenstaf", 3), r("zwamstokje", 3), r("eikelkatapult", 3),
                r("snoepkruisboog", 2), r("pluishamer", 3), r("zwamhoed", 3), r("sporenmantel", 3), r("zwambroek", 3), r("mossloffen", 3),
                r("kristalhelmpje", 2), r("knaagvachtje", 2))), 3, 1.0, 3.0, 300, 700));
        LootTabellen.registreer("economie:baas_mika_spion", new LootTabel(metSieraden(alles(2, 2)), 2, 1.0, 2.8, 250, 600));
    }

    private static List<LootTabel.Regel> alles(int wapen, int harnas) {
        List<LootTabel.Regel> l = new ArrayList<>();
        WAPENS.forEach(id -> l.add(r(id, wapen)));
        HARNAS.forEach(id -> l.add(r(id, harnas)));
        return l;
    }

    /** + kern's 3 jewellery pieces, weight 1 each (the boss chests; integration 1.10.0). */
    private static List<LootTabel.Regel> metSieraden(List<LootTabel.Regel> l) {
        List<LootTabel.Regel> m = new ArrayList<>(l);
        for (String s : List.of("roze_ring", "knuffelketting", "pluisoorbel")) {
            m.add(r(s, 1));
        }
        return m;
    }

    private static LootTabel.Regel r(String id, int w) {
        return new LootTabel.Regel(id, w);
    }
}
