package nl.juiced.guhs.feature.vadswoud;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Boswachterguh, in the ranger's hut high in the Moederboom of the boomhutdorp. The first time he tells you about
 * the guh families; after that a tip about the forest. His shop (for kaasknabbels): vadshout, saplings (four for a giant
 * guh tree), guh nests, rope, bark faces, and the ranger outfit (hat and jacket, only here).
 */
public final class Boswachterguh implements NpcRole {
    public static final String MET_KEY = "guhs_vadswoud_boswachter";
    public static final int TIPS = 6;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.85f);
        var saved = GuhQuests.saved(player);
        if (!saved.getBoolean(MET_KEY)) {
            saved.putBoolean(MET_KEY, true);
            GuhQuests.say(player, npc, "quest.guhs.vadswoud.boswachter.hello");
        } else {
            GuhQuests.say(player, npc, "quest.guhs.vadswoud.boswachter.tip" + npc.getRandom().nextInt(TIPS));
        }
        VadsAdvancements.grant(player, "vadswoud_boswachter_praat");
        npc.openShop(player);
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        Item knabbels = ModItems.KAAS_KNABBELS.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(knabbels, 3, VadswoudFeature.VADSHOUT_STAM.get(), 8));
        offers.add(offer(knabbels, 2, VadswoudFeature.VADSHOUT_PLANKEN.get(), 16));
        offers.add(offer(knabbels, 5, VadswoudFeature.VADSHOUT_ZAAILING.get(), 2));
        offers.add(offer(knabbels, 9, VadswoudFeature.VADSHOUT_ZAAILING.get(), 4));
        offers.add(offer(knabbels, 4, VadswoudFeature.GUHNESTJE.get(), 1));
        offers.add(offer(knabbels, 3, VadswoudFeature.VADSTOUW.get(), 8));
        offers.add(offer(knabbels, 4, VadswoudFeature.VADSHOUT_GEZICHT.get(), 2));
        offers.add(offer(knabbels, 10, ModItems.clothingItem(GuhClothes.BOSWACHTERSHOED), 1));
        offers.add(offer(knabbels, 14, ModItems.clothingItem(GuhClothes.BOSWACHTERSJAS), 1));
        return offers;
    }

    static MerchantOffer offer(ItemLike cost, int price, ItemLike result, int count) {
        return new MerchantOffer(new ItemCost(cost, price), Optional.empty(), new ItemStack(result, count), Integer.MAX_VALUE, 0, 0);
    }
}
