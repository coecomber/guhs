package nl.juiced.guhs.feature.onderwater;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The Zeemeerguh, in front of the giant scallop in the middle of the Guhbubbel. The first time she explains the bubble
 * (pearls, kaaskoraal, the wreck); after that a tip. Always her little diving shop, paid with pearls: the duikhelm,
 * the duikpakje for your guh (only here), a saddle (for riding a Zeemeerguh) and kaaskoraal and mother-of-pearl to
 * build with. Meeting her fills in the Zeemeerguh page of your Guhdex.
 */
public final class ZeemeerguhRole implements NpcRole {
    public static final String MET_KEY = "guhs_onderwater_met";
    public static final int TIPS = 5;
    /** How close you must be to her for the Guhdex (like any guh: right next to her). */
    private static final double SEE_RANGE = 2.5;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.25f);
        npc.level().playSound(null, npc, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1f, 1f);
        seen(player);
        var saved = GuhQuests.saved(player);
        if (!saved.getBoolean(MET_KEY)) {
            saved.putBoolean(MET_KEY, true);
            GuhQuests.say(player, npc, "quest.guhs.onderwater.hello");
        } else {
            GuhQuests.say(player, npc, "quest.guhs.onderwater.tip" + npc.getRandom().nextInt(TIPS));
        }
        npc.openShop(player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 20 == 0) {
            for (ServerPlayer player : npc.level().getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(SEE_RANGE))) {
                seen(player);
            }
        }
    }

    /** The Zeemeerguh page of the Guhdex (the character and the wild variant share it). */
    public static void seen(ServerPlayer player) {
        GuhAdvancements.grant(player, "seen_" + GuhVariant.ZEEMEERGUH.id());
        GuhWorldData data = GuhWorldData.get(player.server);
        if (data.player(player.getUUID()).seen.add(GuhVariant.ZEEMEERGUH)) {
            data.setDirty();
            player.displayClientMessage(Component.translatable("gui.guhs.guhdex.new", GuhVariant.ZEEMEERGUH.displayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        Item parel = OnderwaterFeature.PAREL.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(new ItemCost(parel, 10), new ItemStack(OnderwaterFeature.DUIKHELM.get())));
        offers.add(offer(new ItemCost(parel, 4), new ItemStack(ModItems.clothingItem(GuhClothes.DUIKBRIL))));
        offers.add(offer(new ItemCost(parel, 5), new ItemStack(ModItems.clothingItem(GuhClothes.SNORKEL))));
        offers.add(offer(new ItemCost(parel, 7), new ItemStack(ModItems.clothingItem(GuhClothes.ZWEMBAND))));
        offers.add(offer(new ItemCost(parel, 6), new ItemStack(Items.SADDLE)));
        offers.add(offer(new ItemCost(parel, 2), new ItemStack(OnderwaterFeature.KAASKORAAL.get(), 4)));
        offers.add(offer(new ItemCost(parel, 2), new ItemStack(OnderwaterFeature.KAASKORAALBLOK.get(), 8)));
        offers.add(offer(new ItemCost(parel, 3), new ItemStack(OnderwaterFeature.PARELMOER.get(), 8)));
        offers.add(offer(new ItemCost(parel, 3), new ItemStack(OnderwaterFeature.PARELMOER_TEGELS.get(), 8)));
        offers.add(offer(new ItemCost(parel, 1), new ItemStack(ModItems.KAAS_KNABBELS.get(), 12)));
        return offers;
    }

    private static MerchantOffer offer(ItemCost cost, ItemStack result) {
        return new MerchantOffer(cost, Optional.empty(), result, Integer.MAX_VALUE, 0, 0);
    }
}
