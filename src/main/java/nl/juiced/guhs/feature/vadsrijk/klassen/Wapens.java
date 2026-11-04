package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;

/**
 * The class weapon base types (4 per class incl. kern's 3 Knuffelridder weapons). Every one has an element. Fix round 1:
 * {@code schade + perLevel x level} is the damage PER SECOND; one hit = that x the class factor x the weapon's interval
 * (kern.Aanvalstempo), see NOTITIES_FIX_GEVECHT.md.
 * Also a loot table "klassen:wapens" with all 13 class weapons (for the economie/wezens slices, e.g. a class-weapon chest).
 */
public final class Wapens {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final String KLODDERKLING = "klodderkling", PLUISBOOG = "pluisboog", DRUPPELBOOG = "druppelboog", SNOEPKRUISBOOG = "snoepkruisboog",
            EIKELKATAPULT = "eikelkatapult", ROZENSTAF = "rozenstaf", VLAMMENSTAF = "vlammenstaf", ZWAMSTOKJE = "zwamstokje", SLIJMBOEK = "slijmboek";

    public static final List<String> ALLE = List.of(KLODDERKLING, PLUISBOOG, DRUPPELBOOG, SNOEPKRUISBOOG, EIKELKATAPULT, ROZENSTAF, VLAMMENSTAF,
            ZWAMSTOKJE, SLIJMBOEK);

    static {
        for (String id : ALLE) {
            ITEMS.registerItem("vr_kl_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
        }
    }

    private Wapens() {
    }

    static void registreer() {
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        // Knuffelridder (kern has kussenzwaard/knuffelknots/pluishamer): a lifesteal sword
        w(KLODDERKLING, "zwaard", r, Element.WATER, 5.2, 1.05, Stats.van(Stat.LEVENSROOF, 3), Stats.van(Stat.KRACHT, 0.3, Stat.LEVENSROOF, 0.1));
        // Pluisjager: interval boog 1.1 s, kruisboog 2.0 s, katapult 1.6 s (kern.Aanvalstempo)
        w(PLUISBOOG, "boog", j, Element.ROZE, 5.5, 1.1, Stats.van(Stat.BEHENDIGHEID, 2), Stats.van(Stat.BEHENDIGHEID, 0.4));
        w(DRUPPELBOOG, "boog", j, Element.WATER, 5.5, 1.1, Stats.van(Stat.KRITIEK, 3), Stats.van(Stat.BEHENDIGHEID, 0.3, Stat.KRITIEK, 0.1));
        w(SNOEPKRUISBOOG, "kruisboog", j, Element.VUUR, 5.7, 1.15, Stats.van(Stat.KRITIEKSCHADE, 10), Stats.van(Stat.BEHENDIGHEID, 0.3));
        w(EIKELKATAPULT, "katapult", j, Element.NATUUR, 5.2, 1.05, Stats.van(Stat.AANVALSSNELHEID, 4), Stats.van(Stat.BEHENDIGHEID, 0.3));
        // Vadsmagier: interval staf 1.3 s, toverstok 0.9 s, boek 1.6 s; the staff element is the Roze Vuurbal's element
        w(ROZENSTAF, "staf", m, Element.ROZE, 5.5, 1.1, Stats.van(Stat.WIJSHEID, 2), Stats.van(Stat.WIJSHEID, 0.4));
        w(VLAMMENSTAF, "staf", m, Element.VUUR, 5.5, 1.1, Stats.van(Stat.ELEMENTSCHADE, 6), Stats.van(Stat.WIJSHEID, 0.3));
        w(ZWAMSTOKJE, "toverstok", m, Element.NATUUR, 5.5, 1.1, Stats.van(Stat.COOLDOWNREDUCTIE, 4), Stats.van(Stat.WIJSHEID, 0.3));
        w(SLIJMBOEK, "boek", m, Element.WATER, 5.3, 1.05, Stats.van(Stat.HEALKRACHT, 8), Stats.van(Stat.WIJSHEID, 0.3, Stat.HEALKRACHT, 0.2));
        List<LootTabel.Regel> regels = new java.util.ArrayList<>();
        for (String id : List.of("kussenzwaard", "knuffelknots", "pluishamer")) {
            regels.add(new LootTabel.Regel(id, 3));
        }
        for (String id : ALLE) {
            regels.add(new LootTabel.Regel(id, 3));
        }
        LootTabellen.registreer("klassen:wapens", new LootTabel(regels, 1, 1.0, 1.0, 0, 0));
    }

    private static void w(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, "guhs:vr_kl_" + id, soort, klasse, e, schade, perLevel, basis, groei, 3));
    }
}
