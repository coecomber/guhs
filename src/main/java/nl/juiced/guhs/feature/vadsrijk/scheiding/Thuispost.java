package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * Y-SCHEIDING (channel 13 of SCHEIDING_RAPPORT.md): the GENERIC rule for survival items handed out while the player is inside.
 * <p>The rest of the mod gives items in ~485 places with {@code getInventory().add(...)}; ten of them fire wherever the player is
 * (Sherlock's magnifier and case rewards, the dream-guh's morning gift, Guhpixel/Snuffel leftovers, Appa/Muziekboekje/Rook
 * one-time gifts, Momo, the Opa's kelder delivery...). Instead of patching each one, {@code mixin.InventoryScheidingMixin} asks
 * {@link #omleiden} at the start of {@code Inventory.add} / {@code placeItemBackInInventory}: when the player's Vadsrijk inventory
 * is live and the nearest Guhs code on the call stack is NOT Vadsrijk code, the stack goes to the stored home inventory instead
 * ({@link Inventaris#legThuis}: out on the way home, else the home "gevonden" stash). Vanilla pickups (no Guhs code on the stack)
 * and all Vadsrijk code (loot, quests, shops) keep filling the live Vadsrijk inventory as before.
 * <p>New code that gives a survival item can also call {@link Scheiding#geefThuis} directly.
 */
public final class Thuispost {
    /** Guhs code (class name prefixes) that counts as Vadsrijk code here: its items stay in the live inventory. */
    public static final List<String> RIJK_CODE = new CopyOnWriteArrayList<>(List.of("nl.juiced.guhs.feature.vadsrijk.",
            // KeepOnDeath puts back what the player had at death, on the side that was live then (kern's swap sorts it out)
            "nl.juiced.guhs.event.KeepOnDeathHandler",
            // the gametest helpers act on behalf of a test
            "nl.juiced.guhs.gametest."));
    private static final String GUHS = "nl.juiced.guhs.";
    /** (re-entrancy: legThuis itself may add to the inventory) */
    private static boolean bezig;

    private Thuispost() {
    }

    /** Called by the mixin. True = the stack was taken (count 0) and sent home; the caller pretends it was added. */
    public static boolean omleiden(Inventory inv, ItemStack s) {
        if (bezig || s.isEmpty() || !(inv.player instanceof ServerPlayer p) || !Spelers.data(p).binnen) {
            return false;
        }
        Optional<String> wie = eersteGuhs();
        if (wie.isEmpty() || RIJK_CODE.stream().anyMatch(wie.get()::startsWith)) {
            return false;
        }
        bezig = true;
        try {
            Inventaris.legThuis(p, s.copy());
        } finally {
            bezig = false;
        }
        s.setCount(0);
        return true;
    }

    /** The nearest Guhs class on the stack (the mixin's merged handler shows up as the vanilla class, so it is skipped). */
    static Optional<String> eersteGuhs() {
        return StackWalker.getInstance().walk(st -> st.map(StackWalker.StackFrame::getClassName)
                .filter(c -> c.startsWith(GUHS) && !c.startsWith(Thuispost.class.getName()) && !c.startsWith("nl.juiced.guhs.mixin."))
                .findFirst());
    }
}
