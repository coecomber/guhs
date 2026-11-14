package nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensPlekken;

/**
 * Zone-4 loot from the creatures (lv 50-56, items {@code guhs:vr_z4_mob_<id>}):
 * <ul>
 *   <li>the <b>Schelpenset</b> (4, coast and rocks: Kracht/Behendigheid/Verdediging, for Ridder and Jager);</li>
 *   <li>the <b>Lampionset</b> (4, the lantern islands: Wijsheid/Heal-kracht, for the Magier);</li>
 *   <li>3 pieces of jewellery: parelring, schelpenketting, lampionoorbel;</li>
 *   <li>6 class weapons (2 per class): Ankerknots + Golfbrekerzwaard (Ridder), Vliegerboog + Harpoenkruisboog (Jager),
 *       Lampionstaf + Koraalstaf (Magier), in the per-weapon DPS model ~7 % above the zone-3 weapons;</li>
 *   <li>2 socket crystals on the zone-2 gem system: Parelkristal (Vads) and Zeeglas (Elementweerstand), dropped by every zone-4
 *       foe at lv 50+ inside the zone box (normal 8 % ruw, elite 35 % geslepen, boss always stralend).</li>
 * </ul>
 * See {@link Z4Sets} for the set bonuses. <b>New slots (t-items):</b> 5 pieces for the gloves / cape / armband slots
 * ({@link #NIEUW}: Schelpenwanten + Lampioncape belong to the sets, Koraalwantjes, Vliegercape, Zeeglasarmband are loose). t-items'
 * {@code Slot.HANDSCHOENEN/CAPE/ARMBAND} were on THEIR branch only, so everything here looks the slots up BY NAME
 * ({@link #slot}): before the integration merge they simply don't exist (no items, no base types); after it they appear.
 */
public final class Z4Loot {
    /** id -> slot (the generator tools/features/vadsrijk_t_z4wezens.py has the same lists). */
    public static final Map<String, Slot> STUKKEN = new LinkedHashMap<>();

    static {
        STUKKEN.put("schelpenhelm", Slot.HOOFD);
        STUKKEN.put("schelpenpantser", Slot.BORST);
        STUKKEN.put("schelpenbroek", Slot.BENEN);
        STUKKEN.put("schelpenlaarzen", Slot.VOETEN);
        STUKKEN.put("lampionmuts", Slot.HOOFD);
        STUKKEN.put("lampionjurk", Slot.BORST);
        STUKKEN.put("lampionbroek", Slot.BENEN);
        STUKKEN.put("lampionslofjes", Slot.VOETEN);
        STUKKEN.put("parelring", Slot.RING);
        STUKKEN.put("schelpenketting", Slot.KETTING);
        STUKKEN.put("lampionoorbel", Slot.OORBEL);
    }

    public static final List<String> SCHELPEN = List.of("schelpenhelm", "schelpenpantser", "schelpenbroek", "schelpenlaarzen");
    public static final List<String> LAMPION = List.of("lampionmuts", "lampionjurk", "lampionbroek", "lampionslofjes");

    /** The six class weapons (items guhs:vr_z4_&lt;id&gt;). */
    public static final String ANKERKNOTS = "ankerknots", GOLFBREKER = "golfbreker", VLIEGERBOOG = "vliegerboog", HARPOENKRUISBOOG = "harpoenkruisboog",
            LAMPIONSTAF = "lampionstaf", KORAALSTAF = "koraalstaf";
    public static final List<String> WAPENS = List.of(ANKERKNOTS, GOLFBREKER, VLIEGERBOOG, HARPOENKRUISBOOG, LAMPIONSTAF, KORAALSTAF);

    /** The two zone-4 socket crystals (items guhs:vr_z4_kristal_&lt;id&gt;). */
    public static final List<String> KRISTALLEN = List.of("parelkristal", "zeeglas");
    public static final int ZONE4_LEVEL = 50;

    /** A piece for one of t-items' new slots: id, slot NAME, set (null = loose), stats (stat, value at level 1, ...). */
    public record Nieuw(String id, String slot, String set, Object[] stats) {
    }

    public static final Map<String, Nieuw> NIEUW = new LinkedHashMap<>();

    static {
        nieuw("schelpenwanten", "HANDSCHOENEN", "schelpen", Stat.VERDEDIGING, 13, Stat.KRACHT, 6, Stat.BEHENDIGHEID, 6);
        nieuw("lampioncape", "CAPE", "lampion", Stat.VADS, 60, Stat.WIJSHEID, 9, Stat.HEALKRACHT, 4);
        nieuw("koraalwantjes", "HANDSCHOENEN", null, Stat.VERDEDIGING, 12, Stat.ELEMENTWEERSTAND, 6);
        nieuw("vliegercape", "CAPE", null, Stat.VADS, 58, Stat.LOOPSNELHEID, 4);
        nieuw("zeeglasarmband", "ARMBAND", null, Stat.AANVALSSNELHEID, 5, Stat.KRITIEK, 2);
    }

    private static void nieuw(String id, String slot, String set, Object... stats) {
        NIEUW.put(id, new Nieuw(id, slot, set, stats));
    }

    /** t-items' slot by name, or null when that slot does not exist on this build. */
    @javax.annotation.Nullable
    public static Slot slot(String naam) {
        for (Slot s : Slot.values()) {
            if (s.name().equals(naam)) {
                return s;
            }
        }
        return null;
    }

    /** The new-slot pieces that exist on this build (their slot exists). */
    public static List<Nieuw> nieuweStukken() {
        return NIEUW.values().stream().filter(n -> slot(n.slot()) != null).toList();
    }

    private static final List<LootTabel.Regel> EXTRA = new ArrayList<>();

    private Z4Loot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_z4_mob_" + basis;
    }

    /** An armour piece: base stats ~1.35x zone 3 (lv 50-56 gear), growth per level like zones 1-3. */
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
            groei.plus(s, v * (s.hoofd() ? nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType.HOOFD_GROEI : 0.06));
        }
    }

    private static void wapen(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, "guhs:vr_z4_" + id, soort, klasse, e, schade, perLevel, basis, groei, 3));
    }

    /** From Z4WezensRegistry.register. */
    public static void basisTypes() {
        // Schelpenset: coast armour from shells and sea-glass (Kracht + Behendigheid + Verdediging)
        BasisTypes.registreer(stuk("schelpenhelm", 21, 64, Stat.KRACHT, 8, Stat.BEHENDIGHEID, 6, Stat.VERDEDIGING, 4));
        BasisTypes.registreer(stuk("schelpenpantser", 34, 104, Stat.KRACHT, 11, Stat.VERDEDIGING, 8));
        BasisTypes.registreer(stuk("schelpenbroek", 29, 88, Stat.BEHENDIGHEID, 10, Stat.KRITIEK, 2));
        BasisTypes.registreer(stuk("schelpenlaarzen", 17, 56, Stat.KRACHT, 6, Stat.LOOPSNELHEID, 5));
        // Lampionset: soft lantern silk (Wijsheid + Heal-kracht)
        BasisTypes.registreer(stuk("lampionmuts", 19, 62, Stat.WIJSHEID, 11, Stat.ELEMENTSCHADE, 4));
        BasisTypes.registreer(stuk("lampionjurk", 31, 100, Stat.WIJSHEID, 14, Stat.HEALKRACHT, 6));
        BasisTypes.registreer(stuk("lampionbroek", 27, 86, Stat.WIJSHEID, 11, Stat.ELEMENTWEERSTAND, 5));
        BasisTypes.registreer(stuk("lampionslofjes", 17, 54, Stat.WIJSHEID, 8, Stat.COOLDOWNREDUCTIE, 3));
        // jewellery
        BasisTypes.registreer(sieraad("parelring", Stat.VADS, 70, Stat.KRITIEK, 3));
        BasisTypes.registreer(sieraad("schelpenketting", Stat.VERDEDIGING, 14, Stat.ELEMENTWEERSTAND, 4));
        BasisTypes.registreer(sieraad("lampionoorbel", Stat.HEALKRACHT, 10, Stat.ELEMENTSCHADE, 6));
        // weapons: ~7 % above zone 3 (6.7 + 1.35/lv against 6.3 + 1.26/lv), plus a coast side stat
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        wapen(ANKERKNOTS, "knots", r, Element.WATER, 6.7, 1.35, Stats.van(Stat.VERDEDIGING, 8, Stat.VADS, 110), Stats.van(Stat.KRACHT, 0.38, Stat.VERDEDIGING, 0.16));
        wapen(GOLFBREKER, "zwaard", r, Element.WATER, 6.9, 1.38, Stats.van(Stat.KRACHT, 6, Stat.KRITIEKSCHADE, 12), Stats.van(Stat.KRACHT, 0.45));
        wapen(VLIEGERBOOG, "boog", j, Element.NATUUR, 6.7, 1.35, Stats.van(Stat.BEHENDIGHEID, 4, Stat.LOOPSNELHEID, 5), Stats.van(Stat.BEHENDIGHEID, 0.43, Stat.KRITIEK, 0.05));
        wapen(HARPOENKRUISBOOG, "kruisboog", j, Element.WATER, 6.9, 1.38, Stats.van(Stat.KRITIEKSCHADE, 18), Stats.van(Stat.BEHENDIGHEID, 0.39));
        wapen(LAMPIONSTAF, "staf", m, Element.VUUR, 6.8, 1.36, Stats.van(Stat.ELEMENTSCHADE, 10, Stat.WIJSHEID, 3), Stats.van(Stat.WIJSHEID, 0.43));
        wapen(KORAALSTAF, "staf", m, Element.ROZE, 6.6, 1.33, Stats.van(Stat.WIJSHEID, 4, Stat.HEALKRACHT, 14), Stats.van(Stat.WIJSHEID, 0.39, Stat.HEALKRACHT, 0.16));
        Aanvalstempo.TYPE.put(LAMPIONSTAF, 1.6);   // a slower, bigger lantern blast (like the zone-3 Lantaarnstaf)
        List<LootTabel.Regel> w = new ArrayList<>();
        for (String id : WAPENS) {
            w.add(new LootTabel.Regel(id, 3));
        }
        LootTabellen.registreer("z4wezens:wapens", new LootTabel(w, 1, 1.0, 1.0, 0, 0));
        // t-items' new slots (only when they exist): growth like the jewellery
        for (Nieuw n : nieuweStukken()) {
            Stats basis = new Stats(), groei = new Stats();
            voegToe(basis, groei, n.stats());
            String soort = n.slot().equals("HANDSCHOENEN") ? "handschoenen" : n.slot().equals("CAPE") ? "cape" : "sieraad";
            BasisTypes.registreer(new BasisType(n.id(), itemId(n.id()), slot(n.slot()), soort, null, Element.GEEN, 0, 0, basis, groei, 3));
        }
    }

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
        }
        for (String w : WAPENS) {
            l.add(new LootTabel.Regel(w, 1));
        }
        for (Nieuw n : nieuweStukken()) {   // t-items' slots: the set glove/cape with its set's weight, loose pieces weight 1
            int gewicht = 1;
            List<String> set = "schelpen".equals(n.set()) ? SCHELPEN : "lampion".equals(n.set()) ? LAMPION : List.of();
            for (int i = 0; i + 1 < paren.length; i += 2) {
                if (set.contains(paren[i])) {
                    gewicht = (Integer) paren[i + 1];
                    break;
                }
            }
            if (n.set() == null || set.stream().anyMatch(x -> java.util.Arrays.asList(paren).contains(x))) {
                l.add(new LootTabel.Regel(n.id(), gewicht));
            }
        }
        l.addAll(EXTRA);
        return l;
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("z4wezens:" + mob, new LootTabel(r, 1, nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.gewoon(kans), 1.4, muntMin, muntMax));
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        elite.add(new LootTabel.Regel("parelring", 2));
        elite.add(new LootTabel.Regel("schelpenketting", 2));
        elite.add(new LootTabel.Regel("lampionoorbel", 2));
        LootTabellen.registreer("z4wezens:" + mob + "_elite", new LootTabel(elite, 2, nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.elite(kans), nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans.eliteGeluk(3.2), muntMin * 3, muntMax * 3));
    }

    /** Another slice's zone-4 piece (a base type id) in every zone-4 creature table (rebuilds them): t-items' new slots. */
    public static void voegToe(String basisType, int gewicht) {
        EXTRA.add(new LootTabel.Regel(basisType, gewicht));
        tabellen();
    }

    /** One table per creature (after {@link #basisTypes}). A bit richer than zone 3: fewer creatures, so each one counts. */
    public static void tabellen() {
        List<LootTabel.Regel> kust = regels("schelpenhelm", 3, "schelpenpantser", 3, "schelpenbroek", 3, "schelpenlaarzen", 3, "schelpenketting", 1,
                "lampionslofjes", 1);
        List<LootTabel.Regel> licht = regels("lampionmuts", 3, "lampionjurk", 3, "lampionbroek", 3, "lampionslofjes", 3, "lampionoorbel", 1,
                "schelpenhelm", 1);
        List<LootTabel.Regel> gemengd = regels("schelpenhelm", 2, "schelpenpantser", 2, "schelpenbroek", 2, "schelpenlaarzen", 2, "lampionmuts", 2,
                "lampionjurk", 2, "lampionbroek", 2, "lampionslofjes", 2, "parelring", 1);
        tabel("mopperkrab", kust, 0.3, 70, 130);
        tabel("pruilrob", kust, 0.34, 80, 150);
        tabel("stekelpluis", kust, 0.3, 70, 130);
        tabel("zeurslak", kust, 0.34, 80, 150);
        tabel("koraalkobold", gemengd, 0.28, 64, 120);
        tabel("mokmeeuw", gemengd, 0.3, 70, 130);
        tabel("bromvlieger", licht, 0.3, 72, 136);
        tabel("lampionkwal", licht, 0.32, 74, 140);
        tabel("mistmopje", licht, 0.34, 80, 150);
        tabel("vliegerknecht", gemengd, 0.36, 90, 170);
    }

    // ---- crystals ------------------------------------------------------------------------------------------------------------

    /** From Z4WezensRegistry.register: the drop hook (zone-3's own hook only works inside zone 3). */
    public static void kristalHaak() {
        Haken.VERSLAGEN.add(v -> {
            VadsrijkMob m = v.mob();
            if (m.mobLevel() < ZONE4_LEVEL || !Z4WezensPlekken.inZone(m.getX(), m.getZ())) {
                return;
            }
            boolean baas = m instanceof VadsrijkBaas;
            int q = Kristallen.dropKwaliteit(m.elite(), baas);
            double kans = baas ? 1.0 : m.elite() ? 0.35 : 0.08;
            for (ServerPlayer p : v.helpers()) {
                if (p.getRandom().nextDouble() >= kans) {
                    continue;
                }
                ItemStack s = Kristallen.stack(KRISTALLEN.get(p.getRandom().nextInt(KRISTALLEN.size())), q, 1);
                LootBundel.laatVallen(p, m.position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), q);
            }
        });
    }
}
