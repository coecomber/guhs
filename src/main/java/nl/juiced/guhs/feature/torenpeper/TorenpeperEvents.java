package nl.juiced.guhs.feature.torenpeper;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Game-bus events of bbq2 (toren-peper): once a second the lost Rookguhs of a player at step 3 of the lighthouse's
 * questline ({@link Vuurtoren#tik}), every two seconds "did you brew your first pepper drink?" ({@link Pepertuin#gebrouwen}),
 * a pepper that a player stirs into a Guhbrouwketel themselves ({@link Pepertuin#eigenPeper}),
 * and the player's own plants of the kweekbakken to their client whenever it starts afresh ({@link Kweek#sync}).
 */
public final class TorenpeperEvents {
    private TorenpeperEvents() {
    }

    @SubscribeEvent
    public static void opSpelerTik(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (p.tickCount % 20 == 7) {
            Vuurtoren.tik(p);
        }
        if (p.tickCount % 40 == 13) {
            Pepertuin.gebrouwen(p);
        }
    }

    /**
     * A right-click on a block, after everybody else had their say and just before the block is used: a brewing pepper that
     * goes into a Guhbrouwketel counts for its own player ({@link Pepertuin#eigenPeper}). Sneaking with something in a hand
     * skips the block, so that is no stirring.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void opRechtsklik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.isCanceled() && event.getUseBlock() != TriState.FALSE && !p.isSecondaryUseActive()) {
            Pepertuin.eigenPeper(p, event.getPos(), event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void opInloggen(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Kweek.sync(p);
        }
    }

    @SubscribeEvent
    public static void opOpnieuw(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Kweek.sync(p);
        }
    }

    @SubscribeEvent
    public static void opAndereWereld(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Kweek.sync(p);
        }
    }
}
