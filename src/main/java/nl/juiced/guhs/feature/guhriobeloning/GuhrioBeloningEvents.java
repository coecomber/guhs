package nl.juiced.guhs.feature.guhriobeloning;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The game-bus events of guhrio-beloning: every tick of a player (a head against a ?-block, sneaking on a green pipe, a
 * pipe trip under way), nothing hurts a player inside a pipe, and forgetting a player who leaves.
 */
public final class GuhrioBeloningEvents {
    private GuhrioBeloningEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Vraagblok.kijk(p);
            Pijpreis.tick(p);
        }
    }

    /** A player inside a pipe is not there for anything that would hurt them. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Pijpreis.onderweg(p)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Vraagblok.wis(event.getEntity().getUUID());
        Pijpreis.wis(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        Vraagblok.wisAlles();
        Pijpreis.wisAlles();
    }
}
