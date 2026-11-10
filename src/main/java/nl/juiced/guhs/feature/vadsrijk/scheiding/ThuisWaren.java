package nl.juiced.guhs.feature.vadsrijk.scheiding;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Y-INT (user decision, round Y): Vadsmunten only buy Vadsrijk DECO and CLOTHING for home. This is the one list of what may
 * reach the home (survival) inventory through Meubelmeester Mollie (Knuffelkwartier) or the Vadsrijk Post:
 * <ul>
 *   <li>clothing: every Guh clothing item ({@link GuhClothingItem}: the Vadsrijk outfits, the ambacht hats/apron, heart glasses);</li>
 *   <li>Vadsrijk deco: the {@code vr_eco_*} deco blocks ({@link EcoSpul#DECO}: vaandel, klodderreus-knuffel, opperzwambeeld,
 *   mini-koning) and the guh-kite keepsake ({@code vr_amb_vlieger}, an item without a use);</li>
 *   <li>house furniture: the Guhs furniture (guh_stoel, guh_tafel, guh_bank, every beanbag and cushion colour) and the bunting.</li>
 * </ul>
 * Not allowed (removed from Mollie in round Y): vanilla building blocks (cherry planks, bookshelves, lanterns, bed, glass,
 * carpet, flower pots), the Guh cake (food) and the guh flowers (suspicious stew / dye). What a player already bought stays theirs.
 */
public final class ThuisWaren {
    private ThuisWaren() {
    }

    /** May this stack be bought with Vadsmunten and delivered home? */
    public static boolean mag(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        Item i = s.getItem();
        if (i instanceof GuhClothingItem || i == Materialen.VLIEGER.get() || i == ModBlocks.VLAGGETJES.get().asItem()) {
            return true;
        }
        for (var d : EcoSpul.DECO) {
            if (i == d.get().asItem()) {
                return true;
            }
        }
        if (i == ModBlocks.GUH_STOEL.get().asItem() || i == ModBlocks.GUH_TAFEL.get().asItem() || i == ModBlocks.GUH_BANK.get().asItem()) {
            return true;
        }
        return i instanceof BlockItem b && (ModBlocks.ZITZAKKEN.values().stream().anyMatch(z -> z.get() == b.getBlock())
                || ModBlocks.KUSSENS.values().stream().anyMatch(k -> k.get() == b.getBlock()));
    }
}
