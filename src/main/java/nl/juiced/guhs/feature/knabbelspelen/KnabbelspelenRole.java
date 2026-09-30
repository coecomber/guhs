package nl.juiced.guhs.feature.knabbelspelen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.doolhof.Anker;
import nl.juiced.guhs.registry.ModItems;

/**
 * Juf Vahoegsakee (SPELLEIDERGUH) in the middle of the circus tent: explains the six events and the zeskamp score
 * table, opens a round ({@link Wedstrijd}), whistles, cheers, and sells the sports outfit for spelenlintjes. The
 * zeskamp top 3 floats above her; every field has the top 3 of its event.
 */
public class KnabbelspelenRole implements NpcRole {
    /** Prices in spelenlintjes (headband ~4, whistle ~6, shirt ~10). */
    public static final int ZWEETBANDJE = 4, FLUITJE = 6, SPORTSHIRTJE = 10;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        Wedstrijd.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel world)) {
            return;
        }
        if (npc.tickCount % 100 == 1) {
            Anker anker = Speelvelden.anker(npc);
            if (anker != null) {
                Wedstrijd.showScores(world, anker);
            }
        }
        Wedstrijd w = Wedstrijd.of(npc);
        if (w != null) {
            w.tick(npc);
        }
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(ZWEETBANDJE, GuhClothes.SPELEN_ZWEETBANDJE));
        offers.add(clothes(FLUITJE, GuhClothes.SPELEN_FLUITJE));
        offers.add(clothes(SPORTSHIRTJE, GuhClothes.SPELEN_SPORTSHIRTJE));
        offers.add(offer(2, KnabbelspelenFeature.BLIK_ITEM.get(), 3));
        offers.add(offer(2, KnabbelspelenFeature.KAASMELKFLES_ITEM.get(), 2));
        offers.add(offer(1, ModItems.KAAS_KNABBELS.get(), 10));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(KnabbelspelenFeature.SPELENLINTJE.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer offer(int price, Item item, int count) {
        return new MerchantOffer(new ItemCost(KnabbelspelenFeature.SPELENLINTJE.get(), price), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }
}
