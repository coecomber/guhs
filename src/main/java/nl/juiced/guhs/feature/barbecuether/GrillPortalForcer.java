package nl.juiced.guhs.feature.barbecuether;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Where a barbecue portal leads, the vanilla way (NetherPortalBlock + PortalForcer): the Guhmensie <-> the Barbecuether,
 * coordinates divided / multiplied by 8. An existing barbecue portal near the spot is used (16 blocks around in the
 * Barbecuether, 128 in the Guhmensie); otherwise a new one is built on a good spot nearby, or on a little grillkool
 * platform in a carved-out pocket.
 * <p>
 * bbq2 phase 3 (PHASE3 R14): a portal never comes out inside a story structure or a protected quest building. A story
 * structure can stand right where a portal from near the Guhmensie's spawn arrives (the mine was seen 125 blocks from
 * 0,0), and "a good spot nearby" used to be anywhere within 16 blocks, the carved-out pocket even exactly on the spot: a
 * grillkool frame inside the mine, air cut out of its rock. Now the search starts outside every sluier
 * ({@link nl.juiced.guhs.feature.verhaal.Sluiers#buitenAlleMuren}: known from the world's guaranteed spots, no chunk has
 * to be loaded for it), a spot inside a protected box ({@link nl.juiced.guhs.feature.wereld.Bescherming}) is never taken,
 * a portal that stands inside a sluier is not used as an arrival, and the pocket is not carved into a protected building
 * (then the portal simply does not work from that spot: walk a few blocks and light another).
 */
public final class GrillPortalForcer {
    /** The other side, or null: the barbecue portal only works between the Guhmensie and the Barbecuether. */
    @Nullable
    public static ResourceKey<Level> targetDimension(ResourceKey<Level> from) {
        if (from == ModDimensions.GUHMENSION) {
            return BarbecuetherFeature.BARBECUETHER;
        }
        if (from == BarbecuetherFeature.BARBECUETHER) {
            return ModDimensions.GUHMENSION;
        }
        return null;
    }

    /** Where you come out (before looking for a portal): x and z scaled like the Nether (1 block there = 8 here). */
    public static BlockPos scaledTarget(DimensionType from, DimensionType to, WorldBorder border, double x, double y, double z) {
        double scale = DimensionType.getTeleportationScale(from, to);
        return border.clampToBounds(x * scale, y, z * scale);
    }

    @Nullable
    public static TeleportTransition getDestination(ServerLevel from, Entity entity, BlockPos portalPos) {
        ResourceKey<Level> targetKey = targetDimension(from.dimension());
        ServerLevel target = targetKey == null ? null : from.getServer().getLevel(targetKey);
        if (target == null) {
            return null;
        }
        boolean toBarbecue = targetKey == BarbecuetherFeature.BARBECUETHER;
        WorldBorder border = target.getWorldBorder();
        BlockPos exitPos = scaledTarget(from.dimensionType(), target.dimensionType(), border, entity.getX(), entity.getY(), entity.getZ());

        Optional<BlockPos> existing = findClosestPortalPosition(target, exitPos, toBarbecue, border);
        BlockUtil.FoundRectangle rect;
        TeleportTransition.PostTeleportTransition post;
        if (existing.isPresent()) {
            BlockPos found = existing.get();
            BlockState state = target.getBlockState(found);
            rect = BlockUtil.getLargestRectangleAround(found, state.getValue(GrillPortalBlock.AXIS), 21, Direction.Axis.Y, 21,
                    p -> target.getBlockState(p) == state);
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(found));
        } else {
            Direction.Axis axis = entity.level().getBlockState(portalPos).getOptionalValue(GrillPortalBlock.AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> made = createPortal(target, exitPos, axis);
            if (made.isEmpty()) {
                return null;
            }
            rect = made.get();
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        }
        if (targetKey == nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            post = post.then(nl.juiced.guhs.quest.GuhDex.GIVE_ON_ARRIVAL);     // back in the Guhmensie: a Guhdex if you lost yours
        }
        return transition(entity, portalPos, rect, target, post);
    }

    public static Optional<BlockPos> findClosestPortalPosition(ServerLevel level, BlockPos exitPos, boolean toBarbecue, WorldBorder border) {
        PoiManager poi = level.getPoiManager();
        int radius = toBarbecue ? 16 : 128;
        poi.ensureLoadedAndValid(level, exitPos, radius);
        return poi.getInSquare(type -> type.is(BarbecuetherFeature.PORTAAL_POI.getKey()), exitPos, radius, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(border::isWithinBounds)
                .filter(p -> level.getBlockState(p).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get()))
                .filter(p -> !nl.juiced.guhs.feature.verhaal.Sluiers.bijMuur(level, p, 0))   // (R14: never arrive inside a story structure)
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(exitPos)).thenComparingInt(Vec3i::getY));
    }

    private static TeleportTransition transition(Entity entity, BlockPos pos, BlockUtil.FoundRectangle rect, ServerLevel level,
                                                  TeleportTransition.PostTeleportTransition post) {
        BlockState here = entity.level().getBlockState(pos);
        Direction.Axis axis;
        Vec3 offset;
        if (here.hasProperty(GrillPortalBlock.AXIS)) {
            axis = here.getValue(GrillPortalBlock.AXIS);
            BlockUtil.FoundRectangle from = BlockUtil.getLargestRectangleAround(pos, axis, 21, Direction.Axis.Y, 21,
                    p -> entity.level().getBlockState(p) == here);
            offset = entity.getRelativePortalPosition(axis, from);
        } else {
            axis = Direction.Axis.X;
            offset = new Vec3(0.5, 0.0, 0.0);
        }
        BlockPos corner = rect.minCorner;
        Direction.Axis exitAxis = level.getBlockState(corner).getOptionalValue(GrillPortalBlock.AXIS).orElse(Direction.Axis.X);
        double w = rect.axis1Size, h = rect.axis2Size;
        EntityDimensions dims = entity.getDimensions(entity.getPose());
        int turn = axis == exitAxis ? 0 : 90;
        Vec3 speed = entity.getDeltaMovement();
        Vec3 newSpeed = axis == exitAxis ? speed : new Vec3(speed.z, speed.y, -speed.x);
        double along = dims.width() / 2.0 + (w - dims.width()) * offset.x();
        double up = (h - dims.height()) * offset.y();
        double across = 0.5 + offset.z();
        boolean alongX = exitAxis == Direction.Axis.X;
        Vec3 target = new Vec3(corner.getX() + (alongX ? along : across), corner.getY() + up, corner.getZ() + (alongX ? across : along));
        Vec3 free = PortalShape.findCollisionFreePosition(target, level, entity, dims);
        return new TeleportTransition(level, free, newSpeed, entity.getYRot() + turn, entity.getXRot(), post);
    }

    /** How far (blocks, sideways) a new portal stays away from Guhdalfs sluiers: the search radius of 16, the frame and a little air. */
    public static final int MUUR_MARGE = 22;

    /** May a block of a portal be put here: not in a protected building, not at a sluier? */
    public static boolean magHier(ServerLevel level, BlockPos pos) {
        return !nl.juiced.guhs.feature.wereld.Bescherming.beschermd(level, pos) && !nl.juiced.guhs.feature.verhaal.Sluiers.bijMuur(level, pos, 4);
    }

    /** May a whole frame (4 wide along {@code direction}, 5 high, its lower inner corner at {@code pos}) be built here? */
    private static boolean magFrame(ServerLevel level, BlockPos pos, Direction direction) {
        for (int i = -1; i <= 2; i += 3) {
            for (int j = -1; j <= 3; j += 2) {
                if (!magHier(level, pos.relative(direction, i).above(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Vanilla's PortalForcer.createPortal, with a grillkool frame (and grillkool platform). */
    public static Optional<BlockUtil.FoundRectangle> createPortal(ServerLevel level, BlockPos wens, Direction.Axis axis) {
        // (R14) the search starts outside every story structure
        BlockPos pos = nl.juiced.guhs.feature.verhaal.Sluiers.buitenAlleMuren(level, wens, MUUR_MARGE);
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double best = -1.0;
        BlockPos bestPos = null;
        double fallback = -1.0;
        BlockPos fallbackPos = null;
        WorldBorder border = level.getWorldBorder();
        int top = Math.min(level.getMaxY() + 1, level.getMinY() + level.getLogicalHeight()) - 1;
        BlockPos.MutableBlockPos m = pos.mutable();
        for (BlockPos.MutableBlockPos p : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH)) {
            int k = Math.min(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()));
            if (border.isWithinBounds(p) && border.isWithinBounds(p.move(direction, 1))) {
                p.move(direction.getOpposite(), 1);
                for (int l = k; l >= level.getMinY(); l--) {
                    p.setY(l);
                    if (canReplace(level, p)) {
                        int start = l;
                        while (l > level.getMinY() && canReplace(level, p.move(Direction.DOWN))) {
                            l--;
                        }
                        if (l + 4 <= top) {
                            int depth = start - l;
                            if (depth <= 0 || depth >= 3) {
                                p.setY(l);
                                if (canHostFrame(level, p, m, direction, 0) && magFrame(level, p, direction)) {
                                    double d = pos.distSqr(p);
                                    if (canHostFrame(level, p, m, direction, -1) && canHostFrame(level, p, m, direction, 1)
                                            && (best == -1.0 || best > d)) {
                                        best = d;
                                        bestPos = p.immutable();
                                    }
                                    if (best == -1.0 && (fallback == -1.0 || fallback > d)) {
                                        fallback = d;
                                        fallbackPos = p.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (best == -1.0 && fallback != -1.0) {
            bestPos = fallbackPos;
            best = fallback;
        }
        BlockState frame = BarbecuetherFeature.GRILLKOOL.get().defaultBlockState();
        if (best == -1.0) {
            int low = Math.max(level.getMinY() + 1, 70);
            int high = top - 9;
            if (high < low) {
                return Optional.empty();
            }
            bestPos = border.clampToBounds(new BlockPos(pos.getX() - direction.getStepX(), Mth.clamp(pos.getY(), low, high), pos.getZ() - direction.getStepZ()));
            if (!magFrame(level, bestPos, direction)) {
                return Optional.empty();   // (R14: no pocket is carved into a protected building; the portal does not work from here)
            }
            Direction side = direction.getClockWise();
            for (int i = -1; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = -1; k < 3; k++) {
                        BlockState state = k < 0 ? frame : Blocks.AIR.defaultBlockState();
                        m.setWithOffset(bestPos, j * direction.getStepX() + i * side.getStepX(), k, j * direction.getStepZ() + i * side.getStepZ());
                        level.setBlockAndUpdate(m, state);
                    }
                }
            }
        }
        for (int i = -1; i < 3; i++) {
            for (int j = -1; j < 4; j++) {
                if (i == -1 || i == 2 || j == -1 || j == 3) {
                    m.setWithOffset(bestPos, i * direction.getStepX(), j, i * direction.getStepZ());
                    level.setBlock(m, frame, 3);
                }
            }
        }
        BlockState portal = BarbecuetherFeature.BARBECUETHER_PORTAAL.get().defaultBlockState().setValue(GrillPortalBlock.AXIS, axis);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                m.setWithOffset(bestPos, i * direction.getStepX(), j, i * direction.getStepZ());
                level.setBlock(m, portal, 18);
            }
        }
        return Optional.of(new BlockUtil.FoundRectangle(bestPos.immutable(), 2, 3));
    }

    private static boolean canReplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private static boolean canHostFrame(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos offset, Direction direction, int offsetScale) {
        Direction side = direction.getClockWise();
        for (int i = -1; i < 3; i++) {
            for (int j = -1; j < 4; j++) {
                offset.setWithOffset(origin, direction.getStepX() * i + side.getStepX() * offsetScale, j,
                        direction.getStepZ() * i + side.getStepZ() * offsetScale);
                if (j < 0 && !level.getBlockState(offset).isSolid()) {
                    return false;
                }
                if (j >= 0 && !canReplace(level, offset)) {
                    return false;
                }
            }
        }
        return true;
    }

    private GrillPortalForcer() {
    }
}
