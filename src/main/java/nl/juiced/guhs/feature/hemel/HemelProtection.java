package nl.juiced.guhs.feature.hemel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.world.ModDimensions;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The Hemelkapelletje can't be broken or built in (like the floating guh islands): the whole sky part (the islet, the
 * chapel, the clouds, the lift columns) and the cloud plaza on the ground. Doors, the hemelkist, benches and the Knuffelhart
 * itself still work. Creative players may change it. The Knuffelhart can't be broken anywhere ({@link HemelEvents}).
 */
public final class HemelProtection {
    /** Template layout (keep in sync with tools/features/hemel_bouw.py: SKY_FROM, PLAZA_R, PLAZA, G). */
    public static final int SKY_FROM = 24, PLAZA_RADIUS = 11, PLAZA_X = 20, PLAZA_Z = 31, GROND = 4;
    /** Extra protected boxes, only for the GameTests (the test world has no generated chapel). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();

    /** Is this spot of the chapel's piece protected: the sky part, or the plaza (on and above its floor)? */
    public static boolean protectedPart(StructurePiece piece, BlockPos pos) {
        BoundingBox box = piece.getBoundingBox();
        if (!box.isInside(pos)) {
            return false;
        }
        if (pos.getY() >= box.minY() + SKY_FROM) {
            return true;
        }
        if (pos.getY() < box.minY() + GROND - 1) {
            return false;
        }
        BlockPos plein = plein(piece);
        int dx = pos.getX() - plein.getX(), dz = pos.getZ() - plein.getZ();
        return dx * dx + dz * dz <= PLAZA_RADIUS * PLAZA_RADIUS;
    }

    /** The world position of the plaza's middle (the template is placed rotated). */
    static BlockPos plein(StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece p) {
            BlockPos rel = StructureTemplate.transform(new BlockPos(PLAZA_X, GROND, PLAZA_Z), Mirror.NONE, p.getRotation(), BlockPos.ZERO);
            return p.getPosition().offset(rel);
        }
        BoundingBox b = piece.getBoundingBox();
        return new BlockPos((b.minX() + b.maxX()) / 2, b.minY() + GROND, (b.minZ() + b.maxZ()) / 2);
    }

    public static boolean protectedAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        StructureStart start = kapelletjeAt(server, pos);
        return start != null && start.getPieces().stream().anyMatch(p -> protectedPart(p, pos));
    }

    /** The chapel with a piece at this spot (Guhmension only), or null. */
    public static StructureStart kapelletjeAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return null;
        }
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(HemelFeature.KAPELLETJE);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        return start.isValid() ? start : null;
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.hemel.no_build").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
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

    /** Using an item on a block (buckets, flint and steel, axes, hoes...): not here. Opening the chest, sitting, the heart: fine. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty() || event.getItemStack().is(HemelFeature.HERINNERING.get())) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos()) || denied(event.getEntity(), event.getPos().relative(face))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel) {
            event.getAffectedBlocks().removeIf(pos -> protectedAt(event.getLevel(), pos)
                    || event.getLevel().getBlockState(pos).is(HemelFeature.KNUFFELHART.get()));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private HemelProtection() {
    }
}
