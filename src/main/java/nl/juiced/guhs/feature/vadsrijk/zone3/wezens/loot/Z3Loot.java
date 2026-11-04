package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot;

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
 * Zone-3 loot from the creatures (lv 30-50): 15 base types (items guhs:vr_z3_mob_&lt;id&gt;):
 * <ul>
 *   <li>the <b>Bloesemset</b> (4, crown + branches, lv 30-38: Behendigheid/Kritiek);</li>
 *   <li>the <b>Schorsset</b> (4, the Schorsstad, lv 40-43: Verdediging, ZONE3_PLAN §9.4; the Piepvel cape parts are bazen's);</li>
 *   <li>the <b>Wortelset</b> (4, the Wortelvelden + Wortelduister, lv 43-50: Wijsheid/Elementweerstand, carries its own light);</li>
 *   <li>3 pieces of jewellery: honingring, harsketting, sporenoorbel.</li>
 * </ul>
 * See {@link Z3Sets} for the bonuses. A table per creature "z3wezens:&lt;naam&gt;" + "_elite", and a rich one per contract
 * elite "z3wezens:contract_&lt;id&gt;". Weapons: z3-systemen's six zone-3 class weapons (Z3Wapens.ALLE) plus whatever another slice
 * adds with {@link #voegWapenToe}. Crystals: z3-systemen drops them itself for every
 * defeated creature (as z2-systemen does), so nothing here.
 */
public final class Z3Loot {
    /** id -> slot (the generator tools/features/vadsrijk_z3_wezens.py has the same list: HARNAS + SIERADEN). */
    public static final Map<String, Slot> STUKKEN = new LinkedHashMap<>();

    static {
        STUKKEN.put("bloesemkap", Slot.HOOFD);
        STUKKEN.put("bloesemjak", Slot.BORST);
        STUKKEN.put("bloesembroek", Slot.BENEN);
        STUKKEN.put("bloesemtrippers", Slot.VOETEN);
        STUKKEN.put("schorshelm", Slot.HOOFD);
        STUKKEN.put("schorspantser", Slot.BORST);
        STUKKEN.put("schorsbeenstukken", Slot.BENEN);
        STUKKEN.put("schorslaarzen", Slot.VOETEN);
        STUKKEN.put("wortelkroon", Slot.HOOFD);
        STUKKEN.put("wortelmantel", Slot.BORST);
        STUKKEN.put("wortelbroek", Slot.BENEN);
        STUKKEN.put("wortelstappers", Slot.VOETEN);
        STUKKEN.put("honingring", Slot.RING);
        STUKKEN.put("harsketting", Slot.KETTING);
        STUKKEN.put("sporenoorbel", Slot.OORBEL);
    }

    /** The 8 contract trophies (items guhs:vr_z3_mob_trofee_&lt;id&gt;; plain keepsakes for the Trofeeënhal / the huisje). */
    public static final List<String> TROFEEEN = List.of("ouwe_rotkeiler", "moeder_wesp", "de_harsreus", "kapitein_kaaskop", "grootvader_rups",
            "de_stille_ziener", "slangenkoningin_sapsie", "bramzwijn_de_brede");

    private static final List<LootTabel.Regel> EXTRA = new ArrayList<>();

    private Z3Loot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_z3_mob_" + basis;
    }

    public static boolean sieraad(String id) {
        return STUKKEN.get(id) != null && STUKKEN.get(id).sieraad();
    }

    /** An armour piece; base stats ~1.6x zone 2 (lv 30-50 gear), growth per level like zones 1-2. */
    private static BasisType stuk(String id, double vd, double vads, Object... extra) {
        Stats basis = Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads);
        Stats groei = Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2);
        voegToe(basis, groei, extra);
        return new BasisType(id, itemId(id), STUKKEN.get(id), "harnas", null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    private static BasisType sieraad(String id, Object... stats) {
        Stats basis = new Stats(), groei = new Stats();
        voegToe(basis, groei, stats);
        return BasisType.sieraad(id, itemId(id), STUKKEN.get(id), basis, groei, 3);
    }

    private static void voegToe(Stats basis, Stats groei, Object... extra) {
        for (int i = 0; i + 1 < extra.length; i += 2) {
            Stat s = (Stat) extra[i];
            double v = ((Number) extra[i + 1]).doubleValue();
            basis.plus(s, v);
            groei.plus(s, v * (s.hoofd() ? 0.25 : 0.06));
        }
    }

    /** From Z3WezensRegistry.register. */
    public static void basisTypes() {
        // Bloesemset: behendigheid + kritiek, light and quick
        BasisTypes.registreer(stuk("bloesemkap", 12, 38, Stat.BEHENDIGHEID, 8, Stat.KRITIEK, 2));
        BasisTypes.registreer(stuk("bloesemjak", 20, 62, Stat.BEHENDIGHEID, 10));
        BasisTypes.registreer(stuk("bloesembroek", 17, 54, Stat.BEHENDIGHEID, 8, Stat.KRITIEKSCHADE, 4));
        BasisTypes.registreer(stuk("bloesemtrippers", 10, 34, Stat.BEHENDIGHEID, 6, Stat.LOOPSNELHEID, 5));
        // Schorsset: verdediging + kracht, heavy bark
        BasisTypes.registreer(stuk("schorshelm", 16, 46, Stat.KRACHT, 7, Stat.VERDEDIGING, 5));
        BasisTypes.registreer(stuk("schorspantser", 26, 76, Stat.KRACHT, 9, Stat.VERDEDIGING, 8));
        BasisTypes.registreer(stuk("schorsbeenstukken", 22, 64, Stat.KRACHT, 8));
        BasisTypes.registreer(stuk("schorslaarzen", 13, 40, Stat.VERDEDIGING, 6, Stat.VADS, 20));
        // Wortelset: wijsheid + elementweerstand, roots and lantern-light
        BasisTypes.registreer(stuk("wortelkroon", 15, 48, Stat.WIJSHEID, 9, Stat.ELEMENTWEERSTAND, 5));
        BasisTypes.registreer(stuk("wortelmantel", 24, 76, Stat.WIJSHEID, 11, Stat.HEALKRACHT, 4));
        BasisTypes.registreer(stuk("wortelbroek", 21, 66, Stat.WIJSHEID, 9, Stat.ELEMENTWEERSTAND, 4));
        BasisTypes.registreer(stuk("wortelstappers", 13, 42, Stat.WIJSHEID, 6, Stat.COOLDOWNREDUCTIE, 3));
        // jewellery
        BasisTypes.registreer(sieraad("honingring", Stat.HEALKRACHT, 8, Stat.KRITIEK, 3));
        BasisTypes.registreer(sieraad("harsketting", Stat.VERDEDIGING, 10, Stat.VADS, 40));
        BasisTypes.registreer(sieraad("sporenoorbel", Stat.ELEMENTSCHADE, 9, Stat.LEVENSROOF, 2));
    }

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
        }
        for (String w : nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Wapens.ALLE) {   // z3-systemen's six zone-3 class weapons
            l.add(new LootTabel.Regel(w, 1));
        }
        l.addAll(EXTRA);
        return l;
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("z3wezens:" + mob, new LootTabel(r, 1, kans, 1.3, muntMin, muntMax));
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        elite.add(new LootTabel.Regel("honingring", 2));
        elite.add(new LootTabel.Regel("harsketting", 2));
        elite.add(new LootTabel.Regel("sporenoorbel", 2));
        LootTabellen.registreer("z3wezens:" + mob + "_elite", new LootTabel(elite, 2, Math.min(1, kans * 3.5), 3.0, muntMin * 3, muntMax * 3));
    }

    /** Another slice's zone-3 weapon (a base type id) in every zone-3 creature table (rebuilds them). */
    public static void voegWapenToe(String basisType, int gewicht) {
        EXTRA.add(new LootTabel.Regel(basisType, gewicht));
        tabellen();
    }

    public static final List<String> BLOESEM = List.of("bloesemkap", "bloesemjak", "bloesembroek", "bloesemtrippers");
    public static final List<String> SCHORS = List.of("schorshelm", "schorspantser", "schorsbeenstukken", "schorslaarzen");
    public static final List<String> WORTEL = List.of("wortelkroon", "wortelmantel", "wortelbroek", "wortelstappers");

    /** One table per creature (after {@link #basisTypes}). */
    public static void tabellen() {
        List<LootTabel.Regel> kruin = regels("bloesemkap", 3, "bloesemjak", 3, "bloesembroek", 3, "bloesemtrippers", 3, "honingring", 1);
        List<LootTabel.Regel> takken = regels("bloesemkap", 2, "bloesemjak", 2, "bloesembroek", 2, "bloesemtrippers", 2, "schorshelm", 1, "schorslaarzen", 1,
                "honingring", 1, "sporenoorbel", 1);
        List<LootTabel.Regel> schors = regels("schorshelm", 3, "schorspantser", 3, "schorsbeenstukken", 3, "schorslaarzen", 3, "harsketting", 1);
        List<LootTabel.Regel> velden = regels("wortelkroon", 2, "wortelmantel", 2, "wortelbroek", 2, "wortelstappers", 2, "schorspantser", 1,
                "schorsbeenstukken", 1, "sporenoorbel", 1);
        List<LootTabel.Regel> duister = regels("wortelkroon", 3, "wortelmantel", 3, "wortelbroek", 3, "wortelstappers", 3, "sporenoorbel", 1,
                "harsketting", 1);
        tabel("bladrover", kruin, 0.22, 34, 66);
        tabel("grauwe_rups", kruin, 0.24, 36, 70);
        tabel("bijtbloesem", kruin, 0.2, 30, 60);
        tabel("kaasschutter", takken, 0.24, 40, 76);
        tabel("schorsmarter", takken, 0.2, 36, 68);
        tabel("grauwe_wesp", takken, 0.2, 36, 68);
        tabel("schorskruiper", schors, 0.24, 46, 88);
        tabel("lantaarnloze", schors, 0.24, 46, 88);
        tabel("harsgolem", schors, 0.32, 56, 104);
        tabel("rotzwijn", velden, 0.26, 52, 96);
        tabel("wortelslang", velden, 0.24, 50, 94);
        tabel("mika_schimmelboer", velden, 0.26, 54, 100);
        tabel("wortelschim", duister, 0.24, 56, 104);
        tabel("dove_ziener", duister, 0.34, 70, 130);
        // the contract elites: 3 rolls, always something, lots of luck (Zeldzaam+ is likely), the right set for their area
        contract("ouwe_rotkeiler", velden);
        contract("moeder_wesp", takken);
        contract("de_harsreus", schors);
        contract("kapitein_kaaskop", takken);
        contract("grootvader_rups", kruin);
        contract("de_stille_ziener", duister);
        contract("slangenkoningin_sapsie", velden);
        contract("bramzwijn_de_brede", velden);
    }

    private static void contract(String id, List<LootTabel.Regel> basis) {
        List<LootTabel.Regel> r = new ArrayList<>(basis);
        r.add(new LootTabel.Regel("honingring", 2));
        r.add(new LootTabel.Regel("harsketting", 2));
        r.add(new LootTabel.Regel("sporenoorbel", 2));
        LootTabellen.registreer("z3wezens:contract_" + id, new LootTabel(r, 3, 1.0, 6.0, 400, 800));
    }
}
