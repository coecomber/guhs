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
    /** How many normal stacks of one thing fit in one slot (1 = a plain chest slot; the knapzak: {@link Knapzak#STAPEL_FACTOR}). */
    private final int stapelFactor;

    public Vak(int grootte) {
        this(grootte, 1);
    }

    public Vak(int grootte, int stapelFactor) {
        super(grootte);
        this.stapelFactor = Math.max(1, stapelFactor);
    }

    /**
     * The most of this item one slot holds with this factor: the item's own stack size x factor (64 -> 640, 16 -> 160). Things
     * that do not stack at all (gear, bundles) stay at 1: a "stack" of them would share one set of components, so anything that
     * changes one of them would change them all.
     */
    public static int plafond(ItemStack s, int factor) {
        int m = s.getMaxStackSize();
        return factor <= 1 || m <= 1 ? m : m * factor;
    }

    public int stapelFactor() {
        return stapelFactor;
    }

    @Override
    public int getMaxStackSize() {
        return super.getMaxStackSize() * stapelFactor;
    }

    /** (setItem cuts every stack down to this, so it must be the big limit here too) */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return plafond(stack, stapelFactor);
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
                if (ItemStack.isSameItemSameComponents(a, s) && a.getCount() < getMaxStackSize(a)) {
                    int k = Math.min(s.getCount(), getMaxStackSize(a) - a.getCount());
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
