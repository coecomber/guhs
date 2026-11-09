package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.Optional;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.9.5: the Leverkist's own screen: a big chest (six rows, the vanilla look) whose slots hold up to
 * {@link Leverkist#GROTE_STAPEL} of one thing. Taking out by hand goes a normal stack at a time (so a cursor never holds
 * thousands); putting in, shift-clicking and the post fill the big stacks.
 */
public class LeverkistMenu extends ChestMenu {
    public LeverkistMenu(int id, Inventory inventory, Container kist) {
        super(KoppelingFeature.LEVERKIST_MENU.get(), id, inventory, kist, Leverkist.SLOTS / 9);
        for (int i = 0; i < Leverkist.SLOTS; i++) {
            Slot oud = slots.get(i);
            Slot groot = new GrootVak(kist, oud.getContainerSlot(), oud.x, oud.y);
            groot.index = i;
            slots.set(i, groot);
        }
    }

    /** The client side: an empty stand-in with the same big stacks (the server sends the real contents). */
    public static LeverkistMenu client(int id, Inventory inventory) {
        return new LeverkistMenu(id, inventory, new SimpleContainer(Leverkist.SLOTS) {
            @Override
            public int getMaxStackSize() {
                return Leverkist.GROTE_STAPEL;
            }
        });
    }

    /** A slot that holds a big stack, but hands out at most a normal stack at a time. */
    static class GrootVak extends Slot {
        GrootVak(Container c, int slot, int x, int y) {
            super(c, slot, x, y);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Leverkist.GROTE_STAPEL;
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            int normaal = getItem().isEmpty() ? amount : getItem().getMaxStackSize();
            return super.tryRemove(Math.min(amount, normaal), Math.min(maxAmount, normaal), player);
        }
    }
}
