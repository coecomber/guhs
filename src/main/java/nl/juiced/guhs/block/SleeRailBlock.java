package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.block.entity.SleeRailBlockEntity;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.slee.SleePath;

/**
 * The "anchor" of a 4x4 piece of guh sled rail: holds the piece's direction and shape, and its block entity draws the
 * whole piece. The other 15 blocks are {@link SleeRailPartBlock}s. Breaking any of them breaks the whole piece.
 */
public class SleeRailBlock extends BaseEntityBlock {
    public static final MapCodec<SleeRailBlock> CODEC = simpleCodec(SleeRailBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<SleePath.Shape> SHAPE = EnumProperty.create("shape", SleePath.Shape.class);

    public SleeRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, SleePath.Shape.STRAIGHT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // the block entity renderer draws the rails
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SleeRailBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SleeRailPartBlock.shapeFor(state.getValue(SHAPE), state.getValue(FACING), 0);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SleeRailPartBlock.floats(state.getValue(SHAPE)) ? net.minecraft.world.phys.shapes.Shapes.empty() : getShape(state, level, pos, context);
    }

    /** Structures (Guhland) can be placed turned: the piece turns along. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    /** However the anchor goes, its parts go too. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && !level.isClientSide) {
            for (BlockPos part : new SleePath.Placement(pos, state.getValue(FACING), state.getValue(SHAPE)).blocks()) {
                BlockState partState = level.getBlockState(part);
                if (partState.getBlock() instanceof SleeRailPartBlock && SleeRailPartBlock.anchor(partState, part).equals(pos)) {
                    level.setBlock(part, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(itemFor(state.getValue(SHAPE)));
    }

    public static net.minecraft.world.item.Item itemFor(SleePath.Shape shape) {
        return switch (shape) {
            case STRAIGHT -> ModItems.SLEERAIL_RECHT.get();
            case CURVE_LEFT, CURVE_RIGHT -> ModItems.SLEERAIL_BOCHT.get();
            case SLOPE -> ModItems.SLEERAIL_HELLING.get();
            case DROP -> ModItems.SLEERAIL_DROP.get();
            case SPIRAL_LEFT, SPIRAL_RIGHT -> ModItems.SLEERAIL_KURKENTREKKER.get();
            case JUMP -> ModItems.SLEERAIL_SCHANS.get();
        };
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
