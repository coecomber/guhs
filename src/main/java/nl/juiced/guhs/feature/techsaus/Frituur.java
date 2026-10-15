package nl.juiced.guhs.feature.techsaus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.registry.ModItems;

/**
 * What the Frituurautomaat fries: the same snacks as the frying pan you fill with Mika's vet by hand ({@code block.FryingPanBlock}:
 * kaas knabbels become gefrituurde kaasknabbels, a guh fish becomes a gebakken guh vis). Another part of the mod adds its
 * own snack with {@link #zet} from its {@code register}.
 */
public final class Frituur {
    private static final Map<Supplier<? extends Item>, Supplier<? extends Item>> SNACKS = new ConcurrentHashMap<>();

    static {
        zet(ModItems.KAAS_KNABBELS, ModItems.GEFRITUURDE_KAASKNABBELS);
        zet(ModItems.GUH_VIS, ModItems.GEBAKKEN_GUH_VIS);
    }

    /** From now on the Frituurautomaat fries {@code rauw} into {@code gefrituurd} (one for one). */
    public static void zet(Supplier<? extends Item> rauw, Supplier<? extends Item> gefrituurd) {
        SNACKS.put(rauw, gefrituurd);
    }

    /** What this becomes in the frituur (EMPTY: it is not something to fry). */
    public static ItemStack resultaat(ItemStack rauw) {
        if (rauw.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (Map.Entry<Supplier<? extends Item>, Supplier<? extends Item>> snack : SNACKS.entrySet()) {
            if (rauw.is(snack.getKey().get())) {
                return new ItemStack(snack.getValue().get());
            }
        }
        return ItemStack.EMPTY;
    }

    private Frituur() {
    }
}
