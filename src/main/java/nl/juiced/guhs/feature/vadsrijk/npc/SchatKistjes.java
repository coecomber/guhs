package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * 1.10.15: a real little chest at every secret treasure ("??? Ergens in ...") of zones 1-6. The treasures were only a spot you had
 * to stand on (glitter within 14 blocks) while every hint says "er staat een kist": a player stood at the coordinates, saw no chest
 * and walked away. Now, when a player comes within {@link #STRAAL} blocks, an empty spot at the treasure gets a chest (it comes back
 * if it is ever gone). It is decoration: it cannot be opened (no storage in the Vadsrijk) or broken; a click says how it works. The
 * treasure itself is still found by standing next to it (the zones' VIND_STRAAL, 4 blocks since 1.10.15), once per player.
 */
public final class SchatKistjes {
    /** How close a player must come before the chest is (re)placed (its chunk is loaded then). */
    public static final int STRAAL = 32;
    private static final String L = "vadsrijk.npc.schatkistje.";

    private SchatKistjes() {
    }

    /** Every secret-treasure map of the Vadsrijk (zone 1 .. 6). */
    public static List<Map<String, BlockPos>> bronnen() {
        return List.of(NpcPlekken.SCHAT, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken.SCHAT,
                nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.SCHAT, nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4VerhaalPlekken.SCHAT,
                nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalPlekken.SCHAT, nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalPlekken.SCHAT);
    }

    /** Is this block position one of the treasure spots? */
    public static boolean isSchat(BlockPos p) {
        for (Map<String, BlockPos> m : bronnen()) {
            if (m.containsValue(p)) {
                return true;
            }
        }
        return false;
    }

    static void registreer() {
        NeoForge.EVENT_BUS.addListener(SchatKistjes::klik);
        NeoForge.EVENT_BUS.addListener(SchatKistjes::breek);
    }

    /** Every 2 s: the treasure spots near players get their chest. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 13) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            for (Map<String, BlockPos> m : bronnen()) {
                for (BlockPos b : m.values()) {
                    if (b == null) {
                        continue;
                    }
                    double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5);
                    if (dx * dx + dz * dz <= STRAAL * STRAAL && Math.abs(p.getY() - b.getY()) <= STRAAL) {
                        zet(sl, b);
                    }
                }
            }
        }
    }

    /** Puts the chest at {@code b} when that spot is empty (air or a plant, no fluid). Returns true when it stands there now. Public for the gametests. */
    public static boolean zet(ServerLevel sl, BlockPos b) {
        if (!sl.isLoaded(b)) {
            return false;
        }
        BlockState s = sl.getBlockState(b);
        if (s.getBlock() instanceof ChestBlock) {
            return true;
        }
        if (!(s.isAir() || s.canBeReplaced()) || !s.getFluidState().isEmpty() || s.hasBlockEntity()) {
            return false;   // (something else stands there, or water: leave it alone)
        }
        sl.setBlock(b, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, kant(sl, b)), 3);
        return true;
    }

    /** The chest faces the most open side (so its front is seen, not pressed against a wall). */
    private static Direction kant(ServerLevel sl, BlockPos b) {
        for (Direction d : new Direction[] {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST}) {
            if (sl.getBlockState(b.relative(d)).isAir()) {
                return d;
            }
        }
        return Direction.SOUTH;
    }

    private static void klik(PlayerInteractEvent.RightClickBlock e) {
        if (!Vadsrijk.in(e.getEntity()) || !(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof ChestBlock) || !isSchat(e.getPos())) {
            return;
        }
        e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable(L + "klik").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void breek(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && Vadsrijk.in(sl) && !e.getPlayer().isCreative() && e.getState().getBlock() instanceof ChestBlock
                && isSchat(e.getPos())) {
            e.setCanceled(true);
        }
    }
}
