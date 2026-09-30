package nl.juiced.guhs.world;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.GuhPortalBlock;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModPoiTypes;

/**
 * Works out where a guh portal leads: the Guhmension (from anywhere) or the Overworld (from the Guhmension),
 * at the same x/z. Re-uses a guh portal near that spot, otherwise builds a new one on the surface.
 */
public final class GuhPortalForcer {
    private static final int SEARCH_RADIUS = 48;

    @Nullable
    public static TeleportTransition getDestination(ServerLevel from, Entity entity, BlockPos portalPos) {
        ResourceKey<Level> targetKey = from.dimension() == ModDimensions.GUHMENSION ? Level.OVERWORLD : ModDimensions.GUHMENSION;
        ServerLevel target = from.getServer().getLevel(targetKey);
        if (target == null) {
            return null;
        }
        WorldBorder border = target.getWorldBorder();
        BlockPos wanted = border.clampToBounds(entity.getX(), entity.getY(), entity.getZ());

        BlockPos exitPortal = findPortal(target, wanted).orElseGet(() -> buildPortal(target, wanted));
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            nl.juiced.guhs.quest.Reisguh.nearPortal(target, exitPortal);        // a Reisguh by every portal in the Guhmension
        }
        BlockState exitState = target.getBlockState(exitPortal);
        Direction.Axis axis = exitState.getOptionalValue(GuhPortalBlock.AXIS).orElse(Direction.Axis.X);
        BlockUtil.FoundRectangle rect = BlockUtil.getLargestRectangleAround(exitPortal, axis, 21, Direction.Axis.Y, 21,
                p -> target.getBlockState(p) == exitState);

        // stand in the middle of the bottom row of the portal
        BlockPos corner = rect.minCorner;
        double along = rect.axis1Size / 2.0;
        Vec3 pos = axis == Direction.Axis.X
                ? new Vec3(corner.getX() + along, corner.getY(), corner.getZ() + 0.5)
                : new Vec3(corner.getX() + 0.5, corner.getY(), corner.getZ() + along);
        TeleportTransition.PostDimensionTransition post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        if (targetKey == ModDimensions.GUHMENSION) {
            post = post.then(nl.juiced.guhs.quest.GuhDex.GIVE_ON_ARRIVAL);     // a Guhdex for everyone who comes in without one
        }
        return new TeleportTransition(target, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), post);
    }

    private static Optional<BlockPos> findPortal(ServerLevel level, BlockPos near) {
        PoiManager poi = level.getPoiManager();
        poi.ensureLoadedAndValid(level, near, SEARCH_RADIUS);
        return poi.getInSquare(type -> type.is(ModPoiTypes.GUH_PORTAL.getKey()), near, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(level.getWorldBorder()::isWithinBounds)
                .filter(p -> level.getBlockState(p).is(ModBlocks.GUH_PORTAL.get()))
                .min(Comparator.comparingDouble(p -> p.distSqr(near)));
    }

    /**
     * Builds a 4x5 Block-of-Kaasknabbels portal (2x3 inside) standing on a small pink wool platform,
     * on the surface at x/z. Returns one of the portal blocks.
     */
    private static BlockPos buildPortal(ServerLevel level, BlockPos near) {
        // Load/generate the chunk first: for a chunk that isn't loaded yet, level.getHeight() just returns the
        // bottom of the world, which buried the portal deep underground.
        ChunkAccess chunk = level.getChunk(near.getX() >> 4, near.getZ() >> 4);
        int surface = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX() & 15, near.getZ() & 15) + 1;
        int y = Math.max(level.getMinY() + 2, Math.min(surface, level.getMaxY() + 1 - 8));
        BlockPos base = new BlockPos(near.getX(), y, near.getZ());
        BlockState frame = ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState();
        BlockState portal = ModBlocks.GUH_PORTAL.get().defaultBlockState().setValue(GuhPortalBlock.AXIS, Direction.Axis.X);

        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // platform to stand on + room to walk out on both sides
                BlockPos floor = base.offset(dx, -1, dz);
                if (!level.getBlockState(floor).isSolid()) {
                    level.setBlockAndUpdate(floor, Blocks.PINK_WOOL.defaultBlockState());
                }
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        // frame first (placing its last block may already light the portal), then make sure the inside is portal
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                if (dx == -1 || dx == 2 || dy == 0 || dy == 4) {
                    level.setBlockAndUpdate(base.offset(dx, dy, 0), frame);
                }
            }
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(base.offset(dx, dy, 0), portal, 18);
            }
        }
        nl.juiced.guhs.quest.Reisguh.nearPortal(level, base.offset(0, 1, 0));
        return base.offset(0, 1, 0);
    }

    private GuhPortalForcer() {
    }
}
