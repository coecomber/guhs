package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.ArrayList;
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
 * Zone-2 class weapons (lv 20-35): two per class, items {@code guhs:vr_z2_<id>}. Numbers follow the fix-round-1 damage model
 * (f-gevecht, kern.Aanvalstempo): {@code schade + perLevel x level} is the damage PER SECOND rating, the attack interval comes from
 * the weapon kind (zwaard 1.0 s, hamer 2.4 s, boog 1.1 s, kruisboog 2.0 s, staf 1.3 s, boek 1.6 s). The ratings are ~7 % above
 * the zone-1 weapons of the same level, plus a zone-2 side stat. Loot table {@code z2systemen:wapens} (all six, for the
 * z2-wezens / z2-bazen tables).
 * <p>Before integration with f-gevecht (old model, per hit) these numbers are too low per hit; that is expected.
 * TODO integration: if a weapon should get its own interval, add it to {@code Aanvalstempo.TYPE} (e.g. galmhamer 3.0 s).
 */
public final class Z2Wapens {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final String KRISTALKLING = "kristalkling", GALMHAMER = "galmhamer", IJSPEESBOOG = "ijspeesboog",
            SPLINTERKRUISBOOG = "splinterkruisboog", KRISTALSTAF = "kristalstaf", POOLLICHTBOEK = "poollichtboek";
    public static final List<String> ALLE = List.of(KRISTALKLING, GALMHAMER, IJSPEESBOOG, SPLINTERKRUISBOOG, KRISTALSTAF, POOLLICHTBOEK);

    static {
        for (String id : ALLE) {
            ITEMS.registerItem("vr_z2_" + id, RijkItem::new, () -> new Item.Properties().stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
        }
    }

    private Z2Wapens() {
    }

    static void registreer() {
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        w(KRISTALKLING, "zwaard", r, Element.WATER, 5.9, 1.18, Stats.van(Stat.KRITIEK, 3), Stats.van(Stat.KRACHT, 0.35, Stat.KRITIEK, 0.05));
        w(GALMHAMER, "hamer", r, Element.VUUR, 6.0, 1.2, Stats.van(Stat.KRACHT, 4, Stat.VERDEDIGING, 4), Stats.van(Stat.KRACHT, 0.4));
        w(IJSPEESBOOG, "boog", j, Element.WATER, 5.9, 1.18, Stats.van(Stat.BEHENDIGHEID, 3), Stats.van(Stat.BEHENDIGHEID, 0.4, Stat.KRITIEK, 0.05));
        w(SPLINTERKRUISBOOG, "kruisboog", j, Element.NATUUR, 6.1, 1.22, Stats.van(Stat.KRITIEKSCHADE, 12), Stats.van(Stat.BEHENDIGHEID, 0.35));
        w(KRISTALSTAF, "staf", m, Element.ROZE, 5.9, 1.18, Stats.van(Stat.WIJSHEID, 3), Stats.van(Stat.WIJSHEID, 0.4));
        w(POOLLICHTBOEK, "boek", m, Element.VUUR, 5.8, 1.16, Stats.van(Stat.HEALKRACHT, 10, Stat.ELEMENTSCHADE, 5),
                Stats.van(Stat.WIJSHEID, 0.35, Stat.HEALKRACHT, 0.15));
        List<LootTabel.Regel> regels = new ArrayList<>();
        for (String id : ALLE) {
            regels.add(new LootTabel.Regel(id, 3));
        }
        LootTabellen.registreer("z2systemen:wapens", new LootTabel(regels, 1, 1.0, 1.0, 0, 0));
    }

    private static void w(String id, String soort, String klasse, Element e, double schade, double perLevel, Stats basis, Stats groei) {
        BasisTypes.registreer(BasisType.wapen(id, "guhs:vr_z2_" + id, soort, klasse, e, schade, perLevel, basis, groei, 3));
    }
}
