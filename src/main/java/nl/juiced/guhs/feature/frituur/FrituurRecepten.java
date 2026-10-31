package nl.juiced.guhs.feature.frituur;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * Everything the frying pan makes. A recipe with {@code leren} needs {@link SnackRecepten#kent} first; its {@code id} is the
 * id of what comes out, and that is also the id of the recipe in {@link SnackRecepten}.
 */
public final class FrituurRecepten {
    /** What De Vetste Hap takes: the triple fried knabbels in your hand, the rest from your inventory, the fat from the pan. */
    public static final int HAP_DRIEDUBBEL = 16, HAP_KROKETTEN = 4, HAP_FRIKANDELLEN = 4, HAP_VET = 16;

    public record Recept(String id, Supplier<? extends Item> in, Supplier<? extends Item> uit, boolean leren) {
    }

    private static Recept vrij(String id, Supplier<? extends Item> in, Supplier<? extends Item> uit) {
        return new Recept(id, in, uit, false);
    }

    private static Recept leren(String id, Supplier<? extends Item> in, Supplier<? extends Item> uit) {
        return new Recept(id, in, uit, true);
    }

    public static final Recept VETSTE_HAP = leren("vetste_hap", FrituurFeature.DRIEDUBBEL, FrituurFeature.VETSTE_HAP);

    public static final List<Recept> ALLE = List.of(
            vrij("gefrituurde_kaasknabbels", ModItems.KAAS_KNABBELS, ModItems.GEFRITUURDE_KAASKNABBELS),
            vrij("gebakken_guh_vis", ModItems.GUH_VIS, ModItems.GEBAKKEN_GUH_VIS),
            vrij("dubbel_gefrituurde_kaasknabbels", ModItems.GEFRITUURDE_KAASKNABBELS, FrituurFeature.DUBBEL),
            vrij("driedubbel_gefrituurde_kaasknabbels", FrituurFeature.DUBBEL, FrituurFeature.DRIEDUBBEL),
            vrij("patatje_kaassaus", () -> Items.POTATO, FrituurFeature.PATATJE_KAASSAUS),
            leren("patatje_vads", () -> Items.BAKED_POTATO, FrituurFeature.PATATJE_VADS),
            leren("patatje_oorlog", () -> Items.POISONOUS_POTATO, FrituurFeature.PATATJE_OORLOG),
            leren("patatje_guhspeciaal", FrituurFeature.PATATJE_KAASSAUS, FrituurFeature.PATATJE_GUHSPECIAAL),
            leren("kaaskroket", KaasmijnFeature.KAASBROK, FrituurFeature.KAASKROKET),
            leren("frikandel_guhspeciaal", () -> Items.PORKCHOP, FrituurFeature.FRIKANDEL),
            leren("bitterguhllen", () -> Items.BEEF, FrituurFeature.BITTERGUHLLEN),
            leren("kaassoufflguh", () -> Items.BREAD, FrituurFeature.KAASSOUFFLGUH),
            VETSTE_HAP);

    /** The recipe that starts from what you hold (null: the pan has nothing to do with it). */
    @Nullable
    public static Recept vind(ItemStack stack) {
        for (Recept r : ALLE) {
            if (stack.is(r.in().get())) {
                return r;
            }
        }
        return null;
    }

    /** Makes one Vetste Hap (only with everything there, else a message saying what is missing). */
    public static boolean maakVetsteHap(Player player, ItemStack hand, FryingPanBlockEntity pan) {
        Inventory inv = player.getInventory();
        if (hand.getCount() < HAP_DRIEDUBBEL || inv.countItem(FrituurFeature.KAASKROKET.get()) < HAP_KROKETTEN
                || inv.countItem(FrituurFeature.FRIKANDEL.get()) < HAP_FRIKANDELLEN || pan.getCharges() < HAP_VET) {
            player.sendOverlayMessage(Component.translatable("block.guhs.frying_pan.vetste_hap_nodig", HAP_DRIEDUBBEL, HAP_KROKETTEN,
                    HAP_FRIKANDELLEN, HAP_VET));
            return false;
        }
        if (!player.getAbilities().instabuild) {
            hand.shrink(HAP_DRIEDUBBEL);
            haalWeg(inv, FrituurFeature.KAASKROKET.get(), HAP_KROKETTEN);
            haalWeg(inv, FrituurFeature.FRIKANDEL.get(), HAP_FRIKANDELLEN);
        }
        pan.setCharges(pan.getCharges() - HAP_VET);
        inv.placeItemBackInInventory(new ItemStack(FrituurFeature.VETSTE_HAP.get()));
        return true;
    }

    private static void haalWeg(Inventory inv, Item item, int aantal) {
        for (int i = 0; i < inv.getContainerSize() && aantal > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int weg = Math.min(aantal, s.getCount());
                s.shrink(weg);
                aantal -= weg;
            }
        }
    }

    private FrituurRecepten() {
    }
}
