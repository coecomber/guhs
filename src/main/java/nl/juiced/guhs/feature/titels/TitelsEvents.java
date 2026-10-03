package nl.juiced.guhs.feature.titels;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Where the chosen title shows:
 * <ul>
 *   <li>the player list: {@link PlayerEvent.TabListNameFormat} (server; as the two old titles did);</li>
 *   <li>chat (and every other message with the player's name: /say, /me, death messages...): {@link PlayerEvent.NameFormat}
 *   on the server, the display name vanilla's chat decoration uses;</li>
 *   <li>above the head: the same {@link PlayerEvent.NameFormat}, fired on the client for the players it renders; the
 *   client knows their titles from guhs:titels_actief ({@link Titels#clientTitel}).</li>
 * </ul>
 * Both events append to the name another mod may have set already (nicknames, ranks), never replace it.
 */
public final class TitelsEvents {
    /** How often (ticks) the titles of a player are checked (newly earned, the one that shows). */
    public static final int CHECK_TICKS = 40;

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Titels.Titel t = Titels.actief(player);
            if (t != null) {
                Component base = event.getDisplayName() != null ? event.getDisplayName() : player.getName();
                event.setDisplayName(Titels.metTitel(base, t));
            }
        }
    }

    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat event) {
        Titels.Titel t = event.getEntity() instanceof ServerPlayer player ? Titels.actief(player)
                : event.getEntity().level().isClientSide() ? Titels.clientTitel(event.getEntity().getUUID()) : null;
        if (t != null) {
            event.setDisplayname(Titels.metTitel(event.getDisplayname(), t));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Titels.kijk(p);
            Titels.ververs(p);   // (the newcomer learns everybody's title, everybody learns the newcomer's)
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Titels.vergeet(p);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % CHECK_TICKS == 0) {
            Titels.kijk(p);
        }
    }

    private TitelsEvents() {
    }
}
