package nl.juiced.guhs.feature.eilanden;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The floating guh islands can't be broken (like the verstopguh house): no breaking, building, buckets, fire or
 * explosions on the islands and on the wolkenlift square below them (the ordinary ground under the islands is free).
 * You can still open the chest. Players in creative mode may change it.
 */
public final class EilandenProtection {
    /** Template layout (keep in sync with tools/features/eilanden.py): the lifts stand in the middle of the square. */
    public static final int ISLANDS_FROM = 36, SQUARE_RADIUS = 13;

    /** Whether this spot of the islands' building is protected: the sky part, or the lift square on the ground. */
    public static boolean protectedPart(BoundingBox piece, BlockPos pos) {
        if (!piece.isInside(pos)) {
            return false;
        }
        if (pos.getY() >= piece.minY() + ISLANDS_FROM) {
            return true;
        }
        int cx = (piece.minX() + piece.maxX()) / 2, cz = (piece.minZ() + piece.maxZ()) / 2;
        return Math.abs(pos.getX() - cx) <= SQUARE_RADIUS && Math.abs(pos.getZ() - cz) <= SQUARE_RADIUS;
    }

    public static boolean protectedAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        StructureStart start = EilandenFeature.islandsAt(server, pos);
        return start != null && start.getPieces().stream().anyMatch(p -> protectedPart(p.getBoundingBox(), pos));
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.eilanden.no_build").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && protectedAt(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Using an item on a block (buckets, flint and steel, axes, hoes...): not on the islands. Opening the chest is fine. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getItemStack().isEmpty()) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(face))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel) {
            event.getAffectedBlocks().removeIf(pos -> protectedAt(event.getLevel(), pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private EilandenProtection() {
    }
}
