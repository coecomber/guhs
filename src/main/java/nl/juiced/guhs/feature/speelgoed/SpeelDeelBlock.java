package nl.juiced.guhs.feature.speelgoed;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
/**
 * The invisible filler blocks of a glijbaantje, wip or schommel ({@link ToestelBlock}): remembers where the controller
 * is (DX/DZ: -1..1 stored +1, DY: 0..1 down), takes its piece of the toy's shape, passes clicks on and breaking any
 * part breaks the whole toy.
 */
public class SpeelDeelBlock extends Block {
    public static final MapCodec<SpeelDeelBlock> CODEC = simpleCodec(SpeelDeelBlock::new);
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 1);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);

    public SpeelDeelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 1).setValue(DY, 1).setValue(DZ, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    static BlockState voor(BlockState base, BlockPos controller, BlockPos part) {
        return base.setValue(DX, part.getX() - controller.getX() + 1).setValue(DY, part.getY() - controller.getY())
                .setValue(DZ, part.getZ() - controller.getZ() + 1);
    }

    public static BlockPos controller(BlockState state, BlockPos pos) {
        return pos.offset(1 - state.getValue(DX), -state.getValue(DY), 1 - state.getValue(DZ));
    }

    /** This part's shape: its piece of the toy (in local offsets). */
    private VoxelShape vorm(BlockState state, BlockGetter level, BlockPos pos, boolean botsen) {
        BlockPos c = controller(state, pos);
        BlockState cs = level.getBlockState(c);
        if (!(cs.getBlock() instanceof ToestelBlock t)) {
            return botsen ? Shapes.empty() : Shapes.block();
        }
        Direction f = cs.getValue(ToestelBlock.FACING);
        // world offset -> local offset (x right, z back)
        int wx = pos.getX() - c.getX(), wz = pos.getZ() - c.getZ();
        Direction rechts = f.getClockWise(), achter = f.getOpposite();
        int lx = wx * rechts.getStepX() + wz * rechts.getStepZ();
        int lz = wx * achter.getStepX() + wz * achter.getStepZ();
        VoxelShape s = t.vorm(f, lx, pos.getY() - c.getY(), lz, botsen);
        return !botsen && s.isEmpty() ? Block.box(2, 0, 2, 14, 4, 14) : s;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm(state, level, pos, true);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // (the controller's model draws the whole toy)
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(controller(state, pos)).getBlock() instanceof ToestelBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos c = controller(state, pos);
        if (level.getBlockState(c).getBlock() instanceof ToestelBlock) {
            level.destroyBlock(c, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** 1.1.0 (onRemove is gone): a part is gone, so the whole toy goes (only for changes with neighbour updates). */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockPos c = controller(state, pos);
        if (!level.getBlockState(pos).is(this) && level.getBlockState(c).getBlock() instanceof ToestelBlock) {
            level.destroyBlock(c, true);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
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
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockState c = level.getBlockState(controller(state, pos));
        return c.getBlock() instanceof ToestelBlock ? new ItemStack(c.getBlock()) : ItemStack.EMPTY;
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
