package nl.juiced.guhs.feature.guhpixel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Where a {@link Rang} shows: a PREFIX "[VADS] " before the name in chat and every other message with the name
 * ({@link PlayerEvent.NameFormat} on the server), in the player list ({@link PlayerEvent.TabListNameFormat}) and above the
 * head (NameFormat on the client, which knows the ranks from guhs:guhpixel_rangen). Only for players who unlocked
 * Guhpixel and did not switch it off in the Guhdex tab. The titles (feature.titels) add their suffix by themselves: both
 * listeners build on the name that is already there.
 */
public final class Rangen {
    private static final String UIT = "RangUit";
    /** Client: the ranks of the players on the server (ordinals). */
    private static volatile Map<UUID, Integer> client = Map.of();

    /** Does this player show a rank (unlocked, and not switched off)? */
    public static boolean toont(ServerPlayer p) {
        return Toegang.heeft(p) && !Muntjes.data(p).getBooleanOr(UIT, false);
    }

    public static boolean aan(ServerPlayer p) {
        return !Muntjes.data(p).getBooleanOr(UIT, false);
    }

    /** The player switches the prefix on or off (Guhdex tab). */
    public static void zet(ServerPlayer p, boolean aan) {
        Muntjes.data(p).putBoolean(UIT, !aan);
        PxData.vuil(p.level().getServer());
        ververs(p);
    }

    @Nullable
    public static Rang getoond(ServerPlayer p) {
        return toont(p) ? Muntjes.rang(p) : null;
    }

    /** "[VADS] Naam". */
    public static Component metRang(Component naam, Rang r) {
        return Component.empty().append(r.naam()).append(" ").append(naam);
    }

    /** Recomputes this player's names and tells every client who shows which rank. */
    public static void ververs(ServerPlayer p) {
        p.refreshDisplayName();
        p.refreshTabListName();
        MinecraftServer server = p.level().getServer();
        if (server != null) {
            GuhpixelPayloads.Rangen stand = stand(server);
            for (ServerPlayer ander : server.getPlayerList().getPlayers()) {
                ModNetworking.sendTo(ander, stand);
            }
        }
    }

    static GuhpixelPayloads.Rangen stand(MinecraftServer server) {
        Map<UUID, Integer> out = new LinkedHashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Rang r = getoond(p);
            if (r != null) {
                out.put(p.getUUID(), r.ordinal());
            }
        }
        return new GuhpixelPayloads.Rangen(out);
    }

    /** (Client) the server's list. */
    public static void zetClient(Map<UUID, Integer> rangen) {
        client = Map.copyOf(rangen);
    }

    @Nullable
    private static Rang clientRang(UUID speler) {
        Integer r = client.get(speler);
        return r == null ? null : Rang.op(r);
    }

    // (HIGH: before the titles' suffix, so "[VADS] Naam ✿ Titel" is built from the inside out either way)
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Rang r = getoond(p);
            if (r != null) {
                Component basis = event.getDisplayName() != null ? event.getDisplayName() : p.getName();
                event.setDisplayName(metRang(basis, r));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onName(PlayerEvent.NameFormat event) {
        Rang r = event.getEntity() instanceof ServerPlayer p ? getoond(p)
                : event.getEntity().level().isClientSide() ? clientRang(event.getEntity().getUUID()) : null;
        if (r != null) {
            event.setDisplayname(metRang(event.getDisplayname(), r));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ververs(p);
            GuhpixelPayloads.hud(p);
        }
    }

    private Rangen() {
    }
}
