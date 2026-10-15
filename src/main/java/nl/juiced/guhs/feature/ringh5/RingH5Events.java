package nl.juiced.guhs.feature.ringh5;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * bbq2 (ring-h5): the game-bus events of chapter 5. Per player: once a second the chapter looks where they are
 * ({@link Hoofdstuk#seconde}: the steps, Smikagol's route, the smoke), every other tick what pushes them back
 * ({@link Hoofdstuk#snel}: the smoke, the locked side door). The Eye, the guards and the riders tick themselves. And no
 * player is ever burnt inside the valley.
 */
public final class RingH5Events {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        int klok = p.tickCount + p.getId();
        if (klok % 20 == 0) {
            Hoofdstuk.seconde(p);
        }
        if (klok % 2 == 0) {
            Hoofdstuk.snel(p);
        }
    }

    /**
     * Nothing in the valley burns a player: the world may let a spring of kaasfrituursaus well up in its rock (it burns
     * like lava), and {@link Hoofdstuk#dempSaus} only clears it once somebody is near.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().is(DamageTypeTags.IS_FIRE) && Terrein.kent(p.level(), p.position())) {
            event.setCanceled(true);
            p.clearFire();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Hoofdstuk.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Hoofdstuk.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        Hoofdstuk.wisAlles();
    }

    private RingH5Events() {
    }
}
