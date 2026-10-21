package nl.juiced.guhs.feature.beverbeek;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * The beaver of a ranger post: a safari bever with the tag {@link #TAG}. You can pet it (right-click: hearts, a happy
 * squeak, a line in the action bar), and that is all: no taming, no lead, no picking up, no board (the click is taken
 * here before anything else sees it).
 */
public final class Aaibever {
    public static final String TAG = "guhs_aaibever";
    public static final int REGELS = 4;

    public static boolean is(Entity e) {
        return e instanceof SafariDier d && d.entityTags().contains(TAG);
    }

    static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        if (!is(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        aai(level, (SafariDier) event.getTarget());
        event.getEntity().sendOverlayMessage(Component.translatable("gui.guhs.beverbeek.aai." + level.getRandom().nextInt(REGELS))
                .withStyle(ChatFormatting.GOLD));
    }

    static void opInteractieSpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        if (is(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** One pet: hearts, its own sound and its happy animation. */
    public static void aai(ServerLevel level, SafariDier bever) {
        level.sendParticles(ParticleTypes.HEART, bever.getX(), bever.getY() + bever.getBbHeight() + 0.3, bever.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        bever.playAmbientSound();
        bever.speelActie("blij", 40);
    }

    private Aaibever() {
    }
}
