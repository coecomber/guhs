package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Tikiguh's Tiki stall on the surf beach of Guhwai'i (3.0): a guh behind a big carved tiki mask who sells the Tiki
 * decorations for schelpjesmunten (what you earn by surfing and dancing the hula). No clothes here: only lovely things for
 * at home. The offers never run out.
 */
public final class TikiWinkel {
    /** What Tikiguh sells: the item, how many, and the price in schelpjesmunten (the shop's order). */
    public record Aanbod(ItemLike item, int aantal, int prijs) {
    }

    public static Map<String, Aanbod> aanbod() {
        Map<String, Aanbod> a = new LinkedHashMap<>();
        a.put("fakkel", new Aanbod(GuhwaiiSpellenBlocks.TIKI_FAKKEL.get(), 2, 2));
        a.put("masker", new Aanbod(GuhwaiiSpellenBlocks.TIKI_MASKER.get(), 1, 3));
        a.put("masker_roze", new Aanbod(GuhwaiiSpellenBlocks.TIKI_MASKER_ROZE.get(), 1, 3));
        a.put("beeld", new Aanbod(GuhwaiiSpellenBlocks.TIKI_BEELD.get(), 1, 6));
        a.put("rietdak", new Aanbod(GuhwaiiSpellenBlocks.TIKI_RIETDAK.get(), 8, 2));
        a.put("rietdak_trap", new Aanbod(GuhwaiiSpellenBlocks.TIKI_RIETDAK_TRAP.get(), 8, 2));
        a.put("rietdak_plaat", new Aanbod(GuhwaiiSpellenBlocks.TIKI_RIETDAK_PLAAT.get(), 8, 1));
        a.put("bloemenslinger", new Aanbod(GuhwaiiSpellenBlocks.HIBISCUS_SLINGER.get(), 4, 2));
        a.put("schelpjeslampion", new Aanbod(GuhwaiiSpellenBlocks.SCHELPJES_LAMPION.get(), 2, 3));
        a.put("surfplankrek", new Aanbod(GuhwaiiSpellenBlocks.SURFPLANK_REK.get(), 1, 5));
        a.put("bloemenmat", new Aanbod(GuhwaiiSpellenBlocks.HULA_BLOEMENMAT.get(), 4, 2));
        a.put("kruk", new Aanbod(GuhwaiiSpellenBlocks.TIKI_KRUK.get(), 2, 2));
        a.put("radiootje", new Aanbod(GuhwaiiSpellenBlocks.TIKI_RADIOOTJE.get(), 1, 10));
        return a;
    }

    public static MerchantOffers offers() {
        MerchantOffers offers = new MerchantOffers();
        for (Aanbod a : aanbod().values()) {
            offers.add(new MerchantOffer(new ItemCost(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), a.prijs()), new ItemStack(a.item(), a.aantal()),
                    Integer.MAX_VALUE, 0, 0));
        }
        return offers;
    }

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        int munten = GuhQuests.count(player, GuhwaiiSpellenBlocks.SCHELPJESMUNT.get());
        GuhQuests.say(player, npc, munten == 0 ? "quest.guhs.guhwaiispellen.tiki.blut"
                : "quest.guhs.guhwaiispellen.tiki.hallo" + (1 + player.getRandom().nextInt(4)), munten);
        npc.openShop(player);
    }

    private TikiWinkel() {
    }
}
