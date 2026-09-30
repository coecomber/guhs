package nl.juiced.guhs.feature.huisje;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The invisible filler that makes a Guhhuisje really take up its whole guh head (2x2x2, 3x3x3 or 4x4x4). Remembers
 * where the controller ({@link HuisjeBlock}) is (DX/DZ: -3..3 stored +3, DY: 0..3 down), passes clicks on to it, and
 * breaking any part breaks the whole huisje.
 */
public class HuisjeDeelBlock extends Block {
    public static final MapCodec<HuisjeDeelBlock> CODEC = simpleCodec(HuisjeDeelBlock::new);
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 6);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 3);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 6);

    public HuisjeDeelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 3).setValue(DY, 1).setValue(DZ, 3));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    /** The part state for a block at part, belonging to the controller at controller. */
    static BlockState voor(BlockState base, BlockPos controller, BlockPos part) {
        return base.setValue(DX, part.getX() - controller.getX() + 3).setValue(DY, part.getY() - controller.getY())
                .setValue(DZ, part.getZ() - controller.getZ() + 3);
    }

    public static BlockPos controller(BlockState state, BlockPos pos) {
        return pos.offset(3 - state.getValue(DX), -state.getValue(DY), 3 - state.getValue(DZ));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // (the controller's renderer draws the whole guh head)
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(controller(state, pos)).getBlock() instanceof HuisjeBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    /** 3.0: only the owner (or an op) can break a huisje (see HuisjeBlock). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(controller(state, pos)) instanceof HuisjeBlockEntity be && !be.magBreken(player) ? 0f
                : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos c = controller(state, pos);
        if (level.getBlockState(c).getBlock() instanceof HuisjeBlock) {
            level.destroyBlock(c, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        BlockPos c = controller(state, pos);
        if (!newState.is(this) && !level.isClientSide && level.getBlockState(c).getBlock() instanceof HuisjeBlock) {
            level.destroyBlock(c, true);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        BlockPos c = controller(state, pos);
        return level.getBlockState(c).useItemOn(stack, level, player, hand, hit.withPosition(c));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos c = controller(state, pos);
        return level.getBlockState(c).useWithoutItem(level, player, hit.withPosition(c));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState c = level.getBlockState(controller(state, pos));
        return c.getBlock() instanceof HuisjeBlock ? new ItemStack(c.getBlock()) : ItemStack.EMPTY;
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
