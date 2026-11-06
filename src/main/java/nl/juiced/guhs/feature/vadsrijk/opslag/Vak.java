package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * T-OPSLAG: one tab of a knapzak or the bank: a plain container that lives in {@link OpslagData} (the menu edits it directly).
 */
public final class Vak extends SimpleContainer {
    public Vak(int grootte) {
        super(grootte);
    }

    /** How many slots hold something. */
    public int gevuld() {
        int n = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Merges equal stacks and sorts by item id, then name. Counts are kept exactly (tested). */
    public void sorteer() {
        List<ItemStack> alle = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack s = removeItemNoUpdate(i);
            if (s.isEmpty()) {
                continue;
            }
            for (ItemStack a : alle) {
                if (ItemStack.isSameItemSameComponents(a, s) && a.getCount() < a.getMaxStackSize()) {
                    int k = Math.min(s.getCount(), a.getMaxStackSize() - a.getCount());
                    a.grow(k);
                    s.shrink(k);
                    if (s.isEmpty()) {
                        break;
                    }
                }
            }
            if (!s.isEmpty()) {
                alle.add(s);
            }
        }
        alle.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> s.getHoverName().getString()).thenComparing(s -> -s.getCount()));
        for (int i = 0; i < alle.size(); i++) {
            setItem(i, alle.get(i));   // (as many kinds as there were slots, never more: merging only shrinks the list)
        }
        setChanged();
    }
}
