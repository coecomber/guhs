package nl.juiced.guhs.feature.beroepen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A see-through ghost tile on Bob de Guhbouwer's roof: a dakpan still goes here (right-click it with one, see
 * {@link DakpanItem}). Solid to walk on, can't be broken in survival.
 */
public class DakplekBlock extends Block {
    public static final MapCodec<DakplekBlock> CODEC = simpleCodec(DakplekBlock::new);

    public DakplekBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
        return neighbour.is(this) || super.skipRendering(state, neighbour, side);
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
