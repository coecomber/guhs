package nl.juiced.guhs.feature.bio.blokkenwolk;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Drijvende bloesemblaadjes: blossom petals floating on the water. The block sits in the air above still water, like a lily
 * pad, but it is only a film of petals: nothing collides with it, so boats, swimmers, fish and guhs pass through and it is
 * never knocked loose. It goes (and drops its petals) when the water under it goes.
 * <p>
 * {@link #DICHTHEID} 1 to 3: a few loose petals, a drift, a thick patch; using more petals on a patch thickens it. Each
 * density has several pictures in several turns, chosen by the spot (the blockstate file), so a streak of even one density
 * never looks tiled; worldgen that only knows the default state (1, the loosest) gets a loose streak by itself.
 */
public class BloesemblaadjesBlock extends Block {
    public static final MapCodec<BloesemblaadjesBlock> CODEC = simpleCodec(BloesemblaadjesBlock::new);
    public static final int MAX = 3;
    public static final IntegerProperty DICHTHEID = IntegerProperty.create("dichtheid", 1, MAX);
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 1, 16);

    public BloesemblaadjesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DICHTHEID, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DICHTHEID);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    /** Petals float on still water (a water source, also a waterlogged block that is full to the brim), with no water above it. */
    public static boolean drijft(LevelReader level, BlockPos pos) {
        FluidState onder = level.getFluidState(pos.below());
        return onder.is(FluidTags.WATER) && onder.isSource() && level.getFluidState(pos).isEmpty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return drijft(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    /** More petals on a patch that is not full yet: it gets thicker instead of becoming a second block. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(DICHTHEID) < MAX
                || super.canBeReplaced(state, context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState er = context.getLevel().getBlockState(context.getClickedPos());
        return er.is(this) ? er.setValue(DICHTHEID, Math.min(MAX, er.getValue(DICHTHEID) + 1)) : defaultBlockState();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    /**
     * The item: placed on the water you look at (as the lily pad item does), or on a patch of petals you look at, to
     * thicken it.
     */
    public static class Voorwerp extends BlockItem {
        public Voorwerp(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            BlockHitResult raak = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (raak.getType() != HitResult.Type.BLOCK) {
                return InteractionResult.PASS;
            }
            return plaats(level, player, hand, raak);
        }

        /** Places petals for a hit on water (above it) or on petals (on them). */
        public InteractionResult plaats(Level level, Player player, InteractionHand hand, BlockHitResult raak) {
            BlockHitResult doel = level.getBlockState(raak.getBlockPos()).is(getBlock()) ? raak : raak.withPosition(raak.getBlockPos().above());
            return place(new BlockPlaceContext(new UseOnContext(player, hand, doel)));
        }
    }
}
