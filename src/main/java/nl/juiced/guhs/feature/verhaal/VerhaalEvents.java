package nl.juiced.guhs.feature.verhaal;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import nl.juiced.guhs.entity.GuhEntity;

/** 3.0: the game-bus side of the story guhs (registered by {@link VerhaalFeature#register}). */
public final class VerhaalEvents {
    /** A guh placed by a template with the story keys becomes a real story copy. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof GuhEntity guh && VerhaalGuhs.isKopie(guh)) {
            VerhaalGuhs.opJoin(guh);
        }
    }

    /** Story copies are never tamed, and a story variant is never tamed the normal way (only VerhaalGuhs.tem). */
    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof GuhEntity guh && (VerhaalGuhs.isKopie(guh) || guh.getVariant().isVerhaalGuh())) {
            event.setCanceled(true);
        }
    }

    private VerhaalEvents() {
    }
}
