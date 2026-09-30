package nl.juiced.guhs.feature.kaasmijn;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Mijnguh, behind his counter in the head of the kaasmijn. The first time he explains the mine and lends you a
 * pickaxe; whenever you come back without one you get another. After that, a mining tip and his little shop:
 * the miner outfit (only here), kaasknabbels, kaassaus, the kaashouweel, and goudkaas for a pile of kaasbrokken.
 * He also clears away the carts nobody rides any more.
 */
public final class Mijnguh implements NpcRole {
    public static final String MET_KEY = "guhs_kaasmijn_met";
    public static final int TIPS = 5;
    /** The whole mine around him (he sits in the middle of the head; the mine is 96 wide and 34 deep). */
    private static final int CLEANUP_EVERY = 200;
    /** How far around you he looks for a pickaxe you left lying about, before lending another. */
    private static final int LYING_AROUND = 24;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.8f);
        var saved = GuhQuests.saved(player);
        if (!saved.getBooleanOr(MET_KEY, false)) {
            saved.putBoolean(MET_KEY, true);
            GuhQuests.say(player, npc, "quest.guhs.kaasmijn.hello");
            lend(player, npc);
            return;
        }
        if (LeenhouweelItem.count(player) == 0) {
            if (lyingAround(player)) {
                GuhQuests.say(player, npc, "quest.guhs.kaasmijn.on_floor");   // (one each: pick that one up first)
            } else if (!player.getInventory().getSelectedItem().isEmpty() && player.getInventory().getFreeSlot() < 0) {
                GuhQuests.say(player, npc, "quest.guhs.kaasmijn.full");       // (no room: the shop still opens)
            } else {
                GuhQuests.say(player, npc, "quest.guhs.kaasmijn.loan");
                lend(player, npc);
                return;
            }
            npc.openShop(player);
            return;
        }
        GuhQuests.say(player, npc, "quest.guhs.kaasmijn.tip" + npc.getRandom().nextInt(TIPS));
        npc.openShop(player);
    }

    /** Is one of the Mijnguh's pickaxes lying on the floor near this player (they dropped it, or had no room)? */
    static boolean lyingAround(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(LYING_AROUND),
                e -> e.getItem().getItem() instanceof LeenhouweelItem).isEmpty();
    }

    /** A loaner pickaxe for the player (in the selected slot if that's free; no room: none, he says so). */
    public static boolean lend(ServerPlayer player, GuhNpcEntity npc) {
        ItemStack pickaxe = new ItemStack(KaasmijnFeature.LEENHOUWEEL.get());
        if (player.getInventory().getSelectedItem().isEmpty()) {
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), pickaxe);
        } else if (!player.getInventory().add(pickaxe)) {
            GuhQuests.say(player, npc, "quest.guhs.kaasmijn.full");
            return false;
        }
        GuhAdvancements.grant(player, "kaasmijn_leenhouweel");
        return true;
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % CLEANUP_EVERY == 0 && npc.level() instanceof ServerLevel level) {
            KarretjesautomaatBlock.cleanupCarts(level, new AABB(npc.blockPosition()).inflate(60, 44, 60), CLEANUP_EVERY);
        }
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        Item brok = KaasmijnFeature.KAASBROK.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(new ItemCost(brok, 6), null, new ItemStack(ModItems.clothingItem(GuhClothes.KAASMIJN_ZAKDOEK))));
        offers.add(offer(new ItemCost(brok, 10), null, new ItemStack(ModItems.clothingItem(GuhClothes.KAASMIJN_HELM))));
        offers.add(offer(new ItemCost(brok, 16), null, new ItemStack(ModItems.clothingItem(GuhClothes.KAASMIJN_OVERALL))));
        offers.add(offer(new ItemCost(brok, 4), null, new ItemStack(ModItems.KAAS_KNABBELS.get(), 12)));
        offers.add(offer(new ItemCost(brok, 3), new ItemCost(Items.BUCKET), new ItemStack(ModItems.KAAS_SAUS_BUCKET.get())));
        offers.add(offer(new ItemCost(KaasmijnFeature.GOUDKAAS.get(), 1), new ItemCost(brok, 16), new ItemStack(KaasmijnFeature.KAASHOUWEEL.get())));
        offers.add(offer(new ItemCost(brok, 32), null, new ItemStack(KaasmijnFeature.GOUDKAAS.get())));
        return offers;
    }

    private static MerchantOffer offer(ItemCost a, @Nullable ItemCost b, ItemStack result) {
        return new MerchantOffer(a, Optional.ofNullable(b), result, Integer.MAX_VALUE, 0, 0);
    }
}
