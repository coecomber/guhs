package nl.juiced.guhs.feature.guhwaii;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Guhwai'i's game events: the ohana questline notices when a player walks into 626's capsule, the scanner's measurements
 * tick, and Lilo and Nani's stilt house and the capsule can't be broken or built in (only the rommeltjes can be cleaned
 * up; creative players may change everything).
 */
public final class GuhwaiiEvents {
    /** Extra protected boxes, only for the GameTests (the test world has no generated buildings). */
    public static final List<BoundingBox> TEST_GEBIEDEN = new CopyOnWriteArrayList<>();

    private GuhwaiiEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scanner.tick();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 40 != 0) {
            return;
        }
        if (p.serverLevel().dimension() == ModDimensions.GUHMENSION && in(p.serverLevel(), p.blockPosition(), GuhwaiiFeature.CAPSULE)) {
            Ohana.inCapsule(p);
        }
    }

    static boolean in(ServerLevel level, BlockPos pos, ResourceKey<Structure> key) {
        Structure s = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(key);
        return s != null && level.structureManager().getStructureWithPieceAt(pos, s).isValid();
    }

    /** Is this spot part of Lilo and Nani's stilt house or 626's capsule (server side)? */
    public static boolean beschermd(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_GEBIEDEN) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return server.dimension() == ModDimensions.GUHMENSION && (in(server, pos, GuhwaiiFeature.OHANA) || in(server, pos, GuhwaiiFeature.CAPSULE));
    }

    private static boolean mag(Player player, Level level, BlockPos pos) {
        if (player == null || player.getAbilities().instabuild || !beschermd(level, pos)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.guhwaii.beschermd").withStyle(ChatFormatting.AQUA), true);
        return false;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(GuhwaiiFeature.ROMMELTJE.get())) {
            return;   // (626's mess may always be cleaned up)
        }
        if (event.getLevel() instanceof Level level && !mag(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && event.getLevel() instanceof Level level && !mag(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(p -> beschermd(event.getLevel(), p));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().getAbilities().instabuild && event.getPosition().isPresent()
                && !event.getState().is(GuhwaiiFeature.ROMMELTJE.get()) && beschermd(event.getEntity().level(), event.getPosition().get())) {
            event.setNewSpeed(0);
        }
    }
}
