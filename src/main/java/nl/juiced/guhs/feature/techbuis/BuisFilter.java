package nl.juiced.guhs.feature.techbuis;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a Filterstuk lets through and what a Voorraadmeter counts: a short list of example items (ghosts: the list never
 * holds real items), and three settings.
 * <ul>
 *   <li>{@link #behalve}: the list turned around ("everything except these"); for the Voorraadmeter: signal when there is
 *       LESS than the number;</li>
 *   <li>{@link #precies}: an item must match the example exactly (name, enchantments, what is inside), not just its kind;</li>
 *   <li>{@link #getal}: the Filterstuk leaves at least this many of each kind behind in what it takes from; the
 *       Voorraadmeter gives its signal from this many.</li>
 * </ul>
 * An empty list lets everything through (and counts everything).
 */
public final class BuisFilter {
    /** The settings as menu data (FilterMenu): the three numbers a screen sees and the buttons change. */
    public static final int DATA_BEHALVE = 0, DATA_PRECIES = 1, DATA_GETAL = 2, DATA_AANTAL = 3;
    public static final int MAX_GETAL = 9999;

    private final SimpleContainer items;
    private final Runnable veranderd;
    private boolean behalve;
    private boolean precies;
    private int getal;

    public BuisFilter(int vakken, int getal, Runnable veranderd) {
        this.items = new SimpleContainer(vakken) {
            @Override
            public void setChanged() {
                super.setChanged();
                veranderd.run();
            }
        };
        this.getal = getal;
        this.veranderd = veranderd;
    }

    /** The example items (count 1 each; empty slots in between are fine). */
    public SimpleContainer items() {
        return items;
    }

    public boolean behalve() {
        return behalve;
    }

    public boolean precies() {
        return precies;
    }

    public int getal() {
        return getal;
    }

    public void zetBehalve(boolean aan) {
        behalve = aan;
        veranderd.run();
    }

    public void zetPrecies(boolean aan) {
        precies = aan;
        veranderd.run();
    }

    public void zetGetal(int n) {
        getal = Mth.clamp(n, 0, MAX_GETAL);
        veranderd.run();
    }

    /** Puts an example in the first free slot (false: it is on the list already, or the list is full). */
    public boolean voegToe(ItemStack stack) {
        if (stack.isEmpty() || opLijst(stack)) {
            return false;
        }
        for (int i = 0; i < items.getContainerSize(); i++) {
            if (items.getItem(i).isEmpty()) {
                items.setItem(i, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    /** No examples at all. */
    public boolean isLeeg() {
        return items.isEmpty();
    }

    /** A list that asks for particular things: "only these", with at least one example. */
    public boolean isGericht() {
        return !behalve && !isLeeg();
    }

    private boolean opLijst(ItemStack stack) {
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack voorbeeld = items.getItem(i);
            if (!voorbeeld.isEmpty() && (precies ? ItemStack.isSameItemSameComponents(voorbeeld, stack) : ItemStack.isSameItem(voorbeeld, stack))) {
                return true;
            }
        }
        return false;
    }

    /** Does this item get through (is it counted)? */
    public boolean past(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isLeeg()) {
            return true;
        }
        return opLijst(stack) != behalve;
    }

    /** Is this item counted by a Voorraadmeter: on the list, or the list is empty? ({@link #behalve} means something else there.) */
    public boolean telt(ItemStack stack) {
        return !stack.isEmpty() && (isLeeg() || opLijst(stack));
    }

    /** The names of the examples, for the hover readout. */
    public List<Component> namen() {
        List<Component> uit = new ArrayList<>();
        for (int i = 0; i < items.getContainerSize(); i++) {
            if (!items.getItem(i).isEmpty()) {
                uit.add(items.getItem(i).getHoverName());
            }
        }
        return uit;
    }

    public void opslaan(ValueOutput uit) {
        ContainerHelper.saveAllItems(uit, items.getItems(), true);
        uit.putBoolean("Behalve", behalve);
        uit.putBoolean("Precies", precies);
        uit.putInt("Getal", getal);
    }

    public void laden(ValueInput in) {
        items.getItems().clear();
        ContainerHelper.loadAllItems(in, items.getItems());
        behalve = in.getBooleanOr("Behalve", false);
        precies = in.getBooleanOr("Precies", false);
        getal = Mth.clamp(in.getIntOr("Getal", getal), 0, MAX_GETAL);
    }

    /** The settings for a menu (the server's side: live). */
    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int id) {
                return switch (id) {
                    case DATA_BEHALVE -> behalve ? 1 : 0;
                    case DATA_PRECIES -> precies ? 1 : 0;
                    case DATA_GETAL -> getal;
                    default -> 0;
                };
            }

            @Override
            public void set(int id, int waarde) {
                switch (id) {
                    case DATA_BEHALVE -> zetBehalve(waarde != 0);
                    case DATA_PRECIES -> zetPrecies(waarde != 0);
                    case DATA_GETAL -> zetGetal(waarde);
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_AANTAL;
            }
        };
    }
}
