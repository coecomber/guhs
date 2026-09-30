package nl.juiced.guhs.feature.kleding;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * The Guh kleermaker (2.9): no random villager trades any more, but a fixed full offer (his everyday set + the three
 * ear bows, ModVillagers.kleermakerAanbod). Vanilla picks two random trades per level, so every kleermaker gets the whole
 * list put in before you can trade with him (on right-click, and every couple of seconds).
 */
public final class KledingKleermaker {
    /** Bump when the offer changes: every kleermaker then gets the new list. */
    public static final int VERSIE = 1;
    private static final String TAG = "guhs_kleermaker_aanbod";

    /** True for a guh-village kleermaker. */
    public static boolean is(Villager villager) {
        return villager.getVillagerData().getProfession() == ModVillagers.GUH_KLEERMAKER.get();
    }

    /** Gives a kleermaker the fixed full offer (once, or again when {@link #VERSIE} changed). Returns whether it changed. */
    public static boolean zorgVoorAanbod(Villager villager) {
        if (!villager.level().isClientSide() && !is(villager)) {
            villager.getPersistentData().remove(TAG);   // (lost the job: a new kleermaker career starts fresh)
            return false;
        }
        if (villager.level().isClientSide() || villager.getPersistentData().getIntOr(TAG, 0) == VERSIE) {
            return false;
        }
        MerchantOffers offers = new MerchantOffers();
        for (VillagerTrades.ItemListing listing : ModVillagers.kleermakerAanbod()) {
            MerchantOffer offer = listing.getOffer(villager, villager.getRandom());
            if (offer != null) {
                offers.add(offer);
            }
        }
        villager.setOffers(offers);
        villager.getPersistentData().putInt(TAG, VERSIE);
        return true;
    }

    private KledingKleermaker() {
    }
}
