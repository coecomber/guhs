package nl.juiced.guhs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.slee.SleePath;

/**
 * The invisible rest of a sled rail piece. Remembers where its anchor is (offset -3..3 sideways, 0..7 up) and
 * breaks the whole piece when broken. Its shape is a thin plate, or a step on slopes, so you can walk on the rails;
 * the coaster pieces (drop, corkscrew) can only be clicked, not walked on.
 */
public class SleeRailPartBlock extends Block {
    public static final MapCodec<SleeRailPartBlock> CODEC = simpleCodec(SleeRailPartBlock::new);
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 6);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 7);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 6);

    private static final VoxelShape PLATE = Block.box(0, 0, 0, 16, 3, 16);

    public SleeRailPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 3).setValue(DY, 0).setValue(DZ, 3));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    /** The part state for a block at `offset` from its anchor. */
    public BlockState forOffset(BlockPos offset) {
        return defaultBlockState().setValue(DX, offset.getX() + 3).setValue(DY, offset.getY()).setValue(DZ, offset.getZ() + 3);
    }

    public static BlockPos anchor(BlockState state, BlockPos pos) {
        return pos.offset(3 - state.getValue(DX), -state.getValue(DY), 3 - state.getValue(DZ));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState anchor = level.getBlockState(anchor(state, pos));
        if (anchor.getBlock() instanceof SleeRailBlock) {
            return shapeFor(anchor.getValue(SleeRailBlock.SHAPE), anchor.getValue(SleeRailBlock.FACING), state.getValue(DY));
        }
        return PLATE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState anchor = level.getBlockState(anchor(state, pos));
        if (anchor.getBlock() instanceof SleeRailBlock && floats(anchor.getValue(SleeRailBlock.SHAPE))) {
            return Shapes.empty();
        }
        return getShape(state, level, pos, context);
    }

    /** The drop and the corkscrews wind through the air: their blocks are only there to click. */
    public static boolean floats(SleePath.Shape shape) {
        return shape == SleePath.Shape.DROP || shape == SleePath.Shape.SPIRAL_LEFT || shape == SleePath.Shape.SPIRAL_RIGHT;
    }

    /** A plate for flat pieces; a step (going up towards `facing`) for slopes. */
    public static VoxelShape shapeFor(SleePath.Shape shape, Direction facing, int dy) {
        if (shape != SleePath.Shape.SLOPE) {
            return PLATE;
        }
        VoxelShape upper = switch (facing) {
            case SOUTH -> Block.box(0, 0, 8, 16, 16, 16);
            case EAST -> Block.box(8, 0, 0, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 8, 16, 16);
            default -> Block.box(0, 0, 0, 16, 16, 8);
        };
        return Shapes.or(Block.box(0, 0, 0, 16, 8, 16), upper);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(anchor(state, pos)).getBlock() instanceof SleeRailBlock;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos anchor = anchor(state, pos);
        if (level.getBlockState(anchor).getBlock() instanceof SleeRailBlock) {
            level.destroyBlock(anchor, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos anchor = anchor(state, pos);
        if (level.getBlockState(anchor).getBlock() instanceof SleeRailBlock) {
            level.destroyBlock(anchor, true);
        }
    }

    /** Structures can be placed turned: the offset to the anchor turns along. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockPos offset = new BlockPos(state.getValue(DX) - 3, state.getValue(DY), state.getValue(DZ) - 3).rotate(rotation);
        return forOffset(offset);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockState anchor = level.getBlockState(anchor(state, pos));
        return anchor.getBlock() instanceof SleeRailBlock ? new ItemStack(SleeRailBlock.itemFor(anchor.getValue(SleeRailBlock.SHAPE))) : ItemStack.EMPTY;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
