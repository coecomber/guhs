package nl.juiced.guhs.feature.bakkerij;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/**
 * Bakker Korstje (BAKKERGUH) in the Knabbelbakkerij: explains and starts his order game ({@link BakkerijGame}), cheers
 * you on, bakes the feesttaart for the Grote Knusfeest (through the game), and sells the baker's outfit, a knabbeloven
 * and ingredients for bakmunten. The world's top 3 floats above his head.
 */
public class BakkerijRole implements NpcRole {
    /** Prices in bakmunten. */
    public static final int MUTSJE = 5, SCHORTJE = 7, STRIKJE = 4, OVEN = 8;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        BakkerijGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 == 1 && npc.level() instanceof ServerLevel) {
            BakkerijGame.showScores(npc);
        }
        BakkerijGame game = BakkerijGame.of(npc);
        if (game != null) {
            game.tick(npc);
        } else {
            BakkerijGame.invite(npc);
        }
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(MUTSJE, GuhClothes.BAKKERSMUTSJE));
        offers.add(clothes(SCHORTJE, GuhClothes.BAKKERSSCHORTJE));
        offers.add(clothes(STRIKJE, GuhClothes.MEELSTRIKJE));
        // 2.9: the big chef's hat and jacket are only sold here now (feature.kleding)
        offers.add(clothes(nl.juiced.guhs.feature.kleding.KledingFeature.PRIJS_KOKSMUTS, GuhClothes.CHEF_HAT));
        offers.add(clothes(nl.juiced.guhs.feature.kleding.KledingFeature.PRIJS_KOKSBUIS, GuhClothes.CHEF_JACKET));
        offers.add(offer(OVEN, BakkerijFeature.KNABBELOVEN_ITEM.get(), 1));
        offers.add(offer(2, BakkerijFeature.SCHOORSTEEN_ITEM.get(), 1));
        offers.add(offer(1, Items.SUGAR, 8));
        offers.add(offer(1, Items.EGG, 6));
        offers.add(offer(1, Items.WHEAT, 10));
        offers.add(offer(1, Items.PINK_DYE, 6));
        offers.add(offer(2, ModItems.KAAS_KNABBELS.get(), 12));
        offers.add(offer(3, BakkerijFeature.bakje(Recept.VADSDONUT), 4));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(BakkerijFeature.BAKMUNT.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer offer(int price, Item item, int count) {
        return new MerchantOffer(new ItemCost(BakkerijFeature.BAKMUNT.get(), price), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }
}
