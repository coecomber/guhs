package nl.juiced.guhs.feature.golf;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/** The Golfguh (in the clubhouse of the guh golf course): runs the games ({@link GolfGame}) and sells the golf outfit. */
final class GolfRole implements NpcRole {
    static final GolfRole INSTANCE = new GolfRole();
    /** The golf outfit and its price in golfballetjes (a round at par earns about 23). */
    static final int PRICE_PET = 10, PRICE_ZONNEKLEP = 8, PRICE_TRUI = 16;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GolfGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        GolfGame.of(npc).tick(npc);
    }

    /** Only here: the vadsige golfpet, the guhzonneklep and the ruitjesvadstrui, for golfballetjes. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRICE_ZONNEKLEP, GuhClothes.GOLF_ZONNEKLEP));
        offers.add(offer(PRICE_PET, GuhClothes.GOLF_PET));
        offers.add(offer(PRICE_TRUI, GuhClothes.GOLF_TRUI));
        return offers;
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(GolfFeature.GOLFBALLETJE.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }

    private GolfRole() {
    }
}
