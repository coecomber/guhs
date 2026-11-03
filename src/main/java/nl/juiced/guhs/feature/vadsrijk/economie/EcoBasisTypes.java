package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;

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
 * The zone-1 base types the economie slice adds on top of kern's five (design 4: Klodderset = behendigheid/slijm, Zwamset =
 * wijsheid/sporen, the Pluis pieces that complete the ridder's set, a weapon or two per class, and the guhddelijke
 * Klodderkroon of Koning Klodder). Item ids {@code guhs:vr_item_<id>}. Loot tables for the wezens/bazen slices:
 * {@link #TABEL_KLODDER}, {@link #TABEL_ZWAM}, {@link #TABEL_ALLES}.
 */
public final class EcoBasisTypes {
    public static final String TABEL_KLODDER = "economie:klodder", TABEL_ZWAM = "economie:zwam", TABEL_ALLES = "economie:alles";

    /** id, slot, soort, klasse (null = everyone), element, weapon damage + per level, base stats + per level. */
    record Def(String id, Slot slot, String soort, String klasse, Element element, double schade, double schadePerLevel, Stats basis, Stats groei) {
    }

    public static final List<Def> ALLE = new ArrayList<>();

    static {
        // weapons
        wapen("klodderzwaard", "zwaard", Klassen.RIDDER, Element.WATER, 9, 1.7, Stat.KRACHT);
        wapen("pluisboog", "boog", Klassen.JAGER, Element.NATUUR, 7, 1.5, Stat.BEHENDIGHEID);
        wapen("klodderkatapult", "boog", Klassen.JAGER, Element.WATER, 8, 1.6, Stat.BEHENDIGHEID);
        wapen("vadsstaf", "staf", Klassen.MAGIER, Element.ROZE, 7, 1.5, Stat.WIJSHEID);
        wapen("zwamstaf", "staf", Klassen.MAGIER, Element.NATUUR, 8, 1.6, Stat.WIJSHEID);
        // the ridder's pluis pieces (kern has the pluishelm + knuffelharnas)
        harnas("pluisbroek", Slot.BENEN, 5, 15, null);
        harnas("pluissloffen", Slot.VOETEN, 3, 9, null);
        // the Klodderset (behendigheid, a little slime resistance)
        harnas("klodderhelm", Slot.HOOFD, 3, 10, Stat.BEHENDIGHEID);
        harnas("klodderjas", Slot.BORST, 5, 17, Stat.BEHENDIGHEID);
        harnas("klodderbroek", Slot.BENEN, 4, 13, Stat.BEHENDIGHEID);
        harnas("kloddersloffen", Slot.VOETEN, 2, 8, Stat.BEHENDIGHEID);
        // the Zwamset (wijsheid)
        harnas("zwamhoed", Slot.HOOFD, 2, 11, Stat.WIJSHEID);
        harnas("zwamgewaad", Slot.BORST, 4, 18, Stat.WIJSHEID);
        harnas("zwambroek", Slot.BENEN, 3, 13, Stat.WIJSHEID);
        harnas("zwamsloffen", Slot.VOETEN, 2, 8, Stat.WIJSHEID);
        // guhddelijk: Koning Klodder's crown (a real head piece; the cosmetic Klodderkroontje is separate)
        ALLE.add(new Def("klodderkroon", Slot.HOOFD, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 4, Stat.VADS, 20, Stat.KRACHT, 2, Stat.BEHENDIGHEID, 2, Stat.WIJSHEID, 2),
                Stats.van(Stat.VERDEDIGING, 1, Stat.VADS, 4, Stat.KRACHT, 0.3, Stat.BEHENDIGHEID, 0.3, Stat.WIJSHEID, 0.3)));
    }

    private static void wapen(String id, String soort, String klasse, Element e, double schade, double perLevel, Stat hoofd) {
        ALLE.add(new Def(id, Slot.WAPEN, soort, klasse, e, schade, perLevel, Stats.van(hoofd, 2), Stats.van(hoofd, 0.4)));
    }

    private static void harnas(String id, Slot slot, double verdediging, double vads, Stat extra) {
        Stats b = Stats.van(Stat.VERDEDIGING, verdediging, Stat.VADS, vads);
        Stats g = Stats.van(Stat.VERDEDIGING, verdediging * 0.25, Stat.VADS, vads * 0.2);
        if (extra != null) {
            b.plus(extra, 1);
            g.plus(extra, 0.25);
        }
        ALLE.add(new Def(id, slot, "harnas", null, Element.GEEN, 0, 0, b, g));
    }

    private EcoBasisTypes() {
    }

    public static String itemId(String id) {
        return "guhs:vr_item_" + id;
    }

    /** Registers the base types and the loot tables (from EconomieRegistry.register). */
    static void registreer() {
        for (Def d : ALLE) {
            BasisTypes.registreer(new BasisType(d.id(), itemId(d.id()), d.slot(), d.soort(), d.klasse(), d.element(), d.schade(), d.schadePerLevel(),
                    d.basis(), d.groei(), 3));
        }
        LootTabellen.registreer(TABEL_KLODDER, new LootTabel(List.of(r("klodderzwaard", 2), r("klodderkatapult", 2), r("klodderhelm", 3), r("klodderjas", 3),
                r("klodderbroek", 3), r("kloddersloffen", 3), r("pluisbroek", 1), r("pluissloffen", 1)), 1, 0.2, 1.0, 3, 12));
        LootTabellen.registreer(TABEL_ZWAM, new LootTabel(List.of(r("zwamstaf", 2), r("pluisboog", 2), r("zwamhoed", 3), r("zwamgewaad", 3),
                r("zwambroek", 3), r("zwamsloffen", 3), r("vadsstaf", 1)), 1, 0.2, 1.0, 5, 16));
        List<LootTabel.Regel> alles = new ArrayList<>();
        for (Def d : ALLE) {
            if (!d.id().equals("klodderkroon")) {
                alles.add(r(d.id(), 2));
            }
        }
        LootTabellen.registreer(TABEL_ALLES, new LootTabel(alles, 1, 0.25, 1.2, 4, 14));
    }

    private static LootTabel.Regel r(String id, int w) {
        return new LootTabel.Regel(id, w);
    }
}
