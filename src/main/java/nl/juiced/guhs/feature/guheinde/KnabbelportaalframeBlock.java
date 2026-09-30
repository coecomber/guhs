package nl.juiced.guhs.feature.guheinde;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A knabbelportaalframe (the end portal frame of the Knabbelkelder). Twelve of them, three on each side of a 3x3 square,
 * each with an Oog van Vadsig in it, open the portal to the Guheinde ({@link #tryOpenPortal}).
 */
public class KnabbelportaalframeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KnabbelportaalframeBlock> CODEC = simpleCodec(KnabbelportaalframeBlock::new);
    public static final BooleanProperty OOG = BooleanProperty.create("oog");
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 13, 16);
    private static final VoxelShape WITH_OOG = Shapes.or(BASE, Block.box(4, 13, 4, 12, 16, 12));

    public KnabbelportaalframeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OOG, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OOG);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OOG) ? WITH_OOG : BASE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(OOG) ? 15 : 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private static boolean filled(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof KnabbelportaalframeBlock && state.getValue(OOG);
    }

    /**
     * The middle of the 3x3 portal whose ring of twelve frames is complete and includes this frame, or null. The ring:
     * three frames on each side, two blocks from the middle, no corners.
     */
    @Nullable
    public static BlockPos completeRingCenter(Level level, BlockPos frame) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos c = frame.offset(dx, 0, dz);
                if (inRing(c, frame) && ringComplete(level, c)) {
                    return c;
                }
            }
        }
        return null;
    }

    /** Is this frame one of the twelve of the ring around middle c? */
    public static boolean inRing(BlockPos c, BlockPos frame) {
        int dx = frame.getX() - c.getX(), dz = frame.getZ() - c.getZ();
        return frame.getY() == c.getY() && ((Math.abs(dx) == 2 && Math.abs(dz) <= 1) || (Math.abs(dz) == 2 && Math.abs(dx) <= 1));
    }

    public static boolean ringComplete(Level level, BlockPos c) {
        for (int i = -1; i <= 1; i++) {
            if (!filled(level, c.offset(i, 0, -2)) || !filled(level, c.offset(i, 0, 2)) || !filled(level, c.offset(-2, 0, i))
                    || !filled(level, c.offset(2, 0, i))) {
                return false;
            }
        }
        return true;
    }

    /** After an eye went in: if the ring is now complete, fill the 3x3 with portal. Returns whether it opened. */
    public static boolean tryOpenPortal(Level level, BlockPos frame) {
        BlockPos c = completeRingCenter(level, frame);
        if (c == null) {
            return false;
        }
        BlockState portal = GuheindeFeature.GUHEINDE_PORTAAL.get().defaultBlockState();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                level.setBlock(c.offset(i, 0, j), portal, 2);
            }
        }
        level.playSound(null, c, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1f, 1.3f);
        return true;
    }
}
