package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * Zone-5 loot (Hoogvads, lv 56-60). Items {@code guhs:vr_z5_mob_<id>}:
 * <ul>
 *   <li>the <b>Wolkwachtset</b>: helmet, cuirass, leg plates, boots + (with t-items' new slots) gloves and a cape: a 6-piece set
 *       ({@link Z5Sets}: 2 / 4 / 6 pieces);</li>
 *   <li>jewellery: Wolkring, Walvisketting, Regendruppeloorbel + (t-items) the Stormarmband;</li>
 *   <li>6 class weapons (2 per class), on the zone-3 rating line ~8 % higher (6.8 + 1.36/lv);</li>
 *   <li>tables {@code z5wezens:<creature>} (+ "_elite"), {@code z5wezens:sparren} (the sparring reward) and {@code z5wezens:baas}
 *       (the base roll of the boss chests).</li>
 * </ul>
 * <b>t-items:</b> the slots HANDSCHOENEN / CAPE / ARMBAND and the stat BAZENSCHADE are looked up BY NAME ({@link #slot}, {@link #stat}):
 * without t-items in the tree those pieces/stats are simply left out (nothing breaks); merged, they appear on their own.
 */
public final class Z5Loot {
    /** A slot of t-items by name, or null when this tree does not have it. */
    @Nullable
    public static Slot slot(String naam) {
        try {
            return Slot.valueOf(naam);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static Stat stat(String naam) {
        try {
            return Stat.valueOf(naam);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static final Slot HANDSCHOENEN = slot("HANDSCHOENEN"), CAPE = slot("CAPE"), ARMBAND = slot("ARMBAND");

    /** id -> slot of every armour/jewellery piece that exists in this tree (the generator has the full list). */
    public static final Map<String, Slot> STUKKEN = new LinkedHashMap<>();
    /** The Wolkwachtset, in order (only the pieces that exist here). */
    public static final List<String> WOLKWACHT = new ArrayList<>();

    private static void stuk(String id, @Nullable Slot slot, boolean set) {
        if (slot == null) {
            return;
        }
        STUKKEN.put(id, slot);
        if (set) {
            WOLKWACHT.add(id);
        }
    }

    static {
        stuk("wolkwachthelm", Slot.HOOFD, true);
        stuk("wolkwachtkuras", Slot.BORST, true);
        stuk("wolkwachtbeenstukken", Slot.BENEN, true);
        stuk("wolkwachtlaarzen", Slot.VOETEN, true);
        stuk("wolkwachthandschoenen", HANDSCHOENEN, true);
        stuk("wolkwachtcape", CAPE, true);
        stuk("wolkring", Slot.RING, false);
        stuk("walvisketting", Slot.KETTING, false);
        stuk("regendruppeloorbel", Slot.OORBEL, false);
        stuk("stormarmband", ARMBAND, false);
    }

    public static final String WOLKWACHTZWAARD = "wolkwachtzwaard", WALVISBOTKNOTS = "walvisbotknots", STORMBOOG = "stormboog",
            HARPOENKRUISBOOG = "harpoenkruisboog", WOLKENSTAF = "wolkenstaf", BLIKSEMSTAF = "bliksemstaf";
    public static final List<String> WAPENS = List.of(WOLKWACHTZWAARD, WALVISBOTKNOTS, STORMBOOG, HARPOENKRUISBOOG, WOLKENSTAF, BLIKSEMSTAF);

    private Z5Loot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_z5_mob_" + basis;
    }

    private static void voegToe(Stats basis, Stats groei, Object... extra) {
        for (int i = 0; i + 1 < extra.length; i += 2) {
            Stat s = (Stat) extra[i];
            if (s == null) {
                continue;
            }
            double v = ((Number) extra[i + 1]).doubleValue();
            basis.plus(s, v);
            groei.plus(s, v * (s.hoofd() ? nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType.HOOFD_GROEI : 0.06));
        }
    }

    /** An armour piece; base ~1.25x zone 3 (lv 56-60 gear). */
    private static BasisType harnas(String id, double vd, double vads, Object... extra) {
        Stats basis = Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads);
        Stats groei = Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2);
        voegToe(basis, groei, extra);
        return new BasisType(id, itemId(id), STUKKEN.get(id), "harnas", null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    /** A virtual-slot piece (gloves, cape, jewellery): only its own stats. */
    private static BasisType virtueel(String id, String soort, Object... stats) {
        Stats basis = new Stats(), groei = new Stats();
        voegToe(basis, groei, stats);
        return new BasisType(id, itemId(id), STUKKEN.get(id), soort, null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    private static void wapen(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, itemId(id), soort, klasse, e, schade, perLevel, basis, groei, 3));
    }

    /** From Z5WezensRegistry.register. */
    public static void basisTypes() {
        Stat bazen = stat("BAZENSCHADE");
        BasisTypes.registreer(harnas("wolkwachthelm", 20, 58, Stat.VERDEDIGING, 6, Stat.KRACHT, 6, Stat.WIJSHEID, 6));
        BasisTypes.registreer(harnas("wolkwachtkuras", 33, 96, Stat.VERDEDIGING, 10, Stat.VADS, 40));
        BasisTypes.registreer(harnas("wolkwachtbeenstukken", 28, 82, Stat.BEHENDIGHEID, 8, Stat.KRACHT, 6));
        BasisTypes.registreer(harnas("wolkwachtlaarzen", 17, 52, Stat.LOOPSNELHEID, 6, Stat.VERDEDIGING, 5));
        if (STUKKEN.containsKey("wolkwachthandschoenen")) {
            BasisTypes.registreer(virtueel("wolkwachthandschoenen", "handschoenen", Stat.VERDEDIGING, 12, Stat.KRACHT, 6, Stat.BEHENDIGHEID, 6));
        }
        if (STUKKEN.containsKey("wolkwachtcape")) {
            BasisTypes.registreer(virtueel("wolkwachtcape", "cape", Stat.VADS, 52, Stat.ELEMENTWEERSTAND, 6, bazen, 3));
        }
        BasisTypes.registreer(virtueel("wolkring", "sieraad", Stat.KRITIEK, 4, Stat.KRITIEKSCHADE, 8, bazen, 2));
        BasisTypes.registreer(virtueel("walvisketting", "sieraad", Stat.VADS, 60, Stat.HEALKRACHT, 8));
        BasisTypes.registreer(virtueel("regendruppeloorbel", "sieraad", Stat.ELEMENTSCHADE, 10, Stat.COOLDOWNREDUCTIE, 3));
        if (STUKKEN.containsKey("stormarmband")) {
            BasisTypes.registreer(virtueel("stormarmband", "sieraad", Stat.AANVALSSNELHEID, 5, Stat.LOOPSNELHEID, 3));
        }
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        wapen(WOLKWACHTZWAARD, "zwaard", r, Element.ROZE, 6.8, 1.36, Stats.van(Stat.KRACHT, 6, Stat.VERDEDIGING, 6), Stats.van(Stat.KRACHT, 0.4, Stat.VERDEDIGING, 0.15));
        wapen(WALVISBOTKNOTS, "knots", r, Element.WATER, 7.0, 1.40, Stats.van(Stat.KRACHT, 6, Stat.VADS, 90), Stats.van(Stat.KRACHT, 0.45));
        wapen(STORMBOOG, "boog", j, Element.WATER, 6.8, 1.36, Stats.van(Stat.BEHENDIGHEID, 4, Stat.LOOPSNELHEID, 5), Stats.van(Stat.BEHENDIGHEID, 0.42, Stat.KRITIEK, 0.05));
        wapen(HARPOENKRUISBOOG, "kruisboog", j, Element.VUUR, 7.0, 1.40, Stats.van(Stat.KRITIEKSCHADE, 16, Stat.KRITIEK, 2), Stats.van(Stat.BEHENDIGHEID, 0.4));
        wapen(WOLKENSTAF, "staf", m, Element.ROZE, 6.7, 1.34, Stats.van(Stat.WIJSHEID, 4, Stat.HEALKRACHT, 14), Stats.van(Stat.WIJSHEID, 0.4, Stat.HEALKRACHT, 0.15));
        wapen(BLIKSEMSTAF, "staf", m, Element.WATER, 6.9, 1.38, Stats.van(Stat.ELEMENTSCHADE, 9, Stat.KRITIEK, 2), Stats.van(Stat.WIJSHEID, 0.44));
        Aanvalstempo.TYPE.put(BLIKSEMSTAF, 1.5);
    }

    private static final List<LootTabel.Regel> EXTRA = new ArrayList<>();

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            if (STUKKEN.containsKey((String) paren[i]) || WAPENS.contains((String) paren[i])) {
                l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
            }
        }
        for (String w : WAPENS) {
            l.add(new LootTabel.Regel(w, 1));
        }
        l.addAll(EXTRA);
        return l;
    }

    /** Another slice's zone-5 drop (a base type id) in every zone-5 creature table (rebuilds them). */
    public static void voegToe(String basisType, int gewicht) {
        EXTRA.add(new LootTabel.Regel(basisType, gewicht));
        tabellen();
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("z5wezens:" + mob, new LootTabel(r, 1, nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.gewoon(kans), 1.3, muntMin, muntMax));
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        for (String s : List.of("wolkring", "walvisketting", "regendruppeloorbel", "stormarmband")) {
            if (STUKKEN.containsKey(s)) {
                elite.add(new LootTabel.Regel(s, 2));
            }
        }
        LootTabellen.registreer("z5wezens:" + mob + "_elite", new LootTabel(elite, 2, nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.elite(kans), nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.eliteGeluk(3.0), muntMin * 3, muntMax * 3));
    }

    /** One table per creature + the sparring and boss tables (after {@link #basisTypes}). */
    public static void tabellen() {
        List<LootTabel.Regel> proef = regels("wolkwachthelm", 3, "wolkwachtkuras", 2, "wolkwachtbeenstukken", 2, "wolkwachtlaarzen", 3,
                "wolkwachthandschoenen", 2, "wolkring", 1);
        List<LootTabel.Regel> buik = regels("wolkwachtkuras", 3, "wolkwachtbeenstukken", 3, "wolkwachthandschoenen", 2, "walvisketting", 1,
                "stormarmband", 1);
        List<LootTabel.Regel> rand = regels("wolkwachthelm", 2, "wolkwachtcape", 3, "wolkwachtlaarzen", 2, "regendruppeloorbel", 1, "stormarmband", 1);
        List<LootTabel.Regel> nest = regels("wolkwachtcape", 2, "wolkwachthandschoenen", 2, "wolkwachtkuras", 2, "wolkring", 1, "walvisketting", 1);
        tabel("bezeten_oefenpop", proef, 0.24, 60, 110);
        tabel("mika_saboteur", buik, 0.26, 66, 120);
        tabel("mika_lontlegger", buik, 0.26, 66, 120);
        tabel("grauwe_pluismijt", buik, 0.12, 30, 60);
        tabel("grauwe_wolkduivel", rand, 0.24, 70, 124);
        tabel("mistslier", rand, 0.26, 72, 130);
        tabel("mika_harpoenier", rand, 0.26, 72, 130);
        tabel("vermomde_mika", nest, 0.3, 80, 140);
        List<LootTabel.Regel> set = new ArrayList<>();
        for (String s : WOLKWACHT) {
            set.add(new LootTabel.Regel(s, 1));
        }
        LootTabellen.registreer("z5wezens:sparren", new LootTabel(set, 1, 1.0, 2.0, 40, 80));
        List<LootTabel.Regel> baas = regels("wolkwachthelm", 2, "wolkwachtkuras", 2, "wolkwachtbeenstukken", 2, "wolkwachtlaarzen", 2,
                "wolkwachthandschoenen", 2, "wolkwachtcape", 2, "wolkring", 2, "walvisketting", 2, "regendruppeloorbel", 2, "stormarmband", 2);
        LootTabellen.registreer("z5wezens:baas", new LootTabel(baas, 2, 1.0, 4.0, 900, 1600));
    }
}
