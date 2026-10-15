package nl.juiced.guhs.feature.vadskracht;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;

/**
 * A part of a machine bigger than one block ({@link Meerblok}): an invisible filler (the kern's model or renderer draws the
 * whole machine) that remembers where its kern is, has no item, passes clicks on to the kern, and takes the whole machine
 * with it when it is broken. Its vadskracht knoop and its item and fluid capabilities are the kern's
 * ({@link VadskrachtFeature} registers that for {@link VadskrachtFeature#MACHINE_DEEL}; a subclass with its own id registers
 * the same with {@link VadskrachtFeature#deelCapabilities}).
 * <p>
 * {@code guhs:machine_deel} is the ready-made one every machine may use.
 */
public class MachineDeelBlock extends Block {
    public static final MapCodec<MachineDeelBlock> CODEC = simpleCodec(MachineDeelBlock::new);
    /** Where this part sits relative to its kern: dx + 2, dy, dz + 2. */
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 4);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 4);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 4);

    public MachineDeelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 2).setValue(DY, 1).setValue(DZ, 2));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    /** The state of a part at this offset from its kern. */
    public static BlockState staat(Block deel, Vec3i vanKern) {
        return deel.defaultBlockState().setValue(DX, vanKern.getX() + 2).setValue(DY, vanKern.getY()).setValue(DZ, vanKern.getZ() + 2);
    }

    public static BlockPos kern(BlockState state, BlockPos pos) {
        return pos.offset(2 - state.getValue(DX), -state.getValue(DY), 2 - state.getValue(DZ));
    }

    /** Is the kern of this part still there (a block that is the kern of a machine with a part on this spot)? */
    protected boolean heeftKern(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos kern = kern(state, pos);
        BlockState kernState = level.getBlockState(kern);
        return kernState.getBlock() instanceof Meerblok.Vorm && Meerblok.delen(kern, kernState).contains(pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // the kern draws everything
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return heeftKern(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        return heeftKern(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos kern = kern(state, pos);
        if (heeftKern(state, level, pos)) {
            level.destroyBlock(kern, !player.getAbilities().instabuild, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** However a part disappears (explosion, piston, command...), the whole machine goes with it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
        BlockPos kern = kern(state, pos);
        BlockState kernState = level.getBlockState(kern);
        if (kernState.getBlock() instanceof Meerblok.Vorm && Meerblok.delen(kern, kernState).contains(pos)) {
            level.destroyBlock(kern, true);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        BlockPos kern = kern(state, pos);
        return heeftKern(state, level, pos) ? level.getBlockState(kern).useItemOn(stack, level, player, hand, hit.withPosition(kern))
                : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos kern = kern(state, pos);
        return heeftKern(state, level, pos) ? level.getBlockState(kern).useWithoutItem(level, player, hit.withPosition(kern)) : InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockPos kern = kern(state, pos);
        return heeftKern(state, level, pos) ? level.getBlockState(kern).getCloneItemStack(level, kern, includeData) : ItemStack.EMPTY;
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
