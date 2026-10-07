package nl.juiced.guhs.feature.snuffeldorp;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.snuffel.Eiland;

/** The game-bus events of Snuffeldorp: the story's tick for every player, the moestuin that is not trampled, and forgetting who stood where. */
public final class DorpEvents {
    private DorpEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Dorp.tick(p);
        }
    }

    /** Tuinder Knolletje's beds stay beds: a dog that jumps into the moestuin tramples nothing. */
    @SubscribeEvent
    public static void onVertrappen(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && Eiland.in(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Wegversperring.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        Wegversperring.wis();
    }
}
