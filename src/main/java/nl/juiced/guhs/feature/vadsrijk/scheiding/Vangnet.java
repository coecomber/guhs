package nl.juiced.guhs.feature.vadsrijk.scheiding;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;

/**
 * Y-SCHEIDING (channel 1 of SCHEIDING_RAPPORT.md): while kern's swap empties the hands (cursor item, the 2x2 grid, an open menu
 * that gives its items back), vanilla DROPS whatever doesn't fit in a full inventory, and the swap runs after the dimension
 * change: a survival cursor item landed on Vadsrijk ground (and the other way round). During {@link #tijdens} every drop of that
 * player is cancelled and kept in the "gevonden" stash of the side it came from ({@code /guhs vadsrijk kist} on that side).
 */
public final class Vangnet {
    @Nullable
    private static ServerPlayer bezig;

    private Vangnet() {
    }

    /** Runs {@code r} with this player's drops caught (server thread only, like the swap itself). */
    public static void tijdens(ServerPlayer p, Runnable r) {
        ServerPlayer vorige = bezig;
        bezig = p;
        try {
            r.run();
        } finally {
            bezig = vorige;
        }
    }

    static void gooi(ItemTossEvent e) {
        if (bezig != null && e.getPlayer() == bezig) {
            e.setCanceled(true);
            Inventaris.bewaar(bezig, e.getEntity().getItem());   // (still the old side: kern flips "binnen" after the swap)
            e.getEntity().discard();
        }
    }
}
