package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
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
 * Zone-3 class weapons (lv 30-50, ZONE3_PLAN §9.4): two per class, items {@code guhs:vr_z3_<id>}. Numbers in the per-weapon
 * DPS-rating model (kern.Aanvalstempo: {@code schade + perLevel x level} = damage per second, the interval from the weapon kind
 * or {@link Aanvalstempo#TYPE}): ~7 % above the zone-2 weapons (6.3 + 1.26/lv against 5.9 + 1.18/lv), plus a zone-3 side stat.
 * <ul>
 *   <li>Ridder: Takkenknots (knots 1.4 s, Natuur, Verdediging), Schorshamer (hamer 2.4 s, Vuur, Kracht).</li>
 *   <li>Jager: Liaanboog (boog 1.1 s, Natuur, Loopsnelheid), Raatkruisboog (kruisboog 2.0 s, Water... honey, Kritiekschade).</li>
 *   <li>Magier: Bloesemstaf (staf 1.3 s, Roze, Heal-kracht), Lantaarnstaf (staf, own interval 1.6 s, Vuur, Elementschade).</li>
 * </ul>
 * Loot table {@code z3systemen:wapens} (all six, for the z3-wezens / z3-bazen tables).
 */
public final class Z3Wapens {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final String TAKKENKNOTS = "takkenknots", SCHORSHAMER = "schorshamer", LIAANBOOG = "liaanboog", RAATKRUISBOOG = "raatkruisboog",
            BLOESEMSTAF = "bloesemstaf", LANTAARNSTAF = "lantaarnstaf";
    public static final List<String> ALLE = List.of(TAKKENKNOTS, SCHORSHAMER, LIAANBOOG, RAATKRUISBOOG, BLOESEMSTAF, LANTAARNSTAF);

    static {
        for (String id : ALLE) {
            ITEMS.registerItem("vr_z3_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
        }
    }

    private Z3Wapens() {
    }

    static void registreer() {
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        w(TAKKENKNOTS, "knots", r, Element.NATUUR, 6.3, 1.26, Stats.van(Stat.VERDEDIGING, 6, Stat.VADS, 80), Stats.van(Stat.KRACHT, 0.35, Stat.VERDEDIGING, 0.15));
        w(SCHORSHAMER, "hamer", r, Element.VUUR, 6.5, 1.30, Stats.van(Stat.KRACHT, 5, Stat.KRITIEKSCHADE, 10), Stats.van(Stat.KRACHT, 0.42));
        w(LIAANBOOG, "boog", j, Element.NATUUR, 6.3, 1.26, Stats.van(Stat.BEHENDIGHEID, 3, Stat.LOOPSNELHEID, 4), Stats.van(Stat.BEHENDIGHEID, 0.4, Stat.KRITIEK, 0.05));
        w(RAATKRUISBOOG, "kruisboog", j, Element.WATER, 6.5, 1.30, Stats.van(Stat.KRITIEKSCHADE, 15), Stats.van(Stat.BEHENDIGHEID, 0.36));
        w(BLOESEMSTAF, "staf", m, Element.ROZE, 6.2, 1.24, Stats.van(Stat.WIJSHEID, 3, Stat.HEALKRACHT, 12), Stats.van(Stat.WIJSHEID, 0.36, Stat.HEALKRACHT, 0.15));
        w(LANTAARNSTAF, "staf", m, Element.VUUR, 6.4, 1.28, Stats.van(Stat.ELEMENTSCHADE, 8), Stats.van(Stat.WIJSHEID, 0.4));
        Aanvalstempo.TYPE.put(LANTAARNSTAF, 1.6);
        List<LootTabel.Regel> regels = new ArrayList<>();
        for (String id : ALLE) {
            regels.add(new LootTabel.Regel(id, 3));
        }
        LootTabellen.registreer("z3systemen:wapens", new LootTabel(regels, 1, 1.0, 1.0, 0, 0));
    }

    private static void w(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, "guhs:vr_z3_" + id, soort, klasse, e, schade, perLevel, basis, groei, 3));
    }
}
