package nl.juiced.guhs.feature.einde2.reizen;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * einde2 phase B, slice reizen: its game events. The flights and the guests (every tick), the team round the campfires
 * (every second), the waiting spot of the whale questline (every half second); a player who leaves; actors of a flight
 * that a chunk brings back after a restart (thrown away); the dev command {@code /guhs einde2 reizen ...}.
 */
public final class E2ReizenEvents {
    private E2ReizenEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        E2Reizen.tick(server);
        int t = server.getTickCount();
        if (t % 20 == 11) {
            E2Team.tick(server);
        }
        if (t % 10 == 5) {
            Leegtewalvis.tick(server);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            E2Reizen.weg(p);
            Walvis.vergeet(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            E2Team.weg(p, null);
        }
    }

    /** An actor of a flight or a campfire copy that was saved with its chunk: its moment is over. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.loadedFromDisk() && E2Reizen.isTijdelijk(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        E2Reizen.leeg();
        E2Team.leeg();
        Leegtewalvis.leeg();
        Walvis.leeg();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        E2ReizenCommando.register(event);
    }
}
