package nl.juiced.guhs.feature.guhwaii;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * The vadsigheid-scanner (from 626-guh's capsule): a round scan plate with a big screen on a post behind it. Put a guh on
 * the plate (walk it there, hold a picked-up guh, or just click: your nearest guh hops on) and click: the meter
 * VADSIGHEIDSNIVEAU fills up... and breaks right through to ONBEREKENBAAR VAHOEG ({@link Scanner}). Works on every guh.
 * FACING = the way the screen looks (towards the one who placed it); the post stands at the back.
 */
public class ScannerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ScannerBlock> CODEC = simpleCodec(ScannerBlock::new);
    private static final VoxelShape PLAAT = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape[] VORM = new VoxelShape[4];

    static {
        // the post at the back (the side opposite FACING), per horizontal direction (2D data value order: S, W, N, E)
        VORM[Direction.SOUTH.get2DDataValue()] = Shapes.or(PLAAT, Block.box(6, 3, 0, 10, 16, 3));
        VORM[Direction.NORTH.get2DDataValue()] = Shapes.or(PLAAT, Block.box(6, 3, 13, 10, 16, 16));
        VORM[Direction.EAST.get2DDataValue()] = Shapes.or(PLAAT, Block.box(0, 3, 6, 3, 16, 10));
        VORM[Direction.WEST.get2DDataValue()] = Shapes.or(PLAAT, Block.box(13, 3, 6, 16, 16, 10));
    }

    public ScannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            Scanner.gebruik(server, pos, sp, ItemStack.EMPTY);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem && !(stack.getItem() instanceof nl.juiced.guhs.item.PickedUpGuhItem)
                && player.isSecondaryUseActive()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            Scanner.gebruik(server, pos, sp, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /** A soft blue glow drifting up from the scan plate now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.45f, 0.85f, 1f), 0.6f), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.25, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
