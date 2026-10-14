package nl.juiced.guhs.feature.ringvlucht;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.4.2 (rookguh): the game events of the Rookguh-vlucht.
 * <ul>
 *   <li>Every {@link #ONTHOUD_TICKS} ticks a player's copies are remembered ({@link Vlucht#onthoudRond}).</li>
 *   <li>A player the Rookguh just put down is not hurt by anything for a moment ({@link Vlucht#netGeland}; /kill and the
 *       void still work).</li>
 *   <li>A logout forgets the flight (they stay where they took off).</li>
 * </ul>
 */
public final class RingVluchtEvents {
    public static final int ONTHOUD_TICKS = 100;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.isSpectator() && (p.tickCount + p.getId()) % ONTHOUD_TICKS == 0) {
            Vlucht.onthoudRond(p);
        }
    }

    @SubscribeEvent
    public static void onSchade(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && Vlucht.netGeland(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Vlucht.vergeet(event.getEntity().getUUID());
    }

    private RingVluchtEvents() {
    }
}
