package nl.juiced.guhs.feature.katapult;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/** Kapitein Floepguh (on the wall of the Knabbelkatapult): runs the runs of forts ({@link KatapultGame}) and sells his outfit. */
final class KatapultRole implements NpcRole {
    static final KatapultRole INSTANCE = new KatapultRole();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        KatapultGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        KatapultGame.of(npc).tick(npc);
    }

    /** Only here: the katapulthelmpje, the pluisbal-oorbelletjes and the katapultriem, for katapultsterren. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(KatapultFeature.PRIJS_HELMPJE, GuhClothes.KATAPULT_HELMPJE));
        offers.add(offer(KatapultFeature.PRIJS_OORBELLETJES, GuhClothes.KATAPULT_OORBELLETJES));
        offers.add(offer(KatapultFeature.PRIJS_RIEM, GuhClothes.KATAPULT_RIEM));
        return offers;
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(KatapultFeature.KATAPULTSTER.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }

    private KatapultRole() {
    }
}
