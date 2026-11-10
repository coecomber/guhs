package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

/**
 * Y-SCHEIDING: storage that isn't bound to a place in one world (channels 2, 3 and 11 of SCHEIDING_RAPPORT.md). Checked when a
 * menu opens (whatever opened it: a block, an item, a command like FTB Essentials {@code /enderchest}, a packet): a menu that
 * belongs to the other side is closed at once, before anything can be moved.
 * <ul>
 *   <li>inside the Vadsrijk: no ender chest (the survival one) and no survival Guhs storage ({@link #ALLEEN_THUIS});</li>
 *   <li>outside: no Vadsrijk storage ({@link #ALLEEN_RIJK}).</li>
 * </ul>
 * Creative players (ops/dev) may open Vadsrijk storage outside (the BondBank already let them); the ender chest and survival
 * storage stay closed inside for everyone.
 */
public final class Menus {
    /** Menus that must never open on the Vadsrijk side (survival storage). */
    public static final List<Predicate<AbstractContainerMenu>> ALLEEN_THUIS = new CopyOnWriteArrayList<>();
    /** Menus that must never open outside the Vadsrijk (Vadsrijk storage). */
    public static final List<Predicate<AbstractContainerMenu>> ALLEEN_RIJK = new CopyOnWriteArrayList<>();

    static {
        ALLEEN_THUIS.add(m -> m instanceof ChestMenu c && c.getContainer() instanceof PlayerEnderChestContainer);
        // survival Guhs storage (all block-bound today, and nothing can be placed inside: defence in depth)
        ALLEEN_THUIS.add(klasse("nl.juiced.guhs.menu.BankGuhMenu"));
        ALLEEN_THUIS.add(klasse("nl.juiced.guhs.feature.erfenis.koppeling.LeverkistMenu"));
        // Vadsrijk storage: knapzak/bank (OpslagMenu), materiaaltas, the Knuffelbond bank (its own gate let creative players in
        // from outside)
        ALLEEN_RIJK.add(klasse("nl.juiced.guhs.feature.vadsrijk.opslag.OpslagMenu"));
        ALLEEN_RIJK.add(klasse("nl.juiced.guhs.feature.vadsrijk.opslag.MateriaalMenu"));
        ALLEEN_RIJK.add(klasse("nl.juiced.guhs.feature.vadsrijk.gilden.BondBank$Menu"));
    }

    private Menus() {
    }

    /** May this menu be open for this player on this side? */
    public static boolean mag(AbstractContainerMenu m, boolean binnen) {
        for (Predicate<AbstractContainerMenu> t : binnen ? ALLEEN_THUIS : ALLEEN_RIJK) {
            if (t.test(m)) {
                return false;
            }
        }
        return true;
    }

    static void open(ServerPlayer p, AbstractContainerMenu m) {
        boolean binnen = Scheiding.binnen(p);
        if (m == p.inventoryMenu || mag(m, binnen) || (!binnen && p.isCreative())) {
            return;   // (creative players, i.e. ops/dev, may look into Vadsrijk storage from outside, as BondBank already allowed)
        }
        p.closeContainer();
        p.sendOverlayMessage(Component.translatable(Scheiding.binnen(p) ? "vadsrijk.scheiding.menu.thuis" : "vadsrijk.scheiding.menu.rijk")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A menu class by name (for storage menus of other packages; no hard dependency). */
    static Predicate<AbstractContainerMenu> klasse(String naam) {
        return m -> {
            for (Class<?> c = m.getClass(); c != null; c = c.getSuperclass()) {
                if (c.getName().equals(naam)) {
                    return true;
                }
            }
            return false;
        };
    }
}
