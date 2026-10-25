package nl.juiced.guhs.feature.safari.rit;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The safaripoort (round 3, J2): a gate of 4 x 6 blocks across the truck road where it goes from one zone into another
 * (the park generator puts one at every crossing). Every part is this block (deel_x 0..3 along {@link #AS}, deel_y
 * 0..5); the corner part (0, 0) has the {@link SafariPoortBlockEntity} that draws the whole gate (GeckoLib: two thick
 * wooden doors, the lion-guh shield, pink rope and lamps) and runs it: a click opens or closes it, a truck opens it in
 * time and it closes again behind the truck and the riders' own guhs, and a gate somebody left open shuts itself after
 * {@link SafariPoortBlockEntity#OPEN_MAX} ticks.
 * <p>
 * The hard rule: an open gate only lets players, guhs and the rides through. For anything else (the park animals, a
 * race ostrich, also with a player on its back) the closed doors stay a wall, so no animal ever walks into another zone.
 */
public class SafariPoortBlock extends BaseEntityBlock {
    public static final MapCodec<SafariPoortBlock> CODEC = simpleCodec(SafariPoortBlock::new);
    /** The axis the gate lies along (its four parts); the road goes through it the other way. */
    public static final EnumProperty<Direction.Axis> AS = EnumProperty.create("as", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    public static final IntegerProperty DEEL_X = IntegerProperty.create("deel_x", 0, 3);
    public static final IntegerProperty DEEL_Y = IntegerProperty.create("deel_y", 0, 5);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int BREED = 4, HOOG = 6;

    private static final VoxelShape PLAAT_X = Block.box(0, 0, 6, 16, 16, 10), PLAAT_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape BALK_X = Block.box(0, 0, 5, 16, 10, 11), BALK_Z = Block.box(5, 0, 0, 11, 10, 16);

    public SafariPoortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AS, Direction.Axis.X).setValue(DEEL_X, 0).setValue(DEEL_Y, 0).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AS, DEEL_X, DEEL_Y, OPEN);
    }

    /**
     * 1.7.0: a structure placed turned or mirrored turns the gate along: the axis turns, and where the row of parts now
     * runs the negative way deel_x counts from the other end (so {@link #hoek} still finds the corner).
     */
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return nl.juiced.guhs.block.Draaien.rij(state, AS, DEEL_X, BREED, rotation.rotate(nl.juiced.guhs.block.Draaien.langs(state.getValue(AS))));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return nl.juiced.guhs.block.Draaien.rij(state, AS, DEEL_X, BREED, mirror.mirror(nl.juiced.guhs.block.Draaien.langs(state.getValue(AS))));
    }

    // --- the parts ------------------------------------------------------------------------------------------------------------

    /** The corner part (deel 0, 0) of the gate this part belongs to. */
    public static BlockPos hoek(BlockPos pos, BlockState state) {
        int dx = state.getValue(DEEL_X), dy = state.getValue(DEEL_Y);
        return state.getValue(AS) == Direction.Axis.X ? pos.offset(-dx, -dy, 0) : pos.offset(0, -dy, -dx);
    }

    /** Part (dx, dy) of the gate with this corner. */
    public static BlockPos deel(BlockPos hoek, Direction.Axis as, int dx, int dy) {
        return as == Direction.Axis.X ? hoek.offset(dx, dy, 0) : hoek.offset(0, dy, dx);
    }

    /** Places a whole closed gate with its corner at hoek (the park generator does the same in its tiles). */
    public static void zet(Level level, BlockPos hoek, Direction.Axis as) {
        BlockState basis = SafariRitten.POORT.get().defaultBlockState().setValue(AS, as);
        for (int dy = 0; dy < HOOG; dy++) {
            for (int dx = 0; dx < BREED; dx++) {
                level.setBlock(deel(hoek, as, dx, dy), basis.setValue(DEEL_X, dx).setValue(DEEL_Y, dy), Block.UPDATE_ALL);
            }
        }
    }

    /** Opens or closes every part of the gate with this corner (with the creak of a big wooden gate). */
    public static void zetOpen(Level level, BlockPos hoek, boolean open) {
        BlockState h = level.getBlockState(hoek);
        if (!(h.getBlock() instanceof SafariPoortBlock) || h.getValue(OPEN) == open) {
            return;
        }
        Direction.Axis as = h.getValue(AS);
        for (int dy = 0; dy < HOOG; dy++) {
            for (int dx = 0; dx < BREED; dx++) {
                BlockPos p = deel(hoek, as, dx, dy);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof SafariPoortBlock) {
                    level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos midden = deel(hoek, as, 2, 1);
        level.playSound(null, midden, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    // --- who may pass -----------------------------------------------------------------------------------------------------

    /**
     * May this entity walk through the open gate? Players, guhs and the rides (the truck) yes; everything else (the park
     * animals, the race ostrich - ridden or not -, mobs) no: for them an open gate is still a wall.
     */
    public static boolean magDoor(@Nullable Entity e) {
        if (e == null) {
            return false;
        }
        return e instanceof Player || e instanceof GuhEntity || e instanceof SchemaRit;
    }

    private static boolean magDoor(CollisionContext context) {
        return context instanceof EntityCollisionContext ec && magDoor(ec.getEntity());
    }

    private static VoxelShape plaat(BlockState state) {
        boolean x = state.getValue(AS) == Direction.Axis.X;
        return state.getValue(DEEL_Y) == HOOG - 1 ? (x ? BALK_X : BALK_Z) : (x ? PLAAT_X : PLAAT_Z);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN) || state.getValue(DEEL_Y) == HOOG - 1) {
            return plaat(state);
        }
        // open: only the hinge columns at both ends (so you can still click it shut)
        int dx = state.getValue(DEEL_X);
        return dx == 0 || dx == BREED - 1 ? plaat(state) : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN) || state.getValue(DEEL_Y) == HOOG - 1 || !magDoor(context)) {
            return plaat(state);
        }
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND && state.getValue(OPEN) && state.getValue(DEEL_Y) < HOOG - 1;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // --- using, placing, breaking ----------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && sl.getBlockEntity(hoek(pos, state)) instanceof SafariPoortBlockEntity be) {
            be.klik(sl);
        }
        return InteractionResult.SUCCESS;
    }

    /** From the item: the gate stands across your view (4 wide to your right, 6 high), if there is room for it. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis as = context.getHorizontalDirection().getClockWise().getAxis();
        BlockPos hoek = context.getClickedPos();
        Level level = context.getLevel();
        for (int dy = 0; dy < HOOG; dy++) {
            for (int dx = 0; dx < BREED; dx++) {
                BlockPos p = deel(hoek, as, dx, dy);
                if (!level.getBlockState(p).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(p)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(AS, as);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            zet(level, pos, state.getValue(AS));
        }
    }

    /** Breaking one part takes the whole gate down (one item back, not 24). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos hoek = hoek(pos, state);
            Direction.Axis as = state.getValue(AS);
            for (int dy = 0; dy < HOOG; dy++) {
                for (int dx = 0; dx < BREED; dx++) {
                    BlockPos p = deel(hoek, as, dx, dy);
                    if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof SafariPoortBlock) {
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
            if (!player.isCreative()) {
                popResource(level, pos, new ItemStack(this));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // --- the block entity --------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(DEEL_X) == 0 && state.getValue(DEEL_Y) == 0 ? new SafariPoortBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, SafariRitten.POORT_BE.get(), SafariPoortBlockEntity::tick);
    }
}
