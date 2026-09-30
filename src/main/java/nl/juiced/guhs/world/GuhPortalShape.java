package nl.juiced.guhs.world;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.GuhPortalBlock;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Finds a rectangular Block-of-Kaasknabbels frame (like a nether portal frame: inside 2x3 up to 21x21, corners optional)
 * and fills it with guh portal blocks. Adapted from vanilla's PortalShape.
 */
public class GuhPortalShape {
    public static final int MIN_WIDTH = 2;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_SIZE = 21;

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    @Nullable
    private BlockPos bottomLeft;
    private int width;
    private int height;
    private int numPortalBlocks;

    /** Tries both axes; returns a valid, still empty frame containing {@code insidePos}. */
    public static Optional<GuhPortalShape> findEmptyShape(LevelAccessor level, BlockPos insidePos) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            GuhPortalShape shape = new GuhPortalShape(level, insidePos, axis);
            if (shape.isValid() && shape.numPortalBlocks == 0) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    public GuhPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = calculateBottomLeft(pos);
        if (this.bottomLeft != null) {
            this.width = calculateWidth();
            if (this.width > 0) {
                this.height = calculateHeight();
            }
        }
    }

    public static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.BLOCK_OF_KAASKNABBELS.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.GUH_PORTAL.get());
    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - MAX_SIZE);
        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction left = rightDir.getOpposite();
        int dist = distanceUntilEdgeAboveFrame(pos, left) - 1;
        return dist < 0 ? null : pos.relative(left, dist);
    }

    private int calculateWidth() {
        int w = distanceUntilEdgeAboveFrame(bottomLeft, rightDir);
        return w >= MIN_WIDTH && w <= MAX_SIZE ? w : 0;
    }

    /** Walks along the bottom row: every step must be empty with frame underneath, ending at a frame block. */
    private int distanceUntilEdgeAboveFrame(BlockPos start, Direction dir) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= MAX_SIZE; i++) {
            p.set(start).move(dir, i);
            BlockState state = level.getBlockState(p);
            if (!isEmpty(state)) {
                return isFrame(state) ? i : 0;
            }
            if (!isFrame(level.getBlockState(p.move(Direction.DOWN)))) {
                break;
            }
        }
        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int h = distanceUntilTop(p);
        if (h < MIN_HEIGHT || h > MAX_SIZE) {
            return 0;
        }
        for (int i = 0; i < width; i++) {
            if (!isFrame(level.getBlockState(p.set(bottomLeft).move(Direction.UP, h).move(rightDir, i)))) {
                return 0;
            }
        }
        return h;
    }

    private int distanceUntilTop(BlockPos.MutableBlockPos p) {
        for (int y = 0; y < MAX_SIZE; y++) {
            if (!isFrame(level.getBlockState(p.set(bottomLeft).move(Direction.UP, y).move(rightDir, -1)))
                    || !isFrame(level.getBlockState(p.set(bottomLeft).move(Direction.UP, y).move(rightDir, width)))) {
                return y;
            }
            for (int x = 0; x < width; x++) {
                BlockState state = level.getBlockState(p.set(bottomLeft).move(Direction.UP, y).move(rightDir, x));
                if (!isEmpty(state)) {
                    return y;
                }
                if (state.is(ModBlocks.GUH_PORTAL.get())) {
                    numPortalBlocks++;
                }
            }
        }
        return MAX_SIZE;
    }

    public boolean isValid() {
        return bottomLeft != null && width >= MIN_WIDTH && width <= MAX_SIZE && height >= MIN_HEIGHT && height <= MAX_SIZE;
    }

    /** Valid frame that is completely filled with portal blocks (used to decide if the portal should break). */
    public boolean isComplete() {
        return isValid() && numPortalBlocks == width * height;
    }

    public void createPortalBlocks() {
        BlockState portal = ModBlocks.GUH_PORTAL.get().defaultBlockState().setValue(GuhPortalBlock.AXIS, axis);
        BlockPos.betweenClosed(bottomLeft, bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1))
                .forEach(p -> level.setBlock(p, portal, 18));
    }
}
