package nl.juiced.guhs.feature.dromen;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;

/**
 * Which island a dream starts on: what you gave your guh when you tucked it in (ontwerp ronde 4). Item tags
 * {@code guhs:droom_start/<island>} (data/guhs/tags/item/droom_start/; tools/features/dromen.py), so other parts add their
 * own things (part D's droomzand, part B's maanlampjes) without touching this class:
 * <ul>
 *   <li>badje: a badeendje, a zeepje;</li>
 *   <li>sterren: the sterrenlantaarn, a sterrenkoekje, the sterrenhemel-knuffel;</li>
 *   <li>herinnering: a roze guh koek, an ansichtkaart, a knabbelkoekje;</li>
 *   <li>molentje: droomzand, a lampion (a little moon-light);</li>
 *   <li>liedjes: a knuffel or a guhkussen;</li>
 *   <li>nothing (or anything else): the Droomplein.</li>
 * </ul>
 * When an item is in more than one tag, the first in {@link #VOLGORDE} wins.
 */
public final class DroomStart {
    /** The order the tags are asked in (the specific ones first, the many knuffels last). */
    public static final List<String> VOLGORDE = List.of(DroomEilanden.BADJE, DroomEilanden.STERREN, DroomEilanden.HERINNERING,
            DroomEilanden.MOLENTJE, DroomEilanden.LIEDJES);

    private DroomStart() {
    }

    public static TagKey<Item> tag(String eiland) {
        return TagKey.create(Registries.ITEM, Guhs.id("droom_start/" + eiland));
    }

    /** The island a dream with this item starts on ({@link DroomEilanden#PLEIN} for nothing or anything else). */
    public static String eilandVoor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DroomEilanden.PLEIN;
        }
        for (String eiland : VOLGORDE) {
            if (stack.is(tag(eiland))) {
                return eiland;
            }
        }
        return DroomEilanden.PLEIN;
    }
}
