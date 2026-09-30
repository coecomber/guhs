package nl.juiced.guhs.feature.meppen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Mepguh (in the Mika-mephal): explains Mika meppen, starts a game and runs a little shop with the Mika-hunter
 * outfit (only sold here) and Mika trophies, for mepmunten.
 */
public class MeppenRole implements NpcRole {
    /** Prices in mepmunten. */
    public static final int MEDAILLE = 4, HOED = 6, VEST = 8, TROFEE = 2;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        MepGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        MepGame.of(npc).tick(npc);
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(MEDAILLE, GuhClothes.MIKAMEPPER_MEDAILLE));
        offers.add(clothes(HOED, GuhClothes.MIKAJAGER_HOED));
        offers.add(clothes(VEST, GuhClothes.MIKAJAGER_VEST));
        offers.add(new MerchantOffer(new ItemCost(MeppenFeature.MEPMUNT.get(), TROFEE), new ItemStack(MeppenFeature.TROFEE_ITEM.get()),
                Integer.MAX_VALUE, 0, 0));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(MeppenFeature.MEPMUNT.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }
}
