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
    /** 1.7.0, deco (a bit more than the blocks): two balls on tees, the hole with a flag, the golf rack, the golf cart. */
    static final int PRICE_TEE = 3, PRICE_VLAGGETJE = 6, PRICE_REK = 10, PRICE_KARRETJE = 64;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GolfGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        GolfGame.of(npc).tick(npc);
    }

    /** Only here: the vadsige golfpet, the guhzonneklep and the ruitjesvadstrui (1.7.0: and the golf shop), for golfballetjes. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRICE_ZONNEKLEP, GuhClothes.GOLF_ZONNEKLEP));
        offers.add(offer(PRICE_PET, GuhClothes.GOLF_PET));
        offers.add(offer(PRICE_TRUI, GuhClothes.GOLF_TRUI));
        // 1.7.0: the golf shop: building blocks (1 golfballetje for 4) and deco; the golf trophy is a reward, never for sale
        offers.add(blocks(GolfFeature.VILT_ITEM.get(), 4));
        for (var item : Golf18.RUITJES_ITEMS) {
            offers.add(blocks(item.get(), 4));
        }
        offers.add(new MerchantOffer(new ItemCost(GolfFeature.GOLFBALLETJE.get(), PRICE_TEE), new ItemStack(Golf18.GOLFBAL_TEE_ITEM.get(), 2),
                Integer.MAX_VALUE, 0, 0));
        offers.add(deco(PRICE_VLAGGETJE, Golf18.GOLF_VLAGGETJE_ITEM.get()));
        offers.add(deco(PRICE_REK, Golf18.GOLFREK_ITEM.get()));
        offers.add(deco(PRICE_KARRETJE, Golf18.GOLFKARRETJE_ITEM.get()));
        offers.add(offer(nl.juiced.guhs.feature.voetjes.VoetjesFeature.PRIJS_GOLFSCHOENTJES, GuhClothes.GOLFSCHOENTJES));   // (the feet slot)
        return offers;
    }

    /** 1.7.0: building blocks: one golfballetje for this many. */
    private static MerchantOffer blocks(net.minecraft.world.item.Item item, int count) {
        return new MerchantOffer(new ItemCost(GolfFeature.GOLFBALLETJE.get(), 1), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer deco(int price, net.minecraft.world.item.Item item) {
        return new MerchantOffer(new ItemCost(GolfFeature.GOLFBALLETJE.get(), price), new ItemStack(item), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(GolfFeature.GOLFBALLETJE.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }

    private GolfRole() {
    }
}
