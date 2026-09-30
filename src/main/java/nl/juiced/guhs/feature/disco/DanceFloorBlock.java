package nl.juiced.guhs.feature.disco;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A tile of the light-up dance floor around the Simon-says tiles: its (animated) texture runs through all the disco
 * colours by itself. The four {@link #FASE} variants run a step apart and are laid out by position (2x2), so a floor of
 * them twinkles in a checkerboard. Purely client side: no ticking, no light updates.
 */
public class DanceFloorBlock extends Block {
    public static final MapCodec<DanceFloorBlock> CODEC = simpleCodec(DanceFloorBlock::new);
    public static final IntegerProperty FASE = IntegerProperty.create("fase", 0, 3);

    public DanceFloorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FASE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FASE);
    }

    /** The phase that belongs to this spot: neighbours always differ. */
    public static int fase(int x, int z) {
        return (x & 1) + 2 * (z & 1);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FASE, fase(context.getClickedPos().getX(), context.getClickedPos().getZ()));
    }
}
