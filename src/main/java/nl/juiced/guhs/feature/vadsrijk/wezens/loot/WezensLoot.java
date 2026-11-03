package nl.juiced.guhs.feature.vadsrijk.wezens.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * Zone-1 armour from the wezens (CONTRACT_VADSRIJK 8): 14 base types (6 loose pieces in three tiers + the Klodderset and the
 * Zwamset of 4 pieces each, see {@link SetBonussen}) and a loot table per creature, "wezens:&lt;naam&gt;" plus a richer
 * "wezens:&lt;naam&gt;_elite" (more rolls, more luck, more coins) that RijkWezen uses for elites. Items: guhs:vr_mob_&lt;id&gt;.
 * The kern weapons (kussenzwaard, knuffelknots, pluishamer) drop too; economie can add its weapons with {@link #extraWapens}.
 */
public final class WezensLoot {
    /** id -> slot, in the order of the generator (tools/features/vadsrijk_wezens.py HARNAS). */
    public static final Map<String, Slot> HARNAS = new LinkedHashMap<>();

    static {
        HARNAS.put("slijmmutsje", Slot.HOOFD);
        HARNAS.put("bubbelvest", Slot.BORST);
        HARNAS.put("plakpantoffels", Slot.VOETEN);
        HARNAS.put("steelbroek", Slot.BENEN);
        HARNAS.put("knaagvachtje", Slot.BORST);
        HARNAS.put("kristalhelmpje", Slot.HOOFD);
        HARNAS.put("klodderkapje", Slot.HOOFD);
        HARNAS.put("klodderhesje", Slot.BORST);
        HARNAS.put("klodderbroek", Slot.BENEN);
        HARNAS.put("klodderstuiters", Slot.VOETEN);
        HARNAS.put("zwamhoed", Slot.HOOFD);
        HARNAS.put("sporenmantel", Slot.BORST);
        HARNAS.put("zwambroek", Slot.BENEN);
        HARNAS.put("mossloffen", Slot.VOETEN);
    }

    /** Extra weapon base types (id, weight) that every table also rolls; economie/klassen may add theirs before the tables are built. */
    public static final List<LootTabel.Regel> extraWapens = new ArrayList<>();

    static {
        // integration 1.10.0: the 12 class weapons (kern's 3 ridder weapons + klassen.Wapens), 4 per class, weight 1 each
        for (String w : List.of("kussenzwaard", "knuffelknots", "pluishamer")) {
            extraWapens.add(new LootTabel.Regel(w, 1));
        }
        // (= klassen.Wapens.ALLE; literal so this class never triggers the klassen item registration)
        for (String w : List.of("klodderkling", "pluisboog", "druppelboog", "snoepkruisboog", "eikelkatapult", "rozenstaf", "vlammenstaf",
                "zwamstokje", "slijmboek")) {
            extraWapens.add(new LootTabel.Regel(w, 1));
        }
    }

    /** Kern's jewellery base types (roll in the elite tables only). */
    public static final List<String> SIERADEN = List.of("roze_ring", "knuffelketting", "pluisoorbel");

    private WezensLoot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_mob_" + basis;
    }

    private static BasisType stuk(String id, double vd, double vads, Object... extra) {
        Stats basis = Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads);
        Stats groei = Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2);
        for (int i = 0; i + 1 < extra.length; i += 2) {
            Stat s = (Stat) extra[i];
            double v = ((Number) extra[i + 1]).doubleValue();
            basis.plus(s, v);
            if (s.hoofd()) {
                groei.plus(s, v * 0.25);
            }
        }
        return new BasisType(id, itemId(id), HARNAS.get(id), "harnas", null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    /** The 14 base types (from WezensRegistry.register). */
    public static void basisTypes() {
        // loose pieces: tier 1 (Klodderweide), tier 2 (Sporenwoud), tier 3 (Druipgrotten/Zwamtempel)
        BasisTypes.registreer(stuk("slijmmutsje", 3, 10, Stat.ELEMENTWEERSTAND, 3));
        BasisTypes.registreer(stuk("bubbelvest", 5, 16));
        BasisTypes.registreer(stuk("plakpantoffels", 2, 8, Stat.LOOPSNELHEID, 4));
        BasisTypes.registreer(stuk("steelbroek", 6, 18, Stat.VERDEDIGING, 2));
        BasisTypes.registreer(stuk("knaagvachtje", 9, 26, Stat.KRITIEK, 2));
        BasisTypes.registreer(stuk("kristalhelmpje", 7, 20, Stat.ELEMENTWEERSTAND, 6));
        // Klodderset: behendigheid + slime
        BasisTypes.registreer(stuk("klodderkapje", 3, 10, Stat.BEHENDIGHEID, 3));
        BasisTypes.registreer(stuk("klodderhesje", 5, 15, Stat.BEHENDIGHEID, 4));
        BasisTypes.registreer(stuk("klodderbroek", 4, 12, Stat.BEHENDIGHEID, 3));
        BasisTypes.registreer(stuk("klodderstuiters", 2, 8, Stat.BEHENDIGHEID, 2, Stat.LOOPSNELHEID, 3));
        // Zwamset: wijsheid + spores
        BasisTypes.registreer(stuk("zwamhoed", 3, 12, Stat.WIJSHEID, 3));
        BasisTypes.registreer(stuk("sporenmantel", 5, 16, Stat.WIJSHEID, 4, Stat.HEALKRACHT, 3));
        BasisTypes.registreer(stuk("zwambroek", 4, 13, Stat.WIJSHEID, 3));
        BasisTypes.registreer(stuk("mossloffen", 2, 9, Stat.WIJSHEID, 2));
    }

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
        }
        l.addAll(extraWapens);
        return l;
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("wezens:" + mob, new LootTabel(r, 1, kans, 1.0, muntMin, muntMax));
        // elites (integration 1.10.0): also kern's 3 jewellery pieces, weight 1 each (rare)
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        for (String s : SIERADEN) {
            elite.add(new LootTabel.Regel(s, 1));
        }
        LootTabellen.registreer("wezens:" + mob + "_elite", new LootTabel(elite, 2, Math.min(1, kans * 3.5), 2.5, muntMin * 3, muntMax * 3));
    }

    /** One table per creature (from WezensRegistry.register, after {@link #basisTypes}). */
    public static void tabellen() {
        List<LootTabel.Regel> weide = regels("slijmmutsje", 4, "bubbelvest", 3, "plakpantoffels", 4, "klodderkapje", 2, "klodderhesje", 2, "klodderbroek", 2,
                "klodderstuiters", 2);
        List<LootTabel.Regel> woud = regels("steelbroek", 4, "slijmmutsje", 1, "plakpantoffels", 1, "zwamhoed", 2, "sporenmantel", 2, "zwambroek", 2,
                "mossloffen", 2, "klodderkapje", 1, "klodderstuiters", 1);
        List<LootTabel.Regel> grot = regels("knaagvachtje", 3, "kristalhelmpje", 3, "steelbroek", 2, "klodderhesje", 1, "klodderbroek", 1, "zwamhoed", 1,
                "sporenmantel", 1);
        List<LootTabel.Regel> tempel = regels("zwamhoed", 3, "sporenmantel", 3, "zwambroek", 3, "mossloffen", 3, "kristalhelmpje", 2, "knaagvachtje", 2);
        tabel("klodder", weide, 0.16, 2, 8);
        tabel("bubbelklodder", weide, 0.18, 3, 9);
        tabel("stuiterklodder", weide, 0.2, 3, 10);
        tabel("moederklodder", weide, 0.35, 10, 30);
        tabel("hoedjesbol", woud, 0.18, 5, 14);
        tabel("sporenpuffer", woud, 0.2, 5, 15);
        tabel("zwamwachter", woud, 0.26, 8, 20);
        tabel("mosgeest", woud, 0.2, 6, 16);
        tabel("druipslijm", grot, 0.2, 8, 20);
        tabel("kristalklodder", grot, 0.24, 9, 22);
        tabel("knaag", grot, 0.14, 5, 12);
        tabel("glimzwam", grot, 0.28, 10, 24);
        tabel("tempelridder", tempel, 0.26, 14, 32);
        tabel("sporenpriester", tempel, 0.28, 14, 34);
    }
}
