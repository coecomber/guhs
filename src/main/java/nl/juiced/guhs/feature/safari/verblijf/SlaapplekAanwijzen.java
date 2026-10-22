package nl.juiced.guhs.feature.safari.verblijf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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

/**
 * Test round 5: "Slaapplek aanwijzen" in the board menu. The menu closes, and the next right-click on a block inside the
 * home (within two minutes, any hand item) makes that block's top the family's sleeping spot, with a puff of pink. A click
 * outside the home says so and keeps waiting; a click on the board itself stops pointing.
 */
public final class SlaapplekAanwijzen {
    private record Wacht(ResourceKey<Level> dim, BlockPos bord, long tot) {
    }

    private static final Map<UUID, Wacht> WACHT = new HashMap<>();
    static final int DUUR = 20 * 120;
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF7AC8, 1.4f);

    static void begin(ServerPlayer p, VerblijfsbordBlockEntity be) {
        WACHT.put(p.getUUID(), new Wacht(p.level().dimension(), be.getBlockPos(), p.level().getGameTime() + DUUR));
        p.sendSystemMessage(Component.translatable("gui.guhs.verblijf.thuisplek.wijs.begin").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Is this player pointing at a sleeping spot? */
    public static boolean wacht(ServerPlayer p) {
        Wacht w = WACHT.get(p.getUUID());
        return w != null && w.tot() >= p.level().getGameTime() && w.dim() == p.level().dimension();
    }

    static void klik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (!wacht(p)) {
            WACHT.remove(p.getUUID());
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Wacht w = WACHT.get(p.getUUID());
        ServerLevel level = p.level();
        BlockPos geklikt = event.getPos();
        if (geklikt.equals(w.bord())) {
            WACHT.remove(p.getUUID());
            p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.thuisplek.wijs.gestopt"));
            return;
        }
        if (!(level.getBlockEntity(w.bord()) instanceof VerblijfsbordBlockEntity be)) {
            WACHT.remove(p.getUUID());
            return;
        }
        BlockPos plek = geklikt.above();
        if (!be.binnen(plek.getX(), plek.getZ()) || Math.abs(plek.getY() - w.bord().getY()) > 8) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.thuisplek.te_ver").withStyle(ChatFormatting.RED));
            return;
        }
        WACHT.remove(p.getUUID());
        be.zetSlaapplek(plek);
        level.sendParticles(ROZE, plek.getX() + 0.5, plek.getY() + 0.3, plek.getZ() + 0.5, 30, 0.6, 0.3, 0.6, 0);
        level.playSound(null, plek, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.thuisplek.gezet", plek.toShortString()).withStyle(ChatFormatting.GREEN));
    }

    static void weg(UUID speler) {
        WACHT.remove(speler);
    }

    private SlaapplekAanwijzen() {
    }
}
