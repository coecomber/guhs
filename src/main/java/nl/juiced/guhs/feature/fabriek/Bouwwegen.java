package nl.juiced.guhs.feature.fabriek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Every way the Bouwblokken-fabriek knows to turn ONE kind of block into another (1.8.0): the stonecutter recipes, the
 * furnace (gladde varianten) and the crafting recipes made of one single kind of item (trappen, platen, muurtjes,
 * bakstenen, tegels: 6 blocks are 4 stairs). Read from the game's own recipes, so a datapack's blocks join in. Kept per
 * server; the colours are {@link Verven}.
 */
public final class Bouwwegen {
    /** In: {@code nodig} of one item that every one of the {@code in} ingredients accepts; out: {@code uit} of the target. */
    public record Weg(List<Ingredient> in, int uit) {
        public int nodig() {
            return in.size();
        }

        public boolean neemt(ItemStack bron) {
            if (bron.isEmpty()) {
                return false;
            }
            for (Ingredient i : in) {
                if (!i.test(bron)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static MinecraftServer van;
    private static Map<Item, List<Weg>> wegen = Map.of();

    private Bouwwegen() {
    }

    /** The ways to the target item (empty: none). */
    public static List<Weg> naar(ServerLevel level, Item doel) {
        if (van != level.getServer()) {
            van = level.getServer();
            wegen = lees(level);
        }
        return wegen.getOrDefault(doel, List.of());
    }

    /** Forget the ways (a /reload). */
    public static void vergeet() {
        van = null;
    }

    private static Map<Item, List<Weg>> lees(ServerLevel level) {
        Map<Item, List<Weg>> m = new HashMap<>();
        for (var houder : level.recipeAccess().getRecipes()) {
            Optional<Weg> weg = Optional.empty();
            ItemStack uit = ItemStack.EMPTY;
            if (houder.value() instanceof StonecutterRecipe || houder.value() instanceof SmeltingRecipe) {
                SingleItemRecipe r = (SingleItemRecipe) houder.value();
                uit = r.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                weg = Optional.of(new Weg(List.of(r.input()), uit.getCount()));
            } else if (houder.value() instanceof CraftingRecipe r && (r instanceof ShapedRecipe || r instanceof ShapelessRecipe)) {
                List<Ingredient> in = new ArrayList<>(r.placementInfo().ingredients());
                if (in.isEmpty() || in.size() > 9) {
                    continue;
                }
                uit = r.assemble(CraftingInput.EMPTY);
                weg = Optional.of(new Weg(List.copyOf(in), uit.getCount()));
            }
            if (weg.isPresent() && !uit.isEmpty()) {
                m.computeIfAbsent(uit.getItem(), k -> new ArrayList<>()).add(weg.get());
            }
        }
        return m;
    }
}
