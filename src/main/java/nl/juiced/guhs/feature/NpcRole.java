package nl.juiced.guhs.feature;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * What a guh character (GuhNpcEntity) of a feature does: talk when right-clicked, tick, and (optionally) run a shop.
 * A feature's role is found by its NPC kind in {@link Features#role}. State of the character goes in npc.roleData.
 */
public interface NpcRole {
    /** Right-clicked by a player (server side). */
    void talk(GuhNpcEntity npc, ServerPlayer player);

    /** Every tick (server side). */
    default void tick(GuhNpcEntity npc) {
    }

    /** The shop (trades), or null for none. Opened with npc.openShop(player). */
    @Nullable
    default MerchantOffers offers(GuhNpcEntity npc) {
        return null;
    }

    /**
     * 3.0: the answer (option id) the player picked in the talking screen this NPC opened (nl.juiced.guhs.feature.verhaal.Praat,
     * the PraatScherm without a "sleutel"); -1 = the screen was read to the end / closed.
     */
    default void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
    }
}
