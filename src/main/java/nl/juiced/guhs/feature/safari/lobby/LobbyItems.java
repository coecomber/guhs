package nl.juiced.guhs.feature.safari.lobby;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.effect.MobEffects;
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

    // (test round 3, J3) the ijskraam's animal ice creams and the kampvuur's roasted marshmallow: always edible (nobody is
    // hungry in the safari park), each with a small effect for the walk
    public static final DeferredItem<IJsjeItem> PANDAIJSJE = ijsje("safari_pandaijsje", () -> MobEffects.SPEED, 20 * 60);
    public static final DeferredItem<IJsjeItem> ZEBRASTREEP = ijsje("safari_zebrastreep", () -> MobEffects.JUMP_BOOST, 20 * 60);
    public static final DeferredItem<IJsjeItem> PINGUINIJSJE = ijsje("safari_pinguinijsje", () -> MobEffects.DOLPHINS_GRACE, 20 * 60);
    public static final DeferredItem<IJsjeItem> FLAMINGOSORBET = ijsje("safari_flamingosorbet", () -> MobEffects.NIGHT_VISION, 20 * 120);
    public static final DeferredItem<IJsjeItem> MARSHMALLOW = ijsje("safari_marshmallow", null, 0);
    /** The ijskraam's menu, in the order of the ijsverkoper's options. */
    public static final List<DeferredItem<IJsjeItem>> IJSJES = List.of(PANDAIJSJE, ZEBRASTREEP, PINGUINIJSJE, FLAMINGOSORBET);

    private static DeferredItem<IJsjeItem> ijsje(String id, java.util.function.Supplier<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> effect,
                                                 int ticks) {
        return ITEMS.registerItem(id, p -> new IJsjeItem(p, effect, ticks),
                () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).alwaysEdible().build()));
    }

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
        IJSJES.forEach(i -> out.accept(new ItemStack(i.get())));
        out.accept(new ItemStack(MARSHMALLOW.get()));
    }

    private LobbyItems() {
    }
}
