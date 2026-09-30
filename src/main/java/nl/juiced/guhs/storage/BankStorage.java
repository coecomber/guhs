package nl.juiced.guhs.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;

/**
 * The Bank Guh's stomach: unlimited item types, up to Long.MAX_VALUE of each.
 * Items with different components (names, enchantments, damage...) are stored separately.
 */
public class BankStorage {
    /** Wraps a count-1 stack so it can be a map key (same item + same components = same key). */
    private record Key(ItemStack stack) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }

    private final Map<Key, Long> items = new LinkedHashMap<>();
    private int version;
    private final Runnable onChange;

    public BankStorage(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Puts the whole stack in (it's infinite, so it always fits). The passed stack is left untouched. */
    public void insert(ItemStack stack) {
        insert(stack, stack.getCount());
    }

    public void insert(ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0) {
            return;
        }
        Key key = new Key(stack.copyWithCount(1));
        items.merge(key, amount, (a, b) -> {
            long sum = a + b;
            return sum < 0 ? Long.MAX_VALUE : sum; // overflow guard, just in case
        });
        changed();
    }

    /** Takes up to {@code max} of the given item out; returns what it could take (maybe empty). */
    public ItemStack extract(ItemStack like, int max) {
        Key key = new Key(like.copyWithCount(1));
        Long have = items.get(key);
        if (have == null || max <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(have, max);
        if (have - taken <= 0) {
            items.remove(key);
        } else {
            items.put(key, have - taken);
        }
        changed();
        return key.stack().copyWithCount(taken);
    }

    public long count(ItemStack like) {
        return items.getOrDefault(new Key(like.copyWithCount(1)), 0L);
    }

    public int version() {
        return version;
    }

    public BankContents snapshot() {
        List<BankContents.Entry> list = new ArrayList<>(items.size());
        items.forEach((k, v) -> list.add(new BankContents.Entry(k.stack().copy(), v)));
        return new BankContents(List.copyOf(list));
    }

    public void load(BankContents contents) {
        items.clear();
        for (BankContents.Entry e : contents.entries()) {
            if (!e.item().isEmpty() && e.count() > 0) {
                items.merge(new Key(e.item().copyWithCount(1)), e.count(), Long::sum);
            }
        }
        version++;
    }

    private void changed() {
        version++;
        onChange.run();
    }
}
