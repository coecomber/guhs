package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De smid-guh (SMIDGUH) in the Bliksemsmidse: soot on his nose, short sentences on the beat of his hammer. He presses
 * clouds into blocks: you bring wolkenpluis, he gives cloud blocks, the cloud furniture of the Wolkenweide and a few
 * pieces only he makes. His shop is the trading screen every guh shop uses; the prices ({@link #AANBOD}) are in
 * wolkenpluis, which a wolkenschaapje gives 1 to 3 of per shearing, every five minutes: a small herd pays for a bed in
 * a few minutes, and plain cloud costs what the crafting grid asks.
 */
public final class Smidguh implements NpcRole {
    public static final Smidguh ROL = new Smidguh();

    /** One line of his shop: this much wolkenpluis for that many of a thing (a block or item id of this mod). */
    public record Ruil(int pluis, String id, int aantal) {
    }

    public static final List<Ruil> AANBOD = List.of(
            new Ruil(4, "wolkenblok_wit", 4), new Ruil(4, "wolkenblok_roze", 4), new Ruil(5, "bliksemsmidse_wolk", 4), new Ruil(8, "regenboogblok", 6),
            new Ruil(5, "wolkenlamp", 1), new Ruil(8, "wolkenbank", 1), new Ruil(10, "wolkenbed", 1),
            new Ruil(6, "bliksemsmidse_wolkentafel", 1), new Ruil(5, "bliksemsmidse_wolkenplank", 1), new Ruil(12, "bliksemsmidse_onweerswolkje", 1));
    public static final int HALLO_REGELS = 4;

    /** Who has his shop open: how many trades they had done when they opened it (the "traded" statistic). */
    private static final Map<UUID, Integer> KLANTEN = new ConcurrentHashMap<>();

    public static Item pluis() {
        return Bio.item("wolkenpluis", Items.WHITE_WOOL);
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        boolean heeft = player.getInventory().countItem(pluis()) > 0;
        GuhQuests.say(player, npc, heeft ? "gui.guhs.smidguh.hallo." + npc.getRandom().nextInt(HALLO_REGELS) : "gui.guhs.smidguh.geen_pluis");
        npc.playSound(BouwWolk2Slice.HAMER.get(), 0.6f, 0.95f + npc.getRandom().nextFloat() * 0.15f);
        KLANTEN.put(player.getUUID(), geruild(player));
        npc.openShop(player);
    }

    private static int geruild(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
    }

    /** While somebody shops: did they buy something? The first time is the proof for the quest, and he says so. */
    @Override
    public void tick(GuhNpcEntity npc) {
        if (KLANTEN.isEmpty() || !(npc.level() instanceof ServerLevel level)) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Integer>> it = KLANTEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> k = it.next();
            if (!(level.getPlayerByUUID(k.getKey()) instanceof ServerPlayer p)) {
                if (level.getServer().getPlayerList().getPlayer(k.getKey()) == null) {
                    it.remove();
                }
                continue;
            }
            if (p.distanceToSqr(npc) > 100) {
                continue;                                         // (the customer of another smid)
            }
            int nu = geruild(p);
            if (nu > k.getValue()) {
                k.setValue(nu);
                geruild(npc, p);
            }
            if (!npc.isCustomer(p)) {
                it.remove();
            }
        }
    }

    /** This player just traded with him. */
    public static void geruild(GuhNpcEntity npc, ServerPlayer player) {
        npc.playSound(BouwWolk2Slice.HAMER.get(), 0.7f, 1.0f);
        var adv = player.level().getServer().getAdvancements().get(nl.juiced.guhs.Guhs.id("quest/smidguh_eerste_ruil"));
        if (adv != null && !player.getAdvancements().getOrStartProgress(adv).isDone()) {
            GuhQuests.say(player, npc, "gui.guhs.smidguh.geruild");
        }
        GuhAdvancements.grant(player, "smidguh_eerste_ruil");
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        for (Ruil r : AANBOD) {
            offers.add(new MerchantOffer(new ItemCost(pluis(), r.pluis()), new ItemStack(Bio.item(r.id(), Items.WHITE_WOOL), r.aantal()), Integer.MAX_VALUE, 0, 0));
        }
        return offers;
    }

    private Smidguh() {
    }
}
