package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

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
 * De Guhde (in the Kegelgrot): runs the games ({@link KegelGame}) and sells, for kegelknopen, the bowlingshirtje and the
 * zweetbandje, the building blocks (1 kegelknoop for 4) and the deco. Never sold out.
 */
final class KegelRole implements NpcRole {
    static final KegelRole INSTANCE = new KegelRole();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        KegelGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        KegelGame.of(npc).tick(npc);
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        KegelGame.antwoord(npc, player, optie);
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(prijs(KegelgrotFeature.PRIJS_ZWEETBANDJE, new ItemStack(ModItems.clothingItem(GuhClothes.KEGELGROT_ZWEETBANDJE))));
        offers.add(prijs(KegelgrotFeature.PRIJS_SHIRTJE, new ItemStack(ModItems.clothingItem(GuhClothes.KEGELGROT_BOWLINGSHIRTJE))));
        for (var item : KegelgrotFeature.BOUWBLOKKEN) {
            offers.add(prijs(1, new ItemStack(item.get(), 4)));
        }
        offers.add(prijs(KegelgrotFeature.PRIJS_KEGELTJE, new ItemStack(KegelgrotFeature.KEGELTJE_ITEM.get())));
        offers.add(prijs(KegelgrotFeature.PRIJS_NEON, new ItemStack(KegelgrotFeature.NEON_KEGELBORD_ITEM.get())));
        offers.add(prijs(KegelgrotFeature.PRIJS_REK, new ItemStack(KegelgrotFeature.WOLBALREK_ITEM.get())));
        return offers;
    }

    private static MerchantOffer prijs(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(KegelgrotFeature.KEGELKNOOP.get(), price), result, Integer.MAX_VALUE, 0, 0);
    }

    private KegelRole() {
    }
}
