package nl.juiced.guhs.feature.bio.blokkendal;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shoji: a thin paper panel in a wooden grid, one block big, that really slides. Right-click: the panel slides sideways
 * (to the side {@link #KANT} says) and stands as a narrow stack against the post there, so you can walk through.
 * <ul>
 *   <li>Panels above each other are one sliding door: they open and close together (a doorway is as high as you stack).</li>
 *   <li>Two panels next to each other that slide away from each other are a pair: click one, both open.</li>
 *   <li>Placing: on top of or under a panel you get the same panel one higher. Next to a panel it joins that wall
 *       (same front) and slides the other way, so two panels are a pair by themselves; a lone panel slides into the wall
 *       beside it, or to the side of the block you clicked on.</li>
 * </ul>
 * It never blocks light (no occlusion, sky light goes through). FACING is the way the placer looked, like a door.
 */
public class ShojiBlock extends Block {
    public static final MapCodec<ShojiBlock> CODEC = simpleCodec(ShojiBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** The side the panel slides to (left and right as the placer saw it). */
    public static final EnumProperty<DoorHingeSide> KANT = BlockStateProperties.DOOR_HINGE;
    /** How far a sliding door reaches up and down from the panel you click. */
    public static final int BEREIK = 8;

    private static final Map<Direction, VoxelShape> DICHT = net.minecraft.world.phys.shapes.Shapes.rotateHorizontal(Block.box(0, 0, 7, 16, 16, 9));
    private static final Map<Direction, VoxelShape> OPEN_LINKS = net.minecraft.world.phys.shapes.Shapes.rotateHorizontal(Block.box(0, 0, 5, 3, 16, 9));
    private static final Map<Direction, VoxelShape> OPEN_RECHTS = net.minecraft.world.phys.shapes.Shapes.rotateHorizontal(Block.box(13, 0, 5, 16, 16, 9));

    public ShojiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(KANT, DoorHingeSide.LEFT));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, KANT);
    }

    /** The neighbour on the side this panel slides to. */
    static Direction schuifkant(BlockState state) {
        Direction f = state.getValue(FACING);
        return state.getValue(KANT) == DoorHingeSide.LEFT ? f.getCounterClockWise() : f.getClockWise();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        if (!state.getValue(OPEN)) {
            return DICHT.get(f);
        }
        return (state.getValue(KANT) == DoorHingeSide.LEFT ? OPEN_LINKS : OPEN_RECHTS).get(f);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type != PathComputationType.WATER && state.getValue(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction d : new Direction[] {Direction.DOWN, Direction.UP}) {   // one higher or lower: the same panel
            BlockState other = level.getBlockState(pos.relative(d));
            if (other.is(this)) {
                return other;
            }
        }
        Direction facing = context.getHorizontalDirection();
        for (Direction side : new Direction[] {facing.getCounterClockWise(), facing.getClockWise()}) {   // join a wall of panels: same front
            BlockState other = level.getBlockState(pos.relative(side));
            if (other.is(this) && other.getValue(FACING).getAxis() == facing.getAxis()) {
                facing = other.getValue(FACING);
                break;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(KANT, kant(level, pos, facing, context.getClickLocation()));
    }

    /** Which way a new panel slides: away from a panel beside it, else into the one wall beside it, else to the clicked half. */
    private DoorHingeSide kant(Level level, BlockPos pos, Direction facing, Vec3 klik) {
        Direction links = facing.getCounterClockWise(), rechts = facing.getClockWise();
        BlockState l = level.getBlockState(pos.relative(links)), r = level.getBlockState(pos.relative(rechts));
        boolean paneelLinks = l.is(this) && l.getValue(FACING) == facing, paneelRechts = r.is(this) && r.getValue(FACING) == facing;
        if (paneelLinks != paneelRechts) {
            return paneelLinks ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        }
        if (!paneelLinks) {
            boolean muurLinks = l.isCollisionShapeFullBlock(level, pos.relative(links)), muurRechts = r.isCollisionShapeFullBlock(level, pos.relative(rechts));
            if (muurLinks != muurRechts) {
                return muurLinks ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            }
        }
        // how far to the left (0) or right (1) of the block the click was, seen along FACING
        double x = klik.x - pos.getX() - 0.5, z = klik.z - pos.getZ() - 0.5;
        double naarRechts = x * rechts.getStepX() + z * rechts.getStepZ();
        return naarRechts > 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean open = !state.getValue(OPEN);
        schuif(level, pos, state, open);
        BlockPos naast = pos.relative(schuifkant(state).getOpposite());   // the pair: the panel on my free side that slides the other way
        BlockState partner = level.getBlockState(naast);
        if (isPartner(state, partner)) {
            schuif(level, naast, partner, open);
        }
        level.playSound(player, pos, open ? SoundEvents.BAMBOO_WOOD_DOOR_OPEN : SoundEvents.BAMBOO_WOOD_DOOR_CLOSE, SoundSource.BLOCKS, 0.9f,
                level.getRandom().nextFloat() * 0.1f + 1.0f);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    /** Two panels in one wall that slide away from each other. */
    static boolean isPartner(BlockState state, BlockState other) {
        return other.getBlock() == state.getBlock() && other.getValue(FACING) == state.getValue(FACING) && other.getValue(KANT) != state.getValue(KANT);
    }

    /** Opens or closes this panel and the same panels above and below it. */
    private void schuif(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        for (Direction d : new Direction[] {Direction.UP, Direction.DOWN}) {
            BlockPos.MutableBlockPos p = pos.mutable();
            for (int i = 0; i < BEREIK; i++) {
                p.move(d);
                BlockState s = level.getBlockState(p);
                if (!s.is(this) || s.getValue(FACING) != state.getValue(FACING) || s.getValue(KANT) != state.getValue(KANT)) {
                    break;
                }
                level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(KANT);
    }
}
