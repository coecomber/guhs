package nl.juiced.guhs.feature.techbezorg;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.Kisten;

/**
 * A Haltepaaltje (bbq2): a stop of the Bezorgguhtje. It stands next to (or on) the chest or machine it serves:
 * {@link #FACING} points at that block (down, north, south, west or east, like a hopper's spout). {@link #OPHALEN} is what
 * happens here: true = the guhtje takes things out (the green sign, arrow up), false = it brings things (the orange sign,
 * arrow down). The filter, the Stepstation it belongs to and who placed it are in {@link HaltepaaltjeBlockEntity}.
 * You can walk through it (like a sign), and it needs something to stand on.
 * Resources: tools/features/tech_bezorg.py.
 */
public class HaltepaaltjeBlock extends BaseEntityBlock {
    public static final MapCodec<HaltepaaltjeBlock> CODEC = simpleCodec(HaltepaaltjeBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty OPHALEN = BooleanProperty.create("ophalen");
    private static final VoxelShape VORM = Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 10, 9.5), Block.box(4, 10, 4, 12, 16, 12));
    /** Where a new pole looks for something to serve, after the block it was clicked on. */
    private static final Direction[] ZOEK = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN};

    public HaltepaaltjeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPHALEN, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPHALEN);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HaltepaaltjeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    // --- placing ---

    /** It points at the block it was clicked on when that holds items, else at a neighbour that does, else where you clicked. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction geklikt = context.getClickedFace().getOpposite();
        Direction keuze = null;
        if (geklikt != Direction.UP && heeftSpullen(level, pos, geklikt)) {
            keuze = geklikt;
        }
        for (int i = 0; keuze == null && i < ZOEK.length; i++) {
            if (heeftSpullen(level, pos, ZOEK[i])) {
                keuze = ZOEK[i];
            }
        }
        if (keuze == null) {
            keuze = geklikt == Direction.UP ? Direction.DOWN : geklikt;
        }
        BlockState state = defaultBlockState().setValue(FACING, keuze);
        return state.canSurvive(level, pos) ? state : null;
    }

    private static boolean heeftSpullen(Level level, BlockPos paal, Direction kant) {
        return Kisten.van(level, paal.relative(kant), kant.getOpposite()) != null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity halte) {
            halte.geplaatst(player);
        }
    }

    /** Something to stand on: any block with a collision shape under it. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos onder = pos.below();
        return !level.getBlockState(onder).getCollisionShape(level, onder).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    // --- using ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity halte && player instanceof ServerPlayer sp) {
            sp.openMenu(halte, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
