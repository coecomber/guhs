package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
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
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;

/**
 * Zone-6 loot from the creatures (lv 60-70; items {@code guhs:vr_z6_mob_<id>}, weapons {@code guhs:vr_z6_<id>}). No item level
 * (R-besluit: level + rarity, the u-specs tooltip arrows compare). Clear stat names (user rule).
 * <ul>
 *   <li><b>Duinwandelaar</b> (6, Ridder + Jager: Kracht / Behendigheid / Verdediging): tulband, mantel, broek, laarzen,
 *       wanten (gloves slot), cape (cape slot);</li>
 *   <li><b>Duinzijde</b> (6, Magier: Wijsheid / Heal-kracht / Elementschade): the same six slots in rose silk;</li>
 *   <li><b>Karavaanwacht</b> (4, field bosses, Episch): wanten, cape, armband, laarzen ({@code Z6BazenLoot});</li>
 *   <li><b>Sterrenwacht</b> (6, the dungeon, Episch): kap, sterrenkaartmantel, broek, laarzen, messing handschoenen,
 *       sterrenkaartcape ({@code Z6BazenLoot});</li>
 *   <li>5 jewellery: Saffraanring and Woestijnroosring (both fit ring 1 AND ring 2), Rozenketting, Sterrenoorbel,
 *       Zoutkristalarmband;</li>
 *   <li>6 class weapons, one per SPECIALISATION style (u-specs): Rozenkromzwaard (Schildridder), Dadelpalmknots (Reuzenkussen),
 *       Duinvalkboog (Scherpschutter), Karavaankruisboog (Strikkenzetter), Saffraanstaf (Sterrenwever), Oasefluit (Zegenzanger);
 *       the tooltip says which style ({@link #SPEC});</li>
 *   <li>2 socket crystals on the zone-2 gem system: Rozenkwarts (Kritiek) and Sterrenzout (Schade tegen bazen);</li>
 *   <li>pets that drop (Fennekje, Springpiepje, Sterretje, Dadelaapje), Sterrenstof (material) from Zandgeestjes and bosses,
 *       Gouden Veren from area leaders (10 %).</li>
 * </ul>
 */
public final class Z6Loot {
    /** id -> slot (the generator tools/features/vadsrijk_u_z6wezens.py has the same lists). */
    public static final Map<String, Slot> STUKKEN = new LinkedHashMap<>();
    public static final List<String> DUINWANDELAAR = List.of("duinwandelaarstulband", "duinwandelaarsmantel", "duinwandelaarsbroek",
            "duinwandelaarslaarzen", "duinwandelaarswanten", "duinwandelaarscape");
    public static final List<String> DUINZIJDE = List.of("zijdetulband", "zijdegewaad", "zijdepofbroek", "zijdeslofjes", "zijdehandschoentjes", "zijdesluier");
    public static final List<String> KARAVAANWACHT = List.of("karavaanwachtwanten", "karavaanwachtcape", "karavaanwachtarmband", "karavaanwachtlaarzen");
    public static final List<String> STERRENWACHT = List.of("sterrenwachtkap", "sterrenkaartmantel", "sterrenwachtbroek", "sterrenwachtlaarzen",
            "messinghandschoenen", "sterrenkaartcape");
    public static final List<String> SIERADEN = List.of("saffraanring", "woestijnroosring", "rozenketting", "sterrenoorbel", "zoutkristalarmband");

    static {
        Slot[] zes = {Slot.HOOFD, Slot.BORST, Slot.BENEN, Slot.VOETEN, Slot.HANDSCHOENEN, Slot.CAPE};
        for (int i = 0; i < 6; i++) {
            STUKKEN.put(DUINWANDELAAR.get(i), zes[i]);
            STUKKEN.put(DUINZIJDE.get(i), zes[i]);
            STUKKEN.put(STERRENWACHT.get(i), zes[i]);
        }
        STUKKEN.put("karavaanwachtwanten", Slot.HANDSCHOENEN);
        STUKKEN.put("karavaanwachtcape", Slot.CAPE);
        STUKKEN.put("karavaanwachtarmband", Slot.ARMBAND);
        STUKKEN.put("karavaanwachtlaarzen", Slot.VOETEN);
        STUKKEN.put("saffraanring", Slot.RING);
        STUKKEN.put("woestijnroosring", Slot.RING);
        STUKKEN.put("rozenketting", Slot.KETTING);
        STUKKEN.put("sterrenoorbel", Slot.OORBEL);
        STUKKEN.put("zoutkristalarmband", Slot.ARMBAND);
    }

    public static final String ROZENKROMZWAARD = "rozenkromzwaard", DADELPALMKNOTS = "dadelpalmknots", DUINVALKBOOG = "duinvalkboog",
            KARAVAANKRUISBOOG = "karavaankruisboog", SAFFRAANSTAF = "saffraanstaf", OASEFLUIT = "oasefluit";
    public static final List<String> WAPENS = List.of(ROZENKROMZWAARD, DADELPALMKNOTS, DUINVALKBOOG, KARAVAANKRUISBOOG, SAFFRAANSTAF, OASEFLUIT);
    /** Which specialisation style each weapon is made for (tooltip line vadsrijk.z6wezens.spec.&lt;spec&gt;). */
    public static final Map<String, String> SPEC = Map.of(ROZENKROMZWAARD, "schildridder", DADELPALMKNOTS, "reuzenkussen", DUINVALKBOOG,
            "scherpschutter", KARAVAANKRUISBOOG, "strikkenzetter", SAFFRAANSTAF, "sterrenwever", OASEFLUIT, "zegenzanger");

    public static final List<String> KRISTALLEN = List.of("rozenkwarts", "sterrenzout");
    public static final int ZONE6_LEVEL = 60;

    private Z6Loot() {
    }

    public static String itemId(String basis) {
        return "guhs:vr_z6_mob_" + basis;
    }

    private static void voegToe(Stats basis, Stats groei, Object... extra) {
        for (int i = 0; i + 1 < extra.length; i += 2) {
            Stat s = (Stat) extra[i];
            double v = ((Number) extra[i + 1]).doubleValue();
            basis.plus(s, v);
            groei.plus(s, v * (s.hoofd() ? 0.25 : 0.06));
        }
    }

    /** An armour piece (head/chest/legs/feet): ~1.12x zone 5 (lv 60-70 gear). */
    private static BasisType harnas(String id, double vd, double vads, Object... extra) {
        Stats basis = Stats.van(Stat.VERDEDIGING, vd, Stat.VADS, vads);
        Stats groei = Stats.van(Stat.VERDEDIGING, vd * 0.25, Stat.VADS, vads * 0.2);
        voegToe(basis, groei, extra);
        return new BasisType(id, itemId(id), STUKKEN.get(id), "harnas", null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    /** A virtual-slot piece (gloves, cape, armband, jewellery): only its own stats. */
    private static BasisType virtueel(String id, Object... stats) {
        Slot s = STUKKEN.get(id);
        String soort = s == Slot.HANDSCHOENEN ? "handschoenen" : s == Slot.CAPE ? "cape" : "sieraad";
        Stats basis = new Stats(), groei = new Stats();
        voegToe(basis, groei, stats);
        return new BasisType(id, itemId(id), s, soort, null, Element.GEEN, 0, 0, basis, groei, 3);
    }

    private static void wapen(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, "guhs:vr_z6_" + id, soort, klasse, e, schade, perLevel, basis, groei, 3));
    }

    /** Registers one piece, harness or virtual by its slot. */
    public static void stuk(String id, double vd, double vads, Object... extra) {
        BasisTypes.registreer(STUKKEN.get(id).harnas() ? harnas(id, vd, vads, extra) : virtueel(id, extra));
    }

    /** From Z6WezensRegistry.register. */
    public static void basisTypes() {
        // Duinwandelaar: wrapped linen in rose and sand, a turban with guh ears (Ridder + Jager)
        stuk("duinwandelaarstulband", 23, 66, Stat.KRACHT, 7, Stat.BEHENDIGHEID, 7, Stat.VERDEDIGING, 6);
        stuk("duinwandelaarsmantel", 37, 108, Stat.KRACHT, 11, Stat.VERDEDIGING, 10);
        stuk("duinwandelaarsbroek", 31, 92, Stat.BEHENDIGHEID, 11, Stat.KRITIEK, 3);
        stuk("duinwandelaarslaarzen", 19, 60, Stat.KRACHT, 6, Stat.LOOPSNELHEID, 6);
        stuk("duinwandelaarswanten", 0, 0, Stat.VERDEDIGING, 13, Stat.KRACHT, 7, Stat.BEHENDIGHEID, 7);
        stuk("duinwandelaarscape", 0, 0, Stat.VADS, 62, Stat.ELEMENTWEERSTAND, 6, Stat.LOOPSNELHEID, 3);
        // Duinzijde: rose silk with saffron trim (Magier)
        stuk("zijdetulband", 21, 64, Stat.WIJSHEID, 12, Stat.ELEMENTSCHADE, 5);
        stuk("zijdegewaad", 34, 104, Stat.WIJSHEID, 15, Stat.HEALKRACHT, 7);
        stuk("zijdepofbroek", 29, 90, Stat.WIJSHEID, 12, Stat.ELEMENTWEERSTAND, 6);
        stuk("zijdeslofjes", 19, 58, Stat.WIJSHEID, 9, Stat.COOLDOWNREDUCTIE, 3);
        stuk("zijdehandschoentjes", 0, 0, Stat.WIJSHEID, 9, Stat.ELEMENTSCHADE, 6, Stat.VERDEDIGING, 8);
        stuk("zijdesluier", 0, 0, Stat.VADS, 58, Stat.HEALKRACHT, 8, Stat.WIJSHEID, 6);
        // jewellery
        stuk("saffraanring", 0, 0, Stat.KRITIEK, 4, Stat.KRITIEKSCHADE, 9);
        stuk("woestijnroosring", 0, 0, Stat.VADS, 72, Stat.HEALKRACHT, 6);
        stuk("rozenketting", 0, 0, Stat.VERDEDIGING, 15, Stat.ELEMENTWEERSTAND, 5);
        stuk("sterrenoorbel", 0, 0, Stat.ELEMENTSCHADE, 11, Stat.COOLDOWNREDUCTIE, 3);
        stuk("zoutkristalarmband", 0, 0, Stat.AANVALSSNELHEID, 5, Stat.KRITIEK, 3);
        // weapons: ~6 % above zone 5 (7.2 + 1.44/lv), each with the side stats its specialisation style wants
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        wapen(ROZENKROMZWAARD, "zwaard", r, Element.ROZE, 7.2, 1.44, Stats.van(Stat.VERDEDIGING, 12, Stat.VADS, 120),
                Stats.van(Stat.KRACHT, 0.4, Stat.VERDEDIGING, 0.2));
        wapen(DADELPALMKNOTS, "knots", r, Element.NATUUR, 7.5, 1.50, Stats.van(Stat.KRACHT, 9, Stat.KRITIEKSCHADE, 14), Stats.van(Stat.KRACHT, 0.5));
        wapen(DUINVALKBOOG, "boog", j, Element.NATUUR, 7.3, 1.46, Stats.van(Stat.KRITIEK, 5, Stat.KRITIEKSCHADE, 10), Stats.van(Stat.BEHENDIGHEID, 0.45, Stat.KRITIEK, 0.05));
        wapen(KARAVAANKRUISBOOG, "kruisboog", j, Element.VUUR, 7.2, 1.44, Stats.van(Stat.AANVALSSNELHEID, 6, Stat.BEHENDIGHEID, 5),
                Stats.van(Stat.BEHENDIGHEID, 0.43));
        wapen(SAFFRAANSTAF, "staf", m, Element.VUUR, 7.3, 1.46, Stats.van(Stat.ELEMENTSCHADE, 12, Stat.WIJSHEID, 4), Stats.van(Stat.WIJSHEID, 0.45));
        wapen(OASEFLUIT, "staf", m, Element.WATER, 7.0, 1.40, Stats.van(Stat.WIJSHEID, 5, Stat.HEALKRACHT, 16), Stats.van(Stat.WIJSHEID, 0.42, Stat.HEALKRACHT, 0.16));
        List<LootTabel.Regel> w = new ArrayList<>();
        for (String id : WAPENS) {
            w.add(new LootTabel.Regel(id, 3));
        }
        LootTabellen.registreer("z6wezens:wapens", new LootTabel(w, 1, 1.0, 1.0, 0, 0));
    }

    private static List<LootTabel.Regel> regels(Object... paren) {
        List<LootTabel.Regel> l = new ArrayList<>();
        for (int i = 0; i + 1 < paren.length; i += 2) {
            l.add(new LootTabel.Regel((String) paren[i], (Integer) paren[i + 1]));
        }
        for (String s : WAPENS) {
            l.add(new LootTabel.Regel(s, 1));
        }
        return l;
    }

    private static void tabel(String mob, List<LootTabel.Regel> r, double kans, int muntMin, int muntMax) {
        LootTabellen.registreer("z6wezens:" + mob, new LootTabel(r, 1, kans, 1.4, muntMin, muntMax));
        List<LootTabel.Regel> elite = new ArrayList<>(r);
        for (String s : SIERADEN) {
            elite.add(new LootTabel.Regel(s, 2));
        }
        LootTabellen.registreer("z6wezens:" + mob + "_elite", new LootTabel(elite, 2, Math.min(1, kans * 3.5), 3.4, muntMin * 3, muntMax * 3));
    }

    /** The open-world pool: both sets (weight 3 each piece), a ring now and then. */
    private static List<LootTabel.Regel> open(String extra, int gewicht) {
        List<Object> p = new ArrayList<>();
        for (String s : DUINWANDELAAR) {
            p.add(s);
            p.add(3);
        }
        for (String s : DUINZIJDE) {
            p.add(s);
            p.add(3);
        }
        p.add(extra);
        p.add(gewicht);
        return regels(p.toArray());
    }

    /** One table per creature (after {@link #basisTypes}). */
    public static void tabellen() {
        tabel("zandvlooi", open("saffraanring", 1), 0.12, 20, 50);
        tabel("fennekguh", open("woestijnroosring", 1), 0.3, 90, 160);
        tabel("springpiep", open("saffraanring", 1), 0.3, 90, 160);
        tabel("duinkruiper", open("rozenketting", 1), 0.32, 95, 170);
        tabel("dadelaapje", open("woestijnroosring", 2), 0.34, 120, 200);
        tabel("gierguh", open("sterrenoorbel", 1), 0.32, 100, 180);
        tabel("spiegelslang", open("zoutkristalarmband", 2), 0.34, 105, 190);
        tabel("zoutkreeft", open("rozenketting", 2), 0.36, 110, 200);
        tabel("zandgeestje", open("sterrenoorbel", 2), 0.34, 105, 190);
        tabel("ruinewachter", open("zoutkristalarmband", 2), 0.4, 130, 230);
        tabel("mika_lampendover", open("saffraanring", 1), 0.34, 110, 200);
        tabel("mika_ringdrager", open("sterrenoorbel", 2), 0.42, 140, 250);
        tabel("gedoofd_sterretje", open("sterrenoorbel", 2), 0.38, 130, 230);
        tabel("zoutslang", open("zoutkristalarmband", 6), 1.0, 600, 900);
    }

    // ---- crystals, pets, star dust, feathers ----------------------------------------------------------------------------------

    /** From Z6WezensRegistry.register: the drop hook for everything a zone-6 foe gives besides its table. */
    public static void haken() {
        Haken.VERSLAGEN.add(v -> {
            VadsrijkMob m = v.mob();
            if (m.mobLevel() < ZONE6_LEVEL || !(Z6WezensPlekken.inZone(m.getX(), m.getZ()) || Z6WezensPlekken.inKerker(m.getX(), m.getY(), m.getZ())
                    || m instanceof Z6Wezen)) {
                return;
            }
            boolean baas = m instanceof VadsrijkBaas;
            int q = Kristallen.dropKwaliteit(m.elite(), baas);
            double kans = baas ? 1.0 : m.elite() ? 0.35 : 0.08;
            for (ServerPlayer p : v.helpers()) {
                if (p.getRandom().nextDouble() < kans) {
                    ItemStack s = Kristallen.stack(KRISTALLEN.get(p.getRandom().nextInt(KRISTALLEN.size())), q, 1);
                    LootBundel.laatVallen(p, m.position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), q);
                }
                int stof = baas ? 3 + p.getRandom().nextInt(4) : m instanceof Z6Wezen w && "zandgeestje".equals(w.naam()) ? 1 + p.getRandom().nextInt(2) : 0;
                if (stof > 0) {
                    ItemStack s = new ItemStack(Z6WezensRegistry.STERRENSTOF.get(), stof);
                    LootBundel.laatVallen(p, m.position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), 0);
                }
                if (m instanceof Z6Wezen w) {
                    if (w.huisdier() != null && p.getRandom().nextDouble() < w.huisdierKans() && !Huisdieren.heeft(p, w.huisdier())) {
                        Huisdieren.geef(p, w.huisdier());
                        p.sendSystemMessage(Component.translatable("vadsrijk.z6wezens.huisdier.gevonden",
                                Component.translatable("vadsrijk.huisdier." + w.huisdier() + ".naam")).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    if (w.leider() && p.getRandom().nextDouble() < 0.1) {
                        GoudenVeren.geef(p, 1);
                    }
                }
            }
        });
    }
}
