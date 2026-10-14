package nl.juiced.guhs.feature.park.plek;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.4.2 (Het Knabbelpark, de picknickweide): de kraampjesguh behind the counter of the picknick kraampje. A cheerful word
 * and the shop: picnic treats, picnic blankets and a playground of your own, all for vads (vahoege vads). Every player,
 * as often as they like; nothing runs out.
 */
public final class Kraamguh implements NpcRole {
    public static final NpcRole ROL = new Kraamguh();
    /** How many greetings there are (quest.guhs.parkplek.kraam.hallo.0..). */
    public static final int GROETEN = 4;

    private Kraamguh() {
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GuhQuests.say(player, npc, "quest.guhs.parkplek.kraam.hallo." + player.getRandom().nextInt(GROETEN));
        GuhAdvancements.grant(player, "parkplek_kraampje");
        npc.openShop(player);
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(1, ModItems.KAAS_KNABBELS.get(), 12));
        offers.add(offer(1, Items.COOKIE, 6));
        offers.add(offer(1, Items.APPLE, 4));
        offers.add(offer(1, Items.SWEET_BERRIES, 8));
        offers.add(offer(2, Items.CAKE, 1));
        offers.add(offer(1, PlekA.PICKNICKKLEED_ITEM.get(), 4));
        offers.add(offer(2, ParkFeature.BANKJE_ITEM.get(), 2));
        offers.add(offer(3, PlekA.GLIJBAAN_ITEM.get(), 3));
        offers.add(offer(3, PlekA.SCHOMMEL_ITEM.get(), 1));
        offers.add(offer(3, PlekA.WIP_ITEM.get(), 1));
        return offers;
    }

    private static MerchantOffer offer(int vads, Item item, int count) {
        return new MerchantOffer(new ItemCost(ModItems.VAHOEGE_VADS.get(), vads), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }
}
