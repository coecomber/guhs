package nl.juiced.guhs.feature.eilanden;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The wolkenlift: a fluffy cloud pad. It blows a column of {@link WolkenstroomBlock} straight up (up to {@link #MAX_HEIGHT}
 * blocks, until something is in the way) that lifts you to the top and puffs you off in the direction it faces. Placed
 * while sneaking it's a "down" lift: its column lets you float down softly instead. The floating guh islands have
 * both; the player's own ones face where the player looked when placing it.
 */
public class WolkenliftBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WolkenliftBlock> CODEC = simpleCodec(WolkenliftBlock::new);
    public static final BooleanProperty DOWN = WolkenstroomBlock.DOWN;
    public static final int MAX_HEIGHT = 96;

    public WolkenliftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean down = context.getPlayer() != null && context.getPlayer().isSecondaryUseActive();
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(DOWN, down);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            buildColumn(level, pos, state);
        }
    }

    /**
     * Fills the air above the pad with wolkenstroom (facing and direction of the pad), at most {@link #MAX_HEIGHT}
     * blocks and never past the build limit or through a block. Returns how many stream blocks it placed.
     */
    public static int buildColumn(Level level, BlockPos pad, BlockState state) {
        BlockState stream = EilandenFeature.WOLKENSTROOM.get().defaultBlockState()
                .setValue(WolkenstroomBlock.FACING, state.getValue(FACING)).setValue(DOWN, state.getValue(DOWN));
        int placed = 0;
        BlockPos.MutableBlockPos p = pad.mutable();
        for (int i = 1; i <= MAX_HEIGHT; i++) {
            p.setY(pad.getY() + i);
            if (level.isOutsideBuildHeight(p)) {
                break;
            }
            BlockState here = level.getBlockState(p);
            if (!here.isAir() && !here.is(EilandenFeature.WOLKENSTROOM.get())) {
                break;
            }
            level.setBlock(p, stream, Block.UPDATE_CLIENTS);
            placed++;
        }
        return placed;
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
