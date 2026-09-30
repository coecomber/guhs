package nl.juiced.guhs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.registry.ModBlocks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
/**
 * Invisible filler that makes the big Guh Wheel really take up 3x3 blocks. Remembers where it sits relative to the
 * wheel (the wheel block is the bottom middle), passes clicks on to it, and breaking it breaks the whole wheel.
 */
public class GuhWheelPartBlock extends Block {
    public static final MapCodec<GuhWheelPartBlock> CODEC = simpleCodec(GuhWheelPartBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Sideways position along the wheel: 0 = left, 1 = middle, 2 = right. */
    public static final IntegerProperty SIDE = IntegerProperty.create("side", 0, 2);
    /** Height above the wheel block: 0..2. */
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 2);

    public GuhWheelPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, 1).setValue(HEIGHT, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, HEIGHT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // the wheel's renderer draws everything
    }

    /** The sideways direction along the wheel for a wheel facing this way. */
    public static Direction along(Direction facing) {
        return facing.getClockWise();
    }

    public static BlockPos wheelPos(BlockState state, BlockPos pos) {
        return pos.relative(along(state.getValue(FACING)), 1 - state.getValue(SIDE)).below(state.getValue(HEIGHT));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(wheelPos(state, pos)).is(ModBlocks.GUH_WHEEL.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return canSurvive(state, level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos wheel = wheelPos(state, pos);
        if (level.getBlockState(wheel).is(ModBlocks.GUH_WHEEL.get())) {
            level.destroyBlock(wheel, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** However a part disappears (explosion, piston, command...), the whole wheel goes with it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos wheel = wheelPos(state, pos);
        if (level.getBlockState(wheel).is(ModBlocks.GUH_WHEEL.get())) {
            level.destroyBlock(wheel, true);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        BlockPos wheel = wheelPos(state, pos);
        return level.getBlockState(wheel).useItemOn(stack, level, player, hand, hit.withPosition(wheel));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos wheel = wheelPos(state, pos);
        return level.getBlockState(wheel).useWithoutItem(level, player, hit.withPosition(wheel));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModBlocks.GUH_WHEEL.get());
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
