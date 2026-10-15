package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The block of a vadskracht battery (the Knabbelbatterij of tech-bronnen is one of these): storage that a net fills with
 * what its sources give too much and empties when its machines ask more ({@link BatterijBlockEntity}). It shows how full it
 * is in five steps ({@link #LADING}: 0 = empty .. 4 = full; the face sleeps when empty and looks surprised when full) and to
 * a comparator (0..15).
 * <pre>
 * BATTERIJ = BLOCKS.registerBlock("knabbelbatterij", p -> new BatterijBlock(p, VadsGetallen.BATTERIJ, () -> BATTERIJ_BE.get()), ...);
 * BATTERIJ_BE = BLOCK_ENTITIES.register("knabbelbatterij", () -> new BlockEntityType<>(
 *         (pos, state) -> new BatterijBlockEntity(BATTERIJ_BE.get(), pos, state), BATTERIJ.get()));
 * // capabilities: VadskrachtFeature.knoopCapability(event, BATTERIJ_BE.get());
 * // resources:    vadskracht.batterij(h, "knabbelbatterij", basis, accent)
 * </pre>
 */
public class BatterijBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LADING = IntegerProperty.create("lading", 0, 4);

    private final long max;
    private final Supplier<? extends BlockEntityType<? extends BatterijBlockEntity>> type;
    private final MapCodec<BatterijBlock> codec;

    public BatterijBlock(Properties p, long max, Supplier<? extends BlockEntityType<? extends BatterijBlockEntity>> type) {
        super(p);
        this.max = max;
        this.type = type;
        this.codec = simpleCodec(props -> new BatterijBlock(props, max, type));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LADING, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LADING);
    }

    /** How much one of these holds, in VK. */
    public long max() {
        return max;
    }

    /** The step (0..4) that shows this much: 0 only when empty, 4 only when full. */
    public static int lading(long inhoud, long max) {
        if (inhoud <= 0 || max <= 0) {
            return 0;
        }
        return inhoud >= max ? 4 : 1 + (int) Math.min(2, inhoud * 3 / max);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatterijBlockEntity(type.get(), pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
        level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof BatterijBlockEntity batterij) || batterij.vadsMax() <= 0 || batterij.vadsInhoud() <= 0) {
            return 0;
        }
        return Mth.clamp(1 + (int) (batterij.vadsInhoud() * 14 / batterij.vadsMax()), 1, 15);
    }
}
