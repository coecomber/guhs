package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * De Erfenis: the fixed story moments (Sherlock's cases start on these, never at random). The kern fires them; the
 * sherlock slice (and anyone else) listens with {@link #luister}. Ids (contract): {@code brief_gelezen}, {@code aangekomen},
 * {@code plukje_opgeruimd:<n>}, {@code vondst:<id>}, {@code gebouw_gebouwd:<id>}, {@code gebouw_level:<id>:<lvl>},
 * {@code hoeve_level:<n>}, {@code bestelling_geleverd:<totaal>}, {@code bewoners:<totaal>}, {@code knusheid:<totaal>}.
 * <p>
 * A moment for a player who is offline waits (saved with the erfenis data) and is delivered at the next login, in order.
 */
public final class ErfenisMomenten {
    private static final List<BiConsumer<ServerPlayer, String>> LUISTERAARS = new CopyOnWriteArrayList<>();

    private ErfenisMomenten() {
    }

    /** Listen to every moment (called on the server thread, with the player online). */
    public static void luister(BiConsumer<ServerPlayer, String> l) {
        LUISTERAARS.add(l);
    }

    /** A moment for this player: now when online, else at the next login. */
    public static void melding(MinecraftServer server, UUID speler, String moment) {
        ServerPlayer p = server.getPlayerList().getPlayer(speler);
        if (p == null) {
            ErfenisData.get(server).bewaarMoment(speler, moment);
            return;
        }
        for (BiConsumer<ServerPlayer, String> l : LUISTERAARS) {
            try {
                l.accept(p, moment);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Erfenis: moment {} listener failed", moment, e);
            }
        }
    }

    /** Login: the moments that waited. */
    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MinecraftServer s = p.level().getServer();
            for (String m : ErfenisData.get(s).haalMomenten(p.getUUID())) {
                melding(s, p.getUUID(), m);
            }
        }
    }
}
