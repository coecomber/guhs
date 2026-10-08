package nl.juiced.guhs.feature.ringh6;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * bbq2 (ring-h6): the events of the Frituurberg (registered by {@link RingH6Feature}).
 * <ul>
 *   <li>Every tick of a player on a mountain: {@link Klim#tik} (no fire, the frituur, a long fall, Smikagol's grab); once a
 *       second: the chapter's upkeep ({@link Klim#seconde}).</li>
 *   <li>A player on a Frituurberg is never hurt by fire, the frituur or a fall: whoever climbs it, whoever visits it.</li>
 *   <li>A player the Rookguhs just put down at home is not hurt by anything for a moment ({@link Thuis#netGeland}).</li>
 *   <li>Logging out and the server stopping: nothing of a climb is left behind.</li>
 * </ul>
 */
public final class RingH6Events {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Klim.tik(p);
        if ((p.tickCount + p.getId()) % 20 == 7) {
            Klim.seconde(p);
        }
    }

    @SubscribeEvent
    public static void onSchade(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (Thuis.netGeland(p)) {
            event.setCanceled(true);                          // (1.4.1: the Rookguhs just put them down at home: nothing hurts)
            return;
        }
        boolean vuur = event.getSource().is(DamageTypeTags.IS_FIRE);
        if (Berg.van(p) != null && (vuur || event.getSource().is(DamageTypeTags.IS_FALL))) {
            event.setCanceled(true);
        } else if (vuur && Klim.beschermdOpWeg(p)) {
            // (PHASE3 R15) on the way to the mountain, across the frituur sea: the ring bearer of this chapter is not burnt
            event.setCanceled(true);
            p.clearFire();
        }
    }

    @SubscribeEvent
    public static void onUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            vergeet(p);
        }
    }

    @SubscribeEvent
    public static void onDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Berg.vergeet(p.getUUID());
            Klim.vergeet(p.getUUID());
            Kolen.vergeet(p.getUUID());
        }
    }

    private static void vergeet(ServerPlayer p) {
        Berg.vergeet(p.getUUID());
        Klim.vergeet(p.getUUID());
        Kolen.vergeet(p.getUUID());
        Thuis.vergeet(p.getUUID());
    }

    @SubscribeEvent
    public static void onGestopt(ServerStoppedEvent event) {
        Klim.wisAlles();
        Kolen.wisAlles();
    }

    private RingH6Events() {
    }
}
