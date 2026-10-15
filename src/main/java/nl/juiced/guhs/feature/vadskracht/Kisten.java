package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Items in and out of anything that holds items (a chest, a machine, the Bank Guh, a Hapluikje...), through NeoForge 26.1's
 * item capability: a {@link ResourceHandler} of {@link ItemResource}s with transactions. These helpers hide the
 * transactions: every method opens its own and commits it (unless {@code simuleer}); called inside somebody else's
 * transaction it joins that one. Nothing here ever loses an item: what does not fit comes back.
 */
public final class Kisten {
    /** The item handler of the block here, seen from this side (null: no side / the inside); null when it holds no items. */
    @Nullable
    public static ResourceHandler<ItemResource> van(Level level, BlockPos pos, @Nullable Direction kant) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.Item.BLOCK, pos, kant) : null;
    }

    /** Puts the stack in; returns the REMAINDER (empty when everything went in). The given stack is not changed. */
    public static ItemStack stop(ResourceHandler<ItemResource> in, ItemStack stack) {
        return stop(in, stack, false);
    }

    public static ItemStack stop(ResourceHandler<ItemResource> in, ItemStack stack, boolean simuleer) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction tx = open()) {
            int erin = in.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (!simuleer) {
                tx.commit();
            }
            return erin >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - erin);
        }
    }

    /** Takes up to max of the first kind of item that passes; returns what was taken (empty: nothing). */
    public static ItemStack neem(ResourceHandler<ItemResource> uit, Predicate<ItemStack> wat, int max) {
        return neem(uit, wat, max, false);
    }

    public static ItemStack neem(ResourceHandler<ItemResource> uit, Predicate<ItemStack> wat, int max, boolean simuleer) {
        if (max <= 0) {
            return ItemStack.EMPTY;
        }
        try (Transaction tx = open()) {
            for (int i = 0; i < uit.size(); i++) {
                ItemResource soort = uit.getResource(i);
                if (soort.isEmpty() || !soort.test(wat)) {
                    continue;
                }
                int eruit = uit.extract(soort, Math.min(max, soort.getMaxStackSize()), tx);
                if (eruit > 0) {
                    if (!simuleer) {
                        tx.commit();
                    }
                    return soort.toStack(eruit);
                }
            }
            return ItemStack.EMPTY;
        }
    }

    /** Moves up to max items that pass from one handler to another, in one transaction; returns how many moved. */
    public static int verplaats(ResourceHandler<ItemResource> van, ResourceHandler<ItemResource> naar, Predicate<ItemStack> wat, int max) {
        if (max <= 0) {
            return 0;
        }
        try (Transaction tx = open()) {
            int n = ResourceHandlerUtil.move(van, naar, soort -> soort.test(wat), max, tx);
            tx.commit();
            return n;
        }
    }

    /** How many items that pass the handler holds. */
    public static long tel(ResourceHandler<ItemResource> h, Predicate<ItemStack> wat) {
        long n = 0;
        for (int i = 0; i < h.size(); i++) {
            ItemResource soort = h.getResource(i);
            if (!soort.isEmpty() && soort.test(wat)) {
                n += h.getAmountAsLong(i);
            }
        }
        return n;
    }

    /** Does the whole stack fit? */
    public static boolean past(ResourceHandler<ItemResource> in, ItemStack stack) {
        return stop(in, stack, true).isEmpty();
    }

    /** A transaction of our own: a root one, or inside the transaction that is open on this thread. */
    @SuppressWarnings("deprecation")
    static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    private Kisten() {
    }
}
