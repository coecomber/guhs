package nl.juiced.guhs.feature.fabriek;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * (1.8.0) Linking a Bouwblokken-fabriek to a chest: sneak + empty hand on the fabriek, then click the chest (within
 * {@link FabriekGetallen#KOPPEL_BEREIK} blocks, {@link FabriekGetallen#KOPPEL_TIJD} ticks to do it). Sneak-clicking the
 * fabriek a second time instead forgets its links (bank and chest). Who is linking what lives only in memory.
 */
public final class Koppelen {
    private record Wacht(GlobalPos fabriek, long tot) {
    }

    private static final Map<UUID, Wacht> WACHT = new ConcurrentHashMap<>();

    private Koppelen() {
    }

    public static void begin(ServerPlayer speler, BouwblokkenFabriekBlockEntity fabriek) {
        ServerLevel level = speler.level();
        GlobalPos hier = GlobalPos.of(level.dimension(), fabriek.getBlockPos());
        Wacht w = WACHT.remove(speler.getUUID());
        if (w != null && w.fabriek().equals(hier) && w.tot() >= level.getGameTime()) {
            fabriek.koppelBank(null);
            fabriek.koppelKist(null);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.los"));
            level.playSound(null, fabriek.getBlockPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.6f, 1.4f);
            return;
        }
        WACHT.put(speler.getUUID(), new Wacht(hier, level.getGameTime() + FabriekGetallen.KOPPEL_TIJD));
        speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.klik_kist", FabriekGetallen.KOPPEL_BEREIK));
        level.playSound(null, fabriek.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
    }

    /** The player clicks a block: when they are linking, a chest becomes the fabriek's chest. */
    static void klik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer speler) || !(speler.level() instanceof ServerLevel level)) {
            return;
        }
        Wacht w = WACHT.get(speler.getUUID());
        if (w == null) {
            return;
        }
        BlockPos pos = event.getPos();
        if (w.tot() < level.getGameTime() || !w.fabriek().dimension().equals(level.dimension())) {
            WACHT.remove(speler.getUUID());
            return;
        }
        if (pos.equals(w.fabriek().pos())) {
            return;   // (the fabriek itself: begin() decides)
        }
        WACHT.remove(speler.getUUID());
        if (!(level.getBlockEntity(w.fabriek().pos()) instanceof BouwblokkenFabriekBlockEntity fabriek)) {
            return;
        }
        if (Kisten.van(level, pos, null) == null || level.getBlockEntity(pos) instanceof MachineBlockEntity) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.geen_kist"));
        } else if (!pos.closerThan(fabriek.getBlockPos(), FabriekGetallen.KOPPEL_BEREIK)) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.te_ver", FabriekGetallen.KOPPEL_BEREIK));
        } else {
            fabriek.koppelKist(pos);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.koppel.kist", pos.getX(), pos.getY(), pos.getZ()));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
