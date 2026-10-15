package nl.juiced.guhs.feature.bestaand;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * bbq2 (bestaand): the clicks that are not clicks on a block of this slice. A tuft of Mikakruid is air in the world (only
 * the player who must pull it sees it, {@link Schijn}), and a plush cage is made of the grillpaleis's own blocks; both are
 * recognised by WHERE the click is (the template position in the building around it). The interact events fire on the
 * server before it looks at the real block, so a click on a block that only the client knows arrives here all the same.
 * A click that was handled is cancelled: nothing is placed against a cage or into a weed.
 */
public final class BestaandEvents {
    private BestaandEvents() {
    }

    static void rechtsklik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        ServerLevel level = p.level();
        BlockPos pos = event.getPos();
        boolean gedaan = event.getHand() == InteractionHand.MAIN_HAND && Tuintje.klik(p, level, pos);
        if (!gedaan) {
            gedaan = Kooien.klik(p, level, pos, event.getItemStack(), event.getHand());
        }
        if (gedaan) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            Schijn.straks(p);   // (the game now sends the real blocks at the click: show this player's own again right after)
        }
    }

    /** Pulling a weed with the left button works too (it looks like a plant, after all). */
    static void linksklik(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !p.isWithinBlockInteractionRange(event.getPos(), 1.0)) {
            return;
        }
        if (Tuintje.klik(p, p.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }
}
