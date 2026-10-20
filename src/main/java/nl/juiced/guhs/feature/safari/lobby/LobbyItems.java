package nl.juiced.guhs.feature.safari.lobby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The lobby's own items: the aaihoek foods that vanilla does not have (komkommer, paprika) and the eight zone badges the
 * experts hand out when a zone of your Safari-dex is complete.
 */
public final class LobbyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Item> KOMKOMMER = ITEMS.registerItem("safari_komkommer", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final DeferredItem<Item> PAPRIKA = ITEMS.registerItem("safari_paprika", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()));

    /** zone -> its badge ("safari_badge_savanne"...). */
    public static final Map<String, DeferredItem<Item>> BADGES = new LinkedHashMap<>();

    static {
        for (String zone : List.of("savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie")) {
            BADGES.put(zone, ITEMS.registerItem("safari_badge_" + zone, Item::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
        }
    }

    static void creative(Consumer<ItemStack> out) {
        out.accept(new ItemStack(KOMKOMMER.get()));
        out.accept(new ItemStack(PAPRIKA.get()));
        BADGES.values().forEach(b -> out.accept(new ItemStack(b.get())));
    }

    private LobbyItems() {
    }
}
