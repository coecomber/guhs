package nl.juiced.guhs.feature.mijnspellen.karretjes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/**
 * Conducteur Piepwiel (in the Wisselgrot): runs the shifts ({@link KarretjesGame}) and sells, for ertsbonnetjes, the
 * conducteurspetje, the building blocks (1 ertsbonnetje for 4) and the deco. Never sold out.
 */
final class KarretjesRole implements NpcRole {
    static final KarretjesRole INSTANCE = new KarretjesRole();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        KarretjesGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        KarretjesGame.of(npc).tick(npc);
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        KarretjesGame.antwoord(npc, player, optie);
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(prijs(KarretjesFeature.PRIJS_PETJE, new ItemStack(ModItems.clothingItem(GuhClothes.KARRETJES_CONDUCTEURSPETJE))));
        for (var item : KarretjesFeature.BOUWBLOKKEN) {
            offers.add(prijs(1, new ItemStack(item.get(), 4)));
        }
        offers.add(deco(KarretjesFeature.PRIJS_SEINLAMPJE, KarretjesFeature.SEINLAMPJE_ITEM.get()));
        offers.add(deco(KarretjesFeature.PRIJS_KLOKJE, KarretjesFeature.STATIONSKLOKJE_ITEM.get()));
        offers.add(deco(KarretjesFeature.PRIJS_KARRETJE, KarretjesFeature.ERTSKARRETJE_ITEM.get()));
        return offers;
    }

    private static MerchantOffer deco(int price, Item item) {
        return prijs(price, new ItemStack(item));
    }

    private static MerchantOffer prijs(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(KarretjesFeature.ERTSBONNETJE.get(), price), result, Integer.MAX_VALUE, 0, 0);
    }

    private KarretjesRole() {
    }
}
