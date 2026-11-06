package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;

/**
 * Where gathered materials go and where recipes take them from. t-opslag builds a "materiaaltas" (no API published when this
 * was written): it plugs in by setting {@link #BRON} (one small interface, see {@link Bron}). Without it everything uses the
 * player's own inventory (overflow: kern's verloren-gevonden chest via {@code EcoUtil.geefOfBewaar}). Recipes count and take
 * from the inventory FIRST, then from the bag.
 */
public final class MateriaalTas {
    /** The adapter for t-opslag's materiaaltas. */
    public interface Bron {
        /** Puts (part of) the stack in the bag; shrinks {@code s} by what went in. */
        void stop(ServerPlayer p, ItemStack s);

        /** How many items in the bag match. */
        int tel(ServerPlayer p, Predicate<ItemStack> wat);

        /** Takes up to n matching items out of the bag (the stacks taken, with their components). */
        List<ItemStack> neem(ServerPlayer p, Predicate<ItemStack> wat, int n);
    }

    /** Set by t-opslag (null = inventory only). */
    @Nullable
    public static volatile Bron BRON = null;

    private MateriaalTas() {
    }

    /** A gathered material: into the bag (when there is one), else the inventory (overflow kept for later). */
    public static void geef(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        Bron b = BRON;
        if (b != null && Materialen.van(s) != null) {
            b.stop(p, s);
        }
        if (!s.isEmpty()) {
            EcoUtil.geefOfBewaar(p, s);
        }
    }

    public static int tel(ServerPlayer p, Predicate<ItemStack> wat) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && wat.test(s)) {
                n += s.getCount();
            }
        }
        Bron b = BRON;
        return b == null ? n : n + b.tel(p, wat);
    }

    /** Takes n matching items (inventory first, then the bag). Check {@link #tel} first: takes what there is. */
    public static List<ItemStack> neem(ServerPlayer p, Predicate<ItemStack> wat, int n) {
        List<ItemStack> uit = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && wat.test(s)) {
                int k = Math.min(n, s.getCount());
                uit.add(s.split(k));
                n -= k;
            }
        }
        Bron b = BRON;
        if (n > 0 && b != null) {
            uit.addAll(b.neem(p, wat, n));
        }
        return uit;
    }
}
