package nl.juiced.guhs.feature.beroepen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Right-clicking the beroepen's guhtjes: the guhtje in the tree (Blusguh's job) and Snotje (the Apotheek's). They
 * aren't tamed, fed or dressed: the click is theirs.
 */
public final class BeroepenEvents {
    static boolean vanOns(Entity e) {
        var d = e.getPersistentData();
        return e instanceof GuhEntity && (d.contains(Brandweer.BOOMGUHTJE) || d.contains(Apotheek.SNOTJE));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !vanOns(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GuhEntity g = (GuhEntity) event.getTarget();
        if (g.getPersistentData().getBooleanOr(Brandweer.BOOMGUHTJE, false)) {
            Brandweer.red(player, g);
        } else if (g.getPersistentData().contains(Apotheek.SNOTJE)) {
            Apotheek.snotje(player, g, event.getItemStack());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide() && vanOns(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private BeroepenEvents() {
    }
}
