package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.function.Function;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-OPSLAG: the public API of the virtual storage (knapzak per character, bank + materiaaltas account-wide).
 * <ul>
 *   <li>{@link #KARAKTER}: which character is active (t-karakter sets this to its KarakterSleutel; default "1").</li>
 *   <li>{@link #sluit}: close any storage screen; call it BEFORE a character switch (t-karakter) so nothing is in flight.</li>
 *   <li>{@link #tel} / {@link #neem}: count / take an item from the inventory AND the materiaaltas (crafting, upgrades, quests).</li>
 *   <li>Materials: {@link Materiaaltas#registreer}, {@link Materiaaltas#regel}, the item tag {@code guhs:vr_materiaal},
 *   {@link Materiaaltas#geef}.</li>
 *   <li>Bank: {@link Bank#bankier}(npcId) makes any NPC a banker (1 line); {@link Bank#open}.</li>
 *   <li>Knapzak: {@link Knapzak#ontgrendel}(p, bron) unlocks a tab (quest/achievement reward); {@link Knapzak#open}.</li>
 * </ul>
 * Everything is server-authoritative and lives in one player attachment ({@link OpslagData}), saved with the inventory.
 */
public final class Opslag {
    /** The active character's key. t-karakter: {@code Opslag.KARAKTER = p -> KarakterSleutel.van(p)}. */
    public static Function<ServerPlayer, String> KARAKTER = p -> "1";

    private Opslag() {
    }

    public static OpslagData data(Player p) {
        return p.getData(OpslagRegistry.DATA);
    }

    /**
     * May this player use storage right now? Only in the dimension with the Vadsrijk inventory live (kern's swap), alive, not
     * a spectator. (So nothing from the home inventory can ever get into Vadsrijk storage, or the other way round.)
     */
    public static boolean magOpenen(ServerPlayer p) {
        return Vadsrijk.in(p) && Spelers.data(p).binnen && p.isAlive() && !p.isSpectator();
    }

    /** Closes an open knapzak/bank/materiaaltas screen (the cursor item goes back into the inventory). */
    public static void sluit(ServerPlayer p) {
        if (p.containerMenu instanceof OpslagMenu || p.containerMenu instanceof MateriaalMenu) {
            p.closeContainer();
        }
    }

    /** How many of this item the player has in the inventory plus the materiaaltas (clamped to int). */
    public static int tel(ServerPlayer p, Item item) {
        long n = Materiaaltas.tel(p, item);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    /**
     * Takes n of this item: the inventory first, then the materiaaltas. Checks first: returns false (and takes nothing) when
     * there are fewer than n.
     */
    public static boolean neem(ServerPlayer p, Item item, int n) {
        if (n <= 0) {
            return true;
        }
        if (tel(p, item) < n) {
            return false;
        }
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        inv.setChanged();
        if (n > 0) {
            Materiaaltas.neem(p, item, n);
        }
        return true;
    }

    /**
     * Puts as much of the stack as fits into the main inventory (36 slots: first onto equal stacks, then empty slots); the stack
     * keeps the rest. Unlike {@code Inventory.add} it never "eats" a stack that does not fit for a creative player.
     */
    public static void inInventaris(Inventory inv, ItemStack s) {
        for (int i = 0; i < 36 && !s.isEmpty(); i++) {
            ItemStack x = inv.getItem(i);
            if (!x.isEmpty() && ItemStack.isSameItemSameComponents(x, s) && x.getCount() < x.getMaxStackSize()) {
                int k = Math.min(s.getCount(), x.getMaxStackSize() - x.getCount());
                x.grow(k);
                s.shrink(k);
            }
        }
        for (int i = 0; i < 36 && !s.isEmpty(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, s.split(Math.min(s.getCount(), s.getMaxStackSize())));
            }
        }
        inv.setChanged();
    }

    /** Helpers for the shared-file hooks (EcoUtil / Questlog): bag count for an inventory's owner (0 when not a server player). */
    public static long tasVoor(Inventory inv, Item item) {
        return inv.player instanceof ServerPlayer sp ? Materiaaltas.tel(sp, item) : 0;
    }

    /** Hook helper: take what the inventory did not have from the bag (for code that already took from the inventory). */
    public static void neemRestUitTas(Inventory inv, Item item, int rest) {
        if (rest > 0 && inv.player instanceof ServerPlayer sp) {
            Materiaaltas.neem(sp, item, rest);
        }
    }
}
