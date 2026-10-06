package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Reading and making Bouwtekeningen: the public entry points for anything that wants to know what a drawing says
 * (the Knutselmachine; the Bestelguh of the next update).
 * <pre>
 * Bouwtekening t = Bouwtekeningen.lees(stack);          // null: not a drawing, or an empty sheet
 * t.resultaat();  t.ingredienten();  t.rooster();       // what it makes, what one craft needs
 * Bouwtekeningen.recept(level, t);                      // the crafting recipe behind it, as it is NOW (null: gone)
 * ItemStack tekening = Bouwtekeningen.teken(level, grid);   // a new drawing of what these 9 stacks craft (empty: nothing)
 * </pre>
 */
public final class Bouwtekeningen {
    /** What is drawn on this stack; null when it is not a Bouwtekening or nothing is drawn on it yet. */
    @Nullable
    public static Bouwtekening lees(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(TechmachineFeature.TEKENING.get());
    }

    /** Is this an empty sheet (a Bouwtekening item without a drawing)? */
    public static boolean isLeeg(ItemStack stack) {
        return stack.is(TechmachineFeature.BOUWTEKENING.get()) && !stack.has(TechmachineFeature.TEKENING.get());
    }

    /**
     * The crafting recipe a drawing stands for, as the server knows it now: the recipe with the drawn id when it still
     * fits the grid, else whichever recipe fits the grid; null when nothing crafts from this grid any more.
     */
    @Nullable
    public static RecipeHolder<CraftingRecipe> recept(ServerLevel level, Bouwtekening tekening) {
        CraftingInput invoer = tekening.invoer();
        if (tekening.recept().isPresent()) {
            RecipeHolder<?> bekend = level.recipeAccess().byKey(tekening.recept().get()).orElse(null);
            if (bekend != null && bekend.value() instanceof CraftingRecipe recept && recept.matches(invoer, level)) {
                @SuppressWarnings("unchecked")
                RecipeHolder<CraftingRecipe> houder = (RecipeHolder<CraftingRecipe>) bekend;
                return houder;
            }
        }
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, invoer, level).orElse(null);
    }

    /**
     * What these stacks craft when they lie on a 3 x 3 grid (row by row; shorter lists are filled up with empty cells),
     * as a drawing; null when no recipe fits or its result is empty.
     */
    @Nullable
    public static Bouwtekening maak(ServerLevel level, List<ItemStack> rooster) {
        List<ItemStack> cellen = new ArrayList<>(Bouwtekening.VAKJES);
        for (int i = 0; i < Bouwtekening.VAKJES; i++) {
            ItemStack cel = i < rooster.size() ? rooster.get(i) : ItemStack.EMPTY;
            cellen.add(cel.isEmpty() ? ItemStack.EMPTY : cel.copyWithCount(1));
        }
        CraftingInput invoer = CraftingInput.of(Bouwtekening.BREED, Bouwtekening.BREED, new ArrayList<>(cellen));
        Optional<RecipeHolder<CraftingRecipe>> recept = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, invoer, level);
        if (recept.isEmpty()) {
            return null;
        }
        ItemStack resultaat = recept.get().value().assemble(invoer);
        if (resultaat.isEmpty() || !resultaat.isItemEnabled(level.enabledFeatures())) {
            return null;
        }
        return new Bouwtekening(cellen, resultaat.copy(), Optional.of(recept.get().id()));
    }

    /** A Bouwtekening item with this drawing on it. */
    public static ItemStack metTekening(Bouwtekening tekening) {
        ItemStack stack = new ItemStack(TechmachineFeature.BOUWTEKENING.get());
        stack.set(TechmachineFeature.TEKENING.get(), tekening);
        // (the item model shows a drawn sheet: tools/features/tech_machines.py, custom_model_data 1)
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1f), List.of(), List.of(), List.of()));
        return stack;
    }

    /** {@link #maak} + {@link #metTekening}: the drawn item, or an empty stack when these stacks craft nothing. */
    public static ItemStack teken(ServerLevel level, List<ItemStack> rooster) {
        Bouwtekening tekening = maak(level, rooster);
        return tekening == null ? ItemStack.EMPTY : metTekening(tekening);
    }

    private Bouwtekeningen() {
    }
}
