package nl.juiced.guhs.feature.vadswoud;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Knabbelplukker, in her guh-head hut next to the berry garden of the boomhutdorp. She trades knabbelbessen both
 * ways (and bakes pies of them), and has the picker's outfit (cap and basket, only here). Now and then she eats the
 * harvest herself. Njeg.
 */
public final class Knabbelplukker implements NpcRole {
    public static final String MET_KEY = "guhs_vadswoud_plukker";
    public static final int TIPS = 5;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        var saved = GuhQuests.saved(player);
        if (!saved.getBoolean(MET_KEY)) {
            saved.putBoolean(MET_KEY, true);
            GuhQuests.say(player, npc, "quest.guhs.vadswoud.plukker.hello");
        } else {
            GuhQuests.say(player, npc, "quest.guhs.vadswoud.plukker.tip" + npc.getRandom().nextInt(TIPS));
        }
        VadsAdvancements.grant(player, "vadswoud_plukker_praat");
        npc.openShop(player);
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        Item bessen = VadswoudFeature.KNABBELBESSEN.get();
        Item knabbels = ModItems.KAAS_KNABBELS.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(Boswachterguh.offer(knabbels, 4, bessen, 12));
        offers.add(Boswachterguh.offer(bessen, 12, knabbels, 6));
        offers.add(Boswachterguh.offer(bessen, 9, VadswoudFeature.KNABBELBESSENTAARTJE.get(), 2));
        offers.add(Boswachterguh.offer(bessen, 10, ModItems.GEFRITUURDE_KAASKNABBELS.get(), 2));
        offers.add(Boswachterguh.offer(bessen, 16, VadswoudFeature.GUHNESTJE.get(), 1));
        offers.add(Boswachterguh.offer(bessen, 20, ModItems.clothingItem(GuhClothes.PLUKMUTS), 1));
        offers.add(Boswachterguh.offer(bessen, 28, ModItems.clothingItem(GuhClothes.PLUKMANDJE), 1));
        return offers;
    }
}
