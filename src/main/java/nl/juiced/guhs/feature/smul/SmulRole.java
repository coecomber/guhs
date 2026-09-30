package nl.juiced.guhs.feature.smul;

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
 * The Smulguh of the Vadsig eetfestijn: explains and starts the catching game ({@link SmulGame}), cheers you on, and sells
 * the smul outfit (only here!) and a few treats for smulmunten. The world's top 3 floats above her head.
 */
public class SmulRole implements NpcRole {
    /** Prices of the smul outfit, in smulmunten. */
    public static final int SLABBETJE = 5, BAKKERSMUTS = 7, SCHORT = 9;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        SmulGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 == 1 && npc.level() instanceof net.minecraft.server.level.ServerLevel) {
            SmulGame.showScores(npc);                                   // the top 3 board above her head
        }
        SmulGame game = SmulGame.of(npc);
        if (game != null) {
            game.tick(npc);
        } else {
            SmulGame.invite(npc);
        }
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(SLABBETJE, GuhClothes.SMUL_SLABBETJE));
        offers.add(clothes(BAKKERSMUTS, GuhClothes.SMUL_BAKKERSMUTS));
        offers.add(clothes(SCHORT, GuhClothes.SMUL_SCHORT));
        offers.add(treat(2, SmulFeature.GOUDEN_SMULKNABBEL.get(), 1));
        offers.add(treat(1, ModItems.GUH_CUPCAKE.get(), 4));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(SmulFeature.SMULMUNT.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer treat(int price, Item item, int count) {
        return new MerchantOffer(new ItemCost(SmulFeature.SMULMUNT.get(), price), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }
}
