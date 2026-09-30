package nl.juiced.guhs.feature.doolhof;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
 * Meneer Vadskronkel (DOOLHOFGUH) at the entrance of Het Guhdoolhof: explains the maze, grows a new one for your level
 * ({@link DoolhofGame}), calls passers-by, and sells the explorer's outfit and hedge things for doolhofknabbels.
 * The world's top 3 of every level floats above his head (and over the exit gate).
 */
public class DoolhofRole implements NpcRole {
    /** Prices in doolhofknabbels (petje ~4, accessory ~6, bigger piece ~8-10). */
    public static final int HOEDJE = 4, RUGZAKJE = 8, KOMPAS = 6;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        DoolhofGame.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel world)) {
            return;
        }
        if (npc.tickCount % 100 == 1) {
            DoolhofGame.showScores(npc);
        }
        DoolhofGame game = DoolhofGame.of(npc);
        if (game != null) {
            game.tick(npc);
        } else if (npc.tickCount % (20 * 30) == 0) {
            Player near = world.getNearestPlayer(npc, 10);
            if (near instanceof ServerPlayer p && !DoolhofGame.isPlaying(p) && !p.isSpectator()) {
                p.sendOverlayMessage(Component.literal("<").append(npc.getDisplayName()).append("> ")
                        .append(Component.translatable("quest.guhs.doolhof.invite" + (1 + world.getRandom().nextInt(3)))).withStyle(ChatFormatting.GREEN));
                npc.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 1f, 0.9f);
            }
        }
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(HOEDJE, GuhClothes.DOOLHOF_HOEDJE));
        offers.add(clothes(KOMPAS, GuhClothes.DOOLHOF_KOMPAS));
        offers.add(clothes(RUGZAKJE, GuhClothes.DOOLHOF_RUGZAKJE));
        offers.add(offer(1, DoolhofFeature.HEG_ITEM.get(), 8));
        offers.add(offer(2, DoolhofFeature.HEG_GEZICHT_ITEM.get(), 2));
        offers.add(offer(2, DoolhofFeature.LANTAARN_ITEM.get(), 2));
        offers.add(offer(1, ModItems.KAAS_KNABBELS.get(), 10));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(DoolhofFeature.DOOLHOFKNABBEL.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer offer(int price, Item item, int count) {
        return new MerchantOffer(new ItemCost(DoolhofFeature.DOOLHOFKNABBEL.get(), price), new ItemStack(item, count), Integer.MAX_VALUE, 0, 0);
    }
}
