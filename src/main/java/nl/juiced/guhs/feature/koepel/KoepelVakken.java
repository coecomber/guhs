package nl.juiced.guhs.feature.koepel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import nl.juiced.guhs.Guhs;

/** Small helpers for the Koepel machines' own slots (count, take, put, in a range of slots). */
final class KoepelVakken {
    private KoepelVakken() {
    }

    /** An item of the guhs mod by its id, or AIR (an item of another 1.8 branch that is not there yet). */
    static Item guhs(String id) {
        return BuiltInRegistries.ITEM.getValue(Guhs.id(id));
    }

    static boolean bestaat(Item item) {
        return item != null && item != Items.AIR;
    }

    static int tel(ItemStacksResourceHandler v, Item item, int van, int tot) {
        int n = 0;
        for (int i = van; i < tot; i++) {
            if (v.getResource(i).is(item)) {
                n += v.getAmountAsInt(i);
            }
        }
        return n;
    }

    /** Takes n of item out of slots van..tot-1 (the caller counted first). */
    static void neem(ItemStacksResourceHandler v, Item item, int n, int van, int tot) {
        for (int i = van; i < tot && n > 0; i++) {
            ItemResource r = v.getResource(i);
            if (r.is(item)) {
                int hier = v.getAmountAsInt(i);
                int weg = Math.min(hier, n);
                v.set(i, hier - weg == 0 ? ItemResource.EMPTY : r, hier - weg);
                n -= weg;
            }
        }
    }

    /** Is there room for this stack in slots van..tot-1? */
    static boolean past(ItemStacksResourceHandler v, ItemStack stack, int van, int tot) {
        int rest = stack.getCount();
        ItemResource soort = ItemResource.of(stack);
        for (int i = van; i < tot && rest > 0; i++) {
            ItemResource r = v.getResource(i);
            if (r.isEmpty()) {
                rest -= stack.getMaxStackSize();
            } else if (r.matches(stack)) {
                rest -= v.getCapacityAsInt(i, soort) - v.getAmountAsInt(i);
            }
        }
        return rest <= 0;
    }

    /** Puts the stack into slots van..tot-1 (check {@link #past} first); returns what did not fit. */
    static int stop(ItemStacksResourceHandler v, ItemStack stack, int van, int tot) {
        int rest = stack.getCount();
        ItemResource soort = ItemResource.of(stack);
        for (int i = van; i < tot && rest > 0; i++) {
            ItemResource r = v.getResource(i);
            if (r.matches(stack)) {
                int erbij = Math.min(rest, v.getCapacityAsInt(i, soort) - v.getAmountAsInt(i));
                if (erbij > 0) {
                    v.set(i, r, v.getAmountAsInt(i) + erbij);
                    rest -= erbij;
                }
            }
        }
        for (int i = van; i < tot && rest > 0; i++) {
            if (v.getResource(i).isEmpty()) {
                int erin = Math.min(rest, stack.getMaxStackSize());
                v.set(i, soort, erin);
                rest -= erin;
            }
        }
        return rest;
    }
}
