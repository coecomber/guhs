package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Vormstaf;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * "Zelf aanwijzen" (design 2.8): after the button in the Bouwopdracht tab, the player's next click with the vormstaf on a
 * block is the job's spot (two clicks for a job between two points: two trees, two chests, a garland). It waits two minutes.
 */
public final class Aanwijzen {
    private record Wacht(ResourceKey<Level> dim, BlockPos bord, String taak, int nodig, List<BlockPos> punten, long tot) {
    }

    private static final Map<UUID, Wacht> WACHT = new HashMap<>();
    private static final int DUUR = 20 * 120;

    static void begin(ServerPlayer p, VerblijfsbordBlockEntity be, String taak) {
        Sjabloon s = Sjablonen.van(taak);
        if (s == null) {
            return;
        }
        WACHT.put(p.getUUID(), new Wacht(p.level().dimension(), be.getBlockPos(), taak, s.punten(), new ArrayList<>(), p.level().getGameTime() + DUUR));
        p.sendSystemMessage(Component.translatable(s.punten() == 2 ? "gui.guhs.bouwopdracht.wijs.twee" : "gui.guhs.bouwopdracht.wijs.een")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Is this player pointing for a job? (The vormstaf's own click is skipped then.) */
    public static boolean wacht(ServerPlayer p) {
        Wacht w = WACHT.get(p.getUUID());
        return w != null && w.tot() >= p.level().getGameTime() && w.dim() == p.level().dimension();
    }

    /** A click with the vormstaf on a block (both sides; the server does the work). */
    public static void klik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof Vormstaf)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (!wacht(p)) {
            WACHT.remove(p.getUUID());
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Wacht w = WACHT.get(p.getUUID());
        w.punten().add(event.getPos().immutable());
        ServerLevel level = p.level();
        level.playSound(null, event.getPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.4f + w.punten().size() * 0.2f);
        if (w.punten().size() < w.nodig()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.wijs.nog"));
            return;
        }
        WACHT.remove(p.getUUID());
        if (!(level.getBlockEntity(w.bord()) instanceof VerblijfsbordBlockEntity be)) {
            return;
        }
        Bouwopdracht.start(p, level, be, w.taak(), w.punten());
    }

    public static void weg(UUID speler) {
        WACHT.remove(speler);
    }

    private Aanwijzen() {
    }
}
