package nl.juiced.guhs.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The Bank Guh's stomach: unlimited item types, and of each type (item + components: names, enchantments, damage...) at
 * most {@link #CAP}, until the bank is upgraded ({@link #setUpgraded}): then there is no limit any more.
 * <p>
 * bbq2 rules, all of them here so that every way in obeys them:
 * <ul>
 * <li>{@link #insert} is the single way in. It takes what fits and tells what did NOT fit: the caller keeps that
 * remainder. Nothing is ever thrown away here.</li>
 * <li>A stomach that already holds more than the cap of something (banks from before the cap) keeps all of it: nothing is
 * clamped on load, taking out always works, putting more of that item in is refused until it is under the cap again
 * (or the bank is upgraded).</li>
 * <li>{@link #restore} puts back what was taken out a moment ago in the same action (it ignores the cap, so a failed
 * "take" can never lose items).</li>
 * </ul>
 * Pipes and hoppers come in through {@link BankHandler} (the item capability), which uses the same rules.
 */
public class BankStorage {
    /** How many of one kind of item fit in a Bank Guh that is not upgraded. */
    public static final int CAP = 256;

    /** Insertion ordered; a key is an item with its components (count-less). Values are always above zero. */
    private final Map<ItemResource, Long> items = new LinkedHashMap<>();
    private boolean upgraded;
    /** Changes whenever the contents or the upgrade change; the open screen is synced on it. */
    private int version;
    /** Counts every change and never goes back (unlike {@link #version} after a rolled back transaction). */
    private long mutations;
    private final Runnable onChange;

    public BankStorage(Runnable onChange) {
        this.onChange = onChange;
    }

    // =====================================================================================================================
    // the cap and the upgrade
    // =====================================================================================================================

    public boolean isUpgraded() {
        return upgraded;
    }

    public void setUpgraded(boolean upgraded) {
        if (this.upgraded != upgraded) {
            this.upgraded = upgraded;
            changed();
        }
    }

    /** The most of one kind this stomach takes: {@link #CAP}, or no limit when upgraded. */
    public long cap() {
        return upgraded ? Long.MAX_VALUE : CAP;
    }

    /** How many more of this item fit (0: full, also when it holds more than the cap from before the cap existed). */
    public long room(ItemStack like) {
        return like.isEmpty() ? 0 : room(ItemResource.of(like));
    }

    /** How many more of this kind fit (the same rule, for a kind without a stack). */
    public long room(ItemResource key) {
        return Math.max(0, cap() - amount(key));
    }

    /** Is there no room for even one more of this item? */
    public boolean isFull(ItemStack like) {
        return room(like) <= 0;
    }

    // =====================================================================================================================
    // in and out
    // =====================================================================================================================

    /**
     * Puts in as much of the stack as fits and returns the REMAINDER (empty when everything went in). The passed stack is
     * left untouched: the caller replaces its own stack by what comes back.
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long in = insert(stack, stack.getCount());
        return in >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) in);
    }

    /** Puts in up to {@code amount} of this item; returns how many really went in (the rest stays with the caller). */
    public long insert(ItemStack like, long amount) {
        if (like.isEmpty() || amount <= 0) {
            return 0;
        }
        ItemResource key = ItemResource.of(like);
        long in = Math.min(amount, room(key));
        if (in <= 0) {
            return 0;
        }
        set(key, amount(key) + in);
        return in;
    }

    /**
     * Puts back what was taken out a moment ago in the same action (a "take" that did not fit in the inventory). It
     * ignores the cap: a stomach from before the cap may hold more than the cap, and what came out of it must be able to
     * go back.
     */
    public void restore(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemResource key = ItemResource.of(stack);
        long sum = amount(key) + stack.getCount();
        set(key, sum < 0 ? Long.MAX_VALUE : sum);   // (overflow guard, just in case)
    }

    /** Takes up to {@code max} of the given item out; returns what it could take (maybe empty). Always allowed. */
    public ItemStack extract(ItemStack like, int max) {
        if (like.isEmpty() || max <= 0) {
            return ItemStack.EMPTY;
        }
        ItemResource key = ItemResource.of(like);
        long have = amount(key);
        if (have <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(have, max);
        set(key, have - taken);
        return key.toStack(taken);
    }

    public long count(ItemStack like) {
        return like.isEmpty() ? 0 : amount(ItemResource.of(like));
    }

    public int version() {
        return version;
    }

    public BankContents snapshot() {
        List<BankContents.Entry> list = new ArrayList<>(items.size());
        items.forEach((k, v) -> list.add(new BankContents.Entry(k.toStack(), v)));
        return new BankContents(List.copyOf(list));
    }

    /** Loads a saved stomach as it is: never clamped to the cap (see the class comment). */
    public void load(BankContents contents) {
        items.clear();
        for (BankContents.Entry e : contents.entries()) {
            if (!e.item().isEmpty() && e.count() > 0) {
                items.merge(ItemResource.of(e.item()), e.count(), (a, b) -> a + b < 0 ? Long.MAX_VALUE : a + b);
            }
        }
        version++;
        mutations++;
    }

    // =====================================================================================================================
    // for BankHandler (same package): the raw contents
    // =====================================================================================================================

    long amount(ItemResource key) {
        Long have = items.get(key);
        return have == null ? 0 : have;
    }

    /** Sets how many of this item there are (0 removes it); no cap check. */
    void set(ItemResource key, long count) {
        if (count <= 0) {
            items.remove(key);
        } else {
            items.put(key, count);
        }
        changed();
    }

    /** Like {@link #set}, for undoing a transaction: the screen's version goes back too (nothing changed after all). */
    void undo(ItemResource key, long count, int versionBefore) {
        set(key, count);
        version = versionBefore;
    }

    Iterable<ItemResource> keys() {
        return items.keySet();
    }

    boolean has(ItemResource key) {
        return items.containsKey(key);
    }

    long mutations() {
        return mutations;
    }

    private void changed() {
        version++;
        mutations++;
        onChange.run();
    }
}
