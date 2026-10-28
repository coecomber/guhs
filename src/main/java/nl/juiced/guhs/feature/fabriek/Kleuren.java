package nl.juiced.guhs.feature.fabriek;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * (1.8.0) Which dye a flower is for the Kleurmolen, and the dye of a colour for the Regenboogpers. A flower gives what its
 * own crafting recipe gives (vanilla and the guhs ones: kaasbloem, zingbloemetjes, oogbloempjes...); the guhs plants
 * without one get a colour of their own here, so that all sixteen colours grow back:
 * maanbloemetje = zwart (the night), kaasriet = groen, guhbloemetje = limoen, knabbelkoorbloem = cyaan,
 * plus cocoa beans (bruin) and cactus (groen).
 */
public final class Kleuren {
    /** The plants that get a colour from the Kleurmolen without a crafting recipe of their own. */
    public static final Map<String, DyeColor> EIGEN = Map.of(
            "guhs:maanbloemetje", DyeColor.BLACK,
            "guhs:kaasriet", DyeColor.GREEN,
            "guhs:guhbloemetje", DyeColor.LIME,
            "guhs:knabbelkoorbloem", DyeColor.CYAN,
            "minecraft:cocoa_beans", DyeColor.BROWN,
            "minecraft:cactus", DyeColor.GREEN);

    /** What a flower is (asked every tick): forgotten on a /reload. */
    private static final Map<Item, ItemStack> ONTHOUDEN = new ConcurrentHashMap<>();

    private Kleuren() {
    }

    public static void vergeet() {
        ONTHOUDEN.clear();
    }

    /** The dye of this colour (white_dye, ...). */
    public static Item kleurstof(DyeColor kleur) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(kleur.getName() + "_dye"));
    }

    /** The colour of this dye item (null: not one of the sixteen dyes). */
    @javax.annotation.Nullable
    public static DyeColor kleur(ItemStack stack) {
        for (DyeColor k : DyeColor.values()) {
            if (stack.is(kleurstof(k))) {
                return k;
            }
        }
        return null;
    }

    /** May this go into the Kleurmolen (a flower or one of {@link #EIGEN})? */
    public static boolean maalbaar(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.FLOWERS) || EIGEN.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
    }

    /** What the flower is as dye BEFORE the bonus (its recipe's count), or empty. */
    public static ItemStack van(ServerLevel level, ItemStack bloem) {
        if (!maalbaar(bloem)) {
            return ItemStack.EMPTY;
        }
        DyeColor eigen = EIGEN.get(BuiltInRegistries.ITEM.getKey(bloem.getItem()).toString());
        if (eigen != null) {
            return new ItemStack(kleurstof(eigen));
        }
        return ONTHOUDEN.computeIfAbsent(bloem.getItem(), i -> {
            CraftingInput in = CraftingInput.of(1, 1, List.of(bloem.copyWithCount(1)));
            ItemStack uit = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
            return kleur(uit) != null ? uit : ItemStack.EMPTY;
        }).copy();
    }
}
