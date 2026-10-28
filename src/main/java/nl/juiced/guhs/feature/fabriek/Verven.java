package nl.juiced.guhs.feature.fabriek;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Dyeing by the recipes the game already has (1.8.0, the Verfbad and the Bouwblokken-fabriek): first one block + the dye side
 * by side (wool, beds, knuffelblokken, everything that is dyed one at a time), then eight blocks around the dye (glass,
 * terracotta, candles...). Whatever a datapack adds dyes along by itself.
 */
public final class Verven {
    /** What dyeing gives, and how many of the block it takes. */
    public record Uitkomst(ItemStack resultaat, int blokken) {
    }

    /** Pairs already asked (block item, dye item): the machines ask every tick. Forgotten on a /reload. */
    private static final Map<Long, Optional<Uitkomst>> ONTHOUDEN = new ConcurrentHashMap<>();

    private Verven() {
    }

    public static void vergeet() {
        ONTHOUDEN.clear();
    }

    /** What this block with this dye becomes (null: no dye recipe for them). Kept per pair of items. */
    @Nullable
    public static Uitkomst van(ServerLevel level, ItemStack blok, ItemStack verf) {
        if (blok.isEmpty() || verf.isEmpty()) {
            return null;
        }
        long sleutel = (long) BuiltInRegistries.ITEM.getId(blok.getItem()) << 32 | BuiltInRegistries.ITEM.getId(verf.getItem());
        return ONTHOUDEN.computeIfAbsent(sleutel, k -> Optional.ofNullable(zoek(level, blok, verf))).orElse(null);
    }

    @Nullable
    private static Uitkomst zoek(ServerLevel level, ItemStack blok, ItemStack verf) {
        ItemStack b = blok.copyWithCount(1), v = verf.copyWithCount(1);
        for (CraftingInput in : List.of(CraftingInput.of(2, 1, List.of(b, v)), CraftingInput.of(2, 1, List.of(v, b)))) {
            ItemStack uit = maak(level, in);
            if (!uit.isEmpty() && !ItemStack.isSameItem(uit, b)) {
                return new Uitkomst(uit, 1);
            }
        }
        List<ItemStack> rond = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            rond.add(i == 4 ? v : b.copy());
        }
        ItemStack uit = maak(level, CraftingInput.of(3, 3, rond));
        return uit.isEmpty() || ItemStack.isSameItem(uit, b) ? null : new Uitkomst(uit, 8);
    }

    private static ItemStack maak(ServerLevel level, CraftingInput in) {
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
    }
}
