package nl.juiced.guhs.feature.bio.blokkendal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.Guhs;

/**
 * Guh-bamboe: grows like bamboo (a stalk that gets a block taller now and then, up to a height of its own between
 * {@link #MIN_HOOGTE} and {@link #MAX_HOOGTE}; bonemeal helps; cut the foot and the rest falls), with its own look.
 * The leaves follow by themselves: the top block carries the big tuft, the one under it a small one.
 * <p>For worldgen (which does not run the block's own logic): a stalk of height n is n blocks, from the top
 * {@code leaves=large, small, none, none...} (height 2: {@code small, none}; height 1: {@code small}), all {@code stage=0}.
 */
public class GuhBamboeBlock extends Block implements BonemealableBlock {
    public static final MapCodec<GuhBamboeBlock> CODEC = simpleCodec(GuhBamboeBlock::new);
    public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
    /** 0: still growing, 1: full-grown. */
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    public static final int MIN_HOOGTE = 5, MAX_HOOGTE = 10;
    /** What guh-bamboe stands on besides vanilla's bamboo ground (the Guhmensie's wool and grass). */
    public static final TagKey<Block> GROND = TagKey.create(Registries.BLOCK, Guhs.id("guh_bamboe_grond"));

    /** Game tests only (their box is four blocks high): the height every stalk grows to; 0 = each its own. */
    public static volatile int proefHoogte;

    private static final VoxelShape STENGEL = Block.column(6, 0, 16), MET_BLAD = Block.column(10, 0, 16), BOTSING = Block.column(3, 0, 16);

    public GuhBamboeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEAVES, BambooLeaves.SMALL).setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEAVES, STAGE);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(LEAVES) == BambooLeaves.LARGE ? MET_BLAD : STENGEL).move(state.getOffset(pos));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOTSING.move(state.getOffset(pos));
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState onder = level.getBlockState(pos.below());
        return onder.is(this) || onder.is(BlockTags.SUPPORTS_BAMBOO) || onder.is(GROND);
    }

    /** The leaves a stalk block carries: seen from what stands on it and how much stalk is under it. */
    private BambooLeaves blad(BlockGetter level, BlockPos pos) {
        BlockState boven = level.getBlockState(pos.above());
        if (!boven.is(this)) {
            return level.getBlockState(pos.below()).is(this) && level.getBlockState(pos.below(2)).is(this) ? BambooLeaves.LARGE : BambooLeaves.SMALL;
        }
        return boven.getValue(LEAVES) == BambooLeaves.LARGE ? BambooLeaves.SMALL : BambooLeaves.NONE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getFluidState(pos).isEmpty() || !canSurvive(defaultBlockState(), level, pos)) {
            return null;
        }
        return defaultBlockState().setValue(LEAVES, blad(level, pos));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighbor, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        if (direction.getAxis() == Direction.Axis.Y) {
            BambooLeaves blad = blad(level, pos);
            boolean top = !level.getBlockState(pos.above()).is(this);
            if (blad != state.getValue(LEAVES)) {
                // (a stalk that became the top again, because it was cut above, grows on)
                return state.setValue(LEAVES, blad).setValue(STAGE, top && direction == Direction.UP ? 0 : state.getValue(STAGE));
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(STAGE) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) == 0 && level.isEmptyBlock(pos.above()) && level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(3) == 0) {
            groei(level, pos);
        }
    }

    private int onder(BlockGetter level, BlockPos pos) {
        int n = 0;
        while (n < MAX_HOOGTE && level.getBlockState(pos.below(n + 1)).is(this)) {
            n++;
        }
        return n;
    }

    private int boven(BlockGetter level, BlockPos pos) {
        int n = 0;
        while (n < MAX_HOOGTE && level.getBlockState(pos.above(n + 1)).is(this)) {
            n++;
        }
        return n;
    }

    /** How tall the stalk that stands on this foot wants to be (every stalk its own height). */
    public static int volgroeid(BlockPos voet) {
        if (proefHoogte > 0) {
            return proefHoogte;
        }
        return MIN_HOOGTE + Math.floorMod(Mth.getSeed(voet.getX(), voet.getY(), voet.getZ()), MAX_HOOGTE - MIN_HOOGTE + 1);
    }

    /** One block taller, if the top (this block) is not full-grown; true when it grew. */
    public boolean groei(ServerLevel level, BlockPos top) {
        BlockState state = level.getBlockState(top);
        if (!state.is(this) || state.getValue(STAGE) != 0 || !level.isEmptyBlock(top.above()) || level.isOutsideBuildHeight(top.above())) {
            return false;
        }
        int hoogte = onder(level, top) + 1;
        int doel = volgroeid(top.below(hoogte - 1));
        if (hoogte >= doel) {
            level.setBlock(top, state.setValue(STAGE, 1), Block.UPDATE_CLIENTS);
            return false;
        }
        BlockState nieuw = defaultBlockState().setValue(LEAVES, hoogte >= 2 ? BambooLeaves.LARGE : BambooLeaves.SMALL).setValue(STAGE, hoogte + 1 >= doel ? 1 : 0);
        level.setBlock(top.above(), nieuw, Block.UPDATE_ALL);    // (the blocks under it take their new leaves in updateShape)
        return true;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos top = pos.above(boven(level, pos));
        return level.getBlockState(top).getValue(STAGE) == 0 && level.isEmptyBlock(top.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int keer = 1 + random.nextInt(2);
        for (int i = 0; i < keer; i++) {
            if (!groei(level, pos.above(boven(level, pos)))) {
                return;
            }
        }
    }
}
