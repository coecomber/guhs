package nl.juiced.guhs.feature.scheidingproef;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Y-SCHEIDING, test only: stands in for "the rest of the mod" (code outside feature/vadsrijk) handing a player an item the
 * usual way, so VadsrijkScheidingGameTests can check that it lands in the stored home inventory while the player is inside.
 */
public final class ThuisGeverProef {
    private ThuisGeverProef() {
    }

    public static boolean geef(ServerPlayer p, ItemStack s) {
        return p.getInventory().add(s);
    }

    public static void legTerug(ServerPlayer p, ItemStack s) {
        p.getInventory().placeItemBackInInventory(s);
    }
}
