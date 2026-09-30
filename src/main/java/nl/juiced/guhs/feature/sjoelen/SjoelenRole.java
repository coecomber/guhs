package nl.juiced.guhs.feature.sjoelen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/** Opoe Njegschuif (in the Sjoelhuisje): runs the sjoel turns ({@link SjoelGame}) and sells her knitted sjoel outfit. */
final class SjoelenRole implements NpcRole {
    static final SjoelenRole INSTANCE = new SjoelenRole();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        SjoelGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        SjoelGame.of(npc).tick(npc);
    }

    /** Only here: the sjoelpetje, the sjoelbroche and the sjoelvestje, for sjoelschijfjes. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(SjoelenFeature.PRIJS_PETJE, GuhClothes.SJOELEN_PETJE));
        offers.add(offer(SjoelenFeature.PRIJS_BROCHE, GuhClothes.SJOELEN_BROCHE));
        offers.add(offer(SjoelenFeature.PRIJS_VESTJE, GuhClothes.SJOELEN_VESTJE));
        return offers;
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(SjoelenFeature.SJOELSCHIJFJE.get(), price), new ItemStack(ModItems.clothingItem(clothes)),
                Integer.MAX_VALUE, 0, 0);
    }

    private SjoelenRole() {
    }
}
